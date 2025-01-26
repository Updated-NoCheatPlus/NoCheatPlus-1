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
package fr.neatmonster.nocheatplus.checks.moving.velocity;

import fr.neatmonster.nocheatplus.utilities.TickTask;

public class PairEntry {
    /** Tick at which velocity got added. */
    public final int tick;

    /** The amount of X Axis velocity. */
    public final double x;
    
    /** The amount of X Axis velocity. */
    public final double z;

    /** Initial value for the actCount. */
    public final int initialActCount;

    /**
     * Flags to indicate special cases, such as splitting a used entry, if not
     * all gets used.
     */
    public final long flags;

    /** Count down for invalidation. */
    public int actCount;
    
    public PairEntry(double x, double z, int actCount) {
        this(TickTask.getTick(), x, z, actCount);
    }

    public PairEntry(int tick, double x, double z, int actCount) {
        this(tick, x, z, 0L, actCount);
    }

    public PairEntry(int tick, double x, double z, long flags, int actCount) {
        this.tick = tick;
        this.x = x;
        this.z = z;
        this.actCount = actCount;
        this.initialActCount = actCount;
        this.flags = flags;
    }

    public boolean hasFlag(final long flag) {
        return (flags & flag) == flag;
    }

    public String toString() {
        return "PairEntry(tick=" + tick + " x=" + x + " z=" + z + " flags=" + flags + " activate=" + actCount + "/" + initialActCount + ")";
    }
}
