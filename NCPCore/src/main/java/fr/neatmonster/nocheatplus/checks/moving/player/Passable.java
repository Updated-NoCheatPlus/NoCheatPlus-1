/*
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.neatmonster.nocheatplus.checks.moving.player;

import java.util.Arrays;
import java.util.Locale;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.collision.Axis;
import fr.neatmonster.nocheatplus.utilities.collision.ICollidePassable;
import fr.neatmonster.nocheatplus.utilities.collision.PassableAxisTracing;
import fr.neatmonster.nocheatplus.utilities.location.LocUtil;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.location.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;

public class Passable extends Check {

    /** TESTING RATHER. */

    // TODO: Configuration.
    // TODO: Test cases.
    // TODO: Should keep an eye on passable vs. on-ground, when checking with reduced margins.
    // rt_xzFactor = 1.0; // Problems: Doors, fences.
    private static double rt_xzFactor = 0.99999999999;
    // rt_heightFactor = 1.0; // Since 10.2 (at some point) passable FP with 2-high ceiling.
    private static double rt_heightFactor = 0.99999999999;

    /**
     * Convenience for player moving, to keep a better overview.
     * 
     * @param from
     * @param to
     * @return
     */
    public static boolean isPassable(Location from, Location to) {
        // TODO ... alternate axes ? Currently only used in a simple check (y-axis only).
        return BlockProperties.isPassableAxisWise(from, to);
    }

    private final ICollidePassable rayTracing = new PassableAxisTracing();
    private final BlockChangeTracker blockTracker;

    public Passable() {
        super(CheckType.MOVING_PASSABLE);
        // TODO: Configurable maxSteps?
        rayTracing.setMaxSteps(60);
        blockTracker = NCPAPIProvider.getNoCheatPlusAPI().getBlockChangeTracker();
    }

    public Location check(final Player player, 
            final PlayerLocation from, final PlayerLocation to, 
            final MovingData data, final MovingConfig cc, final IPlayerData pData, 
            final int tick, final boolean useBlockChangeTracker) {
        return checkActual(player, from, to, data, cc, pData, tick, useBlockChangeTracker);
    }

    private Location checkActual(final Player player, 
            final PlayerLocation from, final PlayerLocation to, 
            final MovingData data, final MovingConfig cc, final IPlayerData pData,
            final int tick, final boolean useBlockChangeTracker) {

        final boolean debug = pData.isDebugActive(type);

        // TODO: Distinguish feet vs. box.

        // Block distances (sum, max) for from-to (not for loc!).
        final int manhattan = from.manhattan(to);

        // Check default order first, then others.
        rayTracing.setAxisOrder(Axis.AXIS_ORDER_YXZ);
        String newTag = checkRayTracing(player, from, to, manhattan, 
                data, cc, debug, tick, useBlockChangeTracker);
        if (newTag != null) {
            newTag = checkRayTracingAlernateOrder(player, from, to, manhattan, 
                    debug, data, cc, tick, 
                    useBlockChangeTracker, newTag);
        }
        // Finally handle violations.
        if (newTag == null) {
            // (Might consider if vl>=1: only decrease if from and loc are passable too, though micro...)
            data.passableVL *= 0.99;
            return null;
        }
        else {
            // Direct return.
            return potentialViolation(player, from, to, manhattan, 
                    debug, newTag, data, cc);
        }
    }

    private String checkRayTracingAlernateOrder(final Player player, 
            final PlayerLocation from, final PlayerLocation to, 
            final int manhattan, final boolean debug,
            final MovingData data, final MovingConfig cc, 
            final int tick, final boolean useBlockChangeTracker,
            final String previousTag) {
        /*
         * General assumption for now: Not all combinations have to be checked.
         * If y-first works, only XZ and ZX need to be checked. There may be
         * more/less restrictions in vanilla client code (e.g. Z
         * collision = end).
         */
        Axis axis = rayTracing.getCollidingAxis();
        // (YXZ is the default order, for which ray-tracing collides.)
        if (axis == Axis.X_AXIS || axis == Axis.Z_AXIS) {
            // Test the horizontal alternative only.
            rayTracing.setAxisOrder(Axis.AXIS_ORDER_YZX);
            return checkRayTracing(player, from, to, manhattan, data, cc, 
                    debug, tick, useBlockChangeTracker);
        }
        else if (axis == Axis.Y_AXIS) {
            // Test both horizontal options, each before vertical.
            rayTracing.setAxisOrder(Axis.AXIS_ORDER_XZY);
            if (checkRayTracing(player, from, to, manhattan, data, cc, 
                    debug, tick, useBlockChangeTracker) == null) {
                return null;
            }
            rayTracing.setAxisOrder(Axis.AXIS_ORDER_ZXY);
            return checkRayTracing(player, from, to, manhattan, data, cc, 
                    debug, tick, useBlockChangeTracker);
        }
        else {
            return previousTag; // In case nothing could be done.
        }
    }

    private String checkRayTracing(final Player player, 
            final PlayerLocation from, final PlayerLocation to, final int manhattan, 
            final MovingData data, final MovingConfig cc, final boolean debug,
            final int tick, final boolean useBlockChangeTracker) {
        String tags = null;
        // NOTE: axis order is set externally.
        setNormalMargins(rayTracing, from);
        rayTracing.set(from, to);
        rayTracing.setIgnoreInitiallyColliding(true);
        if (useBlockChangeTracker) { // TODO: Extra flag for 'any' block changes.
            rayTracing.setBlockChangeTracker(blockTracker, data.blockChangeRef, tick, from.getWorld().getUID());
        }
        //rayTracing.setCutOppositeDirectionMargin(true);
        rayTracing.loop();
        rayTracing.setIgnoreInitiallyColliding(false);
        //rayTracing.setCutOppositeDirectionMargin(false);
        //getEyeHeight() is a way to check isSwimming but not in water
        if (rayTracing.collides() && !Bridge1_13.isSwimming(player) && !Bridge1_9.isGliding(player) && !(player.getEyeHeight()<0.45)) {
            tags = "raytracing_collide_";
        }
        else if (rayTracing.getStepsDone() >= rayTracing.getMaxSteps()) {
            tags = "raytracing_maxsteps_";
        }
        if (debug) {
            debugExtraCollisionDetails(player, rayTracing, "std");
        }
        rayTracing.cleanup();
        return tags;
    }

    /**
     * Default/normal margins.
     * @param rayTracing
     * @param from
     */
    private void setNormalMargins(final ICollidePassable rayTracing, final PlayerLocation from) {
        rayTracing.setMargins(from.getBoxMarginVertical() * rt_heightFactor, from.getWidth() / 2.0 * rt_xzFactor); // max from/to + resolution ?
    }

    /**
     * Axis-wise ray-tracing violation skipping conditions.
     * 
     * @param player
     * @param from
     * @param to
     * @param manhattan
     * @param tags
     * @param data
     * @param cc
     * @return
     */
    private Location potentialViolation(final Player player, 
            final PlayerLocation from, final PlayerLocation to, 
            final int manhattan, final boolean debug,
            String tags, final MovingData data, final MovingConfig cc) {

        // TODO: Might need the workaround for fences.

        return actualViolation(player, from, to, tags, debug, data, cc);
    }

    private Location actualViolation(final Player player, 
            final PlayerLocation from, final PlayerLocation to,
            final String tags, final boolean debug,
            final MovingData data, final MovingConfig cc) {
        Location setBackLoc = null; // Alternative to from.getLocation().

        // Prefer the set back location from the data.
        if (data.hasSetBack()) {
            /*
             * TODO: Harmonize with MovingUtil.getApplicableSetBackLocation
             * (somehow include the desired set back type / loc / context).
             */
            setBackLoc = data.getSetBack(to); // TODO
            if (debug) {
                debug(player, "Using set back location for passable.");
            }
        }

        // Return the reset position.
        data.passableVL += 1d;
        final ViolationData vd = new ViolationData(this, player, data.passableVL, 1, cc.passableActions);
        if (debug || vd.needsParameters()) {
            vd.setParameter(ParameterName.LOCATION_FROM, String.format(Locale.US, "%.2f, %.2f, %.2f", from.getX(), from.getY(), from.getZ()));
            vd.setParameter(ParameterName.LOCATION_TO, String.format(Locale.US, "%.2f, %.2f, %.2f", to.getX(), to.getY(), to.getZ()));
            vd.setParameter(ParameterName.DISTANCE, String.format(Locale.US, "%.2f", TrigUtil.distance(from, to)));
            if (!tags.isEmpty()) {
                vd.setParameter(ParameterName.TAGS, tags);
            }
        }
        if (executeActions(vd).willCancel()) {
            // TODO: Consider another set back position for this, also keeping track of players moving around in blocks.
            final Location newTo;
            if (setBackLoc != null) {
                // Ensure the given location is cloned.
                newTo = LocUtil.clone(setBackLoc);
            } else {
                newTo = from.getLocation();
                if (debug) {
                    debug(player, "Using from location for passable.");
                }
            }
            newTo.setYaw(to.getYaw());
            newTo.setPitch(to.getPitch());
            return newTo;
        }
        else{
            // No cancel action set.
            return null;
        }
    }

    /**
     * Debug only if colliding.
     * 
     * @param player
     * @param rayTracing
     * @param tag
     */
    private void debugExtraCollisionDetails(Player player, ICollidePassable rayTracing, String tag) {
        if (rayTracing.collides()) {
            debug(player, "Raytracing collision with order " + Arrays.toString(rayTracing.getAxisOrder()) 
            + " (" + tag + "): " + rayTracing.getCollidingAxis());
        }
        else if (rayTracing.getStepsDone() >= rayTracing.getMaxSteps()) {
            debug(player, "Raytracing max steps exceeded (" + tag + "): "+ rayTracing.getCollidingAxis());
        }
        // TODO: Detect having used past block changes and log or set a tag.
    }

}
