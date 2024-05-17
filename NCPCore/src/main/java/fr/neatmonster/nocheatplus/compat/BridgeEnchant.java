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
package fr.neatmonster.nocheatplus.compat;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;

public final class BridgeEnchant {

    @SuppressWarnings("deprecation")
    private static Enchantment parseEnchantment(final String name) {
        try {
            return Enchantment.getByName(name);
        } catch (Exception e) {
            return null;
        }
    }

    public static Enchantment getFirst(String... names) {
        for (String name : names) {
            final Enchantment type = parseEnchantment(name);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    public static Enchantment getFirstNotNull(String... names) {
        final Enchantment type = getFirst(names);
        if (type == null) {
            throw new NullPointerException("Enchant not present: " + StringUtil.join(names, ", "));
        }
        else {
            return type;
        }
    }

    private final static Enchantment DEPTH_STRIDER = parseEnchantment("DEPTH_STRIDER");

    private final static Enchantment THORNS = parseEnchantment("THORNS");
    
    private final static Enchantment RIPTIDE = parseEnchantment("RIPTIDE");
    
    private final static Enchantment FEATHER_FALLING = getFirstNotNull("FEATHER_FALLING", "PROTECTION_FALL");

    private final static Enchantment SOUL_SPEED = parseEnchantment("SOUL_SPEED");

    private final static Enchantment SWIFT_SNEAK = parseEnchantment("SWIFT_SNEAK");
    
    public final static Enchantment EFFICIENCY = getFirstNotNull("EFFICIENCY", "DIG_SPEED");

    public final static Enchantment AQUA_AFFINITY = getFirstNotNull("AQUA_AFFINITY", "WATER_WORKER");

    /**
     * Retrieve the maximum level for an enchantment, present in any armor slot.
     * 
     * @param player
     * @param enchantment
     *            If null, 0 will be returned.
     * @return 0 if none found, or the maximum found.
     */
    private static int getMaxLevelArmor(final Player player, final Enchantment enchantment) {
        if (enchantment == null) {
            return 0;
        }
        int level = 0;
        // Find the maximum level for the given enchantment.
        final ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            final ItemStack item = armor[i];
            if (!BlockProperties.isAir(item)) {
                level = Math.max(item.getEnchantmentLevel(enchantment), level);
            }
        }
        return level;
    }

    /**
     * Retrieve the level for an enchantment, present in helmet slot.
     * 
     * @param player
     * @param enchantment
     *            If null, 0 will be returned.
     * @return 0 if not found and vice versa
     */
    private static int getHelmetLevelArmor(final Player player, final Enchantment enchantment) {
        if (enchantment == null) {
            return 0;
        }
        int level = 0;
        final ItemStack armor = player.getInventory().getHelmet();
        if (!BlockProperties.isAir(armor)) {
            level = Math.max(armor.getEnchantmentLevel(enchantment), level);
        }
        return level;
    }

    /**
     * Retrieve the level for an enchantment, present in chestplate slot.
     * 
     * @param player
     * @param enchantment
     *            If null, 0 will be returned.
     * @return 0 if not found and vice versa
     */
    private static int getChestplateLevelArmor(final Player player, final Enchantment enchantment) {
        if (enchantment == null) {
            return 0;
        }
        int level = 0;
        final ItemStack armor = player.getInventory().getChestplate();
        if (!BlockProperties.isAir(armor)) {
            level = Math.max(armor.getEnchantmentLevel(enchantment), level);
        }
        return level;
    }

    /**
     * Retrieve the level for an enchantment, present in leggings slot.
     * 
     * @param player
     * @param enchantment
     *            If null, 0 will be returned.
     * @return 0 if not found and vice versa
     */
    private static int getLeggingsLevelArmor(final Player player, final Enchantment enchantment) {
        if (enchantment == null) {
            return 0;
        }
        int level = 0;
        final ItemStack armor = player.getInventory().getLeggings();
        if (!BlockProperties.isAir(armor)) {
            level = Math.max(armor.getEnchantmentLevel(enchantment), level);
        }
        return level;
    }

    /**
     * Retrieve the level for an enchantment, present in boots slot.
     * 
     * @param player
     * @param enchantment
     *            If null, 0 will be returned.
     * @return 0 if not found and vice versa
     */
    private static int getBootsLevelArmor(final Player player, final Enchantment enchantment) {
        if (enchantment == null) {
            return 0;
        }
        int level = 0;
        final ItemStack armor = player.getInventory().getBoots();
        if (!BlockProperties.isAir(armor)) {
            level = Math.max(armor.getEnchantmentLevel(enchantment), level);
        }
        return level;
    }

    /**
     * Test, if there is any armor with the given enchantment on.
     * 
     * @param player
     * @param enchantment
     *            If null, false will be returned.
     * @return
     */
    private static boolean hasArmor(final Player player, final Enchantment enchantment) {
        if (enchantment == null) {
            return false;
        }
        final PlayerInventory inv = player.getInventory();
        final ItemStack[] contents = inv.getArmorContents();
        for (int i = 0; i < contents.length; i++){
            final ItemStack stack = contents[i];
            if (stack != null && stack.getEnchantmentLevel(enchantment) > 0){
                return true;
            }
        }
        return false;
    }

    /**
     * Test, if there is helmet with the given enchantment on.
     * 
     * @param player
     * @param enchantment
     *            If null, false will be returned.
     * @return
     */
    private static boolean hasHelmet(final Player player, final Enchantment enchantment) {
        if (enchantment == null) {
            return false;
        }
        final ItemStack contents = player.getInventory().getHelmet();
        if (contents != null && contents.getEnchantmentLevel(enchantment) > 0){
            return true;
        }
        return false;
    }

    /**
     * Test, if there is a chestplate with the given enchantment on.
     * 
     * @param player
     * @param enchantment
     *            If null, false will be returned.
     * @return
     */
    private static boolean hasChestplate(final Player player, final Enchantment enchantment) {
        if (enchantment == null) {
            return false;
        }
        final ItemStack contents = player.getInventory().getChestplate();
        if (contents != null && contents.getEnchantmentLevel(enchantment) > 0){
            return true;
        }
        return false;
    }

    /**
     * Test, if there are leggings with the given enchantment on.
     * 
     * @param player
     * @param enchantment
     *            If null, false will be returned.
     * @return
     */
    private static boolean hasLeggings(final Player player, final Enchantment enchantment) {
        if (enchantment == null) {
            return false;
        }
        final ItemStack contents = player.getInventory().getLeggings();
        if (contents != null && contents.getEnchantmentLevel(enchantment) > 0){
            return true;
        }
        return false;
    }

    /**
     * Test, if there are boots with the given enchantment on.
     * 
     * @param player
     * @param enchantment
     *            If null, false will be returned.
     * @return
     */
    private static boolean hasBoots(final Player player, final Enchantment enchantment) {
        if (enchantment == null) {
            return false;
        }
        final ItemStack contents = player.getInventory().getBoots();
        if (contents != null && contents.getEnchantmentLevel(enchantment) > 0){
            return true;
        }
        return false;
    }

    public static boolean hasThorns() {
        return THORNS != null;
    }

    public static boolean hasDepthStrider() {
        return DEPTH_STRIDER != null;
    }
    
    public static boolean hasFeatherFalling() {
        return FEATHER_FALLING != null;
    }

    public static boolean hasSoulSpeed() {
        return SOUL_SPEED != null;
    }

    public static boolean hasSwiftSneak() {
        return SWIFT_SNEAK != null;
    }

    /**
     * Check if the player might return some damage due to the "thorns"
     * enchantment.
     * 
     * @param player
     * @return
     */
    public static boolean hasThorns(final Player player) {
        return hasArmor(player, THORNS);
    }

    /**
     * Check if the player has "Soul Speed" enchant
     * enchantment.
     * 
     * @param player
     * @return
     */
    public static boolean hasSoulSpeed(final Player player) {
        return hasBoots(player, SOUL_SPEED);
    }

    /**
     * 
     * @param player
     * @return Maximum level of FEATHER_FALLING found on armor items, capped at 4.
     *         Will return 0 if not available.
     */
    public static int getFeatherFallingLevel(final Player player) {
        // Cap at four.
        return Math.min(4, getBootsLevelArmor(player, FEATHER_FALLING));
    }
    
    /**
     * 
     * @param player
     * @return Maximum level of DEPTH_STRIDER found on armor items, capped at 3.
     *         Will return 0 if not available.
     */
    public static int getDepthStriderLevel(final Player player) {
        // Cap at three.
        return Math.min(3, getBootsLevelArmor(player, DEPTH_STRIDER));
    }

    /**
     * 
     * @param player
     * @return Maximum level of SOUL_SPEED found on armor items, capped at 3.
     *         Will return 0 if not available.
     */
    public static int getSoulSpeedLevel(final Player player) {
        // Cap at three.
        return Math.min(3, getBootsLevelArmor(player, SOUL_SPEED));
    }

    /**
     * 
     * @param player
     * @return Maximum level of SWIFT_SNEAK found on armor items, capped at 3.
     *         Will return 0 if not available.
     */
    public static int getSwiftSneakLevel(final Player player) {
        // Cap at three.
        return Math.min(3, getLeggingsLevelArmor(player, SWIFT_SNEAK));
    }
    
    /**
     * @param player
     * @return The speed increment granted by the swift sneak enchant level (clamped)
     *         (Formula is from LocalPlayer aiStep)
     */      
    public static float getSwiftSneakIncrement(final Player player) {
      return MathUtil.clamp((float)getSwiftSneakLevel(player) * 0.15f, 0.0f, 1.0f);
   }

    /**
     * Retrieve the maximum level for an enchantment, present in main and off hand slot.
     * 
     * @param player
     * @param enchantment
     *            If null, 0 will be returned.
     * @return 0 if none found, or the maximum found.
     */
    private static int getTrident(final Player player, final Enchantment enchantment) {
        if (enchantment == null) {
            return 0;
        }
        int level = 0;
        // Find the maximum level for the given enchantment.
        final ItemStack mainHand = player.getInventory().getItemInMainHand();
        final ItemStack offHand = player.getInventory().getItemInOffHand();
        if (mainHand.getType().equals(BridgeMaterial.TRIDENT)) {
            // Found in main hand already, return.
            return Math.max(mainHand.getEnchantmentLevel(enchantment), level);
        }
        if (offHand.getType().equals(BridgeMaterial.TRIDENT)) {
            level = Math.max(offHand.getEnchantmentLevel(enchantment), level);
        }
        return level;
    }
    
    public static int getRiptideLevel(final Player player) {
    	return Math.min(3, getTrident(player, RIPTIDE));
    }
}
