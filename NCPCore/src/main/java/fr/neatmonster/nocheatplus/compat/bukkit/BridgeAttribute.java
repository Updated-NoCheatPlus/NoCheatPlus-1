package fr.neatmonster.nocheatplus.compat.bukkit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.attribute.Attribute;

import fr.neatmonster.nocheatplus.utilities.StringUtil;

/**
 * Utility class providing static methods to bridge compatibility issues
 * arising from changes in Bukkit attributes between Minecraft versions,
 * specifically from MC 1.21.2 to 1.21.3.
 */
public class BridgeAttribute {
    
    // Holds all attributes, mapped by their lowercase names for easy lookup.
    private static final Map<String, Attribute> all = new HashMap<String, Attribute>();
    static {
        for (Attribute attribute : Arrays.asList(Attribute.values())) {
            String name = attribute.name().toLowerCase(Locale.ROOT);
            all.put(name, attribute);
        }
    }
    
    /**
     * Retrieves an attribute by its name (case-insensitive).
     *
     * @param name The name of the attribute to retrieve.
     * @return The corresponding {@link Attribute} if found, or {@code null} if not found.
     */
    public static Attribute get(String name) {
        return all.get(name.toLowerCase());
    }
    
    /**
     * Finds the first attribute that matches any of the provided names.
     *
     * @param names The names to search for, in order of priority.
     * @return The first matching {@link Attribute}, or {@code null} if none are found.
     */
    public static Attribute getFirst(String... names) {
        for (String name : names) {
            final Attribute type = get(name);
            if (type != null) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Finds the first attribute from the provided names that is not null.
     * Throws a {@link NullPointerException} if none are found.
     *
     * @param names The names to search for, in order of priority.
     * @return The first matching {@link Attribute}.
     * @throws NullPointerException if no matching attribute is found.
     */
    public static Attribute getFirstNotNull(String... names) {
        final Attribute attribute = getFirst(names);
        if (attribute == null) {
            throw new NullPointerException("Attribute not present: " + StringUtil.join(names, ", "));
        }
        return attribute;
    }
    
    public static final Attribute GRAVITY = getFirstNotNull("gravity", "generic_gravity");
    public static final Attribute MOVEMENT_SPEED = getFirstNotNull("movement_speed", "generic_movement_speed");
    public static final Attribute SCALE = getFirstNotNull("scale", "generic_scale");
    public static final Attribute MINING_EFFICIENCY = getFirstNotNull("mining_efficiency", "player_mining_efficiency");
    public static final Attribute SUBMERGED_MINING_SPEED = getFirstNotNull("submerged_mining_speed", "player_submerged_mining_speed");
    public static final Attribute MOVEMENT_EFFICIENCY = getFirstNotNull("movement_efficiency", "generic_movement_efficiency");
    public static final Attribute WATER_MOVEMENT_EFFICIENCY = getFirstNotNull("water_movement_efficiency", "generic_water_movement_efficiency");
    public static final Attribute STEP_HEIGHT = getFirstNotNull("step_height", "generic_step_height");
    public static final Attribute INTERACTION_RANGE = getFirstNotNull("block_interaction_range", "player_block_interaction_range");
    public static final Attribute ATTACK_RANGE = getFirstNotNull("entity_interaction_range", "player_entity_interaction_range");
    public static final Attribute SNEAKING_SPEED = getFirstNotNull("sneaking_speed", "player_sneaking_speed");
    public static final Attribute JUMP_STRENGTH = getFirstNotNull("jump_strength", "generic_jump_strength"); 
    public static final Attribute BLOCK_BREAK_SPEED = getFirstNotNull("block_break_speed", "player_block_break_speed");
    public static final Attribute FALL_DAMAGE_MULTIPLIER = getFirstNotNull("fall_damage_multiplier", "generic_fall_damage_multiplier");
    public static final Attribute SAFE_FALL_DISTANCE = getFirstNotNull("safe_fall_distance", "generic_safe_fall_distance");
}
