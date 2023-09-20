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
package fr.neatmonster.nocheatplus.utilities.moving;

import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.LiftOffEnvelope;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;


/**
 * A library to confine most / some of the moving magic.
 * @author asofold
 *
 */
public class Magic {
	
	//////////////////////////
	// Vanilla Physics      //
	//////////////////////////
	public static final double DEFAULT_FLYSPEED = 0.1;
    /** EntityLiving, travel */
    public static final float HORIZONTAL_INERTIA = 0.91f;
    /** EntityLiving, jumpFromGround */
    public static final float BUNNYHOP_BOOST = 0.2f;
    /** EntityLiving, noJumpDelay field */
    public static final int BUNNYHOP_MAX_DELAY = 10;
    /** EntityLiving, handleOnClimbable */
    public static final double CLIMBABLE_MAX_SPEED = 0.15f;
    /** EntityLiving, flyingSpeed (NMS field can be misleading, this is for air in general, not strictly creative-fly) */
    public static final float AIR_ACCELERATION = 0.02f;
    /** EntityLiving, travel */
    public static final float LIQUID_BASE_ACCELERATION = 0.02f;
    /** EntityLiving, travel */
    public static final float HORIZONTAL_SWIMMING_INERTIA = 0.9f;
    /** EntityLiving, getWaterSlowDown */
    public static final float WATER_HORIZONTAL_INERTIA = 0.8f;
    /** EntityLiving, travel */
    public static final float DOLPHIN_GRACE_INERTIA = 0.96f;
    /** EntityLiving, travel */
    public static final float STRIDER_OFF_GROUND_MULTIPLIER = 0.5f;
    /** LocalPlayer, aiStep */
    public static final float SNEAK_MULTIPLIER = 0.3f;
    /** MCPK */
    public static final float SPRINT_MULTIPLIER = 1.3f;
    /** LocalPlayer, aiStep */
    public static final float USING_ITEM_MULTIPLIER = 0.2f;
    /** EntityHuman, maybeBackOffFromEdge */
    public static final double SNEAK_STEP_DISTANCE = 0.05;
    /** EntityLiving, travel */
    public static final float LAVA_HORIZONTAL_INERTIA = 0.5f;
    /** Result of 0.6^3. */
    public static final float DEFAULT_FRICTION_CUBED = 0.6f * 0.6f * 0.6f; // 0.21600002f;
    /** Result of (0.6 * 0.91)^3. Used by legacy clients. Newer clients use the one above, not inertia. */
    public static final float CUBED_INERTIA = 0.16277136f;
    /** HoneyBlock */
    public static final double SLIDE_START_AT_VERTICAL_MOTION_THRESHOLD = 0.13;
    /** HoneyBlock */
    public static final float SLIDE_SPEED_THROTTLE = 0.05f;
    /** EntityLiving, aiStep */
    public static final double NEGLIGIBLE_SPEED_THRESHOLD = 0.003;
    /** EntityLiving, aiStep */
    public static final double NEGLIGIBLE_SPEED_THRESHOLD_LEGACY = 0.005;
    /** EntityLiving, jumpInLiquid */
    public static final double LIQUID_SPEED_GAIN = 0.039;
    /** EntityLiving, goDownInWater */
    public static final double LIQUID_GRAVITY = -LIQUID_SPEED_GAIN;
    /** EntityLiving, travel */
    public static final double WATER_VERTICAL_INERTIA = 0.8;
    /** EntityLiving, travel */
    public static final double LAVA_VERTICAL_INERTIA = 0.5;
    /** EntityLiving, travel */
    public static final double DEFAULT_SLOW_FALL_GRAVITY = 0.01;
    /** EntityLiving, travel */
    public static final double DEFAULT_GRAVITY = 0.08;
    /** EntityLiving, travel */
    public static final double FRICTION_MEDIUM_AIR = 0.98;
    /** EntityHuman, attack */
    public static final double ATTACK_SLOWDOWN = 0.6;
    
    
    
    ///////////////////////////////////////////////
    // CraftBukkit/Minecraft constants.          //
    ///////////////////////////////////////////////
    public static final float CB_DEFAULT_WALKSPEED = 0.2f;
    /** Minimum squared distance for bukkit to fire PlayerMoveEvents. PlayerConnection.java */
    public static final double CraftBukkit_minMoveSqDist = 1f / 256;
    /** Minimum looking direction change for bukkit to fire PlayerMoveEvents. PlayerConnection.java */
    public static final float CraftBukkit_minLookChange = 10f;
    /** The minimum squared root distance for clients to send flying packet to the server (EntityPlayerSP.java): movements smaller than this are not sent. (Thanks Mojang!) */
    public static final double Minecraft_minMoveSqrtDistance = 0.03;
    

    
    
