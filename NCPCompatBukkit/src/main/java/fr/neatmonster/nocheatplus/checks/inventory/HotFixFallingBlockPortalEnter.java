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
package fr.neatmonster.nocheatplus.checks.inventory;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.compat.bukkit.BridgeMaterial;
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
import fr.neatmonster.nocheatplus.logging.Streams;
import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;
import fr.neatmonster.nocheatplus.utilities.location.LocUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.WrapBlockCache;
import fr.neatmonster.nocheatplus.worlds.IWorldData;

/**
 * Hot fix for 1.9 and 1.10 (possibly later?): falling block duplication via end
 * portals + pistons.
 * <hr>
 * For registration use NoCheatPlusAPI.addComponent.
 * 
 * @author asofold
 *
 */
public class HotFixFallingBlockPortalEnter implements Listener {

    public static void testAvailability() {
        if (ReflectionUtil.getClass("org.bukkit.event.entity.EntityPortalEnterEvent") == null
                || ReflectionUtil.getClass("org.bukkit.entity.FallingBlock") == null) {
            throw new RuntimeException("Not available.");
        }
        if (!ServerVersion.isMinecraftVersionUnknown() && ServerVersion.compareMinecraftVersion("1.9") < 0) {
            throw new RuntimeException("Not needed.");
        }
    }

    /** Temporary use only: setWorld(null) after use. */
    private final Location useLoc = new Location(null, 0, 0, 0);

    private final WrapBlockCache wrapBlockCache; // TODO: Fetch a getter from the registry.

    public HotFixFallingBlockPortalEnter() {
        testAvailability();
        wrapBlockCache = new WrapBlockCache();
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        final Entity entity = event.getEntity();
        final Material mat;
        if (entity instanceof FallingBlock) {
            mat = ((FallingBlock) entity).getMaterial();
        }
        else if (entity instanceof Item) {
            // TODO: Not sure if needed.
            if (((Item) entity).getItemStack().getType().hasGravity()) {
                mat = ((Item) entity).getItemStack().getType();
            }
            else {
                mat = null;
            }
        }
        else {
            mat = null;
        }
        if (mat != null) {
            final Location loc = entity.getLocation(useLoc);
            final World world = loc.getWorld();
            final IWorldData worldData = NCPAPIProvider.getNoCheatPlusAPI().getWorldDataManager().getWorldData(world);
            if (worldData.getGenericInstance(InventoryConfig.class).hotFixFallingBlockEndPortalActive) {
                final BlockCache blockCache = wrapBlockCache.getBlockCache();
                blockCache.setAccess(world);
                final boolean nearbyPortal = BlockProperties.collidesId(blockCache, 
                        loc.getX() - 2.0, loc.getY() - 2.0, loc.getZ() - 2.0, 
                        loc.getX() + 3.0, loc.getY() + 3.0, loc.getZ() + 3.0, 
                        BridgeMaterial.END_PORTAL);
                blockCache.cleanup();
                if (nearbyPortal) {
                    // Likely spigot currently removes entities entering portals anyway (cross-world teleport issues).
                    // On remove: Looks like setDropItem(false) wouldn't suffice.
                    entity.remove();
                    // TODO: STATUS: should have another stream for violations/protection.
                    final String message = "[INVENTORY_HOTFIX] Remove falling block entering a portal near an end portal: " + mat + " at " + world.getName() + "/" + LocUtil.simpleFormatPosition(loc);
                    NCPAPIProvider.getNoCheatPlusAPI().getLogManager().info(Streams.STATUS, message);
                }
            }
            useLoc.setWorld(null);
        }
    }

}
