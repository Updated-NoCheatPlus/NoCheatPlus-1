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

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent.State;

import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;

/**
 * Very, very important check.
 * TODO: Demote this shit into a simple boolean config option...
 * @author asofold
 *
 */
public class MunchHausen extends Check {
    public MunchHausen(){
        super(CheckType.COMBINED_MUNCHHAUSEN);
    }

    public boolean checkFish(final Player player, final Entity caught, final State state) {
        if (caught == null || !(caught instanceof Player)) return false;
        final Player caughtPlayer = (Player) caught;
        final IPlayerData pData = DataManager.getPlayerData(player);
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        if (player.equals(caughtPlayer)){
            data.munchHausenVL += 1.0;
            if (executeActions(player, data.munchHausenVL, 1.0, 
                    pData.getGenericInstance(CombinedConfig.class).munchHausenActions).willCancel()){
                return true;
            }
        }
        else data.munchHausenVL *= 0.96;
        return false;
    }
}
