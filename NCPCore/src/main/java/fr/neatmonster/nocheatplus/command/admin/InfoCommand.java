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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import fr.neatmonster.nocheatplus.checks.ViolationHistory;
import fr.neatmonster.nocheatplus.checks.ViolationHistory.ViolationLevel;
import fr.neatmonster.nocheatplus.command.BaseCommand;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.players.DataManager;

public class InfoCommand extends BaseCommand {

	public InfoCommand(JavaPlugin plugin) {
		super(plugin, "info", Permissions.COMMAND_INFO);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length != 2) {
            sender.sendMessage((sender instanceof Player ? TAG : CTAG) + "Please specify a player.");
            return true;
        }
		handleInfoCommand(sender, args[1]);
		return true;
	}
	
    /**
     * Handle the '/nocheatplus info' command.
     * 
     * @param sender
     *            the sender
     * @param playerName
     *            the player name
     * @return true, if successful
     */
    private void handleInfoCommand(final CommandSender sender, String playerName) {

    	final String cG, cR, cGO, bold, italicbold; 
        if (sender instanceof Player) {
            cG = ChatColor.GRAY.toString(); 
            cR = ChatColor.RED.toString();
            cGO = ChatColor.GOLD.toString();
            bold = ChatColor.BOLD.toString();
            italicbold = ChatColor.BOLD + "" + ChatColor.ITALIC;
        }
        else cG = cR = cGO = bold = italicbold = "";

    	final Player player = DataManager.getPlayer(playerName);
    	if (player != null) playerName = player.getName();
    	
    	final ViolationHistory history = ViolationHistory.getHistory(playerName, false);
    	final boolean known = player != null || history != null;
    	if (history == null){
    		sender.sendMessage((sender instanceof Player ? TAG : CTAG) + "No entries for " + cR + playerName + cG + "'s violations " + ( known? "" : "(exact spelling ?)") + ".");
    		return;
    	}
    	
        final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        final ViolationLevel[] violations = history.getViolationLevels();
        if (violations.length > 0) {
            sender.sendMessage((sender instanceof Player ? TAG : CTAG) + "Displaying " + cR + playerName + cG + "'s violations: ");
            for (final ViolationLevel violationLevel : violations) {

                final long time = violationLevel.time;
                final String[] parts = violationLevel.check.split("\\.");
                final String check = parts[parts.length - 1].toLowerCase();
                final String parent = parts[parts.length - 2].toLowerCase();
                final long sumVL = Math.round(violationLevel.sumVL);
                final long maxVL = Math.round(violationLevel.maxVL);
                final long avVl  = Math.round(violationLevel.sumVL / (double) violationLevel.nVL);
                sender.sendMessage(
                    cG + bold +"[" + cG + dateFormat.format(new Date(time)) + bold + "] " + cGO + italicbold + parent + "." + check  
                    +cG+bold + "\n• "+ cG + "Sum: " + cR + sumVL  + cG + " VLs."
                    +cG+bold + "\n• "+ cG + "Triggered: " + cR + violationLevel.nVL + cG + " times."
                    +cG+bold + "\n• "+ cG + "Average: " + cR + avVl + cG + " VL."
                    +cG+bold + "\n• "+ cG + "Max: " + cR + maxVL + cG + " VL.");
            }
        } 
        else sender.sendMessage((sender instanceof Player ? TAG : CTAG) + "No violations to display for player " + cR + playerName);
        
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