    ///////////////////////////////////////////
    // NoCheatPlus Physics / constants       //
    ///////////////////////////////////////////
    // *----------Misc.----------*
    public static final double PAPER_DIST = 0.01;
    public static final double PREDICTION_TOLERANCE = 0.0001;
    /**
     * Extreme move check threshold (Actual like 3.9 upwards with velocity,
     * velocity downwards may be like -1.835 max., but falling will be near 3
     * too.)
     */
    public static final double EXTREME_MOVE_DIST_VERTICAL = 4.0;
    public static final double EXTREME_MOVE_DIST_HORIZONTAL = 15.0;
    /** Minimal xz-margin for chunk load. */
    public static final double CHUNK_LOAD_MARGIN_MIN = 3.0;
    
    // *----------Gravity----------*
    public static final double GRAVITY_MAX = 0.0834;
    /** Likely the result of minimum gravity for CraftBukkit (below this distance, movemenets won't be fired) */
    public static final double GRAVITY_MIN = 0.0624; 
    /** Odd gravity: to be used with formula lastYDist * data.lastFrictionVertical - gravity, after failing the fallingEnvelope() check (old vDistrel) */
    public static final double GRAVITY_ODD = 0.05;
    /** Assumed minimal average decrease per move, suitable for regarding 3 moves. */
    public static final float GRAVITY_VACC = (float) (GRAVITY_MIN * 0.6); // 0.03744
    /** Span of gravity between maximum and minimum. 0.021 */
    public static final double GRAVITY_SPAN = GRAVITY_MAX - GRAVITY_MIN;
    /** This is actually 0.01, but this value matches with gravity formula (lastDelta * friction - gravity). Old vdistrel */
    public static final double SLOW_FALL_GRAVITY = 0.0097; 
    /** Somewhat arbitrary value to use with the legacy vAcc check with slowfall */
    public static final float GRAVITY_SLOW_FALL_VACC = (float)(SLOW_FALL_GRAVITY * 0.6);
    
    // *----------Friction (old)----------*
    /** NoCheatPlus water friction (old hSpeed handling */
    public static final double FRICTION_MEDIUM_WATER = 0.98;
    /** NoCheatPlus lava friction (old handling */
    public static final double FRICTION_MEDIUM_LAVA = 0.535;
    /** Like medium_air but a bit more precise */
    public static final double FRICTION_MEDIUM_ELYTRA_AIR = 0.9800002;
    
    // *----------Horizontal speeds/modifiers----------*
    public static final double WALK_SPEED = 0.221D;
    public static final double[] modSwim = new double[] {
            // Horizontal AND vertical with body fully in water
            0.115D / WALK_SPEED,  
            // Horizontal swimming only, 1.13 (Do not multiply with thisMove.walkSpeed)
            0.044D / WALK_SPEED,  
            // Vertical swimming only, 1.13 
            0.3D / WALK_SPEED, 
            // Horizontal with body out of water (surface level)
            0.146D / WALK_SPEED,}; 
    public static final double modDownStream = 0.19D / (WALK_SPEED * modSwim[0]);
    public static final double[] modDepthStrider = new double[] {
            1.0,
            0.1645 / modSwim[0] / WALK_SPEED,
            0.1995 / modSwim[0] / WALK_SPEED,
            1.0 / modSwim[0], // Results in walkspeed.
    };
    /** Somewhat arbitrary horizontal speed gain maximum for advance glide phase. */
    public static final double GLIDE_HORIZONTAL_GAIN_MAX = GRAVITY_MAX / 2.0;
    
