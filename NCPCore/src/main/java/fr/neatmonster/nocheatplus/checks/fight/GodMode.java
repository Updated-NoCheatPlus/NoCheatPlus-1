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
package fr.neatmonster.nocheatplus.checks.fight;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.net.NetData;
import fr.neatmonster.nocheatplus.compat.bukkit.BridgeHealth;
import fr.neatmonster.nocheatplus.compat.IBridgeCrossPlugin;
import fr.neatmonster.nocheatplus.compat.SchedulerHelper;
import fr.neatmonster.nocheatplus.components.registry.event.IGenericInstanceHandle;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.CheckUtils;
import fr.neatmonster.nocheatplus.utilities.TickTask;

/**
 * The GodMode check will find out if a player tried to stay invulnerable after being hit or after dying.
 */
public class GodMode extends Check {

    private final IGenericInstanceHandle<IBridgeCrossPlugin> crossPlugin = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstanceHandle(IBridgeCrossPlugin.class);

    /**
     * Instantiates a new god mode check.
     */
    public GodMode() {
        super(CheckType.FIGHT_GODMODE);
    }

    /**
     * "New" style god mode check. Much more sensitive.
     * Likely irrelevant check by now. Subject to removal.<br>
     * Dates back to 2012 when god-mode exploits used to be quite common.
     * Nowadays, such cheats are exceptionally rare and usually the result of a conflict of some sort with plugins, rather than a vanilla bug.
     * 
     * @param player
     * @param playerIsFake
     * @param damage
     * @param data
     * @param pData
     * @return True if to "cancel", to be interpreted as "cancel their invulnerability", thus inflicting/enforcing damage.
     */
    public boolean check(final Player player, final boolean playerIsFake, final double damage, final FightData data, final IPlayerData pData) {

        // TODO: In fact, this check is ineffective in general. Usually, GodMode cheats and the like don't even fire EntityDamageByEntity events
        //       so this check wouldn't even have the chance to pick up the cheat.
        final int tick = TickTask.getTick();
        final int noDamageTicks = Math.max(0, player.getNoDamageTicks());
        final int invulnerabilityTicks = playerIsFake ? 0 : mcAccess.getHandle().getInvulnerableTicks(player);
        /** Return, reduce VL */
        boolean legit = false; 
        /** Set tick/ndt and return */
        boolean set = false;
        /** Reset accumulator counter */
        boolean resetAcc = false; 
        /** Reset all and return */
        boolean resetAll = false;
        /** Check difference to expectation */
        final int dTick = tick - data.lastDamageTick;
        /** Check difference to expectation */
        final int dNDT = data.lastNoDamageTicks - noDamageTicks;
        final int delta = dTick - dNDT;
        final double health = BridgeHealth.getHealth(player);

        if (data.godModeHealth > health) {
            data.godModeHealthDecreaseTick = tick;
            legit = set = resetAcc = true;
        }

        // TODO: Might account for ndt/2 on regain health (!).
        // Invulnerable or inconsistent.
        // TODO: might check as well if NCP has taken over invulnerable ticks of this player.
        if (invulnerabilityTicks != Integer.MAX_VALUE && invulnerabilityTicks > 0 
            || tick < data.lastDamageTick) {
            // (Second criteria is for MCAccessBukkit.)
            legit = set = resetAcc = true;
        }

        // Reset accumulator.
        if (20 + data.godModeAcc < dTick || dTick > 40) {
            legit = resetAcc = set = true;
        }

        // Check if reduced more than expected or new/count down fully.
        // TODO: Mostly workarounds.
        if (delta <= 0  || data.lastNoDamageTicks <= player.getMaximumNoDamageTicks() / 2 
            || dTick > data.lastNoDamageTicks 
            || damage > BridgeHealth.getLastDamage(player)
            || damage == 0.0) {
            // Not resetting acc.
            legit = set = true;
        }
        if (dTick == 1 && noDamageTicks < 19) {
            set = true;
        }
        if (delta == 1) {
            // Ignore these, but keep reference value from before.
            legit = true;
        }

        // Bukkit.getServer().broadcastMessage("God " + player.getName() + " delta=" + delta + " dt=" + dTick + " dndt=" + dNDT + " acc=" + data.godModeAcc + " d=" + damage + " ndt=" + noDamageTicks + " h=" + health + " slag=" + TickTask.getLag(dTick, true));
        // TODO: might check last damage taken as well (really taken with health change)
        // Resetting
        data.godModeHealth = health;
        if (resetAcc || resetAll) {
            data.godModeAcc = 0;
        }
        
        // Reset all.
        if (resetAll) {
            data.lastNoDamageTicks = 0;
            data.lastDamageTick = 0;
            return false;
        }
        // Only set the tick values.
        if (set) {
            data.lastNoDamageTicks = noDamageTicks;
            data.lastDamageTick = tick;
            return false;
        }
        // Decrease VL and return
        if (legit) {
            data.godModeVL *= 0.97;
            return false;
        }

        if (tick < data.godModeHealthDecreaseTick){
            data.godModeHealthDecreaseTick = 0;
        }
        else {
            final int dht = tick - data.godModeHealthDecreaseTick;
            if (dht <= 20) {
                return false; 
            }
        }

        final FightConfig cc = pData.getGenericInstance(FightConfig.class); 
        // Check for client side lag.
        final long now = System.currentTimeMillis();
        final long maxAge = cc.godModeLagMaxAge;
        long keepAlive = Long.MIN_VALUE;
        if (NCPAPIProvider.getNoCheatPlusAPI().hasFeatureTag("checks", "KeepAliveFrequency")) {
            keepAlive = pData.getGenericInstance(NetData.class).lastKeepAliveTime;
        }
        keepAlive = Math.max(keepAlive, CheckUtils.guessKeepAliveTime(player, now, maxAge, pData));

        if (keepAlive != Double.MIN_VALUE && now - keepAlive > cc.godModeLagMinAge && now - keepAlive < maxAge) {
            // Assume lag.
            return false;
        }

        // Violation probably.
        data.godModeAcc += delta;
        boolean cancel = false;
        // TODO: bounds
        if (data.godModeAcc > 2) {
            data.godModeVL += delta;
            cancel = executeActions(player, data.godModeVL, delta, pData.getGenericInstance(FightConfig.class).godModeActions).willCancel();
        }
        // Set tick values.
        data.lastNoDamageTicks = noDamageTicks;
        data.lastDamageTick = tick;
        return cancel;
    }

    /**
     * If a player apparently died, make sure they really die after some time if they didn't already, by setting up a
     * Bukkit task.
     * Again, most likely irrelevant check by now, but you can't be too sure with this game.
     * 
     * @param player
     *            the player
     */
    public void death(final Player player) {
        // First check if the player is really dead (e.g. another plugin could have just fired an artificial event).
        if (BridgeHealth.getHealth(player) <= 0.0 && player.isDead()
            && crossPlugin.getHandle().isNativeEntity(player)) {
            try {
                // Schedule a task to be executed in roughly 1.5 seconds.
                SchedulerHelper.runSyncDelayedTaskForEntity(player, Bukkit.getPluginManager().getPlugin("NoCheatPlus"), (arg) -> {
                    try {
                        // Check again if the player should be dead, and if the game didn't mark them as dead.
                        if (mcAccess.getHandle().shouldBeZombie(player)){
                            // Artificially "kill" them.
                            mcAccess.getHandle().setDead(player, 19);
                        }
                    } 
                    catch (final Exception ignored) {}
                }, null, 30);
            } 
            catch (final Exception ignored) {}
        }
    }
}