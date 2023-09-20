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

import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.combined.CombinedData;
import fr.neatmonster.nocheatplus.checks.combined.Improbable;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
import fr.neatmonster.nocheatplus.components.registry.event.IHandle;
import fr.neatmonster.nocheatplus.components.registry.feature.IDisableListener;
import fr.neatmonster.nocheatplus.hooks.ExemptionSettings;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.InventoryUtil;
import fr.neatmonster.nocheatplus.utilities.collision.CollisionUtil;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.moving.Magic;

/**
 * Watch over open inventories - check with "combined" static access, put here because it has too much to do with inventories.
 * @author asofold
 */
public class Open extends Check implements IDisableListener {

    private static Open instance = null;

    private UUID nestedPlayer = null;

    private final IHandle<ExemptionSettings> exeSet = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstanceHandle(ExemptionSettings.class);
    
    // TODO: Add specific contexts (allow different settings for fight / blockbreak etc.).

    /**
     * Static access check, if there is a cancel-action flag the caller should have stored that locally already and use the result to know if to cancel or not.
     * @param player
     * @return If cancelling some event is opportune (open inventory and cancel flag set).
     */
    public static boolean checkClose(Player player) {
        return instance.check(player);
    }

    public Open() {
        super(CheckType.INVENTORY_OPEN);
        instance = this;
    }

    @Override
    public void onDisable() {
        instance = null;
        nestedPlayer = null;
    }

    /**
     * Enforce a closed inventory on this event/action, without checking for any specific condition. <br>
     * This check contains the isEnabled checking (!). Inventory is closed if set in the config. <br>
     * Also resets inventory opening time and interaction time.
     * 
     * @param player
     * @return If cancelling some event is opportune (open inventory and cancel flag set).
     */
    public boolean check(final Player player) {
        final boolean isShulkerBox = player.getOpenInventory().getTopInventory().getType().toString().equals("SHULKER_BOX");
        if (
            // TODO: POC: Item duplication with teleporting NPCS, having their inventory open.
            exeSet.getHandle().isRegardedAsNpc(player)
            || !isEnabled(player) 
            || !InventoryUtil.hasAnyInventoryOpen(player)
            // Workaround, Open would disallow players from opening the container if standing on top
            // of the shulker. Reason for this is unknown
            || isShulkerBox) {
            return false;
        }
        final IPlayerData pData = DataManager.getPlayerData(player);
        final InventoryConfig cc = pData.getGenericInstance(InventoryConfig.class);
        if (cc.openClose) {
            // NOTE about UUID stuff: https://github.com/NoCheatPlus/NoCheatPlus/commit/a41ff38c997bcca780da32681e92216880e9e1b0
            final UUID id = player.getUniqueId();
            if ((this.nestedPlayer == null || !id.equals(this.nestedPlayer))) {
                // (The second condition represents an error, but we don't handle alternating things just yet.)
                this.nestedPlayer = id;
                player.closeInventory();
                pData.getGenericInstance(InventoryData.class).inventoryOpenTime = 0;
                pData.getGenericInstance(InventoryData.class).containerInteractTime = 0;
                this.nestedPlayer = null;
                return true;
            }
        }
        return false;
    }

