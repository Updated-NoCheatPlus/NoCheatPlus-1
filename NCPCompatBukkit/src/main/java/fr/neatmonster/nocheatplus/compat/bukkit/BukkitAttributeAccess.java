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

import java.util.UUID;

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

public class BukkitAttributeAccess implements IAttributeAccess {

    public BukkitAttributeAccess() {
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
    private AttributeModifier getModifier(final AttributeInstance attrInst, final UUID id) {
        for (final AttributeModifier mod : attrInst.getModifiers()) {
            if (id.equals(mod.getUniqueId())) {
                return mod;
            }
        }
        return null;
    }

    private double getMultiplier(final AttributeModifier mod) {
        return AttribUtil.getMultiplier(operationToInt(mod.getOperation()), mod.getAmount());
    }
    
    @Override
    public float getMovementSpeed(final Player player) {
        // / by 2 to get the base value 0.1f
        return (player.getWalkSpeed() / 2f) * (float) getSpeedMultiplier(player);
    }
    
    @Override
    public double getSpeedMultiplier(final Player player) {
        final AttributeInstance attrInst = BridgeBukkitAPI.getSpeedAttributeInstance(player);
        final double val = attrInst.getValue() / attrInst.getBaseValue();
        final AttributeModifier mod = getModifier(attrInst, AttribUtil.ID_SPRINT_BOOST);
        return mod == null ? val : (val / getMultiplier(mod));
    }
    
    @Override
    public double getSprintMultiplier(final Player player) {
        final AttributeInstance attrInst = BridgeBukkitAPI.getSpeedAttributeInstance(player);
        final AttributeModifier mod = getModifier(attrInst, AttribUtil.ID_SPRINT_BOOST);
        return mod == null ? 1.0 : getMultiplier(mod);
    }
    
    @Override
    public double getGravity(Player player) {
        double gravity;
        final PlayerMoveData thisMove = DataManager.getPlayerData(player).getGenericInstance(MovingData.class).playerMoves.getCurrentMove();
        if (!BridgeMisc.hasGravity(player)) {
            return 0.0;
        }
        if (ServerVersion.isLowerThan("1.20.5")) {
            // Doesn't exist. Calculate manually.
            gravity = thisMove.yDistance <= 0.0 && !Double.isInfinite(Bridge1_13.getSlowfallingAmplifier(player)) ? Magic.SLOW_FALL_GRAVITY : Magic.DEFAULT_GRAVITY;
        }
        else {
            final AttributeInstance attrInst = BridgeBukkitAPI.getGravityAttributeInstance(player);
            // Fail-safe.
            if (attrInst == null) {
                gravity = thisMove.yDistance <= 0.0 && !Double.isInfinite(Bridge1_13.getSlowfallingAmplifier(player)) ? Magic.SLOW_FALL_GRAVITY : Magic.DEFAULT_GRAVITY;
            }
            else gravity = MathUtil.clamp(attrInst.getValue(), -1.0, 1.0);
            if (thisMove.yDistance <= 0.0 && !Double.isInfinite(Bridge1_13.getSlowfallingAmplifier(player))) {
                gravity = Math.min(gravity, Magic.SLOW_FALL_GRAVITY);
            }
        }
        return gravity;
    }
    
    @Override
    public double getSafeFallDistance(Player player) {
        if (ServerVersion.isLowerThan("1.20.5")) {
            // Doesn't exist. Calculate manually.
            return Magic.FALL_DAMAGE_DIST;
        }
        final AttributeInstance attrInst = BridgeBukkitAPI.getSafeFallAttributeInstance(player);
        // Fail-safe
        if (attrInst == null) return Magic.FALL_DAMAGE_DIST;
        return MathUtil.clamp(attrInst.getValue(), -1024.0, 1024.0);
    }
    
    @Override
    public double getFallDamageMultiplier(Player player) {
        if (ServerVersion.isLowerThan("1.20.5")) {
            // Doesn't exist. Calculate manually.
            return 1.0;
        }
        final AttributeInstance attrInst = BridgeBukkitAPI.getFallMultAttributeInstance(player);
        // Fail-safe
        if (attrInst == null) return 1.0;
        return MathUtil.clamp(attrInst.getValue(), 1.0, 100.0);
    }
    
    @Override
    public double getBreakingSpeedMultiplier(Player player) {
        if (ServerVersion.isLowerThan("1.20.5")) {
            // Doesn't exist. Calculate manually.
            return 1.0;
        }
        final AttributeInstance attrInst = BridgeBukkitAPI.getBreakSpeedAttributeInstance(player);
        // Fail-safe
        if (attrInst == null) return 1.0;
        return  MathUtil.clamp(attrInst.getValue(), 1.0, 1024.0);
    }
    
    @Override
    public double getJumpGainMultiplier(Player player) {
        if (ServerVersion.isLowerThan("1.20.5")) {
            // Doesn't exist. Calculate manually.
            return 1.0;
        }
        final AttributeInstance attrInst = BridgeBukkitAPI.getJumpPowerAttributeInstance(player);
        // Fail-safe
        if (attrInst == null) return 1.0;
        final double val = attrInst.getValue() / attrInst.getBaseValue();
        return MathUtil.clamp(val, 0.0, 76.19047619047619); // MathUtil.clamp(attrInst.getValue(), 0.0, 32.0);
    }
    
    @Override
    public double getPlayerSneakingFactor(Player player) {
        // Not available. 1.21 and above only.
        return Magic.SNEAK_MULTIPLIER + BridgeEnchant.getSwiftSneakIncrement(player);
    }
    
    @Override
    public double getPlayerMaxBlockReach(Player player) {
        if (ServerVersion.isLowerThan("1.20.5")) {
            // Doesn't exist. Calculate manually.
            return 4.5;
        }
        final AttributeInstance attrInst = BridgeBukkitAPI.getBlockInteractionRangeAttributeInstance(player);
        // Fail-safe
        if (attrInst == null) return 4.5;
        return MathUtil.clamp(attrInst.getValue(), 0.0, 64.0);
    }
    
    @Override
    public double getPlayerMaxAttackReach(Player player) {
        if (ServerVersion.isLowerThan("1.20.5")) {
            // Doesn't exist. Calculate manually.
            return 3.0;
        }
        final AttributeInstance attrInst = BridgeBukkitAPI.getEntityInteractionRangeAttributeInstance(player);
        // Fail-safe
        if (attrInst == null) return 3.0;
        return MathUtil.clamp(attrInst.getValue(), 0.0, 64.0);
    }
    
    @Override
    public double getMaxStepUp(Player player) {
        if (ServerVersion.isLowerThan("1.20.5")) {
            // Does not exist, calculate manually.
            if (player.isInsideVehicle()) {
                if (player.getVehicle() != null && MaterialUtil.isBoat(player.getVehicle().getType())) {
                    // Boats are unable to step.
                    return 0.0;
                }
                // All other vehicles have a step value of 1.0
                return 1.0;
            }
            final MovingConfig cc = DataManager.getPlayerData(player).getGenericInstance(MovingConfig.class);
            return cc.sfStepHeight;
        }
        final AttributeInstance attrInst = BridgeBukkitAPI.getStepHeightAttributeInstance(player);
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
        // Unsupported. Only since 1.21.
        return depthStrider;
    }
    
    @Override
    public float getMovementEfficiency(Player player) {
        // Not available. 1.21 and above only.
        return 0.0f;
    }
    
    @Override
    public double getSubmergedMiningSpeedMultiplier(Player player) {
        // Not available. 1.21 and above only.
        return 1.0;
    }
    
    @Override
    public double getMiningEfficiency(Player player) {
        // Not available. 1.21 and above only.
        return 0.0;
    }
    
    @Override
    public double getEntityScale(Player player) {
        if (ServerVersion.isLowerThan("1.20.5")) {
            return 1.0;
        }
        final AttributeInstance attrInst = BridgeBukkitAPI.getScaleAttributeInstance(player);
        // Fail-safe.
        if (attrInst == null) return 1.0;
        return MathUtil.clamp(attrInst.getValue(), 0.0625, 16.0);
    }
}
