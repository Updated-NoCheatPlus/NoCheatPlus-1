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
package fr.neatmonster.nocheatplus.command.admin.top;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationHistory;
import fr.neatmonster.nocheatplus.checks.ViolationHistory.VLView;
import fr.neatmonster.nocheatplus.command.BaseCommand;
import fr.neatmonster.nocheatplus.command.CommandUtil;
import fr.neatmonster.nocheatplus.compat.SchedulerHelper;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.utilities.CheckTypeUtil;
import fr.neatmonster.nocheatplus.utilities.FCFSComparator;

public class TopCommand extends BaseCommand{
    
    private static final String[][] comparatorArgs = new String[][]{
        //{"-player", "-name", "-playername"},
        //{"-check", "-type", "-checktype}
        {"-sumvl", "-sum", "-vl"},
        {"-n", "-number", "-num", "-nvl"},
        {"-avgvl", "-avg", "-avl", "-av", "-average", "-averagevl", "-avvl"},
        {"-maxvl", "-max", "-maximum", "-maximumvl"},
        {"-time", "-"},
    };
    
    protected static class PrimaryThreadWorker implements Runnable{
        private final Collection<CheckType> checkTypes;
        private final CommandSender sender;
        private final Comparator<VLView> comparator;
        private final int n;
        private final Plugin plugin;
        public PrimaryThreadWorker(CommandSender sender, Collection<CheckType> checkTypes, Comparator<VLView> comparator, int n, Plugin plugin) {
            this.checkTypes = new LinkedHashSet<CheckType>(checkTypes);
            this.sender = sender;
            this.comparator = comparator;
            this.n = n;
            this.plugin = plugin;
        }
        
        @Override
        public void run() {
            final Iterator<CheckType> it = checkTypes.iterator();
            List<VLView> views = null;
            CheckType type = null;
            while (it.hasNext()) {
                type = it.next();
                it.remove();
                views = ViolationHistory.getView(type);
                if (views.isEmpty()) {
                    views = null;
                } else {
                    break;
                }
            }
            if (views == null) {
                sender.sendMessage(TAG + "No history to process.");
            } else {
                // Start sorting and result processing asynchronously.
                final CheckType ct = type;
                final List<VLView> vlviews = views;
                SchedulerHelper.runTaskAsync(plugin, (arg) -> new AsynchronousWorker(sender, ct, vlviews, checkTypes, comparator, n, plugin).run());
            }
        }
        
    }
    
    protected static class AsynchronousWorker implements Runnable{
        private final CommandSender sender;
        private final CheckType checkType;
        private final List<VLView> views;
        private final Collection<CheckType> checkTypes;
        private final Comparator<VLView> comparator;
        private final int n;
        private final Plugin plugin;
        public AsynchronousWorker(CommandSender sender, CheckType checkType, List<VLView> views, Collection<CheckType> checkTypes, Comparator<VLView> comparator, int n, Plugin plugin) {
            this.sender = sender;
            this.checkType = checkType;
            this.views = views;
            this.checkTypes = checkTypes;
            this.comparator = comparator;
            this.n = n;
            this.plugin = plugin;
        }
        @Override
        public void run() {
            final DecimalFormat format = new DecimalFormat("#.#");

            final String c1, c2, c3, bo, it;
            if (sender instanceof Player) {
                c1 = ChatColor.GRAY.toString();
                c2 = ChatColor.RED.toString();
                c3 = ChatColor.GOLD.toString();
                bo = ChatColor.BOLD.toString();
                it = ChatColor.ITALIC.toString();
            } else {
                c1 = c2 = c3 = bo = it = "";
            }
            
            // Sort
            Collections.sort(views, comparator);
            // Display.
            final StringBuilder builder = new StringBuilder(100 + 32 * views.size());
            builder.append((sender instanceof Player ? TAG : CTAG) + "Top results for check: " + c3 + bo +""+ it + checkType.toString().toLowerCase());
            int done = 0;

            for (final VLView view : views) {
                builder.append(c1 + "\n• "+ c1 +"Player with top results: " + c2 + view.name);
                // sum
                builder.append(c1 + "\n• " + c1 + "Sum: " + c2 + format.format(view.sumVL) + c1 + " VLs.");
                // n
                builder.append(c1 + "\n• " + c1 + "Triggered: " + c2 + view.nVL + c1 + " times.");
                // avg
                builder.append(c1 + "\n• " + c1 + "Average: " + c2 + format.format(view.sumVL / view.nVL) + c1 + " VL.");
                // max
                builder.append(c1 + "\n• " + c1 + "Max: " + c2 + format.format(view.maxVL) + c1 + " VL.");
    
                done ++;
                if (done >= n) {
                    break;
                }
            }
            if (views.isEmpty()) {
                builder.append((sender instanceof Player ? TAG : CTAG) + "Nothing to display.");
            }
            final String message = builder.toString();
            SchedulerHelper.runSyncTask(plugin, (arg) -> sender.sendMessage(message));
            if (!checkTypes.isEmpty()) {
                SchedulerHelper.runSyncTask(plugin, (arg) -> new PrimaryThreadWorker(sender, checkTypes, comparator, n, plugin).run());
            }
        }
    }

