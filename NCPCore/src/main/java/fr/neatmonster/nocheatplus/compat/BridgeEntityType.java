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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.EntityType;

import fr.neatmonster.nocheatplus.utilities.StringUtil;

public class BridgeEntityType {
    /** Actual lower case name to Material map for all existing materials. */
    private static final Map<String, EntityType> all = new HashMap<String, EntityType>();
    static {
        for (EntityType type : EntityType.values()) {
            String name = type.name().toLowerCase(Locale.ROOT);
            all.put(name, type);
        }
    }
    
    public static EntityType getFirst(String... names) {
        for (String name : names) {
            final EntityType type = get(name);
            if (type != null) {
                return type;
            }
        }
        return null;
    }
    
    public static EntityType get(String name) {
        return all.get(name.toLowerCase());
    }
    
    public static EntityType getFirstNotNull(String... names) {
        final EntityType type = getFirst(names);
        if (type == null) {
            throw new NullPointerException("EntityType not present: " + StringUtil.join(names, ", "));
        }
        else {
            return type;
        }
    }
    
    public static final EntityType EYE_OF_ENDER = getFirstNotNull("eye_of_ender", "ender_signal");
    public static final EntityType EXPERIENCE_BOTTLE = getFirstNotNull("experience_bottle", "thrown_exp_bottle");
    public static final EntityType SPLASH_POTION = getFirstNotNull("potion", "splash_potion");
    
    public static final EntityType FIREWORK = getFirstNotNull("firework", "firework_rocket");
    public static final EntityType FISHING_HOOK = getFirstNotNull("fishing_hook", "fishing_bobber");
    public static final EntityType ITEM = getFirstNotNull("dropped_item", "item");
    public static final EntityType SNOW_GOLEM = getFirstNotNull("snowman", "snow_golem");
    public static final EntityType TNT = getFirstNotNull("primed_tnt", "tnt");
    public static final EntityType MUSHROOM_COW = getFirst("mushroom_cow");
    public static final EntityType ENDER_CRYSTAL = getFirstNotNull("ender_crystal", "end_crystal");
    public static final EntityType LEASH_KNOT = getFirst("leash_hitch", "leash_knot");
    public static final EntityType ZOMBIFIED_PIGLIN = getFirstNotNull("pig_zombie", "zombified_piglin");
    public static final EntityType WIND_CHARGE = getFirst("wind_charge");
    
    public static final Map<EntityType, Double> LEGACY_ENTITY_WIDTH = init();
    
    private static Map<EntityType, Double> init() {
        Map<EntityType, Double> map = new HashMap<EntityType, Double>();
        map.put(EYE_OF_ENDER, 0.25);
        map.put(FIREWORK, 0.25);
        map.put(FISHING_HOOK, 0.25);
        map.put(ITEM, 0.25);
        if (MUSHROOM_COW != null) map.put(MUSHROOM_COW, 0.25);
        map.put(TNT, 0.98);
        map.put(SNOW_GOLEM, 0.4);
        map.put(ENDER_CRYSTAL, 2.0);
        if (LEASH_KNOT != null) map.put(LEASH_KNOT, 0.5);
        return map;
    }
    
    public static final Set<EntityType> PROJECTILE_CHECK_LIST = Collections.unmodifiableSet(
            new HashSet<EntityType>(Arrays.asList(
                    EntityType.ENDER_PEARL,
                    EntityType.EGG,
                    EntityType.SNOWBALL,
                    EYE_OF_ENDER,
                    EXPERIENCE_BOTTLE,
                    SPLASH_POTION))
            
            );
}
