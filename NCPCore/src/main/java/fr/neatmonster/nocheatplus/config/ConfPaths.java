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
    public static final String SUB_ALLOWINSTANTBREAK                     = "allow-instant-break";
    public static final String SUB_ASCEND                                = "ascend";
    public static final String SUB_DEBUG                                 = "debug";
    public static final String SUB_DESCEND                               = "descend";
    public static final String SUB_GRAVITY                               = "gravity";
    public static final String SUB_GROUND                                = "ground";
    public static final String SUB_HORIZONTAL                            = "horizontal";
    public static final String SUB_HORIZONTALSPEED                       = "horizontal-speed"; // Phase out.
    public static final String SUB_LAG                                   = "lag";
    public static final String SUB_MAXHEIGHT                             = "max-height";
    public static final String SUB_MODEL                                 = "model";
    public static final String SUB_MODIFIERS                             = "modifiers";
    public static final String SUB_MODSPRINT                             = "mod-sprint";
    /** No trailing dot! */
    public static final String SUB_OVERRIDEFLAGS                         = "override-flags";
    public static final String SUB_BREAKINGTIME                          = "breaking-time";
    public static final String SUB_SPEED                                 = "speed";
    public static final String SUB_VERTICAL                              = "vertical";
    public static final String SUB_VERTICALSPEED                         = "vertical-speed"; // Phase out.

    // Composite sub-paths.
    public static final String SUB_HORIZONTAL_SPEED                      = SUB_HORIZONTAL + "." + SUB_SPEED;
    public static final String SUB_HORIZONTAL_MODSPRINT                  = SUB_HORIZONTAL + "." + SUB_MODSPRINT;
    public static final String SUB_VERTICAL_ASCEND                       = SUB_VERTICAL + "." + SUB_ASCEND;
    public static final String SUB_VERTICAL_ASCEND_SPEED                 = SUB_VERTICAL_ASCEND + "." + SUB_SPEED;
    public static final String SUB_VERTICAL_DESCEND                      = SUB_VERTICAL + "." + SUB_DESCEND;
    public static final String SUB_VERTICAL_DESCEND_SPEED                = SUB_VERTICAL_DESCEND + "." + SUB_SPEED;
    public static final String SUB_VERTICAL_MAXHEIGHT                    = SUB_VERTICAL + "." + SUB_MAXHEIGHT;
    public static final String SUB_VERTICAL_GRAVITY                      = SUB_VERTICAL + "." + SUB_GRAVITY;
    public static final String SUB_BLOCKCACHE_WORLD_MINY                 = "block-cache.minimal-world-Y";

    // General.
    public static final String SAVEBACKCONFIG                            = "saveback-config";

    // Configuration version.
    /*
     * NOTE: The configuration allows setLastChangeBuildNumber(path), however
     * some of these settings are still needed for that.
     */
    @GlobalConfig // TODO: Per file versions should also be supported.
    public static final String CONFIGVERSION                             = "config-version.";
    public static final String CONFIGVERSION_NOTIFY                      = CONFIGVERSION + "notify";
    public static final String CONFIGVERSION_NOTIFYMAXPATHS              = CONFIGVERSION + "notify-max-paths";
    /** Build number of the build for which the default config was first created (DefaultConfig.buildNumber), updated with first save. */
    public static final String CONFIGVERSION_CREATED                     = CONFIGVERSION + "created";
    /** Build number of the build for which the default config was first created (DefaultConfig.buildNumber), updated with each save. */
    public static final String CONFIGVERSION_SAVED                       = CONFIGVERSION + "saved";

    @GlobalConfig
    private static final String LOGGING                                  = "logging.";
    public static final String  LOGGING_ACTIVE                           = LOGGING + SUB_ACTIVE;
    public static final String  LOGGING_MAXQUEUESIZE                     = LOGGING + "max-queue-size";

    private static final String LOGGING_BACKEND                          = LOGGING + "backend.";
    private static final String LOGGING_BACKEND_CONSOLE                  = LOGGING_BACKEND + "console.";
    public static final String  LOGGING_BACKEND_CONSOLE_ACTIVE           = LOGGING_BACKEND_CONSOLE + SUB_ACTIVE;
    public static final String  LOGGING_BACKEND_CONSOLE_ASYNCHRONOUS     = LOGGING_BACKEND_CONSOLE + "asynchronous";
    private static final String LOGGING_BACKEND_FILE                     = LOGGING_BACKEND + "file.";
    public static final String  LOGGING_BACKEND_FILE_ACTIVE              = LOGGING_BACKEND_FILE + SUB_ACTIVE;
    public static final String  LOGGING_BACKEND_FILE_FILENAME            = LOGGING_BACKEND_FILE + "filename";
    public static final String  LOGGING_BACKEND_FILE_PREFIX              = LOGGING_BACKEND_FILE + "prefix";
    private static final String LOGGING_BACKEND_INGAMECHAT               = LOGGING_BACKEND + "ingame-chat.";
    public static final String  LOGGING_BACKEND_INGAMECHAT_ACTIVE        = LOGGING_BACKEND_INGAMECHAT + SUB_ACTIVE;
    public static final String  LOGGING_BACKEND_INGAMECHAT_PREFIX        = LOGGING_BACKEND_INGAMECHAT + "prefix";

    private static final String LOGGING_EXTENDED                         = LOGGING + "extended.";
    public static final String  LOGGING_EXTENDED_STATUS                  = LOGGING_EXTENDED + "status";
    private static final String LOGGING_EXTENDED_COMMANDS                = LOGGING_EXTENDED + "commands.";
    public static final String  LOGGING_EXTENDED_COMMANDS_ACTIONS        = LOGGING_EXTENDED_COMMANDS + "actions";
    private static final String LOGGING_EXTENDED_ALLVIOLATIONS           = LOGGING_EXTENDED + "all-violations.";
    public static final String  LOGGING_EXTENDED_ALLVIOLATIONS_DEBUG     = LOGGING_EXTENDED_ALLVIOLATIONS + "debug";
    public static final String  LOGGING_EXTENDED_ALLVIOLATIONS_DEBUGONLY = LOGGING_EXTENDED_ALLVIOLATIONS + "debug-only";
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
    private static final String DATA_CONSISTENCYCHECKS                   = DATA + "consistency-checks.";
    public static final  String DATA_CONSISTENCYCHECKS_CHECK             = DATA_CONSISTENCYCHECKS + SUB_ACTIVE;
    public static final  String DATA_CONSISTENCYCHECKS_INTERVAL          = DATA_CONSISTENCYCHECKS + "interval";
    public static final  String DATA_CONSISTENCYCHECKS_MAXTIME           = DATA_CONSISTENCYCHECKS + "max-time";
    /**
     * This might not might not be used by checks. <br>
     * Used by: DataMan/Player-instances
     * 
     */
    public static final  String DATA_CONSISTENCYCHECKS_SUPPRESSWARNINGS  = DATA_CONSISTENCYCHECKS + "suppress-warnings";

    // Permission caching setup.
    private static final String PERMISSIONS                               = "permissions.";
    private static final String PERMISSIONS_POLICY                        = PERMISSIONS + "policy.";
    public static final  String PERMISSIONS_POLICY_DEFAULT                = PERMISSIONS_POLICY + "default";
    public static final  String PERMISSIONS_POLICY_RULES                  = PERMISSIONS_POLICY + "rules";

    private static final String PROTECT                                  = "protection.";
    // Other commands settings
    @GlobalConfig
    private static final String PROTECT_COMMANDS                         = PROTECT + "commands.";
    private static final String PROTECT_COMMANDS_CONSOLEONLY             = PROTECT_COMMANDS + "console-only.";
    public  static final String PROTECT_COMMANDS_CONSOLEONLY_ACTIVE      = PROTECT_COMMANDS_CONSOLEONLY + SUB_ACTIVE;
    public  static final String PROTECT_COMMANDS_CONSOLEONLY_MSG         = PROTECT_COMMANDS_CONSOLEONLY + "message";
    public  static final String PROTECT_COMMANDS_CONSOLEONLY_CMDS        = PROTECT_COMMANDS_CONSOLEONLY + "commands";
    // Plugins settings.
    private static final String PROTECT_PLUGINS                          = PROTECT + "plugins.";
    @GlobalConfig
    private static final String PROTECT_PLUGINS_HIDE                     = PROTECT_PLUGINS + "hide.";
    public static  final String PROTECT_PLUGINS_HIDE_ACTIVE              = PROTECT_PLUGINS_HIDE + SUB_ACTIVE;
    private static final String PROTECT_PLUGINS_HIDE_NOCOMMAND           = PROTECT_PLUGINS_HIDE + "unknown-command.";
    public static  final String PROTECT_PLUGINS_HIDE_NOCOMMAND_MSG       = PROTECT_PLUGINS_HIDE_NOCOMMAND + "message";
    public static  final String PROTECT_PLUGINS_HIDE_NOCOMMAND_CMDS      = PROTECT_PLUGINS_HIDE_NOCOMMAND + "commands";
    private static final String PROTECT_PLUGINS_HIDE_NOPERMISSION        = PROTECT_PLUGINS_HIDE + "no-permission.";
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
    public static final String  BLOCKBREAK_FASTBREAK_STRICT              = BLOCKBREAK_FASTBREAK + "strict";
    private static final String BLOCKBREAK_FASTBREAK_BUCKETS             = BLOCKBREAK + "buckets.";
    public static final String  BLOCKBREAK_FASTBREAK_BUCKETS_CONTENTION  = BLOCKBREAK_FASTBREAK_BUCKETS + "contention";
    @GlobalConfig
    public static final String  BLOCKBREAK_FASTBREAK_BUCKETS_N           = BLOCKBREAK_FASTBREAK_BUCKETS + "number";
    @GlobalConfig
    public static final String  BLOCKBREAK_FASTBREAK_BUCKETS_DUR         = BLOCKBREAK_FASTBREAK_BUCKETS + "duration";
    public static final String  BLOCKBREAK_FASTBREAK_BUCKETS_FACTOR      = BLOCKBREAK_FASTBREAK_BUCKETS + "factor";
    public static final String  BLOCKBREAK_FASTBREAK_DELAY               = BLOCKBREAK_FASTBREAK + "delay";
    public static final String  BLOCKBREAK_FASTBREAK_GRACE               = BLOCKBREAK_FASTBREAK + "grace";
    public static final String  BLOCKBREAK_FASTBREAK_MOD_SURVIVAL        = BLOCKBREAK_FASTBREAK + "interval-survival";
    public static final String  BLOCKBREAK_FASTBREAK_ACTIONS             = BLOCKBREAK_FASTBREAK + "actions";

    private static final String BLOCKBREAK_FREQUENCY                     = BLOCKBREAK + "frequency.";
    public static final String  BLOCKBREAK_FREQUENCY_CHECK               = BLOCKBREAK_FREQUENCY + SUB_ACTIVE;
    public static final String  BLOCKBREAK_FREQUENCY_MOD_CREATIVE        = BLOCKBREAK_FREQUENCY + "interval-creative";
    public static final String  BLOCKBREAK_FREQUENCY_MOD_SURVIVAL        = BLOCKBREAK_FREQUENCY + "interval-survival";
    private static final String BLOCKBREAK_FREQUENCY_BUCKETS             = BLOCKBREAK_FREQUENCY + "buckets.";
    @GlobalConfig
    public static final String  BLOCKBREAK_FREQUENCY_BUCKETS_DUR         = BLOCKBREAK_FREQUENCY_BUCKETS + "duration";
    public static final String  BLOCKBREAK_FREQUENCY_BUCKETS_FACTOR      = BLOCKBREAK_FREQUENCY_BUCKETS + "factor";
    @GlobalConfig
    public static final String  BLOCKBREAK_FREQUENCY_BUCKETS_N           = BLOCKBREAK_FREQUENCY_BUCKETS + "number";
    private static final String BLOCKBREAK_FREQUENCY_SHORTTERM           = BLOCKBREAK_FREQUENCY + "short-term.";
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
    public static final String  BLOCKBREAK_WRONGBLOCK_IMPROBABLE_FEEDONLY = BLOCKBREAK_WRONGBLOCK_IMPROBABLE + "feed-only";
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

    public static final String  BLOCKPLACE_BOATSONWATERONLY              = BLOCKPLACE + "boats-on-water-only";
 
    private static final String BLOCKPLACE_AGAINST                       = BLOCKPLACE + "against.";
    public static final String  BLOCKPLACE_AGAINST_CHECK                 = BLOCKPLACE_AGAINST + SUB_ACTIVE;
    public static final String BLOCKPLACE_AGAINST_ACTIONS                = BLOCKPLACE_AGAINST + "actions";

    private static final String BLOCKPLACE_AUTOSIGN                         = BLOCKPLACE + "autosign.";
    public static final String  BLOCKPLACE_AUTOSIGN_CHECK                = BLOCKPLACE_AUTOSIGN + SUB_ACTIVE;
    public static final String  BLOCKPLACE_AUTOSIGN_SKIPEMPTY            = BLOCKPLACE_AUTOSIGN + "skip-empty";
    public static final String  BLOCKPLACE_AUTOSIGN_ACTIONS              = BLOCKPLACE_AUTOSIGN + "actions";

    private static final String BLOCKPLACE_DIRECTION                     = BLOCKPLACE + "direction.";
    public static final String  BLOCKPLACE_DIRECTION_CHECK               = BLOCKPLACE_DIRECTION + SUB_ACTIVE;
    public static final String  BLOCKPLACE_DIRECTION_ACTIONS             = BLOCKPLACE_DIRECTION + "actions";

    private static final String BLOCKPLACE_FASTPLACE                     = BLOCKPLACE + "fastplace.";
    public static final String  BLOCKPLACE_FASTPLACE_CHECK               = BLOCKPLACE_FASTPLACE + SUB_ACTIVE;
    public static final String  BLOCKPLACE_FASTPLACE_LIMIT               = BLOCKPLACE_FASTPLACE + "limit";
    private static final String BLOCKPLACE_FASTPLACE_SHORTTERM           = BLOCKPLACE_FASTPLACE + "short-term.";
    public static final String  BLOCKPLACE_FASTPLACE_SHORTTERM_TICKS     = BLOCKPLACE_FASTPLACE_SHORTTERM + "ticks";
    public static final String  BLOCKPLACE_FASTPLACE_SHORTTERM_LIMIT     = BLOCKPLACE_FASTPLACE_SHORTTERM + "limit";
    private static final String BLOCKPLACE_FASTPLACE_IMPROBABLE          = BLOCKPLACE_FASTPLACE + "improbable.";
    public static final String  BLOCKPLACE_FASTPLACE_IMPROBABLE_FEEDONLY = BLOCKPLACE_FASTPLACE_IMPROBABLE + "feed-only";
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
    public static final String  BLOCKPLACE_SCAFFOLD_TOOLSWITCH           = BLOCKPLACE_SCAFFOLD + "tool-switch";
    private static final String BLOCKPLACE_SCAFFOLD_IMPROBABLE           = BLOCKPLACE_SCAFFOLD + "improbable.";
    public static final String  BLOCKPLACE_SCAFFOLD_IMPROBABLE_FEEDONLY  = BLOCKPLACE_SCAFFOLD_IMPROBABLE +"feed-only";
    public static final String  BLOCKPLACE_SCAFFOLD_IMPROBABLE_WEIGHT    = BLOCKPLACE_SCAFFOLD_IMPROBABLE +"weight";
    public static final String  BLOCKPLACE_SCAFFOLD_ACTIONS              = BLOCKPLACE_SCAFFOLD + "actions";

    private static final String BLOCKPLACE_SPEED                         = BLOCKPLACE + "speed.";
    public static final String  BLOCKPLACE_SPEED_CHECK                   = BLOCKPLACE_SPEED + SUB_ACTIVE;
    public static final String  BLOCKPLACE_SPEED_INTERVAL                = BLOCKPLACE_SPEED + "interval";
    private static final String BLOCKPLACE_SPEED_IMPROBABLE              = BLOCKPLACE_SPEED + "improbable.";
    public static final String  BLOCKPLACE_SPEED_IMPROBABLE_FEEDONLY     = BLOCKPLACE_SPEED_IMPROBABLE + "feed-only";
    public static final String  BLOCKPLACE_SPEED_IMPROBABLE_WEIGHT       = BLOCKPLACE_SPEED_IMPROBABLE + "weight";
    public static final String  BLOCKPLACE_SPEED_ACTIONS                 = BLOCKPLACE_SPEED + "actions";

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

    private static final String CHAT_COMMANDS                            = CHAT + "commands.";
    public static final String  CHAT_COMMANDS_CHECK                      = CHAT_COMMANDS + SUB_ACTIVE;
    @GlobalConfig
    public static final String  CHAT_COMMANDS_EXCLUSIONS                 = CHAT_COMMANDS + "exclusions";
    @GlobalConfig
    public static final String CHAT_COMMANDS_HANDLEASCHAT                = CHAT_COMMANDS + "handle-as-chat";
    public static final String  CHAT_COMMANDS_LEVEL                      = CHAT_COMMANDS + "level";
    private static final String CHAT_COMMANDS_SHORTTERM                  = CHAT_COMMANDS + "short-term.";
    public static final String  CHAT_COMMANDS_SHORTTERM_TICKS            = CHAT_COMMANDS_SHORTTERM + "ticks";
    public static final String  CHAT_COMMANDS_SHORTTERM_LEVEL            = CHAT_COMMANDS_SHORTTERM + "level";
    public static final String  CHAT_COMMANDS_ACTIONS                    = CHAT_COMMANDS + "actions";

    // Text
    private static final String CHAT_TEXT                                = CHAT + "text.";
    public static final String CHAT_TEXT_CHECK                           = CHAT_TEXT + SUB_ACTIVE;
    public static final String CHAT_TEXT_DEBUG                           = CHAT_TEXT + "debug";
    public static final String CHAT_TEXT_ENGINE_MAXIMUM                  = CHAT_TEXT + "maximum";
    public static final String CHAT_TEXT_ALLOWVLRESET                    = CHAT_TEXT + "allow-VL-reset";
    public static final String CHAT_TEXT_FREQ                            = CHAT_TEXT + "frequency.";
    public static final String CHAT_TEXT_FREQ_NORM                       = CHAT_TEXT_FREQ + "normal.";
    public static final String CHAT_TEXT_FREQ_NORM_FACTOR                = CHAT_TEXT_FREQ_NORM + "factor";
    public static final String CHAT_TEXT_FREQ_NORM_LEVEL                 = CHAT_TEXT_FREQ_NORM + "level";
    public static final String CHAT_TEXT_FREQ_NORM_WEIGHT                = CHAT_TEXT_FREQ_NORM + "weight";
    public static final String CHAT_TEXT_FREQ_NORM_MIN                   = CHAT_TEXT_FREQ_NORM + "minimum";
    public static final String CHAT_TEXT_FREQ_NORM_ACTIONS               = CHAT_TEXT_FREQ_NORM + "actions";
    private static final String CHAT_TEXT_FREQ_SHORTTERM                 = CHAT_TEXT_FREQ + "short-term.";
    public static final String CHAT_TEXT_FREQ_SHORTTERM_FACTOR           = CHAT_TEXT_FREQ_SHORTTERM + "factor";
    public static final String CHAT_TEXT_FREQ_SHORTTERM_LEVEL            = CHAT_TEXT_FREQ_SHORTTERM + "level";
    public static final String CHAT_TEXT_FREQ_SHORTTERM_WEIGHT           = CHAT_TEXT_FREQ_SHORTTERM + "weight";
    public static final String  CHAT_TEXT_FREQ_SHORTTERM_MIN             = CHAT_TEXT_FREQ_SHORTTERM + "minimum";
    public static final String CHAT_TEXT_FREQ_SHORTTERM_ACTIONS          = CHAT_TEXT_FREQ_SHORTTERM + "actions";

    // (Some of the following paths must be public for generic config reading.)
    // Per message checks.
    private static final String CHAT_TEXT_MSG                      = CHAT_TEXT + "message.";
    public static final String  CHAT_TEXT_MSG_LETTERCOUNT          = CHAT_TEXT_MSG + "letter-count";
    public static final String  CHAT_TEXT_MSG_PARTITION            = CHAT_TEXT_MSG + "partition";
    public static final String  CHAT_TEXT_MSG_UPPERCASE            = CHAT_TEXT_MSG + "uppercase";

    public static final String CHAT_TEXT_MSG_REPEATCANCEL          = CHAT_TEXT_MSG + "repeat-violation";
    public static final String CHAT_TEXT_MSG_AFTERJOIN             = CHAT_TEXT_MSG + "after-join";
    public static final String CHAT_TEXT_MSG_REPEATSELF            = CHAT_TEXT_MSG + "repeat-self";
    public static final String CHAT_TEXT_MSG_REPEATGLOBAL          = CHAT_TEXT_MSG + "repeat-global";
    public static final String CHAT_TEXT_MSG_NOMOVING              = CHAT_TEXT_MSG + "nomoving";

    private static final String CHAT_TEXT_MSG_WORDS                = CHAT_TEXT_MSG + "words.";
    public static final String  CHAT_TEXT_MSG_WORDS_LENGTHAV       = CHAT_TEXT_MSG_WORDS + "length-av";
    public static final String  CHAT_TEXT_MSG_WORDS_LENGTHMSG      = CHAT_TEXT_MSG_WORDS + "length-msg";
    public static final String  CHAT_TEXT_MSG_WORDS_NOLETTER       = CHAT_TEXT_MSG_WORDS + "no-letter";
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

    private static final String CHAT_LOGINS                              = CHAT + "logins.";
    public static final String  CHAT_LOGINS_CHECK                        = CHAT_LOGINS + SUB_ACTIVE;
    public static final String  CHAT_LOGINS_PERWORLDCOUNT                = CHAT_LOGINS + "per-world-count";
    public static final String  CHAT_LOGINS_SECONDS                      = CHAT_LOGINS + "seconds";
    public static final String  CHAT_LOGINS_LIMIT                        = CHAT_LOGINS + "limit";
    public static final String  CHAT_LOGINS_KICKMESSAGE                  = CHAT_LOGINS + "kick-message";
    public static final String  CHAT_LOGINS_STARTUPDELAY                 = CHAT_LOGINS + "startup-delay";

    private static final String CHAT_RELOG                               = CHAT + "relog.";
    public static final String  CHAT_RELOG_CHECK                         = CHAT_RELOG + SUB_ACTIVE;
    public static final String  CHAT_RELOG_KICKMESSAGE                   = CHAT_RELOG + "kick-message";
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

    private static final String COMBINED_ENDERPEARL                      = COMBINED + "enderpearl.";
    public static final String  COMBINED_ENDERPEARL_CHECK                = COMBINED_ENDERPEARL + SUB_ACTIVE;
    public static final String  COMBINED_ENDERPEARL_PREVENTCLICKBLOCK    = COMBINED_ENDERPEARL + "prevent-click-on-block";

    private static final String COMBINED_IMPROBABLE                      = COMBINED + "improbable.";
    public static final String  COMBINED_IMPROBABLE_CHECK                = COMBINED_IMPROBABLE + SUB_ACTIVE;
    public static final String  COMBINED_IMPROBABLE_LEVEL                = COMBINED_IMPROBABLE + "level";
    public static final String  COMBINED_IMPROBABLE_ACTIONS              = COMBINED_IMPROBABLE + "actions";

    private static final String COMBINED_INVULNERABLE                       = COMBINED + "invulnerable.";
    public static final String  COMBINED_INVULNERABLE_CHECK                 = COMBINED_INVULNERABLE + SUB_ACTIVE;
    private static final String COMBINED_INVULNERABLE_INITIALTICKS          = COMBINED_INVULNERABLE + "initial-ticks.";
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
    public static final String  COMBINED_YAWRATE_IMPROBABLE_FEEDONLY     = COMBINED_YAWRATE_IMPROBABLE + "feed-only";
    public static final String  COMBINED_YAWRATE_IMPROBABLE_WEIGHT       = COMBINED_YAWRATE_IMPROBABLE + "weight";
    private static final String COMBINED_YAWRATE_PENALTY                 = COMBINED_YAWRATE + "penalty.";
    public static final String  COMBINED_YAWRATE_PENALTY_FACTOR          = COMBINED_YAWRATE_PENALTY + "factor";
    public static final String  COMBINED_YAWRATE_PENALTY_MIN             = COMBINED_YAWRATE_PENALTY + "minimum";
    public static final String  COMBINED_YAWRATE_PENALTY_MAX             = COMBINED_YAWRATE_PENALTY + "maximum";

    public static final String  FIGHT                                    = CHECKS + "fight.";
    public static final String  FIGHT_ACTIVE                             = FIGHT + SUB_ACTIVE;

    public static final String  FIGHT_CANCELDEAD                         = FIGHT + "cancel-dead";
    public static final String  FIGHT_TOOLCHANGEPENALTY                  = FIGHT + "tool-change-penalty";
	public static final String  FIGHT_MAXLOOPLETENCYTICKS                = FIGHT + "max-loop-latency-ticks";
    public static final String  FIGHT_KNOCKBACKVELOCITY                  = FIGHT + "knockback-velocity";

    private static final String FIGHT_ANGLE                              = FIGHT + "angle.";
    public static final String  FIGHT_ANGLE_CHECK                        = FIGHT_ANGLE + SUB_ACTIVE;
    public static final String  FIGHT_ANGLE_THRESHOLD                    = FIGHT_ANGLE + "threshold.";
    public static final String  FIGHT_ANGLE_THRESHOLD_MOVE               = FIGHT_ANGLE_THRESHOLD + "avg-move";
    public static final String  FIGHT_ANGLE_THRESHOLD_TIME               = FIGHT_ANGLE_THRESHOLD + "avg-time";
    public static final String  FIGHT_ANGLE_THRESHOLD_YAW                = FIGHT_ANGLE_THRESHOLD + "avg-yaw";
    public static final String  FIGHT_ANGLE_THRESHOLD_SWITCH             = FIGHT_ANGLE_THRESHOLD + "avg-switch";
    public static final String  FIGHT_ANGLE_ACTIONS                      = FIGHT_ANGLE + "actions";
    
    private static final String FIGHT_CRITICAL                           = FIGHT + "critical.";
    public static final String  FIGHT_CRITICAL_CHECK                     = FIGHT_CRITICAL + SUB_ACTIVE;
    public static final String  FIGHT_CRITICAL_ACTIONS                   = FIGHT_CRITICAL + "actions";

    private static final String FIGHT_DIRECTION                          = FIGHT + "direction.";
    public static final String  FIGHT_DIRECTION_CHECK                    = FIGHT_DIRECTION + SUB_ACTIVE;
    public static final String  FIGHT_DIRECTION_STRICT                   = FIGHT_DIRECTION + "strict";
	public static final String  FIGHT_DIRECTION_FAILALL                  = FIGHT_DIRECTION + "failall";
    public static final String  FIGHT_DIRECTION_LOOPPRECISION            = FIGHT_DIRECTION + "loop-precision";
    public static final String  FIGHT_DIRECTION_STRICTANGLEPRECISION     = FIGHT_DIRECTION + "strict-angle-precision";
    public static final String  FIGHT_DIRECTION_PENALTY                  = FIGHT_DIRECTION + "penalty";
    public static final String  FIGHT_DIRECTION_ACTIONS                  = FIGHT_DIRECTION + "actions";

    private static final String FIGHT_FASTHEAL                           = FIGHT + "fastheal.";
    public static final String  FIGHT_FASTHEAL_CHECK                     = FIGHT_FASTHEAL + SUB_ACTIVE;
    public static final String  FIGHT_FASTHEAL_INTERVAL                  = FIGHT_FASTHEAL + "interval";
    public static final String  FIGHT_FASTHEAL_BUFFER                    = FIGHT_FASTHEAL + "buffer";
    public static final String  FIGHT_FASTHEAL_ACTIONS                   = FIGHT_FASTHEAL + "actions";

    private static final String FIGHT_GODMODE                            = FIGHT + "godmode.";
    public static final String  FIGHT_GODMODE_CHECK                      = FIGHT_GODMODE + SUB_ACTIVE;
    public static final String  FIGHT_GODMODE_LAGMINAGE                  = FIGHT_GODMODE + "min-age";
    public static final String  FIGHT_GODMODE_LAGMAXAGE                  = FIGHT_GODMODE + "max-age";
    public static final String  FIGHT_GODMODE_ACTIONS                    = FIGHT_GODMODE + "actions";

    private static final String FIGHT_NOSWING                            = FIGHT + "noswing.";
    public static final String  FIGHT_NOSWING_CHECK                      = FIGHT_NOSWING + SUB_ACTIVE;
    public static final String  FIGHT_NOSWING_ACTIONS                    = FIGHT_NOSWING + "actions";

    private static final String FIGHT_IMPOSSIBLEHIT                      = FIGHT + "impossiblehit.";
    public static final String  FIGHT_IMPOSSIBLEHIT_CHECK                = FIGHT_IMPOSSIBLEHIT + SUB_ACTIVE;
    private static final String FIGHT_IMPOSSIBLEHIT_IMPROBABLE           = FIGHT_IMPOSSIBLEHIT + "improbable.";
    public static final String  FIGHT_IMPOSSIBLEHIT_IMPROBABLE_WEIGHT    = FIGHT_IMPOSSIBLEHIT_IMPROBABLE + "weight";
    public static final String  FIGHT_IMPOSSIBLEHIT_ACTIONS              = FIGHT_IMPOSSIBLEHIT + "actions";

    private static final String FIGHT_REACH                              = FIGHT + "reach.";
    public static final String  FIGHT_REACH_CHECK                        = FIGHT_REACH + SUB_ACTIVE;
    public static final String  FIGHT_REACH_SURVIVALDISTANCE             = FIGHT_REACH + "survival-distance";
    public static final String  FIGHT_REACH_PENALTY                      = FIGHT_REACH + "penalty";
    public static final String  FIGHT_REACH_PRECISION                    = FIGHT_REACH + "precision";
    public static final String  FIGHT_REACH_REDUCE                       = FIGHT_REACH + "reduce";
    public static final String  FIGHT_REACH_REDUCEDISTANCE               = FIGHT_REACH + "reduce-distance";
    public static final String  FIGHT_REACH_REDUCESTEP                   = FIGHT_REACH + "reduce-step";
    private static final String FIGHT_REACH_IMPROBABLE                   = FIGHT_REACH + "improbable.";
    public static final String  FIGHT_REACH_IMPROBABLE_FEEDONLY          = FIGHT_REACH_IMPROBABLE + "feed-only";
    public static final String  FIGHT_REACH_IMPROBABLE_WEIGHT            = FIGHT_REACH_IMPROBABLE + "weight";
    public static final String  FIGHT_REACH_ACTIONS                      = FIGHT_REACH + "actions";

    public static final String FIGHT_SELFHIT                             = FIGHT + "selfhit.";
    public static final String FIGHT_SELFHIT_CHECK                       = FIGHT_SELFHIT + SUB_ACTIVE;
	public static final String FIGHT_SELFHIT_EXCLUDEPROJECTILE           = FIGHT_SELFHIT + "exclude-projectile";
	public static final String FIGHT_SELFHIT_MESSAGE                     = FIGHT_SELFHIT + "warn-player";
    public static final String FIGHT_SELFHIT_ACTIONS                     = FIGHT_SELFHIT + "actions";
    
    private static final String FIGHT_YAWRATE                            = FIGHT + "yawrate.";
    public static final String  FIGHT_YAWRATE_CHECK                      = FIGHT_YAWRATE + SUB_ACTIVE;

    public static final String  INVENTORY                                = CHECKS + "inventory.";
    public static final String  INVENTORY_ACTIVE                         = INVENTORY + SUB_ACTIVE;

    private static final String INVENTORY_FASTCLICK                      = INVENTORY + "fastclick.";
    public static final String  INVENTORY_FASTCLICK_CHECK                = INVENTORY_FASTCLICK + SUB_ACTIVE;
    public static final String  INVENTORY_FASTCLICK_EXCLUDE              = INVENTORY_FASTCLICK + "exclude";
    public static final String  INVENTORY_FASTCLICK_SPARECREATIVE        = INVENTORY_FASTCLICK + "spare-creative";
    private static final String INVENTORY_FASTCLICK_LIMIT                = INVENTORY_FASTCLICK + "limit.";
    public static final String  INVENTORY_FASTCLICK_LIMIT_SHORTTERM      = INVENTORY_FASTCLICK_LIMIT + "short-term";
    public static final String  INVENTORY_FASTCLICK_LIMIT_NORMAL         = INVENTORY_FASTCLICK_LIMIT + "normal";
    public static final String  INVENTORY_FASTCLICK_MIN_INTERACT_TIME    = INVENTORY_FASTCLICK + "min-interaction-duration";
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
    public static final String  INVENTORY_GUTENBERG_PAGELIMIT            = INVENTORY_GUTENBERG + "page-limit";
    public static final String  INVENTORY_GUTENBERG_ACTIONS              = INVENTORY_GUTENBERG + "actions";

    private static final String INVENTORY_INSTANTBOW                     = INVENTORY + "instantbow.";
    public static final String  INVENTORY_INSTANTBOW_CHECK               = INVENTORY_INSTANTBOW + SUB_ACTIVE;
    public static final String  INVENTORY_INSTANTBOW_STRICT              = INVENTORY_INSTANTBOW + "strict";
    public static final String  INVENTORY_INSTANTBOW_DELAY               = INVENTORY_INSTANTBOW + "delay";
    private static final String INVENTORY_INSTANTBOW_IMPROBABLE          = INVENTORY_INSTANTBOW + "improbable.";
    public static final String  INVENTORY_INSTANTBOW_IMPROBABLE_FEEDONLY = INVENTORY_INSTANTBOW_IMPROBABLE + "feed-only";
    public static final String  INVENTORY_INSTANTBOW_IMPROBABLE_WEIGHT   = INVENTORY_INSTANTBOW_IMPROBABLE + "weight";
    public static final String  INVENTORY_INSTANTBOW_ACTIONS             = INVENTORY_INSTANTBOW + "actions";

    private static final String INVENTORY_OPEN                           = INVENTORY + "open.";
    public static final String INVENTORY_OPEN_CHECK                     = INVENTORY_OPEN + SUB_ACTIVE;
    public static final String INVENTORY_OPEN_CLOSE                     = INVENTORY_OPEN + "close";
    public static final String INVENTORY_OPEN_CLOSE_ON_MOVE             = INVENTORY_OPEN + "close-on-move";
    public static final String INVENTORY_OPEN_DISABLE_CREATIVE           = INVENTORY_OPEN + "disable-creative";
    public static final String INVENTORY_OPEN_IMPROBABLE_WEIGHT          = INVENTORY_OPEN + "improbable-weight";

    // Inventory hot-fix.
    private static final String INVENTORY_HOTFIX                            = INVENTORY + "hotfix.";
    private static final String INVENTORY_HOTFIX_DUPE                       = INVENTORY_HOTFIX + "duplication.";
    public static final String  INVENTORY_HOTFIX_DUPE_FALLINGBLOCKENDPORTAL = INVENTORY_HOTFIX_DUPE + "falling-block-endportal";

    public static final String  MOVING                                   = CHECKS + "moving.";
    public static final String  MOVING_ACTIVE                            = MOVING + SUB_ACTIVE;

    private static final String MOVING_CREATIVEFLY                       = MOVING + "creativefly.";
    public static final String  MOVING_CREATIVEFLY_CHECK                 = MOVING_CREATIVEFLY + SUB_ACTIVE;
    public static final String  MOVING_CREATIVEFLY_IGNORECREATIVE        = MOVING_CREATIVEFLY + "ignore-creative";
    public static final String  MOVING_CREATIVEFLY_IGNOREALLOWFLIGHT     = MOVING_CREATIVEFLY + "ignore-allow-flight";
    public static final String  MOVING_CREATIVEFLY_MODEL                 = MOVING_CREATIVEFLY + SUB_MODEL + ".";
    public static final String  MOVING_CREATIVEFLY_ACTIONS               = MOVING_CREATIVEFLY + "actions";
    public static final String  MOVING_CREATIVEFLY_EYTRA_FWRESET         = MOVING_CREATIVEFLY_MODEL + "elytra.reset-Fw-Onground";
    public static final String  MOVING_CREATIVEFLY_EYTRA_STRICT          = MOVING_CREATIVEFLY_MODEL + "elytra.strict";

    private static final String MOVING_MOREPACKETS                       = MOVING + "morepackets.";
    public static final String  MOVING_MOREPACKETS_CHECK                 = MOVING_MOREPACKETS + SUB_ACTIVE;
    public static final String  MOVING_MOREPACKETS_SECONDS               = MOVING_MOREPACKETS + "seconds";
    public static final String  MOVING_MOREPACKETS_EPSIDEAL              = MOVING_MOREPACKETS + "eps-ideal";
    public static final String  MOVING_MOREPACKETS_EPSMAX                = MOVING_MOREPACKETS + "eps-max";
    private static final String MOVING_MOREPACKETS_BURST                 = MOVING_MOREPACKETS + "burst.";
    public static final String  MOVING_MOREPACKETS_BURST_PACKETS         = MOVING_MOREPACKETS_BURST + "packets";
    public static final String  MOVING_MOREPACKETS_BURST_DIRECT          = MOVING_MOREPACKETS_BURST + "direct-violation";
    public static final String  MOVING_MOREPACKETS_BURST_EPM             = MOVING_MOREPACKETS_BURST + "epm-violation";
    public static final String  MOVING_MOREPACKETS_SETBACKAGE            = MOVING_MOREPACKETS + "setbackage";
    public static final String  MOVING_MOREPACKETS_ACTIONS               = MOVING_MOREPACKETS + "actions";

    private static final String MOVING_NOFALL                            = MOVING + "nofall.";
    public static final String  MOVING_NOFALL_CHECK                      = MOVING_NOFALL + SUB_ACTIVE;
    public static final String  MOVING_NOFALL_DEALDAMAGE                 = MOVING_NOFALL + "deal-damage";
    public static final String  MOVING_NOFALL_SKIPALLOWFLIGHT            = MOVING_NOFALL + "skip-allow-flight";
    // TODO: A reset section (violation, teleport, vehicle) + @Moved.
    public static final String  MOVING_NOFALL_RESETONVL                  = MOVING_NOFALL + "reset-on-violation";
    public static final String  MOVING_NOFALL_RESETONTP                  = MOVING_NOFALL + "reset-on-teleport";
    public static final String  MOVING_NOFALL_RESETONVEHICLE             = MOVING_NOFALL + "reset-on-vehicle";
    public static final String  MOVING_NOFALL_ANTICRITICALS              = MOVING_NOFALL + "anti-criticals";
    public static final String  MOVING_NOFALL_ACTIONS                    = MOVING_NOFALL + "actions";

    public static final String  MOVING_PASSABLE                             = MOVING + "passable.";
    public static final String  MOVING_PASSABLE_CHECK                       = MOVING_PASSABLE + SUB_ACTIVE;
    //private static final String MOVING_PASSABLE_RAYTRACING                  = MOVING_PASSABLE + "raytracing.";
    public static final String  MOVING_PASSABLE_ACTIONS                     = MOVING_PASSABLE + "actions";
    public static final String  MOVING_PASSABLE_RT_XZ_FACTOR                = MOVING_PASSABLE + "horizontal-margins";
    public static final String  MOVING_PASSABLE_RT_Y_FACTOR                 = MOVING_PASSABLE + "vertical-margins";
    private static final String MOVING_PASSABLE_UNTRACKED                   = MOVING_PASSABLE + "untracked.";
    private static final String MOVING_PASSABLE_UNTRACKED_TELEPORT          = MOVING_PASSABLE_UNTRACKED + "teleport.";
    public static final String  MOVING_PASSABLE_UNTRACKED_TELEPORT_ACTIVE   = MOVING_PASSABLE_UNTRACKED_TELEPORT + SUB_ACTIVE;
    private static final String MOVING_PASSABLE_UNTRACKED_CMD               = MOVING_PASSABLE_UNTRACKED + "command.";
    public static final String  MOVING_PASSABLE_UNTRACKED_CMD_ACTIVE        = MOVING_PASSABLE_UNTRACKED_CMD + SUB_ACTIVE;
    public static final String  MOVING_PASSABLE_UNTRACKED_CMD_TRYTELEPORT   = MOVING_PASSABLE_UNTRACKED_CMD + "try-teleport";
    public static final String  MOVING_PASSABLE_UNTRACKED_CMD_PREFIXES      = MOVING_PASSABLE_UNTRACKED_CMD + "prefixes";

    private static final String MOVING_SURVIVALFLY                          = MOVING + "survivalfly.";
    public static final String MOVING_SURVIVALFLY_CHECK                     = MOVING_SURVIVALFLY + SUB_ACTIVE;
    public static final String MOVING_SURVIVALFLY_STEPHEIGHT                = MOVING_SURVIVALFLY + "stepheight";
    private static final String MOVING_SURVIVALFLY_EXTENDED                 = MOVING_SURVIVALFLY + "extended.";
    public static final String MOVING_SURVIVALFLY_EXTENDED_NOSLOW           = MOVING_SURVIVALFLY_EXTENDED + "noslow";
    public static final String MOVING_SURVIVALFLY_EXTENDED_RESETITEM        = MOVING_SURVIVALFLY_EXTENDED + "reset-activeitem";
    private static final String MOVING_SURVIVALFLY_LENIENCY                 = MOVING_SURVIVALFLY + "leniency.";
    public static final String MOVING_SURVIVALFLY_LENIENCY_FREEZECOUNT      = MOVING_SURVIVALFLY_LENIENCY + "freeze-count";
    public static final String MOVING_SURVIVALFLY_LENIENCY_FREEZEINAIR      = MOVING_SURVIVALFLY_LENIENCY + "freeze-inair";
    private static final String MOVING_SURVIVALFLY_SETBACKPOLICY            = MOVING_SURVIVALFLY + "setback-policy.";
    public static final String MOVING_SURVIVALFLY_SETBACKPOLICY_FALLDAMAGE  = MOVING_SURVIVALFLY_SETBACKPOLICY + "falldamage";
    public static final String MOVING_SURVIVALFLY_SETBACKPOLICY_VOIDTOVOID  = MOVING_SURVIVALFLY_SETBACKPOLICY + "void-to-void";
    public static final String MOVING_SURVIVALFLY_ACTIONS                   = MOVING_SURVIVALFLY + "actions";
    private static final String MOVING_SURVIVALFLY_VLFREQUENCY              = MOVING_SURVIVALFLY_LENIENCY + "violationfrequency.";
    public static final String MOVING_SURVIVALFLY_VLFREQUENCY_ACTIVE        = MOVING_SURVIVALFLY_VLFREQUENCY + "active";
    public static final String MOVING_SURVIVALFLY_VLFREQUENCY_DEBUG         = MOVING_SURVIVALFLY_VLFREQUENCY + "debug";
    public static final String MOVING_SURVIVALFLY_VLFREQUENCY_MAXTHRESHOLDVL = MOVING_SURVIVALFLY_VLFREQUENCY + "max-threshold-vl";
    public static final String MOVING_SURVIVALFLY_VLFREQUENCY_NOADDITIONVL  = MOVING_SURVIVALFLY_VLFREQUENCY + "no-addition-vl";
    public static final String MOVING_SURVIVALFLY_VLFREQUENCY_LASTVIOLATEDMOVECOUNT = MOVING_SURVIVALFLY_VLFREQUENCY + "last-violated-move-count";
    public static final String MOVING_SURVIVALFLY_VLFREQUENCY_AMOUNTTOADD   = MOVING_SURVIVALFLY_VLFREQUENCY + "amount-to-add";

    @GlobalConfig
    public static final String  MOVING_SURVIVALFLY_HOVER                    = MOVING_SURVIVALFLY + "hover.";
    public static final String  MOVING_SURVIVALFLY_HOVER_CHECK              = MOVING_SURVIVALFLY_HOVER + SUB_ACTIVE;
    public static final String  MOVING_SURVIVALFLY_HOVER_STEP               = MOVING_SURVIVALFLY_HOVER + "step";
    public static final String  MOVING_SURVIVALFLY_HOVER_TICKS              = MOVING_SURVIVALFLY_HOVER + "ticks";
    public static final String  MOVING_SURVIVALFLY_HOVER_LOGINTICKS         = MOVING_SURVIVALFLY_HOVER + "login-ticks";
    public static final String  MOVING_SURVIVALFLY_HOVER_FALLDAMAGE         = MOVING_SURVIVALFLY_HOVER + "fall-damage";
    public static final String  MOVING_SURVIVALFLY_HOVER_SFVIOLATION        = MOVING_SURVIVALFLY_HOVER + "sf-violation";

    // Special (to be sorted in or factored out).
    private static final String MOVING_VELOCITY                             = MOVING + "velocity.";
    public static final String  MOVING_VELOCITY_ACTIVATIONCOUNTER           = MOVING_VELOCITY + "activation-counter";
    public static final String  MOVING_VELOCITY_ACTIVATIONTICKS             = MOVING_VELOCITY + "activation-ticks";

    public static final String  MOVING_NOFALL_YONGROUND                     = MOVING_NOFALL + "yonground";
    public static final String  MOVING_YONGROUND                            = MOVING + "yonground";

    // General.
    public static final String  MOVING_TEMPKICKILLEGAL                      = MOVING + "temp-kick-illegal";
    public static final String  MOVING_IGNORESTANCE                         = MOVING + "ignore-stance";
    // TODO: Might add a section for illegal move.
    private static final String MOVING_LOADCHUNKS                           = MOVING + "loadchunks.";
    public static final String  MOVING_LOADCHUNKS_JOIN                      = MOVING_LOADCHUNKS + "join";
    public static final String  MOVING_LOADCHUNKS_MOVE                      = MOVING_LOADCHUNKS + "move";
    public static final String  MOVING_LOADCHUNKS_TELEPORT                  = MOVING_LOADCHUNKS + "teleport";
    public static final String  MOVING_LOADCHUNKS_WORLDCHANGE               = MOVING_LOADCHUNKS + "world-change";
    public static final String  MOVING_SPEEDGRACE                           = MOVING + "speed-grace";
    public static final String  MOVING_ENFORCELOCATION                      = MOVING + "enforce-location";
    private static final String MOVING_SETBACK                              = MOVING + "setback.";
    public static final String  MOVING_SETBACK_METHOD                       = MOVING_SETBACK + "method";

    private static final String MOVING_TRACE                                = MOVING + "trace.";
    public static final String  MOVING_TRACE_MAXAGE                         = MOVING_TRACE + "max-age";
    public static final String  MOVING_TRACE_MAXSIZE                        = MOVING_TRACE + "max-size";

    // Vehicles.
    private static final String MOVING_VEHICLE                              = MOVING + "vehicle.";
    public static final String  MOVING_VEHICLE_ENFORCELOCATION              = MOVING_VEHICLE + "enforce-location";
    public static final String  MOVING_VEHICLE_PREVENTDESTROYOWN            = MOVING_VEHICLE + "prevent-destroy-own";
    public static final String  MOVING_VEHICLE_SCHEDULESETBACKS             = MOVING_VEHICLE + "schedule-setbacks";
    public static final String  MOVING_VEHICLE_DELAYADDPASSENGER            = MOVING_VEHICLE + "schedule-set-passenger";
    public static final String  MOVING_VEHICLE_IGNOREDVEHICLES              = MOVING_VEHICLE + "ignored-vehicles";
    private static final String MOVING_VEHICLE_MOREPACKETS                  = MOVING_VEHICLE + "morepackets.";
    public static final String  MOVING_VEHICLE_MOREPACKETS_CHECK            = MOVING_VEHICLE_MOREPACKETS + SUB_ACTIVE;
    public static final String  MOVING_VEHICLE_MOREPACKETS_ACTIONS          = MOVING_VEHICLE_MOREPACKETS + "actions";
    private static final String MOVING_VEHICLE_ENVELOPE                     = MOVING_VEHICLE + "envelope.";
    public static final String  MOVING_VEHICLE_ENVELOPE_ACTIVE              = MOVING_VEHICLE_ENVELOPE + SUB_ACTIVE;
    public static final String  MOVING_VEHICLE_ENVELOPE_HSPEEDCAP           = MOVING_VEHICLE_ENVELOPE + "hdist-cap"; // Section.
    public static final String  MOVING_VEHICLE_ENVELOPE_ACTIONS             = MOVING_VEHICLE_ENVELOPE + "actions";

    private static final String MOVING_MESSAGE                              = MOVING + "message.";
    public static final  String MOVING_MESSAGE_ILLEGALPLAYERMOVE            = MOVING_MESSAGE + "illegal-player-move";
    public static final  String MOVING_MESSAGE_ILLEGALVEHICLEMOVE           = MOVING_MESSAGE + "illegal-vehicle-move";

    public static final String  NET                                         = CHECKS + "net.";
    public static final String  NET_ACTIVE                                  = NET + SUB_ACTIVE;

    private static final String NET_ATTACKFREQUENCY                         = NET + "attackfrequency.";
    public static final String  NET_ATTACKFREQUENCY_ACTIVE                  = NET_ATTACKFREQUENCY + SUB_ACTIVE;
    // TODO: Generic config for seconds.
    public static final String  NET_ATTACKFREQUENCY_SECONDS                 = NET_ATTACKFREQUENCY + "limitforseconds.";
    public static final String  NET_ATTACKFREQUENCY_SECONDS_HALF            = NET_ATTACKFREQUENCY_SECONDS + "half";
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
    public static final String  NET_FLYINGFREQUENCY_PACKETSPERSECOND        = NET_FLYINGFREQUENCY + "packets-per-second";
    public static final String  NET_FLYINGFREQUENCY_ACTIONS                 = NET_FLYINGFREQUENCY + "actions";

    private static final String NET_KEEPALIVEFREQUENCY                      = NET + "keepalivefrequency.";
    public static final String  NET_KEEPALIVEFREQUENCY_ACTIVE               = NET_KEEPALIVEFREQUENCY + SUB_ACTIVE;
    public static final String  NET_KEEPALIVEFREQUENCY_SECONDS              = NET_KEEPALIVEFREQUENCY + "seconds";
    public static final String  NET_KEEPALIVEFREQUENCY_ACTIONS              = NET_KEEPALIVEFREQUENCY + "actions";

    private static final String NET_MOVING                                  = NET + "moving.";
    public static final String  NET_MOVING_ACTIVE                           = NET_MOVING + SUB_ACTIVE;
    public static final String  NET_MOVING_ACTIONS                          = NET_MOVING + "actions";

    private static final String NET_PACKETFREQUENCY                         = NET + "packetfrequency.";
    public static final  String NET_PACKETFREQUENCY_ACTIVE                  = NET_PACKETFREQUENCY + SUB_ACTIVE;
    public static final  String NET_PACKETFREQUENCY_PPS                     = NET_PACKETFREQUENCY + "limit-per-second";
    public static final  String NET_PACKETFREQUENCY_SECONDS                 = NET_PACKETFREQUENCY + "seconds";
    public static final  String NET_PACKETFREQUENCY_ACTIONS                 = NET_PACKETFREQUENCY + "actions";

    private static final String NET_SOUNDDISTANCE                           = NET + "sounddistance.";
    public static final String  NET_SOUNDDISTANCE_ACTIVE                    = NET_SOUNDDISTANCE + SUB_ACTIVE;
    public static final String  NET_SOUNDDISTANCE_MAXDISTANCE               = NET_SOUNDDISTANCE + "max-distance";
    
    private static final String NET_SUPERSEDED                              = NET + "superseded.";
    private static final String NET_SUPERSEDED_FLYING                       = NET_SUPERSEDED + "flying.";
    public static final String  NET_SUPERSEDED_FLYING_CANCELWAITING         = NET_SUPERSEDED_FLYING + "cancel-waiting";
    
    private static final String NET_TOGGLEFREQUENCY                         = NET + "togglefrequency.";
    public static final String NET_TOGGLEFREQUENCY_ACTIVE                   = NET_TOGGLEFREQUENCY + SUB_ACTIVE;
    public static final String NET_TOGGLEFREQUENCY_SECONDS                  = NET_TOGGLEFREQUENCY + "seconds";
    public static final String NET_TOGGLEFREQUENCY_LIMIT                    = NET_TOGGLEFREQUENCY + "limit";
    public static final String NET_TOGGLEFREQUENCY_ACTIONS                  = NET_TOGGLEFREQUENCY + "actions";
    
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
    public static final String COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_BUKKITINTERFACE = COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC + "bukkit-npc";
    private static final String COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_METADATA = COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC + "metadata.";
    public static final String COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_METADATA_ACTIVE = COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_METADATA + SUB_ACTIVE;
    public static final String COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_METADATA_KEYS = COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_METADATA + "keys";
    public static final String COMPATIBILITY_EXEMPTIONS_REMOVE_JOIN      = COMPATIBILITY_EXEMPTIONS_REMOVE + "join";
    public static final String COMPATIBILITY_EXEMPTIONS_REMOVE_LEAVE     = COMPATIBILITY_EXEMPTIONS_REMOVE + "leave";
    // TODO: remove: tick, check / remove metadata on tick/leave?.

    public static final String COMPATIBILITY_SERVER                      = COMPATIBILITY + "server.";
    public static final String COMPATIBILITY_SERVER_CBDEDICATED          = COMPATIBILITY_SERVER + "CB-dedicated.";
    public static final String COMPATIBILITY_SERVER_CBDEDICATED_ENABLE   = COMPATIBILITY_SERVER_CBDEDICATED + "enable";
    public static final String COMPATIBILITY_SERVER_CBREFLECT            = COMPATIBILITY_SERVER + "CB-eflect.";
    public static final String COMPATIBILITY_SERVER_CBREFLECT_ENABLE     = COMPATIBILITY_SERVER_CBREFLECT + "enable";

    public static final  String COMPATIBILITY_BLOCKS                            = COMPATIBILITY + "blocks.";
    public static final  String COMPATIBILITY_BLOCKS_CHANGETRACKER              = COMPATIBILITY_BLOCKS + "change-tracker.";
    public static final  String COMPATIBILITY_BLOCKS_CHANGETRACKER_ACTIVE       = COMPATIBILITY_BLOCKS_CHANGETRACKER + SUB_ACTIVE;
    public static final  String COMPATIBILITY_BLOCKS_CHANGETRACKER_PISTONS      = COMPATIBILITY_BLOCKS_CHANGETRACKER + "pistons";
    public static final  String COMPATIBILITY_BLOCKS_CHANGETRACKER_MAXAGETICKS  = COMPATIBILITY_BLOCKS_CHANGETRACKER + "max-age-icks";
    private static final String COMPATIBILITY_BLOCKS_CHANGETRACKER_PERWORLD     = COMPATIBILITY_BLOCKS_CHANGETRACKER + "perworld.";
    public static final  String COMPATIBILITY_BLOCKS_CHANGETRACKER_PERWORLD_MAXENTRIES = COMPATIBILITY_BLOCKS_CHANGETRACKER_PERWORLD + "max-entries";

    // Moved paths.
    @Moved(newPath = LOGGING_BACKEND_CONSOLE_ACTIVE)
    public static final String  LOGGING_CONSOLE                          = "logging.console";
    @Moved(newPath = LOGGING_BACKEND_FILE_ACTIVE)
    public static final String  LOGGING_FILE                             = "logging.file";
    @Moved(newPath = LOGGING_BACKEND_FILE_FILENAME)
    public static final String  LOGGING_FILENAME                         = "logging.filename";
    @Moved(newPath = LOGGING_BACKEND_INGAMECHAT_ACTIVE)
    public static final String  LOGGING_INGAMECHAT                       = "logging.ingame-chat";
    @Moved(newPath = PROTECT_PLUGINS_HIDE_ACTIVE)
    public static final String  MISCELLANEOUS_PROTECTPLUGINS             = "miscellaneous.protect-plugins";
    @Moved(newPath = PROTECT_PLUGINS_HIDE_NOCOMMAND_MSG)
    public static  final String PROTECT_PLUGINS_HIDE_MSG_NOCOMMAND       = "protection.plugins.hide.messages.unknown-command";
    @Moved(newPath = PROTECT_PLUGINS_HIDE_NOPERMISSION_MSG)
    public static  final String PROTECT_PLUGINS_HIDE_MSG_NOPERMISSION    = "protection.plugins.hide.messages.no-permission";
    @Moved(newPath = PROTECT_COMMANDS_CONSOLEONLY_ACTIVE)
    public static final String  MISCELLANEOUS_OPINCONSOLEONLY            = "miscellaneous.opinconsoleonly";
    @Moved(newPath = INVENTORY_OPEN_CHECK)
    public static final String  INVENTORY_ENSURECLOSE                    = "checks.inventory.ensureclose";
    @Moved(newPath = LOGGING_EXTENDED_STATUS)
    public static final String LOGGING_DEBUG                             = "logging.debug";
    @Moved(newPath = MOVING_CREATIVEFLY_MODEL + "creative." + SUB_HORIZONTALSPEED)
    public static final String  MOVING_CREATIVEFLY_HORIZONTALSPEED       = "checks.moving.creativefly.horizontal-speed";
    @Moved(newPath = MOVING_CREATIVEFLY_MODEL + "creative." + SUB_VERTICALSPEED)
    public static final String  MOVING_CREATIVEFLY_VERTICALSPEED         = "checks.moving.creativefly.vertical-speed";
    @Moved(newPath = MOVING_CREATIVEFLY_MODEL + "creative." + SUB_MAXHEIGHT)
    public static final String  MOVING_CREATIVEFLY_MAXHEIGHT             = "checks.moving.creativefly.max-height";
    @Moved(newPath = MOVING_SURVIVALFLY_SETBACKPOLICY_FALLDAMAGE)
    public static final String MOVING_SURVIVALFLY_FALLDAMAGE             = "checks.moving.survivalfly.fall-damage";
    @Moved(newPath=MOVING_VEHICLE_ENFORCELOCATION)
    public static final String  MOVING_VEHICLES_ENFORCELOCATION          = "checks.moving.vehicles.enforce-location";
    @Moved(newPath=MOVING_VEHICLE_PREVENTDESTROYOWN)
    public static final String  MOVING_VEHICLES_PREVENTDESTROYOWN        = "checks.moving.vehicles.prevent-destroy-own";
    @Moved(newPath=MOVING_VEHICLE_MOREPACKETS_CHECK)
    public static final String  MOVING_MOREPACKETSVEHICLE_CHECK          = "checks.moving.morepacketsvehicle.active";
    @Moved(newPath=MOVING_VEHICLE_MOREPACKETS_ACTIONS)
    public static final String  MOVING_MOREPACKETSVEHICLE_ACTIONS        = "checks.moving.morepacketsvehicle.actions";

    // Deprecated paths (just removed).
    // TODO: Deprecate or rename (->falldistancemin)?
    @Deprecated
    public static final String  FIGHT_CRITICAL_FALLDISTANCE              = FIGHT_CRITICAL + "fall-distance";
    @Deprecated
    public static final String  FIGHT_CRITICAL_FALLDISTLENIENCY          = FIGHT_CRITICAL + "fall-dist-leniency";
    @Deprecated
    public static final String  MOVING_SPRINTINGGRACE                       = MOVING + "sprinting-grace";
    @Deprecated
    public static final String  MOVING_SPLITMOVES                           = MOVING + "split-moves"; // Needs better categories...
    @Deprecated
    public static final String  INVENTORY_FASTCLICK_TWEAKS1_5            = INVENTORY_FASTCLICK + "tweaks1_5";
    @Deprecated
    public static final  String INVENTORY_INVENTORYMOVE                  = INVENTORY + "inventorymove.";
    @Deprecated
    public static final  String INVENTORY_INVENTORYMOVE_CHECK            = INVENTORY_INVENTORYMOVE + SUB_ACTIVE;
    @Deprecated
    public static final  String INVENTORY_INVENTORYMOVE_DISABLECREATIVE  = INVENTORY_INVENTORYMOVE + "disable_creative";
    @Deprecated
    public static final  String INVENTORY_INVENTORYMOVE_HDISTDIVISOR     = INVENTORY_INVENTORYMOVE + "hdistdivisor";
    @Deprecated
    public static final  String INVENTORY_INVENTORYMOVE_IMPROBABLE       = INVENTORY_INVENTORYMOVE + "improbable.";
    @Deprecated
    public static final  String INVENTORY_INVENTORYMOVE_IMPROBABLE_FEEDONLY = INVENTORY_INVENTORYMOVE_IMPROBABLE + "feedonly";
    @Deprecated
    public static final  String INVENTORY_INVENTORYMOVE_IMPROBABLE_WEIGHT= INVENTORY_INVENTORYMOVE_IMPROBABLE + "weight";
    @Deprecated
    public static final  String INVENTORY_INVENTORYMOVE_ACTIONS          = INVENTORY_INVENTORYMOVE + "actions";
    @Deprecated
    public static final String MOVING_SURVIVALFLY_LENIENCY_HBUFMAX          = "checks.moving.survivalfly.leniency.hbufmax";
    @Deprecated
    public static final String MOVING_SURVIVALFLY_VLFREQUENCY_MAXTOTALVLS   = "checks.moving.survivalfly.violationfrequency.maxleniencyvl";
    @Deprecated
    public static final String MOVING_SURVIVALFLY_VLFREQUENCY_MINADDEDVLS   = "checks.moving.survivalfly.violationfrequency.mintoadd";
    @Deprecated
    public static final String MOVING_SURVIVALFLY_VLFREQUENCY_MOVECOUNT     = "checks.moving.survivalfly.violationfrequency.movecount";
    @Deprecated
    public static final String MOVING_SURVIVALFLY_VLFREQUENCY_MOREVLS       = "checks.moving.survivalfly.violationfrequency.morevls";
    @Deprecated
    private static final String INVENTORY_ITEMS                          = INVENTORY + "items.";
    @Deprecated
    public static final String  INVENTORY_ITEMS_CHECK                    = INVENTORY_ITEMS + SUB_ACTIVE;
    @Deprecated
    private static final String INVENTORY_INSTANTEAT                     = INVENTORY + "instanteat.";
    @Deprecated
    public static final String  INVENTORY_INSTANTEAT_CHECK               = INVENTORY_INSTANTEAT + SUB_ACTIVE;
    @Deprecated
    public static final String  INVENTORY_INSTANTEAT_ACTIONS             = INVENTORY_INSTANTEAT + "actions";
    @Deprecated
    public static final String  BLOCKPLACE_PREVENTMISC_BOATSANYWHERE        = BLOCKPLACE + "boatsanywhere";
    @Deprecated
    private static final String NET_FLYINGFREQUENCY_REDUNDANT               = NET_FLYINGFREQUENCY + "reduceredundant.";
    @Deprecated
    public static final String  NET_FLYINGFREQUENCY_REDUNDANT_ACTIVE        = NET_FLYINGFREQUENCY_REDUNDANT + SUB_ACTIVE;
    @Deprecated
    public static final String  NET_FLYINGFREQUENCY_REDUNDANT_SECONDS       = NET_FLYINGFREQUENCY_REDUNDANT + "seconds";
    @Deprecated
    public static final String  NET_FLYINGFREQUENCY_REDUNDANT_ACTIONS       = NET_FLYINGFREQUENCY_REDUNDANT + "actions";
    @Deprecated
    public static final String  MOVING_VELOCITY_STRICTINVALIDATION          = MOVING_VELOCITY + "strictinvalidation";
    @Deprecated
    private static final String FIGHT_SPEED                              = FIGHT + "speed.";
    @Deprecated
    public static final String  FIGHT_SPEED_CHECK                        = FIGHT_SPEED + SUB_ACTIVE;
    @Deprecated
    public static final String  FIGHT_SPEED_LIMIT                        = FIGHT_SPEED + "limit";
    @Deprecated
    private static final String FIGHT_SPEED_BUCKETS                      = FIGHT_SPEED + "buckets.";
    @Deprecated
    public static final String  FIGHT_SPEED_BUCKETS_N                    = FIGHT_SPEED_BUCKETS + "number";
    @Deprecated
    public static final String  FIGHT_SPEED_BUCKETS_DUR                  = FIGHT_SPEED_BUCKETS + "duration";
    @Deprecated
    public static final String  FIGHT_SPEED_BUCKETS_FACTOR               = FIGHT_SPEED_BUCKETS + "factor";
    @Deprecated
    private static final String FIGHT_SPEED_SHORTTERM                    = FIGHT_SPEED + "shortterm.";
    @Deprecated
    public static final String  FIGHT_SPEED_SHORTTERM_LIMIT              = FIGHT_SPEED_SHORTTERM + "limit";
    @Deprecated
    public static final String  FIGHT_SPEED_SHORTTERM_TICKS              = FIGHT_SPEED_SHORTTERM + "ticks";
    @Deprecated
    private static final String FIGHT_SPEED_IMPROBABLE                   = FIGHT_SPEED + "improbable.";
    @Deprecated
    public static final String  FIGHT_SPEED_IMPROBABLE_FEEDONLY          = FIGHT_SPEED_IMPROBABLE + "feedonly";
    @Deprecated
    public static final String  FIGHT_SPEED_IMPROBABLE_WEIGHT            = FIGHT_SPEED_IMPROBABLE + "weight";
    @Deprecated
    public static final String  FIGHT_SPEED_ACTIONS                      = FIGHT_SPEED + "actions";
    @Deprecated
    public static final String MOVING_SURVIVALFLY_SLOWNESSSPRINTHACK     = MOVING_SURVIVALFLY + "slownesssprinthack";
    @Deprecated
    public static final String MOVING_SURVIVALFLY_GROUNDHOP              = MOVING_SURVIVALFLY + "groundhop";
    @Deprecated
    public static final String  MOVING_ASSUMESPRINT                      = MOVING + "assumesprint";
    @Deprecated
    private static final String COMBINED_BEDLEAVE                        = COMBINED + "bedleave.";
    @Deprecated
    public static final String  COMBINED_BEDLEAVE_CHECK                  = COMBINED_BEDLEAVE + SUB_ACTIVE;
    @Deprecated
    public static final String  COMBINED_BEDLEAVE_ACTIONS                = COMBINED_BEDLEAVE + "actions";
    @Deprecated
    private static final String CHAT_COLOR                               = CHAT + "color.";
    @Deprecated
    public static final String  CHAT_COLOR_CHECK                         = CHAT_COLOR + SUB_ACTIVE;
    @Deprecated
    public static final String  CHAT_COLOR_ACTIONS                       = CHAT_COLOR + "actions";
    @Deprecated
    private static final String INVENTORY_DROP                           = INVENTORY + "drop.";
    @Deprecated
    public static final String  INVENTORY_DROP_CHECK                     = INVENTORY_DROP + SUB_ACTIVE;
    @Deprecated
    public static final String  INVENTORY_DROP_LIMIT                     = INVENTORY_DROP + "limit";
    @Deprecated
    public static final String  INVENTORY_DROP_TIMEFRAME                 = INVENTORY_DROP + "timeframe";
    @Deprecated
    public static final String  INVENTORY_DROP_ACTIONS                   = INVENTORY_DROP + "actions";
    @Deprecated
    public static final  String INVENTORY_INVENTORYMOVE_HDISTLENIENCY    = "checks.inventory.inventorymove.hdist_leniency";
    @Deprecated
    public static final  String INVENTORY_INVENTORYMOVE_HDISTMIN         = "checks.inventory.inventorymove.hdist_min";
    // Clients settings
    @Deprecated
    private static final String PROTECT_CLIENTS                          = PROTECT + "clients.";
    @Deprecated
    private static final String PROTECT_CLIENTS_MOTD                     = PROTECT_CLIENTS + "motd.";
    @Deprecated
    public static final String  PROTECT_CLIENTS_MOTD_ACTIVE              = PROTECT_CLIENTS_MOTD + SUB_ACTIVE;
    @Deprecated
    public static final String  PROTECT_CLIENTS_MOTD_ALLOWALL            = PROTECT_CLIENTS_MOTD + "allowall";
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
        final List<String> cfModels = Arrays.asList("creative", "spectator", "survival", "adventure", "elytra");
        multiEntries.add(new ManyMoved(MOVING_CREATIVEFLY_MODEL, cfModels, "horizontal-speed", SUB_HORIZONTAL_SPEED));
        multiEntries.add(new ManyMoved(MOVING_CREATIVEFLY_MODEL, cfModels, "mod-sprint", SUB_HORIZONTAL_MODSPRINT));
        multiEntries.add(new ManyMoved(MOVING_CREATIVEFLY_MODEL, cfModels, "max-height", SUB_VERTICAL_MAXHEIGHT));
        multiEntries.add(new ManyMoved(MOVING_CREATIVEFLY_MODEL, cfModels, "vertical-speed", SUB_VERTICAL_ASCEND_SPEED));

        // Expand ManyMoved entries.
        for (ManyMoved entry : multiEntries) {
            entries.addAll(entry.getWrapMoved());
        }
        return entries;
    }

}