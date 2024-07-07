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
package fr.neatmonster.nocheatplus.checks.chat;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.CheckListener;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.command.CommandUtil;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.SchedulerHelper;
import fr.neatmonster.nocheatplus.components.NoCheatPlusAPI;
import fr.neatmonster.nocheatplus.components.data.ICheckData;
import fr.neatmonster.nocheatplus.components.data.IData;
import fr.neatmonster.nocheatplus.components.registry.factory.IFactoryOne;
import fr.neatmonster.nocheatplus.components.registry.feature.INotifyReload;
import fr.neatmonster.nocheatplus.components.registry.feature.JoinLeaveListener;
import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigFile;
import fr.neatmonster.nocheatplus.config.ConfigManager;
import fr.neatmonster.nocheatplus.logging.Streams;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.players.PlayerFactoryArgument;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.ds.prefixtree.SimpleCharPrefixTree;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;
import fr.neatmonster.nocheatplus.worlds.WorldFactoryArgument;

/**
 * Central location to listen to events that are relevant for the chat checks.
 * 
 * @see ChatEvent
 */
public class ChatListener extends CheckListener implements INotifyReload, JoinLeaveListener {

    // Checks.
    /** Captcha handler. */
    private final Captcha captcha  = addCheck(new Captcha());  

    /** Commands repetition check. */
    private final Commands commands = addCheck(new Commands()); 

    /** Logins check (global,how many simualtenous logins can the server accept in a specified time-frame) */
    private final Logins logins = addCheck(new Logins());

    /** Chat message check. */
    private final Text text = addCheck(new Text());

    /** Relogging check (per-player re-log time. */
    private final Relog relog  = addCheck(new Relog());

    // Auxiliary stuff.
    /** Commands to be ignored completely. */
    private final SimpleCharPrefixTree commandExclusions = new SimpleCharPrefixTree();

    /** Commands to be handled as chat (thus handled by Text and not Command check). */
    private final SimpleCharPrefixTree chatCommands = new SimpleCharPrefixTree();

    /** Commands not to be executed in-game.  */
    private final SimpleCharPrefixTree consoleOnlyCommands = new SimpleCharPrefixTree(); 

    /** Set world to null after use, primary thread only. */
    private final Location useLoc = new Location(null, 0, 0, 0);

