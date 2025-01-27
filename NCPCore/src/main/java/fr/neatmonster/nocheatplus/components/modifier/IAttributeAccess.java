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
package fr.neatmonster.nocheatplus.components.modifier;

import org.bukkit.entity.Player;

/**
 * Encapsulate attribute access. Note that some of the methods may exclude
 * specific modifiers, or otherwise perform calculations, e.g. in order to
 * return a multiplier to be applied to typical walking speed.
 * 
 * @author asofold
 *
 */
public interface IAttributeAccess {

    /**
     * Generic speed modifier as a multiplier.
     * This is global, meaning it contains every single speed modifier (slowness, speed, sprint, powder snow, soul speed etc...), unless specified otherwise.
     *
     * @param player
     * @return A multiplier for the allowed speed, excluding the sprint boost
     *         modifier (!). If not possible to determine, it should
     *         Double.MAX_VALUE.
     */
    public double getSpeedMultiplier(Player player);

    /**
     * Sprint boost modifier as a multiplier.
     * Do note that, in its current state, this attribute is not proofed against de-synchronization issues, hence its unreliability.
     * 
     * @param player
     * @return The sprint boost modifier as a multiplier. If not possible to
     *         determine, it should be Double.MAX_VALUE.
     */
    public double getSprintMultiplier(Player player);
    
    /**
     * Retrieve the player's movement speed by multiplying walk-speed with getSpeedAttributeMultiplier. <br>
     * This takes into account Bukkit's /walkspeed command as well.
     * 
     * @param player
     * @return The player speed. If not possible to
     *         determine, it should be Float.MAX_VALUE.
     */
    public float getMovementSpeed(Player player);
    
    /**
     * Retrieve the player's gravity attribute.
     * <hr>
     * <b>Note:</b> This attribute is exclusive to Minecraft 1.20.5 and later.
     *
     * @param player The player whose gravity attribute is being accessed.
     * @return The gravity attribute value, which ranges between -1.0 and 1.0.
     *         Default value is 0.08 and 0.01 for slow fall.<br> If not possible to determine, it should return Double.MAX_VALUE.
     */
    public double getGravity(Player player);
    
    /**
     * Retrieve the player's safe fall distance attribute.
     * This attribute represents the maximum distance (in blocks) the player can fall without taking fall damage.
     * <hr>
     * <b>Note:</b> This attribute is exclusive to Minecraft 1.20.5 and later.
     *
     * @param player The player whose safe fall distance attribute is being accessed.
     * @return The safe fall distance attribute value, which can range between -1024.0 and 1024.0.
     *         Default value is 3.0.<br> If not possible to determine, it should return Double.MAX_VALUE.
     */
    public double getSafeFallDistance(Player player);
    
    /**
     * Retrieve the player's fall damage attribute multiplier.
     * This is a standalone multiplier that affects the amount of fall damage the player takes, and it is unbound from the actual fall distance. <br>
     * <hr>
     * <b>Note:</b> This attribute is exclusive to Minecraft 1.20.5 and later.
     *
     * @param player The player whose fall damage attribute is being accessed.
     * @return The fall damage multiplier value, which can range between 0.0 and 100.0.
     *         Default value is 1.0.<br> If not possible to determine, it should return Double.MAX_VALUE.
     */
    public double getFallDamageMultiplier(Player player);
    
    /**
     * Retrieve the player's block breaking speed attribute multiplier.<br>
     * This is a standalone multiplier that gets applied to breaking speed, and it is unbound from any other modifier/enchant/effect.
     * <hr>
     * <b>Note:</b> This attribute is exclusive to Minecraft 1.20.5 and later.
     *
     * @param player The player whose block breaking speed attribute is being accessed.
     * @return The block breaking speed multiplier value, which can range between 0.0 and 1024.0.
     *         Default value is 1.0.<br> If not possible to determine, it should return Double.MAX_VALUE.
     */
    public double getBreakingSpeedMultiplier(Player player);
    
    /**
     * Retrieve the player's lift-off speed attribute, converted to a standalone multiplier.
     * <hr>
     * <b>Note:</b> This attribute is exclusive to Minecraft 1.20.5 and later.
     *
     * @param player The player whose jump strength attribute is being accessed.
     * @return The jump strength attribute multiplier, which can range between 0.0 and 76.19047619047619
     *         Default value is 1.0.<br> If not possible to determine, it should return Double.MAX_VALUE.
     */
    public double getJumpGainMultiplier(Player player);
    
    /**
     * Retrieve the player's sneaking speed attribute.
     * It is not a separate multiplier applied to sneaking speed, but rather the direct factor
     * that affects how much the player's speed is reduced when sneaking. 
     * This means that: increasing this value up to 1.0 will make the player move at normal walking speed; 
     * decreasing it down to 0.0 would mean making the player unable to move at all when sneaking.
     * It also includes sneaking-related enchants, like Swift Sneak.
     * <hr>
     * <b>Note:</b> This attribute is exclusive to Minecraft 1.21 and later.
     *
     * @param player The player whose sneaking speed attribute is being accessed.
     * @return The sneaking speed attribute value, which ranges from 0.0 to 1.0.
     *         Default value is 0.3.<br> If not possible to determine, it should return Double.MAX_VALUE.
     */
    public double getPlayerSneakingFactor(Player player);
    
