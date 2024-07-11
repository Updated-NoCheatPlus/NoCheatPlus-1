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

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.components.registry.event.IHandle;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.utilities.location.RichEntityLocation;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.WrapBlockCache;

/**
 * Represent a move with start and end point. Short-term use of
 * RichEntityLocation instances (from and to), and a Location for temporary use.
 * 
 * @author asofold
 *
 * @param <REL>
 *            Location type to use.
 */
public abstract class MoveInfo <REL extends RichEntityLocation, E extends Entity> {

    /**
     * Would need cloning for passing to external API. This is not initialized
     * in set. World is set to null on cleanup!
     */
    public final Location useLoc = new Location(null, 0, 0, 0);
    public final WrapBlockCache wrapCache;
    public final REL from;
    public final REL to;

    public MoveInfo(final IHandle<MCAccess> mcAccess, REL from, REL to){
        wrapCache = new WrapBlockCache();
        this.from = from;
        this.to = to;
    }

    /**
     * Initialize from, and if given to. Note that useLoc and data are left
     * untouched.
     * 
     * @param entity
     * @param from
     *            Must not be null.
     * @param to
     *            Can be null.
     * @param yOnGround
     */
    public final void set(final E entity, final Location from, final Location to, final double yOnGround){
        final BlockCache cache = wrapCache.getBlockCache();
        if (entity instanceof Player) {
            cache.setPlayerData(DataManager.getPlayerData((Player)entity));
        } 
        cache.setAccess(from.getWorld());
        this.from.setBlockCache(cache);
        set(this.from, from, entity, yOnGround);
        if (to != null){
            this.to.setBlockCache(cache);
            set(this.to, to, entity, yOnGround);
        }
        // Note: using set to reset to by passing null won't work. 
    }

    /**
     * Called after setting BlockCache for the passed rLoc. (Needed to avoid
     * issues with generics.)
     * 
     * @param rLoc
     * @param loc
     * @param entity
     * @param yOnGround
     */
    protected abstract void set(REL rLoc, Location loc, E entity, double yOnGround);

    /**
     * Clear caches and remove World references and such. Also resets the world
     * of useLoc.
     */
    public final void cleanup(){
        useLoc.setWorld(null);
        from.cleanup();
        to.cleanup();
        wrapCache.cleanup();
    }
}
