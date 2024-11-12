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
package fr.neatmonster.nocheatplus.checks.moving.model;

import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.components.location.IGetLocationWithLook;
import fr.neatmonster.nocheatplus.utilities.location.RichBoundsLocation;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;

/**
 * Carry data of a move, involving from- and to- locations. This is for
 * temporary storage and often resetting, also to encapsulate some data during
 * checking. The I/Location instead of I/Position is used in order to be
 * compatible with passing these to set back handling and similar.
 * 
 * @author asofold
 *
 */
public class MoveData {

    //////////////////////////////////////////
    // Guaranteed to be initialized with set.
    //////////////////////////////////////////
    /**
     * Indicate if data has been set. Likely there will be sets of properties
     * with a flag for each such set.
     */
    public boolean valid = false; // Must initialize.

    /**
     * Start point of a move, or a static location (join/teleport).
     */
    public final LocationData from = new LocationData();

    /**
     * Indicate if coordinates for a move end-point and distances are present.
     * Always set on setPositions call.
     */
    public boolean toIsValid = false; // Must initialize.


    /////////////////////////////////////////////////////////////////////
    // Only set if a move end-point is set, i.e. toIsValid set to true.
    /////////////////////////////////////////////////////////////////////
    // Coordinates and distances.
    /**
     * End point of a move.
     */
    public final LocationData to = new LocationData();

    /**
     * The horizontal distance covered by a move on the X axis. Only valid if toIsValid is set to true.
     */
    public double xDistance;

    /**
     * The vertical distance covered by a move. Note the sign for moving up or
     * down. Only valid if toIsValid is set to true.
     */
    public double yDistance;

    /**
     * The horizontal distance covered by a move on the Z axis. Only valid if toIsValid is set to true.
     */
    public double zDistance;

    /**
     * The horizontal distance covered by a move. Only valid if toIsValid is set to true.
     */
    public double hDistance;

    /** Total distance squared. Only valid if toIsValid is set to true. */
    public double distanceSquared;


    //////////////////////////////////////////////////////////
    // Reset with set, could be lazily set during checking.
    //////////////////////////////////////////////////////////
    // Properties involving the environment.
    /**
     * Head is obstructed for a living entity, or can't/couldn't move (further) up due to
     * being blocked somehow. Should expect descending next move, if in air.
     * <br>
     * Set in SurvivalFly with the {@link fr.neatmonster.nocheatplus.utilities.location.RichEntityLocation#collide(Vector, boolean, double[])} method.
     * <li>NOTE: This is much stricter than {@link fr.neatmonster.nocheatplus.utilities.location.RichEntityLocation#seekCollisionAbove(double, boolean)}.<br>
     * This only considers the exact moment of collision, the latter is much more lenient.</li>
     */
    public boolean headObstructed;

    /**
     * Somehow the player has touched ground with this move (including
     * workarounds), thus the client might move up next move. This flag is only
     * updated by from/to.onGround, if MoveData.setExtraProperties is called for
     * this instance.
     */
    public boolean touchedGround;

    /**
     * The player touched ground by the means of a lost-ground workaround exclusively
     */
    public boolean touchedGroundWorkaround, fromLostGround, toLostGround;
    
    // Meta stuff
    /**
     * The fly check that was using the current data. One of MOVING_SURVIVALFLY,
     * MOVING_CREATIVEFLY, UNKNOWN.
     */
    public CheckType flyCheck;

    /**
     * The ModelFlying instance used with this move, will be null if it doesn't
     * apply.
     */
    public ModelFlying modelFlying;

    private void setPositions(final IGetLocationWithLook from, final IGetLocationWithLook to) {
        this.from.setLocation(from);
        this.to.setLocation(to);
        xDistance = this.to.getX() - this.from.getX();
        yDistance = this.to.getY() - this.from.getY();
        zDistance = this.to.getZ() - this.from.getZ();
        hDistance = TrigUtil.xzDistance(from, to);
        distanceSquared = yDistance * yDistance + hDistance * hDistance;
        toIsValid = true;
        flyCheck = null;
        modelFlying = null;
    }

    /**
     * Set with join / teleport / set back.
     * @param x
     * @param y
     * @param z
     * @param yaw
     * @param pitch
     */
    private void setPositions(final String worldName, final double x, final double y, final double z, final float yaw, final float pitch) {
        from.setLocation(worldName, x, y, z, yaw, pitch);
        toIsValid = false;
    }

    /**
     * Valid is set to true here, so call this last on overriding.
     */
    protected void resetBase() {
        // Reset extra properties.
        from.extraPropertiesValid = false;
        to.extraPropertiesValid = false;
        // Properties involving the environment.
        headObstructed = false;
        touchedGround = false;
        touchedGroundWorkaround = false;
        fromLostGround = false;
        toLostGround = false;
        // Done.
        valid = true;
    }

    /**
     * Set some basic data and reset all other properties properly. Does not set
     * extra properties for locations.
     * 
     * @param from
     * @param to
     */
    public void set(final IGetLocationWithLook from, final IGetLocationWithLook to) {
        setPositions(from, to);
        resetBase();
        // TODO: this.from/this.to setExtraProperties ?
    }

    /**
     * Set with join / teleport / set back. Does not set extra properties for
     * locations.
     * 
     * @param loc
     */
    public void set(final IGetLocationWithLook loc) {
        setPositions(loc.getWorldName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        resetBase();
    }

    /**
     * Set with join / teleport / set back, also set extra properties.
     * 
     * @param loc
     */
    public void setWithExtraProperties(final RichBoundsLocation loc) {
        set(loc);
        from.setExtraProperties(loc);
        if (this.from.onGround) {
            this.touchedGround = true;
        }
    }

    /**
     * Update extra properties (onGround and other) within LocationData (from,
     * to), update touchedGround.
     * 
     * @param from
     * @param to
     */
    public void setExtraProperties(final RichBoundsLocation from, final RichBoundsLocation to) {
        this.from.setExtraProperties(from);
        this.to.setExtraProperties(to);
        if (this.from.onGround || this.to.onGround) {
            this.touchedGround = true;
        }
    }

    /**
     * Fast invalidation: just set the flags.
     */
    public void invalidate() {
        valid = false;
        toIsValid = false;
        from.extraPropertiesValid = false;
        to.extraPropertiesValid = false;
    }

    public void addExtraProperties(final StringBuilder builder, final String prefix) {
        if (from.extraPropertiesValid && from.onGroundOrResetCond) {
            if (prefix != null && !prefix.isEmpty()) {
                builder.append(prefix);
            }
            builder.append("from/x: ");
            from.addExtraProperties(builder);
        }
        if (to.extraPropertiesValid && to.onGroundOrResetCond) {
            if (prefix != null && !prefix.isEmpty()) {
                builder.append(prefix);
            }
            builder.append(" to/x: ");
            to.addExtraProperties(builder);
        }
    }

}
