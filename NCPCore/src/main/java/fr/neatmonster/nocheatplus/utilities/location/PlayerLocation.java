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
package fr.neatmonster.nocheatplus.utilities.location;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.compat.BridgeEnchant;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.components.registry.event.IHandle;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.collision.CollisionUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;

/**
 * Lots of content for a location a player is supposed to be at. Constructors
 * for convenient use.
 */
public class PlayerLocation extends RichEntityLocation {

    // "Heavy" object members that need to be set to null on cleanup. //

    /** The player. */
    private Player player = null;


    /**
     * Instantiates a new player location.
     *
     * @param mcAccess
     *            the mc access
     * @param blockCache
     *            BlockCache instance, may be null.
     */
    public PlayerLocation(final IHandle<MCAccess> mcAccess, final BlockCache blockCache) {
        super(mcAccess, blockCache);
    }
    
    /**
     * Gets the player.
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Straw-man method to account for this specific bug: https://bugs.mojang.com/browse/MC-2404
     * Should not be used outside its intended context (sneaking on edges), or if vanilla uses it.
     */
    public boolean isAboveGround() {
        final IPlayerData pData = DataManager.getPlayerData(player);
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        double yBelow = player.getFallDistance() - cc.sfStepHeight;
        double[] AABB = new double[]{minX, minY+yBelow, minZ, maxX, maxY+yBelow, maxZ};
        return  isOnGround() 
                || pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16_2) 
                && (
                    player.getFallDistance() < cc.sfStepHeight && CollisionUtil.isEmpty(blockCache, player, AABB)
                )
            ;
    }
    
    /**
     * From TridentItem.java
     * Get the riptiding force. Not context-aware.
     *
     * @param player
     * @param onGround
     * @return A Vector containing the riptiding force's components (x,y,z).
     */
    public Vector getTridentPropellingForce(final Player player, boolean onGround) {
        // Only players are allowed to riptide (hence why this is in PlayerLocation and not RichEntity).
        final double RiptideLevel = BridgeEnchant.getRiptideLevel(player);
        if (RiptideLevel > 0.0) {
            // Compute the force of the push
            float x = -TrigUtil.sin(getYaw() * TrigUtil.toRadians) * TrigUtil.cos(getPitch() * TrigUtil.toRadians);
            float y = -TrigUtil.sin(getPitch() * TrigUtil.toRadians);
            float z = TrigUtil.cos(getYaw() * TrigUtil.toRadians) * TrigUtil.cos(getPitch() * TrigUtil.toRadians);
            float distance = MathUtil.sqrt(x*x + y*y + z*z);
            float force = 3.0f * ((1.0f + (float) RiptideLevel) / 4.0f);
            x *= force / distance;
            y *= force / distance;
            z *= force / distance;
            if (onGround) {
                y += 1.1999999284744263f;
            }
            return new Vector(x, y, z);
        }
        return new Vector();
    }
    
    /**
     * From EntityHuman.java <br>
     * Set the speed needed to back the player off from edges in the given vector.
     * This assumes that you have already checked for preconditions (!) <br>
     * [Pre-requisites are: the player must be _shifting_(not sneaking); must be above ground; must have negative or 0 y-distance; must not be flying]
     * 
     * @param vector
     * @return the modified vector
     */
    public Vector maybeBackOffFromEdge(Vector vector) {
        // Only players are capable of crouching (hence why this is in PlayerLocation and not RichEntity).
        final IPlayerData pData = DataManager.getPlayerData(player);
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        double xDistance = vector.getX();
        double zDistance = vector.getZ();
        /** Parameter for searching for collisions below */
        double yBelow = pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_11) ? -cc.sfStepHeight : -1 + CollisionUtil.COLLISION_EPSILON;

        // Move AABB alongside the X axis.
        double[] offsetAABB_X = new double[]{minX+xDistance, minY+yBelow, minZ, maxX+xDistance, maxY+yBelow, maxZ};
        while (xDistance != 0.0 && CollisionUtil.isEmpty(blockCache, player, offsetAABB_X)) {
            if (xDistance < 0.05 && xDistance >= -0.05) {
                xDistance = 0.0;
            } 
            else if (xDistance > 0.0) {
                xDistance -= 0.05;
            } 
            else xDistance += 0.05;
        }
        // Move AABB alongside the Z axis.
        double[] offsetAABB_Z = new double[]{minX, minY+yBelow, minZ+zDistance, maxX, maxY+yBelow, maxZ+zDistance};
        while (zDistance != 0.0 && CollisionUtil.isEmpty(blockCache, player, offsetAABB_Z)) {
            if (zDistance < 0.05 && zDistance >= -0.05) {
                zDistance = 0.0;
            } 
            else if (zDistance > 0.0) {
                zDistance -= 0.05;
            } 
            else zDistance += 0.05;
        }
        // Move AABB alongside both (diagonally)
        double[] offsetAABB_XZ = new double[]{minX+xDistance, minY+yBelow, minZ+zDistance, maxX+xDistance, maxY+yBelow, maxZ+zDistance};
        while (xDistance != 0.0 && zDistance != 0.0 && CollisionUtil.isEmpty(blockCache, player, offsetAABB_XZ)) {
            if (xDistance < 0.05 && xDistance >= -0.05) {
                xDistance = 0.0;
            } 
            else if (xDistance > 0.0) {
                xDistance -= 0.05;
            } 
            else xDistance += 0.05;

            if (zDistance < 0.05 && zDistance >= -0.05) {
                zDistance = 0.0;
            } 
            else if (zDistance > 0.0) {
                zDistance -= 0.05;
            } 
            else zDistance += 0.05;
        }
        vector = new Vector(xDistance, 0.0, zDistance);
        return vector;
    }

    /**
     * Sets the player location object. See
     * {@link #set(Location, Player, double)}.
     *
     * @param location
     *            the location
     * @param player
     *            the player
     */
    public void set(final Location location, final Player player) {
        set(location, player, 0.001);
    }

    /**
     * Sets the player location object. Does not account for special conditions like
     * gliding with elytra with special casing, instead the maximum of accessible heights is used (eyeHeight, nms height/length). Does not set or reset blockCache.
     *
     * @param location
     *            the location
     * @param player
     *            the player
     * @param yOnGround
     *            the y on ground
     */
    public void set(final Location location, final Player player, final double yOnGround) {
        super.set(location, player, yOnGround);
        // Entity reference.
        this.player = player;
    }

    /**
     * Set with specific height/length/eyeHeight properties.
     * @param location
     * @param player
     * @param width
     * @param eyeHeight
     * @param height
     * @param fullHeight
     * @param yOnGround
     */
    public void set(final Location location, final Player player, final double width,  
            final double eyeHeight, final double height, final double fullHeight, final double yOnGround) {
        super.doSetExactHeight(location, player, true, width, eyeHeight, height, fullHeight, yOnGround);
        // Entity reference.
        this.player = player;
    }

    /**
     * Not supported.
     *
     * @param location
     *            the location
     * @param entity
     *            the entity
     * @param yOnGround
     *            the y on ground
     */
    @Override
    public void set(Location location, Entity entity, double yOnGround) {
        throw new UnsupportedOperationException("Set must specify an instance of Player.");
    }

    /**
     * Not supported.
     *
     * @param location
     *            the location
     * @param entity
     *            the entity
     * @param fullHeight
     *            the full height
     * @param yOnGround
     *            the y on ground
     */
    @Override
    public void set(Location location, Entity entity, double fullHeight, double yOnGround) {
        throw new UnsupportedOperationException("Set must specify an instance of Player.");
    }

    /**
     * Not supported.
     *
     * @param location
     *            the location
     * @param entity
     *            the entity
     * @param fullWidth
     *            the full width
     * @param fullHeight
     *            the full height
     * @param yOnGround
     *            the y on ground
     */
    @Override
    public void set(Location location, Entity entity, double fullWidth, double fullHeight, double yOnGround) {
        throw new UnsupportedOperationException("Set must specify an instance of Player.");
    }

    /**
     * Set cached info according to other.<br>
     * Minimal optimizations: take block flags directly, on-ground max/min
     * bounds, only set stairs if not on ground and not reset-condition.
     *
     * @param other
     *            the other
     */
    public void prepare(final PlayerLocation other) {
        super.prepare(other);
    }

    /**
     * Set some references to null.
     */
    public void cleanup() {
        super.cleanup();
        player = null; // Still reset, to be sure.
    }

    /**
     * Check for bounding box properties that might crash the server (if
     * available, not the absolute coordinates).
     *
     * @return true, if successful
     */
    public boolean hasIllegalStance() {
        // TODO: This doesn't check this location, but the player.
        return getMCAccess().isIllegalBounds(player).decide(); // MAYBE = NO
    }

    /* (non-Javadoc)
     * @see fr.neatmonster.nocheatplus.utilities.RichEntityLocation#toString()
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(128);
        builder.append("PlayerLocation(");
        builder.append(world == null ? "null" : world.getName());
        builder.append('/');
        builder.append(Double.toString(x));
        builder.append(", ");
        builder.append(Double.toString(y));
        builder.append(", ");
        builder.append(Double.toString(z));
        builder.append(')');
        return builder.toString();
    }

}
