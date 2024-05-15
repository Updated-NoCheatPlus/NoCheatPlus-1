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
import fr.neatmonster.nocheatplus.checks.combined.Improbable;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.components.registry.event.IHandle;
import fr.neatmonster.nocheatplus.components.registry.feature.IDisableListener;
import fr.neatmonster.nocheatplus.hooks.ExemptionSettings;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.InventoryUtil;
import fr.neatmonster.nocheatplus.utilities.collision.CollisionUtil;

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
     * 
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
     * Enforce a closed inventory on this generic event/action (without checking for any specific side condition) <br>
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
     * To be called on PlayerMoveEvents; pre-requsite is InventoryUtil.hasAnyInventoryOpen having returned true. <br>
     * (Against InventoryMove cheats and similar)<br>
     * 
     * @param player
     * @param pData
     * @return True, if the open inventory needs to be closed during this movement.
     */
    public boolean checkOnMove(final Player player, final IPlayerData pData) {
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
            // Can't check vehicles
            || player.isInsideVehicle()
            // In creative or middle click
            || creative 
            // Ignore merchant inventories.
            || isMerchant 
            // Velocity is ignored altogether
            || mData.getOrUseVerticalVelocity(thisMove.yDistance) != null
            || mData.useHorizontalVelocity(thisMove.hDistance - mData.getHorizontalFreedom()) >= thisMove.hDistance - mData.getHorizontalFreedom()
            // Ignore entity pushing.
            || pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) && CollisionUtil.isCollidingWithEntities(player, 0.5, 0.1, 0.5, true)) {
            return false;
        }
            
        // Actual detection
        if (thisMove.hasImpulse || Bridge1_9.isGlidingWithElytra(player) && (thisMove.from.getYaw() != thisMove.to.getYaw() || thisMove.from.getPitch() != thisMove.to.getPitch())) {
            // Use SurvivalFly prediction handling to see if the player is actively moving.
            // WASD keys presses are irrelevant when gliding but the telltale sign that let's you know that the player is moving in their inventory is if roations don't match
            if (cc.openImprobableWeight > 0.0) {
                Improbable.feed(player, cc.openImprobableWeight, System.currentTimeMillis());
            }
            return true;
        }
        return false;
    }
}
