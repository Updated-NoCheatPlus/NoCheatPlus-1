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
package fr.neatmonster.nocheatplus.checks;

import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.permissions.RegisteredPermission;

/**
 * Type of checks (containing configuration and dataFactory classes, name and
 * permission).
 */
public enum CheckType {
    ALL(CheckTypeType.SPECIAL, null, Permissions.CHECKS, ConfPaths.CHECKS_ACTIVE),

    BLOCKBREAK(CheckTypeType.GROUP, CheckType.ALL, Permissions.BLOCKBREAK),
    /**
     * This will allow breaking all special blocks, currently only liquid. Later
     * there might be more sub-types.
     */
    BLOCKBREAK_BREAK(CheckTypeType.CHECK, BLOCKBREAK, Permissions.BLOCKBREAK_BREAK),
    BLOCKBREAK_DIRECTION(CheckTypeType.CHECK, BLOCKBREAK, Permissions.BLOCKBREAK_DIRECTION),
    BLOCKBREAK_FASTBREAK(CheckTypeType.CHECK, BLOCKBREAK, Permissions.BLOCKBREAK_FASTBREAK),
    BLOCKBREAK_FREQUENCY(CheckTypeType.CHECK, BLOCKBREAK, Permissions.BLOCKBREAK_FREQUENCY),
    BLOCKBREAK_NOSWING(CheckTypeType.CHECK, BLOCKBREAK, Permissions.BLOCKBREAK_NOSWING),
    BLOCKBREAK_REACH(CheckTypeType.CHECK, BLOCKBREAK, Permissions.BLOCKBREAK_REACH),
    BLOCKBREAK_WRONGBLOCK(CheckTypeType.CHECK, BLOCKBREAK, Permissions.BLOCKBREAK_WRONGBLOCK),

    BLOCKINTERACT(CheckTypeType.GROUP, CheckType.ALL, Permissions.BLOCKINTERACT),
    BLOCKINTERACT_DIRECTION(CheckTypeType.CHECK, BLOCKINTERACT, Permissions.BLOCKINTERACT_DIRECTION),
    BLOCKINTERACT_REACH(CheckTypeType.CHECK, BLOCKINTERACT, Permissions.BLOCKINTERACT_REACH),
    BLOCKINTERACT_SPEED(CheckTypeType.CHECK, BLOCKINTERACT, Permissions.BLOCKINTERACT_SPEED),
    BLOCKINTERACT_VISIBLE(CheckTypeType.CHECK, BLOCKINTERACT, Permissions.BLOCKINTERACT_VISIBLE),

    BLOCKPLACE(CheckTypeType.GROUP, CheckType.ALL, Permissions.BLOCKPLACE),
    BLOCKPLACE_AGAINST(CheckTypeType.CHECK, BLOCKPLACE, Permissions.BLOCKPLACE_AGAINST),
    BLOCKPLACE_AUTOSIGN(CheckTypeType.CHECK, BLOCKPLACE, Permissions.BLOCKPLACE_AUTOSIGN),
    BLOCKPLACE_DIRECTION(CheckTypeType.CHECK, BLOCKPLACE, Permissions.BLOCKPLACE_DIRECTION),
    BLOCKPLACE_FASTPLACE(CheckTypeType.CHECK, BLOCKPLACE, Permissions.BLOCKPLACE_FASTPLACE),
    BLOCKPLACE_NOSWING(CheckTypeType.CHECK, BLOCKPLACE, Permissions.BLOCKPLACE_NOSWING),
    BLOCKPLACE_REACH(CheckTypeType.CHECK, BLOCKPLACE, Permissions.BLOCKBREAK_REACH),
    BLOCKPLACE_SCAFFOLD(CheckTypeType.CHECK, BLOCKPLACE, Permissions.BLOCKBREAK_REACH),
    BLOCKPLACE_SPEED(CheckTypeType.CHECK, BLOCKPLACE, Permissions.BLOCKPLACE_SPEED),

