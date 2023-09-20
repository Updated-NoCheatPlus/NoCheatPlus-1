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
 * Basic preset envelopes for moving off one medium.
 * Jump speed values are from Minecraft (JumpPower); heights are our estimations.
 * 
 * @author asofold
 *
 */
public enum LiftOffEnvelope {
    /** Normal in-air lift off, without any restrictions/specialties. */
    NORMAL(0.42, 1.26, 6, true),
    /** (Non-vanilla) Simple calm water surface, strong limit */
    LIMIT_LIQUID(0.1, 0.27, 3, false),
    // NOTE: Stuck-speed all have a jump height that is equal to lift-off speed.
    /** Web jump envelope (stuck-speed) */
    LIMIT_WEBS(0.021, 0.021, 0, true),
    /** Berry bush jump envelope (stuck-speed). */
    LIMIT_SWEET_BERRY(0.315, 0.315, 0, true), 
    /** Powder snow jump envelope (stuck-speed). */
    LIMIT_POWDER_SNOW(0.63, 0.63, 0, true),
    /** Honey block jump envelope. */
    LIMIT_HONEY_BLOCK(0.21, 0.4, 4, true), 
    /** This medium is not covered by the enum */
    UNKNOWN(0.0, 0.0, 0, false)
    ;

    private double jumpGain;
    private double maxJumpHeight;
    private int maxJumpPhase;
    private boolean jumpEffectApplies;

    private LiftOffEnvelope(double jumpGain, double maxJumpHeight, int maxJumpPhase, boolean jumpEffectApplies) {
        this.jumpGain = jumpGain;
        this.maxJumpHeight = maxJumpHeight;
        this.maxJumpPhase = maxJumpPhase;
        this.jumpEffectApplies = jumpEffectApplies;
    }

    /**
     * The expected speed with lift-off.
     * 
     * @param jumpAmplifier
     * @return The lift-off speed.
     */
    public double getJumpGain(double jumpAmplifier) {
        if (jumpEffectApplies && jumpAmplifier != 0.0) {
            return Math.max(0.0, jumpGain + 0.1 * jumpAmplifier);
        }
        return jumpGain;
    }

    /**
     * The expected speed with lift-off, with a custom factor for the jump amplifier.
     * 
     * @param jumpAmplifier
     * @param factor 
     *             Meant for stuck-speed
     * @return The lift-off speed.
     */
    public double getJumpGain(double jumpAmplifier, double factor) {
        if (jumpEffectApplies && jumpAmplifier != 0.0) {
            return Math.max(0.0, jumpGain + 0.1 * jumpAmplifier * factor);
        }
        return jumpGain;
    }
    
    /**
     * Maximum jump height in blocks.
     * Might not be the most accurate value; partly taken from various sources, partly from testing.
     * 
     * @param jumpAmplifier
     * @return The maximum jump height for this envelope
     */
    public double getMaxJumpHeight(double jumpAmplifier) {
        if (jumpEffectApplies && jumpAmplifier > 0.0) {
            // NOTE: The jumpAmplifier value is one higher than the MC level.
            if (jumpAmplifier < 10.0) {
                // Classic.
                // TODO: Can be confined more.
                return maxJumpHeight + 0.6 + jumpAmplifier - 1.0;
            }
            else if (jumpAmplifier < 19) {
                // Quadratic, without accounting for gravity.
                return 0.6 + (jumpAmplifier + 3.2) * (jumpAmplifier + 3.2) / 16.0;
            }
            else {
                // Quadratic, with some amount of gravity counted in.
                return 0.6 + (jumpAmplifier + 3.2) * (jumpAmplifier + 3.2) / 16.0 - (jumpAmplifier * (jumpAmplifier - 1.0) / 2.0) * (0.0625 / 2.0);
            }
        } 
        // TODO: < 0.0 ?
        return maxJumpHeight;
    }
    
    /**
     * The maximum phases (read as: moving-events) a jump can have before the player is expected to lose altitude.
     * Intended for true in-air phases only. Thus stuck-speed blocks will have a phase of 0.
     * 
     * @param jumpAmplifier
     * @return The maximum jump phase.
     */
    public int getMaxJumpPhase(double jumpAmplifier) {
        if (jumpEffectApplies && jumpAmplifier > 0.0) {
            return (int) Math.round((0.5 + jumpAmplifier) * (double) maxJumpPhase);
        } 
        // TODO: < 0.0 ?
        return maxJumpPhase;
    }
    
    /**
     * @return Whether the jump boost potion/effect applies for this envelope.
     */
    public boolean jumpEffectApplies() {
        return jumpEffectApplies;
    }
}
