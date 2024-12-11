package fr.neatmonster.nocheatplus.checks.moving.envelope;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.components.modifier.IAttributeAccess;
import fr.neatmonster.nocheatplus.components.registry.event.IGenericInstanceHandle;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;
import fr.neatmonster.nocheatplus.utilities.moving.Magic;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;

/**
 * Various auxiliary methods for moving behaviour modeled after the client or otherwise observed on the server-side.
 */
public class PlayerEnvelopes {
    
    private static final IGenericInstanceHandle<IAttributeAccess> attributeAccess = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstanceHandle(IAttributeAccess.class);
    
   /* public static boolean isExtremeMoveLegit() {
        return BridgeMisc.isRipgliding(player) 
               || Bridge1_13.isRiptiding(player) && (thisMove.collideY || lastMove.collideY) 
               || !Double.isInfinite(Bridge1_9.getLevitationAmplifier(player)) && Bridge1_9.getLevitationAmplifier(player) >= 50
    }*/

    /**
     * Advanced glide phase vertical gain envelope.
     * 
     * @param yDistance
     * @param previousYDistance
     * @return
     */
    public static boolean glideVerticalGainEnvelope(final double yDistance, final double previousYDistance) {
        return  // Sufficient speed of descending.
                yDistance < Magic.GLIDE_DESCEND_PHASE_MIN && previousYDistance < Magic.GLIDE_DESCEND_PHASE_MIN
                // Controlled difference.
                && yDistance - previousYDistance > Magic.GLIDE_DESCEND_GAIN_MAX_NEG 
                && yDistance - previousYDistance < Magic.GLIDE_DESCEND_GAIN_MAX_POS;
    }

    /**
     * Friction envelope testing, with a different kind of leniency (relate
     * off-amount to decreased amount), testing if 'friction' has been accounted
     * for in a sufficient but not necessarily exact way.<br>
     * In the current shape this method is meant for higher speeds rather (needs
     * a twist for low speed comparison).
     * 
     * @param thisMove
     * @param lastMove
     * @param friction
     *            Friction factor to apply.
     * @param minGravity
     *            Amount to subtract from frictDist by default.
     * @param maxOff
     *            Amount yDistance may be off the friction distance.
     * @param decreaseByOff
     *            Factor, how many times the amount being off friction distance
     *            must fit into the decrease from lastMove to thisMove.
     * @return
     */
    public static boolean enoughFrictionEnvelope(final PlayerMoveData thisMove, final PlayerMoveData lastMove, final double friction, 
                                                 final double minGravity, final double maxOff, final double decreaseByOff) {
    
        // TODO: Elaborate... could have one method to test them all?
        final double frictDist = lastMove.yDistance * friction - minGravity;
        final double off = Math.abs(thisMove.yDistance - frictDist);
        return off <= maxOff && Math.abs(thisMove.yDistance - lastMove.yDistance) <= off * decreaseByOff;
    }

    /**
     * A non-vanilla formula for if the player is (well) within in-air falling envelope.
     * 
     * @param yDistance
     * @param lastYDist
     * @param lastFrictionVertical
     * @param extraGravity Extra amount to fall faster.
     * @return
     */
    public static boolean isFrictionFalling(final double yDistance, final double lastYDist, 
                                            final double lastFrictionVertical, final double extraGravity) {
        if (yDistance >= lastYDist) {
            return false;
        }
        final double frictDist = lastYDist * lastFrictionVertical - Magic.GRAVITY_MIN;
        return yDistance <= frictDist + extraGravity && yDistance > frictDist - Magic.GRAVITY_SPAN - extraGravity;
    }
    
