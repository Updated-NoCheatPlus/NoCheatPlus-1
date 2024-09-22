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
	
	/** Whether this movement is influenced by gravity */
	public boolean hasGravity;
    
	/** Player action set on PlayerMoveEvents. NOTE: this is NOT the toggle glide moment, but the entire gliding phase. */
	public boolean isGliding;

    /** Set with BridgeMisc.isUsingItem(player) on PlayerMoveEvents. */
    public boolean slowedByUsingAnItem;
	
	/** 
	 * Player action set on PlayerMoveEvents. 
	 * NOTE: this is NOT the propelling moment triggered by PlayerRiptideEvent, 
	 * it is the entire riptide phase (for which the game activates its tick counter (See ItemTrident.java, entityHuman.startAutoSpinAttack(20))
	 */
	public boolean isRiptiding;
	
	/** Player action set on PlayerMoveEvents */
	public boolean isSprinting;
	
	/** Player action set on PlayerMoveEvents */
	public boolean isCrouching;
	
	/** Player action set on PlayerMoveEvents */
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
     *  The theoretical collision that has been set in this movement by SurvivalFly. This is set only if a theoretical speed is actually found
     *  (In other words, only if the player isn't cheating)
     */
    public boolean collideX;

    /**
     * Estimated Z distance only. Set in SurvivalFly. Could be overridden multiple times
     * during processing of moving checks.
     */
    public double zAllowedDistance;

    /**
     *  The theoretical collision that has been set in this movement by SurvivalFly. This is set only if a theoretical speed is actually found
     *  (In other words, only if the player isn't cheating)
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
     * The vertical collision as set by RichEntityLocation#collide() in SurvivalFly (vdistrel).
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
     * Due to the thresholds and other subtleties with the PlayerMoveEvent, there could have been other
     * (micro-) moves by the player which could not be checked, because Bukkit did not fire an event for them. One moving event
     * is split into several other moves, with a cap.
     */
    public int multiMoveCount;
    
    /**
     * Mojang introduced a new "mechanic" in 1.17 which allows player to re-send their position on right-clicking.
     * On Bukkit's side, this translates in a PlayerMoveEvent which doesn't actually have any movement change (PME.getFrom() and PME.getTo() contain the same location)
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
     * Indicates whether this movement has a horizontal impulse, meaning the player has actively pressed a WASD key.
     * This helps differentiate between active movement by the player and passive movement caused by other sources 
     * (e.g., push or velocity).
     *
     * <ul>
     * <li>YES: The horizontal movement was predicted, confirming active player input.</li>
     * <li>MAYBE: The horizontal movement could not be predicted, so it is unclear if the player actively pressed a WASD key.</li>
     * <li>NO: The horizontal movement was predicted to have no active input from the player.</li>
     * </ul>
     */
    public AlmostBoolean hasImpulse;
    
    /**
     * Indicates the direction of strafing movement (LEFT/RIGHT/NONE).
     *
     * <p>This value is set even if the horizontal movement could not be predicted, so the strafe direction might be inaccurate.
     * Check {@link PlayerMoveData#hasImpulse} to determine the reliability of this value.
     */
    public InputDirection.StrafeDirection strafeImpulse;
    
    /**
     * Indicates the direction of forward movement (FORWARD/BACKWARD/NONE).
     *
     * <p>This value is set even if the horizontal movement could not be predicted, so the forward direction might be inaccurate.
     * Check {@link PlayerMoveData#hasImpulse} to determine the reliability of this value.
     */
    public InputDirection.ForwardDirection forwardImpulse;
    
    /**
     * Judge if this horizontal (x/z) collision is to be considered as minor.
     * This is for Minecraft's sprinting reset mechanic.
     * Only set if the appropriate speed to set was found.
     */
    public boolean negligibleHorizontalCollision;


    @Override
    protected void resetBase() {
        // Properties of the player.
        hasLevitation = false;
        hasSlowfall = false;
        hasGravity = true; // Assume one to have gravity rather than the opposite... :)
        hasAttackSlowDown = false;
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
        hasImpulse = AlmostBoolean.NO;
        // Super class last, because it'll set valid to true in the end.
        super.resetBase();
    }

}
