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
package fr.neatmonster.nocheatplus.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import fr.neatmonster.nocheatplus.config.PathUtils.ManyMoved;
import fr.neatmonster.nocheatplus.config.PathUtils.WrapMoved;

/**
 * Paths for the configuration options. Making everything final static prevents accidentally modifying any of these.
 */
public abstract class ConfPaths {

    // Sub-paths that are used with different path prefixes potentially.
    // TODO: These might better be in another class.
    public static final String SUB_ACTIVE                                = "active";
    public static final String SUB_ALLOWINSTANTBREAK                     = "allowinstantbreak";
    public static final String SUB_ASCEND                                = "ascend";
    public static final String SUB_DEBUG                                 = "debug";
    public static final String SUB_DESCEND                               = "descend";
    public static final String SUB_GRAVITY                               = "gravity";
    public static final String SUB_GROUND                                = "ground";
    public static final String SUB_HORIZONTAL                            = "horizontal";
    public static final String SUB_HORIZONTALSPEED                       = "horizontalspeed"; // Phase out.
    public static final String SUB_LAG                                   = "lag";
    public static final String SUB_MAXHEIGHT                             = "maxheight";
    public static final String SUB_MODEL                                 = "model";
    public static final String SUB_MODIFIERS                             = "modifiers";
    public static final String SUB_MODSPRINT                             = "modsprint";
    /** No trailing dot! */
    public static final String SUB_OVERRIDEFLAGS                         = "overrideflags";
    public static final String SUB_BREAKINGTIME                          = "breakingtime";
    public static final String SUB_SPEED                                 = "speed";
    public static final String SUB_VERTICAL                              = "vertical";
    public static final String SUB_VERTICALSPEED                         = "verticalspeed"; // Phase out.

    // Composite sub-paths.
    public static final String SUB_HORIZONTAL_SPEED                      = SUB_HORIZONTAL + "." + SUB_SPEED;
    public static final String SUB_HORIZONTAL_MODSPRINT                  = SUB_HORIZONTAL + "." + SUB_MODSPRINT;
    public static final String SUB_VERTICAL_ASCEND                       = SUB_VERTICAL + "." + SUB_ASCEND;
    public static final String SUB_VERTICAL_ASCEND_SPEED                 = SUB_VERTICAL_ASCEND + "." + SUB_SPEED;
    public static final String SUB_VERTICAL_DESCEND                      = SUB_VERTICAL + "." + SUB_DESCEND;
    public static final String SUB_VERTICAL_DESCEND_SPEED                = SUB_VERTICAL_DESCEND + "." + SUB_SPEED;
    public static final String SUB_VERTICAL_MAXHEIGHT                    = SUB_VERTICAL + "." + SUB_MAXHEIGHT;
    public static final String SUB_VERTICAL_GRAVITY                      = SUB_VERTICAL + "." + SUB_GRAVITY;

    // General.
    public static final String SAVEBACKCONFIG                            = "savebackconfig";

    // Configuration version.
    /*
     * NOTE: The configuration allows setLastChangeBuildNumber(path), however
     * some of these settings are still needed for that.
     */
    @GlobalConfig // TODO: Per file versions should also be supported.
    public static final String CONFIGVERSION                             = "configversion.";
    public static final String CONFIGVERSION_NOTIFY                      = CONFIGVERSION + "notify";
    public static final String CONFIGVERSION_NOTIFYMAXPATHS              = CONFIGVERSION + "notifymaxpaths";
    /** Build number of the build for which the default config was first created (DefaultConfig.buildNumber), updated with first save. */
    public static final String CONFIGVERSION_CREATED                     = CONFIGVERSION + "created";
    /** Build number of the build for which the default config was first created (DefaultConfig.buildNumber), updated with each save. */
    public static final String CONFIGVERSION_SAVED                       = CONFIGVERSION + "saved";

    @GlobalConfig
    private static final String LOGGING                                  = "logging.";
    public static final String  LOGGING_ACTIVE                           = LOGGING + SUB_ACTIVE;
    public static final String  LOGGING_MAXQUEUESIZE                     = LOGGING + "maxqueuesize";

    private static final String LOGGING_BACKEND                          = LOGGING + "backend.";
    private static final String LOGGING_BACKEND_CONSOLE                  = LOGGING_BACKEND + "console.";
    public static final String  LOGGING_BACKEND_CONSOLE_ACTIVE           = LOGGING_BACKEND_CONSOLE + SUB_ACTIVE;
    public static final String  LOGGING_BACKEND_CONSOLE_ASYNCHRONOUS     = LOGGING_BACKEND_CONSOLE + "asynchronous";
    private static final String LOGGING_BACKEND_FILE                     = LOGGING_BACKEND + "file.";
    public static final String  LOGGING_BACKEND_FILE_ACTIVE              = LOGGING_BACKEND_FILE + SUB_ACTIVE;
    public static final String  LOGGING_BACKEND_FILE_FILENAME            = LOGGING_BACKEND_FILE + "filename";
    public static final String  LOGGING_BACKEND_FILE_PREFIX              = LOGGING_BACKEND_FILE + "prefix";
    private static final String LOGGING_BACKEND_INGAMECHAT               = LOGGING_BACKEND + "ingamechat.";
    public static final String  LOGGING_BACKEND_INGAMECHAT_ACTIVE        = LOGGING_BACKEND_INGAMECHAT + SUB_ACTIVE;
    public static final String  LOGGING_BACKEND_INGAMECHAT_PREFIX        = LOGGING_BACKEND_INGAMECHAT + "prefix";

    private static final String LOGGING_EXTENDED                         = LOGGING + "extended.";
    public static final String  LOGGING_EXTENDED_STATUS                  = LOGGING_EXTENDED + "status";
    private static final String LOGGING_EXTENDED_COMMANDS                = LOGGING_EXTENDED + "commands.";
    public static final String  LOGGING_EXTENDED_COMMANDS_ACTIONS        = LOGGING_EXTENDED_COMMANDS + "actions";
    private static final String LOGGING_EXTENDED_ALLVIOLATIONS           = LOGGING_EXTENDED + "allviolations.";
    public static final String  LOGGING_EXTENDED_ALLVIOLATIONS_DEBUG     = LOGGING_EXTENDED_ALLVIOLATIONS + "debug";
    public static final String  LOGGING_EXTENDED_ALLVIOLATIONS_DEBUGONLY = LOGGING_EXTENDED_ALLVIOLATIONS + "debugonly";
    private static final String LOGGING_EXTENDED_ALLVIOLATIONS_BACKEND   = LOGGING_EXTENDED_ALLVIOLATIONS + "backend.";
    public static final String  LOGGING_EXTENDED_ALLVIOLATIONS_BACKEND_TRACE     = LOGGING_EXTENDED_ALLVIOLATIONS_BACKEND + "trace";
    public static final String  LOGGING_EXTENDED_ALLVIOLATIONS_BACKEND_NOTIFY    = LOGGING_EXTENDED_ALLVIOLATIONS_BACKEND + "notify";

    @GlobalConfig
    private static final String MISCELLANEOUS = "miscellaneous.";
    //public static final String  MISCELLANEOUS_CHECKFORUPDATES                = MISCELLANEOUS + "checkforupdates";
    //public static final String  MISCELLANEOUS_UPDATETIMEOUT                    = MISCELLANEOUS + "updatetimeout";

    /** TEMP: hidden flag to disable all lag adaption with one flag. */
    public static final String MISCELLANEOUS_LAG                         = MISCELLANEOUS + "lag";

    // Extended data-related settings.
    @GlobalConfig
    private static final String DATA                                     = "data.";
    // Expired data removal.
    private static final String DATA_EXPIRATION                          = DATA + "expiration.";
    public static final String  DATA_EXPIRATION_ACTIVE                   = DATA_EXPIRATION + SUB_ACTIVE;
    public static final String  DATA_EXPIRATION_DURATION                 = DATA_EXPIRATION + "duration";
    public static final String  DATA_EXPIRATION_DATA                     = DATA_EXPIRATION + "data";
    public static final String  DATA_EXPIRATION_HISTORY                  = DATA_EXPIRATION + "history";
    // Consistency checking.
    private static final String DATA_CONSISTENCYCHECKS                   = DATA + "consistencychecks.";
    public static final  String DATA_CONSISTENCYCHECKS_CHECK             = DATA_CONSISTENCYCHECKS + SUB_ACTIVE;
    public static final  String DATA_CONSISTENCYCHECKS_INTERVAL          = DATA_CONSISTENCYCHECKS + "interval";
    public static final  String DATA_CONSISTENCYCHECKS_MAXTIME           = DATA_CONSISTENCYCHECKS + "maxtime";
    /**
     * This might not might not be used by checks. <br>
     * Used by: DataMan/Player-instances
     * 
     */
    public static final  String DATA_CONSISTENCYCHECKS_SUPPRESSWARNINGS  = DATA_CONSISTENCYCHECKS + "suppresswarnings";

    // Permission caching setup.
    private static final String PERMISSIONS                               = "permissions.";
    private static final String PERMISSIONS_POLICY                        = PERMISSIONS + "policy.";
    public static final  String PERMISSIONS_POLICY_DEFAULT                = PERMISSIONS_POLICY + "default";
    public static final  String PERMISSIONS_POLICY_RULES                  = PERMISSIONS_POLICY + "rules";

    private static final String PROTECT                                  = "protection.";
    // Clients settings.
    private static final String PROTECT_CLIENTS                          = PROTECT + "clients.";
    @GlobalConfig
    private static final String PROTECT_CLIENTS_MOTD                     = PROTECT_CLIENTS + "motd.";
    public static final String  PROTECT_CLIENTS_MOTD_ACTIVE              = PROTECT_CLIENTS_MOTD + SUB_ACTIVE;
    public static final String  PROTECT_CLIENTS_MOTD_ALLOWALL            = PROTECT_CLIENTS_MOTD + "allowall";
    // Other commands settings
    @GlobalConfig
    private static final String PROTECT_COMMANDS                         = PROTECT + "commands.";
    private static final String PROTECT_COMMANDS_CONSOLEONLY             = PROTECT_COMMANDS + "consoleonly.";
    public  static final String PROTECT_COMMANDS_CONSOLEONLY_ACTIVE      = PROTECT_COMMANDS_CONSOLEONLY + SUB_ACTIVE;
    public  static final String PROTECT_COMMANDS_CONSOLEONLY_MSG         = PROTECT_COMMANDS_CONSOLEONLY + "message";
    public  static final String PROTECT_COMMANDS_CONSOLEONLY_CMDS        = PROTECT_COMMANDS_CONSOLEONLY + "commands";
    // Plugins settings.
    private static final String PROTECT_PLUGINS                          = PROTECT + "plugins.";
    @GlobalConfig
    private static final String PROTECT_PLUGINS_HIDE                     = PROTECT_PLUGINS + "hide.";
    public static  final String PROTECT_PLUGINS_HIDE_ACTIVE              = PROTECT_PLUGINS_HIDE + SUB_ACTIVE;
    private static final String PROTECT_PLUGINS_HIDE_NOCOMMAND           = PROTECT_PLUGINS_HIDE + "unknowncommand.";
    public static  final String PROTECT_PLUGINS_HIDE_NOCOMMAND_MSG       = PROTECT_PLUGINS_HIDE_NOCOMMAND + "message";
    public static  final String PROTECT_PLUGINS_HIDE_NOCOMMAND_CMDS      = PROTECT_PLUGINS_HIDE_NOCOMMAND + "commands";
    private static final String PROTECT_PLUGINS_HIDE_NOPERMISSION        = PROTECT_PLUGINS_HIDE + "nopermission.";
    public static  final String PROTECT_PLUGINS_HIDE_NOPERMISSION_MSG    = PROTECT_PLUGINS_HIDE_NOPERMISSION + "message";
    public static  final String PROTECT_PLUGINS_HIDE_NOPERMISSION_CMDS   = PROTECT_PLUGINS_HIDE_NOPERMISSION + "commands";

