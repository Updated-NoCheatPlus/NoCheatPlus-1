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
    
    /**
     * The distance covered by a move from the setback point to the to.getY() point.
     */
    public double setBackYDistance;
    
    /**
     * Indicates that this movement has/should have been slowed down due to the player hitting an entity (sprinting will be reset also).<br>
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
     * Allowed horizontal distance. Set in SurvivalFly.
     */
    public double hAllowedDistance;

    /**
     * Allowed vertical distance mostly use for elytra. Set in CreativeFly.check.
     */
    public double yAllowedDistance;

    /**
     * Vertical allowed distance estimated by checks.
     * TODO: Get rid of yAllowedDistance and use this one instead for both checks.
     */
    public double vAllowedDistance;


    // Properties involving the environment.
    /** This move was a bunny hop. */
    public boolean bunnyHop;
   
    /** The player can potentially step with this move. Set in SurvivalFly.check(vdistrel) */
    public boolean canStep;

    /** The player can potentially jump with this move. Set in SurvivalFly.check(vdistrel) */
    public boolean canJump;


    // Meta stuff.
    /**
     * Due to the thresholds for moving events, there could have been other
     * (micro-) moves by the player which could not be checked. One moving event
     * is split into several other moves, with a cap.
     */
    public int multiMoveCount;
    
    /**
     * Mojang introduced a new mechanic in 1.17 which allows player to re-send their position on right clicking.
     * So there could have been a duplicate move (to) of the one (from) that has just been sent.
     * This moving event is skipped from being processed.
     * Do note that players cannot send duplicate packets in a row, there has to be a non-duplicate packet in between each duplicate one.
     * (Sequence is: normal -> redundant -> normal (...))
     */
    public boolean duplicateEvent;

    /**
     * Just the used vertical velocity. Could be overridden multiple times
     * during processing of moving checks.
     */
    public SimpleEntry verVelUsed = null;
    

    @Override
    protected void resetBase() {
        // Properties of the player.
        hasLevitation = false;
        hasSlowfall = false;
        hasGravity = true; // Assume one to have gravity rather than the opposite... :)
        hasAttackSlowDown = false;
        // Properties involving the environment.
        bunnyHop = false;
        canStep = false;
        canJump = false;
        // Bounds set by checks.
        xAllowedDistance = 0.0;
        yAllowedDistance = 0.0;
        zAllowedDistance = 0.0;
        hAllowedDistance = 0.0;
        vAllowedDistance = 0.0;
        // Meta stuff.
        multiMoveCount = 0;
        verVelUsed = null;
        duplicateEvent = false;
        // Super class last, because it'll set valid to true in the end.
        super.resetBase();
    }

}
