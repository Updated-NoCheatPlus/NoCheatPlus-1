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
package fr.neatmonster.nocheatplus.utilities;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import fr.neatmonster.nocheatplus.checks.inventory.InventoryData;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;

/**
 * Auxiliary/convenience methods for inventories.
 * @author asofold
 *
 */
public class InventoryUtil {

    /**
     * Collect non-block items by suffix of their Material name (case insensitive).
     * @param suffix
     * @return
     */
    public static List<Material> collectItemsBySuffix(String suffix) {
        suffix = suffix.toLowerCase();
        final List<Material> res = new LinkedList<Material>();
        for (final Material mat : Material.values()) {
            if (!mat.isBlock() && mat.name().toLowerCase().endsWith(suffix)) {
                res.add(mat);
            }
        }
        return res;
    }

    /**
     * Collect non-block items by suffix of their Material name (case insensitive).
     * @param prefix
     * @return
     */
    public static List<Material> collectItemsByPrefix(String prefix) {
        prefix = prefix.toLowerCase();
        final List<Material> res = new LinkedList<Material>();
        for (final Material mat : Material.values()) {
            if (!mat.isBlock() && mat.name().toLowerCase().startsWith(prefix)) {
                res.add(mat);
            }
        }
        return res;
    }

    /**
     * Does not account for special slots like armor.
     *
     * @param inventory
     *            the inventory
     * @return the free slots
     */
    public static int getFreeSlots(final Inventory inventory) {
        final ItemStack[] contents = inventory.getContents();
        int count = 0;
        for (int i = 0; i < contents.length; i++) {
            if (BlockProperties.isAir(contents[i])) {
                count ++;
            }
        }
        return count;
    }

    /**
     * Count slots with type-id and data (enchantments and other meta data are
     * ignored at present).
     *
     * @param inventory
     *            the inventory
     * @param reference
     *            the reference
     * @return the stack count
     */
    public static int getStackCount(final Inventory inventory, final ItemStack reference) {
        if (inventory == null) return 0;
        if (reference == null) return getFreeSlots(inventory);
        final Material mat = reference.getType();
        final int durability = reference.getDurability();
        final ItemStack[] contents = inventory.getContents();
        int count = 0;
        for (int i = 0; i < contents.length; i++) {
            final ItemStack stack = contents[i];
            if (stack == null) {
                continue;
            }
            else if (stack.getType() == mat && stack.getDurability() == durability) {
                count ++;
            }
        }
        return count;
    }

    /**
     * Sum of bottom + top inventory slots with item type / data, see:
     * getStackCount(Inventory, reference).
     *
     * @param view
     *            the view
     * @param reference
     *            the reference
     * @return the stack count
     */
    public static int getStackCount(final InventoryView view, final ItemStack reference) {
        return getStackCount(view.getBottomInventory(), reference) + getStackCount(view.getTopInventory(), reference);
    }

    //    /**
    //     * Search for players / passengers (broken by name: closes the inventory of
    //     * first player found including entity and passengers recursively).
    //     *
    //     * @param entity
    //     *            the entity
    //     * @return true, if successful
    //     */
    //    public static boolean closePlayerInventoryRecursively(Entity entity) {
    //        // Find a player.
    //        final Player player = PassengerUtil.getFirstPlayerIncludingPassengersRecursively(entity);
    //        if (player != null && closeOpenInventory((Player) entity)) {
    //            return true;
    //        } else {
    //            return false;
    //        }
    //    }

    /**
     * Check if the player's inventory is open by looking up the current InventoryView type, via player#getOpenInventory().
     * Note that this method cannot be used to check for one's own inventory, because Bukkit returns CRAFTING/CREATIVE as default InventoryView type.
     * (See InventoryData.inventoryOpenTime)
     *
     * @param player
     *            the player
     * @return True, if the opened inventory is of any type that isn't CRAFTING/CREATIVE and is not null.
     */
    public static boolean hasInventoryOpen(final Player player) {
        final InventoryView view = player.getOpenInventory();
        return view != null && view.getType() != InventoryType.CRAFTING && view.getType() != InventoryType.CREATIVE; // Exclude the CRAFTING and CREATIVE inv type.
    }

   /**
    * Check if the player has opened any kind of inventory (including their own).
    * If the inventory status cannot be assumed from player#getOpenInventory() (see hasInventoryOpen(player)), look up if we have registered the first inventory click time.
    * 
    * @param player
    *            the player
    * @return True, if inventory status is known, or can be assumed with InventoryData.inventoryOpenTime
    */
    public static boolean hasAnyInventoryOpen(final Player player) {
        final IPlayerData pData = DataManager.getPlayerData(player);
        final InventoryData data = pData.getGenericInstance(InventoryData.class);
        return hasInventoryOpen(player) || data.inventoryOpenTime != 0; 
    }
    