    // *----------Vertical speeds/modifiers----------*
    public static final double climbSpeedAscend        = 0.119;
    public static final double climbSpeedDescend       = 0.151;
    public static final double snowClimbSpeedAscend    = 0.1764;
    public static final double snowClimbSpeedDescend   = 0.118;
    public static final double bushSpeedDescend        = 0.09;
    public static final double bubbleStreamDescend     = 0.49; // from wiki.
    public static final double bubbleStreamAscend      = 0.9; // 1.1 from wiki. Wiki is too fast 
    /** Some kind of minimum y descend speed (note the negative sign), for an already advanced gliding/falling phase with elytra. */
    public static final double GLIDE_DESCEND_PHASE_MIN = -Magic.GRAVITY_MAX - Magic.GRAVITY_SPAN;
    /** Somewhat arbitrary, advanced glide phase, maximum descend speed gain (absolute value is negative). */
    public static final double GLIDE_DESCEND_GAIN_MAX_NEG = -GRAVITY_MAX;
    /**
     * Somewhat arbitrary, advanced glide phase, maximum descend speed gain
     * (absolute value is positive, a negative gain seen in relation to the
     * moving direction).
     */
    public static final double GLIDE_DESCEND_GAIN_MAX_POS = GRAVITY_ODD / 1.95;
    
    // *----------On ground judgement----------*
    public static final double Y_ON_GROUND_MIN = 0.000001;
    public static final double Y_ON_GROUND_MAX = 0.01;
    public static final double Y_ON_GROUND_DEFAULT = 0.00001;
    /** LEGACY NCP YONGROUND VALUES */
    // public static final double Y_ON_GROUND_MIN = 0.00001;
    // public static final double Y_ON_GROUND_MAX = 0.0626;
    // TODO: Model workarounds as lost ground, use Y_ON_GROUND_MIN?
    // public static final double Y_ON_GROUND_DEFAULT = 0.025; // Jump upwards, while placing blocks. // Old 0.016
    // public static final double Y_ON_GROUND_DEFAULT = 0.029; // Bounce off slime blocks.
    

    // *----------Falling distance / damage / Nofall----------*
    /** The lower bound of fall distance for taking fall damage. */
    public static final double FALL_DAMAGE_DIST = 3.0;
    /** The minimum damage amount that actually should get applied. */
    public static final double FALL_DAMAGE_MINIMUM = 0.5;
    /**
     * The maximum distance that can be achieved with bouncing back from slime
     * blocks.
     */
    public static final double BOUNCE_VERTICAL_MAX_DIST = 3.5;   
    /** BlockBed, bounceUp method */
    public static final double BED_BOUNCE_MULTIPLIER = 0.66;
    /** BlockBed, bounceUp method */
    public static final double BED_BOUNCE_MULTIPLIER_ENTITYLIVING = 0.8;


    /**
     * 
     * @param thisMove
     *            Not strictly the latest move in MovingData.
     * @return
     */
    public static boolean touchedIce(final PlayerMoveData thisMove) {
        return thisMove.from.onIce || thisMove.from.onBlueIce || thisMove.to.onIce || thisMove.to.onBlueIce;
    } 

    /**
     * The absolute per-tick base speed for swimming vertically.
     * 
     * @return
     */
    public static double swimBaseSpeedV(boolean isSwimming) {
        // TODO: Does this have to be the dynamic walk speed (refactoring)?
        return isSwimming ? WALK_SPEED * modSwim[2] + 0.1 : WALK_SPEED * modSwim[0] + 0.07; // 0.244
    }

