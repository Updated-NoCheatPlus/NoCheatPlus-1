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
package fr.neatmonster.nocheatplus.checks.blockplace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.TickTask;

public class AutoSign extends Check {

    /** Reference time that is needed to edit the most complicated sign :). */
    private static long maxEditTime = 1500;
    /** Fastest time "possible" estimate for an empty sign. */
    private static long minEditTime = 150;
    /** Minimum time needed to add one extra line (not the first). */
    private static long minLineTime = 50;
    /** Minimum time needed to type a character. */
    private static long minCharTime = 50;

    private final List<String> tags = new ArrayList<String>();

    final Set<Character> chars = new HashSet<Character>(15 * 4);

    public AutoSign() {
        super(CheckType.BLOCKPLACE_AUTOSIGN);
    }
    
    /**
     * Checks a player
     * 
     * @param player
     * @param block
     * @param lines
     * @param pData
     * @return true if the player failed the check.
     */
    public boolean check(final Player player, final Block block, final String[] lines, final IPlayerData pData) {
        tags.clear();
        final long time = System.currentTimeMillis();
        final BlockPlaceData data = pData.getGenericInstance(BlockPlaceData.class);
        final BlockPlaceConfig cc = pData.getGenericInstance(BlockPlaceConfig.class);
        Material mat = block.getType();
        String s = mat.toString();
        if (s.endsWith("_WALL_HANGING_SIGN")) {
            s = s.replace("WALL_HANGING", "HANGING");
            // A "wooden_wall_hanging_sign" block is just an "wooden_hanging_sign" as an item.
            mat = Material.getMaterial(s);
        }
        else if (s.endsWith("_WALL_SIGN")) {
            s = s.replace("_WALL_SIGN", "_SIGN");
            // a "wooden_wall_sign" block is just a "wooden_sign" as an item.
            mat = Material.getMaterial(s);
        } 
        else if (s.endsWith("WALL_SIGN")) {
            s = s.replace("WALL_", "");
            // A "wall_sign" block is just a "sign" as an item.
            mat = Material.getMaterial(s);
        }
        else if (s.equals("SIGN_POST")) {
            mat = Material.getMaterial("SIGN");
        }

        if (pData.isDebugActive(CheckType.BLOCKPLACE_AUTOSIGN)) {
            debug(player, "Block-place hash: " + BlockPlaceListener.getBlockPlaceHash(block, mat) + ", Material type: " + mat + " / " + s);
        }

        // Check hash match
        if (data.autoSignPlacedHash != 0 && data.autoSignPlacedHash != BlockPlaceListener.getBlockPlaceHash(block, mat)) {
            tags.add("block_mismatch");
            return handleViolation(player, maxEditTime, data, cc);
        }

        if (time < data.signOpenTime) {
            data.signOpenTime = 0;
            return false;
        }
        // Check time, mind lag.
        final long editTime = time - data.signOpenTime;
        long expected = getExpectedEditTime(lines, cc.autoSignSkipEmpty);
        if (expected == 0) {
            return false;
        }
        expected = (long) (expected / TickTask.getLag(expected, true));
        if (expected > editTime){
            tags.add("edit_time");
            return handleViolation(player, expected - editTime, data, cc);
        }
        return false;
    }

    private long getExpectedEditTime(final String[] lines, final boolean skipEmpty) {
        long expected = minEditTime;
        int n = 0;
        for (String line : lines){
            if (line != null){
                line = line.trim().toLowerCase();
                if (!line.isEmpty()){
                    chars.clear();
                    n += 1;
                    for (final char c : line.toCharArray()) {
                        chars.add(c);
                    }
                    expected += minCharTime * chars.size();
                }
            }
        }
        if (skipEmpty && n == 0) {
            return 0;
        }
        if (n > 1) {
            expected += minLineTime * n;
        }
        return expected;
    }

    /**
     * 
     * @param player
     * @param violationTime Amount of too fast editing.
     * @param data
     * @return
     */
    private boolean handleViolation(final Player player, final long violationTime, final BlockPlaceData data, final BlockPlaceConfig cc) {
        final double addedVL = 10.0 * Math.min(maxEditTime, violationTime) / maxEditTime;
        data.autoSignVL += addedVL;
        final ViolationData vd = new ViolationData(this, player, data.autoSignVL, addedVL, cc.autoSignActions);
        if (vd.needsParameters()){
            vd.setParameter(ParameterName.TAGS, StringUtil.join(tags, "+"));
        }
        return executeActions(vd).willCancel();
    }
}
