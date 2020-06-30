package fr.neatmonster.nocheatplus.checks.inventory;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.StringUtil;

/**
 * A simple check to prevent players from interacting with their inventory if they shouldn't be allowed to.
 */

public class InventoryMove extends Check {
	
	public InventoryMove() {
	    super(CheckType.INVENTORY_INVENTORYMOVE);
    }
	
    public boolean check(final Player player, final InventoryData data, final IPlayerData pData, final InventoryConfig cc, final SlotType type) {

    final MovingData mData = pData.getGenericInstance(MovingData.class);
    final PlayerMoveData thisMove = mData.playerMoves.getCurrentMove();
    final boolean isOnGround = thisMove.touchedGround || thisMove.from.onGround || thisMove.to.onGround;
    final boolean creative = player.getGameMode() == GameMode.CREATIVE && ((type == SlotType.QUICKBAR) || cc.invMoveDisableCreative);
    List<String> tags = new LinkedList<String>();
    boolean cancel = false;
    
    // Tags 
    // TODO: Not remove, still left as observation level 
    //if (player.isBlocking() && !(player.getOpenInventory().getTopInventory().getType() == InventoryType.MERCHANT)) {
    //	tags.add("isBlocking");
    //}
    if (player.isSneaking()) {
    	tags.add("isSneaking");
    }
    else if (Bridge1_13.isSwimming(player)) {	
        if (!isOnGround) tags.add("isSwimming");	
    }
    // Remove? Seem bugged like isBlocking but such a rarer case
    else if (player.isSprinting() && !tags.contains("isSwimming")) {
    	tags.add("isSprinting");
    }
    else if (player.isDead()) {
    	tags.add("isDead");
    }
    
     // Do the actual check 
     if (!tags.isEmpty() && !creative) {
         // Violation
         data.invMoveVL += 1D;
         final ViolationData vd = new ViolationData(this, player, data.invMoveVL, 1, pData.getGenericInstance(InventoryConfig.class).invMoveActionList);
         if (vd.needsParameters()) vd.setParameter(ParameterName.TAGS, StringUtil.join(tags, "+"));
         cancel = executeActions(vd).willCancel();
      }

     /*
      * Check if the player is actively moving while clicking in their inventory.
      * Need to have certain conditions to make sure the player is not moving from past events (Ice, Water flow)
      */
     if (!cancel && !creative && isOnGround && ((System.currentTimeMillis() - data.lastMoveEvent) < 65)
             && !player.isInsideVehicle() && (mData.sfOnIce == 0) && !thisMove.from.inLiquid) {
         final PlayerMoveData lastMove = mData.playerMoves.getFirstPastMove();

         if (Math.abs(thisMove.hDistance - lastMove.hDistance) < cc.invMoveHdistLeniency && thisMove.hDistance > cc.invMoveHdistMin) {
             data.invMoveVL += 1D;
             final ViolationData vd = new ViolationData(this, player, data.invMoveVL, 1, pData.getGenericInstance(InventoryConfig.class).invMoveActionList);
             tags.add("isMoving");
             if (vd.needsParameters()) vd.setParameter(ParameterName.TAGS, StringUtil.join(tags, "+"));

             cancel = executeActions(vd).willCancel();
         }

     }

     return cancel;
   }
 }
