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
	public boolean isSneaking;
	
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
     * Mostly intended to be used for h-speed prediction.
     */
    public boolean hasAttackSlowDown;


    // Bounds set by checks.
    /**
     * Estimated X distance only. Set in SurvivalFly.
     */
    public double xAllowedDistance;

    /**
     * Estimated Z distance only. Set in SurvivalFly.
     */
    public double zAllowedDistance;

    /**
     * Combined XZ distance. Set in SurvivalFly.
     */
    public double hAllowedDistance;

    /**
     * Vertical allowed distance estimated by checks.
     */
    public double yAllowedDistance;


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
     * Due to the thresholds and other subtitles with the PlayerMoveEvent, there could have been other
     * (micro-) moves by the player which could not be checked, since Bukkit skipped them. One moving event
     * is split into several other moves, with a cap.
     */
    public int multiMoveCount;
    
    /**
     * Mojang introduced a new "mechanic" in 1.17 which allows player to re-send their position on right-clicking.
     * On Bukkit's side, this translates in a PlayerMoveEvent which doesn't actually have any movement change (PME.getFrom() and PME.getTo() contain the same location)
     * This moving event is skipped from being processed.
     * Do note that players cannot send duplicate packets in a row, there has to be a non-duplicate packet in between each duplicate one.
     * (Sequence is: normal PME -> duplicate PME -> normal PME(...))
     */
    public boolean duplicateEvent;

    /**
     * Just the used vertical velocity. Could be overridden multiple times
     * during processing of moving checks.
     */
    public SimpleEntry verVelUsed = null;
    
    /**
     * Signal that this movement has horizontal impulse: meaning, the player has actively pressed a WASD key.
     * Intention is to be able to differentiate when the player is actively moving VS being passively moved by other sources (i.e.: push and velocity)
     */
    public boolean hasImpulse;
    

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
    	isSneaking = false;
    	isSwimming = false;
        slowedByUsingAnItem = false;
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
        // Meta stuff.
        multiMoveCount = 0;
        verVelUsed = null;
        duplicateEvent = false;
        hasImpulse = false;
        // Super class last, because it'll set valid to true in the end.
        super.resetBase();
    }

}