    // Checks!
    private static final String CHECKS                                   = "checks.";
    /** Debug flag to debug all checks (!), individual sections debug flags override this, if present. */
    public static final  String CHECKS_ACTIVE                            = CHECKS + SUB_ACTIVE;
    public static final  String CHECKS_LAG                               = CHECKS + SUB_LAG;
    public static final  String CHECKS_DEBUG                             = CHECKS + SUB_DEBUG;
    public static final String  BLOCKBREAK                               = CHECKS + "blockbreak.";

    public static final String  BLOCKBREAK_ACTIVE                        = BLOCKBREAK + SUB_ACTIVE;
    public static final String  BLOCKBREAK_DEBUG                         = BLOCKBREAK + "debug";


    private static final String BLOCKBREAK_DIRECTION                     = BLOCKBREAK + "direction.";
    public static final String  BLOCKBREAK_DIRECTION_CHECK               = BLOCKBREAK_DIRECTION + SUB_ACTIVE;
    public static final String  BLOCKBREAK_DIRECTION_ACTIONS             = BLOCKBREAK_DIRECTION + "actions";

    private static final String BLOCKBREAK_FASTBREAK                     = BLOCKBREAK + "fastbreak.";
    public static final String  BLOCKBREAK_FASTBREAK_CHECK               = BLOCKBREAK_FASTBREAK + SUB_ACTIVE;
    public static final String  BLOCKBREAK_FASTBREAK_STRICT                 = BLOCKBREAK_FASTBREAK + "strict";
    private static final String BLOCKBREAK_FASTBREAK_BUCKETS             = BLOCKBREAK + "buckets.";
    public static final String  BLOCKBREAK_FASTBREAK_BUCKETS_CONTENTION  = BLOCKBREAK_FASTBREAK_BUCKETS + "contention";
    @GlobalConfig
    public static final String  BLOCKBREAK_FASTBREAK_BUCKETS_N           = BLOCKBREAK_FASTBREAK_BUCKETS + "number";
    @GlobalConfig
    public static final String  BLOCKBREAK_FASTBREAK_BUCKETS_DUR         = BLOCKBREAK_FASTBREAK_BUCKETS + "duration";
    public static final String  BLOCKBREAK_FASTBREAK_BUCKETS_FACTOR      = BLOCKBREAK_FASTBREAK_BUCKETS + "factor";
    public static final String  BLOCKBREAK_FASTBREAK_DELAY               = BLOCKBREAK_FASTBREAK + "delay";
    public static final String  BLOCKBREAK_FASTBREAK_GRACE               = BLOCKBREAK_FASTBREAK + "grace";
    public static final String  BLOCKBREAK_FASTBREAK_MOD_SURVIVAL        = BLOCKBREAK_FASTBREAK + "intervalsurvival";
    public static final String  BLOCKBREAK_FASTBREAK_ACTIONS             = BLOCKBREAK_FASTBREAK + "actions";

    private static final String BLOCKBREAK_FREQUENCY                     = BLOCKBREAK + "frequency.";
    public static final String  BLOCKBREAK_FREQUENCY_CHECK               = BLOCKBREAK_FREQUENCY + SUB_ACTIVE;
    public static final String  BLOCKBREAK_FREQUENCY_MOD_CREATIVE        = BLOCKBREAK_FREQUENCY + "intervalcreative";
    public static final String  BLOCKBREAK_FREQUENCY_MOD_SURVIVAL        = BLOCKBREAK_FREQUENCY + "intervalsurvival";
    private static final String BLOCKBREAK_FREQUENCY_BUCKETS             = BLOCKBREAK_FREQUENCY + "buckets.";
    @GlobalConfig
    public static final String  BLOCKBREAK_FREQUENCY_BUCKETS_DUR         = BLOCKBREAK_FREQUENCY_BUCKETS + "duration";
    public static final String  BLOCKBREAK_FREQUENCY_BUCKETS_FACTOR      = BLOCKBREAK_FREQUENCY_BUCKETS + "factor";
    @GlobalConfig
    public static final String  BLOCKBREAK_FREQUENCY_BUCKETS_N           = BLOCKBREAK_FREQUENCY_BUCKETS + "number";
    private static final String BLOCKBREAK_FREQUENCY_SHORTTERM           = BLOCKBREAK_FREQUENCY + "shortterm.";
    public static final String  BLOCKBREAK_FREQUENCY_SHORTTERM_LIMIT     = BLOCKBREAK_FREQUENCY_SHORTTERM + "limit";
    public static final String  BLOCKBREAK_FREQUENCY_SHORTTERM_TICKS     = BLOCKBREAK_FREQUENCY_SHORTTERM + "ticks";
    public static final String  BLOCKBREAK_FREQUENCY_ACTIONS             = BLOCKBREAK_FREQUENCY + "actions";

    private static final String BLOCKBREAK_NOSWING                       = BLOCKBREAK + "noswing.";
    public static final String  BLOCKBREAK_NOSWING_CHECK                 = BLOCKBREAK_NOSWING + SUB_ACTIVE;
    public static final String  BLOCKBREAK_NOSWING_ACTIONS               = BLOCKBREAK_NOSWING + "actions";

    private static final String BLOCKBREAK_REACH                         = BLOCKBREAK + "reach.";
    public static final String  BLOCKBREAK_REACH_CHECK                   = BLOCKBREAK_REACH + SUB_ACTIVE;
    public static final String  BLOCKBREAK_REACH_ACTIONS                 = BLOCKBREAK_REACH + "actions";

    private static final String BLOCKBREAK_WRONGBLOCK                    = BLOCKBREAK + "wrongblock.";
    public static final String  BLOCKBREAK_WRONGBLOCK_CHECK              = BLOCKBREAK_WRONGBLOCK + SUB_ACTIVE;
    public static final String  BLOCKBREAK_WRONGBLOCK_LEVEL              = BLOCKBREAK_WRONGBLOCK + "level";
    private static final String BLOCKBREAK_WRONGBLOCK_IMPROBABLE         = BLOCKBREAK_WRONGBLOCK + "improbable.";
    public static final String  BLOCKBREAK_WRONGBLOCK_IMPROBABLE_FEEDONLY = BLOCKBREAK_WRONGBLOCK_IMPROBABLE + "feedonly";
    public static final String  BLOCKBREAK_WRONGBLOCK_IMPROBABLE_WEIGHT   = BLOCKBREAK_WRONGBLOCK_IMPROBABLE + "weight";
    public static final String  BLOCKBREAK_WRONGBLOCK_ACTIONS            = BLOCKBREAK_WRONGBLOCK + "actions";

    public static final String BLOCKINTERACT                             = CHECKS + "blockinteract.";
    public static final String BLOCKINTERACT_ACTIVE                      = BLOCKINTERACT + SUB_ACTIVE;

    private static final String BLOCKINTERACT_DIRECTION                  = BLOCKINTERACT + "direction.";
    public static final String  BLOCKINTERACT_DIRECTION_CHECK            = BLOCKINTERACT_DIRECTION + SUB_ACTIVE;
    public static final String  BLOCKINTERACT_DIRECTION_ACTIONS          = BLOCKINTERACT_DIRECTION + "actions";

    private static final String BLOCKINTERACT_REACH                      = BLOCKINTERACT + "reach.";
    public static final String  BLOCKINTERACT_REACH_CHECK                = BLOCKINTERACT_REACH + SUB_ACTIVE;
    public static final String  BLOCKINTERACT_REACH_ACTIONS              = BLOCKINTERACT_REACH + "actions";

    private static final String BLOCKINTERACT_SPEED                      = BLOCKINTERACT + "speed.";
    public static final String BLOCKINTERACT_SPEED_CHECK                 = BLOCKINTERACT_SPEED + SUB_ACTIVE;
    public static final String BLOCKINTERACT_SPEED_INTERVAL              = BLOCKINTERACT_SPEED + "interval";
    public static final String BLOCKINTERACT_SPEED_LIMIT                 = BLOCKINTERACT_SPEED + "limit";
    public static final String BLOCKINTERACT_SPEED_ACTIONS               = BLOCKINTERACT_SPEED + "actions";

    private static final String BLOCKINTERACT_VISIBLE                    = BLOCKINTERACT + "visible.";
    public static final String  BLOCKINTERACT_VISIBLE_CHECK              = BLOCKINTERACT_VISIBLE + SUB_ACTIVE;
    public static final String  BLOCKINTERACT_VISIBLE_ACTIONS            = BLOCKINTERACT_VISIBLE + "actions";

    // BLOCKPLACE
    public static final String  BLOCKPLACE                               = CHECKS + "blockplace.";
    public static final String  BLOCKPLACE_ACTIVE                        = BLOCKPLACE + SUB_ACTIVE;

    private static final String BLOCKPLACE_AGAINST                       = BLOCKPLACE + "against.";
    public static final String  BLOCKPLACE_AGAINST_CHECK                 = BLOCKPLACE_AGAINST + SUB_ACTIVE;
    public static final String BLOCKPLACE_AGAINST_ACTIONS                = BLOCKPLACE_AGAINST + "actions";

    private static final String BLOCKPLACE_AUTOSIGN                         = BLOCKPLACE + "autosign.";
    public static final String  BLOCKPLACE_AUTOSIGN_CHECK                = BLOCKPLACE_AUTOSIGN + SUB_ACTIVE;
    public static final String  BLOCKPLACE_AUTOSIGN_SKIPEMPTY            = BLOCKPLACE_AUTOSIGN + "skipempty";
    public static final String  BLOCKPLACE_AUTOSIGN_ACTIONS              = BLOCKPLACE_AUTOSIGN + "actions";

    private static final String BLOCKPLACE_DIRECTION                     = BLOCKPLACE + "direction.";
    public static final String  BLOCKPLACE_DIRECTION_CHECK               = BLOCKPLACE_DIRECTION + SUB_ACTIVE;
    public static final String  BLOCKPLACE_DIRECTION_ACTIONS             = BLOCKPLACE_DIRECTION + "actions";

    private static final String BLOCKPLACE_FASTPLACE                     = BLOCKPLACE + "fastplace.";
    public static final String  BLOCKPLACE_FASTPLACE_CHECK               = BLOCKPLACE_FASTPLACE + SUB_ACTIVE;
    public static final String  BLOCKPLACE_FASTPLACE_LIMIT               = BLOCKPLACE_FASTPLACE + "limit";
    private static final String BLOCKPLACE_FASTPLACE_SHORTTERM           = BLOCKPLACE_FASTPLACE + "shortterm.";
    public static final String  BLOCKPLACE_FASTPLACE_SHORTTERM_TICKS     = BLOCKPLACE_FASTPLACE_SHORTTERM + "ticks";
    public static final String  BLOCKPLACE_FASTPLACE_SHORTTERM_LIMIT     = BLOCKPLACE_FASTPLACE_SHORTTERM + "limit";
    private static final String BLOCKPLACE_FASTPLACE_IMPROBABLE          = BLOCKPLACE_FASTPLACE + "improbable.";
    public static final String  BLOCKPLACE_FASTPLACE_IMPROBABLE_FEEDONLY = BLOCKPLACE_FASTPLACE_IMPROBABLE + "feedonly";
    public static final String  BLOCKPLACE_FASTPLACE_IMPROBABLE_WEIGHT   = BLOCKPLACE_FASTPLACE_IMPROBABLE + "weight";
    public static final String  BLOCKPLACE_FASTPLACE_ACTIONS             = BLOCKPLACE_FASTPLACE + "actions";

