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

import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.components.registry.event.IHandle;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;

// TODO: Auto-generated Javadoc
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
     * Set the ground false, in case the player is on powder snow without boots.
     * This only concerns players, not entities.
     * 
     * @return True, for the ordinary case.
     */
    public boolean isOnGround() {
        // Always override the result with powder snow, even if the value is cached already.
        if (BlockProperties.isPowderSnow(getTypeId(Location.locToBlock(x), Location.locToBlock(y - 0.01), Location.locToBlock(z)))
            && !BridgeMisc.hasLeatherBootsOn(player)) { 
            onGround = false;
            // Powder snow below without boots: no candidate for ground.
            // Standing in between max and min isn't possible with or without boots.
            // Standing on a block different than powder snow will fallback to the usual onGround logic (will return true).
            return onGround;
        }   
        return super.isOnGround();
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