   /**
    * Test the player has recently opened an inventory of any type (own, containers).
    * 
    * @param player
    * @param timeAge In milliseconds to be considered as 'recent activity' (inclusive)
    * @return True if the inventory has been opened within the specified time-frame.
    *         False if they've been in the inventory for some time (beyond age).
    * @throws IllegalArgumentException If the timeAge parameter is negative
    */
    public static boolean hasOpenedInvRecently(final Player player, final long timeAge) {
        if (timeAge < 0) {
            throw new IllegalArgumentException("timeAge cannot be negative.");
        }
        final long now = System.currentTimeMillis();
        final IPlayerData pData = DataManager.getPlayerData(player);
        final InventoryData data = pData.getGenericInstance(InventoryData.class);
        return hasAnyInventoryOpen(player) && (now - data.inventoryOpenTime <= timeAge);     
    }
    
   /** 
    * Checks if the time between block interaction and inventory click is recent.
    * 
    * @param player
    * @param timeAge In milliseconds between the BLOCK interaction and inventory click to be considered as 'recent activity' (exclusive)
    * @return True if the time between interaction and inventory click is too recent, false otherwise (beyond age).
    * @throws IllegalArgumentException If the timeAge parameter is negative
    */
    public static boolean isContainerInteractionRecent(final Player player, final long timeAge) {
        if (timeAge < 0) {
            throw new IllegalArgumentException("timeAge cannot be negative.");
        }
        final IPlayerData pData = DataManager.getPlayerData(player);
        final InventoryData data = pData.getGenericInstance(InventoryData.class);
        if (data.containerInteractTime == 0 || data.lastClickTime == 0) {
            return false;
        }
        // The player first interacts with the container, then clicks in its inventory, so interaction should always be smaller than click time
        return Math.abs(data.lastClickTime - data.containerInteractTime) < timeAge;
    }

    /**
     * Return the first consumable item found, checking main hand first and then
     * off hand, if available. Concerns food/edible, potions, milk bucket.
     *
     * @param player
     *            the player
     * @return null in case no item is consumable.
     */
    public static ItemStack getFirstConsumableItemInHand(final Player player) {
        ItemStack actualStack = Bridge1_9.getItemInMainHand(player);
        if (
                Bridge1_9.hasGetItemInOffHand()
                && (actualStack == null || !InventoryUtil.isConsumable(actualStack.getType()))
                ) {
            // Assume this to make sense.
            actualStack = Bridge1_9.getItemInOffHand(player);
            if (actualStack == null || !InventoryUtil.isConsumable(actualStack.getType())) {
                actualStack = null;
            }
        }
        return actualStack;
    }

    /**
     * Test if the ItemStack is consumable, like food, potions, milk bucket.
     *
     * @param stack
     *            May be null, would return false.
     * @return true, if is consumable
     */
    public static boolean isConsumable(final ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return isConsumable(stack.getType());
    }

    /**
     * Test if the given InventoryType can hold items.
     * Intention of this method is to be used within checks' contexts, thus containers with just 1 or 2 slots are excluded for convenience (performing checks for those would be over doing it).
     *
     * @param stack
     *            May be null, would return false.
     * @return true, if is container
     */
    public static boolean isContainerInventory(final InventoryType type) {
        return type != null && (type == InventoryType.CHEST
                            || type == InventoryType.ENDER_CHEST
                            || type == InventoryType.DISPENSER
                            || type == InventoryType.DROPPER
                            || type == InventoryType.HOPPER
                            // For legacy servers... Ugly.
                            || type.toString().equals("SHULKER_BOX")
                            || type.toString().equals("BARREL"));
        // TODO: Add enchanting table to nerf AutoEnchant cheats?
    }

    /**
     * Test if the Material is consumable, like food, potions, milk bucket.
     *
     * @param type
     *            May be null, would return false.
     * @return true, if is consumable
     */
    public static boolean isConsumable(final Material type) {
        return type != null && (type.isEdible() || type == Material.POTION || type == Material.MILK_BUCKET);
    }

    /**
     * Test for max durability, only makes sense with items that can be in
     * inventory once broken, such as elytra. This method does not (yet) provide
     * legacy support. This tests for ItemStack.getDurability() >=
     * Material.getMaxDurability, so it only is suited for a context where this
     * is what you want to check for.
     * 
     * @param stack
     *            May be null, would yield true.
     * @return
     */
    public static boolean isItemBroken(final ItemStack stack) {
        if (stack == null) {
            return true;
        }
        final Material mat = stack.getType();
        return stack.getDurability() >= mat.getMaxDurability();
    }

    /**
     * Test if the player has an arrow in its inventory
     * 
     * @param i
     * @param fw
     * @return True, if there's an arrow in the inv.
     */
    public static boolean hasArrow(final PlayerInventory i, final boolean fw) {
        if (Bridge1_9.hasElytra()) {
            final Material m = i.getItemInOffHand().getType();
            return (fw && m == Material.FIREWORK_ROCKET) || m.toString().endsWith("ARROW") ||
                   i.contains(Material.ARROW) || i.contains(Material.TIPPED_ARROW) || i.contains(Material.SPECTRAL_ARROW);
        }
        return i.contains(Material.ARROW);
    }

}
