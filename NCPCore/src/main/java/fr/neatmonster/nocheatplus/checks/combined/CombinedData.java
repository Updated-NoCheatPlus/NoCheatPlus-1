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
package fr.neatmonster.nocheatplus.checks.combined;

import java.util.Collection;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.access.ACheckData;
import fr.neatmonster.nocheatplus.components.data.IDataOnRemoveSubCheckData;
import fr.neatmonster.nocheatplus.utilities.PenaltyTime;
import fr.neatmonster.nocheatplus.utilities.ds.count.ActionFrequency;

public class CombinedData extends ACheckData implements IDataOnRemoveSubCheckData {

    // VLs
    public double improbableVL = 0;
    public double munchHausenVL = 0;

    // Invulnerable management:
    /** This is the tick from which on the player is vulnerable again. */
    public int invulnerableTick = Integer.MIN_VALUE;

    // Yawrate check.
    public float lastYaw;
    public long  lastYawTime;
    public float sumYaw;
    public String lastWorld = "";
    public long lastJoinTime;
    public long lastLogoutTime;
    public long lastMoveTime;
    public final ActionFrequency yawFreq = new ActionFrequency(3, 333);

    // General penalty time. Used for fighting mainly, but not only close combat (!), set by yawrate check.
    public final PenaltyTime timeFreeze = new PenaltyTime();

    // Improbable check
    public final ActionFrequency improbableCount = new ActionFrequency(20, 3000); // 20 buckets covering 3 seconds each (full time resolution of 1 minute)

    // *----------No slowdown related data----------*
    /** Whether the player use the item on left hand */
    public boolean offHandUse = false;
    /** Pre check condition */
    public boolean mightUseItem = false;
    /** TODO: */
    public long releaseItemTime = 0;
    /** Detection flag */
    public boolean isHackingRI = false;
    
    /**
     * Reduce Improbable's data by the given amount, capped at a minimum of 0.
     * @param amount
     */
    public void relaxImprobableData(final float amount) {
        ActionFrequency.reduce(System.currentTimeMillis(), amount, improbableCount);
    }

    /**
     * Hard reset Improbable's data.
     * @param amount
     */
    public void resetImprobableData() {
        improbableCount.clear(System.currentTimeMillis());
    }

    @Override
    public boolean dataOnRemoveSubCheckData(Collection<CheckType> checkTypes) {
        for (final CheckType checkType : checkTypes) {
            switch(checkType) {
                // TODO: case COMBINED:
                case COMBINED_IMPROBABLE:
                    improbableVL = 0;
                    improbableCount.clear(System.currentTimeMillis()); // TODO: Document there, which to use.
                    break;
                case COMBINED_YAWRATE:
                    yawFreq.clear(System.currentTimeMillis()); // TODO: Document there, which to use.
                    break;
                case COMBINED_MUNCHHAUSEN:
                    munchHausenVL = 0;
                    break;
                case COMBINED:
                    return true;
                default:
                    break;
            }
        }
        return false;
    }

}
