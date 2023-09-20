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
import fr.neatmonster.nocheatplus.command.BaseCommand;
import fr.neatmonster.nocheatplus.permissions.Permissions;

public class AllowLoginCommand extends BaseCommand {

	public AllowLoginCommand(JavaPlugin plugin) {
		super(plugin, "allowlogin", Permissions.COMMAND_ALLOWLOGIN,
				new String[]{"unkick"});
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {

		if (args.length != 2) {
			sender.sendMessage((sender instanceof Player ? TAG : "") + "Please specify a player to allow to log-in again.");
			return true;
		}
		if (args[1].trim().equals("*")){
			sender.sendMessage((sender instanceof Player ? TAG : "") + "Removed " + NCPAPIProvider.getNoCheatPlusAPI().allowLoginAll() + " players from the 'deny-login' list.");
		}
		else if (NCPAPIProvider.getNoCheatPlusAPI().allowLogin(args[1])){
			sender.sendMessage((sender instanceof Player ? TAG : "") + "Allow to login again: " + args[1].trim());
		}
		else {
			sender.sendMessage((sender instanceof Player ? TAG : "") + "Was not denied to login: " + args[1].trim());
		}
		return true;
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