    /**
     * Test if the player is constricted in an area with a 1.5 blocks-high ceiling (applies to 1.14 clients and above).
     * We cannot detect if players try to jump in here: on the server side, player is seen as never leaving the ground and without any vertical motion change.
     * 
     * @param from
     * @param pData
     * @return If onGround with ground-like blocks above within a margin of 0.09
     */
    public static boolean isVerticallyConstricted(final PlayerLocation from, final PlayerLocation to, final IPlayerData pData) {
        if (pData.getClientVersion().isLowerThan(ClientVersion.V_1_14)) {
            // The AABB is contracted only for 1.14+ players.
            return false;
        }
        if (!pData.isInCrouchingPose()) {
            return false;
        }
        return from.seekCollisionAbove(0.0899, false) && from.isOnGround() && to.isOnGround() && pData.getGenericInstance(MovingData.class).playerMoves.getCurrentMove().yDistance == 0.0; // Do explicitly demand to have no vertical motion, don't rely on just the ground status.
    }

    /**
     * Test if this move is a bunnyhop <br>
     * (Aka: sprint-jump. Increases the player's speed up to roughly twice the usual base speed)
     *
     * @param forceSetOffGround Currently used to ensure that bunnyhopping isn't applied when we're brute-forcing speed with onGround = false, while the player is constricted in a low-ceiling area,
     *                          due to the fact the client does not send any vertical movement change while in this state.
     * @return True, if isJump() returned true while the player is sprinting.
     */
    public static boolean isBunnyhop(final PlayerLocation from, final PlayerLocation to, final IPlayerData pData, boolean fromOnGround, boolean toOnGround, final Player player, boolean forceSetOffGround) {
        return 
               pData.isSprinting()
               && (
                    // 1:  99.9% of cases...
                    isJumpMotion(from, to, player, fromOnGround, toOnGround)
                    // 1: The odd one out. We can't know the ground status of the player, so this will have to do.
                    || isVerticallyConstricted(from, to, pData)
                    && (
                         !forceSetOffGround && pData.getClientVersion().isLowerThan(ClientVersion.V_1_21_2) // At least ensure to not apply this when we're brute-forcing speed with off-ground
                         || BridgeMisc.isSpaceBarImpulseKnown(player) && player.getCurrentInput().isJump()
                    
                    )
               );
    }
    