    /**
     * Simplistic check for past lift off states done via the past move tracking.
     * Does not check if players may be able to lift off at all (i.e: in liquid)
     * @return
     */
    public static boolean isValidLiftOffAvailable(int limit, final MovingData data) {
        limit = Math.min(limit, data.playerMoves.getNumberOfPastMoves());
        for (int i = 0; i < limit; i++) {
            final PlayerMoveData pastMove = data.playerMoves.getPastMove(i);
            if (pastMove.from.onGround && !pastMove.to.onGround 
                && pastMove.toIsValid && pastMove.yDistance > LiftOffEnvelope.NORMAL.getJumpGain(data.jumpAmplifier) - GRAVITY_MAX - Y_ON_GROUND_MIN) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean recentlyInBubbleStream(int limit, MovingData data) {
    	limit = Math.min(limit, data.playerMoves.getNumberOfPastMoves());
        for (int i = 0; i < limit; i++) {
            final PlayerMoveData pastMove = data.playerMoves.getPastMove(i);
            if (pastMove.from.inBubbleStream && !data.playerMoves.getCurrentMove().from.inBubbleStream) {
                return true;
            }
        }
        return false;
	}

    /**
     * Test for a specific move in-air -> water, then water -> in-air.
     * 
     * @param thisMove
     *            Not strictly the latest move in MovingData.
     * @param lastMove
     *            Move before thisMove.
     * @return
     */
    static boolean splashMove(final PlayerMoveData thisMove, final PlayerMoveData lastMove) {
        // Use past move data for two moves.
        return !thisMove.touchedGround && thisMove.from.inWater && !thisMove.to.resetCond // Out of water.
                && !lastMove.touchedGround && !lastMove.from.resetCond && lastMove.to.inWater // Into water.
                && excludeStaticSpeed(thisMove) && excludeStaticSpeed(lastMove)
                ;
    }

    /**
     * Test for a specific move ground/in-air -> water, then water -> in-air.
     * 
     * @param thisMove
     *            Not strictly the latest move in MovingData.
     * @param lastMove
     *            Move before thisMove.
     * @return
     */
    static boolean splashMoveNonStrict(final PlayerMoveData thisMove, final PlayerMoveData lastMove) {
        // Use past move data for two moves.
        return !thisMove.touchedGround && thisMove.from.inWater && !thisMove.to.resetCond // Out of water.
                && !lastMove.from.resetCond && lastMove.to.inWater // Into water.
                && excludeStaticSpeed(thisMove) && excludeStaticSpeed(lastMove)
                ;
    }

    /**
     * Fully in-air move.
     * 
     * @param thisMove
     *            Not strictly the latest move in MovingData.
     * @return
     */
    public static boolean inAir(final PlayerMoveData thisMove) {
        return !thisMove.touchedGround && !thisMove.from.resetCond && !thisMove.to.resetCond;
    }

    /**
     * Test if the player has lifted off from the ground or is landing (not in air, not walking on ground)
     * (Does not check for resetCond)
     * 
     * @return 
     */
    public static boolean liftingOffOrLandingOnGround(final PlayerMoveData move) {
        return move.from.onGround ^ move.to.onGround;
    }

    /**
     * A liquid -> liquid move. Exclude web and climbable.
     * 
     * @param thisMove
     * @return
     */
    public static boolean inLiquid(final PlayerMoveData thisMove) {
        return thisMove.from.inLiquid && thisMove.to.inLiquid && excludeStaticSpeed(thisMove);
    }

    /**
     * A water -> water move. Exclude web and climbable.
     * 
     * @param thisMove
     * @return
     */
    public static boolean inWater(final PlayerMoveData thisMove) {
        return thisMove.from.inWater && thisMove.to.inWater && excludeStaticSpeed(thisMove);
    }

    /**
     * Test if either point is in reset condition (liquid, web, ladder).
     * 
     * @param thisMove
     * @return
     */
    public static boolean resetCond(final PlayerMoveData thisMove) {
        return thisMove.from.resetCond || thisMove.to.resetCond;
    }

    /**
     * Moving out of liquid, might move onto ground. Exclude web and climbable.
     * 
     * @param thisMove
     * @return
     */
    public static boolean leavingLiquid(final PlayerMoveData thisMove) {
        return thisMove.from.inLiquid && !thisMove.to.inLiquid && excludeStaticSpeed(thisMove);
    }

    /**
     * Moving out of water, might move onto ground. Exclude web and climbable.
     * 
     * @param thisMove
     * @return
     */
    public static boolean leavingWater(final PlayerMoveData thisMove) {
        return thisMove.from.inWater && !thisMove.to.inWater && excludeStaticSpeed(thisMove);
    }

    /**
     * Moving into water, might move onto ground. Exclude web and climbable.
     * 
     * @param thisMove
     * @return
     */
    public static boolean intoWater(final PlayerMoveData thisMove) {
        return !thisMove.from.inWater && thisMove.to.inWater && excludeStaticSpeed(thisMove);
    }

    /**
     * Moving into liquid., might move onto ground. Exclude web and climbable.
     * 
     * @param thisMove
     * @return
     */
    public static boolean intoLiquid(final PlayerMoveData thisMove) {
        return !thisMove.from.inLiquid && thisMove.to.inLiquid && excludeStaticSpeed(thisMove);
    }

    /**
     * Exclude moving from/to blocks with static (vertical) speed, such as web, climbable, berry bushes.
     * 
     * @param thisMove
     * @return
     */
    public static boolean excludeStaticSpeed(final PlayerMoveData thisMove) {
        return !thisMove.from.inWeb && !thisMove.to.inWeb
                && !thisMove.from.onClimbable && !thisMove.to.onClimbable
                && !thisMove.from.inBerryBush && !thisMove.to.inBerryBush
                && !thisMove.from.inPowderSnow && !thisMove.to.inPowderSnow;
    }
}
