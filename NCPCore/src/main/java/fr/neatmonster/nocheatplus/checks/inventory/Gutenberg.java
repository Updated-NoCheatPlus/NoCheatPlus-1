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

import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.players.IPlayerData;

/**
 * Simple check for preventing some kind of book exploits
 */
public class Gutenberg extends Check {
    
   /**
    * Instantiates a new gutenberg check.
    */
    public Gutenberg() {
        super(CheckType.INVENTORY_GUTENBERG);
    }
    
    /**
     * Checks a player
     * 
     * @param player
     * @param data
     * @param pData
     * @param pages
     * @return True, if to cancel the event.
     */
    public boolean check(final Player player, final InventoryData data, final IPlayerData pData, int pages) {
    	boolean cancel = false;
    	final InventoryConfig cc = pData.getGenericInstance(InventoryConfig.class);
    	if (pages <= cc.gutenbergPageLimit) {
            // On 1.14+ this is 100, 50 prior.
            // Legitimate.
            return false;
        }
        // Violation.
        final int pagesAboveLimit = pages - cc.gutenbergPageLimit;
        data.gutenbergVL += pagesAboveLimit;
        cancel = executeActions(player, data.gutenbergVL, pagesAboveLimit, cc.gutenbergActions).willCancel();
		return cancel;
    }
}
