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
package fr.neatmonster.nocheatplus.command.actions;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import fr.neatmonster.nocheatplus.command.AbstractCommand;
import fr.neatmonster.nocheatplus.command.BaseCommand;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.utilities.IdUtil;

public class BanCommand extends BaseCommand {

    private final JavaPlugin plugin;
    public BanCommand(JavaPlugin plugin) {
        super(plugin, "ban", Permissions.COMMAND_BAN);
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        if (!demandConsoleCommandSender(sender)) {
            return true;
        }
        // TODO: Consider supporting vanilla syntax or removing this command :p.

        // Args contains "ban" as first arg.
        if (args.length == 1) {
            StaticLog.logWarning("Please specify a player to ban.");
            return true;
        }
        else if (args.length == 2) {
            StaticLog.logWarning("Please specify the ban time (minutes, reason is optional).");
            return true;
        }
        final String name = args[1].trim();
        final String reason;
        if (args.length > 2) {
            reason = AbstractCommand.join(args, 2);
        }
        else {
            reason = "";
        }
        ban(sender, name, reason);
        return true;
    }

    /**
     * 
     * @param sender
     * @param name Trimmed name.
     * @param reason
     */
    void ban(CommandSender sender, String name, String reason) {
        final Server server = Bukkit.getServer();
        Player player = DataManager.getPlayer(name);
        // Pro logic below. 
        if (player == null && !IdUtil.isValidMinecraftUserName(name)) {
            UUID id = IdUtil.UUIDFromStringSafe(name);
            if (id != null) {
                StaticLog.logWarning("Banning by UUID might not work (" + id.toString()+"), relay to the vanilla command.");
            } else {
                StaticLog.logWarning("Might not be a valid user name: " + name);
            }
        }
        if (player != null){
            player.kickPlayer(reason);
        }
        // Relay to the server command for compatibility reasons.
        server.dispatchCommand(server.getConsoleSender(), "ban " + name);
        logBan(sender, player, name, reason);

    }

    private void logBan(CommandSender sender, Player player, String name, String reason) {
        StaticLog.logInfo("(" + sender.getName() + ") Banned " + name + (player != null ? ("/" + player.getName()) : "") + " : " + reason);
    }

    /* (non-Javadoc)
     * @see fr.neatmonster.nocheatplus.command.AbstractCommand#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Complete Players
        if (args.length == 2) {
            List<String> players = Lists.newArrayList();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return players;
        }
        return null;
    }
}
