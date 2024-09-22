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
package fr.neatmonster.nocheatplus.checks.workaround;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import fr.neatmonster.nocheatplus.workaround.IWorkaround;
import fr.neatmonster.nocheatplus.workaround.SimpleWorkaroundRegistry;
import fr.neatmonster.nocheatplus.workaround.WorkaroundCountDown;
import fr.neatmonster.nocheatplus.workaround.WorkaroundCounter;

/**
 * Workaround registry for primary thread use. Potentially cover all checks.
 * 
 * @author asofold
 *
 */
public class WRPT extends SimpleWorkaroundRegistry {

    ///////////////////////
    // Workaround ids.
    ///////////////////////
    // MOVING_SURVIVALFLY
    public static final String W_M_SF_TOUCHDOWN = "m.sf.touch_down_collision";
    public static final String W_M_SF_HEAD_OBSTRUCTION = "m.sf.head_obstruction_collision";
    public static final String W_M_SF_COULD_BE_SETBACK_LOOP = "m.sf.could_be_setback_loop";
    public static final String W_M_SF_LANDING_ON_PWDSNW_FALLDIST_25 = "m.sf.pwdsnw_landing_2.5_falldist";
    public static final String W_M_SF_POST_LOSTGROUND_CASE = "m.sf.after_lostground";
    public static final String W_M_SF_NO_GRAVITY_GAP = "m.sf.too_little_air_time";
    public static final String W_M_SF_INACCURATE_SPLIT_MOVE = "m.sf.bukkit_split_move";
    // Levitation
    public static final String W_M_SF_LEVITATION_1_8_CLIENT = "m.sf.legacy1.8_levitating";
    public static final String W_M_SF_SBYTE_OVERFLOW = "m.sf.sbyte_overflow";
    public static final String W_M_SF_FIRST_MOVE_ASCNEDING_FROM_GROUND = "m.sf.1st_move_ascending";
    public static final String W_M_SF_PWDSNW_ASCEND = "m.sf.pwdsnow_ascend";
    public static final String W_M_SF_BED_STEP_DOWN = "m.sf.bed_stepdown_1_20+";

    // Vehicle: oddInAirDescend
    /**
     * Vehicle descending in-air, skip one vehicle move event during late in-air
     * phase.
     */
    public static final String W_M_V_ENV_INAIR_SKIP = "m.v.env.inair.skip";
    /** Just a counter for back to surface for in-water moves (water-water). */
    public static final String W_M_V_ENV_INWATER_BTS = "m.v.env.inwater.bts";

    
    ///////////////////////
    // Group ids.
    ///////////////////////
    // MOVING_SURVIVALFLY
    /**
     * Group: Reset when not in air jump phase. Both used for players and
     * vehicles with players inside.
     */
    public static final String G_RESET_NOTINAIR = "reset.notinair";

    
    
    
    ///////////////////////
    // WorkaroundSet ids.
    ///////////////////////
    // MOVING
    /** WorkaroundSet: for use in MovingData. */
    public static final String WS_MOVING = "moving";



    public WRPT() {
        // Fill in blueprints, groups, workaround sets.

        // MOVING
        final Collection<IWorkaround> ws_moving = new LinkedList<IWorkaround>();

        // MOVING_SURVIVALFLY
        // Reset once on ground or reset-condition.
        final WorkaroundCountDown[] resetNotInAir = new WorkaroundCountDown[] {
                new WorkaroundCountDown(W_M_V_ENV_INAIR_SKIP, 1),
        };
        ws_moving.addAll(Arrays.asList(resetNotInAir));
        setWorkaroundBluePrint(resetNotInAir);
        setGroup(G_RESET_NOTINAIR, resetNotInAir);

        // Just counters.
        final String[] counters = new String[] {
                // Player
                // Vehicle
                W_M_V_ENV_INWATER_BTS,
                W_M_SF_LANDING_ON_PWDSNW_FALLDIST_25,
                W_M_SF_COULD_BE_SETBACK_LOOP,
                W_M_SF_HEAD_OBSTRUCTION,
                W_M_SF_TOUCHDOWN,
                W_M_SF_LEVITATION_1_8_CLIENT,
                W_M_SF_SBYTE_OVERFLOW,
                W_M_SF_FIRST_MOVE_ASCNEDING_FROM_GROUND,
                W_M_SF_PWDSNW_ASCEND,
                W_M_SF_POST_LOSTGROUND_CASE,
                W_M_SF_NO_GRAVITY_GAP,
                W_M_SF_INACCURATE_SPLIT_MOVE,
                W_M_SF_BED_STEP_DOWN,
        };
        for (final String id : counters) {
            final WorkaroundCounter counter = new WorkaroundCounter(id);
            ws_moving.add(counter);
            setWorkaroundBluePrint(counter);
        }

        // Finally register the set.
        setWorkaroundSetByIds(WS_MOVING, getCheckedIdSet(ws_moving), G_RESET_NOTINAIR);

        // TODO: Command to log global and for players.
    }
}