    private static final String BLOCKPLACE_NOSWING                       = BLOCKPLACE + "noswing.";
    public static final String  BLOCKPLACE_NOSWING_CHECK                 = BLOCKPLACE_NOSWING + SUB_ACTIVE;
    public static final String  BLOCKPLACE_NOSWING_EXCEPTIONS            = BLOCKPLACE_NOSWING + "exceptions";
    public static final String  BLOCKPLACE_NOSWING_ACTIONS               = BLOCKPLACE_NOSWING + "actions";

    private static final String BLOCKPLACE_REACH                         = BLOCKPLACE + "reach.";
    public static final String  BLOCKPLACE_REACH_CHECK                   = BLOCKPLACE_REACH + SUB_ACTIVE;
    public static final String  BLOCKPLACE_REACH_ACTIONS                 = BLOCKPLACE_REACH + "actions";
    
    private static final String BLOCKPLACE_SCAFFOLD                      = BLOCKPLACE + "scaffold.";
    public static final String  BLOCKPLACE_SCAFFOLD_CHECK                = BLOCKPLACE_SCAFFOLD + SUB_ACTIVE;
    public static final String  BLOCKPLACE_SCAFFOLD_ANGLE                = BLOCKPLACE_SCAFFOLD + "angle";
    private static final String BLOCKPLACE_SCAFFOLD_TIME                 = BLOCKPLACE_SCAFFOLD + "time.";
    public static final String  BLOCKPLACE_SCAFFOLD_TIME_AVG             = BLOCKPLACE_SCAFFOLD_TIME + "average";
    public static final String  BLOCKPLACE_SCAFFOLD_TIME_ACTIVE          = BLOCKPLACE_SCAFFOLD_TIME + "active";
    public static final String  BLOCKPLACE_SCAFFOLD_SPRINT               = BLOCKPLACE_SCAFFOLD + "sprint";
    private static final String BLOCKPLACE_SCAFFOLD_ROTATE               = BLOCKPLACE_SCAFFOLD + "rotate.";
    public static final String  BLOCKPLACE_SCAFFOLD_ROTATE_ACTIVE        = BLOCKPLACE_SCAFFOLD_ROTATE + "active";
    public static final String  BLOCKPLACE_SCAFFOLD_ROTATE_DIFFERENCE    = BLOCKPLACE_SCAFFOLD_ROTATE + "difference";
    public static final String  BLOCKPLACE_SCAFFOLD_TOOLSWITCH           = BLOCKPLACE_SCAFFOLD + "toolswitch";
    public static final String  BLOCKPLACE_SCAFFOLD_ACTIONS              = BLOCKPLACE_SCAFFOLD + "actions";

    private static final String BLOCKPLACE_SPEED                         = BLOCKPLACE + "speed.";
    public static final String  BLOCKPLACE_SPEED_CHECK                   = BLOCKPLACE_SPEED + SUB_ACTIVE;
    public static final String  BLOCKPLACE_SPEED_INTERVAL                = BLOCKPLACE_SPEED + "interval";
    private static final String BLOCKPLACE_SPEED_IMPROBABLE              = BLOCKPLACE_SPEED + "improbable.";
    public static final String  BLOCKPLACE_SPEED_IMPROBABLE_FEEDONLY     = BLOCKPLACE_SPEED_IMPROBABLE + "feedonly";
    public static final String  BLOCKPLACE_SPEED_IMPROBABLE_WEIGHT       = BLOCKPLACE_SPEED_IMPROBABLE + "weight";
    public static final String  BLOCKPLACE_SPEED_ACTIONS                 = BLOCKPLACE_SPEED + "actions";

    private static final String BLOCKPLACE_PREVENTMISC                   = BLOCKPLACE + "preventmisc.";
    public static final String  BLOCKPLACE_PREVENTMISC_BOATSANYWHERE     = BLOCKPLACE_PREVENTMISC + "boatsanywhere";

    public static final String  CHAT                                     = CHECKS + "chat.";
    public static final String  CHAT_ACTIVE                              = CHAT + SUB_ACTIVE;

    private static final String CHAT_CAPTCHA                             = CHAT + "captcha.";
    public static final String  CHAT_CAPTCHA_CHECK                       = CHAT_CAPTCHA + SUB_ACTIVE;
    private static final String CHAT_CAPTCHA_SKIP                        = CHAT_CAPTCHA + "skip.";
    public static final String  CHAT_CAPTCHA_SKIP_COMMANDS               = CHAT_CAPTCHA_SKIP + "commands";
    public static final String  CHAT_CAPTCHA_CHARACTERS                  = CHAT_CAPTCHA + "characters";
    public static final String  CHAT_CAPTCHA_LENGTH                      = CHAT_CAPTCHA + "length";
    public static final String  CHAT_CAPTCHA_QUESTION                    = CHAT_CAPTCHA + "question";
    public static final String  CHAT_CAPTCHA_SUCCESS                     = CHAT_CAPTCHA + "success";
    public static final String  CHAT_CAPTCHA_TRIES                       = CHAT_CAPTCHA + "tries";
    public static final String  CHAT_CAPTCHA_ACTIONS                     = CHAT_CAPTCHA + "actions";

    private static final String CHAT_COLOR                               = CHAT + "color.";
    public static final String  CHAT_COLOR_CHECK                         = CHAT_COLOR + SUB_ACTIVE;
    public static final String  CHAT_COLOR_ACTIONS                       = CHAT_COLOR + "actions";

    private static final String CHAT_COMMANDS                            = CHAT + "commands.";
    public static final String  CHAT_COMMANDS_CHECK                      = CHAT_COMMANDS + SUB_ACTIVE;
    @GlobalConfig
    public static final String  CHAT_COMMANDS_EXCLUSIONS                 = CHAT_COMMANDS + "exclusions";
    @GlobalConfig
    public static final String CHAT_COMMANDS_HANDLEASCHAT                = CHAT_COMMANDS + "handleaschat";
    public static final String  CHAT_COMMANDS_LEVEL                      = CHAT_COMMANDS + "level";
    private static final String CHAT_COMMANDS_SHORTTERM                  = CHAT_COMMANDS + "shortterm.";
    public static final String  CHAT_COMMANDS_SHORTTERM_TICKS            = CHAT_COMMANDS_SHORTTERM + "ticks";
    public static final String  CHAT_COMMANDS_SHORTTERM_LEVEL            = CHAT_COMMANDS_SHORTTERM + "level";
    public static final String  CHAT_COMMANDS_ACTIONS                    = CHAT_COMMANDS + "actions";

    // Text
    private static final String CHAT_TEXT                                = CHAT + "text.";
    public static final String CHAT_TEXT_CHECK                           = CHAT_TEXT + SUB_ACTIVE;
    public static final String CHAT_TEXT_DEBUG                           = CHAT_TEXT + "debug";
    public static final String CHAT_TEXT_ENGINE_MAXIMUM                  = CHAT_TEXT + "maximum";
    public static final String CHAT_TEXT_ALLOWVLRESET                    = CHAT_TEXT + "allowvlreset";
    public static final String CHAT_TEXT_FREQ                            = CHAT_TEXT + "frequency.";
    public static final String CHAT_TEXT_FREQ_NORM                       = CHAT_TEXT_FREQ + "normal.";
    public static final String CHAT_TEXT_FREQ_NORM_FACTOR                = CHAT_TEXT_FREQ_NORM + "factor";
    public static final String CHAT_TEXT_FREQ_NORM_LEVEL                 = CHAT_TEXT_FREQ_NORM + "level";
    public static final String CHAT_TEXT_FREQ_NORM_WEIGHT                = CHAT_TEXT_FREQ_NORM + "weight";
    public static final String CHAT_TEXT_FREQ_NORM_MIN                   = CHAT_TEXT_FREQ_NORM + "minimum";
    public static final String CHAT_TEXT_FREQ_NORM_ACTIONS               = CHAT_TEXT_FREQ_NORM + "actions";
    private static final String CHAT_TEXT_FREQ_SHORTTERM                 = CHAT_TEXT_FREQ + "shortterm.";
    public static final String CHAT_TEXT_FREQ_SHORTTERM_FACTOR           = CHAT_TEXT_FREQ_SHORTTERM + "factor";
    public static final String CHAT_TEXT_FREQ_SHORTTERM_LEVEL            = CHAT_TEXT_FREQ_SHORTTERM + "level";
    public static final String CHAT_TEXT_FREQ_SHORTTERM_WEIGHT           = CHAT_TEXT_FREQ_SHORTTERM + "weight";
    public static final String  CHAT_TEXT_FREQ_SHORTTERM_MIN             = CHAT_TEXT_FREQ_SHORTTERM + "minimum";
    public static final String CHAT_TEXT_FREQ_SHORTTERM_ACTIONS          = CHAT_TEXT_FREQ_SHORTTERM + "actions";

    // (Some of the following paths must be public for generic config reading.)
    // Per message checks.
    private static final String CHAT_TEXT_MSG                      = CHAT_TEXT + "message.";
    public static final String  CHAT_TEXT_MSG_LETTERCOUNT          = CHAT_TEXT_MSG + "lettercount";
    public static final String  CHAT_TEXT_MSG_PARTITION            = CHAT_TEXT_MSG + "partition";
    public static final String  CHAT_TEXT_MSG_UPPERCASE            = CHAT_TEXT_MSG + "uppercase";

    public static final String CHAT_TEXT_MSG_REPEATCANCEL          = CHAT_TEXT_MSG + "repeatviolation";
    public static final String CHAT_TEXT_MSG_AFTERJOIN             = CHAT_TEXT_MSG + "afterjoin";
    public static final String CHAT_TEXT_MSG_REPEATSELF            = CHAT_TEXT_MSG + "repeatself";
    public static final String CHAT_TEXT_MSG_REPEATGLOBAL          = CHAT_TEXT_MSG + "repeatglobal";
    public static final String CHAT_TEXT_MSG_NOMOVING              = CHAT_TEXT_MSG + "nomoving";

    private static final String CHAT_TEXT_MSG_WORDS                = CHAT_TEXT_MSG + "words.";
    public static final String  CHAT_TEXT_MSG_WORDS_LENGTHAV       = CHAT_TEXT_MSG_WORDS + "lengthav";
    public static final String  CHAT_TEXT_MSG_WORDS_LENGTHMSG      = CHAT_TEXT_MSG_WORDS + "lengthmsg";
    public static final String  CHAT_TEXT_MSG_WORDS_NOLETTER       = CHAT_TEXT_MSG_WORDS + "noletter";
    // Extended global checks.
    private static final String CHAT_TEXT_GL                       = CHAT_TEXT + "global.";
    public static final String CHAT_TEXT_GL_CHECK                  = CHAT_TEXT_GL + SUB_ACTIVE;
    public static final String CHAT_TEXT_GL_WEIGHT                 = CHAT_TEXT_GL + "weight";
    @GlobalConfig
    public static final String CHAT_TEXT_GL_WORDS                  = CHAT_TEXT_GL + "words.";
    public static final String CHAT_TEXT_GL_WORDS_CHECK            = CHAT_TEXT_GL_WORDS + SUB_ACTIVE;
    @GlobalConfig
    public static final String CHAT_TEXT_GL_PREFIXES               = CHAT_TEXT_GL + "prefixes.";
    public static final String CHAT_TEXT_GL_PREFIXES_CHECK         = CHAT_TEXT_GL_PREFIXES + SUB_ACTIVE;
    @GlobalConfig
    public static final String CHAT_TEXT_GL_SIMILARITY             = CHAT_TEXT_GL + "similarity.";
    public static final String CHAT_TEXT_GL_SIMILARITY_CHECK       = CHAT_TEXT_GL_SIMILARITY + SUB_ACTIVE;
    // Extended per player checks.
    private static final String CHAT_TEXT_PP                       = CHAT_TEXT + "player.";
    public static final String CHAT_TEXT_PP_CHECK                  = CHAT_TEXT_PP + SUB_ACTIVE;
    public static final String CHAT_TEXT_PP_WEIGHT                 = CHAT_TEXT_PP + "weight";
    @GlobalConfig
    public static final String CHAT_TEXT_PP_PREFIXES               = CHAT_TEXT_PP + "prefixes.";
    public static final String CHAT_TEXT_PP_PREFIXES_CHECK         = CHAT_TEXT_PP_PREFIXES + SUB_ACTIVE;
    @GlobalConfig
    public static final String CHAT_TEXT_PP_WORDS                  = CHAT_TEXT_PP + "words.";
    public static final String CHAT_TEXT_PP_WORDS_CHECK            = CHAT_TEXT_PP_WORDS + SUB_ACTIVE;
    @GlobalConfig
    public static final String CHAT_TEXT_PP_SIMILARITY             = CHAT_TEXT_PP + "similarity.";
    public static final String CHAT_TEXT_PP_SIMILARITY_CHECK       = CHAT_TEXT_PP_SIMILARITY + SUB_ACTIVE;