    CHAT(CheckTypeType.GROUP, CheckType.ALL, Permissions.CHAT),
    CHAT_CAPTCHA(CheckTypeType.CHECK, CHAT, Permissions.CHAT_CAPTCHA),
    CHAT_COMMANDS(CheckTypeType.CHECK, CHAT, Permissions.CHAT_COMMANDS),
    CHAT_TEXT(CheckTypeType.CHECK, CHAT, Permissions.CHAT_TEXT),
    CHAT_LOGINS(CheckTypeType.CHECK, CHAT, Permissions.CHAT_LOGINS),
    CHAT_RELOG(CheckTypeType.CHECK, CHAT, Permissions.CHAT_RELOG),


    COMBINED(CheckTypeType.GROUP, CheckType.ALL, Permissions.COMBINED),
    COMBINED_IMPROBABLE(CheckTypeType.CHECK, COMBINED, Permissions.COMBINED_IMPROBABLE),
    COMBINED_MUNCHHAUSEN(CheckTypeType.CHECK, COMBINED, Permissions.COMBINED_MUNCHHAUSEN),
    /** Rather for data removal and exemption. */
    COMBINED_YAWRATE(CheckTypeType.CHECK, COMBINED),

    FIGHT(CheckTypeType.CHECK, CheckType.ALL, Permissions.FIGHT),
    FIGHT_ANGLE(CheckTypeType.CHECK, FIGHT, Permissions.FIGHT_ANGLE),
    FIGHT_CRITICAL(CheckTypeType.CHECK, FIGHT, Permissions.FIGHT_CRITICAL),
    FIGHT_DIRECTION(CheckTypeType.CHECK, FIGHT, Permissions.FIGHT_DIRECTION),
    FIGHT_FASTHEAL(CheckTypeType.CHECK, FIGHT, Permissions.FIGHT_FASTHEAL),
    FIGHT_GODMODE(CheckTypeType.CHECK, FIGHT, Permissions.FIGHT_GODMODE),
    FIGHT_NOSWING(CheckTypeType.CHECK, FIGHT, Permissions.FIGHT_NOSWING),
    FIGHT_REACH(CheckTypeType.CHECK, FIGHT, Permissions.FIGHT_REACH),
    FIGHT_SELFHIT(CheckTypeType.CHECK, FIGHT, Permissions.FIGHT_SELFHIT),
    FIGHT_VISIBLE(CheckTypeType.CHECK, FIGHT, Permissions.FIGHT_VISIBLE),

    INVENTORY(CheckTypeType.GROUP, CheckType.ALL, Permissions.INVENTORY),
    INVENTORY_FASTCLICK(CheckTypeType.CHECK, INVENTORY, Permissions.INVENTORY_FASTCLICK),
    INVENTORY_FASTCONSUME(CheckTypeType.CHECK, INVENTORY, Permissions.INVENTORY_FASTCONSUME),
    INVENTORY_GUTENBERG(CheckTypeType.CHECK, INVENTORY, Permissions.INVENTORY_GUTENBERG),
    INVENTORY_INSTANTBOW(CheckTypeType.CHECK, INVENTORY, Permissions.INVENTORY_INSTANTBOW),
    INVENTORY_MOREINVENTORY(CheckTypeType.CHECK, INVENTORY, Permissions.INVENTORY_MOREINVENTORY),
    INVENTORY_OPEN(CheckTypeType.CHECK, INVENTORY, Permissions.INVENTORY_OPEN),

    MOVING(CheckTypeType.GROUP, CheckType.ALL, Permissions.MOVING),
    MOVING_CREATIVEFLY(CheckTypeType.CHECK, MOVING, Permissions.MOVING_CREATIVEFLY),
    MOVING_MOREPACKETS(CheckTypeType.CHECK, MOVING, Permissions.MOVING_MOREPACKETS),
    MOVING_NOFALL(CheckTypeType.CHECK, MOVING, Permissions.MOVING_NOFALL),
    MOVING_PASSABLE(CheckTypeType.CHECK, MOVING, Permissions.MOVING_PASSABLE),
    MOVING_SURVIVALFLY(CheckTypeType.CHECK, MOVING, Permissions.MOVING_SURVIVALFLY),
    MOVING_VEHICLE(CheckTypeType.GROUP, MOVING, Permissions.MOVING_VEHICLE),
    MOVING_VEHICLE_MOREPACKETS(CheckTypeType.CHECK, MOVING_VEHICLE, Permissions.MOVING_VEHICLE_MOREPACKETS),
    MOVING_VEHICLE_ENVELOPE(CheckTypeType.CHECK, MOVING_VEHICLE, Permissions.MOVING_VEHICLE_ENVELOPE),

