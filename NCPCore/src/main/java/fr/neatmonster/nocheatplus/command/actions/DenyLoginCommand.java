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

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.command.AbstractCommand;
import fr.neatmonster.nocheatplus.command.BaseCommand;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.players.DataManager;

public class DenyLoginCommand extends BaseCommand {

    public DenyLoginCommand(JavaPlugin plugin) {
        super(plugin, "denylogin", Permissions.COMMAND_DENYLOGIN,
                new String[]{"tempkick", "tkick", "tempban", "tban",});
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player) {
            sender.sendMessage(TAG + "This command can only be run from the console.");
            return true;
        }
        // Args contains sub command label as first arg.
        if (args.length == 1) {
            StaticLog.logInfo("Please specify a player to temporarily deny log-in.");
            return true;
        }
        else if (args.length == 2) {
            StaticLog.logInfo("Please specify the log-in denial duration (minutes).");
            return true;
        }
        long base = 60000; // minutes (!)
        final String name = args[1];
        long duration = -1;
        try{
            // TODO: parse for abbreviations like 30s 30m 30h 30d, and set base...
            duration = Integer.parseInt(args[2]);
        }
        catch( NumberFormatException e){};
        if (duration <= 0) return false;
        final long finalDuration = duration * base;
        final String reason;
        if (args.length > 3) reason = AbstractCommand.join(args, 3);
        else reason = "";
        denyLogin(sender, name, finalDuration, reason);
        return true;
    }


    protected void denyLogin(CommandSender sender, String name, long duration, String reason){
        Player player = DataManager.getPlayer(name);
        NCPAPIProvider.getNoCheatPlusAPI().denyLogin(name, duration);
        if (player == null) return;
        player.kickPlayer(reason);
        StaticLog.logInfo("(" + sender.getName() + ") Kicked " + player.getName() + " for " + duration/60000 +" minutes: " + reason);
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