    private static final String CHAT_WARNING                             = CHAT + "warning.";
    public static final String  CHAT_WARNING_CHECK                       = CHAT_WARNING + SUB_ACTIVE;
    public static final String  CHAT_WARNING_LEVEL                       = CHAT_WARNING + "level";
    public static final String  CHAT_WARNING_MESSAGE                     = CHAT_WARNING + "message";
    public static final String  CHAT_WARNING_TIMEOUT                     = CHAT_WARNING + "timeout";

    // NOT YET IN USE
    private static final String CHAT_LOGINS                              = CHAT + "logins.";
    public static final String  CHAT_LOGINS_CHECK                        = CHAT_LOGINS + SUB_ACTIVE;
    public static final String  CHAT_LOGINS_PERWORLDCOUNT                = CHAT_LOGINS + "perworldcount";
    public static final String  CHAT_LOGINS_SECONDS                      = CHAT_LOGINS + "seconds";
    public static final String  CHAT_LOGINS_LIMIT                        = CHAT_LOGINS + "limit";
    public static final String  CHAT_LOGINS_KICKMESSAGE                  = CHAT_LOGINS + "kickmessage";
    public static final String  CHAT_LOGINS_STARTUPDELAY                 = CHAT_LOGINS + "startupdelay";

    private static final String CHAT_RELOG                               = CHAT + "relog.";
    public static final String  CHAT_RELOG_CHECK                         = CHAT_RELOG + SUB_ACTIVE;
    public static final String  CHAT_RELOG_KICKMESSAGE                   = CHAT_RELOG + "kickmessage";
    public static final String  CHAT_RELOG_TIMEOUT                       = CHAT_RELOG + "timeout";
    private static final String CHAT_RELOG_WARNING                       = CHAT_RELOG + "warning.";
    public static final String  CHAT_RELOG_WARNING_MESSAGE               = CHAT_RELOG_WARNING + "message";
    public static final String  CHAT_RELOG_WARNING_NUMBER                = CHAT_RELOG_WARNING + "number";
    public static final String  CHAT_RELOG_WARNING_TIMEOUT               = CHAT_RELOG_WARNING + "timeout";
    public static final String  CHAT_RELOG_ACTIONS                       = CHAT_RELOG + "actions";

    /*
     * Combined !
     */
    public static final String  COMBINED                                 = CHECKS + "combined.";
    public static final String  COMBINED_ACTIVE                          = COMBINED + SUB_ACTIVE;

    private static final String COMBINED_BEDLEAVE                        = COMBINED + "bedleave.";
    public static final String  COMBINED_BEDLEAVE_CHECK                  = COMBINED_BEDLEAVE + SUB_ACTIVE;
    public static final String  COMBINED_BEDLEAVE_ACTIONS                = COMBINED_BEDLEAVE + "actions";

    private static final String COMBINED_ENDERPEARL                      = COMBINED + "enderpearl.";
    public static final String  COMBINED_ENDERPEARL_CHECK                = COMBINED_ENDERPEARL + SUB_ACTIVE;
    public static final String  COMBINED_ENDERPEARL_PREVENTCLICKBLOCK    = COMBINED_ENDERPEARL + "preventclickblock";

    private static final String COMBINED_IMPROBABLE                      = COMBINED + "improbable.";
    public static final String  COMBINED_IMPROBABLE_CHECK                = COMBINED_IMPROBABLE + SUB_ACTIVE;
    public static final String  COMBINED_IMPROBABLE_LEVEL                = COMBINED_IMPROBABLE + "level";

    //    private static final String COMBINED_IMPROBABLE_CHECKS               = COMBINED_IMPROBABLE + "options.";               
    //    public static final String  COMBINED_IMPROBABLE_FASTBREAK_CHECK      = COMBINED_IMPROBABLE_CHECKS + "fastbreak";

    public static final String  COMBINED_IMPROBABLE_ACTIONS              = COMBINED_IMPROBABLE + "actions";

    private static final String COMBINED_INVULNERABLE                       = COMBINED + "invulnerable.";
    public static final String  COMBINED_INVULNERABLE_CHECK                 = COMBINED_INVULNERABLE + SUB_ACTIVE;
    private static final String COMBINED_INVULNERABLE_INITIALTICKS          = COMBINED_INVULNERABLE + "initialticks.";
    public static final String  COMBINED_INVULNERABLE_INITIALTICKS_JOIN     = COMBINED_INVULNERABLE_INITIALTICKS + "join";
    public static final String  COMBINED_INVULNERABLE_IGNORE                = COMBINED_INVULNERABLE + "ignore";
    public static final String  COMBINED_INVULNERABLE_MODIFIERS             = COMBINED_INVULNERABLE + "modifiers"; // no dot !
    private static final String COMBINED_INVULNERABLE_TRIGGERS              = COMBINED_INVULNERABLE + "triggers.";
    public static final String  COMBINED_INVULNERABLE_TRIGGERS_ALWAYS       = COMBINED_INVULNERABLE_TRIGGERS + "always";
    public static final String  COMBINED_INVULNERABLE_TRIGGERS_FALLDISTANCE = COMBINED_INVULNERABLE_TRIGGERS + "falldistance";

    public static final String COMBINED_MUNCHHAUSEN                         = COMBINED + "munchhausen.";
    public static final String COMBINED_MUNCHHAUSEN_CHECK                   = COMBINED_MUNCHHAUSEN + SUB_ACTIVE;
    public static final String COMBINED_MUNCHHAUSEN_ACTIONS                 = COMBINED_MUNCHHAUSEN + "actions";

    private static final String COMBINED_YAWRATE                         = COMBINED + "yawrate.";
    public static final String  COMBINED_YAWRATE_RATE                    = COMBINED_YAWRATE + "rate";
    private static final String COMBINED_YAWRATE_IMPROBABLE              = COMBINED_YAWRATE + "improbable.";
    public static final String  COMBINED_YAWRATE_IMPROBABLE_FEEDONLY     = COMBINED_YAWRATE_IMPROBABLE + "feedonly";
    public static final String  COMBINED_YAWRATE_IMPROBABLE_WEIGHT       = COMBINED_YAWRATE_IMPROBABLE + "weight";
    private static final String COMBINED_YAWRATE_PENALTY                 = COMBINED_YAWRATE + "penalty.";
    public static final String  COMBINED_YAWRATE_PENALTY_FACTOR          = COMBINED_YAWRATE_PENALTY + "factor";
    public static final String  COMBINED_YAWRATE_PENALTY_MIN             = COMBINED_YAWRATE_PENALTY + "minimum";
    public static final String  COMBINED_YAWRATE_PENALTY_MAX             = COMBINED_YAWRATE_PENALTY + "maximum";

    public static final String  FIGHT                                    = CHECKS + "fight.";
    public static final String  FIGHT_ACTIVE                             = FIGHT + SUB_ACTIVE;

    public static final String  FIGHT_CANCELDEAD                         = FIGHT + "canceldead";
    public static final String  FIGHT_TOOLCHANGEPENALTY                  = FIGHT + "toolchangepenalty";
    private static final String FIGHT_PVP                                = FIGHT + "pvp."; 
	public static final String  FIGHT_MAXLOOPLETENCYTICKS                = FIGHT + "maxloopletencyticks";
    public static final String  FIGHT_PVP_KNOCKBACKVELOCITY              = FIGHT_PVP + "knockbackvelocity";

    private static final String FIGHT_ANGLE                              = FIGHT + "angle.";
    public static final String  FIGHT_ANGLE_CHECK                        = FIGHT_ANGLE + SUB_ACTIVE;
    public static final String  FIGHT_ANGLE_THRESHOLD                    = FIGHT_ANGLE + "threshold.";
    public static final String  FIGHT_ANGLE_THRESHOLD_MOVE               = FIGHT_ANGLE_THRESHOLD + "average_move";
    public static final String  FIGHT_ANGLE_THRESHOLD_TIME               = FIGHT_ANGLE_THRESHOLD + "average_time";
    public static final String  FIGHT_ANGLE_THRESHOLD_YAW                = FIGHT_ANGLE_THRESHOLD + "average_yaw";
    public static final String  FIGHT_ANGLE_THRESHOLD_SWITCH             = FIGHT_ANGLE_THRESHOLD + "average_switch";
    public static final String  FIGHT_ANGLE_ACTIONS                      = FIGHT_ANGLE + "actions";
    
    private static final String FIGHT_CRITICAL                           = FIGHT + "critical.";
    public static final String  FIGHT_CRITICAL_CHECK                     = FIGHT_CRITICAL + SUB_ACTIVE;
    // TODO: Deprecate or rename (->falldistancemin)?
    public static final String  FIGHT_CRITICAL_FALLDISTANCE              = FIGHT_CRITICAL + "falldistance";
     public static final String  FIGHT_CRITICAL_FALLDISTLENIENCY         = FIGHT_CRITICAL + "falldistleniency";
    public static final String  FIGHT_CRITICAL_ACTIONS                   = FIGHT_CRITICAL + "actions";

    private static final String FIGHT_DIRECTION                          = FIGHT + "direction.";
    public static final String  FIGHT_DIRECTION_CHECK                    = FIGHT_DIRECTION + SUB_ACTIVE;
    public static final String  FIGHT_DIRECTION_STRICT                   = FIGHT_DIRECTION + "strict";
	public static final String  FIGHT_DIRECTION_FAILALL                  = FIGHT_DIRECTION + "failall";
    public static final String  FIGHT_DIRECTION_LOOPPRECISION            = FIGHT_DIRECTION + "loopprecision";
    public static final String  FIGHT_DIRECTION_STRICTANGLEPRECISION     = FIGHT_DIRECTION + "strictangleprecision";
    public static final String  FIGHT_DIRECTION_PENALTY                  = FIGHT_DIRECTION + "penalty";
    public static final String  FIGHT_DIRECTION_ACTIONS                  = FIGHT_DIRECTION + "actions";

    private static final String FIGHT_FASTHEAL                           = FIGHT + "fastheal.";
    public static final String  FIGHT_FASTHEAL_CHECK                     = FIGHT_FASTHEAL + SUB_ACTIVE;
    public static final String  FIGHT_FASTHEAL_INTERVAL                  = FIGHT_FASTHEAL + "interval";
    public static final String  FIGHT_FASTHEAL_BUFFER                    = FIGHT_FASTHEAL + "buffer";
    public static final String  FIGHT_FASTHEAL_ACTIONS                   = FIGHT_FASTHEAL + "actions";

    private static final String FIGHT_GODMODE                            = FIGHT + "godmode.";
    public static final String  FIGHT_GODMODE_CHECK                      = FIGHT_GODMODE + SUB_ACTIVE;
    public static final String  FIGHT_GODMODE_LAGMINAGE                  = FIGHT_GODMODE + "minage";
    public static final String  FIGHT_GODMODE_LAGMAXAGE                  = FIGHT_GODMODE + "maxage";
    public static final String  FIGHT_GODMODE_ACTIONS                    = FIGHT_GODMODE + "actions";

