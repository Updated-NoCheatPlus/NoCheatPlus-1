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
import fr.neatmonster.nocheatplus.checks.moving.util.AuxMoving;
import fr.neatmonster.nocheatplus.checks.moving.util.MovingUtil;
import fr.neatmonster.nocheatplus.penalties.IPenaltyList;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;

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
     *            the player
     * @return true, if successful
     */
    public boolean check(final Player player, final Location loc, final FightData data, final FightConfig cc, 
                         final IPlayerData pData, final IPenaltyList penaltyList) {

        boolean cancel = false;
        final List<String> tags = new ArrayList<String>();
        final double mcFallDistance = (double) player.getFallDistance();
        final MovingData mData = pData.getGenericInstance(MovingData.class);


        // Check if the hit was a critical hit (very small fall-distance, not on ladder, 
        //  not in liquid, not in vehicle, and without blindness effect).
        if (mcFallDistance > 0.0 && !player.isInsideVehicle() && !player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            
            // Might be a violation.
            final MovingConfig mcc = pData.getGenericInstance(MovingConfig.class);
            final PlayerMoveData thisMove = mData.playerMoves.getCurrentMove();

            if (pData.isDebugActive(type)) {
                debug(player, "y=" + loc.getY() + " mcfalldist=" + mcFallDistance 
                    + " jumpphase: " + mData.sfJumpPhase 
                    + " noFallDist: " + mData.noFallFallDistance + " lowjump: " + mData.sfLowJump 
                    + " toGround: " + thisMove.to.onGround + " fromGround: " + thisMove.from.onGround);
            }
        
            // Tags 
            /*
             * Attempt to look for silent jumps by comparing the players fallDistance and the fallDistance calculated by NoFall.
             * Only check if that fall distance is below/equal to the fall disatnce set in the config.
             * Use the jumpPhase to help determine if it was a silent jump as well
             * 
             * This method should reduce the false positives from the old check, by now focusing on silent jumps rather than just crit hits within a fall distance.
             * 
             */
            // 0.0009 is just a random number for leniency that works fine, maybe add a config option?
            if (Math.abs(mData.noFallFallDistance - mcFallDistance) > 0.0009 
                && mcFallDistance <= cc.criticalFallDistance 
                && mData.sfJumpPhase <= 1
                && !BlockProperties.isResetCond(player, loc, mcc.yOnGround)
                ){
               tags.add("silent_jump");
            }
            else if (mData.sfLowJump) {
                tags.add("low_jump");
            }
            else if (mData.noFallFallDistance != mcFallDistance && thisMove.from.onGround && thisMove.to.onGround) {
                tags.add("falldist_mismatch");
            }
                   
               
            if (!tags.isEmpty()){

                // TODO: Use past move tracking to check for SurvivalFly and the like?
                final PlayerMoveInfo moveInfo = auxMoving.usePlayerMoveInfo();
                moveInfo.set(player, loc, null, mcc.yOnGround);
                
                if (MovingUtil.shouldCheckSurvivalFly(player, moveInfo.from, mData, mcc, pData)) {
                    moveInfo.from.collectBlockFlags(0.4);
                    
                    // TODO: maybe these require a fix/modification with NoFall? For now, exempt the player.
                    // Don't think its possible to fake a crit in these situations either (except for being onGround in a web/water, which is checked for before being exempt) ... or at least from my testing?
                    if (thisMove.from.onClimbable || thisMove.to.onClimbable) {
                        // Ignore climbables
                    } 
                    else if ((thisMove.from.inLiquid | thisMove.to.inLiquid) && thisMove.from.onGround && thisMove.to.onGround) {
                        // Ignore liquids
                    } 
                    else if ((thisMove.from.inWeb | thisMove.to.inWeb) & !thisMove.to.onGround) {
                        // Ignore webs
                    } 
                    else if ((moveInfo.from.getBlockFlags() & BlockProperties.F_BOUNCE25) != 0 && !thisMove.from.onGround && !thisMove.to.onGround) {
                        // Slime blocks
                    }   
                    else {
                        boolean exemptLowJump = false;
                        // False positives with lowJump when the player jumps on/off a block while attacking an entity
                        if (mData.sfLowJump) {
                            if (Math.abs(mData.noFallFallDistance - mcFallDistance) < 0.0009) {
                                if (mcFallDistance > cc.criticalFallDistance) {
                                    if (!thisMove.to.onGround || !thisMove.from.onGround) {
                                        exemptLowJump = true;
                                    }
                                }
                            }
                            else if (!thisMove.to.onGround || !thisMove.from.onGround) {
                                if (Math.abs(mData.noFallFallDistance - mcFallDistance) > 0.0009) {
                                    exemptLowJump = true;
                                }
                            }
                        }
                        if (!exemptLowJump) {
                            data.criticalVL += 1.0;
                            // Execute whatever actions are associated with this check and 
                            //  the violation level and find out if we should cancel the event.
                            final ViolationData vd = new ViolationData(this, player, data.criticalVL, 1.0, cc.criticalActions);
                            if (vd.needsParameters()) vd.setParameter(ParameterName.TAGS, StringUtil.join(tags, "+"));
                            cancel = executeActions(vd).willCancel();
                            // TODO: Introduce penalty instead of cancel.
                        }
                    }
                    auxMoving.returnPlayerMoveInfo(moveInfo);
                }
            }
            if (!cancel) {
                data.criticalVL *= 0.96D;
            }
        }
        return cancel;
    }
}
