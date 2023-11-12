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

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.Lists;

import fr.neatmonster.nocheatplus.command.AbstractCommand;
import fr.neatmonster.nocheatplus.command.BaseCommand;
import fr.neatmonster.nocheatplus.compat.SchedulerHelper;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.players.DataManager;

public class KickCommand extends BaseCommand {

    private final JavaPlugin plugin;
    public KickCommand(JavaPlugin plugin) {
        super(plugin, "kick", Permissions.COMMAND_KICK);
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        if (!demandConsoleCommandSender(sender)) {
            return true;
        }
        // Args contains "kick" as first arg.
        if (args.length < 2) {
            StaticLog.logInfo("Please specify a player to kick (Reason is optional). ");
            return true;
        }
        final String name = args[1];
        final String reason;
        if (args.length > 2) reason = AbstractCommand.join(args, 2);
        else reason = "";
        kick(sender, name, reason);
        return true;
    }

    void kick(CommandSender sender, String name, String reason) {
        Player player = DataManager.getPlayer(name);
        if (player == null) return;
        SchedulerHelper.runSyncTask(plugin, (arg) -> {
            player.kickPlayer(reason);
            StaticLog.logInfo("(" + sender.getName() + ") Kicked " + player.getName() + " : " + reason);
        });
    }

    /* (non-Javadoc)
     * @see fr.neatmonster.nocheatplus.command.AbstractCommand#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
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