    private static final String FIGHT_NOSWING                            = FIGHT + "noswing.";
    public static final String  FIGHT_NOSWING_CHECK                      = FIGHT_NOSWING + SUB_ACTIVE;
    public static final String  FIGHT_NOSWING_ACTIONS                    = FIGHT_NOSWING + "actions";

    private static final String FIGHT_REACH                              = FIGHT + "reach.";
    public static final String  FIGHT_REACH_CHECK                        = FIGHT_REACH + SUB_ACTIVE;
    public static final String  FIGHT_REACH_SURVIVALDISTANCE             = FIGHT_REACH + "survivaldistance";
    public static final String  FIGHT_REACH_PENALTY                      = FIGHT_REACH + "penalty";
    public static final String  FIGHT_REACH_PRECISION                    = FIGHT_REACH + "precision";
    public static final String  FIGHT_REACH_REDUCE                       = FIGHT_REACH + "reduce";
    public static final String  FIGHT_REACH_REDUCEDISTANCE               = FIGHT_REACH + "reducedistance";
    public static final String  FIGHT_REACH_REDUCESTEP                   = FIGHT_REACH + "reducestep";
    private static final String FIGHT_REACH_IMPROBABLE                   = FIGHT_REACH + "improbable.";
    public static final String  FIGHT_REACH_IMPROBABLE_FEEDONLY          = FIGHT_REACH_IMPROBABLE + "feedonly";
    public static final String  FIGHT_REACH_IMPROBABLE_WEIGHT            = FIGHT_REACH_IMPROBABLE + "weight";
    public static final String  FIGHT_REACH_ACTIONS                      = FIGHT_REACH + "actions";

    public static final String FIGHT_SELFHIT                             = FIGHT + "selfhit.";
    public static final String FIGHT_SELFHIT_CHECK                       = FIGHT_SELFHIT + SUB_ACTIVE;
	public static final String FIGHT_SELFHIT_EXCLUDEPROJECTILE           = FIGHT_SELFHIT + "excludeprojectile";
	public static final String FIGHT_SELFHIT_MESSAGE                     = FIGHT_SELFHIT + "warn_player";
    public static final String FIGHT_SELFHIT_ACTIONS                     = FIGHT_SELFHIT + "actions";


    private static final String FIGHT_SPEED                              = FIGHT + "speed.";
    public static final String  FIGHT_SPEED_CHECK                        = FIGHT_SPEED + SUB_ACTIVE;
    public static final String  FIGHT_SPEED_LIMIT                        = FIGHT_SPEED + "limit";
    private static final String FIGHT_SPEED_BUCKETS                      = FIGHT_SPEED + "buckets.";
    @GlobalConfig
    public static final String  FIGHT_SPEED_BUCKETS_N                    = FIGHT_SPEED_BUCKETS + "number";
    @GlobalConfig
    public static final String  FIGHT_SPEED_BUCKETS_DUR                  = FIGHT_SPEED_BUCKETS + "duration";
    public static final String  FIGHT_SPEED_BUCKETS_FACTOR               = FIGHT_SPEED_BUCKETS + "factor";
    private static final String FIGHT_SPEED_SHORTTERM                    = FIGHT_SPEED + "shortterm.";
    public static final String  FIGHT_SPEED_SHORTTERM_LIMIT              = FIGHT_SPEED_SHORTTERM + "limit";
    public static final String  FIGHT_SPEED_SHORTTERM_TICKS              = FIGHT_SPEED_SHORTTERM + "ticks";
    private static final String FIGHT_SPEED_IMPROBABLE                   = FIGHT_SPEED + "improbable.";
    public static final String  FIGHT_SPEED_IMPROBABLE_FEEDONLY          = FIGHT_SPEED_IMPROBABLE + "feedonly";
    public static final String  FIGHT_SPEED_IMPROBABLE_WEIGHT            = FIGHT_SPEED_IMPROBABLE + "weight";
    public static final String  FIGHT_SPEED_ACTIONS                      = FIGHT_SPEED + "actions";
    
    private static final String FIGHT_YAWRATE                            = FIGHT + "yawrate.";
    public static final String  FIGHT_YAWRATE_CHECK                      = FIGHT_YAWRATE + SUB_ACTIVE;

    public static final String  INVENTORY                                = CHECKS + "inventory.";
    public static final String  INVENTORY_ACTIVE                         = INVENTORY + SUB_ACTIVE;

    private static final String INVENTORY_DROP                           = INVENTORY + "drop.";
    public static final String  INVENTORY_DROP_CHECK                     = INVENTORY_DROP + SUB_ACTIVE;
    public static final String  INVENTORY_DROP_LIMIT                     = INVENTORY_DROP + "limit";
    public static final String  INVENTORY_DROP_TIMEFRAME                 = INVENTORY_DROP + "timeframe";
    public static final String  INVENTORY_DROP_ACTIONS                   = INVENTORY_DROP + "actions";

    private static final String INVENTORY_FASTCLICK                      = INVENTORY + "fastclick.";
    public static final String  INVENTORY_FASTCLICK_CHECK                = INVENTORY_FASTCLICK + SUB_ACTIVE;
    public static final String  INVENTORY_FASTCLICK_EXCLUDE              = INVENTORY_FASTCLICK + "exclude";
    public static final String  INVENTORY_FASTCLICK_SPARECREATIVE        = INVENTORY_FASTCLICK + "sparecreative";
    public static final String  INVENTORY_FASTCLICK_TWEAKS1_5            = INVENTORY_FASTCLICK + "tweaks1_5";
    private static final String INVENTORY_FASTCLICK_LIMIT                = INVENTORY_FASTCLICK + "limit.";
    public static final String  INVENTORY_FASTCLICK_LIMIT_SHORTTERM      = INVENTORY_FASTCLICK_LIMIT + "shortterm";
    public static final String  INVENTORY_FASTCLICK_LIMIT_NORMAL         = INVENTORY_FASTCLICK_LIMIT + "normal";
    public static final String  INVENTORY_FASTCLICK_LIMIT_CHEST          = INVENTORY_FASTCLICK_LIMIT + "chest";
    private static final String INVENTORY_FASTCLICK_IMPROBABLE           = INVENTORY_FASTCLICK + "improbable.";
    public static final String  INVENTORY_FASTCLICK_IMPROBABLE_WEIGHT    = INVENTORY_FASTCLICK_IMPROBABLE + "weight";
    public static final String  INVENTORY_FASTCLICK_ACTIONS              = INVENTORY_FASTCLICK + "actions";

    private static final String INVENTORY_FASTCONSUME                    = INVENTORY + "fastconsume.";
    public static final String  INVENTORY_FASTCONSUME_CHECK              = INVENTORY_FASTCONSUME + SUB_ACTIVE;
    public static final String  INVENTORY_FASTCONSUME_DURATION           = INVENTORY_FASTCONSUME + "duration";
    public static final String  INVENTORY_FASTCONSUME_WHITELIST          = INVENTORY_FASTCONSUME + "whitelist";
    public static final String  INVENTORY_FASTCONSUME_ITEMS              = INVENTORY_FASTCONSUME + "items";
    public static final String  INVENTORY_FASTCONSUME_ACTIONS            = INVENTORY_FASTCONSUME + "actions";

    private static final String INVENTORY_GUTENBERG                      = INVENTORY + "gutenberg.";
    public static final String  INVENTORY_GUTENBERG_CHECK                = INVENTORY_GUTENBERG + SUB_ACTIVE;
    public static final String  INVENTORY_GUTENBERG_PAGELIMIT            = INVENTORY_GUTENBERG + "pagelimit";
    public static final String  INVENTORY_GUTENBERG_ACTIONS              = INVENTORY_GUTENBERG + "actions";

    private static final String INVENTORY_INSTANTBOW                     = INVENTORY + "instantbow.";
    public static final String  INVENTORY_INSTANTBOW_CHECK               = INVENTORY_INSTANTBOW + SUB_ACTIVE;
    public static final String  INVENTORY_INSTANTBOW_STRICT              = INVENTORY_INSTANTBOW + "strict";
    public static final String  INVENTORY_INSTANTBOW_DELAY               = INVENTORY_INSTANTBOW + "delay";
    private static final String INVENTORY_INSTANTBOW_IMPROBABLE          = INVENTORY_INSTANTBOW + "improbable.";
    public static final String  INVENTORY_INSTANTBOW_IMPROBABLE_FEEDONLY = INVENTORY_INSTANTBOW_IMPROBABLE + "feedonly";
    public static final String  INVENTORY_INSTANTBOW_IMPROBABLE_WEIGHT   = INVENTORY_INSTANTBOW_IMPROBABLE + "weight";

    public static final String  INVENTORY_INSTANTBOW_ACTIONS             = INVENTORY_INSTANTBOW + "actions";

    private static final String INVENTORY_INSTANTEAT                     = INVENTORY + "instanteat.";
    public static final String  INVENTORY_INSTANTEAT_CHECK               = INVENTORY_INSTANTEAT + SUB_ACTIVE;
    public static final String  INVENTORY_INSTANTEAT_ACTIONS             = INVENTORY_INSTANTEAT + "actions";

    private static final String INVENTORY_ITEMS                          = INVENTORY + "items.";
    public static final String  INVENTORY_ITEMS_CHECK                    = INVENTORY_ITEMS + SUB_ACTIVE;

    private static final String INVENTORY_OPEN                           = INVENTORY + "open.";
    public static final  String INVENTORY_OPEN_CHECK                     = INVENTORY_OPEN + SUB_ACTIVE;
    // TODO: close and cancelother on open-section-level are temporary.
    public static final  String INVENTORY_OPEN_CLOSE                     = INVENTORY_OPEN + "close";
    public static final  String INVENTORY_OPEN_CANCELOTHER               = INVENTORY_OPEN + "cancelother";
    
    public static final  String INVENTORY_INVENTORYMOVE                  = INVENTORY + "inventorymove.";
    public static final  String INVENTORY_INVENTORYMOVE_CHECK            = INVENTORY_INVENTORYMOVE + SUB_ACTIVE;
    public static final  String INVENTORY_INVENTORYMOVE_DISABLECREATIVE  = INVENTORY_INVENTORYMOVE + "disable_creative";
    public static final  String INVENTORY_INVENTORYMOVE_HDISTLENIENCY    = INVENTORY_INVENTORYMOVE + "hdist_leniency";
    public static final  String INVENTORY_INVENTORYMOVE_HDISTMIN         = INVENTORY_INVENTORYMOVE + "hdist_min";
    public static final  String INVENTORY_INVENTORYMOVE_ACTIONS          = INVENTORY_INVENTORYMOVE + "actions";

    // Inventory hot-fix.
    private static final String INVENTORY_HOTFIX                            = INVENTORY + "hotfix.";
    private static final String INVENTORY_HOTFIX_DUPE                       = INVENTORY_HOTFIX + "duplication.";
    public static final String  INVENTORY_HOTFIX_DUPE_FALLINGBLOCKENDPORTAL = INVENTORY_HOTFIX_DUPE + "fallingblockendportal";

    public static final String  MOVING                                   = CHECKS + "moving.";
    public static final String  MOVING_ACTIVE                            = MOVING + SUB_ACTIVE;

