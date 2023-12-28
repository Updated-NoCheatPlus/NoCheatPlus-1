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
package fr.neatmonster.nocheatplus.checks.moving.model;

import org.bukkit.configuration.ConfigurationSection;

import fr.neatmonster.nocheatplus.config.ConfPaths;

/**
 * Parameters for actual flying (context dependent).
 * @author asofold
 *
 */
public class ModelFlying {

    /** The horizontal per-tick distance in creative mode. */
    public static final double HORIZONTAL_SPEED = 0.6D;
    /** The vertical per-tick distance in creative mode. */
    public static final double VERTICAL_ASCEND_SPEED = 1D;

    /**
     * The id for this model. The default model id is null.
     */
    private final String id;

    /** Modifier for horizontal flying speed in per cent. */
    private double horizontalModSpeed = 100.0;
    /**
     * Additional modifier for horizontal flying speed (multiplier), when
     * sprinting.
     */
    private double horizontalModSprint = 1.92;
    /** Modifier for vertical flying speed in per cent, for ascending. */
    private double verticalAscendModSpeed = 100.0;
    /** Maximum flying height above the maximum building height of the map. */
    private double maxHeight = 128;
    /** Apply modifiers like sprint, flyspeed, walkspeed, potions. */
    private boolean applyModifiers = true;
    /** Allow falling with gravity, including friction. */
    private boolean gravity = true;
    /** Default ground moving mechanics (jump, lost ground). */
    private boolean ground = true;

    // TODO: vertical ascend/descend, limit gain a/d/v, limit abs. distance a/d/v
    // TODO: possibly other friction based envelope constraints.
    // TODO: Check if needed: use fly/walk speed.

    private boolean locked = false;

    /**
     * Default model with null id.
     */
    public ModelFlying() {
        this(null);
    }

    public ModelFlying(String id) {
        this.id = id;
    }

    /**
     * Initialize all settings from the given configuration with the given path
     * prefix. Call lock() in order to prevent further changes.
     * 
     * @param id
     * @param config
     * @param prefix
     *            (typically should include a dot, if it's not empty)
     * @param defaults
     *            Default settings to use, if there is no value set for a
     *            certain property.
     */
    public ModelFlying(String id, ConfigurationSection config, String prefix, ModelFlying defaults) {
        // For differing models, some paths may or may not be set in the default configuration.
        this(id);
        horizontalModSpeed(config.getDouble(prefix + ConfPaths.SUB_HORIZONTAL_SPEED, defaults.getHorizontalModSpeed()));
        horizontalModSprint(config.getDouble(prefix + ConfPaths.SUB_HORIZONTAL_MODSPRINT, defaults.getHorizontalModSprint()));
        verticalAscendModSpeed(config.getDouble(prefix + ConfPaths.SUB_VERTICAL_ASCEND_SPEED, defaults.getVerticalAscendModSpeed()));
        maxHeight(config.getDouble(prefix + ConfPaths.SUB_VERTICAL_MAXHEIGHT, defaults.getMaxHeight()));
        applyModifiers(config.getBoolean(prefix + ConfPaths.SUB_MODIFIERS, defaults.getApplyModifiers()));
        gravity(config.getBoolean(prefix + ConfPaths.SUB_VERTICAL_GRAVITY, defaults.getGravity()));
        ground(config.getBoolean(prefix + ConfPaths.SUB_GROUND, defaults.getGround()));
    }

    /**
     * Initialize from another model. Call lock() in order to prevent further
     * changes.
     * 
     * @param id
     * @param defaults
     */
    public ModelFlying(String id, ModelFlying defaults) {
        this(id);
        horizontalModSpeed(defaults.getHorizontalModSpeed());
        horizontalModSprint(defaults.getHorizontalModSprint());
        verticalAscendModSpeed(defaults.getVerticalAscendModSpeed());
        maxHeight(defaults.getMaxHeight());
        applyModifiers(defaults.getApplyModifiers());
        gravity(defaults.getGravity());
        ground(defaults.getGround());
    }

    /**
     * Call to prevent further changes.
     * 
     * @return
     */
    public ModelFlying lock() {
        checkLocked();
        locked = true;
        return this;
    }

    /**
     * Test if the model is locked against changes.
     * 
     * @return
     */
    public boolean isLocked() {
        return locked;
    }

    private void checkLocked() {
        if (locked) {
            throw new RuntimeException("The model has been locked against changes.");
        }
    }

    public String getId() {
        return id;
    }

    public double getHorizontalModSpeed() {
        return horizontalModSpeed;
    }

    public double getHorizontalModSprint() {
        return horizontalModSprint;
    }

    public double getVerticalAscendModSpeed() {
        return verticalAscendModSpeed;
    }

    public double getMaxHeight() {
        return maxHeight;
    }

    public boolean getApplyModifiers() {
        return applyModifiers;
    }

    public boolean getGravity() {
        return gravity;
    }

    public boolean getGround() {
        return ground;
    }


    public ModelFlying horizontalModSpeed(double horizontalModSpeed) {
        checkLocked();
        this.horizontalModSpeed = horizontalModSpeed;
        return this;
    }

    public ModelFlying horizontalModSprint(double horizontalModSprint) {
        checkLocked();
        this.horizontalModSprint = horizontalModSprint;
        return this;
    }

    public ModelFlying verticalAscendModSpeed(double verticalAscendModSpeed) {
        checkLocked();
        this.verticalAscendModSpeed = verticalAscendModSpeed;
        return this;
    }

    public ModelFlying maxHeight(double maxHeight) {
        checkLocked();
        this.maxHeight = maxHeight;
        return this;
    }

    public ModelFlying applyModifiers(boolean applyModifiers) {
        checkLocked();
        this.applyModifiers = applyModifiers;
        return this;
    }

    public ModelFlying gravity(boolean gravity) {
        checkLocked();
        this.gravity = gravity;
        return this;
    }

    public ModelFlying ground(boolean ground) {
        checkLocked();
        this.ground = ground;
        return this;
    }
}
