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

import org.bukkit.potion.PotionEffectType;

import fr.neatmonster.nocheatplus.utilities.StringUtil;

public class BridgePotionEffect {
    @SuppressWarnings("deprecation")
    private static final PotionEffectType parsePotionEffect(final String name) {
        try {
            return PotionEffectType.getByName(name);
        } catch (Exception e) {
            return null;
        }
    }

    public static PotionEffectType getFirst(String... names) {
        for (String name : names) {
            final PotionEffectType type = parsePotionEffect(name);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    public static PotionEffectType getFirstNotNull(String... names) {
        final PotionEffectType type = getFirst(names);
        if (type == null) {
            throw new NullPointerException("PotionEffect not present: " + StringUtil.join(names, ", "));
        }
        else {
            return type;
        }
    }

    public final static PotionEffectType SLOWNESS = getFirstNotNull("SLOWNESS", "SLOW");
    public final static PotionEffectType HASTE = getFirstNotNull("HASTE", "FAST_DIGGING");
    public final static PotionEffectType MINING_FATIGUE = getFirstNotNull("MINING_FATIGUE", "SLOW_DIGGING");
    public final static PotionEffectType JUMP_BOOST = getFirstNotNull("JUMP_BOOST", "JUMP");
}