    private static final String MOVING_CREATIVEFLY                       = MOVING + "creativefly.";
    public static final String  MOVING_CREATIVEFLY_CHECK                 = MOVING_CREATIVEFLY + SUB_ACTIVE;
    public static final String  MOVING_CREATIVEFLY_IGNORECREATIVE        = MOVING_CREATIVEFLY + "ignorecreative";
    public static final String  MOVING_CREATIVEFLY_IGNOREALLOWFLIGHT     = MOVING_CREATIVEFLY + "ignoreallowflight";
    public static final String  MOVING_CREATIVEFLY_MODEL                 = MOVING_CREATIVEFLY + SUB_MODEL + ".";
    public static final String  MOVING_CREATIVEFLY_ACTIONS               = MOVING_CREATIVEFLY + "actions";
	public static final String  MOVING_CREATIVEFLY_EYTRA_FWRESET         = MOVING_CREATIVEFLY_MODEL + "elytra.resetFwOnground";
    public static final String  MOVING_CREATIVEFLY_EYTRA_STRICT          = MOVING_CREATIVEFLY_MODEL + "elytra.strict";

    private static final String MOVING_MOREPACKETS                       = MOVING + "morepackets.";
    public static final String  MOVING_MOREPACKETS_CHECK                 = MOVING_MOREPACKETS + SUB_ACTIVE;
    public static final String  MOVING_MOREPACKETS_SECONDS               = MOVING_MOREPACKETS + "seconds";
    public static final String  MOVING_MOREPACKETS_EPSIDEAL              = MOVING_MOREPACKETS + "epsideal";
    public static final String  MOVING_MOREPACKETS_EPSMAX                = MOVING_MOREPACKETS + "epsmax";
    private static final String MOVING_MOREPACKETS_BURST                 = MOVING_MOREPACKETS + "burst.";
    public static final String  MOVING_MOREPACKETS_BURST_PACKETS         = MOVING_MOREPACKETS_BURST + "packets";
    public static final String  MOVING_MOREPACKETS_BURST_DIRECT          = MOVING_MOREPACKETS_BURST + "directviolation";
    public static final String  MOVING_MOREPACKETS_BURST_EPM             = MOVING_MOREPACKETS_BURST + "epmviolation";
    public static final String  MOVING_MOREPACKETS_SETBACKAGE            = MOVING_MOREPACKETS + "setbackage";
    public static final String  MOVING_MOREPACKETS_ACTIONS               = MOVING_MOREPACKETS + "actions";

    private static final String MOVING_NOFALL                            = MOVING + "nofall.";
    public static final String  MOVING_NOFALL_CHECK                      = MOVING_NOFALL + SUB_ACTIVE;
    public static final String  MOVING_NOFALL_DEALDAMAGE                 = MOVING_NOFALL + "dealdamage";
    public static final String  MOVING_NOFALL_SKIPALLOWFLIGHT            = MOVING_NOFALL + "skipallowflight";
    // TODO: A reset section (violation, teleport, vehicle) + @Moved.
    public static final String  MOVING_NOFALL_RESETONVL                  = MOVING_NOFALL + "resetonviolation";
    public static final String  MOVING_NOFALL_RESETONTP                  = MOVING_NOFALL + "resetonteleport";
    public static final String  MOVING_NOFALL_RESETONVEHICLE             = MOVING_NOFALL + "resetonvehicle";
    public static final String  MOVING_NOFALL_ANTICRITICALS              = MOVING_NOFALL + "anticriticals";
    public static final String  MOVING_NOFALL_ACTIONS                    = MOVING_NOFALL + "actions";

    public static final String  MOVING_PASSABLE                             = MOVING + "passable.";
    public static final String  MOVING_PASSABLE_CHECK                       = MOVING_PASSABLE + SUB_ACTIVE;
    //private static final String MOVING_PASSABLE_RAYTRACING                  = MOVING_PASSABLE + "raytracing.";
    public static final String  MOVING_PASSABLE_ACTIONS                     = MOVING_PASSABLE + "actions";
    public static final String  MOVING_PASSABLE_RT_XZ_FACTOR                = MOVING_PASSABLE + "horizontalmargins";
    public static final String  MOVING_PASSABLE_RT_Y_FACTOR                 = MOVING_PASSABLE + "verticalmargins";
    private static final String MOVING_PASSABLE_UNTRACKED                   = MOVING_PASSABLE + "untracked.";
    private static final String MOVING_PASSABLE_UNTRACKED_TELEPORT          = MOVING_PASSABLE_UNTRACKED + "teleport.";
    public static final String  MOVING_PASSABLE_UNTRACKED_TELEPORT_ACTIVE   = MOVING_PASSABLE_UNTRACKED_TELEPORT + SUB_ACTIVE;
    private static final String MOVING_PASSABLE_UNTRACKED_CMD               = MOVING_PASSABLE_UNTRACKED + "command.";
    public static final String  MOVING_PASSABLE_UNTRACKED_CMD_ACTIVE        = MOVING_PASSABLE_UNTRACKED_CMD + SUB_ACTIVE;
    public static final String  MOVING_PASSABLE_UNTRACKED_CMD_TRYTELEPORT   = MOVING_PASSABLE_UNTRACKED_CMD + "tryteleport";
    public static final String  MOVING_PASSABLE_UNTRACKED_CMD_PREFIXES      = MOVING_PASSABLE_UNTRACKED_CMD + "prefixes";

    private static final String MOVING_SURVIVALFLY                          = MOVING + "survivalfly.";
    public static final String MOVING_SURVIVALFLY_CHECK                     = MOVING_SURVIVALFLY + SUB_ACTIVE;
    public static final String MOVING_SURVIVALFLY_BLOCKINGSPEED             = MOVING_SURVIVALFLY + "blockingspeed";
    public static final String MOVING_SURVIVALFLY_SNEAKINGSPEED             = MOVING_SURVIVALFLY + "sneakingspeed";
    public static final String MOVING_SURVIVALFLY_SPEEDINGSPEED             = MOVING_SURVIVALFLY + "speedingspeed";
    public static final String MOVING_SURVIVALFLY_SPRINTINGSPEED            = MOVING_SURVIVALFLY + "sprintingspeed";
    public static final String MOVING_SURVIVALFLY_SWIMMINGSPEED             = MOVING_SURVIVALFLY + "swimmingspeed";
    public static final String MOVING_SURVIVALFLY_WALKINGSPEED              = MOVING_SURVIVALFLY + "walkingspeed";
    public static final String MOVING_SURVIVALFLY_SLOWNESSSPRINTHACK        = MOVING_SURVIVALFLY + "slownesssprinthack";
    public static final String MOVING_SURVIVALFLY_GROUNDHOP                 = MOVING_SURVIVALFLY + "groundhop";
    public static final String MOVING_SURVIVALFLY_STEPHEIGHT                = MOVING_SURVIVALFLY + "stepheight";
    public static final String MOVING_SURVIVALFLY_CHECKNOSLOW               = MOVING_SURVIVALFLY + "noslow";
    private static final String MOVING_SURVIVALFLY_EXTENDED                 = MOVING_SURVIVALFLY + "extended.";
    public static final String MOVING_SURVIVALFLY_EXTENDED_HACC             = MOVING_SURVIVALFLY_EXTENDED + "horizontal-accounting";
    public static final String MOVING_SURVIVALFLY_EXTENDED_VACC             = MOVING_SURVIVALFLY_EXTENDED + "vertical-accounting";
    public static final String MOVING_SURVIVALFLY_EXTENDED_RESETITEM        = MOVING_SURVIVALFLY_EXTENDED + "reset-activeitem";
    private static final String MOVING_SURVIVALFLY_LENIENCY                 = MOVING_SURVIVALFLY + "leniency.";
    public static final String  MOVING_SURVIVALFLY_LENIENCY_HBUFMAX         = MOVING_SURVIVALFLY_LENIENCY + "hbufmax";
    public static final String  MOVING_SURVIVALFLY_LENIENCY_FREEZECOUNT     = MOVING_SURVIVALFLY_LENIENCY + "freezecount";
    public static final String  MOVING_SURVIVALFLY_LENIENCY_FREEZEINAIR     = MOVING_SURVIVALFLY_LENIENCY + "freezeinair";
    private static final String MOVING_SURVIVALFLY_SETBACKPOLICY            = MOVING_SURVIVALFLY + "setbackpolicy.";
    public static final String MOVING_SURVIVALFLY_SETBACKPOLICY_FALLDAMAGE  = MOVING_SURVIVALFLY_SETBACKPOLICY + "falldamage";
    public static final String MOVING_SURVIVALFLY_SETBACKPOLICY_VOIDTOVOID  = MOVING_SURVIVALFLY_SETBACKPOLICY + "voidtovoid";
    public static final String MOVING_SURVIVALFLY_ACTIONS                   = MOVING_SURVIVALFLY + "actions";
    private static final String MOVING_SURVIVALFLY_VLFREQUENCY              = MOVING_SURVIVALFLY_LENIENCY + "violationfrequency.";
    public static final String MOVING_SURVIVALFLY_VLFREQUENCY_ACTIVE        = MOVING_SURVIVALFLY_VLFREQUENCY + "active";
    public static final String MOVING_SURVIVALFLY_VLFREQUENCY_DEBUG         = MOVING_SURVIVALFLY_VLFREQUENCY + "debug";
    public static final String MOVING_SURVIVALFLY_VLFREQUENCY_MAXTOTALVLS   = MOVING_SURVIVALFLY_VLFREQUENCY + "maxleniencyvl";
    public static final String MOVING_SURVIVALFLY_VLFREQUENCY_MINADDEDVLS   = MOVING_SURVIVALFLY_VLFREQUENCY + "mintoadd";
    public static final String MOVING_SURVIVALFLY_VLFREQUENCY_MOVECOUNT     = MOVING_SURVIVALFLY_VLFREQUENCY + "movecount";
    public static final String MOVING_SURVIVALFLY_VLFREQUENCY_MOREVLS       = MOVING_SURVIVALFLY_VLFREQUENCY + "morevls";

    @GlobalConfig
    public static final String  MOVING_SURVIVALFLY_HOVER                    = MOVING_SURVIVALFLY + "hover.";
    public static final String  MOVING_SURVIVALFLY_HOVER_CHECK              = MOVING_SURVIVALFLY_HOVER + SUB_ACTIVE;
    public static final String  MOVING_SURVIVALFLY_HOVER_STEP               = MOVING_SURVIVALFLY_HOVER + "step";
    public static final String  MOVING_SURVIVALFLY_HOVER_TICKS              = MOVING_SURVIVALFLY_HOVER + "ticks";
    public static final String  MOVING_SURVIVALFLY_HOVER_LOGINTICKS         = MOVING_SURVIVALFLY_HOVER + "loginticks";
    public static final String  MOVING_SURVIVALFLY_HOVER_FALLDAMAGE         = MOVING_SURVIVALFLY_HOVER + "falldamage";
    public static final String  MOVING_SURVIVALFLY_HOVER_SFVIOLATION        = MOVING_SURVIVALFLY_HOVER + "sfviolation";

    // Special (to be sorted in or factored out).
    private static final String MOVING_VELOCITY                             = MOVING + "velocity.";
    public static final String  MOVING_VELOCITY_ACTIVATIONCOUNTER           = MOVING_VELOCITY + "activationcounter";
    public static final String  MOVING_VELOCITY_ACTIVATIONTICKS             = MOVING_VELOCITY + "activationticks";
    public static final String  MOVING_VELOCITY_STRICTINVALIDATION          = MOVING_VELOCITY + "strictinvalidation";

    public static final String  MOVING_NOFALL_YONGROUND                     = MOVING_NOFALL + "yonground";
    public static final String  MOVING_YONGROUND                            = MOVING + "yonground";
    public static final String  MOVING_SURVIVALFLY_YSTEP                    = MOVING_SURVIVALFLY + "ystep";

