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

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.InventoryView;

import fr.neatmonster.nocheatplus.checks.access.ACheckData;
import fr.neatmonster.nocheatplus.utilities.ds.count.ActionFrequency;

/**
 * Player specific dataFactory for the inventory checks.
 */
public class InventoryData extends ACheckData {

    // Violation levels.
    public double fastClickVL;
    public double instantBowVL;
    public double fastConsumeVL;
    public double gutenbergVL;



    // Data shared between the checks.
    /** Remember the last time an inventory click happened. Always updates with each click */
    public long lastClickTime = 0;
    /**
     * Remember the last time at which a container was interacted with (Does NOT concern the inventory opening time; interaction comes first) <br>.
     * The time should be set at the same priority level of InventoryData.lastClickTime.
     * (Otherwise an accidental / by 0 may occur with the interaction check in FastClick, if lastClickTime has already been set and containerInteractTime has yet to be set).
     */
    public long containerInteractTime = 0;
    /**
     * Assumption for estimating if the player's own inventory is open:
     * When opening one's own inventory, no information is sent to the server, but a packet will always be sent on closing any kind of inventory (own included)<br>
     * The client also sends information to the server upon clicking into the inventory. <br>
     * Knowing this, we can register the time when the player initially clicked in the inventory and just assume that it will stay open from that moment on, until we receive an InventoryCloseEvent by Bukkit.<br>
     * This estimation method does come with a drawback however: the first inventory click will always be ignored.<br>
     * 
     * 0 time = closed inventory
     */
    public long inventoryOpenTime;

    // Data of the fast click check.
    public final ActionFrequency fastClickFreq = new ActionFrequency(5, 200L);
    public Material fastClickLastClicked = null;
    public int fastClickLastSlot = InventoryView.OUTSIDE;
    public Material fastClickLastCursor = null;
    public int fastClickLastCursorAmount = 0;

    // Data of the instant bow check.
    /** Last time right click interact on bow. A value of 0 means 'invalid'.*/
    public long instantBowInteract = 0;
    public long instantBowShoot;

    // Data of the fastconsume check.
    public Material fastConsumeFood;
    public long fastConsumeInteract;
    
    // Data of the Open check.
    public SlotType clickedSlotType = null;
}
