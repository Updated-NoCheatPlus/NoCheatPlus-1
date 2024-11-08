package fr.neatmonster.nocheatplus.checks.moving;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.combined.Improbable;
import fr.neatmonster.nocheatplus.checks.moving.envelope.workaround.MagicWorkarounds;
import fr.neatmonster.nocheatplus.checks.moving.model.InputDirection;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.compat.AlmostBoolean;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.components.modifier.IAttributeAccess;
import fr.neatmonster.nocheatplus.components.registry.event.IGenericInstanceHandle;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.collision.CollisionUtil;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.moving.Magic;

/**
 * A class to encapsulate legacy checks/calculations/methods for SurvivalFly.
 */
public class SfLegacy {
    
    /**
     * When in a liquid, the game disregards ground status, and only checks if the player presses the space bar.
     * If pressed, the game sets the "jumping" field to true, but the client doesn't report this impulse until version 1.21.2.
     * <p>Due to this, we must brute-force speed here, by calculating both ascent (when pressing space) and descent speed (without pressing space), and see which one matches the current speed of the player</p>
     * 
     * @param attributeAccess
     * @param justUsedWorkarounds
     * @param tags
     * @param type
     * @param thisMove
     * @param from
     * @param to
     * @param data
     * @param player
     * @param pData
     * @param lastMove
     * @param yDirectionSwitch
     * @param fullyInAir
     * @param fromOnGround
     * @param toOnGround
     * @param isNormalOrPacketSplitMove
     * @param yDistanceAboveLimit
     */
    public static double loopLiquidsAcceleration(IGenericInstanceHandle<IAttributeAccess> attributeAccess, ArrayList<String> justUsedWorkarounds, ArrayList<String> tags, CheckType type, final PlayerMoveData thisMove, final PlayerLocation from, final PlayerLocation to,
                                               final MovingData data, final Player player, final IPlayerData pData, final PlayerMoveData lastMove, boolean yDirectionSwitch, boolean fullyInAir,
                                               boolean fromOnGround, boolean toOnGround, boolean isNormalOrPacketSplitMove, double yDistanceAboveLimit, final MovingConfig cc) {
        /** 0: With space bar pressed. 1: with space bar not pressed 2: swimming not applied at all*/
        double[] yTheoreticalDistance = new double[3];
        boolean[] collideLiquidY = new boolean[3];
        // Initialize with the momentum that has hitherto been calculated.
        yTheoreticalDistance[0] = thisMove.yAllowedDistance;
        yTheoreticalDistance[1] = thisMove.yAllowedDistance;
        yTheoreticalDistance[2] = thisMove.yAllowedDistance;
        boolean isSubmergedInWater = from.isInWater() && thisMove.submergedWaterHeight > 0.0;
        double fluidJumpThreshold = from.getEyeHeight() < 0.4D ? 0.0D : 0.4D;
        if (isSubmergedInWater && (!from.isOnGround() || thisMove.submergedWaterHeight > fluidJumpThreshold)) {
            yTheoreticalDistance[0] += Magic.LIQUID_SPEED_GAIN; 
        } 
        else if (from.isInLava() && (!from.isOnGround() || thisMove.submergedLavaHeight > fluidJumpThreshold)) {
            yTheoreticalDistance[0] += Magic.LIQUID_SPEED_GAIN;
        } 
        else if ((from.isOnGround() || isSubmergedInWater && thisMove.submergedWaterHeight <= fluidJumpThreshold) && data.jumpDelay == 0) {
            yTheoreticalDistance[0] = data.liftOffEnvelope.getJumpGain(data.jumpAmplifier) * attributeAccess.getHandle().getJumpGainMultiplier(player);
            data.jumpDelay = Magic.MAX_JUMP_DELAY;
            thisMove.hasImpulse = AlmostBoolean.YES; 
        }
        if (BridgeMisc.hasGravity(player) && pData.getClientVersion().isLowerThan(ClientVersion.V_1_13)) {
            yTheoreticalDistance[0] -= Magic.LEGACY_LIQUID_GRAVITY;
            yTheoreticalDistance[1] -= Magic.LEGACY_LIQUID_GRAVITY;
        }
        
        if (thisMove.isSwimming && !player.isInsideVehicle()) { 
            Vector lookVector = TrigUtil.getLookingDirection(to, player);
            double swimmingScalar = lookVector.getY() < -0.2 ? 0.085 : 0.06;
            // Note: Since thisMove.isJump is always false because not been set yet, make these conditions unusable, result in brute force
            //if (lookVector.getY() <= 0.0 || thisMove.isJump 
            //    || BlockProperties.getLiquidHeightAt(from.getBlockCache(), Location.locToBlock(from.getX()), Location.locToBlock(from.getY()+1.0-0.1), Location.locToBlock(from.getZ()), BlockFlags.F_WATER, true) != 0.0) {
            yTheoreticalDistance[0] += (lookVector.getY() - yTheoreticalDistance[0]) * swimmingScalar;
            yTheoreticalDistance[1] += (lookVector.getY() - yTheoreticalDistance[1]) * swimmingScalar;
            //}
        }
        
        if (TrigUtil.lengthSquared(data.nextStuckInBlockHorizontal, data.nextStuckInBlockVertical, data.nextStuckInBlockHorizontal) > 1.0E-7) {
            if (from.isInLiquid()) {
                for (int i = 0; i < yTheoreticalDistance.length; i++) {
                    yTheoreticalDistance[i] *= data.nextStuckInBlockVertical;
                }
            } 
            else thisMove.yAllowedDistance *= data.nextStuckInBlockVertical;
        }
        
        // TODO: Needs to be adjusted for on ground pushing
        if (lastMove.slowedByUsingAnItem && !thisMove.slowedByUsingAnItem && thisMove.isRiptiding) {
            final Vector riptideForce = to.getTridentPropellingForce(lastMove.collideY);
            if (from.isInLiquid()) {
                for (int i = 0; i < yTheoreticalDistance.length; i++) {
                    yTheoreticalDistance[i] += riptideForce.getY();
                }
            } 
            else thisMove.yAllowedDistance += riptideForce.getY();
            player.sendMessage("Trident propel(v): " + StringUtil.fdec6.format(thisMove.yDistance) + " / " + StringUtil.fdec6.format(thisMove.yAllowedDistance));
        }
        
        if (from.isInLiquid()) {
            for (int i = 0; i < yTheoreticalDistance.length; i++) {
                Vector collisionVector = from.collide(new Vector(thisMove.xAllowedDistance, yTheoreticalDistance[i], thisMove.zAllowedDistance), fromOnGround || thisMove.touchedGroundWorkaround, cc, from.getAABBCopy());
                if (yTheoreticalDistance[i] != collisionVector.getY()) {
                    // This theoretical speed would result in a collision. Remember it.
                    collideLiquidY[i] = true;
                }
                yTheoreticalDistance[i] = collisionVector.getY();
                thisMove.headObstructed = yTheoreticalDistance[i] != collisionVector.getY() && thisMove.yDistance >= 0.0 && from.seekCollisionAbove() && !fromOnGround;
            }
        } 
        else {
            Vector collisionVector = from.collide(new Vector(thisMove.xAllowedDistance, thisMove.yAllowedDistance, thisMove.zAllowedDistance), fromOnGround || thisMove.touchedGroundWorkaround, cc, from.getAABBCopy());
            thisMove.headObstructed = thisMove.yAllowedDistance != collisionVector.getY() && thisMove.yDistance >= 0.0 && from.seekCollisionAbove() && !fromOnGround;  // New definition of head obstruction: yDistance is checked because Minecraft considers players to be on ground when motion is explicitly negative
            // TODO: Is the gravity-reiteration fix needed for liquids?
            if (lastMove.headObstructed && !thisMove.headObstructed && yDirectionSwitch && thisMove.yDistance <= 0.0 && fullyInAir) {
                thisMove.yAllowedDistance = 0.0; 
                if (BridgeMisc.hasGravity(player)) {
                    thisMove.yAllowedDistance -= data.lastGravity;
                }
                thisMove.yAllowedDistance *= data.lastFrictionVertical;
                tags.add("gravity_reiterate");
            } 
            else thisMove.yAllowedDistance = collisionVector.getY();
            thisMove.collideY = collisionVector.getY() != thisMove.yAllowedDistance;
        }
        
        
        ////////////////////////////////////////////////////////////////////////////
        // Calculate the offset: check for velocity and workarounds on violations // 
        ////////////////////////////////////////////////////////////////////////////
        if (from.isInLiquid()) {
            for (int i = 0; i < yTheoreticalDistance.length; i++) {
                if (MathUtil.almostEqual(thisMove.yDistance, yTheoreticalDistance[i], Magic.PREDICTION_EPSILON)) {
                    thisMove.yAllowedDistance = yTheoreticalDistance[i];
                    thisMove.collideY = collideLiquidY[i];
                    thisMove.isJump = yTheoreticalDistance[0] == thisMove.yAllowedDistance;
                    break;
                }
            }
        }
        final double offset = thisMove.yDistance - thisMove.yAllowedDistance;
        if (Math.abs(offset) < Magic.PREDICTION_EPSILON) {
            // Accuracy margin.
        } 
        else {
            if (MagicWorkarounds.checkPostPredictWorkaround(data, fromOnGround, toOnGround, from, to, thisMove.yAllowedDistance, player, isNormalOrPacketSplitMove)) {
                thisMove.yAllowedDistance = thisMove.yDistance;
                if (pData.isDebugActive(type)) {
                    player.sendMessage("Workaround ID: " + (!justUsedWorkarounds.isEmpty() ? StringUtil.join(justUsedWorkarounds, " , ") : ""));
                }
            } 
            else if (data.getOrUseVerticalVelocity(thisMove.yDistance) == null) {
                yDistanceAboveLimit = Math.max(yDistanceAboveLimit, Math.abs(offset));
                tags.add("vdistrel");
            }
        }
        return yDistanceAboveLimit;
    }
    
