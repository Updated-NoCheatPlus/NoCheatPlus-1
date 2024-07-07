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
package fr.neatmonster.nocheatplus.permissions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The various permission nodes used by NoCheatPlus.
 */
public class Permissions {

    /** Next id to use. */
    private static int id = 1;
    private static final Map<String, RegisteredPermission> permissions = new LinkedHashMap<String, RegisteredPermission>();

    private static final RegisteredPermission add(String stringRepresentation) {
        // TODO: Other concept / lock (?) once the plugin has fetched all on load.
        RegisteredPermission permission = new RegisteredPermission(id, stringRepresentation);
        if (permissions.containsKey(permission.getLowerCaseStringRepresentation())) {
            return permissions.get(permission.getLowerCaseStringRepresentation());
        }
        else {
            id++;
            permissions.put(permission.getLowerCaseStringRepresentation(), permission);
            return permission;
        }
    }

    private static final String NOCHEATPLUS                                = "nocheatplus";

    // Access to all commands and debug info.
    private static final String ADMINISTRATION                             = NOCHEATPLUS + ".admin";
    public static final RegisteredPermission  ADMINISTRATION_DEBUG         = add(ADMINISTRATION + ".debug");

    // Bypasses held extra from command permissions.
    private static final String BYPASS                                     = NOCHEATPLUS + ".bypass";
    public static final RegisteredPermission BYPASS_DENY_LOGIN             = add(BYPASS + "denylogin");

    // Bypass command filter.
    private static final String FILTER                                     = NOCHEATPLUS + ".filter";
    public static final RegisteredPermission  FILTER_COMMAND               = add(FILTER + ".command");
    public static final RegisteredPermission  FILTER_COMMAND_NOCHEATPLUS   = add(FILTER_COMMAND + ".nocheatplus");

    // Notifications (in-game).
    public static final RegisteredPermission  NOTIFY                       = add(NOCHEATPLUS + ".notify");

    // Command permissions.
    public static final RegisteredPermission  COMMAND                      = add(NOCHEATPLUS + ".command");
    public static final RegisteredPermission  COMMAND_COMMANDS             = add(COMMAND + ".commands");
    public static final RegisteredPermission  COMMAND_DEBUG                = add(COMMAND + ".debug");
    public static final RegisteredPermission  COMMAND_EXEMPT               = add(COMMAND + ".exempt");
    public static final RegisteredPermission  COMMAND_EXEMPT_SELF          = add(COMMAND_EXEMPT + ".self");
    public static final RegisteredPermission  COMMAND_EXEMPTIONS           = add(COMMAND + ".exemptions");
    public static final RegisteredPermission  COMMAND_INFO                 = add(COMMAND + ".info");
    public static final RegisteredPermission  COMMAND_INSPECT              = add(COMMAND + ".inspect");
    public static final RegisteredPermission  COMMAND_LAG                  = add(COMMAND + ".lag");
    public static final RegisteredPermission  COMMAND_LOG                  = add(COMMAND + ".log");
    public static final RegisteredPermission  COMMAND_NOTIFY               = add(COMMAND + ".notify");
    public static final RegisteredPermission  COMMAND_RELOAD               = add(COMMAND + ".reload");
    public static final RegisteredPermission  COMMAND_REMOVEPLAYER         = add(COMMAND + ".removeplayer");
    public static final RegisteredPermission  COMMAND_REMOVEPLAYER_SELF    = add(COMMAND_REMOVEPLAYER + ".self");
    public static final RegisteredPermission  COMMAND_RESET                = add(COMMAND + ".reset");
    public static final RegisteredPermission  COMMAND_TOP                  = add(COMMAND + ".top");
    public static final RegisteredPermission  COMMAND_UNEXEMPT             = add(COMMAND + ".unexempt");
    public static final RegisteredPermission  COMMAND_UNEXEMPT_SELF        = add(COMMAND_UNEXEMPT + ".self");
    public static final RegisteredPermission  COMMAND_VERSION              = add(COMMAND + ".version");

