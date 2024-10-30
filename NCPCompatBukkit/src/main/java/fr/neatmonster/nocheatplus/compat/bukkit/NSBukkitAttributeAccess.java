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
package fr.neatmonster.nocheatplus.compat.bukkit;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.compat.AttribUtil;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
import fr.neatmonster.nocheatplus.components.modifier.IAttributeAccess;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;
import fr.neatmonster.nocheatplus.utilities.moving.Magic;

public class NSBukkitAttributeAccess implements IAttributeAccess {
    
    public NSBukkitAttributeAccess() {
        if (ReflectionUtil.getClass("org.bukkit.attribute.AttributeInstance") == null) {
            throw new RuntimeException("Service not available.");
        }
    }
    
    private int operationToInt(final Operation operation) {
        switch (operation) {
            case ADD_NUMBER:
                return 0;
            case ADD_SCALAR:
                return 1;
            case MULTIPLY_SCALAR_1:
                return 2;
            default:
                throw new RuntimeException("Unknown operation: " + operation);
        }
    }
    
    /**
     * The first modifier with the given id that can be found, or null if none
     * is found.
     *
     * @param attrInst
     * @param id
     * @return
     */
    private AttributeModifier getModifier(final AttributeInstance attrInst, final NamespacedKey id) {
        for (final AttributeModifier mod : attrInst.getModifiers()) {
            if (id.equals(mod.getKey())) {
                return mod;
            }
        }
        return null;
    }
    
    private double getMultiplier(final AttributeModifier mod) {
        return AttribUtil.getMultiplier(operationToInt(mod.getOperation()), mod.getAmount());
    }
    
    @Override
    public double getSpeedMultiplier(final Player player) {
        final AttributeInstance attrInst = player.getAttribute(BridgeAttribute.MOVEMENT_SPEED);
        final double val = attrInst.getValue() / attrInst.getBaseValue();
        final AttributeModifier mod = getModifier(attrInst, AttribUtil.NSID_SPRINT_BOOST);
        return mod == null ? val : (val / getMultiplier(mod));
    }
    
    @Override
    public double getSprintMultiplier(final Player player) {
        final AttributeInstance attrInst = player.getAttribute(BridgeAttribute.MOVEMENT_SPEED);
        final AttributeModifier mod = getModifier(attrInst, AttribUtil.NSID_SPRINT_BOOST);
        return mod == null ? 1.0 : getMultiplier(mod);
    }
    
    @Override
    public float getMovementSpeed(final Player player) {
        // / by 2 to get the base value 0.1f
        return (player.getWalkSpeed() / 2f) * (float) getSpeedMultiplier(player);
    }
    
    @Override
    public double getGravity(final Player player) {
        double gravity;
        if (!BridgeMisc.hasGravity(player)) {
            return 0.0;
        }
        final PlayerMoveData thisMove = DataManager.getPlayerData(player).getGenericInstance(MovingData.class).playerMoves.getCurrentMove();
        final AttributeInstance attrInst = player.getAttribute(BridgeAttribute.GRAVITY);
        // Fail-safe.
        if (attrInst == null) {
            gravity = thisMove.yDistance <= 0.0 && !Double.isInfinite(Bridge1_13.getSlowfallingAmplifier(player)) ? Magic.SLOW_FALL_GRAVITY : Magic.DEFAULT_GRAVITY;
        }
        else gravity = MathUtil.clamp(attrInst.getValue(), -1.0, 1.0);
        if (thisMove.yDistance <= 0.0 && !Double.isInfinite(Bridge1_13.getSlowfallingAmplifier(player))) {
            gravity = Math.min(gravity, Magic.SLOW_FALL_GRAVITY);
        }
        return gravity;
    }
    
    @Override
    public double getSafeFallDistance(final Player player) {
        final AttributeInstance attrInst = player.getAttribute(BridgeAttribute.SAFE_FALL_DISTANCE);
        // Fail-safe
        if (attrInst == null) return Magic.FALL_DAMAGE_DIST;
        return MathUtil.clamp(attrInst.getValue(), -1024.0, 1024.0);
    }
    
    @Override
    public double getFallDamageMultiplier(final Player player) {
        final AttributeInstance attrInst = player.getAttribute(BridgeAttribute.FALL_DAMAGE_MULTIPLIER);
        // Fail-safe
        if (attrInst == null) return 1.0;
        return MathUtil.clamp(attrInst.getValue(), 1.0, 100.0);
    }
    
    @Override
    public double getBreakingSpeedMultiplier(final Player player) {
        final AttributeInstance attrInst = player.getAttribute(BridgeAttribute.BLOCK_BREAK_SPEED);
        // Fail-safe
        if (attrInst == null) return 1.0;
        return  MathUtil.clamp(attrInst.getValue(), 1.0, 1024.0);
    }
    
