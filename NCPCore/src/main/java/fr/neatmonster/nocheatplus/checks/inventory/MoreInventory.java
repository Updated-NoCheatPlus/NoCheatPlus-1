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

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.combined.CombinedData;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.players.IPlayerData;

/**
* A check meant to prevent players from exploiting the 4 default crafting slot to get more storage space.
*
*/
public class MoreInventory extends Check {

  /**
    * Instanties a new MoreInventory check
    *
    */
    public MoreInventory() {
        super(CheckType.INVENTORY_MOREINVENTORY);
    }
    

  /**
    * Checks a player
    * @param player 
    * @param PoYdiff the discrepancy between current and last pitch/yaw
    *
    * @author xaw3ep
    * @return true if succesful
    */
    public boolean check(final Player player, final CombinedData cData, final IPlayerData pData, final InventoryType type, 
    	                 final Inventory inv, final boolean PoYdiff) {
        
        // TODO: bring in the moving subcheck in invMove.
        if (type == InventoryType.CRAFTING 
            && (player.isSprinting() || PoYdiff || pData.isShiftKeyPressed() || BridgeMisc.isUsingItem(player))) {
            return true;
        }
        return false;
    }
}
