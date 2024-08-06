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
package fr.neatmonster.nocheatplus.utilities.collision;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker;

/**
 * Represent an orientation concerning Axes (not necessarily a single one).
 * 
 * @author asofold
 *
 */
public enum Axis {
    // TODO: name?

    X_AXIS,
    Y_AXIS,
    Z_AXIS,
    XZ_AXES,
    XYZ_AXES,
    NONE;

    // 
    public static final List<Axis> AXIS_ORDER_YXZ = Collections.unmodifiableList(
            Arrays.asList(new Axis[]{Y_AXIS, X_AXIS, Z_AXIS}));
    public static final List<Axis> AXIS_ORDER_YZX = Collections.unmodifiableList(
            Arrays.asList(new Axis[]{Y_AXIS, Z_AXIS, X_AXIS}));
    public static final List<Axis> AXIS_ORDER_XZY = Collections.unmodifiableList(
            Arrays.asList(new Axis[]{X_AXIS, Z_AXIS, Y_AXIS}));
    public static final List<Axis> AXIS_ORDER_ZXY = Collections.unmodifiableList(
            Arrays.asList(new Axis[]{Z_AXIS, X_AXIS, Y_AXIS}));
    // 
    public static final List<Axis> AXIS_ORDER_XYZ = Collections.unmodifiableList(
            Arrays.asList(new Axis[]{X_AXIS, Y_AXIS, Z_AXIS}));
    public static final List<Axis> AXIS_ORDER_ZYX = Collections.unmodifiableList(
            Arrays.asList(new Axis[]{Z_AXIS, Y_AXIS, X_AXIS}));
    
    public static class RichAxisData {
        public Axis priority;
        public BlockChangeTracker.Direction exclude;
        public RichAxisData(Axis priority, BlockChangeTracker.Direction exclude) {
            this.priority = priority;
            this.exclude = exclude;
        }
    }
}
