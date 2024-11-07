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
package fr.neatmonster.nocheatplus.checks.moving.model;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.velocity.SimpleEntry;
import fr.neatmonster.nocheatplus.compat.AlmostBoolean;

/**
 * Include player-specific data for a move.
 * 
 * @author asofold
 *
 */
public class PlayerMoveData extends MoveData {

    //////////////////////////////////////////////////////////
    // Reset with set, could be lazily set during checking.
    //////////////////////////////////////////////////////////

    // Properties of the player.
	/** Whether this move has the levitation effect active */
	public boolean hasLevitation;
	
	/** Whether this move has the slowfall effect active */
	public boolean hasSlowfall;
	
	/** Whether this movement is influenced by gravity, as reported by {@link fr.neatmonster.nocheatplus.compat.BridgeMisc#hasGravity(LivingEntity)} */
	public boolean hasGravity;
    
	/** Player action set on {@link org.bukkit.event.player.PlayerMoveEvent}. NOTE: this is NOT the toggle glide moment, but the entire gliding phase. */
	public boolean isGliding;
    
    /** Represents how far the player is submerged in lava */
    public double submergedLavaHeight;
    
    /** Represents how far the player is submerged in water */
	public double submergedWaterHeight;

    /** Player action set on {@link org.bukkit.event.player.PlayerMoveEvent}. */
    public boolean slowedByUsingAnItem;
	
	/** 
	 * Player action set on {@link org.bukkit.event.player.PlayerMoveEvent}. 
	 * NOTE: this is NOT the propelling moment triggered by {@link org.bukkit.event.player.PlayerRiptideEvent}, 
	 * it is the entire riptide phase (for which the game activates its tick counter (See ItemTrident.java, entityHuman.startAutoSpinAttack(20))
	 */
	public boolean isRiptiding;
	
	/** Player action set on {@link org.bukkit.event.player.PlayerMoveEvent} */
	public boolean isSprinting;
	
	/** Player action set on {@link org.bukkit.event.player.PlayerMoveEvent} */
	public boolean isCrouching;
	
	/** Player action set on {@link org.bukkit.event.player.PlayerMoveEvent} */
	public boolean isSwimming;
	
    /**
     * The distance covered by a move from the setback point to the to.getY() point.
     * Usually, this corresponds to the jump height, because the set back point is set on ground.<br>
     * This could change in the future with set back policies changes (i.e.: force-fall)
     */
    public double setBackYDistance;
    
    /**
     * Indicates that this movement has/should have been slowed down due to the player hitting an entity (sprinting will be reset as well).<br>
     * Mostly intended to be used for the h-speed prediction.
     */
    public boolean hasAttackSlowDown;


    // Bounds set by checks.
    /**
     * Estimated X distance only. Set in SurvivalFly. Could be overridden multiple times
     * during processing of moving checks.
     */
    public double xAllowedDistance;
    
    /**
     *  The collision oon the X axis that has been set in this movement by SurvivalFly.
     *  Prior to version 1.21.2, this was set only if the theoretical speed prediction to set in this movement was found; or in other words, only if the player isn't cheating.
     *  On 1.21.2 and above, this is always set 
     *  @see PlayerMoveData#hasImpulse
     */ 
    public boolean collideX;

    /**
     * Estimated Z distance only. Set in SurvivalFly. Could be overridden multiple times
     * during processing of moving checks.
     */
    public double zAllowedDistance;

    /**
     *  The collision oon the Z axis that has been set in this movement by SurvivalFly.
     *  Prior to version 1.21.2, this was set only if the theoretical speed prediction to set in this movement was found; or in other words, only if the player isn't cheating.
     *  On 1.21.2 and above, this is always set.
     *  @see PlayerMoveData#hasImpulse
     */
    public boolean collideZ;

    /**
     * Combined XZ distance. Set in SurvivalFly.
     */
    public double hAllowedDistance;

    /**
     * Vertical allowed distance estimated by checks. Could be overridden multiple times
     * during processing of moving checks.
     */
    public double yAllowedDistance;
    
    /**
     * The vertical collision as set by {@link fr.neatmonster.nocheatplus.utilities.location.RichEntityLocation#collide(Vector, boolean, MovingConfig, double[])} in SurvivalFly (vdistrel).
     * Note that this does not differentiate collision above VS below: it considers both.
     */
    public boolean collideY;


    // Properties involving the environment.
    /** This move was a bunny hop. */
    public boolean bunnyHop;
   
    /** This move was a step-up. Set in SurvivalFLy */
    public boolean isStepUp;

    /** This move was a jump. Set in SurvivalFly. */
    public boolean isJump;
    
    /** Highly uncertain movement: player might step up with this movement; we cannot know for sure. Set with lost-ground couldstep */
    public boolean couldStepUp;
    

    // Meta stuff.
    /**
     * Due to the thresholds and other subtleties with the {@link org.bukkit.event.player.PlayerMoveEvent}, there could have been other
     * (micro-) moves by the player which could not be checked, because Bukkit did not fire an event for them. One moving event
     * is split into several other moves, with a cap.
     */
    public int multiMoveCount;
    
