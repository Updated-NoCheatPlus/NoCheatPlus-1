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

/**
* Meant to carry information regarding the player's key presses (WASD)
*/
public class InputDirection {
    
    /** (A/D keys, left = 1, right = -1. A value of 0.0 means no strafe movement) */
    private float strafe;
    /** (W/S keys, forward = 1, backward = -1. A value of 0.0 means not moving backward nor forward) */
    private float forward;
    /** Enum direction of the forward value */
    private ForwardDirection fdir;
    /** Enum direction of the strafe value */
    private StrafeDirection sdir;
    
    /**
     * Compose a new instance meant to represent the player's key presses.
     * 
     * @param strafe Represents sideways movement.
     * @param forward Represents forward and backward movement.
     */
    public InputDirection(float strafe, float forward) {
        this.forward = forward;
        this.strafe = strafe;
        fdir = forward >= 0.0 ? forward == 0.0 ? ForwardDirection.NONE : ForwardDirection.FORWARD : ForwardDirection.BACKWARD;
        sdir = strafe >= 0.0 ? strafe == 0.0 ? StrafeDirection.NONE : StrafeDirection.LEFT : StrafeDirection.RIGHT;
    }
    
    /**
     * @return the strafe value
     */
    public float getStrafe() {
        return strafe;
    }
    
    /**
     * @return the forward value
     */
    public float getForward() {
        return forward;
    }
    
    /**
     * Run an operation with strafeMult and forwardMult.
     * 
     * @param strafeFactor Factor to use with the strafe value
     * @param forwardFactor Factor to use with the forward value
     * @param operation     The operation to execute with strafeMult and forwardMult <br>
     *                      0 = Sets the enum direction to NONE and resets values for both strafe and forward.<br>
     *                      1 = Multiply strafe and forward by strafeMult and forwardMult respectively.<br>
     *                      2 = Divide strafe and forward by strafeMult and forwardMult respectively.
     */
    public void runOperation(double strafeFactor, double forwardFactor, int operation) {
        switch (operation) {
            case 0:
                strafe = 0f;
                forward = 0f;
                fdir = ForwardDirection.NONE;
                sdir = StrafeDirection.NONE;
                break;
            case 1:
                strafe *= strafeFactor;
                forward *= forwardFactor;
                break;
            case 2:
                strafe /= strafeFactor;
                forward /= forwardFactor;
                break;
            default: 
                return;
        }
        
    }
    
    /**
     * @return The enum direction that corresponds to the strafe value (LEFT/RIGHT/NONE)
     */
    public StrafeDirection getStrafeDir() {
        return sdir;
    }
    
    /**
     * @return The enum direction that corresponds to the forward value (FORWARD/BACKWARD/NONE)
     */
    public ForwardDirection getForwardDir() {
        return fdir;
    }

    public enum ForwardDirection {
        NONE,
        FORWARD,
        BACKWARD
    }

    public enum StrafeDirection {
        NONE,
        LEFT,
        RIGHT
    }
}