    /**
     * Retrieve the player's block interaction range attribute.
     * <hr>
     * <b>Note:</b> This attribute is exclusive to Minecraft 1.20.5 and later.
     *
     * @param player The player whose block interaction range attribute is being accessed.
     * @return The block interaction range value, which can range between 0.0 and 64.0.
     *         Default value is 4.5.<br> If not possible to determine, it should return Double.MAX_VALUE.
     */
    public double getPlayerMaxBlockReach(Player player);
    
    /**
     * Retrieve the player's entity interaction range attribute.
     * <hr>
     * <b>Note:</b> This attribute is exclusive to Minecraft 1.20.5 and later.
     *
     * @param player The player whose entity interaction range attribute is being accessed.
     * @return The entity interaction range value, which can range between 0.0 and 64.0.
     *         Default value is 3.0.<br> If not possible to determine, it should return Double.MAX_VALUE.
     */
    public double getPlayerMaxAttackReach(Player player);
    
    /**
     * Retrieve the player's step height attribute.
     * <hr>
     * <b>Note:</b> This attribute is exclusive to Minecraft 1.20.5 and later.
     *
     * @param player The player whose step height attribute is being accessed.
     * @return The step height attribute value, which can range between 0.0 and 10.0.
     *         Default value is: 0.6 for player entities, 1.0 for ridable entities and 0.0 for boats.<br> If not possible to determine, it should return Double.MAX_VALUE.
     */
    public double getMaxStepUp(Player player);
    
    /**
     * Determines how efficiently a player moves through blocks that slow down or impede movement in some other way, such as soul sand or honey blocks.<br>
     * Specifically, the attribute value is used in the `getBlockSpeedFactor()` method (in EntityLiving.java) to interpolate between the default speed factor
     * and a full-speed factor (1.0F). The interpolation is performed using the formula:
     * <pre>
     * MathUtil.lerp((float) this.getAttributeValue(GenericAttributes.MOVEMENT_EFFICIENCY), super.getBlockSpeedFactor(), 1.0F);
     * </pre>
     *
     * <hr>
     * <b>Note:</b> This attribute is not related to the player's general movement speed and is exclusive to Minecraft 1.21 and later.
     *
     * @param player The player whose movement efficiency is being accessed.
     * @return The movement efficiency value, which can range between 0.0 and 1.0. <br>
     *         A value of 0.0 means the player moves at the default reduced speed through such blocks.
     *         Default value is 0.0. <br> If not possible to determine, it should return Float.MAX_VALUE.
     */
    public float getMovementEfficiency(Player player);
    
    /**
     * Retrieve the player's movement speed when submerged in water (depth strider).
     * <hr>
     * <b>Note:</b> This attribute is exclusive to Minecraft 1.21 and later.
     *
     * @param player The player whose submerged movement efficiency attribute is being accessed.
     * @return The submerged movement efficiency attribute value, which ranges from 0.0 to 1.0. <br>
     *         Default value is 0.0. <br> If not possible to determine, it should return Float.MAX_VALUE.
     */
    public float getWaterMovementEfficiency(Player player);
    
    /**
     * Retrieve the player's mining speed factor when submerged in water. <br>
     * This factor indicates the effectiveness of mining underwater, with a factor of 1.0 meaning mining as quickly as on land, and lower values indicating reduced speed. 
     * It is a separate multiplier applied by the game in the `getDestroySpeed()` method in `EntityHuman.java`. 
     * This factor only accounts for the submersion condition; other factors such as not touching the ground also affect mining speed.
     * <hr>
     * <b>Note:</b> This attribute is exclusive to Minecraft 1.21 and later.
     *
     * @param player The player whose submerged mining attribute multiplier is being accessed.
     * @return The submerged mining attribute multiplier value, which ranges from 0.0 to 20.0. <br>
     *         A value of 0.0 means no mining speed, while 20.0 represents very high mining efficiency underwater. <br>
     *         Default value is typically 0.2 when not using Aqua Affinity. <br> If not possible to determine, it should return Double.MAX_VALUE.
     */
    public double getSubmergedMiningSpeedMultiplier(Player player);
    
    /**
     * Retrieve the player's mining efficiency attribute. <br>
     * It affects the speed at which a player can mine blocks using the correct tool, scaling with tool effectiveness and enchantments like Efficiency. 
     * A higher value results in faster mining with correct tools.
     * <hr>
     * <b>Note:</b> This attribute is exclusive to Minecraft 1.21 and later, and is not a multiplier attribute, but an addition to the typical speed.
     *
     * @param player The player whose mining efficiency attribute multiplier is being accessed.
     * @return The mining efficiency attribute multiplier value, which ranges from 0.0 (meaning, no modification to vanilla) to 1024.0D. <br>
     *         Default value is 0.0. <br> If not possible to determine, it should return Double.MAX_VALUE.
     */
    public double getMiningEfficiency(Player player);
    
    /**
     * Retrieve the player's entity size scaling attribute as a multiplier. <br>
     * This attribute adjusts the size of the player entity, affecting both visual appearance and hitbox dimensions. 
     * A value of 1.0 represents the default size, while values above or below 1.0 adjust the entity size proportionally.
     * <hr>
     * <b>Note:</b> This attribute is exclusive to Minecraft 1.20.5 and later.
     *
     * @param player The player whose entity size scaling attribute is being accessed.
     * @return The entity size scaling attribute value, which ranges from 0.0625 to 16.0. <br>
     *         Default value is 1.0. <br> If not possible to determine, it should return Double.MAX_VALUE.
     */
    public double getEntityScale(Player player);
}