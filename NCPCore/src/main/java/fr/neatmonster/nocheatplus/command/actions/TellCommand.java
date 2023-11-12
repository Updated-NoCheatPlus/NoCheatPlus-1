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
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.utilities.ColorUtil;

/**
 * For warnings etc.
 * @author mc_dev
 *
 */
public class TellCommand extends BaseCommand {

	public TellCommand(JavaPlugin plugin) {
		super(plugin, "tell", Permissions.COMMAND_TELL);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, final String[] args) {
		if (!demandConsoleCommandSender(sender)) {
			return true;
		}
		if (args.length < 3) {
			StaticLog.logInfo("Bad setup. Command usage: /ncp tell (playername) (message to send).");
			return true;
		}
		final String name = args[1].trim();
		final String message = AbstractCommand.join(args, 2);
		tell(name, message);
		return true;
	}

	private void tell(String name, String message) {
		Player player = DataManager.getPlayer(name);
		if (player != null) player.sendMessage(ColorUtil.replaceColors(message));
	}

	/* (non-Javadoc)
	 * @see fr.neatmonster.nocheatplus.command.BaseCommand#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
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
