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
package fr.neatmonster.nocheatplus.penalties.fight;

import org.bukkit.event.entity.EntityDamageEvent;

import fr.neatmonster.nocheatplus.compat.bukkit.BridgeHealth;

/**
 * Multiply the final damage by a set amount.
 * 
 * @author asofold
 *
 */
public class FightPenaltyMultiplyDamage extends FightPenaltyEntityDamage {

    private final double multiplier;

    public FightPenaltyMultiplyDamage(final double multiplier) {
        super();
        this.multiplier = multiplier;
    }

    @Override
    public boolean apply(final EntityDamageEvent event) {
        BridgeHealth.multiplyFinalDamage(event, multiplier);
        return true;
    }

}