    // Auxiliary Command permissions.
    public static final RegisteredPermission  COMMAND_ALLOWLOGIN           = add(COMMAND + ".allowlogin");
    public static final RegisteredPermission  COMMAND_BAN                  = add(COMMAND + ".ban");
    public static final RegisteredPermission  COMMAND_DELAY                = add(COMMAND + ".delay");
    public static final RegisteredPermission  COMMAND_DENYLOGIN            = add(COMMAND + ".denylogin");
    public static final RegisteredPermission  COMMAND_KICK                 = add(COMMAND + ".kick");
    public static final RegisteredPermission  COMMAND_KICKLIST             = add(COMMAND + ".kicklist");
    public static final RegisteredPermission  COMMAND_TELL                 = add(COMMAND + ".tell");
    public static final RegisteredPermission  COMMAND_STOPWATCH            = add(COMMAND + ".stopwatch");

    // Permissions for the individual checks.
    public static final RegisteredPermission  CHECKS                       = add(NOCHEATPLUS + ".checks");

    public static final RegisteredPermission  BLOCKBREAK                   = add(CHECKS + ".blockbreak");
    public static final RegisteredPermission  BLOCKBREAK_BREAK             = add(BLOCKBREAK + ".break");
    public static final RegisteredPermission  BLOCKBREAK_BREAK_LIQUID      = add(BLOCKBREAK_BREAK + ".liquid");
    public static final RegisteredPermission  BLOCKBREAK_DIRECTION         = add(BLOCKBREAK + ".direction");
    public static final RegisteredPermission  BLOCKBREAK_FASTBREAK         = add(BLOCKBREAK + ".fastbreak");
    public static final RegisteredPermission  BLOCKBREAK_FREQUENCY         = add(BLOCKBREAK + ".frequency");
    public static final RegisteredPermission  BLOCKBREAK_NOSWING           = add(BLOCKBREAK + ".noswing");
    public static final RegisteredPermission  BLOCKBREAK_REACH             = add(BLOCKBREAK + ".reach");
    public static final RegisteredPermission  BLOCKBREAK_WRONGBLOCK        = add(BLOCKBREAK + ".wrongblock");

    public static final RegisteredPermission  BLOCKINTERACT                = add(CHECKS + ".blockinteract");
    public static final RegisteredPermission  BLOCKINTERACT_DIRECTION      = add(BLOCKINTERACT + ".direction");
    public static final RegisteredPermission  BLOCKINTERACT_REACH          = add(BLOCKINTERACT + ".reach");
    public static final RegisteredPermission  BLOCKINTERACT_SPEED          = add(BLOCKINTERACT + ".speed");
    public static final RegisteredPermission  BLOCKINTERACT_VISIBLE        = add(BLOCKINTERACT + ".visible");

    public static final RegisteredPermission  BLOCKPLACE                   = add(CHECKS + ".blockplace");
    public static final RegisteredPermission  BLOCKPLACE_AGAINST           = add(BLOCKPLACE + ".against");
    public static final RegisteredPermission  BLOCKPLACE_AGAINST_AIR       = add(BLOCKPLACE_AGAINST + ".air");
    public static final RegisteredPermission  BLOCKPLACE_AGAINST_LIQUIDS   = add(BLOCKPLACE_AGAINST + ".liquids");
    public static final RegisteredPermission  BLOCKPLACE_AUTOSIGN          = add(BLOCKPLACE + ".autosign");
    public static final RegisteredPermission  BLOCKPLACE_BOATSONWATERONLY  = add(BLOCKPLACE + ".boatsonwateronly");
    public static final RegisteredPermission  BLOCKPLACE_DIRECTION         = add(BLOCKPLACE + ".direction");
    public static final RegisteredPermission  BLOCKPLACE_FASTPLACE         = add(BLOCKPLACE + ".fastplace");
    public static final RegisteredPermission  BLOCKPLACE_NOSWING           = add(BLOCKPLACE + ".noswing");
    public static final RegisteredPermission  BLOCKPLACE_REACH             = add(BLOCKPLACE + ".reach");
    public static final RegisteredPermission  BLOCKPLACE_SPEED             = add(BLOCKPLACE + ".speed");
    public static final RegisteredPermission  BLOCKPLACE_SCAFFOLD          = add(BLOCKPLACE + ".scaffold");


    public static final RegisteredPermission  CHAT                         = add(CHECKS + ".chat");
    public static final RegisteredPermission  CHAT_CAPTCHA                 = add(CHAT + ".captcha");
    public static final RegisteredPermission  CHAT_COMMANDS                = add(CHAT + ".commands");
    public static final RegisteredPermission  CHAT_LOGINS                  = add(CHAT + ".logins");
    public static final RegisteredPermission  CHAT_RELOG                   = add(CHAT + ".relog");
    public static final RegisteredPermission  CHAT_TEXT                    = add(CHAT + ".text");

