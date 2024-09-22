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

import java.util.UUID;

import org.bukkit.NamespacedKey;

import fr.neatmonster.nocheatplus.utilities.IdUtil;

/**
 * The Class AttribUtil.
 */
public class AttribUtil {
    
    /** The Constant ID_SPRINT_BOOST. */
    public static final UUID ID_SPRINT_BOOST = IdUtil.UUIDFromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");
    /** The Constant ID_SOUL_SPEED. */
    public static final UUID ID_SOUL_SPEED = IdUtil.UUIDFromString("87f46a96-686f-4796-b035-22e16ee9e038");
    /** The Constant ID_POWDER_SNOW. */
    public static final UUID ID_POWDER_SNOW = IdUtil.UUIDFromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce");
    public static final NamespacedKey NSID_SPRINT_BOOST;
    
    static {
        NamespacedKey springBoost;
        try {
            springBoost = NamespacedKey.minecraft("sprinting");
        } catch (NoClassDefFoundError ignored) {
            springBoost = null;
        }
        NSID_SPRINT_BOOST = springBoost;
    }
    
    /**
     * Get a multiplier for an AttributeModifier.
     *
     * @param operator
     *            Exclusively allows operator 2. Otherwise will throw an
     *            IllegalArgumentException.
     * @param value
     *            The modifier value (AttributeModifier).
     * @return A multiplier for direct use.
     * @throws IllegalArgumentException
     *             if the modifier is not 2.
     */
    public static double getMultiplier(final int operator, final double value) {
        // TODO: Might allow 1 too, as it should "work", despite less accurate.
        switch(operator) {
            case 2:
                return 1.0 + value;
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }

}
