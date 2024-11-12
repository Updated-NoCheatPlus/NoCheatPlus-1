package fr.neatmonster.nocheatplus.checks.moving;

import java.util.Collection;

import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.players.IPlayerData;

/**
 * A class to encapsulate legacy checks/calculations/methods for SurvivalFly.
 */
public class SfLegacy {
    
    /**
     * A legacy/auxiliary v-dist-rel check.
     *
     * @param tags
     * @param now
     * @param player
     * @param yDistance
     * @param lastMove
     * @param data
     * @param pData
     * @param fullyInAir
     * @param yDirectionSwitch
     * @param thisMove
     * @return True, in case the player changes their y direction while falling without having touched the ground first.
     */
    public static boolean checkOnChangeOfYDirection(Collection<String> tags, long now, Player player, double yDistance, PlayerMoveData lastMove, MovingData data, IPlayerData pData, boolean fullyInAir, boolean yDirectionSwitch, PlayerMoveData thisMove) {
        boolean violation = false;
        if (fullyInAir && yDirectionSwitch && !(data.timeRiptiding + 2000 > now)) {
            if (yDistance > 0.0) {
                // TODO: Demand consuming queued velocity for valid change (!).
                if (lastMove.touchedGround || lastMove.to.extraPropertiesValid && lastMove.to.resetCond) {
                    // Change to increasing phase
                    tags.add("y_switch_inc");
                } else {
                    // Moving upwards after falling without having touched the ground first
                    if (data.jumpDelay < 9 && !((lastMove.touchedGround || lastMove.from.onGroundOrResetCond) && lastMove.yDistance == 0D)
                            && data.getOrUseVerticalVelocity(yDistance) == null && !thisMove.hasLevitation) {
                        violation = true;
                        tags.add("airjump");
                    } else tags.add("y_inair_switch");
                }
            } else {
                // Change to decreasing phase.
                tags.add("y_switch_dec");
                // (Previously: a low-jump check would have been performed here, let vDistRel catch it instead.)
            }
        }
        return violation;
    }
    
//    /**
//     * The horizontal accounting subcheck:
//     * It monitors average combined-medium (e.g. air+ground or air+water) speed, with a rather simple bucket(s)-overflow mechanism.
//     * We feed 1.0 whenever we're below the allowed BASE speed, and (actual / base) if we're above. 
//     * (hAllowedDistanceBase is about what a player can run at without using special techniques like extra jumping, 
//     * not necessarily the finally allowed speed).
//     *
//     * @return hDistanceAboveLimit
//     */
//    private double horizontalAccounting(final MovingData data, double hDistance, double hDistanceAboveLimit, final PlayerMoveData thisMove, final PlayerLocation from) {
//        
//        /** Final combined-medium horizontal value */
//        final double fcmhv = Math.max(1.0, Math.min(10.0, thisMove.hDistance / thisMove.hAllowedDistanceBase));
//        final boolean movingBackwards = TrigUtil.isMovingBackwards(thisMove.to.getX()-thisMove.from.getX(), thisMove.to.getZ()-thisMove.from.getZ(), LocUtil.correctYaw(from.getYaw()));
//        // With each horizontal move, add the calculated value to the bucket.
//        data.hDistAcc.add((float) fcmhv);
//        
//        // We have enough events.
//        if (data.hDistAcc.count() > 30) {
//            
//            // Get the ratio between: accumulated value / total events counted 
//            // (Currently, only 1 bucket is present, so it shouldn't matter using score instead of bucketScore)
//            final double accValue = (double) data.hDistAcc.score() / data.hDistAcc.count();
//            final double limit;
//            if (data.liftOffEnvelope == LiftOffEnvelope.NORMAL) {
//                limit = from.isInWaterLogged() ? 1.1 : movingBackwards ? 1.15 : 1.34;
//            }
//            else if (data.liftOffEnvelope == LiftOffEnvelope.LIMIT_LIQUID
//                    || data.liftOffEnvelope == LiftOffEnvelope.LIMIT_NEAR_GROUND) {
//                // 1.8.8 in-water moves with jumping near/on surface. 1.2 is max factor for one move (!).
//                limit = ServerIsAtLeast1_10 ? 1.05 : 1.1;
//            }
//            else if (data.liftOffEnvelope == LiftOffEnvelope.POWDER_SNOW) {
//                limit = 1.047;
//            }
//            else if (data.liftOffEnvelope == LiftOffEnvelope.BERRY_JUMP) {
//                limit = 1.057;
//            }
//            else limit = 1.0;
//            
//            // Violation
//            if (accValue > limit) {
//                hDistanceAboveLimit = Math.max(hDistanceAboveLimit, hDistance);
//                bufferUse = false;
//                tags.add("hacc("+ StringUtil.fdec3.format(accValue) +"/" + limit +")");
//                // Reset for now.
//                data.clearHAccounting();
//            }
//            else {
//                // Clear and add
//                data.clearHAccounting();
//                data.hDistAcc.add((float) fcmhv);
//            }
//        }
//        return hDistanceAboveLimit;
//    }
    
    
//    /**
//     * Demand that with time the values decrease.<br>
//     * The ActionAccumulator instance must have 3 buckets, bucket 1 is checked against
//     * bucket 2, 0 is ignored. [Vertical accounting: applies to both falling and jumping]<br>
//     * NOTE: This just checks and adds to tags, no change to acc.
//     *
//     * @param yDistance
//     * @param acc
//     * @param tags
//     * @param tag Tag to be added in case of a violation of this sub-check.
//     * @return A violation value > 0.001, to be interpreted like a moving violation.
//     */
//    private static final double verticalAccounting(final double yDistance,
//                                                   final ActionAccumulator acc, final ArrayList<String> tags,
//                                                   final String tag) {
//        
//        final int count0 = acc.bucketCount(0);
//        if (count0 > 0) {
//            final int count1 = acc.bucketCount(1);
//            if (count1 > 0) {
//                final int cap = acc.bucketCapacity();
//                final float sc0;
//                sc0 = (count0 == cap) ? acc.bucketScore(0) :
//                        // Catch extreme changes quick.
//                        acc.bucketScore(0) * (float) cap / (float) count0 - Magic.GRAVITY_VACC * (float) (cap - count0);
//                final float sc1 = acc.bucketScore(1);
//                if (sc0 > sc1 - 3.0 * Magic.GRAVITY_VACC) {
//                    // TODO: Velocity downwards fails here !!!
//                    if (yDistance <= -1.05 && sc1 < -8.0 && sc0 < -8.0) {
//                        // High falling speeds may pass.
//                        tags.add(tag + "grace");
//                        return 0.0;
//                    }
//                    tags.add(tag);
//                    return sc0 - (sc1 - 3.0 * Magic.GRAVITY_VACC);
//                }
//            }
//        }
//        return 0.0;
//    }
}