    // General.
    public static final String  MOVING_SPLITMOVES                           = MOVING + "splitmoves"; // Needs better categories...
    public static final String  MOVING_TEMPKICKILLEGAL                      = MOVING + "tempkickillegal";
    public static final String  MOVING_IGNORESTANCE                         = MOVING + "ignorestance";
    // TODO: Might add a section for illegal move.
    private static final String MOVING_LOADCHUNKS                           = MOVING + "loadchunks.";
    public static final String  MOVING_LOADCHUNKS_JOIN                      = MOVING_LOADCHUNKS + "join";
    public static final String  MOVING_LOADCHUNKS_MOVE                      = MOVING_LOADCHUNKS + "move";
    public static final String  MOVING_LOADCHUNKS_TELEPORT                  = MOVING_LOADCHUNKS + "teleport";
    public static final String  MOVING_LOADCHUNKS_WORLDCHANGE               = MOVING_LOADCHUNKS + "worldchange";
    public static final String  MOVING_SPRINTINGGRACE                       = MOVING + "sprintinggrace";
    public static final String  MOVING_FLYINGGRACE                          = MOVING + "flyinggrace";
    public static final String  MOVING_ASSUMESPRINT                         = MOVING + "assumesprint";
    public static final String  MOVING_SPEEDGRACE                           = MOVING + "speedgrace";
    public static final String  MOVING_ENFORCELOCATION                      = MOVING + "enforcelocation";
    private static final String MOVING_SETBACK                              = MOVING + "setback.";
    public static final String  MOVING_SETBACK_METHOD                       = MOVING_SETBACK + "method";

    private static final String MOVING_TRACE                                = MOVING + "trace.";
    public static final String  MOVING_TRACE_MAXAGE                         = MOVING_TRACE + "maxage";
    public static final String  MOVING_TRACE_MAXSIZE                        = MOVING_TRACE + "maxsize";

    // Vehicles.
    private static final String MOVING_VEHICLE                              = MOVING + "vehicle.";
    public static final String  MOVING_VEHICLE_ENFORCELOCATION              = MOVING_VEHICLE + "enforcelocation";
    public static final String  MOVING_VEHICLE_PREVENTDESTROYOWN            = MOVING_VEHICLE + "preventdestroyown";
    public static final String  MOVING_VEHICLE_SCHEDULESETBACKS             = MOVING_VEHICLE + "schedulesetbacks";
    public static final String  MOVING_VEHICLE_DELAYADDPASSENGER            = MOVING_VEHICLE + "schedulesetpassenger";
    public static final String  MOVING_VEHICLE_IGNOREDVEHICLES              = MOVING_VEHICLE + "ignoredvehicles";
    private static final String MOVING_VEHICLE_MOREPACKETS                  = MOVING_VEHICLE + "morepackets.";
    public static final String  MOVING_VEHICLE_MOREPACKETS_CHECK            = MOVING_VEHICLE_MOREPACKETS + SUB_ACTIVE;
    public static final String  MOVING_VEHICLE_MOREPACKETS_ACTIONS          = MOVING_VEHICLE_MOREPACKETS + "actions";
    private static final String MOVING_VEHICLE_ENVELOPE                     = MOVING_VEHICLE + "envelope.";
    public static final String  MOVING_VEHICLE_ENVELOPE_ACTIVE              = MOVING_VEHICLE_ENVELOPE + SUB_ACTIVE;
    public static final String  MOVING_VEHICLE_ENVELOPE_HSPEEDCAP           = MOVING_VEHICLE_ENVELOPE + "hdistcap"; // Section.
    public static final String  MOVING_VEHICLE_ENVELOPE_ACTIONS             = MOVING_VEHICLE_ENVELOPE + "actions";

    private static final String MOVING_MESSAGE                              = MOVING + "message.";
    public static final  String MOVING_MESSAGE_ILLEGALPLAYERMOVE            = MOVING_MESSAGE + "illegalplayermove";
    public static final  String MOVING_MESSAGE_ILLEGALVEHICLEMOVE           = MOVING_MESSAGE + "illegalvehiclemove";

    public static final String  NET                                         = CHECKS + "net.";
    public static final String  NET_ACTIVE                                  = NET + SUB_ACTIVE;

    private static final String NET_ATTACKFREQUENCY                         = NET + "attackfrequency.";
    public static final String  NET_ATTACKFREQUENCY_ACTIVE                  = NET_ATTACKFREQUENCY + SUB_ACTIVE;
    // TODO: Generic config for seconds.
    public static final String  NET_ATTACKFREQUENCY_SECONDS                 = NET_ATTACKFREQUENCY + "limitforseconds.";
    public static final String  NET_ATTACKFREQUENCY_SECONDS_HALF             = NET_ATTACKFREQUENCY_SECONDS + "half";
    public static final String  NET_ATTACKFREQUENCY_SECONDS_ONE             = NET_ATTACKFREQUENCY_SECONDS + "one";
    public static final String  NET_ATTACKFREQUENCY_SECONDS_TWO             = NET_ATTACKFREQUENCY_SECONDS + "two";
    public static final String  NET_ATTACKFREQUENCY_SECONDS_FOUR            = NET_ATTACKFREQUENCY_SECONDS + "four";
    public static final String  NET_ATTACKFREQUENCY_SECONDS_EIGHT           = NET_ATTACKFREQUENCY_SECONDS + "eight";
    private static final String NET_ATTACKFREQUENCY_IMPROBABLE              = NET_ATTACKFREQUENCY + "improbable.";
    public static final String  NET_ATTACKFREQUENCY_IMPROBABLE_WEIGHT       = NET_ATTACKFREQUENCY_IMPROBABLE + "weight";
    public static final String  NET_ATTACKFREQUENCY_ACTIONS                 = NET_ATTACKFREQUENCY + "actions";

    private static final String NET_FLYINGFREQUENCY                         = NET + "flyingfrequency.";
    public static final String  NET_FLYINGFREQUENCY_ACTIVE                  = NET_FLYINGFREQUENCY + SUB_ACTIVE;
    @GlobalConfig
    public static final String  NET_FLYINGFREQUENCY_SECONDS                 = NET_FLYINGFREQUENCY + "seconds";
    @GlobalConfig
    public static final String  NET_FLYINGFREQUENCY_PACKETSPERSECOND        = NET_FLYINGFREQUENCY + "packetspersecond";
    public static final String  NET_FLYINGFREQUENCY_ACTIONS                 = NET_FLYINGFREQUENCY + "actions";
    // TODO: Reduceredundant has been removed (implement or remove config).
    private static final String NET_FLYINGFREQUENCY_REDUNDANT               = NET_FLYINGFREQUENCY + "reduceredundant.";
    public static final String  NET_FLYINGFREQUENCY_REDUNDANT_ACTIVE        = NET_FLYINGFREQUENCY_REDUNDANT + SUB_ACTIVE;
    @GlobalConfig
    public static final String  NET_FLYINGFREQUENCY_REDUNDANT_SECONDS       = NET_FLYINGFREQUENCY_REDUNDANT + "seconds";
    public static final String  NET_FLYINGFREQUENCY_REDUNDANT_ACTIONS       = NET_FLYINGFREQUENCY_REDUNDANT + "actions";

    private static final String NET_KEEPALIVEFREQUENCY                      = NET + "keepalivefrequency.";
    public static final String  NET_KEEPALIVEFREQUENCY_ACTIVE               = NET_KEEPALIVEFREQUENCY + SUB_ACTIVE;
    public static final String  NET_KEEPALIVEFREQUENCY_SECONDS              = NET_KEEPALIVEFREQUENCY + "seconds";
    public static final String  NET_KEEPALIVEFREQUENCY_ACTIONS              = NET_KEEPALIVEFREQUENCY + "actions";

    private static final String NET_PACKETFREQUENCY                         = NET + "packetfrequency.";
    public static final  String NET_PACKETFREQUENCY_ACTIVE                  = NET_PACKETFREQUENCY + SUB_ACTIVE;
    public static final  String NET_PACKETFREQUENCY_PPS                     = NET_PACKETFREQUENCY + "limitpersecond";
    public static final  String NET_PACKETFREQUENCY_SECONDS                 = NET_PACKETFREQUENCY + "seconds";
    public static final  String NET_PACKETFREQUENCY_ACTIONS                 = NET_PACKETFREQUENCY + "actions";

    private static final String NET_SOUNDDISTANCE                           = NET + "sounddistance.";
    public static final String  NET_SOUNDDISTANCE_ACTIVE                    = NET_SOUNDDISTANCE + SUB_ACTIVE;
    public static final String  NET_SOUNDDISTANCE_MAXDISTANCE               = NET_SOUNDDISTANCE + "maxdistance";

    /** Somehow superseded packets, typically leniency options .*/
    private static final String NET_SUPERSEDED                              = NET + "superseded.";
    private static final String NET_SUPERSEDED_FLYING                       = NET_SUPERSEDED + "flying.";
    public static final String  NET_SUPERSEDED_FLYING_CANCELWAITING         = NET_SUPERSEDED_FLYING + "cancelwaiting";
    
    private static final String NET_WRONGTURN                               = NET + "wrongturn.";
    public static final String  NET_WRONGTURN_ACTIVE                        = NET_WRONGTURN + SUB_ACTIVE;
    public static final String  NET_WRONGTURN_ACTIONS                       = NET_WRONGTURN + "actions";

    public static final String  STRINGS                                     = "strings";

    // Compatibility section (possibly temporary).
    @GlobalConfig
    public static final String COMPATIBILITY                             = "compatibility.";

    private static final String COMPATIBILITY_EXEMPTIONS                 = COMPATIBILITY + "exemptions.";
    private static final String COMPATIBILITY_EXEMPTIONS_REMOVE          = COMPATIBILITY_EXEMPTIONS + "remove.";
    private static final String COMPATIBILITY_EXEMPTIONS_WILDCARD        = COMPATIBILITY_EXEMPTIONS + "wildcard.";
    private static final String COMPATIBILITY_EXEMPTIONS_WILDCARD_DEFAULT = COMPATIBILITY_EXEMPTIONS_WILDCARD + "default.";
    private static final String COMPATIBILITY_EXEMPTIONS_WILDCARD_DEFAULT_METADATA = COMPATIBILITY_EXEMPTIONS_WILDCARD_DEFAULT + "metadata.";
    public static final String COMPATIBILITY_EXEMPTIONS_WILDCARD_DEFAULT_METADATA_ACTIVE = COMPATIBILITY_EXEMPTIONS_WILDCARD_DEFAULT_METADATA + SUB_ACTIVE;
    public static final String COMPATIBILITY_EXEMPTIONS_WILDCARD_DEFAULT_METADATA_KEYS = COMPATIBILITY_EXEMPTIONS_WILDCARD_DEFAULT_METADATA + "keys";
    private static final String COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC    = COMPATIBILITY_EXEMPTIONS_WILDCARD + "npc.";
    public static final String COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_ACTIVE = COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC + SUB_ACTIVE;
    public static final String COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_BUKKITINTERFACE = COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC + "bukkitnpc";
    private static final String COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_METADATA = COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC + "metadata.";
    public static final String COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_METADATA_ACTIVE = COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_METADATA + SUB_ACTIVE;
    public static final String COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_METADATA_KEYS = COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_METADATA + "keys";
    public static final String COMPATIBILITY_EXEMPTIONS_REMOVE_JOIN      = COMPATIBILITY_EXEMPTIONS_REMOVE + "join";
    public static final String COMPATIBILITY_EXEMPTIONS_REMOVE_LEAVE     = COMPATIBILITY_EXEMPTIONS_REMOVE + "leave";
    // TODO: remove: tick, check / remove metadata on tick/leave?.

    public static final String COMPATIBILITY_SERVER                      = COMPATIBILITY + "server.";
    public static final String COMPATIBILITY_SERVER_CBDEDICATED          = COMPATIBILITY_SERVER + "cbdedicated.";
    public static final String COMPATIBILITY_SERVER_CBDEDICATED_ENABLE   = COMPATIBILITY_SERVER_CBDEDICATED + "enable";
    public static final String COMPATIBILITY_SERVER_CBREFLECT            = COMPATIBILITY_SERVER + "cbreflect.";
    public static final String COMPATIBILITY_SERVER_CBREFLECT_ENABLE     = COMPATIBILITY_SERVER_CBREFLECT + "enable";

