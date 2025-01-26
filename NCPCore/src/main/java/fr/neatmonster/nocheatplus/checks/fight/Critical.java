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
package fr.neatmonster.nocheatplus.checks.fight;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveInfo;
import fr.neatmonster.nocheatplus.checks.moving.velocity.VelocityFlags;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.penalties.IPenaltyList;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.moving.AuxMoving;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;

/**
 * A check used to verify that critical hits done by players are legit.
 */
public class Critical extends Check {
    
    private final AuxMoving auxMoving = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(AuxMoving.class);
    
    /**
     * Instantiates a new critical check.
     */
    public Critical() {
        super(CheckType.FIGHT_CRITICAL);
    }
    
    /**
     * Checks a player.
     * 
     * @param player
     * @param loc
     * @param data
     * @param cc
     * @param pData
     * @param penaltyList
     * @return true, if successful
     */
    public boolean check(final Player player, final Location loc, final FightData data, final FightConfig cc, 
                         final IPlayerData pData, final IPenaltyList penaltyList) {
        boolean cancel = false;
        final List<String> tags = new ArrayList<String>();
        final MovingData mData = pData.getGenericInstance(MovingData.class);
        final MovingConfig mCC = pData.getGenericInstance(MovingConfig.class);
        final PlayerMoveData thisMove = mData.playerMoves.getCurrentMove();
        final PlayerMoveData lastMove = mData.playerMoves.getFirstPastMove();
        final double mcFallDistance = (double) player.getFallDistance();
        final double ncpFallDistance = mData.noFallFallDistance;
        final double realisticFallDistance = MovingUtil.getRealisticFallDistance(player, thisMove.from.getY(), thisMove.to.getY(), mData, pData);
        final PlayerMoveInfo moveInfo = auxMoving.usePlayerMoveInfo();
        moveInfo.set(player, loc, null, mCC.yOnGround);
        
        // Check if the hit was a critical hit (fall distance is present, player is not on a ladder, not in vehicle, and without blindness effect).
        if (mcFallDistance > 0.0 && !player.isInsideVehicle() && !player.hasPotionEffect(PotionEffectType.BLINDNESS)) {

            if (pData.isDebugActive(type)) {
                debug(player, 
                    "Fall distances: MC(" + StringUtil.fdec3.format(mcFallDistance) +") | NCP("+ StringUtil.fdec3.format(ncpFallDistance) +") | R("+ StringUtil.fdec3.format(realisticFallDistance) +")"
                    + "\nfD diff: " + StringUtil.fdec3.format(Math.abs(ncpFallDistance - mcFallDistance))
                    + "\nJumpPhase: " + mData.sfJumpPhase + " | NCP onGround: " + (thisMove.from.onGround ? "ground -> " : "--- -> ") + (thisMove.to.onGround ? "ground" : "---") + " | MC onGround: " + player.isOnGround()
                ); // + ", packet onGround: " + packet.onGround); 
            }
            
            // Check for skipping conditions first.
            moveInfo.from.collectBlockFlags(0.4);
            // False positives with medium counts reset all nofall data when nearby boat
            // TODO: Fix isOnGroundDueToStandingOnAnEntity() to work on entity not nearby
            if (moveInfo.from.isOnGroundDueToStandingOnAnEntity()
                // Edge case with slime blocks
                || (moveInfo.from.getBlockFlags() & BlockFlags.F_BOUNCE25) != 0 && !moveInfo.from.isOnGround() && !moveInfo.to.isOnGround()) {
                auxMoving.returnPlayerMoveInfo(moveInfo);
                return false;
            }
            
            boolean isIllegal =
                       // 0: Don't allow players to perform critical hits in blocks where the game would reset fall distance (water, powder snow, bushes, webs, climbables)
                       moveInfo.from.isResetCond()
                       // 0: Same as above. The game resets fall distance with slowfall
                       || !Double.isInfinite(Bridge1_13.getSlowfallingAmplifier(player))
                       // 0: A full jump from ground requires more than 6 phases/events.
                       || mData.sfJumpPhase > 0 && mData.sfJumpPhase <= mData.liftOffEnvelope.getMaxJumpPhase(mData.jumpAmplifier) 
                       && !moveInfo.from.seekCollisionAbove(0.2) 
                       && (lastMove.verVelUsed == null || !lastMove.verVelUsed.get(0).hasFlag(VelocityFlags.ORIGIN_BLOCK_BOUNCE))
                       // 0: Always invalidate critical hits if we judge the player to be on ground (given enough fall distance)
                       || Math.abs(ncpFallDistance - mcFallDistance) > 1e-5 && (moveInfo.from.isOnGround() || lastMove.touchedGroundWorkaround)
                       // (Let SurvivalFly catch low-jumps).
            ;

            // Handle violations
            if (isIllegal) {
                data.criticalVL += 1.0;
                // Execute whatever actions are associated with this check and 
                //  the violation level and find out if we should cancel the event.
                final ViolationData vd = new ViolationData(this, player, data.criticalVL, 1.0, cc.criticalActions);
                if (vd.needsParameters()) vd.setParameter(ParameterName.TAGS, StringUtil.join(tags, "+"));
                cancel = executeActions(vd).willCancel();
                // TODO: Introduce penalty instead of cancel.
            }
            // Crit was legit, reward the player.
            else data.criticalVL *= 0.96D;
        }
        auxMoving.returnPlayerMoveInfo(moveInfo);
        return cancel;
    }
}