    public TopCommand(JavaPlugin plugin) {
        super(plugin, "top", Permissions.COMMAND_TOP);
        this.usage = TAG + "Optional: Specify number of entries to show (once).\nObligatory: Specify check types (multiple possible).\nOptional: Specify what to sort by (multiple possible: -sumvl, -avgvl, -maxvl, -nvl, -name, -time).\nThis is a heavy operation, use with care."; // -check
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length < 2) {
            sender.sendMessage((sender instanceof Player ? TAG : CTAG) + "Bad setup.\nOptional: Specify number of entries to show (once).\nObligatory: Specify check types (multiple possible).\nOptional: Specify what to sort by (multiple possible: -sumvl, -avgvl, -maxvl, -nvl, -name, -time).\nThis is a heavy operation, use with care."); // -check)
            return true;
        }
        int startIndex = 1;
        int n = 10;
        try {
            n = Integer.parseInt(args[1].trim());
            startIndex = 2;
        } catch (NumberFormatException e) {}
        if (n <= 0) {
            sender.sendMessage((sender instanceof Player ? TAG : CTAG) + "Setting number of entries to 10");
            n = 1;
        } else if ((sender instanceof Player) && n > 300) {
            sender.sendMessage((sender instanceof Player ? TAG : CTAG) + "Capping number of entries at 300.");
            n = 300;
        } else if  (n > 10000) {
            sender.sendMessage((sender instanceof Player ? TAG : CTAG) + "Capping number of entries at 10000.");
            n = 10000;
        }
        
        Set<CheckType> checkTypes = new LinkedHashSet<CheckType>();
        for (int i = startIndex; i < args.length; i ++) {
            CheckType type = null;
            try {
                type = CheckType.valueOf(args[i].trim().toUpperCase().replace('-', '_').replace('.', '_'));
            } catch (Throwable t) {} // ...
            if (type != null) {
                checkTypes.addAll(CheckTypeUtil.getWithDescendants(type)); // Includes type.
            }
        }
        if (checkTypes.isEmpty()) {
            sender.sendMessage((sender instanceof Player ? TAG : CTAG) + "No check types specified.");
            return false;
        }
        
        Comparator<VLView> comparator = VLView.parseMixedComparator(args, startIndex);
        if (comparator == null) {
            // TODO: Default comparator ?
            comparator = new FCFSComparator<VLView>(Arrays.asList(VLView.CmpnVL, VLView.CmpSumVL), true);
        }
        
        // Run a worker task.
        final Comparator<VLView> fcomparator = comparator;
        final int fn = n;
        SchedulerHelper.runSyncTask(access, (arg) -> new PrimaryThreadWorker(sender, checkTypes, fcomparator, fn, access).run());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String arg = args[args.length - 1];
        if (arg.startsWith("-")) {
            while (arg.startsWith("-")) {
                arg = arg.substring(1);
            }
            arg = "-" + arg;
            return CommandUtil.getTabMatches(arg.toLowerCase(), comparatorArgs);
        } else {
            return CommandUtil.getCheckTypeTabMatches(arg);
        }
    }
    
}
