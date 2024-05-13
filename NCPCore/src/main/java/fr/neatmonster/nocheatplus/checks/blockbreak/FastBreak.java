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
package fr.neatmonster.nocheatplus.checks.blockbreak;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.compat.AlmostBoolean;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.BridgePotionEffect;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.PotionUtil;
import fr.neatmonster.nocheatplus.utilities.TickTask;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;

/**
 * A check used to verify if the player isn't breaking blocks faster than possible.
 */
public class FastBreak extends Check {

    /**
     * Instantiates a new fast break check.
     */
    public FastBreak() {
        super(CheckType.BLOCKBREAK_FASTBREAK);
    }

    /**
     * Checks a player for fastbreak. This is NOT for creative mode.
     * 
     * @param player
     *            the player
     * @param block
     *            the block
     * @param isInstaBreak 
     * @param data 
     * @param cc 
     * @param elaspedTime
     * @return true, if successful
     */
    public boolean check(final Player player, final Block block, final AlmostBoolean isInstaBreak, 
            final BlockBreakConfig cc, final BlockBreakData data, final IPlayerData pData) {
        final long now = System.currentTimeMillis();
        boolean cancel = false;

        // Determine expected breaking time by block type.
        final Material blockType = block.getType();
        final long expectedBreakingTime = Math.max(0, Math.round((double) BlockProperties.getBreakingDuration(blockType, player) * (double) cc.fastBreakModSurvival / 100D));

        final long elapsedTime;
        // TODO: Concept for unbreakable blocks? Context: extreme VL.
        // TODO: Should it be breakingTime instead of 0 for inconsistencies?
        if (cc.fastBreakStrict) {
            // Counting interact...break.
            elapsedTime = (data.fastBreakBreakTime > data.fastBreakfirstDamage) ? 0 : now - data.fastBreakfirstDamage;
        }
        else {
            // Counting break...break.
            elapsedTime = (data.fastBreakBreakTime > now) ? 0 : now - data.fastBreakBreakTime;
        }

        // Check if the time used time is lower than expected.
        if (isInstaBreak.decideOptimistically()) {
            // Ignore those for now.
            // TODO: Find out why this was commented out long ago a) did not fix mcMMO b) exploits.
            // TODO: Maybe adjust time to min(time, SOMETHING) for MAYBE/YES.
        }
        else if (elapsedTime < 0) {
            // Ignore it. TODO: ?
        }
        else if (elapsedTime + cc.fastBreakDelay < expectedBreakingTime) {
            // lag or cheat or Minecraft.

            // Count in server side lag, if desired.
            final float lag = pData.getCurrentWorldDataSafe().shouldAdjustToLag(type) 
                    ? TickTask.getLag(expectedBreakingTime, true) : 1f;

                    final long missingTime = expectedBreakingTime - (long) (lag * elapsedTime);

                    if (missingTime > 0) {
                        // Add as penalty
                        data.fastBreakPenalties.add(now, (float) missingTime);


                        // Only raise a violation, if the total penalty score exceeds the contention duration (for lag, delay).
                        if (data.fastBreakPenalties.score(cc.fastBreakBucketFactor) > cc.fastBreakGrace) {
                            // TODO: maybe add one absolute penalty time for big amounts to stop breaking until then
                            final double vlAdded = (double) missingTime / 1000.0;
                            data.fastBreakVL += vlAdded;
                            final ViolationData vd = new ViolationData(this, player, data.fastBreakVL, vlAdded, cc.fastBreakActions);
                            if (vd.needsParameters()) {
                                vd.setParameter(ParameterName.BLOCK_TYPE, blockType.toString());
                            }
                            cancel = executeActions(vd).willCancel();
                        }
                        // else: still within contention limits.
                    }
        }
        else if (expectedBreakingTime > cc.fastBreakDelay) {
            // Fast breaking does not decrease violation level.
            data.fastBreakVL *= 0.9D;
        }

        // TODO: Rework to use (then hopefully completed) BlockBreakKey.
        if (pData.isDebugActive(type)) {
            detailDebugStats(player, isInstaBreak, blockType, 
                    elapsedTime, expectedBreakingTime, data, pData);
        }
        else {
            data.stats = null;
        }

        // (The break time is set in the listener).

        return cancel;
    }

    private void detailDebugStats(final Player player, final AlmostBoolean isInstaBreak,
            final Material blockType, final long elapsedTime, final long expectedBreakingTime,
            final BlockBreakData data, final IPlayerData pData) {
        if (pData.hasPermission(Permissions.ADMINISTRATION_DEBUG, player)) {
            // General stats:
            // TODO: Replace stats by new system (BlockBreakKey once complete), commands to inspect / auto-config.
            data.setStats();
            data.stats.addStats(data.stats.getId(blockType+ "/u", true), elapsedTime);
            data.stats.addStats(data.stats.getId(blockType + "/r", true), expectedBreakingTime);
            player.sendMessage(data.stats.getStatsStr(true));
            // Send info about current break:
            final ItemStack stack = Bridge1_9.getItemInMainHand(player);
            final boolean isValidTool = BlockProperties.isValidTool(blockType, stack);
            final double haste = PotionUtil.getPotionEffectAmplifier(player, BridgePotionEffect.HASTE);
            String msg = (isInstaBreak.decideOptimistically() ? ("[Insta=" + isInstaBreak + "]") : "[Normal]") + "[" + blockType + "] "+ elapsedTime + "u / " + expectedBreakingTime +"r (" + (isValidTool?"tool":"no-tool") + ")" + (Double.isInfinite(haste) ? "" : " haste=" + ((int) haste + 1));
            player.sendMessage(msg);
            //          net.minecraft.server.Item mcItem = net.minecraft.server.Item.byId[stack.getTypeId()];
            //          if (mcItem != null) {
            //              double x = mcItem.getDestroySpeed(((CraftItemStack) stack).getHandle(), net.minecraft.server.Block.byId[blockId]);
            //              player.sendMessage("mc speed: " + x);
            //          }
        }
    }

}