    /**
     * When a WASD key is pressed, the game transforms the impulse into an acceleration vector.
     * This makes horizontal speed calculations rely on impulses.
     * However, until MC 1.21.2, players did not send any information about WASD key presses to the server,
     * requiring speed predictions to loop through all possible key press combinations.
     *<p>This is what gets done here.</p>
     * 
     * @param inputs
     * @param i
     * @param isPredictable
     * @param onGround
     * @param movementSpeed
     * @param thisMove
     * @param lastMove
     * @param from
     * @param debug
     * @param to
     * @param data
     * @param pData
     * @param player
     */
    public static void loopAccelerationAndModifiers(BlockChangeTracker blockChangeTracker, InputDirection[] inputs, AtomicInteger i, boolean isPredictable, boolean onGround, double movementSpeed,
                                                    final PlayerMoveData thisMove, final PlayerMoveData lastMove, PlayerLocation from, boolean debug,
                                                    final PlayerLocation to, final MovingData data, final IPlayerData pData, final Player player, Collection<String> tags) {
        // Only comments added are specifically tied to the looping mechanic
        float sinYaw = TrigUtil.sin(to.getYaw() * TrigUtil.toRadians);
        float cosYaw = TrigUtil.cos(to.getYaw() * TrigUtil.toRadians);
        /* List of predicted X distances. Size is the number of possible inputs (left/right/backwards/forward etc...) */
        double[] xTheoreticalDistance = new double[9];
        /* To keep track which theoretical speed would result in a collision on the X axis */
        boolean[] collideX = new boolean[9];
        /* List of predicted Z distances. Size is the number of possible inputs (left/right/backwards/forward etc...) */
        double[] zTheoreticalDistance = new double[9];
        /* To keep track which theoretical speed would result in a collision on the Z axis */
        boolean[] collideZ = new boolean[9];
        for (i.set(0); i.intValue() < 9; i.incrementAndGet()) {
            // Each slot in the array is initialized with the same momentum first.
            xTheoreticalDistance[i.intValue()] = thisMove.xAllowedDistance;
            zTheoreticalDistance[i.intValue()] = thisMove.zAllowedDistance;
            // Then we proceed to compute all possible accelerations with all theoretical inputs.
            double inputSq = MathUtil.square((double)inputs[i.intValue()].getStrafe()) + MathUtil.square((double)inputs[i.intValue()].getForward()); // Cast to a double because the client does it
            if (inputSq >= 1.0E-7) {
                if (inputSq > 1.0) {
                    double inputForce = Math.sqrt(inputSq);
                    if (inputForce < 1.0E-4) {
                        inputs[i.intValue()].operationToInt(0, 0, 0);
                    }
                    else {
                        inputs[i.intValue()].operationToInt(inputForce, inputForce, 2);
                    }
                }
                // Multiply all inputs by movement speed.
                inputs[i.intValue()].operationToInt(movementSpeed, movementSpeed, 1);
                // The acceleration vector is added to each momentum.
                xTheoreticalDistance[i.intValue()] += inputs[i.intValue()].getStrafe() * (double)cosYaw - inputs[i.intValue()].getForward() * (double)sinYaw;
                zTheoreticalDistance[i.intValue()] += inputs[i.intValue()].getForward() * (double)cosYaw + inputs[i.intValue()].getStrafe() * (double)sinYaw;
            }
        }
        if (from.isOnClimbable() && !from.isInLiquid()) {
            data.clearActiveHorVel();
            for (i.set(0); i.intValue() < 9; i.incrementAndGet()) {
                xTheoreticalDistance[i.intValue()] = MathUtil.clamp(xTheoreticalDistance[i.intValue()], -Magic.CLIMBABLE_MAX_SPEED, Magic.CLIMBABLE_MAX_SPEED);
                zTheoreticalDistance[i.intValue()] = MathUtil.clamp(zTheoreticalDistance[i.intValue()], -Magic.CLIMBABLE_MAX_SPEED, Magic.CLIMBABLE_MAX_SPEED);
            }
        }
        if (TrigUtil.lengthSquared(data.nextStuckInBlockHorizontal, data.nextStuckInBlockVertical, data.nextStuckInBlockHorizontal) > 1.0E-7) {
            for (i.set(0); i.intValue() < 9; i.incrementAndGet()) {
                xTheoreticalDistance[i.intValue()] *= (double) data.nextStuckInBlockHorizontal;
                zTheoreticalDistance[i.intValue()] *= (double) data.nextStuckInBlockHorizontal;
            }
        }
        if (lastMove.slowedByUsingAnItem && !thisMove.slowedByUsingAnItem && thisMove.isRiptiding) {
            Vector propellingForce = to.getTridentPropellingForce(lastMove.touchedGround);
            for (i.set(0); i.intValue() < 9; i.incrementAndGet()) {
                xTheoreticalDistance[i.intValue()] += propellingForce.getX();
                zTheoreticalDistance[i.intValue()] += propellingForce.getZ();
            }
        }
        if (!player.isFlying() && pData.isShiftKeyPressed() && from.isAboveGround() && thisMove.yDistance <= 0.0) {
            for (i.set(0); i.intValue() < 9; i.incrementAndGet()) {
                // TODO: Optimize. Brute forcing collisions with all 9 speed combinations will tank performance.
                Vector backOff = from.maybeBackOffFromEdge(new Vector(xTheoreticalDistance[i.intValue()], thisMove.yDistance, zTheoreticalDistance[i.intValue()]));
                xTheoreticalDistance[i.intValue()] = backOff.getX();
                zTheoreticalDistance[i.intValue()] = backOff.getZ();
            }
        }
        // TODO: Optimize. Brute forcing collisions with all 9 speed combinations will tank performance.
        for (i.set(0); i.intValue() < 9; i.incrementAndGet()) {
            Vector collisionVector = from.collide(new Vector(xTheoreticalDistance[i.intValue()], thisMove.yDistance, zTheoreticalDistance[i.intValue()]), onGround, pData.getGenericInstance(MovingConfig.class), from.getAABBCopy());
            if (xTheoreticalDistance[i.intValue()] != collisionVector.getX()) {
                // This theoretical speed would result in a collision. Remember it.
                collideX[i.intValue()] = true;
            }
            if (zTheoreticalDistance[i.intValue()] != collisionVector.getZ()) {
                // This theoretical speed would result in a collision. Remember it.
                collideZ[i.intValue()] = true;
            }
            xTheoreticalDistance[i.intValue()] = collisionVector.getX();
            zTheoreticalDistance[i.intValue()] = collisionVector.getZ();
        }
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        if (cc.trackBlockMove) {
            for (i.set(0); i.intValue() < 9; i.getAndIncrement()) {
                if (from.matchBlockChange(blockChangeTracker, data.blockChangeRef, xTheoreticalDistance[i.intValue()] < 0.0 ? BlockChangeTracker.Direction.X_NEG : BlockChangeTracker.Direction.X_POS, 0.05)) {
                    xTheoreticalDistance[i.intValue()] = thisMove.xDistance;
                }
            }
            for (i.set(0); i.intValue() < 9; i.getAndIncrement()) {
                if (from.matchBlockChange(blockChangeTracker, data.blockChangeRef, zTheoreticalDistance[i.intValue()] < 0.0 ? BlockChangeTracker.Direction.Z_NEG : BlockChangeTracker.Direction.Z_POS, 0.05)) {
                    zTheoreticalDistance[i.intValue()] = thisMove.zDistance;
                }
            }
        }
        
        
        /////////////////////////////////////////////////////////////////////////////
        // Determine which (and IF) theoretical speed should be set in this move   //
        /////////////////////////////////////////////////////////////////////////////
        /*
          True, if the offset between predicted and actual speed is smaller than the accuracy margin (0.0001).
         */
        boolean found;
        /*
          True will check if BOTH axis have an offset smaller than 0.0001 (against strafe-like cheats and anything of that sort that relies on the specific direction of the move).
          Otherwise, only the combined horizontal distance will be checked against the offset.
         */
        boolean strict = cc.survivalFlyStrictHorizontal;
        for (i.set(0); i.intValue() < 9; i.getAndIncrement()) {
            if (strict) {
                found = MathUtil.isOffsetWithinPredictionEpsilon(thisMove.xDistance, xTheoreticalDistance[i.intValue()]) && MathUtil.isOffsetWithinPredictionEpsilon(thisMove.zDistance, zTheoreticalDistance[i.intValue()]);
            }
            else {
                double theoreticalHDistance = MathUtil.dist(xTheoreticalDistance[i.intValue()], zTheoreticalDistance[i.intValue()]);
                found = MathUtil.isOffsetWithinPredictionEpsilon(thisMove.hDistance, theoreticalHDistance);
            }
            
            boolean forceViolation = false;
            if (found) {
                // These checks must be performed ex-post because they rely on direction data that is set after the prediction.
                if (pData.isSprinting() 
                    && (
                        (inputs[i.intValue()].getStrafeDir().equals(InputDirection.StrafeDirection.LEFT) || inputs[i.intValue()].getStrafeDir().equals(InputDirection.StrafeDirection.RIGHT) || inputs[i.intValue()].getForwardDir().equals(InputDirection.ForwardDirection.BACKWARD)) 
                        && !inputs[i.intValue()].getForwardDir().equals(InputDirection.ForwardDirection.FORWARD) 
                        || player.getFoodLevel() <= 5
                    )) {
                    // || inputs[i].getForward() < 0.8 // hasEnoughImpulseToStartSprinting, in LocalPlayer,java -> aiStep()
                    tags.add("illegalsprint");
                    pData.setSprintingState(false);
                    Improbable.check(player, (float) thisMove.hDistance, System.currentTimeMillis(), "moving.survivalfly.illegalsprint", pData);
                    // Keep looping
                    forceViolation = true;
                }
                if (!forceViolation) {
                    // Found a candidate to set in this move; these collisions are valid.
                    thisMove.collideX = collideX[i.intValue()];
                    thisMove.collideZ = collideZ[i.intValue()];
                    thisMove.collidesHorizontally = thisMove.collideX || thisMove.collideZ;
                    thisMove.negligibleHorizontalCollision = thisMove.collidesHorizontally && CollisionUtil.isHorizontalCollisionNegligible(new Vector(xTheoreticalDistance[i.intValue()], thisMove.yDistance, zTheoreticalDistance[i.intValue()]), to, inputs[i.intValue()].getStrafe(), inputs[i.intValue()].getForward());
                    break;
                }
            }
        }
        
        
        //////////////////////////////////////////////////////////////
        // Finish. Check if the move had been predictable at all    //
        //////////////////////////////////////////////////////////////
        /* The index representing the input associated to the pair of speed to set in this move. */
        int xIdx = -1;
        int zIdx = -1;
        if (i.intValue() >= 9) {
            // Cheating: prevent an index out of bounds (we couldn't find the correct pair of speed to set)
            i.set(4);
        }
        // If the move is unpredictable, the x/z speed cannot be associated to a specific input, thus we set them independently.
        // TODO: How can we know the impulse if the move is uncertain? ...
        if (!isPredictable) {
            // In this case, instead of setting the predicted speed with the smallest delta from the actual speed (0.0001), we select the speed that is closest to the current one, effectively allowing for the maximum predicted speed (just limits speed then).
            xIdx = MathUtil.findClosestIndex(xTheoreticalDistance, thisMove.xDistance);
            zIdx = MathUtil.findClosestIndex(zTheoreticalDistance, thisMove.zDistance);
        }
        // Done, set in this move.
        thisMove.xAllowedDistance = xTheoreticalDistance[!isPredictable ? xIdx : i.intValue()];
        thisMove.zAllowedDistance = zTheoreticalDistance[!isPredictable ? zIdx : i.intValue()];
        // Set more edge data useful for other stuff.
        // If unpredictable, we don't/can't actually know the direction
        thisMove.hasImpulse = !isPredictable ? AlmostBoolean.MAYBE : inputs[i.intValue()].getForwardDir().equals(InputDirection.ForwardDirection.NONE) && inputs[i.intValue()].getStrafeDir().equals(InputDirection.StrafeDirection.NONE) ? AlmostBoolean.NO : AlmostBoolean.YES;
        thisMove.strafeImpulse = inputs[isPredictable ? i.intValue() : xIdx].getStrafeDir();
        thisMove.forwardImpulse = inputs[isPredictable ? i.intValue() : zIdx].getForwardDir();
        if (debug) {
            player.sendMessage("[SurvivalFly] (postPredict) " + (!isPredictable ? "Uncertain" : "Predicted") + " direction: " + inputs[isPredictable ? i.intValue() : xIdx].getForwardDir() +" | "+ inputs[isPredictable ? i.intValue() : xIdx].getStrafeDir());
        }
    }
    