    NET(CheckTypeType.GROUP, CheckType.ALL, Permissions.NET),
    NET_ATTACKFREQUENCY(CheckTypeType.CHECK, NET, Permissions.NET_ATTACKFREQUENCY),
    NET_FLYINGFREQUENCY(CheckTypeType.CHECK, NET, Permissions.NET_FLYINGFREQUENCY),
    NET_KEEPALIVEFREQUENCY(CheckTypeType.CHECK, NET, Permissions.NET_KEEPALIVEFREQUENCY),
    NET_MOVING(CheckTypeType.CHECK, NET, Permissions.NET_MOVING),
    NET_PACKETFREQUENCY(CheckTypeType.CHECK, NET, Permissions.NET_PACKETFREQUENCY),
    NET_SOUNDDISTANCE(CheckTypeType.CHECK, NET), // Can not exempt players from this one.
    NET_TOGGLEFREQUENCY(CheckTypeType.CHECK, NET),
    NET_WRONGTURN(CheckTypeType.CHECK, NET, Permissions.NET_WRONGTURN)
    ;

    public static enum CheckTypeType {
        /** Special types, like ALL */
        SPECIAL,
        /** Potentially obsolete: A check group that is not a check itself. */
        GROUP,
        /** An actual check. Could in future still have sub checks. */
        CHECK
    }

    /** The type of the check type. */
    private final CheckTypeType type;

    /** If not null, this is the check group usually. */
    private final CheckType parent;

    /** The bypass permission. */
    private final RegisteredPermission permission;

    /** Configuration path for the active flag. */
    private final String configPathActive;

    /** Configuration path for the debug flag. */
    private final String configPathDebug;

    /** Configuration path for the lag flag. */
    private final String configPathLag;

    /**
     * Constructor for checks or groups grouped under another check type,
     * without having a permission set, with default activation flag path.
     * 
     * @param parent
     */
    private CheckType(final CheckTypeType type, final CheckType parent) {
        this(type, parent, null);
    }

    /**
     * General constructor
     * 
     * @param type
     *            The type of the check type.
     * @param parent
     *            Super check type (usually the group).
     * @param permission
     *            Bypass permission.
     */
    private CheckType(final CheckTypeType type, final CheckType parent, 
            final RegisteredPermission permission) {
        this(type, parent, permission, null);
    }

    /**
     * General constructor (bottom).
     * 
     * @param type
     * @param parent
     * @param permission
     * @param configPathActive
     */
    private CheckType(final CheckTypeType type, final CheckType parent, 
            final RegisteredPermission permission, 
            final String configPathActive) {
        this.type = type;
        this.parent = parent;
        this.permission = permission;
        this.configPathActive = configPathActive == null ? guessConfigPathActive() : configPathActive ;
        this.configPathDebug = guessConfigPath(this.configPathActive, ConfPaths.SUB_DEBUG);
        this.configPathLag = guessConfigPath(this.configPathActive, ConfPaths.SUB_LAG);
    }

    private String guessConfigPathActive() {
        return guessConfigPathRoot() + ConfPaths.SUB_ACTIVE;
    }

    private String guessConfigPath(String configPathActive, String suffix) {
        final int index = configPathActive.lastIndexOf(".");
        return index == -1 ? suffix: configPathActive.substring(0, index + 1) + suffix;
    }

    private String guessConfigPathRoot() {
        return "checks." + name().toLowerCase().replace('_', '.') + ".";
    }

    public CheckTypeType getType() {
        return type;
    }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
        return toString().toLowerCase().replace("_", ".");
    }

    /**
     * Gets the parent.
     * 
     * @return the parent
     */
    public CheckType getParent() {
        return parent;
    }

    /**
     * Gets the bypass permission for this check type.
     * 
     * @return the permission
     */
    public RegisteredPermission getPermission() {
        return permission;
    }

    /**
     * Return the configuration path for the activation flag.
     * 
     * @return
     */
    public String getConfigPathActive() {
        return configPathActive;
    }

    public String getConfigPathDebug() {
        return configPathDebug;
    }

    public String getConfigPathLag() {
        return configPathLag;
    }
}