    /**
     * Test if the current motion can qualify as a jump.<br>
     * Note that: 
     * 1) This does not concern whether the player actual impulse (e.g., pressing the space bar). 
     *    For that, see {@link BridgeMisc#isSpaceBarImpulseKnown(Player)}.
     * 2) It also does not include upward movement through liquids. While Minecraft considers players as "jumping" if they just press the space bar, we intend jumping in its strict sense (jumping through air)<br><p>
     * For a motion to be considered a legitimate jump, the following conditions must be met:
     * <ul>
     * <li>The player must not be gliding, riptiding, levitating, or in a liquid block.</li>
     * <li>The player's jump phase ({@link fr.neatmonster.nocheatplus.checks.moving.model.LiftOffEnvelope#getMaxJumpPhase(double)}) 
     *     must be very recent (no more than 1).</li>
     * <li>The vertical motion must align with Minecraft's {@code jumpFromGround()} formula 
     *     (defined in {@code EntityLiving.java}). This is the most critical check.</li>
     * <li>The player must be in a "leaving ground" state, transitioning from ground to air. 
     *     Edge cases, such as lost ground, are accounted for here.</li>
     * </ul>
     * Additionally, invoking this method sets the `headObstruction` flag.
     * @return True, if the motion qualifies as a jump.
     */
    public static boolean isJumpMotion(final PlayerLocation from, final PlayerLocation to, final Player player, boolean fromOnGround, boolean toOnGround) {
        final IPlayerData pData = DataManager.getPlayerData(player);
        final MovingData data = pData.getGenericInstance(MovingData.class);
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        ////////////////////////////////
        // 0: Early return conditions.
        ////////////////////////////////
        if (thisMove.hasLevitation || thisMove.isRiptiding || thisMove.isGliding || from.isInLiquid()) {
            // Cannot jump for sure under these conditions
            return false;
        }
        ////////////////////////////////
        // 1: Jump phase condition.
        ////////////////////////////////
        if (data.sfJumpPhase > 1) { // NOTE on the ">" sign and not ">="
            // This event cannot be a jump: the player has been in air for far too long.
            return false;
        }
        ////////////////////////////////////
        // 2: Motion conditions.
        ////////////////////////////////////
        // Validate motion and update the headObstruction flag, if the player does actually collide with something above.
        double jumpGain = data.liftOffEnvelope.getJumpGain(data.jumpAmplifier) * attributeAccess.getHandle().getJumpGainMultiplier(player);
        Vector collisionVector = from.collide(new Vector(0.0, jumpGain, 0.0), fromOnGround || thisMove.touchedGroundWorkaround, from.getAABBCopy());
        thisMove.headObstructed = jumpGain != collisionVector.getY() && thisMove.yDistance >= 0.0 && !toOnGround; // For setting the flag, we don't care about the correct speed.
        jumpGain = collisionVector.getY();
        if (!MathUtil.almostEqual(thisMove.yDistance, jumpGain, Magic.PREDICTION_EPSILON)) { // NOTE: This must be the current move, never the last one.
            // This is not a jumping motion. Abort early.
            return false;
        }
        //////////////////////////////////
        // 3: Ground conditions.
        //////////////////////////////////
        // Finally, if this was a jumping motion and the player has very little air time, validate the ground status.
        // Demand to be in a "leaving ground" state.
        return
                
                // 1: Ordinary lift-off.
                fromOnGround && !toOnGround
                // 1: 1-tick-delayed-jump cases: ordinary and with lost ground
                // By "1-tick-delayed-jump" we mean a specific case where the player jumps, but sends a packet with 0 y-dist while still leaving ground (from ground -> to air)
                // On the next tick, a packet containing the jump motion (0.42) is sent, but the player is already fully in air (air -> air))
                // Mostly observed when jumping up a 1-block-high slope and then jumping immediately after, on the edge of the block. 
                // Technically, this should be considered a lost ground case, however the ground status is detected in this case, just with a delay.
                // TODO: Check for abuses. Check for more strict conditions.
                || lastMove.toIsValid && lastMove.yDistance <= 0.0 && !from.seekCollisionAbove() // This behaviour has not hitherto been observed with head obstruction, thus we can confine this edge case by ruling head obstruction cases out. We call seekCollisionAbove() as we don't need accuracy in this case.
                && (
                            // 2: The usual case, with ground status actually being detected later.
                            // https://gyazo.com/dfab44980c71dc04e62b48c4ffca778e
                            lastMove.from.onGround && !lastMove.to.onGround && !thisMove.touchedGroundWorkaround // Explicitly demand to not be using a lost ground case here.
                            // 2: However, sometimes the ground detection is missed, making this "delayed jump" a true lost-ground case.
                            || (thisMove.touchedGroundWorkaround && (!lastMove.touchedGroundWorkaround || !thisMove.to.onGround)) // TODO: Check which position (fromLostGround or toLostGround). This definition was added prior to adding the distinguishing flags.
                )
            ;
    }

    /**
     * Test if this movement fits into NoCheatPlus' stepping envelope.<br>
     * For NoCheatPlus, "step" has a much simpler meaning: moving <b>from</b> ground <b>to</b> ground without having to jump with the correct motion (with some exceptions due to lost ground)<br>
     * Minecraft has a much more complex way of determining when players should be able to step; 
     * the logic is encapsulated in {@link fr.neatmonster.nocheatplus.utilities.location.RichEntityLocation#collide(Vector, boolean, double[])}
     * @return True if this movement is from and to ground with positive yDistance, as determined by the attribute parameter.
     */
    public static boolean isStepUpByNCPDefinition(final IPlayerData pData, boolean fromOnGround, boolean toOnGround, Player player) {
         final MovingData data = pData.getGenericInstance(MovingData.class);
         final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
         // Step-up is handled by the collide() function in Minecraft, which is called on every move, so one could technically step up even while ripdiing or gliding.
         if (thisMove.isRiptiding) {
             return false;
         }
         if (thisMove.isGliding) {
             return false;
         }
         return  
                fromOnGround && toOnGround && MathUtil.almostEqual(thisMove.yDistance, attributeAccess.getHandle().getMaxStepUp(player), Magic.PREDICTION_EPSILON)
                // 0: Wildcard couldstep
                || thisMove.couldStepUp
                // If the step-up movement doesn't fall into any of the criteria above, let the collide() function handle it instead.
            ;
    }