    /**
     * Loop through the impulse matrix (left/right/forward/backward/f. right/f. left/b. right/b. left), if {@link BridgeMisc#isInputKnown(Player)} returns false
     *
     * @param attributeAccess
     * @param tags
     * @param pData
     * @param player
     * @param inputs
     * @return
     */
    public static boolean loopImpulses(IGenericInstanceHandle<IAttributeAccess> attributeAccess, Collection<String> tags, final IPlayerData pData, final Player player, AtomicInteger i, InputDirection[] inputs) {
        for (int strafe = -1; strafe <= 1; strafe++) {
            for (int forward = -1; forward <= 1; forward++) {
                inputs[i.intValue()] = new InputDirection(strafe * 0.98f, forward * 0.98f);
                i.getAndIncrement();
            }
        }
        if (pData.isInCrouchingPose()) {
            tags.add("crouching");
            for (i.set(0); i.intValue() < 9; i.getAndIncrement()) {
                // Multiply all combinations
                inputs[i.intValue()].operationToInt(attributeAccess.getHandle().getPlayerSneakingFactor(player), attributeAccess.getHandle().getPlayerSneakingFactor(player), 1);
            }
        }
        if (BridgeMisc.isUsingItem(player)) {
            tags.add("usingitem");
            for (i.set(0); i.intValue() < 9; i.getAndIncrement()) { 
                inputs[i.intValue()].operationToInt(Magic.USING_ITEM_MULTIPLIER, Magic.USING_ITEM_MULTIPLIER, 1);
            }
        }
        return true;
    }
    
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
     * @param yDistanceAboveLimit
     * @return
     */
    private static double checkOnChangeOfYDirection(Collection<String> tags, long now, Player player, double yDistance, PlayerMoveData lastMove, MovingData data, IPlayerData pData, boolean fullyInAir, boolean yDirectionSwitch, PlayerMoveData thisMove, double yDistanceAboveLimit) {
        ///////////////////////////////////////////
        // Check on change of Y direction        //
        ///////////////////////////////////////////
        if (fullyInAir && yDirectionSwitch && !(data.timeRiptiding + 2000 > now)) {
            if (yDistance > 0.0) {
                // TODO: Demand consuming queued velocity for valid change (!).
                if (lastMove.touchedGround || lastMove.to.extraPropertiesValid && lastMove.to.resetCond) {
                    // Change to increasing phase
                    tags.add("y_switch_inc");
                }
                else {
                    // Moving upwards after falling without having touched the ground first
                    if (data.jumpDelay < 9 && !((lastMove.touchedGround || lastMove.from.onGroundOrResetCond) && lastMove.yDistance == 0D)
                        && data.getOrUseVerticalVelocity(yDistance) == null && !thisMove.hasLevitation) {
                        Improbable.check(player, (float) Math.min(1.0, Math.abs(yDistance)), System.currentTimeMillis(), "moving.survivalfly.airjump", pData);
                        yDistanceAboveLimit = Math.max(yDistanceAboveLimit, Math.max(1.0, Math.abs(yDistance))); // Ensure that players cannot get a VL of 0.
                        tags.add("airjump");
                    }
                    else tags.add("y_inair_switch");
                }
            }
            else {
                // Change to decreasing phase.
                tags.add("y_switch_dec");
                // (Previously: a low-jump check would have been performed here, let vDistRel catch it instead.)
            }
        }
        return yDistanceAboveLimit;
    }
}