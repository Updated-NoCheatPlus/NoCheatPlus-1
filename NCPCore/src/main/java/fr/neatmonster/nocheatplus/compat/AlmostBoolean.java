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
package fr.neatmonster.nocheatplus.compat;

/**
 * Some tri-state with booleans in mind.
 * @author mc_dev
 *
 */
public enum AlmostBoolean{
    YES,
    NO,
    MAYBE;
    
    /**
     * Matches a boolean value to its corresponding {@code AlmostBoolean} state.
     *
     * @param value The boolean value to match.
     * @return {@code YES} if {@code value} is true, {@code NO} if {@code value} is false.
     */
    public static final AlmostBoolean match(final boolean value) {
        return value ? YES : NO;
    }
    
    /**
     * Matches a string input to its corresponding {@code AlmostBoolean} state.
     *
     * <p>The method recognizes the following string inputs:
     * <ul>
     * <li>"true", "yes", "y" (case-insensitive) map to {@code YES}</li>
     * <li>"false", "no", "n" (case-insensitive) map to {@code NO}</li>
     * <li>"default", "maybe" (case-insensitive) map to {@code MAYBE}</li>
     * </ul>
     *
     * <p>If the input is null or does not match any recognized value, the method returns null.
     *
     * @param input The string input to match, which can be null.
     * @return The corresponding {@code AlmostBoolean} value, or null if the input is unrecognized.
     */
    public static final AlmostBoolean match(String input) {
        if (input == null) {
            return null;
        }
        input = input.trim().toLowerCase();
        if (input.equals("true") || input.equals("yes") || input.equals("y")) {
            return AlmostBoolean.YES;
        }
        else if (input.equals("false") || input.equals("no") || input.equals("n")) {
            return AlmostBoolean.NO;
        }
        else if (input.equals("default") || input.equals("maybe")) {
            return AlmostBoolean.MAYBE;
        } else {
            return null;
        }
    }

    /**
     * Pessimistic interpretation: true if YES.
     * 
     * @return true if the state is {@code YES}, false otherwise.
     */
    public boolean decide(){
        return this == YES;
    }

    /**
     * Optimistic interpretation: true if not NO.
     * 
     * @return true if the state is {@code YES} or {@code MAYBE}, false if the state is {@code NO}.
     */
    public boolean decideOptimistically() {
        return this != NO;
    }

}
