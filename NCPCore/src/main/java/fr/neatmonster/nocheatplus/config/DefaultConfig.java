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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import org.bukkit.Material;

import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.BridgeMaterial;
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;

/**
 * These are the default settings for NoCheatPlus. They will be used in addition to/in replacement of configurations
 * given in the configFactory.yml file.
 */
public class DefaultConfig extends ConfigFile {

    // TODO: Other version details ?

    /**
     * Instantiates a new default configuration.
     */
    public DefaultConfig() {
        super();
        
        /** MC 1.18 expanded the height limit for building by 64 blocks up */
        final int cfMaxWorldY = ServerVersion.compareMinecraftVersion("1.18") >= 0 ? 200 : 128;

        // General.
        set(ConfPaths.SAVEBACKCONFIG, true, 785);

        // Config version.
        set(ConfPaths.CONFIGVERSION_NOTIFY, true, 785);
        set(ConfPaths.CONFIGVERSION_NOTIFYMAXPATHS, 5, 1085);
        //        not set(ConfPaths.CONFIGVERSION_CREATED, -1);
        //        not set(ConfPaths.CONFIGVERSION_SAVED, -1);
        set(ConfPaths.LOGGING_ACTIVE, true, 785);
        set(ConfPaths.LOGGING_MAXQUEUESIZE, 5000, 785);
        set(ConfPaths.LOGGING_EXTENDED_STATUS, false, 785);
        set(ConfPaths.LOGGING_EXTENDED_COMMANDS_ACTIONS, false, 1090);
        set(ConfPaths.LOGGING_EXTENDED_ALLVIOLATIONS_DEBUG, true, 785);
        set(ConfPaths.LOGGING_EXTENDED_ALLVIOLATIONS_DEBUGONLY, false, 785);
        set(ConfPaths.LOGGING_EXTENDED_ALLVIOLATIONS_BACKEND_TRACE, false, 785);
        set(ConfPaths.LOGGING_EXTENDED_ALLVIOLATIONS_BACKEND_NOTIFY, false, 785);
        set(ConfPaths.LOGGING_BACKEND_CONSOLE_ACTIVE, true, 785);
        set(ConfPaths.LOGGING_BACKEND_CONSOLE_ASYNCHRONOUS, true, 785);
        set(ConfPaths.LOGGING_BACKEND_FILE_ACTIVE, true, 785);
        set(ConfPaths.LOGGING_BACKEND_FILE_PREFIX, "", 785);
        set(ConfPaths.LOGGING_BACKEND_FILE_FILENAME, "nocheatplus.log", 785);
        set(ConfPaths.LOGGING_BACKEND_INGAMECHAT_ACTIVE, true, 785);
        set(ConfPaths.LOGGING_BACKEND_INGAMECHAT_PREFIX, "&7&l[&cNC+&7&l]&7 ", 1154);

        // Data settings.
        // Expired offline players data.
        set(ConfPaths.DATA_EXPIRATION_ACTIVE, false, 785);
        set(ConfPaths.DATA_EXPIRATION_DURATION, 60, 785);
        set(ConfPaths.DATA_EXPIRATION_HISTORY, false, 785);
        // Consistency checking.
        set(ConfPaths.DATA_CONSISTENCYCHECKS_CHECK, true, 785);
        set(ConfPaths.DATA_CONSISTENCYCHECKS_INTERVAL, 10, 785);
        set(ConfPaths.DATA_CONSISTENCYCHECKS_MAXTIME, 2, 785);
        set(ConfPaths.DATA_CONSISTENCYCHECKS_SUPPRESSWARNINGS, false, 785);

        // Permission settings.
        set(ConfPaths.PERMISSIONS_POLICY_DEFAULT, "ALWAYS", 1140);
        set(ConfPaths.PERMISSIONS_POLICY_RULES, Arrays.asList(
                "nocheatplus.notify :: INTERVAL:60, -world, -offline", // Not sure about this one.
                "nocheatplus.admin.debug :: INTERVAL:10",
                "nocheatplus.admin* :: ALWAYS",
                // TODO: Command permissions are always checked anyway :p. Will be changed...
                "nocheatplus.command* :: ALWAYS",
                "nocheatplus.bypass* :: ALWAYS",
                "regex:^nocheatplus\\.checks\\..*\\.silent$ :: FALSE",
                /*
                 * Relog, logins: Note: aims at login denial, would invalidate
                 * once offline/world change. +- not sure.
                 */
                "nocheatplus.checks* :: INTERVAL:6"
                ), 1154);


        /* Protection features. */
        // Hide plugins.
        set(ConfPaths.PROTECT_PLUGINS_HIDE_ACTIVE, true, 785);
        set(ConfPaths.PROTECT_PLUGINS_HIDE_NOPERMISSION_MSG, "&cI'm sorry, but you do not have permission to perform this command. Please contact the server administrators if you believe that this is in error.", 785);
        set(ConfPaths.PROTECT_PLUGINS_HIDE_NOPERMISSION_CMDS, Arrays.asList("plugins", "version", "icanhasbukkit"), 785);
        set(ConfPaths.PROTECT_PLUGINS_HIDE_NOCOMMAND_MSG, "Unknown command. Type \"/help\" for help.", 785);
        set(ConfPaths.PROTECT_PLUGINS_HIDE_NOCOMMAND_CMDS, new LinkedList<String>(), 785);
        // Commands (other).
        set(ConfPaths.PROTECT_COMMANDS_CONSOLEONLY_ACTIVE, false, 785);
        set(ConfPaths.PROTECT_COMMANDS_CONSOLEONLY_MSG, "&cI'm sorry, but this command can't be executed in chat. Use the console instead!", 785);
        set(ConfPaths.PROTECT_COMMANDS_CONSOLEONLY_CMDS, Arrays.asList("op", "deop"), 785);

        /* Checks! */
        set(ConfPaths.CHECKS_ACTIVE, true, 1144);
        set(ConfPaths.CHECKS_LAG, true, 1144);
        set(ConfPaths.CHECKS_DEBUG, false, 1144);


        /* BlockBreak */
        set(ConfPaths.BLOCKBREAK_ACTIVE, "default", 1144);
        // Direction
        set(ConfPaths.BLOCKBREAK_DIRECTION_CHECK, "default", 785);
        set(ConfPaths.BLOCKBREAK_DIRECTION_ACTIONS, "cancel", 1154);
        // FastBreak
        set(ConfPaths.BLOCKBREAK_FASTBREAK_CHECK, "default", 785);
        set(ConfPaths.BLOCKBREAK_FASTBREAK_STRICT, true, 785);
        set(ConfPaths.BLOCKBREAK_FASTBREAK_DELAY, 10, 785); 
        set(ConfPaths.BLOCKBREAK_FASTBREAK_MOD_SURVIVAL, 100, 785);
        set(ConfPaths.BLOCKBREAK_FASTBREAK_GRACE, 500, 785);
        set(ConfPaths.BLOCKBREAK_FASTBREAK_ACTIONS, "cancel vl>5 cancel log:fastbreak:4:2:i vl>50 cancel log:fastbreak:0:2:if cmdc:kickfastbreak:2:5", 1154);
        // Frequency
        set(ConfPaths.BLOCKBREAK_FREQUENCY_CHECK, "default", 785);
        set(ConfPaths.BLOCKBREAK_FREQUENCY_MOD_CREATIVE, 95, 785);
        set(ConfPaths.BLOCKBREAK_FREQUENCY_MOD_SURVIVAL, 45, 785);
        set(ConfPaths.BLOCKBREAK_FREQUENCY_SHORTTERM_TICKS, 5, 785);
        set(ConfPaths.BLOCKBREAK_FREQUENCY_SHORTTERM_LIMIT, 7, 785);
        set(ConfPaths.BLOCKBREAK_FREQUENCY_ACTIONS, "cancel vl>5 log:bbfrequency:3:5:i cancel vl>40 log:bbfrequency:0:5:if cancel cmdc:kickfrequency:0:5", 1154);
        // NoSwing
        set(ConfPaths.BLOCKBREAK_NOSWING_CHECK, "default", 785);
        set(ConfPaths.BLOCKBREAK_NOSWING_ACTIONS, "cancel vl>10 log:noswing:0:5:i cancel", 785);
        // Reach
        set(ConfPaths.BLOCKBREAK_REACH_CHECK, "default", 785);
        set(ConfPaths.BLOCKBREAK_REACH_ACTIONS, "cancel", 1154);
        // WrongBlock
        set(ConfPaths.BLOCKBREAK_WRONGBLOCK_CHECK, "default", 785);
        set(ConfPaths.BLOCKBREAK_WRONGBLOCK_LEVEL, 20, 1154);
        set(ConfPaths.BLOCKBREAK_WRONGBLOCK_IMPROBABLE_FEEDONLY, false, 1154);
        set(ConfPaths.BLOCKBREAK_WRONGBLOCK_IMPROBABLE_WEIGHT, 2.0, 1154);
        set(ConfPaths.BLOCKBREAK_WRONGBLOCK_ACTIONS, "cancel vl>10 log:bwrong:2:5:i cancel vl>30 cancel log:bwrong:0:5:if cmdc:kickwb:0:5", 1154);


        /* BlockInteract */
        set(ConfPaths.BLOCKINTERACT_ACTIVE, "default", 1144);
        // Direction
        set(ConfPaths.BLOCKINTERACT_DIRECTION_CHECK, "default", 785);
        set(ConfPaths.BLOCKINTERACT_DIRECTION_ACTIONS, "cancel vl>15 cancel log:bdirection:4:8:i vl>200 cancel log:bdirection:1:5:if cmdc:kickillegalblockinteract:1:5", 1154);
        // Reach
        set(ConfPaths.BLOCKINTERACT_REACH_CHECK, "default", 785);
        set(ConfPaths.BLOCKINTERACT_REACH_ACTIONS, "cancel log:breach:5:6:i", 1154);
        // Speed
        set(ConfPaths.BLOCKINTERACT_SPEED_CHECK, "default", 785);
        set(ConfPaths.BLOCKINTERACT_SPEED_INTERVAL, 2000, 785);
        set(ConfPaths.BLOCKINTERACT_SPEED_LIMIT, 55, 1154); 
        set(ConfPaths.BLOCKINTERACT_SPEED_ACTIONS, "cancel vl>10 cancel log:bspeed:5:4:i cancel vl>500 cancel log:bspeed:0:5:icf cmdc:kickbspeed:2:5", 1154);
        // Visible
        set(ConfPaths.BLOCKINTERACT_VISIBLE_CHECK, "default", 785);
        set(ConfPaths.BLOCKINTERACT_VISIBLE_ACTIONS, "cancel vl>30 log:bvisible:8:5:if cancel", 1154); 


        /* BlockPlace */
        set(ConfPaths.BLOCKPLACE_ACTIVE, "default", 1144);
        // Against
        set(ConfPaths.BLOCKPLACE_AGAINST_CHECK, "default", 785);
        set(ConfPaths.BLOCKPLACE_AGAINST_ACTIONS, "cancel log:against:1:5:i vl>10 cancel log:against:0:2:if cmdc:kickagainst:0:10", 1154);
        // AutoSign
        set(ConfPaths.BLOCKPLACE_AUTOSIGN_CHECK, "default", 785);
        set(ConfPaths.BLOCKPLACE_AUTOSIGN_SKIPEMPTY, false, 785);
        set(ConfPaths.BLOCKPLACE_AUTOSIGN_ACTIONS, "cancel vl>10 log:bautosign:0:3:if cancel", 785);
        // Direction
        set(ConfPaths.BLOCKPLACE_DIRECTION_CHECK, "default", 785);
        set(ConfPaths.BLOCKPLACE_DIRECTION_ACTIONS, "cancel", 1154);
        // FastPlace
        set(ConfPaths.BLOCKPLACE_FASTPLACE_CHECK, "default", 785);
        set(ConfPaths.BLOCKPLACE_FASTPLACE_LIMIT, 15, 1154); 
        set(ConfPaths.BLOCKPLACE_FASTPLACE_SHORTTERM_TICKS, 10, 785);
        set(ConfPaths.BLOCKPLACE_FASTPLACE_SHORTTERM_LIMIT, 6, 785);
        set(ConfPaths.BLOCKPLACE_FASTPLACE_IMPROBABLE_FEEDONLY, false, 1154);
        set(ConfPaths.BLOCKPLACE_FASTPLACE_IMPROBABLE_WEIGHT, 0.3, 1154);
        set(ConfPaths.BLOCKPLACE_FASTPLACE_ACTIONS, "cancel vl>5 cancel log:fastplace:8:3:i vl>20 cancel log:fastplace:2:4:i vl>80 cancel log:fastplace:0:10:if cmdc:kickfastplace:1:10", 1154);
        // Reach
        set(ConfPaths.BLOCKPLACE_REACH_CHECK, "default", 785);
        set(ConfPaths.BLOCKPLACE_REACH_ACTIONS, "cancel", 1154);
        // NoSwing
        set(ConfPaths.BLOCKPLACE_NOSWING_CHECK, "default", 785);
        set(ConfPaths.BLOCKPLACE_NOSWING_EXCEPTIONS, Arrays.asList(BridgeMaterial.LILY_PAD.toString(), Material.FLINT_AND_STEEL.toString()), 1154);
        set(ConfPaths.BLOCKPLACE_NOSWING_ACTIONS, "cancel vl>10 log:noswing:2:5:i cancel", 1154);
        // Scaffold
        set(ConfPaths.BLOCKPLACE_SCAFFOLD_CHECK, "default", 1154);
        set(ConfPaths.BLOCKPLACE_SCAFFOLD_ANGLE, true, 1154);
        set(ConfPaths.BLOCKPLACE_SCAFFOLD_SPRINT, true, 1154);
        set(ConfPaths.BLOCKPLACE_SCAFFOLD_TIME_ACTIVE, true, 1154);
        set(ConfPaths.BLOCKPLACE_SCAFFOLD_TIME_AVG, 2, 1154);
        set(ConfPaths.BLOCKPLACE_SCAFFOLD_ROTATE_ACTIVE, true, 1154);
        set(ConfPaths.BLOCKPLACE_SCAFFOLD_ROTATE_DIFFERENCE, 90, 1154);
        set(ConfPaths.BLOCKPLACE_SCAFFOLD_TOOLSWITCH, true, 1154);
        set(ConfPaths.BLOCKPLACE_SCAFFOLD_IMPROBABLE_FEEDONLY, false, 1154);
        set(ConfPaths.BLOCKPLACE_SCAFFOLD_IMPROBABLE_WEIGHT, 0.4, 1154);
        set(ConfPaths.BLOCKPLACE_SCAFFOLD_ACTIONS, "cancel vl>10 cancel log:scaffold:3:7:if vl>70 cancel log:scaffold:0:5:if cmd:clearscaffold:0:1 cmdc:kickscaffold:0:1", 1154);
        // Speed
        set(ConfPaths.BLOCKPLACE_SPEED_CHECK, "default", 785);
        set(ConfPaths.BLOCKPLACE_SPEED_INTERVAL, 35L, 1154); // Old limit: 45L < FPs with throwable potions.
        set(ConfPaths.BLOCKPLACE_SPEED_IMPROBABLE_FEEDONLY, true, 1154);
        set(ConfPaths.BLOCKPLACE_SPEED_IMPROBABLE_WEIGHT, 0.3, 1154);
        set(ConfPaths.BLOCKPLACE_SPEED_ACTIONS, "cancel vl>100 log:bpspeed:6:7:i cancel vl>1000 log:bpspeed:3:5:if cancel", 1154);
        // Misc preventions
        set(ConfPaths.BLOCKPLACE_PREVENTMISC_BOATSANYWHERE, true);


        /* Chat */
        set(ConfPaths.CHAT_ACTIVE, "default", 1144);
        // Captcha.
        set(ConfPaths.CHAT_CAPTCHA_CHECK, "false", 1154);
        set(ConfPaths.CHAT_CAPTCHA_SKIP_COMMANDS, false, 785);
        set(ConfPaths.CHAT_CAPTCHA_CHARACTERS, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!?><", 1154);
        set(ConfPaths.CHAT_CAPTCHA_LENGTH, 6, 785);
        set(ConfPaths.CHAT_CAPTCHA_QUESTION, "&c&l(!)&7 Please type '&6[captcha]&7' to continue using chat.", 1154);
        set(ConfPaths.CHAT_CAPTCHA_SUCCESS, "&c&l(!)&7 Antispam check passed.", 1154);
        set(ConfPaths.CHAT_CAPTCHA_TRIES, 3, 785);
        set(ConfPaths.CHAT_CAPTCHA_ACTIONS, "cancel cmdc:kickcaptcha vl>4 log:captcha:2:5:cf cancel cmdc:kickcaptcha", 785);
        // Commands
        set(ConfPaths.CHAT_COMMANDS_CHECK, "default", 785);
        set(ConfPaths.CHAT_COMMANDS_EXCLUSIONS, Arrays.asList("undo", "redo"), 1154);
        set(ConfPaths.CHAT_COMMANDS_HANDLEASCHAT, Arrays.asList("me", "msg", "emsg", "essentials:msg", "tell", "etell", "essentials:tell", "say", "esay", "essentials:say", "whisper", "ewhisper", "essentials:whisper", "w", "essentials:w", "ew", "r", "er", "essentials:r", "reply", "essentials:reply", "ereply"), 1154);
        set(ConfPaths.CHAT_COMMANDS_LEVEL, 10, 785);
        set(ConfPaths.CHAT_COMMANDS_SHORTTERM_TICKS, 18, 785);
        set(ConfPaths.CHAT_COMMANDS_SHORTTERM_LEVEL, 3, 785);
        set(ConfPaths.CHAT_COMMANDS_ACTIONS, "log:commands:0:5:cf cancel cmdc:kickcommands vl>20 log:commands:0:5:cf cancel cmdc:tempkick1", 1154);
        // Text (ordering on purpose).
        // Normal
        set(ConfPaths.CHAT_TEXT_CHECK, "default", 785);
        set(ConfPaths.CHAT_TEXT_ALLOWVLRESET, true, 785);
        set(ConfPaths.CHAT_TEXT_FREQ_NORM_MIN, 0.0, 785);
        set(ConfPaths.CHAT_TEXT_FREQ_NORM_FACTOR, 0.9D, 785);
        set(ConfPaths.CHAT_TEXT_FREQ_NORM_WEIGHT, 6, 785);
        set(ConfPaths.CHAT_TEXT_FREQ_NORM_LEVEL, 160, 785);
        set(ConfPaths.CHAT_TEXT_FREQ_NORM_ACTIONS, "cancel cmdc:tellchatnormal vl>7 log:chatnormal:0:5:cif cancel cmdc:tellchatnormal vl>20 log:chatnormal:0:5:cif cancel cmdc:kickchatnormal vl>40 log:chatnormal:0:5:cif cancel cmdc:kickchat5", 1154);
        // ShortTerm                                      
        set(ConfPaths.CHAT_TEXT_FREQ_SHORTTERM_MIN, 2.0, 785);
        set(ConfPaths.CHAT_TEXT_FREQ_SHORTTERM_FACTOR, 0.7, 785);
        set(ConfPaths.CHAT_TEXT_FREQ_SHORTTERM_WEIGHT, 3.0, 785);
        set(ConfPaths.CHAT_TEXT_FREQ_SHORTTERM_LEVEL, 20.0, 785);
        set(ConfPaths.CHAT_TEXT_FREQ_SHORTTERM_ACTIONS, "log:chatfast:2:3:icf cancel cmdc:kickchatfast:0:5 vl>20 cancel log:chatfast:0:5:cif cmdc:kickchatfast vl>40 cancel log:chatfast:0:5:cif cmdc:kickchat1", 1154);
        // Message
        set(ConfPaths.CHAT_TEXT_MSG_LETTERCOUNT, 1.0, 785);
        set(ConfPaths.CHAT_TEXT_MSG_PARTITION, 1.0, 785);
        set(ConfPaths.CHAT_TEXT_MSG_UPPERCASE, 1.2, 1154);
        set(ConfPaths.CHAT_TEXT_MSG_AFTERJOIN, 1.5, 785);
        set(ConfPaths.CHAT_TEXT_MSG_NOMOVING, 1.1, 1154);
        set(ConfPaths.CHAT_TEXT_MSG_REPEATCANCEL, 2.0, 1154);
        set(ConfPaths.CHAT_TEXT_MSG_REPEATGLOBAL, 1.0, 785);
        set(ConfPaths.CHAT_TEXT_MSG_REPEATSELF, 1.5, 785);
        set(ConfPaths.CHAT_TEXT_MSG_WORDS_LENGTHAV, 1.0, 785);
        set(ConfPaths.CHAT_TEXT_MSG_WORDS_LENGTHMSG, 1.0, 785);
        set(ConfPaths.CHAT_TEXT_MSG_WORDS_NOLETTER, 0.2, 1154);
        // Global
        set(ConfPaths.CHAT_TEXT_GL_CHECK, true, 785);
        set(ConfPaths.CHAT_TEXT_GL_WEIGHT, 0.5, 785);
        set(ConfPaths.CHAT_TEXT_GL_WORDS_CHECK, true, 1154);
        set(ConfPaths.CHAT_TEXT_GL_WEIGHT, 1.0, 785);
        set(ConfPaths.CHAT_TEXT_GL_PREFIXES_CHECK , false, 785);
        set(ConfPaths.CHAT_TEXT_GL_SIMILARITY_CHECK , true, 1154);
        // Player
        set(ConfPaths.CHAT_TEXT_PP_CHECK, true, 785);
        set(ConfPaths.CHAT_TEXT_PP_WORDS_CHECK, true, 1154);
        set(ConfPaths.CHAT_TEXT_PP_PREFIXES_CHECK, true, 1154);
        set(ConfPaths.CHAT_TEXT_PP_SIMILARITY_CHECK , true, 1154);
        // Warning (commands + chat).
        set(ConfPaths.CHAT_WARNING_CHECK, true, 785);
        set(ConfPaths.CHAT_WARNING_LEVEL, 67, 785);
        set(ConfPaths.CHAT_WARNING_MESSAGE, "\n  &c&l(!)&7 Please slow down chat, you might get kicked by the antispam.\n \n", 1154);
        set(ConfPaths.CHAT_WARNING_TIMEOUT, 10, 785);
        // Relog
        set(ConfPaths.CHAT_RELOG_CHECK, "default", 785);
        set(ConfPaths.CHAT_RELOG_TIMEOUT, 5000L, 785);
        set(ConfPaths.CHAT_RELOG_WARNING_MESSAGE, "&c&l(!)&7 You relogged really fast! If you keep doing that, you're going to be banned.", 1154);
        set(ConfPaths.CHAT_RELOG_WARNING_NUMBER, 1, 785);
        set(ConfPaths.CHAT_RELOG_KICKMESSAGE, "Too fast re-login, try with a little delay.", 785);
        set(ConfPaths.CHAT_RELOG_WARNING_TIMEOUT, 60000L, 785);
        set(ConfPaths.CHAT_RELOG_ACTIONS, "log:relog:0:10:cf cancel vl>20 log:relog:0:10:cf cancel cmdc:tempkick5", 1154);
        // Logins
        set(ConfPaths.CHAT_LOGINS_CHECK, "default", 785);
        set(ConfPaths.CHAT_LOGINS_STARTUPDELAY, 600, 785);
        set(ConfPaths.CHAT_LOGINS_PERWORLDCOUNT, false, 785);
        set(ConfPaths.CHAT_LOGINS_SECONDS, 10, 785);
        set(ConfPaths.CHAT_LOGINS_LIMIT, 10, 785);
        set(ConfPaths.CHAT_LOGINS_KICKMESSAGE, "Too many players are logging in at the same time, please try again later.", 1154);


        /* Combined */
        set(ConfPaths.COMBINED_ACTIVE, "default", 1144);
        // Enderpearl
        set(ConfPaths.COMBINED_ENDERPEARL_CHECK, true, 1154); // 'default' activation flag bug
        set(ConfPaths.COMBINED_ENDERPEARL_PREVENTCLICKBLOCK, true, 785);
        // Improbable
        set(ConfPaths.COMBINED_IMPROBABLE_CHECK , "default", 785);
        set(ConfPaths.COMBINED_IMPROBABLE_LEVEL, 250, 1154);
        set(ConfPaths.COMBINED_IMPROBABLE_ACTIONS, "cancel vl>20 log:improbable:8:9:if cancel vl>1500 cancel log:improbable:0:10:if cmdc:kickimprobable:0:5", 1154);
        // Invulnerable
        set(ConfPaths.COMBINED_INVULNERABLE_CHECK, true, 785); // Not a check type yet.
        set(ConfPaths.COMBINED_INVULNERABLE_TRIGGERS_ALWAYS, false, 785);
        set(ConfPaths.COMBINED_INVULNERABLE_TRIGGERS_FALLDISTANCE, true, 785);
        set(ConfPaths.COMBINED_INVULNERABLE_INITIALTICKS_JOIN, -1, 785);
        set(ConfPaths.COMBINED_INVULNERABLE_IGNORE, Arrays.asList("FALL"), 785);
        set(ConfPaths.COMBINED_INVULNERABLE_MODIFIERS + ".all", 0, 785);
        // Munchhausen
        set(ConfPaths.COMBINED_MUNCHHAUSEN_CHECK, "default", 785);
        set(ConfPaths.COMBINED_MUNCHHAUSEN_ACTIONS, "cancel vl>100 cancel log:munchhausen:0:60:if", 785);
        // YawRate settings
        set(ConfPaths.COMBINED_YAWRATE_RATE , 290, 1154); // 360
        set(ConfPaths.COMBINED_YAWRATE_PENALTY_FACTOR, 2.0, 1154); 
        set(ConfPaths.COMBINED_YAWRATE_PENALTY_MIN, 450, 1154); // 250
        set(ConfPaths.COMBINED_YAWRATE_PENALTY_MAX, 2500, 1154); // 1500
        set(ConfPaths.COMBINED_YAWRATE_IMPROBABLE_FEEDONLY, false, 1154);
        set(ConfPaths.COMBINED_YAWRATE_IMPROBABLE_WEIGHT, 90.0, 1154); // 100.0


        /* Fight */
        set(ConfPaths.FIGHT_ACTIVE, "default", 1144);
        // General
        set(ConfPaths.FIGHT_CANCELDEAD, true, 785);
        // TODO: Using System.currentTimeMillis() for timestamp not server tick?
        set(ConfPaths.FIGHT_MAXLOOPLETENCYTICKS, 8, 1154);
        set(ConfPaths.FIGHT_TOOLCHANGEPENALTY, 0L, 1154); // Disabled for now, it interferes too much with pvp. 500L
        set(ConfPaths.FIGHT_PVP_KNOCKBACKVELOCITY, "default", 785);
        // YawRate activation flag
        set(ConfPaths.FIGHT_YAWRATE_CHECK, true, 785); 
        // Angle
        set(ConfPaths.FIGHT_ANGLE_CHECK, "default", 785);
        set(ConfPaths.FIGHT_ANGLE_THRESHOLD_MOVE, 100, 1154);
        set(ConfPaths.FIGHT_ANGLE_THRESHOLD_TIME, 150, 1154); 
        set(ConfPaths.FIGHT_ANGLE_THRESHOLD_YAW, 60, 1154); 
        set(ConfPaths.FIGHT_ANGLE_THRESHOLD_SWITCH, 50, 1153);
        set(ConfPaths.FIGHT_ANGLE_ACTIONS, "cancel vl>100 log:angle:6:5:i cancel vl>800 log:angle:0:20:if cancel", 1154);
        // Critical
        set(ConfPaths.FIGHT_CRITICAL_CHECK, "default", 785);
        set(ConfPaths.FIGHT_CRITICAL_FALLDISTANCE, 0.075, 1154); 
        set(ConfPaths.FIGHT_CRITICAL_FALLDISTLENIENCY, 0.0009, 1154);
        set(ConfPaths.FIGHT_CRITICAL_ACTIONS, "cancel vl>5 cancel log:critical:6:10:i vl>60 cancel log:critical:0:5:icf cmd:clearcritical:0:5 cmdc:kickcritical:0:2", 1154);
        // Direction
        set(ConfPaths.FIGHT_DIRECTION_CHECK, "default", 785);
        set(ConfPaths.FIGHT_DIRECTION_STRICT, true, 1154);
	    set(ConfPaths.FIGHT_DIRECTION_FAILALL, true, 1154);
        set(ConfPaths.FIGHT_DIRECTION_PENALTY, 75L, 1154);
        set(ConfPaths.FIGHT_DIRECTION_ACTIONS, "vl>2 log:fdirectionlowvl:5:6:i vl>10 cancel log:fdirection:2:4:if vl>50 cancel log:fdirection:0:7:icf cmdc:kicksuspiciouscombat:1:5", 1154);
        // FastHeal, legacy
        set(ConfPaths.FIGHT_FASTHEAL_CHECK, "default", 785);
        set(ConfPaths.FIGHT_FASTHEAL_INTERVAL, 4000L, 785);
        set(ConfPaths.FIGHT_FASTHEAL_BUFFER, 1000L, 785);
        set(ConfPaths.FIGHT_FASTHEAL_ACTIONS, "cancel vl>10 cancel log:fastheal:2:6:i vl>30 cancel log:fastheal:1:1:i vl>90 cancel log:fastheal:0:10:if cmdc:kickfastheal:0:10", 1154);
        // GodMode
        set(ConfPaths.FIGHT_GODMODE_CHECK, "default", 785);
        set(ConfPaths.FIGHT_GODMODE_LAGMINAGE, 1100, 785); // TODO: ndt/2 => 500-600.
        set(ConfPaths.FIGHT_GODMODE_LAGMAXAGE, 5000, 785);
        set(ConfPaths.FIGHT_GODMODE_ACTIONS, "cancel vl>10 cancel log:godmode:0:5:icf cmdc:kickgod:0:5", 1154);
        // ImpHit
        set(ConfPaths.FIGHT_IMPOSSIBLEHIT_CHECK, "default", 785);
        set(ConfPaths.FIGHT_IMPOSSIBLEHIT_IMPROBABLE_WEIGHT, 0.05, 1154);
        set(ConfPaths.FIGHT_IMPOSSIBLEHIT_ACTIONS, "cancel vl>3 cancel log:impossiblehit:0:5:if", 1154);
        // NoSwing
        set(ConfPaths.FIGHT_NOSWING_CHECK, "default", 785);
        set(ConfPaths.FIGHT_NOSWING_ACTIONS, "vl>2 cancel vl>10 log:noswing:2:5:i cancel", 1154);
        // Reach
        set(ConfPaths.FIGHT_REACH_CHECK, "default", 785);
        set(ConfPaths.FIGHT_REACH_SURVIVALDISTANCE, 4.02, 1154); 
        set(ConfPaths.FIGHT_REACH_PENALTY, 250, 1154);
        set(ConfPaths.FIGHT_REACH_REDUCE, true, 785);
        set(ConfPaths.FIGHT_REACH_PRECISION, true, 1154);
        set(ConfPaths.FIGHT_REACH_REDUCEDISTANCE, 0.8, 785);
        set(ConfPaths.FIGHT_REACH_REDUCESTEP, 0.14, 785);
        set(ConfPaths.FIGHT_REACH_IMPROBABLE_FEEDONLY, false, 1154);
        set(ConfPaths.FIGHT_REACH_IMPROBABLE_WEIGHT, 2.0, 1154);
        set(ConfPaths.FIGHT_REACH_ACTIONS, "cancel vl>1 cancel log:freach:8:9:i vl>5 cancel log:freach:2:6:i vl>12 cancel log:freachhighvl:1:5:if vl>35 cancel log:freachhighvl:0:5:if cmdc:kicksuspiciouscombat:2:1", 1154);
        // SelfHit, legacy
        set(ConfPaths.FIGHT_SELFHIT_CHECK, "default", 785);
        set(ConfPaths.FIGHT_SELFHIT_ACTIONS, "cancel log:fselfhit:0:5:icf cmdc:kickselfhit:0:5", 1154);
        // Speed
        set(ConfPaths.FIGHT_SPEED_CHECK, "default", 785);
        set(ConfPaths.FIGHT_SPEED_LIMIT, 15, 1154); 
        set(ConfPaths.FIGHT_SPEED_ACTIONS, "cancel vl>10 cancel log:fspeed:2:5:i vl>60 cancel log:fspeed:0:5:if cmdc:kickattackfrequency:0:10", 1154);
        set(ConfPaths.FIGHT_SPEED_SHORTTERM_TICKS, 7, 785);
        set(ConfPaths.FIGHT_SPEED_SHORTTERM_LIMIT, 5, 1154);
        set(ConfPaths.FIGHT_SPEED_IMPROBABLE_FEEDONLY, false, 1154);
        set(ConfPaths.FIGHT_SPEED_IMPROBABLE_WEIGHT, 1.0, 1154);
        // Visible
        set(ConfPaths.FIGHT_VISIBLE_CHECK, "default", 1154);
        set(ConfPaths.FIGHT_VISIBLE_ACTIONS, "cancel vl>30 log:fvisible:8:5:if cancel", 1154);


        /* Inventory */
        set(ConfPaths.INVENTORY_ACTIVE, "default", 1144);
        // FastClick
        set(ConfPaths.INVENTORY_FASTCLICK_CHECK, "default", 785);
        set(ConfPaths.INVENTORY_FASTCLICK_EXCLUDE, Arrays.asList("Inventory Name Here"), 1154);
        set(ConfPaths.INVENTORY_FASTCLICK_SPARECREATIVE, true, 785);
        set(ConfPaths.INVENTORY_FASTCLICK_TWEAKS1_5, true, 785);
        set(ConfPaths.INVENTORY_FASTCLICK_LIMIT_SHORTTERM, 4, 785);
        set(ConfPaths.INVENTORY_FASTCLICK_LIMIT_NORMAL, 15, 785);
        set(ConfPaths.INVENTORY_FASTCLICK_LIMIT_CHEST, 155, 1154);
        set(ConfPaths.INVENTORY_FASTCLICK_IMPROBABLE_WEIGHT, 0.7, 1154);
        set(ConfPaths.INVENTORY_FASTCLICK_ACTIONS, "cancel vl>150 cancel log:fastclick:7:5:i vl>400 cancel log:fastclick:1:5:if vl>3000 cancel log:fastclick:1:2:if cmdc:kickfastclick:2:5", 1154);
        // InstantBow
        set(ConfPaths.INVENTORY_INSTANTBOW_CHECK, "default", 785);
        set(ConfPaths.INVENTORY_INSTANTBOW_STRICT, true, 785);
        set(ConfPaths.INVENTORY_INSTANTBOW_DELAY, 75, 1154);
        set(ConfPaths.INVENTORY_INSTANTBOW_IMPROBABLE_FEEDONLY, false, 1085);
        set(ConfPaths.INVENTORY_INSTANTBOW_IMPROBABLE_WEIGHT, 0.6, 1085);
        set(ConfPaths.INVENTORY_INSTANTBOW_ACTIONS, "cancel vl>15 cancel log:instantbow:4:5:i vl>200 cancel log:instantbow:0:10:if cmdc:kickbow:2:10", 1154);
        // InstantEat, legacy. Disabled since it seems to interfer with FastConsume somehow... 
        set(ConfPaths.INVENTORY_INSTANTEAT_CHECK, false, 1154); 
        set(ConfPaths.INVENTORY_INSTANTEAT_ACTIONS, "log:instanteat:2:5:i cancel", 1154);
        // FastConsume
        set(ConfPaths.INVENTORY_FASTCONSUME_CHECK, "default", 785);
        set(ConfPaths.INVENTORY_FASTCONSUME_DURATION, 1.4, 1154); // The actual consumption time needed would be 1.5s. Set to 1.4 for leniency
        set(ConfPaths.INVENTORY_FASTCONSUME_WHITELIST, false, 785);
        set(ConfPaths.INVENTORY_FASTCONSUME_ITEMS, new LinkedList<String>(), 1154);
        set(ConfPaths.INVENTORY_FASTCONSUME_ACTIONS, "cancel vl>2 log:fastconsume:2:5:if cancel vl>35 cancel log:fastconsume:0:5:if cmdc:kickfastconsume:0:5", 1154);
        // InvMove
        set(ConfPaths.INVENTORY_INVENTORYMOVE_CHECK, "default", 1153);
        set(ConfPaths.INVENTORY_INVENTORYMOVE_DISABLECREATIVE, true, 1153); 
        set(ConfPaths.INVENTORY_INVENTORYMOVE_HDISTDIVISOR, 4, 1154);
        set(ConfPaths.INVENTORY_INVENTORYMOVE_IMPROBABLE_FEEDONLY, false, 1154);
        set(ConfPaths.INVENTORY_INVENTORYMOVE_IMPROBABLE_WEIGHT, 2.0, 1154);
        set(ConfPaths.INVENTORY_INVENTORYMOVE_ACTIONS, "cancel vl>10 log:inventorymove:7:8:i cancel", 1154);
        // Gutenberg
        set(ConfPaths.INVENTORY_GUTENBERG_CHECK, "default", 785);
        set(ConfPaths.INVENTORY_GUTENBERG_PAGELIMIT, ServerVersion.compareMinecraftVersion("1.14") >= 0 ? 100 : 50, 1154);
        set(ConfPaths.INVENTORY_GUTENBERG_ACTIONS, "cancel log:gutenberg:0:10:if cmdc:kickinvaliddata:0:5", 1154);
        // Items
        set(ConfPaths.INVENTORY_ITEMS_CHECK, "default", 785);
        // Open
        set(ConfPaths.INVENTORY_OPEN_CHECK, "default", 785);
        set(ConfPaths.INVENTORY_OPEN_CLOSE, true, 785);
        set(ConfPaths.INVENTORY_OPEN_CANCELOTHER, true, 785);
        // Hotfix 1.9+
        set(ConfPaths.INVENTORY_HOTFIX_DUPE_FALLINGBLOCKENDPORTAL, true, 785);


        /* Moving */
        set(ConfPaths.MOVING_ACTIVE, "default", 1144);
        // CreativeFly
        set(ConfPaths.MOVING_CREATIVEFLY_CHECK, "default", 785);
        set(ConfPaths.MOVING_CREATIVEFLY_IGNOREALLOWFLIGHT, true, 785);
        set(ConfPaths.MOVING_CREATIVEFLY_IGNORECREATIVE, false, 785);
        /* 
         * Instruction configuring models:
         * HORIZONTAL_SPEED : Maximum horizontal distance allowed in move with formula:
         *    number_in_config / 100 * 0.6
         * SUB_VERTICAL_ASCEND_SPEED : Maximum vertical distance allowed ascending in move with formula:
         *    number_in_config / 100
         *    if the number is 0 then this model has it own ascending rules, set to other will lead to bypass.
         * SUB_VERTICAL_MAXHEIGHT : Maximum standing height allow , move higher, setback:
         *    number_in_config + 255(max world height)
         * SUB_GROUND : Ground checking, should collect block flags.   
         * SUB_MODIFIERS : Should increase max speed by potions or attribute.
         * SUB_VERTICAL_GRAVITY : Should apply normal falling friction(more likely for compatibility).
         * */
        set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "creative." + ConfPaths.SUB_HORIZONTAL_SPEED, 100, 785);
        set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "creative." + ConfPaths.SUB_VERTICAL_ASCEND_SPEED, 100, 785);
        set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "creative." + ConfPaths.SUB_VERTICAL_MAXHEIGHT, 128, 785);
        if (BridgeMisc.GAME_MODE_SPECTATOR != null) {
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "spectator." + ConfPaths.SUB_HORIZONTAL_SPEED, 450, 1102);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "spectator." + ConfPaths.SUB_VERTICAL_ASCEND_SPEED, 170, 1103);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "spectator." + ConfPaths.SUB_VERTICAL_MAXHEIGHT, cfMaxWorldY, 785);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "spectator." + ConfPaths.SUB_GROUND, false, 785);
        }
        if (Bridge1_9.hasLevitation()) {
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "levitation." + ConfPaths.SUB_HORIZONTAL_SPEED, 50, 785);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "levitation." + ConfPaths.SUB_VERTICAL_ASCEND_SPEED, 10, 785);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "levitation." + ConfPaths.SUB_VERTICAL_MAXHEIGHT, cfMaxWorldY, 1104);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "levitation." + ConfPaths.SUB_VERTICAL_GRAVITY, false, 785);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "levitation." + ConfPaths.SUB_MODIFIERS, false, 785);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "levitation." + ConfPaths.SUB_GROUND, true, 1154);
        }
        if (Bridge1_13.hasSlowfalling()) {
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "slowfalling." + ConfPaths.SUB_HORIZONTAL_SPEED, 65, 1154);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "slowfalling." + ConfPaths.SUB_VERTICAL_ASCEND_SPEED, 0, 1154);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "slowfalling." + ConfPaths.SUB_VERTICAL_MAXHEIGHT, cfMaxWorldY, 1104);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "slowfalling." + ConfPaths.SUB_VERTICAL_GRAVITY, false, 785);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "slowfalling." + ConfPaths.SUB_MODIFIERS, true, 1154);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "slowfalling." + ConfPaths.SUB_GROUND, true, 785);
        }
        if (Bridge1_13.hasIsRiptiding()) {
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "riptiding." + ConfPaths.SUB_HORIZONTAL_SPEED, 700, 1154);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "riptiding." + ConfPaths.SUB_VERTICAL_ASCEND_SPEED, 600, 1154);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "riptiding." + ConfPaths.SUB_VERTICAL_MAXHEIGHT, cfMaxWorldY, 1154);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "riptiding." + ConfPaths.SUB_MODIFIERS, false, 1154);
        }
        if (Bridge1_9.hasElytra()) {
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "elytra." + ConfPaths.SUB_HORIZONTAL_SPEED, 666, 1154);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "elytra." + ConfPaths.SUB_HORIZONTAL_MODSPRINT, 1.0, 1154);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "elytra." + ConfPaths.SUB_VERTICAL_ASCEND_SPEED, 0, 785);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "elytra." + ConfPaths.SUB_VERTICAL_MAXHEIGHT, cfMaxWorldY, 1104);
            set(ConfPaths.MOVING_CREATIVEFLY_MODEL + "elytra." + ConfPaths.SUB_MODIFIERS, false, 785);
			set(ConfPaths.MOVING_CREATIVEFLY_EYTRA_FWRESET, false, 1154);
            set(ConfPaths.MOVING_CREATIVEFLY_EYTRA_STRICT, true, 1154);
        }
        set(ConfPaths.MOVING_CREATIVEFLY_ACTIONS,
                "cancel log:flyfile:3:5:f"
                        + " vl>100 cancel log:creativefly:8:9:i log:flyfile:0:10:f"
                        + " vl>900 cancel log:creativefly:2:4:i log:flyfile:0:5:cf"
                        + " vl>2000 cancel log:creativefly:0:5:icf cmdc:kickfly:0:15"
                        , 1154);
        // MorePackets
        set(ConfPaths.MOVING_MOREPACKETS_CHECK, "default", 785);
        set(ConfPaths.MOVING_MOREPACKETS_SECONDS, 6, 785);
        set(ConfPaths.MOVING_MOREPACKETS_EPSIDEAL, 20, 785); // Ordinary packet frequency
        set(ConfPaths.MOVING_MOREPACKETS_EPSMAX, 22, 785); // Tolereted packets
        set(ConfPaths.MOVING_MOREPACKETS_BURST_PACKETS, 15, 785); // Amount of packets within half a second to generate a single burst event. (15= 1).
        set(ConfPaths.MOVING_MOREPACKETS_BURST_DIRECT, 10, 785); // Maximum burst events within 500ms (Max packets allowed: 15*10=150 within 500ms)
        set(ConfPaths.MOVING_MOREPACKETS_BURST_EPM, 95, 1154); // Maximum burst events per minute
        set(ConfPaths.MOVING_MOREPACKETS_SETBACKAGE, 110, 1154); // Quite harsh. 
        set(ConfPaths.MOVING_MOREPACKETS_ACTIONS, "cancel vl>2 cancel log:morepackets:10:9:i vl>100 cancel log:morepackets:0:2:ifc cmdc:kickpackets:0:10", 1154);
        // NoFall
        set(ConfPaths.MOVING_NOFALL_CHECK, "default", 785);
        set(ConfPaths.MOVING_NOFALL_DEALDAMAGE, true, 785);
        set(ConfPaths.MOVING_NOFALL_SKIPALLOWFLIGHT, true, 785);
        set(ConfPaths.MOVING_NOFALL_RESETONVL, false, 785);
        set(ConfPaths.MOVING_NOFALL_RESETONTP, false, 785);
        set(ConfPaths.MOVING_NOFALL_RESETONVEHICLE, true, 785);
        set(ConfPaths.MOVING_NOFALL_ANTICRITICALS, true, 785);
        set(ConfPaths.MOVING_NOFALL_ACTIONS, "cancel vl>2 cancel log:nofall:0:5:if", 1154); //vl>6 cancel log:nofall:0:1:if cmdc:kickfly:0:5", 1154);
        // Passable
        set(ConfPaths.MOVING_PASSABLE_CHECK, "default", 785);
        set(ConfPaths.MOVING_PASSABLE_ACTIONS, "cancel vl>15 cancel log:passable:7:9:i vl>100 cancel log:passable:1:4:if", 1154);
        set(ConfPaths.MOVING_PASSABLE_UNTRACKED_TELEPORT_ACTIVE, true, 785);
        set(ConfPaths.MOVING_PASSABLE_UNTRACKED_CMD_ACTIVE, true, 785);
        set(ConfPaths.MOVING_PASSABLE_UNTRACKED_CMD_TRYTELEPORT, true, 785);
        set(ConfPaths.MOVING_PASSABLE_UNTRACKED_CMD_PREFIXES, Arrays.asList("sethome", "home set", "setwarp", "warp set", "setback", "set back", "back set", "warp", "home", "tp", "tpa", "tpask", "tpyes", "tphere"), 1154);
        // SurvivalFly
        set(ConfPaths.MOVING_SURVIVALFLY_CHECK, "default", 785);
        set(ConfPaths.MOVING_SURVIVALFLY_STEPHEIGHT, "default", 785);
        set(ConfPaths.MOVING_SURVIVALFLY_EXTENDED_VACC, true, 785);
        set(ConfPaths.MOVING_SURVIVALFLY_EXTENDED_HACC, true, 1154);
        set(ConfPaths.MOVING_SURVIVALFLY_EXTENDED_STEP, true, 1154);
        set(ConfPaths.MOVING_SURVIVALFLY_EXTENDED_NOSLOW, true, 1154);
        set(ConfPaths.MOVING_SURVIVALFLY_EXTENDED_RESETITEM, true, 1154);
        set(ConfPaths.MOVING_SURVIVALFLY_LENIENCY_HBUFMAX, 1.0, 1154);
        // SurvivalFly - ViolationFrequencyHook
        set(ConfPaths.MOVING_SURVIVALFLY_VLFREQUENCY_ACTIVE, true, 1154);
        set(ConfPaths.MOVING_SURVIVALFLY_VLFREQUENCY_DEBUG, false, 1154);
        set(ConfPaths.MOVING_SURVIVALFLY_VLFREQUENCY_MAXTOTALVLS, 35, 1154);
        set(ConfPaths.MOVING_SURVIVALFLY_VLFREQUENCY_MINADDEDVLS, 5, 1154);
        set(ConfPaths.MOVING_SURVIVALFLY_VLFREQUENCY_MOREVLS, 5, 1154);
        set(ConfPaths.MOVING_SURVIVALFLY_VLFREQUENCY_MOVECOUNT, 10, 1154);
        // More leniency features
        set(ConfPaths.MOVING_SURVIVALFLY_LENIENCY_FREEZECOUNT, 40, 1144);
        set(ConfPaths.MOVING_SURVIVALFLY_LENIENCY_FREEZEINAIR, true, 1143);
        set(ConfPaths.MOVING_SURVIVALFLY_SETBACKPOLICY_FALLDAMAGE, true, 785);
        set(ConfPaths.MOVING_SURVIVALFLY_SETBACKPOLICY_VOIDTOVOID, false, 1154);
        set(ConfPaths.MOVING_SURVIVALFLY_ACTIONS, "cancel log:flyfile:6:15:f" 
            + " vl>100 cancel log:survivalfly:10:11:i log:flyfile:6:15:f" 
            + " vl>700 cancel log:survivalfly:8:5:i log:flyfile:1:3:f" 
            + " vl>2100 cancel log:survivalflyhighvl:0:4:icf cmdc:kickfly:0:15", 1154);     
        // SurvivalFly - Hover Subcheck
        set(ConfPaths.MOVING_SURVIVALFLY_HOVER_CHECK, true, 785); // Not a check type yet.
        set(ConfPaths.MOVING_SURVIVALFLY_HOVER_STEP, 5, 785);
        set(ConfPaths.MOVING_SURVIVALFLY_HOVER_TICKS, 85, 785);
        set(ConfPaths.MOVING_SURVIVALFLY_HOVER_LOGINTICKS, 60, 785);
        set(ConfPaths.MOVING_SURVIVALFLY_HOVER_FALLDAMAGE, true, 785);
        set(ConfPaths.MOVING_SURVIVALFLY_HOVER_SFVIOLATION, 200, 1154); 
        // Moving Trace - Lag compensator
        set(ConfPaths.MOVING_TRACE_MAXAGE, 30, 1154);
        set(ConfPaths.MOVING_TRACE_MAXSIZE, 30, 1154);
        // Velocity.
        set(ConfPaths.MOVING_VELOCITY_ACTIVATIONCOUNTER, 20, 1154);
        set(ConfPaths.MOVING_VELOCITY_ACTIVATIONTICKS, 140, 785);
        set(ConfPaths.MOVING_VELOCITY_STRICTINVALIDATION, true, 785);
        // General.
        set(ConfPaths.MOVING_SPLITMOVES, "default", 785);
        set(ConfPaths.MOVING_IGNORESTANCE, "default", 785);
        set(ConfPaths.MOVING_TEMPKICKILLEGAL, true, 785);
        set(ConfPaths.MOVING_LOADCHUNKS_JOIN, true, 785);
        set(ConfPaths.MOVING_LOADCHUNKS_MOVE, false, 1154);
        set(ConfPaths.MOVING_LOADCHUNKS_TELEPORT, true, 785);
        set(ConfPaths.MOVING_LOADCHUNKS_WORLDCHANGE, true, 785);
        set(ConfPaths.MOVING_SPRINTINGGRACE, 4.0, 785);
        set(ConfPaths.MOVING_ASSUMESPRINT, true, 785);
        set(ConfPaths.MOVING_SPEEDGRACE, 4.0, 785);
        set(ConfPaths.MOVING_ENFORCELOCATION, "default", 785);
        set(ConfPaths.MOVING_SETBACK_METHOD, "default", 785);


        /* Vehicles settings */
        set(ConfPaths.MOVING_VEHICLE_PREVENTDESTROYOWN, true, 785);
        set(ConfPaths.MOVING_VEHICLE_ENFORCELOCATION, "default", 785);
        set(ConfPaths.MOVING_VEHICLE_SCHEDULESETBACKS, "default", 785);
        set(ConfPaths.MOVING_VEHICLE_DELAYADDPASSENGER, "default", 1154);
        // VehicleMorePackets
        set(ConfPaths.MOVING_VEHICLE_MOREPACKETS_CHECK, "default", 785);
        set(ConfPaths.MOVING_VEHICLE_MOREPACKETS_ACTIONS, "cancel vl>2 log:morepackets:2:5:if cancel vl>100 cancel log:morepackets:1:2:if cmdc:kickpackets:2:10", 1154);
        // VehicleEnvelope check
        set(ConfPaths.MOVING_VEHICLE_ENVELOPE_ACTIVE, "default", 785);
        set(ConfPaths.MOVING_VEHICLE_ENVELOPE_HSPEEDCAP + ".default", 0.9, 1154);
        set(ConfPaths.MOVING_VEHICLE_ENVELOPE_HSPEEDCAP + ".pig", 0.3, 1154);
        set(ConfPaths.MOVING_VEHICLE_ENVELOPE_ACTIONS, "cancel vl>50 cancel log:vehicleenvelope:10:6:if vl>300 cancel log:vehicleenvelope:0:10:if cmdc:kickvehiclefly:0:10", 1154);
        // Messages
        set(ConfPaths.MOVING_MESSAGE_ILLEGALPLAYERMOVE, "Illegal move.", 785);
        set(ConfPaths.MOVING_MESSAGE_ILLEGALVEHICLEMOVE, "Illegal vehicle move.", 785);


        /* Net */
        set(ConfPaths.NET_ACTIVE, "default", 1144);
        // AttackFrequency
        set(ConfPaths.NET_ATTACKFREQUENCY_ACTIVE, "default", 785);
        set(ConfPaths.NET_ATTACKFREQUENCY_SECONDS_HALF, 8, 1154); // 10
        set(ConfPaths.NET_ATTACKFREQUENCY_SECONDS_ONE, 15, 785); 
        set(ConfPaths.NET_ATTACKFREQUENCY_SECONDS_TWO, 30, 785);
        set(ConfPaths.NET_ATTACKFREQUENCY_SECONDS_FOUR, 60, 785);
        set(ConfPaths.NET_ATTACKFREQUENCY_SECONDS_EIGHT, 95, 1154); //100
        set(ConfPaths.NET_ATTACKFREQUENCY_IMPROBABLE_WEIGHT, 3.0, 1154);
        set(ConfPaths.NET_ATTACKFREQUENCY_ACTIONS, "cancel vl>10 cancel log:attackfrequency:4:5:i vl>200 cancel log:attackfrequency:0:4:if cmdc:kickattackfrequency", 1154);
        // FlyingFrequency
        set(ConfPaths.NET_FLYINGFREQUENCY_ACTIVE, "default", 785);
        set(ConfPaths.NET_FLYINGFREQUENCY_SECONDS, 5, 785);
        set(ConfPaths.NET_FLYINGFREQUENCY_PACKETSPERSECOND, 60, 785);
        set(ConfPaths.NET_FLYINGFREQUENCY_ACTIONS, "cancel vl>20 cancel log:flyingfrequency:3:2:i vl>500 cancel log:flyingfrequency:0:5:if vl>1000 cancel log:flyingfrequency:0:2:icf cmdc:kickflyingfrequency:0:10", 1154); 
        set(ConfPaths.NET_FLYINGFREQUENCY_REDUNDANT_ACTIVE, true, 785);
        set(ConfPaths.NET_FLYINGFREQUENCY_REDUNDANT_SECONDS, 3, 785);
        set(ConfPaths.NET_FLYINGFREQUENCY_REDUNDANT_ACTIONS, "cancel", 785); 
        // KeepAliveFrequency
        set(ConfPaths.NET_KEEPALIVEFREQUENCY_ACTIVE, "default", 785);
        set(ConfPaths.NET_KEEPALIVEFREQUENCY_SECONDS, 20, 1153);
        set(ConfPaths.NET_KEEPALIVEFREQUENCY_ACTIONS, "cancel vl>10 cancel log:keepalive:2:6:i vl>60 cancel log:keepalive:0:10:icf cmdc:kickalive:0:5", 1154);
        // Moving
        set(ConfPaths.NET_MOVING_ACTIVE, "default", 1154);
        set(ConfPaths.NET_MOVING_ACTIONS, "cancel vl>17 cancel log:moving:10:15:i", 1154);
        // PacketFrequency (pre 1.9).
        set(ConfPaths.NET_PACKETFREQUENCY_ACTIVE, "default", 785);
        set(ConfPaths.NET_PACKETFREQUENCY_PPS, 300, 1154); // Old limit: 200, legit 1.8 clients seem to be able to reach such value more often than not...
        set(ConfPaths.NET_PACKETFREQUENCY_SECONDS, 4, 785);
        set(ConfPaths.NET_PACKETFREQUENCY_ACTIONS, "cancel vl>2 cancel cmdc:kickpacketfrequency:0:5", 1154);
        // SoundDistance
        set(ConfPaths.NET_SOUNDDISTANCE_ACTIVE, "default", 785);
        set(ConfPaths.NET_SOUNDDISTANCE_MAXDISTANCE, 320, 785);
        // Superseded
        set(ConfPaths.NET_SUPERSEDED_FLYING_CANCELWAITING, true, 1090);
        // WrongTurn
        set(ConfPaths.NET_WRONGTURN_ACTIVE, "default", 1154);
        set(ConfPaths.NET_WRONGTURN_ACTIONS, "cancel log:wrongturn:0:15:icf cmdc:kickillegalrotations:0:10", 1154);


        // TODO: An extra file might suit these.
        final String start = "&c[player]&7 failed &c[check]&7: ";
        final String end = " &7(&cVL[violations]&7)";
        final String tell = "ncp tell [player] &c&l(!)&7 ";
        final String clear = "ncp removeplayer [player] ";
        final String kick = "ncp kick [player] &c&l(!)&7 ";
        set(ConfPaths.STRINGS + ".against", start + "tried to place a block against liquid blocks or air as if they were solid" + end, 1154);
        set(ConfPaths.STRINGS + ".angle", start + "potentially tried to hit multiple entities at the same time (Tags: &6[tags]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".attackfrequency", start + "seems to be using an autoclicker (Clicks: &6[packets]&7/LimitPerTime-frame: &6[limit]&7, &6[tags]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".bautosign", start + "tried to use the autosign hack (Subcheck: &6[tags]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".bbfrequency", start + "tried to break too many blocks at once" + end, 1154);
        set(ConfPaths.STRINGS + ".bdirection", start + "tried to interact with a block outside their line of sight" + end, 1154);
        set(ConfPaths.STRINGS + ".bpspeed", start + "tried to throw projectiles too quickly" + end, 1154);
        set(ConfPaths.STRINGS + ".breach", start + "tried to interact with a block too far away" + end, 1154);
        set(ConfPaths.STRINGS + ".bspeed", start + "tried to interact with blocks beyond legit speeds" + end, 1154);
        set(ConfPaths.STRINGS + ".bvisible", start + "tried to interact with a block through a solid obstacle" + end, 1154);
        set(ConfPaths.STRINGS + ".bwrong", start + "tried to break another block than interacted with last" + end, 1154);
        set(ConfPaths.STRINGS + ".captcha", "[player] failed captcha repeatedly" + end, 785);
        set(ConfPaths.STRINGS + ".chatfast", start + "acted like spamming (message deleted) (IP: &6[ip]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".chatnormal", start + "seems to be spamming the server chat" + end, 1154);
        set(ConfPaths.STRINGS + ".commands", start + "issued too many commands too quickly" + end, 785);
        set(ConfPaths.STRINGS + ".creativefly", start + "tried to perform a move that deviates from the model (Model: &6[model]&7, Tags: &6[tags]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".critical", start + "tried to perform an illegal critical hit (Tags: &6[tags]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".fastbreak", start + "tried to break a block faster than possible (Block: &6[blocktype]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".fastclick", start + "tried to move items in their inventory too quickly (Tags: &6[tags]&7)" + end, 785);
        set(ConfPaths.STRINGS + ".fastconsume", start + "tried to consume an item too quickly (Food: &6[food][tags]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".fastheal", start + "tried to regenerate their health bar faster than possible (Health: &6[health]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".fastplace", start + "tried to place blocks too quickly" + end, 1154);
        set(ConfPaths.STRINGS + ".fdirection", start + "tried to hit an entity outside their line of sight" + end, 1154);
        set(ConfPaths.STRINGS + ".fdirectionlowvl", start + "could be using an aimbot (hit not canceled)" + end, 1154);
        set(ConfPaths.STRINGS + ".flyfile", start + "tried to perform an illegal move from ([locationfrom]) to ([locationto]) over a distance of ([distance]) blocks, subchecks triggered> ([tags])" + end, 1154);
        set(ConfPaths.STRINGS + ".flyingfrequency", start + "is manipulating packets sent to the server to gain an unfair advantage" + end, 1154);
        set(ConfPaths.STRINGS + ".freach", start + "tried to hit an entity from a suspicious distance (Reach: &6[reachdistance]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".fselfhit", start + "tried to hit themselves" + end, 1154);
        set(ConfPaths.STRINGS + ".fspeed", start + "tried to click [violations] times over the established [limit] CPS limit" + end, 1154);
        set(ConfPaths.STRINGS + ".fvisible", start + "tried to attack target through a solid obstacle" + end, 1154);
        set(ConfPaths.STRINGS + ".godmode", start + "tried to ignore inflicted damage (Health: &7[health]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".gutenberg", start + "created a book with too many pages" + end, 785);
        set(ConfPaths.STRINGS + ".impossiblehit", start + "tried to hit an entity while performing another action (Tags: &6[tags]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".improbable", start + "is exhibiting an erratic behaviour (Check: &6[tags]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".instantbow", start + "tried to shoot an arrow too fast" + end, 1154);
        set(ConfPaths.STRINGS + ".instanteat", start + "tried to consume an item too quickly (Food: &6[food]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".inventorymove", start + "tried to click in their inventory while performing another action (Tags: &6[tags]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".keepalive", start + "is spamming keep-alive packets to the server" + end, 1154);
        set(ConfPaths.STRINGS + ".kickagainst", kick + "Invalid block placements.", 1154);
        set(ConfPaths.STRINGS + ".kickalive", kick + "Too many keep-alive packets.", 1154);
        set(ConfPaths.STRINGS + ".kickattackfrequency", kick + "Unlikely fast clicking.", 1154);
        set(ConfPaths.STRINGS + ".kickbow", kick + "Shooting arrows too quickly.", 1154);
        set(ConfPaths.STRINGS + ".kickbspeed", kick + "Too fast block interactions.", 1154);
        set(ConfPaths.STRINGS + ".kickcaptcha", kick + "Enter the captcha!", 1154);
        set(ConfPaths.STRINGS + ".kickchat1", "ncp tempkick [player] 1 &c&l(!)&7 Do not spam the server chat (1 minute tempkick)", 1154);
        set(ConfPaths.STRINGS + ".kickchat5", "ncp tempkick [player] 5 &c&l(!)&7 You are not allowed to spam the server chat (5 minutes tempkick)", 1154);
        set(ConfPaths.STRINGS + ".kickchatfast", kick + "Stop spamming.", 1154);
        set(ConfPaths.STRINGS + ".kickchatnormal", kick + "Too many chat messages, take a break.", 1154);
        set(ConfPaths.STRINGS + ".kickcommands", "ncp tempkick [player] 1 &c&l(!)&7 Do not spam commands (1 minute tempkick)", 1154);
        set(ConfPaths.STRINGS + ".kickcritical", kick + "Incorrect movements (Critical cheats?)", 1154);
        set(ConfPaths.STRINGS + ".kickfastbreak", kick + "Breaking blocks too fast.", 1154);
        set(ConfPaths.STRINGS + ".kickfastclick", kick + "Unlikely inventory interactions.", 1154);
        set(ConfPaths.STRINGS + ".kickfastconsume", kick + "Using items too quickly.", 1154);
        set(ConfPaths.STRINGS + ".kickfastheal", kick + "Too fast health regeneration.", 1154);
        set(ConfPaths.STRINGS + ".kickfastplace", kick + "Placing blocks too quickly.", 1154);
        set(ConfPaths.STRINGS + ".kickfly", kick + "Moved unexpectedly.", 1154);
        set(ConfPaths.STRINGS + ".kickflyingfrequency", kick + "Kicked for packet spam.", 1154);
        set(ConfPaths.STRINGS + ".kickfrequency", kick + "Illegal block-breaking frequency.", 1154);
        set(ConfPaths.STRINGS + ".kickgod", kick + "GodMode?", 1154);
        set(ConfPaths.STRINGS + ".kickillegalblockinteract", kick + "Illegal block interactions.", 1154);
        set(ConfPaths.STRINGS + ".kickillegalrotations", kick + "Invalid rotations.", 1154);
        set(ConfPaths.STRINGS + ".kickimprobable", kick + "Illegal client modifications.", 1154);
        set(ConfPaths.STRINGS + ".kickinvaliddata", kick + "Invalid book data.", 1154);
        set(ConfPaths.STRINGS + ".kickpacketfrequency", kick + "Too many packets.", 1154);
        set(ConfPaths.STRINGS + ".kickpackets", kick + "You sent too many moves (extreme lag?)", 1154);
        set(ConfPaths.STRINGS + ".kickscaffold", kick + "Unnatural block placements.", 1154);
        set(ConfPaths.STRINGS + ".kickselfhit", kick + "You tried to hit yourself!", 1154);
        set(ConfPaths.STRINGS + ".kicksuspiciouscombat", kick + "Illegal combat modifications.", 1154); 
        set(ConfPaths.STRINGS + ".kickvehiclefly", kick + "Unexpected vehicle movement.", 1154);
        set(ConfPaths.STRINGS + ".kickwb", kick + "Block breaking out of sync.", 1154);
        set(ConfPaths.STRINGS + ".freachhighvl", start + "is most likely using a killaura cheat (Reach: &6[reachdistance]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".morepackets", start + "sent too many moves (&cPackets&7/&cVL [packets]&7)", 1154);
        set(ConfPaths.STRINGS + ".moving", start + "tried to send improbable position updates" + end, 1154);
        set(ConfPaths.STRINGS + ".msgtempdenylogin", "You are temporarily denied to join this server.", 785);
        set(ConfPaths.STRINGS + ".munchhausen", start + "tried to hit themeselves with a fishing rod too many times (the knockback can be potentially abused for bypasses)" + end, 1154);
        set(ConfPaths.STRINGS + ".nofall", start + "tried to alter fall damage or fall distance (Tags: &6[tags]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".noswing", start + "didn't swing arm before performing their action" + end, 1154);
        set(ConfPaths.STRINGS + ".passable", start + "tried to move into a block (bugged player of phase cheat)" + end, 1154);
        set(ConfPaths.STRINGS + ".relog", start + "tried to relog too fast" + end, 1154);
        set(ConfPaths.STRINGS + ".scaffold", start + "tried to place a block in an unnatural way (Tags: &6[tags]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".survivalfly", start + "tried to move unexpectedly" + end, 1154);
        set(ConfPaths.STRINGS + ".survivalflyhighvl", start + "tried to perform an illegal move (Subchecks: &6[tags]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".tellchatnormal", tell + "Too many messages, slow down...", 1154);
        set(ConfPaths.STRINGS + ".tempkick1", "ncp tempkick [player] 1 &c&l(!)&7 You have to wait 1 minute before joining this server again.", 1154);
        set(ConfPaths.STRINGS + ".tempkick5", "ncp tempkick [player] 5 &c&l(!)&7 You have to wait 5 minutes before joining this server again.", 1154);
        set(ConfPaths.STRINGS + ".vehicleenvelope", start + "tried to move a vehicle unexpectedly (Tags/Vehicle: &6[tags]&7)" + end, 1154);
        set(ConfPaths.STRINGS + ".wrongturn", start + "tried to send an impossible pitch rotation (&6>90 &7or&6 <-90&7)" + end, 1154);
        // Clear a check data  
        set(ConfPaths.STRINGS + ".clearcritical", clear + "FIGHT_CRITICAL", 1154);
        set(ConfPaths.STRINGS + ".clearscaffold", clear + "BLOCKPLACE_SCAFFOLD", 1154);


        // Compatibility settings.
        set(ConfPaths.COMPATIBILITY_EXEMPTIONS_WILDCARD_DEFAULT_METADATA_ACTIVE, true, 785);
        set(ConfPaths.COMPATIBILITY_EXEMPTIONS_WILDCARD_DEFAULT_METADATA_KEYS, Arrays.asList("nocheat.exempt"), 785);
        set(ConfPaths.COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_ACTIVE, true, 785);
        set(ConfPaths.COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_BUKKITINTERFACE, true, 785);
        set(ConfPaths.COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_METADATA_ACTIVE, true, 785);
        set(ConfPaths.COMPATIBILITY_EXEMPTIONS_WILDCARD_NPC_METADATA_KEYS, Arrays.asList("NPC"), 785);
        set(ConfPaths.COMPATIBILITY_EXEMPTIONS_REMOVE_JOIN, true, 785);
        set(ConfPaths.COMPATIBILITY_EXEMPTIONS_REMOVE_LEAVE, true, 785);
        set(ConfPaths.COMPATIBILITY_SERVER_CBDEDICATED_ENABLE, true, 785);
        set(ConfPaths.COMPATIBILITY_SERVER_CBREFLECT_ENABLE, true, 785);
        set(ConfPaths.COMPATIBILITY_BLOCKS + ConfPaths.SUB_BREAKINGTIME + ".IRON_BLOCK:PICKAXE:DIAMOND:12", 1139);
        set(ConfPaths.COMPATIBILITY_BLOCKS + ConfPaths.SUB_ALLOWINSTANTBREAK, new LinkedList<String>(), 785);
        set(ConfPaths.COMPATIBILITY_BLOCKS + ConfPaths.SUB_OVERRIDEFLAGS + "." + Material.SNOW.name().toLowerCase(), "default", 785);
        // Make blocks ign_passable+ground_height.
        for (final String name : Arrays.asList(
                // TODO: 
                "moving_piston"
                )) {
            set(ConfPaths.COMPATIBILITY_BLOCKS + ConfPaths.SUB_OVERRIDEFLAGS + "." + name, "default+ign_passable+ground_height", 785);
        }
        set(ConfPaths.COMPATIBILITY_BLOCKS + ConfPaths.SUB_BLOCKCACHE_WORLD_MINY, ServerVersion.compareMinecraftVersion("1.18") >= 0 ? -64 : 0,  1154);
        set(ConfPaths.COMPATIBILITY_BLOCKS_CHANGETRACKER_ACTIVE, true, 1036); // With lastChangedBuildNumber.
        set(ConfPaths.COMPATIBILITY_BLOCKS_CHANGETRACKER_PISTONS, true, 785);
        set(ConfPaths.COMPATIBILITY_BLOCKS_CHANGETRACKER_MAXAGETICKS, 20, 1154);
        set(ConfPaths.COMPATIBILITY_BLOCKS_CHANGETRACKER_PERWORLD_MAXENTRIES, 1000, 785);

        //        // Update internal factory based on all the new entries to the "actions" section.
        //        setActionFactory();
    }
}
