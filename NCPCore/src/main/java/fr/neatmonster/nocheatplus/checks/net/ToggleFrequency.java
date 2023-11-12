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
package fr.neatmonster.nocheatplus.checks.net;

import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.TickTask;

/**
 * The ToggleFrequency check is used to detect players who toggle certain actions too quickly (i.e.: sneaking).
 */
public class ToggleFrequency extends Check {

    /**
     * Instantiates a new toggle frequency check.
     */
    public ToggleFrequency() {
        super(CheckType.NET_TOGGLEFREQUENCY);
    }

    /**
     * Checks a player.
     * 
     * @param player
     *            the player
     * @param data
     * @param cc
     * @param pData
     * @return true, if successful
     */
    public boolean check(final Player player, final NetData data, final NetConfig cc, final IPlayerData pData) {
        boolean cancel = false;
        // (Additium to frequency is done in the listener for each toggle event)
        // Full time resolution
        final long actionFullTime = data.playerActionFreq.bucketDuration() * data.playerActionFreq.numberOfBuckets();
        /** Toggle action events counted */
        final float fullActionScore = data.playerActionFreq.score(1f) / TickTask.getLag(actionFullTime);
        final double violation = fullActionScore > cc.toggleActionLimit ? fullActionScore - cc.toggleActionLimit : 0.0;
        if (violation > 0.0) {
            data.toggleFrequencyVL += violation;
            cancel = executeActions(player, data.toggleFrequencyVL, violation, cc.toggleFrequencyActions).willCancel();
        }
        else data.toggleFrequencyVL *= 0.9;
        return cancel;
    }
}