    /**
     * First move after set back / teleport. Originally has been found with
     * PaperSpigot for MC 1.7.10, however it also does occur on Spigot for MC
     * 1.7.10.
     *
     * @param data
     * @return
     */
    public static boolean couldBeSetBackLoop(final MovingData data) {
        // TODO: Confine to from at block level (offset 0)?
        final double setBackYDistance;
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        if (data.hasSetBack()) {
            setBackYDistance = thisMove.to.getY() - data.getSetBackY();
        }
        // Skip being all too forgiving here.
        //        else if (thisMove.touchedGround) {
        //            setBackYDistance = 0.0;
        //        }
        else {
            return false;
        }
        return !lastMove.toIsValid && data.sfJumpPhase == 0 && thisMove.multiMoveCount > 0
                && setBackYDistance > 0.0 && setBackYDistance < Magic.PAPER_DIST 
                && thisMove.yDistance > 0.0 && thisMove.yDistance < Magic.PAPER_DIST && Magic.inAir(thisMove);
    }

    /**
     * Pre-conditions: A slime block is underneath and the player isn't really
     * sneaking (with negative motion with thisMove). This does not account for pistons pushing (slime) blocks.<br>
     * 
     * @param player
     * @param from
     * @param to
     * @param data
     * @param cc
     * @return
     */
    public static boolean canBounce(final Player player, final PlayerLocation from, final PlayerLocation to, 
                                    final MovingData data, final MovingConfig cc, final IPlayerData pData) {
        
        // Workaround/fix for bed bouncing. getBlockY() would return an int, while a bed's maxY is 0.5625, causing this method to always return false.
        // A better way to do this would to get the maxY through another method, just can't seem to find it :/
        if (pData.isShiftKeyPressed()) {
            return false;
        }
        double blockY = (to.getY() + 0.4375) % 1 == 0 ? to.getY() : to.getBlockY();
        return 
                // 0: Normal envelope (forestall NoFall).
                MovingUtil.getRealisticFallDistance(player, from.getY(), to.getY(), data, pData) > 1.0
                && (
                    // 1: Ordinary.
                    to.getY() - blockY <= Math.max(cc.yOnGround, cc.noFallyOnGround)
                    // 1: With carpet.
                    || BlockProperties.isCarpet(to.getTypeId()) && to.getY() - to.getBlockY() <= 0.9
                ) 
                // 0: Within wobble-distance.
                || to.getY() - blockY < 0.286 && to.getY() - from.getY() > -0.9
                && to.getY() - from.getY() < -Magic.GRAVITY_MIN
                && !to.isOnGround()
                // 0: Wildcard riptiding. No point in checking for distance constraints here when speed is so high.
                || Bridge1_13.isRiptiding(player)
                // 0: Wildcard micro bounces on beds
                || to.isOnGround() && !from.isOnGround() && to.getY() - from.getY() < 0.0 
                && MovingUtil.getRealisticFallDistance(player, from.getY(), to.getY(), data, pData) <= 0.5 // 0.5... Can probably be even smaller, since these are micro bounces.
                && to.isOnBouncyBlock() && !to.isOnSlimeBlock()
                ;
    }

}