    /**
     * To be called during PlayerMoveEvents: determine if the inventory should be allowed to stay open. <br>
     * (Against InventoryMove cheats and similar)<br>
     * 
     * @param player
     * @param pData
     * @return True, if the open inventory needs to be closed during this movement.
     */
    public boolean shouldCloseInventory(final Player player, final IPlayerData pData) {
        final MovingConfig mCC = pData.getGenericInstance(MovingConfig.class);
        final MovingData mData = pData.getGenericInstance(MovingData.class);
        final InventoryConfig cc = pData.getGenericInstance(InventoryConfig.class);
        final InventoryData data = pData.getGenericInstance(InventoryData.class);
        final boolean creative = player.getGameMode() == GameMode.CREATIVE && ((data.clickedSlotType == SlotType.QUICKBAR) || cc.openDisableCreative);
        final boolean isMerchant = player.getOpenInventory().getTopInventory().getType() == InventoryType.MERCHANT;
        final PlayerMoveData thisMove = mData.playerMoves.getCurrentMove();
        
        // Skipping conditions first.
        if (
            // This check relies on data set in SurvivalFly.
            !pData.isCheckActive(CheckType.MOVING_SURVIVALFLY, player)
            // Ignore duplicate packets
            || mData.lastMoveNoMove
            // Player is not moving (or moving extremely little)
            || TrigUtil.isSamePos(thisMove.from, thisMove.to)
            // can't check vehicles
            || player.isInsideVehicle()
            // Players are allowed to click in their inventory while moving with the elytra (but CANNOT look around)
            // (If rotations don't match do close the inventory)
            || Bridge1_9.isGlidingWithElytra(player) 
            && (thisMove.from.getYaw() == thisMove.to.getYaw() || thisMove.from.getPitch() == thisMove.to.getPitch())
            // Similar as above.
            || Bridge1_13.isRiptiding(player)
            // 1 second of leniency should be enough to rule out most false positives caused by friction
            // (Plus the single click delay it takes to register the inventory status)
            || InventoryUtil.hasOpenedInvRecently(player, 1000)
            // In creative or middle click
            || creative 
            // Ignore merchant inventories.
            || isMerchant 
            // Velocity is ignored altogether
            || mData.isVelocityJumpPhase() && !thisMove.from.inLiquid // Survivalfly sets velocity jump phase when in liquid, but we still want to ensure that players are not pressing the space bar.
            || mData.getOrUseVerticalVelocity(thisMove.yDistance) != null
            || mData.useHorizontalVelocity(thisMove.hDistance - mData.getHorizontalFreedom()) >= thisMove.hDistance - mData.getHorizontalFreedom()
            // Ignore entity pushing.
            || pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) && CollisionUtil.isCollidingWithEntities(player, true)) {
            return false;
        }
            
        
        // Actual detection
        final PlayerMoveData lastMove = mData.playerMoves.getFirstPastMove();
        final PlayerMoveData secondLastMove = mData.playerMoves.getSecondPastMove();
        final CombinedData cData = pData.getGenericInstance(CombinedData.class);
        if (
            // Can't sprint while in an inventory
            player.isSprinting() && !Bridge1_13.isSwimming(player) && !thisMove.from.inLiquid
            // Can't use items as well.
            || pData.isUsingItem()
            // Can't possibly look around with an open inventory.
            || (thisMove.from.getYaw() != thisMove.to.getYaw() || thisMove.from.getPitch() != thisMove.to.getPitch())
            // Can't swim also.
            || Bridge1_13.isSwimming(player) 
            && (
                // For whatever reason, Mojang decided that players should be allowed to still swim if the player is touching ground
                !thisMove.touchedGround && !lastMove.touchedGround 
                || (thisMove.hAllowedDistance >= lastMove.hAllowedDistance || MathUtil.almostEqual(thisMove.hAllowedDistance, lastMove.hAllowedDistance, 0.001))
            )
            // Can't have an open inventory while sneaking
            || player.isSneaking() 
            && (
                // 1.12 and below don't accessability settings to allow sneaking while also clicking in the inventory
                pData.getClientVersion().isOlderThan(ClientVersion.V_1_13)
                || thisMove.hAllowedDistance >= lastMove.hAllowedDistance || MathUtil.almostEqual(thisMove.hAllowedDistance, lastMove.hAllowedDistance, 0.001)
            )
            // Jumping
            || thisMove.canJump && thisMove.from.onGround && !thisMove.to.onGround
            && thisMove.yDistance > mData.liftOffEnvelope.getJumpGain(mData.jumpAmplifier, mData.lastStuckInBlockVertical) - Magic.GRAVITY_SPAN
            // Player has more horizontal speed than the liquid pushing speed (in other words, they are actively pressing a WASD key to move)
            //|| thisMove.from.inLiquid && thisMove.hDistance > from.getLiquidPushingVector(player, to.getX()-from.getX(), to.getZ()-from.getZ()).length()
            // Speed was maintained for two moves, assume actively moving.
            || thisMove.hAllowedDistance >= lastMove.hAllowedDistance && !thisMove.from.inLiquid
            ) {
            
            // Feed the improbable.
            if (cc.openImprobableWeight > 0.0) {
                Improbable.feed(player, cc.openImprobableWeight, System.currentTimeMillis());
            }
            return true;
        }
        // Player did slow-down on opening the inventory / is allowed to have the inventory open.
        return false;
    }
}
