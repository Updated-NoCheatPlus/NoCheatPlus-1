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
package fr.neatmonster.nocheatplus.checks.chat;

import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.combined.CombinedData;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.ColorUtil;

public class Relog extends Check {

    public Relog() {
        super(CheckType.CHAT_RELOG);
    }

    /**
     * Check (Join), only call from synchronized code.
     * 
     * @param player
     *            the player
     * @param cc
     *            the cc
     * @param data
     *            the data
     * @return true, if successful
     */
    public boolean unsafeLoginCheck(final Player player, 
            final ChatConfig cc, final ChatData data, final IPlayerData pData) {
        boolean cancel = false;

        final long now = System.currentTimeMillis();

        final CombinedData cData = pData.getGenericInstance(CombinedData.class);

        // Enforce the player does not relog too fast.
        if (now - cData.lastLogoutTime < cc.relogTimeout) {
            if (now - data.relogWarningTime > cc.relogWarningTimeout)
                data.relogWarnings = 0;
            if (data.relogWarnings < cc.relogWarningNumber) {
                player.sendMessage(ColorUtil.replaceColors(cc.relogWarningMessage));
                data.relogWarningTime = now;
                data.relogWarnings++;
            } 
            else {
                // Find out if we need to ban the player or not.
                data.relogVL += 1D;
                cancel = executeActions(player, (double) data.relogVL, 1D, cc.relogActions).willCancel();
            }
        }
        // TODO: decrease relog vl ?

        return cancel;
    }

}