    @SuppressWarnings("unchecked")
    public ChatListener() {
        super(CheckType.CHAT);
        ConfigFile config = ConfigManager.getConfigFile();
        initFilters(config);
        // (text inits in constructor.)
        final NoCheatPlusAPI api = NCPAPIProvider.getNoCheatPlusAPI();
        api.register(api.newRegistrationContext()
                .registerConfigWorld(ChatConfig.class)
                .factory(new IFactoryOne<WorldFactoryArgument, ChatConfig>() {
                    @Override
                    public ChatConfig getNewInstance(WorldFactoryArgument arg) {
                        return new ChatConfig(arg.worldData);
                    }
                })
                .registerConfigTypesPlayer()
                .context() //
                .registerDataPlayer(ChatData.class)
                .factory(new IFactoryOne<PlayerFactoryArgument, ChatData>() {
                    @Override
                    public ChatData getNewInstance(PlayerFactoryArgument arg) {
                        return new ChatData();
                    }
                })
                .addToGroups(CheckType.CHAT, true, IData.class, ICheckData.class)
                .context() //
                );
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerChat(final AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final boolean alreadyCancelled = event.isCancelled();
        if (!DataManager.getPlayerData(player).isCheckActive(CheckType.CHAT, player)) {
            return;
        }
        // Tell TickTask to update cached permissions.
        // (Might omit this if already cancelled.)
        final IPlayerData pData = DataManager.getPlayerData(player);
        final ChatConfig cc = pData.getGenericInstance(ChatConfig.class);
        // Then the no chat check.
        // TODO: isMainThread: Could consider event.isAsync ?
        if (textChecks(player, event.getMessage(), cc, pData, false, alreadyCancelled)) {
            event.setCancelled(true);
        }

    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerLogin(final PlayerLoginEvent event) {
        if (event.getResult() != Result.ALLOWED) {
            return;
        }
        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (!pData.isCheckActive(CheckType.CHAT, player)) {
            return;
        }
        final ChatConfig cc = pData.getGenericInstance(ChatConfig.class);
        final ChatData data = pData.getGenericInstance(ChatData.class);
        // (No forced permission update, because the associated permissions are treated as hints rather.)
        // Reset captcha of player if needed.
        synchronized (data) {
            captcha.resetCaptcha(player, cc, data, pData);
        }
        // Fast relog check.
        if (relog.isEnabled(player, pData) && relog.unsafeLoginCheck(player, cc, data,pData)) {
            event.disallow(Result.KICK_OTHER, cc.relogKickMessage);
        }
        else if (logins.isEnabled(player, pData) && logins.check(player, cc, data)) {
            event.disallow(Result.KICK_OTHER, cc.loginsKickMessage);
        }
    }

    /** We listen to PlayerCommandPreprocess events because commands can be used for spamming too. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        // Tell TickTask to update cached permissions.
        final IPlayerData pData = DataManager.getPlayerData(player);
        final ChatConfig cc = pData.getGenericInstance(ChatConfig.class);
        // Left-trim is necessary because the server accepts leading spaces with commands.
        final String message = event.getMessage();
        final String lcMessage = StringUtil.leftTrim(message).toLowerCase();
        // TODO: Remove bukkit: etc.
        final String[] split = lcMessage.split(" ", 2);
        final String alias = split[0].substring(1);
        final Command command = CommandUtil.getCommand(alias);

        final List<String> messageVars = new ArrayList<String>(); // Could as well use an array and allow null on input of SimpleCharPrefixTree.
        messageVars.add(lcMessage);
        // Message to run chat checks on.
        String checkMessage = message; 
        if (command != null) {
            messageVars.add("/" + command.getLabel().toLowerCase() + (split.length > 1 ? (" " + split[1]) : ""));
        }
        if (alias.indexOf(":") != -1) {
            final int index = message.indexOf(":") + 1;
            if (index < message.length()) {
                checkMessage = message.substring(index);
                messageVars.add(checkMessage.toLowerCase());
            }
        }
        // Prevent commands from being used by players (e.g. /op /deop /reload).
        if (cc.consoleOnlyCheck && consoleOnlyCommands.hasAnyPrefixWords(messageVars)) {
            if (command == null || command.testPermission(player)) {
                player.sendMessage(cc.consoleOnlyMessage);
            }
            event.setCancelled(true);
            return;
        }
        // Handle as chat or command.
        if (chatCommands.hasAnyPrefixWords(messageVars)) {
            // Treat as chat.
            // TODO: Consider requesting permission updates on these, for consistency.
            // TODO: Cut off the command ?.
            if (textChecks(player, checkMessage, cc, pData, true, false)) {
                event.setCancelled(true);
            }
        }
        else if (!commandExclusions.hasAnyPrefixWords(messageVars)) {
            // Treat as command.
            if (commands.isEnabled(player, pData) && commands.check(player, checkMessage, cc, pData, captcha)) {
                event.setCancelled(true);
            }
            else {
                // TODO: Consider always checking these?
                // Note that this checks for prefixes, not prefix words.
                final MovingConfig mcc = pData.getGenericInstance(MovingConfig.class);
                if (mcc.passableUntrackedCommandCheck && mcc.passableUntrackedCommandPrefixes.hasAnyPrefix(messageVars)) {
                    if (checkUntrackedLocation(player, message, mcc, pData)) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private boolean checkUntrackedLocation(final Player player, final String message, 
                                           final MovingConfig mcc, final IPlayerData pData) {
        final Location loc = player.getLocation(useLoc);
        boolean cancel = false;
        if (MovingUtil.shouldCheckUntrackedLocation(player, loc, pData)) {
            final Location newTo = MovingUtil.checkUntrackedLocation(loc);
            if (newTo != null) {
                if (mcc.passableUntrackedCommandTryTeleport 
                    && SchedulerHelper.teleportEntity(player, newTo, BridgeMisc.TELEPORT_CAUSE_CORRECTION_OF_POSITION)) {
                    NCPAPIProvider.getNoCheatPlusAPI().getLogManager().info(Streams.TRACE_FILE, player.getName() + " runs the command '" + message + "' at an untracked location: " + loc + " , teleport to: " + newTo);
                } 
                else {
                    NCPAPIProvider.getNoCheatPlusAPI().getLogManager().info(Streams.TRACE_FILE, player.getName() + " runs the command '" + message + "' at an untracked location: " + loc + " , cancel the command.");
                    cancel = true;
                }
            }
        }
        useLoc.setWorld(null); // Cleanup.
        return cancel;
    }

    private void initFilters(ConfigFile config) {
        CommandUtil.feedCommands(consoleOnlyCommands, config, ConfPaths.PROTECT_COMMANDS_CONSOLEONLY_CMDS, true);
        CommandUtil.feedCommands(chatCommands, config, ConfPaths.CHAT_COMMANDS_HANDLEASCHAT, true);
        CommandUtil.feedCommands(commandExclusions, config, ConfPaths.CHAT_COMMANDS_EXCLUSIONS, true);
    }

    private boolean textChecks(final Player player, final String message, 
                               final ChatConfig cc, final IPlayerData pData,
                               final boolean isMainThread, final boolean alreadyCancelled) {
        return text.isEnabled(player, pData) && text.check(player, message, cc, pData, captcha, isMainThread, alreadyCancelled);
    }

    @Override
    public void onReload() {
        // Read some things from the global config file.
        ConfigFile config = ConfigManager.getConfigFile();
        initFilters(config);
        text.onReload();
        logins.onReload();
    }

    @Override
    public void playerJoins(final Player player) {
        final IPlayerData pData = DataManager.getPlayerData(player);
        final ChatConfig cc = pData.getGenericInstance(ChatConfig.class);
        final ChatData data = pData.getGenericInstance(ChatData.class);
        /*
         * TODO: The isEnabled check must be done with IWorldData (no locking).
         * Specifically because enabling/disabling checks should be done in the
         * primary thread, regardless of the capabilities of WorldDataManager
         * implementation.
         */
        synchronized (data) {
            if (captcha.isEnabled(player, pData)) {
                if (captcha.shouldCheckCaptcha(player, cc, data, pData)) {
                    // shouldCheckCaptcha: only if really enabled.
                    // TODO: Later: add check for cc.captchaOnLogin or so (before shouldCheckCaptcha).
                    // TODO: maybe schedule this to come after other plugins messages.
                    captcha.sendNewCaptcha(player, cc, data);
                }
            }
        }
    }

    @Override
    public void playerLeaves(final Player player) {}
}