    /**
     * Mojang introduced a new "mechanic" in 1.17 which allows player to re-send their position on right-clicking.
     * On Bukkit's side, this translates in a {@link org.bukkit.event.player.PlayerMoveEvent} which doesn't actually have any movement change ({@link PlayerMoveEvent#getFrom()} and {@link PlayerMoveEvent#getTo()} contain the same location)
     * This moving event is skipped from being processed.
     * Do note that players cannot send duplicate packets in a row: after we receive an "empty" PlayerMoveEvent, the next one incoming must have the actual movement change.
     * (Sequence is: normal PME -> duplicate PME -> normal PME(...))
     */
    public boolean hasNoMovementDueToDuplicatePacket;

    /**
     * Just the used vertical velocity. Could be overridden multiple times
     * during processing of moving checks.
     */
    public SimpleEntry verVelUsed = null;
    
    /**
     * Indicates whether this movement has a horizontal impulse, meaning the player actively pressed a WASD key.
     * This helps differentiate between player-driven movement and passive movement (e.g., from external forces like push or velocity).
     * <p>
     * <b>Prior to version 1.21.2, inputs were not sent to the server, so movement impulse was inferred by replicating client-movement calculations.
     * This means this boolean's value depended on our prediction:</b>
     *
     * <ul>
     * <li>YES: Active player input was detected through prediction.</li>
     * <li>MAYBE: Prediction was inconclusive, so player input is unclear.</li>
     * <li>NO: No active player input was detected through prediction.</li>
     * </ul>
     *
     * <p>For clients supporting impulse event-sending, this will only return YES or NO, as input data is always available.</p>
     */
    public AlmostBoolean hasImpulse;
    
    /**
     * Indicates the strafing direction (LEFT, RIGHT, or NONE).
     *
     * <p>This value is set even if horizontal movement could not be accurately predicted, so it may be unreliable, unless the client sends impulse events, in which case it is dependable.
     * Check {@link PlayerMoveData#hasImpulse} for its reliability prior to version 1.21.2 </p>
     */
    public InputDirection.StrafeDirection strafeImpulse;
    
    /**
     * Indicates the forward movement direction (FORWARD, BACKWARD, or NONE).
     *
     * <p>This value is set even if horizontal movement could not be accurately predicted, so it may be unreliable, unless the client sends impulse events, in which case it is dependable.
     * Check {@link PlayerMoveData#hasImpulse} for its reliability prior to version 1.21.2 </p>
     */
    public InputDirection.ForwardDirection forwardImpulse;
    
    /**
     * Judge if this horizontal collision ({@link PlayerMoveData#collideX} or {@link PlayerMoveData#collideZ}) is to be considered as minor.
     * This is for Minecraft's sprinting reset mechanic.
     * Prior to 1.21.2, this was set only if the appropriate speed to set was found, therefore, this would return false if the predicted speed is uncertain.
     * On 1.21.2 and above, this boolean's value is always set.
     * @see PlayerMoveData#hasImpulse
     */
    public boolean negligibleHorizontalCollision;
    
    /**
     * Result of either {@link PlayerMoveData#collideX} or {@link PlayerMoveData#collideZ}.
     * Prior to 1.21.2, this was set only if the appropriate speed to set was found, therefore, this would return false if the predicted speed is uncertain.
     * On 1.21.2 and above, this boolean's value is always set.
     * @see PlayerMoveData#hasImpulse
     */
    public boolean collidesHorizontally;
    
    
    @Override
    protected void resetBase() {
        // Properties of the player.
        hasLevitation = false;
        hasSlowfall = false;
        hasGravity = true; // Assume one to have gravity rather than the opposite... :)
        hasAttackSlowDown = false;
        submergedLavaHeight = 0.0;
        submergedWaterHeight = 0.0;
        isGliding = false;
    	isRiptiding = false;
    	isSprinting = false;
    	isCrouching = false;
    	isSwimming = false;
        slowedByUsingAnItem = false;
        forwardImpulse = InputDirection.ForwardDirection.NONE;
        strafeImpulse = InputDirection.StrafeDirection.NONE;
        // Properties involving the environment.
        bunnyHop = false;
        isStepUp = false;
        isJump = false;
        couldStepUp = false;
        // Bounds set by checks.
        xAllowedDistance = 0.0;
        yAllowedDistance = 0.0;
        zAllowedDistance = 0.0;
        hAllowedDistance = 0.0;
        collideX = false;
        collideY = false;
        collideZ = false;
        // Meta stuff.
        multiMoveCount = 0;
        verVelUsed = null;
        hasNoMovementDueToDuplicatePacket = false;
        negligibleHorizontalCollision = false;
        collidesHorizontally = false;
        hasImpulse = AlmostBoolean.NO;
        // Super class last, because it'll set valid to true in the end.
        super.resetBase();
    }

}