    public static final RegisteredPermission  COMBINED                     = add(CHECKS + ".combined");
    public static final RegisteredPermission  COMBINED_IMPROBABLE          = add(COMBINED + ".improbable");
    public static final RegisteredPermission  COMBINED_MUNCHHAUSEN         = add(COMBINED + ".munchhausen");

    public static final RegisteredPermission  FIGHT                        = add(CHECKS + ".fight");
    public static final RegisteredPermission  FIGHT_ANGLE                  = add(FIGHT + ".angle");
    public static final RegisteredPermission  FIGHT_CRITICAL               = add(FIGHT + ".critical");
    public static final RegisteredPermission  FIGHT_DIRECTION              = add(FIGHT + ".direction");
    public static final RegisteredPermission  FIGHT_FASTHEAL               = add(FIGHT + ".fastheal");
    public static final RegisteredPermission  FIGHT_GODMODE                = add(FIGHT + ".godmode");
    public static final RegisteredPermission  FIGHT_NOSWING                = add(FIGHT + ".noswing");
    public static final RegisteredPermission  FIGHT_REACH                  = add(FIGHT + ".reach");
    public static final RegisteredPermission  FIGHT_SELFHIT                = add(FIGHT + ".selfhit");
    public static final RegisteredPermission  FIGHT_VISIBLE                = add(FIGHT + ".visible");

    public static final RegisteredPermission  INVENTORY                    = add(CHECKS + ".inventory");
    public static final RegisteredPermission  INVENTORY_FASTCLICK          = add(INVENTORY + ".fastclick");
    public static final RegisteredPermission  INVENTORY_FASTCONSUME        = add(INVENTORY + ".fastconsume");
    public static final RegisteredPermission  INVENTORY_GUTENBERG          = add(INVENTORY + ".gutenberg");
    public static final RegisteredPermission  INVENTORY_INSTANTBOW         = add(INVENTORY + ".instantbow");
    public static final RegisteredPermission  INVENTORY_MOREINVENTORY      = add(INVENTORY + ".moreinventory");
    public static final RegisteredPermission  INVENTORY_OPEN               = add(INVENTORY + ".open");

    public static final RegisteredPermission  NET                          = add(CHECKS + ".net");
    public static final RegisteredPermission  NET_ATTACKFREQUENCY          = add(NET + ".attackfrequency");
    public static final RegisteredPermission  NET_FLYINGFREQUENCY          = add(NET + ".flyingfrequency");
    public static final RegisteredPermission  NET_KEEPALIVEFREQUENCY       = add(NET + ".keepalivefrequency");
    public static final RegisteredPermission  NET_MOVING                   = add(NET + ".moving");
    public static final RegisteredPermission  NET_PACKETFREQUENCY          = add(NET + ".packetfrequency");
    public static final RegisteredPermission  NET_TOGGLEFREQUENCY          = add(NET + ".togglefrequency");
    public static final RegisteredPermission  NET_WRONGTURN                = add(NET + ".wrongturn");

    public static final RegisteredPermission  MOVING                       = add(CHECKS + ".moving");
    public static final RegisteredPermission  MOVING_CREATIVEFLY           = add(MOVING + ".creativefly");
    public static final RegisteredPermission  MOVING_MOREPACKETS           = add(MOVING + ".morepackets");
    public static final RegisteredPermission  MOVING_NOFALL                = add(MOVING + ".nofall");
    public static final RegisteredPermission  MOVING_PASSABLE              = add(MOVING + ".passable");
    public static final RegisteredPermission  MOVING_SURVIVALFLY           = add(MOVING + ".survivalfly");

    public static final RegisteredPermission  MOVING_VEHICLE               = add(MOVING + ".vehicle");
    public static final RegisteredPermission  MOVING_VEHICLE_MOREPACKETS   = add(MOVING_VEHICLE + ".morepackets");
    public static final RegisteredPermission  MOVING_VEHICLE_ENVELOPE      = add(MOVING_VEHICLE + ".envelope");

    /**
     * Get a new list with all permissions that have been added up to now. (As
     * long as CheckType is static/final, CheckType initialization will add to
     * here, then the plugin will fetch to create the internal
     * PermissionRegistry.)
     * 
     * @return
     */
    public static List<RegisteredPermission> getPermissions() {
        return new ArrayList<RegisteredPermission>(permissions.values());
    }

}