    @Override
    public double getJumpGainMultiplier(final Player player) {
        final AttributeInstance attrInst = player.getAttribute(BridgeAttribute.JUMP_STRENGTH);
        // Fail-safe
        if (attrInst == null) return 1.0;
        // Convert it to a multiplier, because we use our own handling for jumping motion.
        final double toMultiplier = attrInst.getValue() / attrInst.getBaseValue();
        return MathUtil.clamp(toMultiplier, 0.0, 76.19047619047619); // MathUtil.clamp(attrInst.getValue(), 0.0, 32.0);
    }
    
    @Override
    public double getPlayerSneakingFactor(final Player player) {
        final AttributeInstance attrInst = player.getAttribute(BridgeAttribute.SNEAKING_SPEED);
        // Fail-safe.
        if (attrInst == null) return Magic.SNEAK_MULTIPLIER + BridgeEnchant.getSwiftSneakIncrement(player);
        return MathUtil.clamp(attrInst.getValue(), 0.0, 1.0);
    }
    
    @Override
    public double getPlayerMaxBlockReach(final Player player) {
        final AttributeInstance attrInst = player.getAttribute(BridgeAttribute.INTERACTION_RANGE);
        // Fail-safe
        if (attrInst == null) return 4.5;
        return MathUtil.clamp(attrInst.getValue(), 0.0, 64.0);
    }
    
    @Override
    public double getPlayerMaxAttackReach(final Player player) {
        final AttributeInstance attrInst = player.getAttribute(BridgeAttribute.ATTACK_RANGE);
        // Fail-safe
        if (attrInst == null) return 3.0;
        return MathUtil.clamp(attrInst.getValue(), 0.0, 64.0);
    }
    
    @Override
    public double getMaxStepUp(final Player player) {
        final AttributeInstance attrInst = player.getAttribute(BridgeAttribute.STEP_HEIGHT);
        if (attrInst == null) {
            // Fail-safe.
            if (player.isInsideVehicle()) {
                if (player.getVehicle() != null && MaterialUtil.isBoat(player.getVehicle().getType())) {
                    return 0.0;
                }
                return 1.0;
            }
            final MovingConfig cc = DataManager.getPlayerData(player).getGenericInstance(MovingConfig.class);
            return cc.sfStepHeight;
        }
        return MathUtil.clamp(attrInst.getValue(), 0.0, 10.0);
    }
    
    @Override
    public float getMovementEfficiency(Player player) {
        final AttributeInstance attrInst = player.getAttribute(BridgeAttribute.MOVEMENT_EFFICIENCY);
        // Fail-safe.
        if (attrInst == null) return 0.0f;
        return (float) MathUtil.clamp(attrInst.getValue(), 0.0, 1.0);
    }
    
    @Override
    public float getWaterMovementEfficiency(Player player) {
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (pData.getClientVersion().isLowerThan(ClientVersion.V_1_8)) {
            // Doesn't exist.
            return 0f;
        }
        final float depthStrider = BridgeEnchant.getDepthStriderLevel(player);
        if (pData.getClientVersion().isLowerThan(ClientVersion.V_1_21)) {
            // Use the legacy method.
            return depthStrider;
        }
        if (ServerVersion.isLowerThan("1.21")) {
            // Simulate what ViaVersion does for newer (1.21 clients) on older (1.21-) servers.
            return depthStrider / 3.0f;
        }
        final AttributeInstance attrInst = player.getAttribute(BridgeAttribute.WATER_MOVEMENT_EFFICIENCY);
        if (attrInst == null) return 0.0f;
        return (float) MathUtil.clamp(attrInst.getValue(), 0.0f, 1.0f);
    }
    
    @Override
    public double getSubmergedMiningSpeedMultiplier(Player player) {
        final AttributeInstance attrInst = player.getAttribute(BridgeAttribute.SUBMERGED_MINING_SPEED);
        // Fail-safe.
        if (attrInst == null) return 1.0;
        return MathUtil.clamp(attrInst.getValue(), 0.0, 20.0);
    }
    
    @Override
    public double getMiningEfficiency(Player player) {
        final AttributeInstance attrInst = player.getAttribute(BridgeAttribute.MINING_EFFICIENCY);
        // Fail-safe.
        if (attrInst == null) return 0.0;
        return MathUtil.clamp(attrInst.getValue(), 0.0, 1024.0);
    }
    
    @Override
    public double getEntityScale(Player player) {
        final AttributeInstance attrInst = player.getAttribute(BridgeAttribute.SCALE);
        // Fail-safe.
        if (attrInst == null) return 1.0;
        return MathUtil.clamp(attrInst.getValue(), 0.0625, 16.0);
        
    }
}
