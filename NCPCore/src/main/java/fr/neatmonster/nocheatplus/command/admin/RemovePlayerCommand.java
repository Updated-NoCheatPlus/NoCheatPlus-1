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
package fr.neatmonster.nocheatplus.command.admin;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationHistory;
import fr.neatmonster.nocheatplus.command.BaseCommand;
import fr.neatmonster.nocheatplus.command.CommandUtil;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.utilities.StringUtil;

public class RemovePlayerCommand extends BaseCommand {

    public RemovePlayerCommand(JavaPlugin plugin) {
        super(plugin, "removeplayer", Permissions.COMMAND_REMOVEPLAYER, new String[]{
                "remove",	
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        final String c1, c2, c3, c5, c6;
        if (sender instanceof Player) {
            c1 = ChatColor.GRAY.toString();
            c2 = ChatColor.BOLD.toString();
            c3 = ChatColor.RED.toString();
            c5 = ChatColor.GOLD.toString();
            c6 = ChatColor.WHITE.toString();
        } else {
            c1 = c2 = c3 = c5 = c6 = "";
        }

        if (args.length < 2) {
            sender.sendMessage((sender instanceof Player ? TAG : CTAG) + "Please specify a player's data to remove.");
            return true;
        }
        else if (args.length > 3) {
            sender.sendMessage((sender instanceof Player ? TAG : CTAG) + "Too many arguments. Command usage: /ncp removeplayer (playername) (checktype).");
            return true;
        }
        String playerName = args[1];
        final CheckType checkType;

        if (args.length == 3){
            try{
                checkType = CheckType.valueOf(args[2].toUpperCase().replace('-', '_').replace('.', '_'));
            } catch (Exception e){
                sender.sendMessage((sender instanceof Player ? TAG : CTAG) + "Could not interpret: " + args[2]);
                sender.sendMessage((sender instanceof Player ? TAG : CTAG) + "Check type should be one of: " + StringUtil.join(Arrays.asList(CheckType.values()), c6 + ", " + c3));
                return true;
            }
        }
        else checkType = CheckType.ALL;

        if (playerName.equals("*")){
            DataManager.clearData(checkType);
            sender.sendMessage((sender instanceof Player ? TAG : CTAG) + "Removed all data and history: " + c3 + checkType);
            return true;
        }

        final Player player = DataManager.getPlayer(playerName);
        if (player != null) playerName = player.getName();

        ViolationHistory hist = ViolationHistory.getHistory(playerName, false);
        boolean somethingFound = false;
        if (hist != null){
            somethingFound = hist.remove(checkType);
            if (checkType == CheckType.ALL){
                somethingFound = true;
                ViolationHistory.removeHistory(playerName);
            }
        }

        if (DataManager.removeExecutionHistory(checkType, playerName)) {
            somethingFound = true;
        }

        if (DataManager.removeData(playerName, checkType)) {
            somethingFound = true;
        }

        if (somethingFound){
            sender.sendMessage((sender instanceof Player ? TAG : CTAG) + "Issued history and data removal (" + c3 + checkType + c1 +"): " + c3 + playerName + c1);
        }
        else
            sender.sendMessage((sender instanceof Player ? TAG : CTAG) + "Nothing found (" + c3 + checkType + c1 +"): " + c3 + playerName + c1 + " (spelled correctly?)");
        return true;
    }

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
        // Complete CheckType
        if (args.length == 3) return CommandUtil.getCheckTypeTabMatches(args[2]);
        return null;
    }

    /* (non-Javadoc)
     * @see fr.neatmonster.nocheatplus.command.AbstractCommand#testPermission(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean testPermission(CommandSender sender, Command command, String alias, String[] args) {
        return super.testPermission(sender, command, alias, args) 
                || args.length >= 2 && args[1].trim().equalsIgnoreCase(sender.getName()) 
                && sender.hasPermission(Permissions.COMMAND_REMOVEPLAYER_SELF.getBukkitPermission());
    }

}
