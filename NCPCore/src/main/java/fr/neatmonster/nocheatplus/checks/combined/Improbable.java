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
package fr.neatmonster.nocheatplus.checks.combined;

import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.components.registry.feature.IDisableListener;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.TickTask;

/**
 * This check combines different other checks frequency and occurrences into one count.
 * (Intended for static access by other checks.) 
 *
 * @author asofold
 *
 */
public class Improbable extends Check implements IDisableListener {

    private static Improbable instance = null;

    /**
     * Return if to cancel.
     * @param player
     * @param weights
     * @param now
     * @return
     */
    public static final boolean check(final Player player, final float weight, final long now, final String tags, final IPlayerData pData) {
        return instance.checkImprobable(player, weight, now, tags, pData);
    }

    /**
     * Update time references only, no feeding and no violation.
     * @param player
     * @param now
     * @param pData
     */
    public static final void update(final long now, final IPlayerData pData) {
        pData.getGenericInstance(CombinedData.class).improbableCount.update(now);
    }

    /**
     * Update time references only, no feeding and no violation (convenience method).
     * @param player
     * @param now
     */
    public static final void update(final Player player, final long now) {
        update(now, DataManager.getPlayerData(player));
    }

    /**
     * Feed the check but no violations processing (convenience method).
     * @param player
     * @param weight
     * @param now
     * @param pData
     */
    public static final void feed(final Player player, final float weight, final long now, final IPlayerData pData) {
        pData.getGenericInstance(CombinedData.class).improbableCount.add(now, weight);
    }

    /**
     * Feed the check but no violations processing (convenience method).
     * @param player
     * @param weight
     * @param now
     */
    public static void feed(final Player player, final float weight, long now) {
        feed(player, weight, now, DataManager.getPlayerData(player));
    }

    ////////////////////////////////////
    // Instance methods.
    ///////////////////////////////////

    public Improbable() {
        super(CheckType.COMBINED_IMPROBABLE);
        instance = this;
    }

    /**
     * Checks a player for improbable behaviour.
     * @param player
     * @param weight
     * @param now
     * @param tags
     * @param pData
     * @return true if to cancel
     * 
     */
    private boolean checkImprobable(final Player player, final float weight, final long now, final String tags, final IPlayerData pData) {
        if (!pData.isCheckActive(type, player)) {
            return false;
        }
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        final CombinedConfig cc = pData.getGenericInstance(CombinedConfig.class);
        data.improbableCount.add(now, weight);
        /** Score of the first 3 seconds (a bucket covers 3 secs)*/
        final float shortTerm = data.improbableCount.bucketScore(0);
        double violation = 0.0;
        boolean violated = false;
        if (shortTerm * 0.8f > cc.improbableLevel / 20.0) {
            /** Lag factor for the first window lag (3 seconds) */
            final float lagFactor = pData.getCurrentWorldData().shouldAdjustToLag(type) ? TickTask.getLag(data.improbableCount.bucketDuration(), true) : 1f;
            // Re-check with lag adaptation.
            if (shortTerm / lagFactor > cc.improbableLevel / 20.0) {
                violation += shortTerm * 2D / lagFactor;
                violated = true;
            }
        }
        /** Sum score of all buckets, no weight */
        final double fullTerm = data.improbableCount.score(1.0f);
        if (fullTerm > cc.improbableLevel) {
            /** Full window lag factor, 1 minute */
            final float lagFactor = pData.getCurrentWorldData().shouldAdjustToLag(type) ? TickTask.getLag(data.improbableCount.bucketDuration() * data.improbableCount.numberOfBuckets(), true) : 1f;
            // Re-check with lag adaptation.
            if (fullTerm / lagFactor > cc.improbableLevel) {
                violation += fullTerm / lagFactor;
                violated = true;
            }
        }
        boolean cancel = false;
        if (violated) {
            // Execute actions
            data.improbableVL += violation / 10.0;
            final ViolationData vd = new ViolationData(this, player, data.improbableVL, violation, cc.improbableActions);
            if (tags != null && !tags.isEmpty()) vd.setParameter(ParameterName.TAGS, tags);
            cancel = executeActions(vd).willCancel();
        }
        else data.improbableVL *= 0.8;
        return cancel;
    }

    @Override
    public void onDisable() {
        instance = null;
    }
}