    public static final  String COMPATIBILITY_BLOCKS                            = COMPATIBILITY + "blocks.";
    public static final  String COMPATIBILITY_BLOCKS_CHANGETRACKER              = COMPATIBILITY_BLOCKS + "changetracker.";
    public static final  String COMPATIBILITY_BLOCKS_CHANGETRACKER_ACTIVE       = COMPATIBILITY_BLOCKS_CHANGETRACKER + SUB_ACTIVE;
    public static final  String COMPATIBILITY_BLOCKS_CHANGETRACKER_PISTONS      = COMPATIBILITY_BLOCKS_CHANGETRACKER + "pistons";
    public static final  String COMPATIBILITY_BLOCKS_CHANGETRACKER_MAXAGETICKS  = COMPATIBILITY_BLOCKS_CHANGETRACKER + "maxageticks";
    private static final String COMPATIBILITY_BLOCKS_CHANGETRACKER_PERWORLD     = COMPATIBILITY_BLOCKS_CHANGETRACKER + "perworld.";
    public static final  String COMPATIBILITY_BLOCKS_CHANGETRACKER_PERWORLD_MAXENTRIES = COMPATIBILITY_BLOCKS_CHANGETRACKER_PERWORLD + "maxentries";

    // Moved paths.
    @Moved(newPath = LOGGING_BACKEND_CONSOLE_ACTIVE)
    public static final String  LOGGING_CONSOLE                          = "logging.console";
    @Moved(newPath = LOGGING_BACKEND_FILE_ACTIVE)
    public static final String  LOGGING_FILE                             = "logging.file";
    @Moved(newPath = LOGGING_BACKEND_FILE_FILENAME)
    public static final String  LOGGING_FILENAME                         = "logging.filename";
    @Moved(newPath = LOGGING_BACKEND_INGAMECHAT_ACTIVE)
    public static final String  LOGGING_INGAMECHAT                       = "logging.ingamechat";
    @Moved(newPath = PROTECT_PLUGINS_HIDE_ACTIVE)
    public static final String  MISCELLANEOUS_PROTECTPLUGINS             = "miscellaneous.protectplugins";
    @Moved(newPath = PROTECT_CLIENTS_MOTD_ALLOWALL)
    public static final String  MISCELLANEOUS_ALLOWCLIENTMODS            = "miscellaneous.allowclientmods";
    @Moved(newPath = PROTECT_PLUGINS_HIDE_NOCOMMAND_MSG)
    public static  final String PROTECT_PLUGINS_HIDE_MSG_NOCOMMAND       = "protection.plugins.hide.messages.unknowncommand";
    @Moved(newPath = PROTECT_PLUGINS_HIDE_NOPERMISSION_MSG)
    public static  final String PROTECT_PLUGINS_HIDE_MSG_NOPERMISSION    = "protection.plugins.hide.messages.nopermission";
    @Moved(newPath = PROTECT_COMMANDS_CONSOLEONLY_ACTIVE)
    public static final String  MISCELLANEOUS_OPINCONSOLEONLY            = "miscellaneous.opinconsoleonly";
    @Moved(newPath = INVENTORY_OPEN_CHECK)
    public static final String  INVENTORY_ENSURECLOSE                    = "checks.inventory.ensureclose";
    @Moved(newPath = LOGGING_EXTENDED_STATUS)
    public static final String LOGGING_DEBUG                             = "logging.debug";
    @Moved(newPath = NET_FLYINGFREQUENCY_REDUNDANT_ACTIVE)
    public static final String  NET_FLYINGFREQUENCY_CANCELREDUNDANT      = "checks.net.flyingfrequency.cancelredundant";
    @Moved(newPath = MOVING_CREATIVEFLY_MODEL + "creative." + SUB_HORIZONTALSPEED)
    public static final String  MOVING_CREATIVEFLY_HORIZONTALSPEED       = "checks.moving.creativefly.horizontalspeed";
    @Moved(newPath = MOVING_CREATIVEFLY_MODEL + "creative." + SUB_VERTICALSPEED)
    public static final String  MOVING_CREATIVEFLY_VERTICALSPEED         = "checks.moving.creativefly.verticalspeed";
    @Moved(newPath = MOVING_CREATIVEFLY_MODEL + "creative." + SUB_MAXHEIGHT)
    public static final String  MOVING_CREATIVEFLY_MAXHEIGHT             = "checks.moving.creativefly.maxheight";
    @Moved(newPath = MOVING_SURVIVALFLY_SETBACKPOLICY_FALLDAMAGE)
    public static final String MOVING_SURVIVALFLY_FALLDAMAGE             = "checks.moving.survivalfly.falldamage";
    @Moved(newPath=MOVING_VEHICLE_ENFORCELOCATION)
    public static final String  MOVING_VEHICLES_ENFORCELOCATION             = "checks.moving.vehicles.enforcelocation";
    @Moved(newPath=MOVING_VEHICLE_PREVENTDESTROYOWN)
    public static final String  MOVING_VEHICLES_PREVENTDESTROYOWN           = "checks.moving.vehicles.preventdestroyown";
    @Moved(newPath=MOVING_VEHICLE_MOREPACKETS_CHECK)
    public static final String  MOVING_MOREPACKETSVEHICLE_CHECK          = "checks.moving.morepacketsvehicle.active";
    @Moved(newPath=MOVING_VEHICLE_MOREPACKETS_ACTIONS)
    public static final String  MOVING_MOREPACKETSVEHICLE_ACTIONS        = "checks.moving.morepacketsvehicle.actions";
    @Moved(newPath=MOVING_SURVIVALFLY_LENIENCY_HBUFMAX)
    public static final String MOVING_SURVIVALFLY_HBUFMAX                = "checks.moving.survivalfly.hbufmax";
    // Deprecated paths (just removed).
    @Deprecated
    public static final String  MISCELLANEOUS_REPORTTOMETRICS            = "miscellaneous.reporttometrics";
    @Deprecated
    public static final String  BLOCKBREAK_FASTBREAK_MOD_CREATIVE        = "checks.blockbreak.fastbreak.intervalcreative";
    @Deprecated
    public static final String MOVING_PASSABLE_RAYTRACING_VCLIPONLY      = "checks.moving.passable.raytracing.vcliponly";
    @Deprecated
    public static final String  FIGHT_CRITICAL_VELOCITY                  = "checks.fight.critical.velocity";
    @Deprecated
    public static final  String BLOCKBREAK_FASTBREAK_DEBUG               = "checks.blockbreak.fastbreak.debug";
    @Deprecated
    public static final String  FIGHT_KNOCKBACK_CHECK                    = "checks.fight.knockback.active";
    @Deprecated
    public static final String  FIGHT_KNOCKBACK_INTERVAL                 = "checks.fight.knockback.interval";
    @Deprecated
    public static final String  FIGHT_KNOCKBACK_ACTIONS                  = "checks.fight.knockback.actions";
    @Deprecated
    public static final String NET_FLYINGFREQUENCY_MAXPACKETS            = "checks.net.flyingfrequency.maxpackets";
    @Deprecated
    public static final String COMPATIBILITY_BUKKITONLY                  = "compatibility.bukkitapionly";
    @Deprecated
    public static final String MOVING_SURVIVALFLY_BEDSTEP                ="checks.moving.survivalfly.bedstep";
    @Deprecated
    public static final String  LOGGING_BACKEND_CONSOLE_PREFIX           = "logging.backend.console.prefix";
    @Deprecated
    public static final String  MOVING_VELOCITY_GRACETICKS               = "checks.moving.velocity.graceticks";
    @Deprecated
    public static final String  NET_FLYINGFREQUENCY_STRAYPACKETS_CANCEL  = "checks.net.flyingfrequency.straypackets.cancel";
    @Deprecated // Elytra: Deprecate to force override the old config value (also auto-added to moved).
    public static final String  MOVING_CREATIVEFLY_MODEL_ELYTRA_HORIZONTALSPEED = "checks.moving.creativefly.model.elytra.horizontalspeed";
    @Deprecated
    public  static final String MOVING_TRACE_SIZE                           = "checks.moving.trace.size";
    @Deprecated
    public  static final String MOVING_TRACE_MERGEDIST                      = "checks.moving.trace.mergedist";
    @Deprecated
    public static final String COMPATIBILITY_BLOCKS_IGNOREPASSABLE = "compatibility.blocks.ignorepassable";
    @Deprecated
    public static final String  MOVING_PASSABLE_RAYTRACING_CHECK            = "checks.moving.passable.raytracing.active";
    @Deprecated
    public static final String  MOVING_PASSABLE_RAYTRACING_BLOCKCHANGEONLY  = "checks.moving.passable.raytracing.blockchangeonly";
    @Deprecated
    public static final String  MISCELLANEOUS_MANAGELISTENERS            = "miscellaneous.managelisteners";
    @Deprecated
    public static final String COMPATIBILITY_MANAGELISTENERS             = "compatibility.managelisteners";
    @Deprecated
    public static final String  LOGGING_USESUBSCRIPTIONS                 = "logging.usesubscriptions";
    @Deprecated
    public static final String  LOGGING_BACKEND_INGAMECHAT_SUBSCRIPTIONS = "logging.backend.ingamechat.subscriptions";
    @Deprecated
    public static final String MOVING_SURVIVALFLY_VLFREEZE               = "checks.moving.survivalfly.vlfreeze";
    @Deprecated
    public static final String  MOVING_SURVIVALFLY_LENIENCY_FREEZETIME      = "checks.moving.survivalfly.leniency.freezetime";
    @Deprecated
    public static final String  FIGHT_CRITICAL_CANCEL_CANCEL             = "checks.fight.critical.cancel.cancel";
    @Deprecated
    public static final String  FIGHT_CRITICAL_CANCEL_DIVIDEDAMAGE       = "checks.fight.critical.cancel.dividedamage";
    @Deprecated // Does this need to be done? 
    private static final String FIGHT_CLICKPATTERN                       = "checks.fight.clickpattern";
    @Deprecated
    private static final String NET_FIGHTSYNC                            = "checks.net.fightsync";
    @Deprecated
    public static final String MOVING_SURVIVALFLY_COBWEBHACK             = "checks.moving.survivalfly.cobwebhack";

    /**
     * Get moved paths for which an annotation doesn't work.
     * 
     * @return A new collection of entries.
     */
    public static Collection<WrapMoved> getExtraMovedPaths() {
        final List<WrapMoved> entries = new LinkedList<WrapMoved>();
        final List<ManyMoved> multiEntries = new LinkedList<ManyMoved>();

        // Add entries.
        final List<String> cfModels = Arrays.asList("creative", "spectator", "survival", "adventure", "elytra", "levitation", "slowfalling");
        multiEntries.add(new ManyMoved(MOVING_CREATIVEFLY_MODEL, cfModels, "horizontalspeed", SUB_HORIZONTAL_SPEED));
        multiEntries.add(new ManyMoved(MOVING_CREATIVEFLY_MODEL, cfModels, "modsprint", SUB_HORIZONTAL_MODSPRINT));
        multiEntries.add(new ManyMoved(MOVING_CREATIVEFLY_MODEL, cfModels, "maxheight", SUB_VERTICAL_MAXHEIGHT));
        multiEntries.add(new ManyMoved(MOVING_CREATIVEFLY_MODEL, cfModels, "verticalspeed", SUB_VERTICAL_ASCEND_SPEED));

        // Expand ManyMoved entries.
        for (ManyMoved entry : multiEntries) {
            entries.addAll(entry.getWrapMoved());
        }
        return entries;
    }

}