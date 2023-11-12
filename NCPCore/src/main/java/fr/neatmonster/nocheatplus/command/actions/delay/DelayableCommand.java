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
package fr.neatmonster.nocheatplus.command.actions.delay;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import fr.neatmonster.nocheatplus.command.BaseCommand;
import fr.neatmonster.nocheatplus.compat.SchedulerHelper;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.permissions.RegisteredPermission;

/**
 * A command that allows to specify a delay for running.
 * @author asofold
 *
 */
public abstract class DelayableCommand extends BaseCommand {

    /**
     * Parse an argument for a delay in ticks. The delay is specified with "delay=...".
     * @param args
     * @param index
     * @return
     */
    public static long parseDelay(String[] args, int index){
        return parseDelay(args, index, -1);
    }

    /**
     * Parse an argument for a delay in ticks. The delay is specified with "delay=...".
     * @param args
     * @param index
     * @param preset Preset delay if none is given.
     * @return ticks or -1 if no delay found.
     */
    public static long parseDelay(String[] args, int index, int preset){
        if (args.length <= index) return preset;
        String arg = args[index].trim().toLowerCase();
        if (!arg.startsWith("delay=")) return preset;
        if (arg.length() < 7) return -1;
        try{
            long res = Long.parseLong(arg.substring(6));
            if (res < 0) 
                return -1;
            else 
                return res;
        } catch (NumberFormatException e){
            return -1;
        }
    }

    protected final int delayIndex;
    protected final boolean mustHaveDelay;
    protected final int delayPreset;
    protected boolean demandConsoleCommandSender = false;

    /**
     * (Delay is not obligatory, inserted after the first argument.)
     * @param plugin
     * @param label
     */
    public DelayableCommand(JavaPlugin plugin, String label, RegisteredPermission permission){
        this(plugin, label, permission, 1);
    }

    /**
     * (Delay is not obligatory.)
     * @param plugin
     * @param label
     * @param delayIndex
     */
    public DelayableCommand(JavaPlugin plugin, String label, RegisteredPermission permission, 
            int delayIndex){
        this(plugin, label, permission, delayIndex, -1, false);
    }

    /**
     * 
     * @param plugin
     * @param label Sub command label.
     * @param delayIndex Index at which to look for the delay specification.
     * @param mustHaveDelay If specifying a delay is obligatory.
     */
    public DelayableCommand(JavaPlugin plugin, String label, RegisteredPermission permission, 
            int delayIndex, int delayPreset, boolean mustHaveDelay) {
        this(plugin, label, permission, null, delayIndex, delayPreset, mustHaveDelay);
    }

    /**
     * 
     * @param plugin
     * @param label
     * @param permission
     * @param aliases Sub command label aliases.
     * @param delayIndex Index at which to look for the delay spec.
     * @param delayPreset Preset if no delay is given.
     * @param mustHaveDelay If delay must be specified.
     */
    public DelayableCommand(JavaPlugin plugin, String label, RegisteredPermission permission, String[] aliases, int delayIndex, int delayPreset, boolean mustHaveDelay) {
        super(plugin, label, permission, aliases);
        this.delayIndex = delayIndex;
        this.mustHaveDelay = mustHaveDelay;
        this.delayPreset = delayPreset;
    }

    /**
     * Execute the command, check validity and schedule a task for delayed execution (use schedule(...)).
     * @param sender
     * @param command
     * @param label Command label, this is not necessarily this.label (!), this.label can be the first argument.
     * @param alteredArgs args with the delay specification removed.
     * @param delay
     * @return If the command was understood in general.
     */
    public abstract boolean execute(CommandSender sender, Command command, String label, 
            String[] alteredArgs, long delay);

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, 
            final String label, final String[] args ) {

        if (demandConsoleCommandSender && !demandConsoleCommandSender(sender)) {
            return true;
        }
        // Parse the delay and alter the args accordingly.
        long delay = parseDelay(args, delayIndex, delayPreset);
        String[] alteredArgs;
        if (args.length <= delayIndex){
            // No delay found, if demanded return.
            if (mustHaveDelay) 
            StaticLog.logInfo("Bad setup. Command usage /ncp delay delay=(ticks) (command to delay).");
            alteredArgs = args;
            return true;
        }
        else{
            boolean hasDef = args[delayIndex].startsWith("delay=") && delay != -1;
            alteredArgs = new String[args.length + (hasDef ? -1 : 0)];
            if (alteredArgs.length > 0){
                int increment = 0;
                for (int i = 0; i < args.length; i++){
                    if (i == delayIndex && hasDef){
                        // ignore this one.
                        increment = -1;
                        continue;
                    }
                    alteredArgs[i + increment] = args[i];
                }
            }
        }
        return execute(sender, command, label, alteredArgs, delay);
    }

    /**
     * Execute directly or schedule the task for later execution.
     * @param runnable
     * @param delay Delay in ticks.
     */
    protected void schedule(Runnable runnable, long delay){
        if (delay < 0) 
            runnable.run();
        else if (delay == 0)
            SchedulerHelper.runSyncTask(access, (arg) -> runnable.run());
        else 
            SchedulerHelper.runSyncDelayedTask(access, (arg) -> runnable.run(), delay);
    }

    /* (non-Javadoc)
     * @see fr.neatmonster.nocheatplus.command.AbstractCommand#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Fill in players.
        // TODO: Add altered signature for alteredArgs ?
        return null;
    }
}
