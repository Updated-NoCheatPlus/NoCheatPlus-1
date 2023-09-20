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

import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.CheckListener;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.combined.Combined;
import fr.neatmonster.nocheatplus.checks.combined.Improbable;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.location.tracking.LocationTrace;
import fr.neatmonster.nocheatplus.checks.moving.location.tracking.LocationTrace.ITraceEntry;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveInfo;
import fr.neatmonster.nocheatplus.checks.moving.player.UnusedVelocity;
import fr.neatmonster.nocheatplus.checks.moving.velocity.VelocityFlags;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.BridgeEnchant;
import fr.neatmonster.nocheatplus.compat.BridgeHealth;
import fr.neatmonster.nocheatplus.compat.IBridgeCrossPlugin;
import fr.neatmonster.nocheatplus.components.NoCheatPlusAPI;
import fr.neatmonster.nocheatplus.components.data.ICheckData;
import fr.neatmonster.nocheatplus.components.data.IData;
import fr.neatmonster.nocheatplus.components.registry.event.IGenericInstanceHandle;
import fr.neatmonster.nocheatplus.components.registry.factory.IFactoryOne;
import fr.neatmonster.nocheatplus.components.registry.feature.JoinLeaveListener;
import fr.neatmonster.nocheatplus.penalties.DefaultPenaltyList;
import fr.neatmonster.nocheatplus.penalties.IPenaltyList;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.players.PlayerFactoryArgument;
import fr.neatmonster.nocheatplus.stats.Counters;
import fr.neatmonster.nocheatplus.utilities.TickTask;
import fr.neatmonster.nocheatplus.utilities.build.BuildParameters;
import fr.neatmonster.nocheatplus.utilities.location.LocUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.moving.AuxMoving;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;
import fr.neatmonster.nocheatplus.worlds.WorldFactoryArgument;

/**
 * Central location to listen to events that are relevant for the fight checks.<br>
 * This listener is registered after the CombinedListener.
 * 
 * @see FightEvent
 */
public class FightListener extends CheckListener implements JoinLeaveListener{

    /** The angle check. */
    private final Angle angle = addCheck(new Angle());

    /** The critical check. */
    private final Critical critical = addCheck(new Critical());

    /** The direction check. */
    private final Direction direction = addCheck(new Direction());

    /** Faster health regeneration check. */
    private final FastHeal fastHeal = addCheck(new FastHeal());

    /** The god mode check. */
    private final GodMode godMode = addCheck(new GodMode());

    /** The impossible hit check */
    private final ImpossibleHit impossibleHit = addCheck(new ImpossibleHit());

    /** The no swing check. */
    private final NoSwing noSwing = addCheck(new NoSwing());

    /** The reach check. */
    private final Reach  reach = addCheck(new Reach());

    /** The self hit check */
    private final SelfHit selfHit = addCheck(new SelfHit());

    /** For temporary use: LocUtil.clone before passing deeply, call setWorld(null) after use. */
    private final Location useLoc1 = new Location(null, 0, 0, 0);

    /** For temporary use: LocUtil.clone before passing deeply, call setWorld(null) after use. */
    private final Location useLoc2 = new Location(null, 0, 0, 0);

    /** Auxiliary utilities for moving */
    private final AuxMoving auxMoving = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(AuxMoving.class);
    
    /* Debug */
    private final Counters counters = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(Counters.class);

    /* Debug */
    private final int idCancelDead = counters.registerKey("cancel.dead");

    // Assume it to stay the same all time.
    private final IGenericInstanceHandle<IBridgeCrossPlugin> crossPlugin = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstanceHandle(IBridgeCrossPlugin.class);

    @SuppressWarnings("unchecked")
    public FightListener() {
        super(CheckType.FIGHT);
        final NoCheatPlusAPI api = NCPAPIProvider.getNoCheatPlusAPI();
        api.register(api.newRegistrationContext()
                // FightConfig
                .registerConfigWorld(FightConfig.class)
                .factory(new IFactoryOne<WorldFactoryArgument, FightConfig>() {
                    @Override
                    public FightConfig getNewInstance(WorldFactoryArgument arg) {
                        return new FightConfig(arg.worldData);
                    }
                })
                .registerConfigTypesPlayer()
                .context() //
                // FightData
                .registerDataPlayer(FightData.class)
                .factory(new IFactoryOne<PlayerFactoryArgument, FightData>() {
                    @Override
                    public FightData getNewInstance(PlayerFactoryArgument arg) {
                        return new FightData(arg.playerData.getGenericInstance(FightConfig.class));
                    }
                })
                .addToGroups(CheckType.FIGHT, false, IData.class, ICheckData.class)
                .removeSubCheckData(CheckType.FIGHT, true)
                .context() //
                );
    }

    /**
     * A player attacked something with DamageCause ENTITY_ATTACK.
     * 
     * @param player
     *            The attacking player.
     * @param damaged
     * @param originalDamage
     *            Damage before applying modifiers.
     * @param finalDamage
     *            Damage after applying modifiers.
     * @param tick
     * @param data
     * @return True, if the hit needs to be canceled.
     */
    private boolean handleNormalDamage(final Player player, final boolean attackerIsFake,
                                       final Entity damaged, final boolean damagedIsFake,
                                       final double originalDamage, final double finalDamage, 
                                       final int tick, final FightData data, final IPlayerData pData,
                                       final IPenaltyList penaltyList) {

        final FightConfig cc = pData.getGenericInstance(FightConfig.class);
        final MovingConfig mCc = pData.getGenericInstance(MovingConfig.class);
        final MovingData mData = pData.getGenericInstance(MovingData.class);
        /** Whether a check has requested to cancel the event */
        boolean cancelled = false;
        final boolean debug = pData.isDebugActive(checkType);
        final String worldName = player.getWorld().getName();
        final long now = System.currentTimeMillis();
        final boolean worldChanged = !worldName.equals(data.lastWorld);
        /** Attacker's location */
        final Location loc =  player.getLocation(useLoc1);
        /** Damaged entity's location */
        final Location damagedLoc = damaged.getLocation(useLoc2);
        final double targetMove;
        final int tickAge;
        /** Milliseconds ticks actually took */
        final long msAge; 
        /** Blocks per second */
        final double normalizedMove; 

        // TODO: relative distance (player - target)!
        // TODO: Use trace for this ?
        if (data.lastAttackedX == Double.MAX_VALUE || tick < data.lastAttackTick || worldChanged || tick - data.lastAttackTick > 20) {
            // TODO: 20 ?
            tickAge = 0;
            targetMove = 0.0;
            normalizedMove = 0.0;
            msAge = 0;
        }
        else {
            tickAge = tick - data.lastAttackTick;
            // TODO: Maybe use 3d distance if dy(normalized) is too big. 
            targetMove = TrigUtil.distance(data.lastAttackedX, data.lastAttackedZ, damagedLoc.getX(), damagedLoc.getZ());
            msAge = (long) (50f * TickTask.getLag(50L * tickAge, true) * (float) tickAge);
            normalizedMove = msAge == 0 ? targetMove : targetMove * Math.min(20.0, 1000.0 / (double) msAge);
        }
        // TODO: calculate factor for dists: ticks * 50 * lag
        // TODO: dist < width => skip some checks (direction, ..)

        final LocationTrace damagedTrace;
        final Player damagedPlayer;
        if (damaged instanceof Player) {

            damagedPlayer = (Player) damaged;
            // Log.
            if (debug && DataManager.getPlayerData(damagedPlayer).hasPermission(Permissions.ADMINISTRATION_DEBUG, damagedPlayer)) {
                damagedPlayer.sendMessage("Attacked by " + player.getName() + ": inv=" + mcAccess.getHandle().getInvulnerableTicks(damagedPlayer) + " ndt=" + damagedPlayer.getNoDamageTicks());
            }
            // Check for self hit exploits (mind that projectiles are excluded from this.)
            if (selfHit.isEnabled(player, pData) && selfHit.check(player, damagedPlayer, data, cc)) {
                cancelled = true;
            }
            // Get+update the damaged players.
            // TODO: Problem with NPCs: data stays (not a big problem).
            // (This is done even if the event has already been cancelled, to keep track, if the player is on a horse.)
            damagedTrace = DataManager.getPlayerData(damagedPlayer).getGenericInstance(MovingData.class)
                           .updateTrace(damagedPlayer, damagedLoc, tick, damagedIsFake ? null : mcAccess.getHandle()); //.getTrace(damagedPlayer);
        }
        else {
            damagedPlayer = null; // TODO: This is a temporary workaround.
            // Use a fake trace.
            // TODO: Provide for entities too? E.g. one per player, or a fully fledged bookkeeping thing (EntityData).
            //final MovingConfig mcc = MovingConfig.getConfig(damagedLoc.getWorld().getName());
            damagedTrace = null; //new LocationTrace(mcc.traceSize, mcc.traceMergeDist);
            //damagedTrace.addEntry(tick, damagedLoc.getX(), damagedLoc.getY(), damagedLoc.getZ());
        }

        // Log generic properties of this attack.
        if (debug) {
            debug(player, "Attacks " + (damagedPlayer == null ? ("entity " + damaged.getType()) : ("player" + damagedPlayer.getName())) + " damage=" + (finalDamage == originalDamage ? finalDamage : (originalDamage + "/" + finalDamage)));
        }

        // Can't fight dead.
        if (cc.cancelDead) {
            if (damaged.isDead()) {
                cancelled = true;
            }
            // Only allow damaging others if taken damage this tick.
            if (player.isDead() && data.damageTakenByEntityTick != TickTask.getTick()) {
                cancelled = true;
            }
        }

        // LEGACY: 1.9: sweep attack.
        if (BridgeHealth.DAMAGE_SWEEP == null) {
            // TODO: Account for charge/meter thing?
            final int locHashCode = LocUtil.hashCode(loc);
            if (originalDamage == 1.0) {
                // Might be a sweep attack.
                if (tick == data.sweepTick && locHashCode == data.sweepLocationHashCode) {
                    // TODO: Might limit the amount of 'too far off' sweep hits, possibly silent cancel for low frequency.
                    // Could further guard by checking equality of loc to last location.
                    if (debug) {
                        debug(player, "(Assume sweep attack follow up damage.)");
                    }
                    return cancelled;
                }
            }
            else {
                // TODO: More side conditions for a sweep attack.
                data.sweepTick = tick;
                data.sweepLocationHashCode = locHashCode;
            }
        }

        // LEGACY: thorns.
        if (BridgeHealth.DAMAGE_THORNS == null && originalDamage <= 4.0 && tick == data.damageTakenByEntityTick 
            && data.thornsId != Integer.MIN_VALUE && data.thornsId == damaged.getEntityId()) {
            // Don't handle further, but do respect selfhit/canceldead.
            // TODO: Remove soon, at least version-dependent.
            data.thornsId = Integer.MIN_VALUE;
            return cancelled;
        }
        else data.thornsId = Integer.MIN_VALUE;

        // Run through the main checks.
        // Illegal critical hits
        if (!cancelled && critical.isEnabled(player, pData) 
            && critical.check(player, loc, data, cc, pData, penaltyList)) {
            cancelled = true;
        }
        
        // Arm swing check.
        if (!cancelled && mData.timeRiptiding + 3000 < now 
            && noSwing.isEnabled(player, pData) 
            && noSwing.check(player, data, cc)) {
            cancelled = true;
        }
        
        // Misc. illegal hits check
        if (!cancelled && impossibleHit.isEnabled(player, pData)) {
            if (impossibleHit.check(player, data, cc, pData, mCc.survivalFlyResetItem && mcAccess.getHandle().resetActiveItem(player))) {
                cancelled = true;

                // Still feed the Improbable
                if (cc.impossibleHitImprobableWeight > 0.0f) {
                    Improbable.feed(player, cc.impossibleHitImprobableWeight, System.currentTimeMillis());
                }
            }
        }
        
        // Checks that use the LocationTrace instance of the attacked entity/player.
        // TODO: To be replaced by Fight.HitBox
        if (!cancelled) {
            final boolean reachEnabled = reach.isEnabled(player, pData);
            final boolean directionEnabled = direction.isEnabled(player, pData) && mData.timeRiptiding + 3000 < now;
            if (reachEnabled || directionEnabled) {
                if (damagedTrace != null) {
                    cancelled = locationTraceChecks(player, loc, data, cc, pData, 
                                                    damaged, damagedIsFake, damagedLoc, damagedTrace, tick, now, debug,
                                                    reachEnabled, directionEnabled);
                }
                // Still use the classic methods for non-players. 
                else {
                    if (reachEnabled && reach.check(player, loc, damaged, damagedIsFake, damagedLoc, data, cc, pData)) {
                        cancelled = true;
                    }
                    if (directionEnabled && direction.check(player, loc, damaged, damagedIsFake, damagedLoc, data, cc)) {
                        cancelled = true;
                    }
                }
            }
        }

        // Check angle with allowed window.
        // The "fast turning" checks are checked in any case because they accumulate data.
        // Improbable yaw changing: Moving events might be missing up to a ten degrees change.
        // TODO: Actual angle needs to be related to the best matching trace element(s) (loop checks).
        // TODO: Work into this somehow attacking the same aim and/or similar aim position (not cancel then).
        // TODO: Revise, use own trace.
        // TODO: Should we drop this check? Reasons being:
        //       1) It doesn't do much at all against killauras or even multiauras;
        //       2) Throws a lot of false positives with mob grinders and with ping-poing hitting players;
        //       3) Switchspeed and yaw changes are already monitored by Yawrate... (Redundancy);
        if (angle.isEnabled(player, pData)) {
            if (Combined.checkYawRate(player, loc.getYaw(), now, worldName, pData.isCheckActive(CheckType.COMBINED_YAWRATE, player), pData)) {
                // (Check or just feed).
                cancelled = true;
            }
            // Angle check.
            if (!cancelled && angle.check(player, loc, damaged, worldChanged, data, cc, pData)) {
                if (!cancelled && debug) {
                    debug(player, "FIGHT_ANGLE cancel without yawrate cancel.");
                }
                cancelled = true;
            }
        }

        // Set values.
        data.lastWorld = worldName;
        data.lastAttackTick = tick;
        data.lastAttackedX = damagedLoc.getX();
        data.lastAttackedY = damagedLoc.getY();
        data.lastAttackedZ = damagedLoc.getZ();
        // data.lastAttackedDist = targetDist;

        // Generic attacking penalty.
        // (Cancel after sprinting hacks, because of potential fp).
        if (!cancelled && data.attackPenalty.isPenalty(now)) {
            cancelled = true;
            if (debug) {
                debug(player, "~ attack penalty.");
            }
        }

        // Cleanup.
        useLoc1.setWorld(null);
        useLoc2.setWorld(null);
        return cancelled;
    }

    /**
     * Quick split-off: Checks using a location trace.
     * @param player
     * @param loc
     * @param data
     * @param cc
     * @param damaged
     * @param damagedPlayer
     * @param damagedLoc
     * @param damagedTrace
     * @param tick
     * @param reachEnabled
     * @param directionEnabled
     * @return If to cancel (true) or not (false).
     */
    private boolean locationTraceChecks(final Player player, final Location loc, 
                                        final FightData data, final FightConfig cc, final IPlayerData pData,
                                        final Entity damaged, final boolean damagedIsFake,
                                        final Location damagedLoc, LocationTrace damagedTrace, 
                                        final long tick, final long now, final boolean debug,
                                        final boolean reachEnabled, final boolean directionEnabled) {

        // TODO: Order / splitting off generic stuff.
        /*
         * TODO: Abstract: interface with common setup/loop/post routine, only
         * pass the ACTIVATED checks on to here (e.g. IFightLoopCheck...
         * loopChecks). Support an arbitrary number of loop checks, special
         * behavior -> interface and/or order within loopChecks.
         */
        // (Might pass generic context to factories, for shared + heavy properties.)
        final ReachContext reachContext = reachEnabled ? reach.getContext(player, loc, damaged, damagedLoc, data, cc) : null;
        final DirectionContext directionContext = directionEnabled ? direction.getContext(player, loc, damaged, damagedIsFake, damagedLoc, data, cc) : null;
        final long traceOldest = tick - cc.loopMaxLatencyTicks; // TODO: Set by latency-window.
        // TODO: Iterating direction, which, static/dynamic choice.
        final Iterator<ITraceEntry> traceIt = damagedTrace.maxAgeIterator(traceOldest);
        boolean cancelled = false;
        /** No tick with all checks passed */
        boolean violation = true; 
        /** Passed individually for some tick */
        boolean reachPassed = !reachEnabled; 
        /** Passed individually for some tick */
        boolean directionPassed = !directionEnabled; 
        // TODO: Maintain a latency estimate + max diff and invalidate completely (i.e. iterate from latest NEXT time)], or just max latency.
        // TODO: Consider a max-distance to "now", for fast invalidation.
        long latencyEstimate = -1;
        ITraceEntry successEntry = null;

        while (traceIt.hasNext()) {
            final ITraceEntry entry = traceIt.next();
            // Simplistic just check both until end or hit.
            // TODO: Other default distances/tolerances.
            boolean thisPassed = true;
            if (reachEnabled) {
                if (reach.loopCheck(player, loc, damaged, entry, reachContext, data, cc)) {
                    thisPassed = false;
                }
                else {
                    reachPassed = true;
                }
            }
            // TODO: Efficiency: don't check at all, if strict and !thisPassed.
            if (directionEnabled && (reachPassed || !directionPassed)) {
                if (direction.loopCheck(player, loc, damaged, entry, directionContext, data, cc)) {
                    thisPassed = false;
                }
                else {
                    directionPassed = true;
                }
            }
            if (thisPassed) {
                violation = false;
                latencyEstimate = now - entry.getTime();
                successEntry = entry;
                break;
            }
        }

        // TODO: How to treat mixed state: violation && reachPassed && directionPassed [current: use min violation // thinkable: silent cancel, if actions have cancel (!)]
        // TODO: Adapt according to strictness settings?
        // TODO: violation vs. reachPassed + directionPassed (current: fail one = fail all).
        if (reachEnabled) {
            // TODO: Might ignore if already cancelled by mixed/silent cancel.
            if (reach.loopFinish(player, loc, damaged, reachContext, successEntry, violation, data, cc, pData)) {
                cancelled = true;
            }
        }
        if (directionEnabled) {
            // TODO: Might ignore if already cancelled.
            if (direction.loopFinish(player, loc, damaged, directionContext, violation, data, cc)) {
                cancelled = true;
            }
        }

        // TODO: Log exact state, probably record min/max latency (individually).
        if (debug && latencyEstimate >= 0) {
            debug(player, "Latency estimate: " + latencyEstimate + " ms."); // FCFS rather, at present.
        }
        return cancelled;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamage(final EntityDamageEvent event) {
        final Entity damaged = event.getEntity();
        final Player damagedPlayer = damaged instanceof Player ? (Player) damaged : null;
        final FightData damagedData;
        final boolean damagedIsDead = damaged.isDead();
        final boolean damagedIsFake = !crossPlugin.getHandle().isNativeEntity(damaged);
        IPenaltyList penaltyList = null;

        if (damagedPlayer != null) {
            
            final IPlayerData damagedPData = DataManager.getPlayerData(damagedPlayer);
            damagedData = damagedPData.getGenericInstance(FightData.class);
            if (!damagedIsDead) {
                // God mode check.
                // (Do not test the savage.)
                if (damagedPData.isCheckActive(CheckType.FIGHT_GODMODE, damagedPlayer)) {
                    if (penaltyList == null) {
                        penaltyList = new DefaultPenaltyList();
                    }
                    if (godMode.check(damagedPlayer, damagedIsFake, BridgeHealth.getRawDamage(event), damagedData, damagedPData)) {
                        // It requested to "cancel" the players invulnerability, so set their noDamageTicks to 0.
                        damagedPlayer.setNoDamageTicks(0);
                    }
                }
                // Adjust buffer for fast heal check.
                if (BridgeHealth.getHealth(damagedPlayer) >= BridgeHealth.getMaxHealth(damagedPlayer)) {
                    // TODO: Might use the same FightData instance for GodMode.
                    if (damagedData.fastHealBuffer < 0) {
                        // Reduce negative buffer with each full health.
                        damagedData.fastHealBuffer /= 2;
                    }
                    // Set reference time.
                    damagedData.fastHealRefTime = System.currentTimeMillis();
                }
                // TODO: TEST: Check unused velocity for the damaged player. (Needs more efficient pre condition checks.)

            }
            if (damagedPData.isDebugActive(checkType)) {
                // TODO: Pass result to further checks for reference?
                UnusedVelocity.checkUnusedVelocity(damagedPlayer, CheckType.FIGHT, damagedPData);
            }
        }
        else damagedData = null;

        // Attacking entities.
        if (event instanceof EntityDamageByEntityEvent) {
            if (penaltyList == null) {
                penaltyList = new DefaultPenaltyList();
            }
            onEntityDamageByEntity(damaged, damagedPlayer, damagedIsDead, damagedIsFake, damagedData, (EntityDamageByEntityEvent) event, penaltyList);
        }

        if (penaltyList != null && !penaltyList.isEmpty()) {
            penaltyList.applyAllApplicablePenalties(event, true);
        }

    }

    /**
     * (Not an event listener method: call from EntityDamageEvent handler at
     * EventPriority.LOWEST.)
     * 
     * @param damagedPlayer
     * @param damagedIsDead
     * @param damagedData
     * @param event
     */
    private void onEntityDamageByEntity(final Entity damaged, final Player damagedPlayer, 
                                        final boolean damagedIsDead, final boolean damagedIsFake, 
                                        final FightData damagedData, final EntityDamageByEntityEvent event,
                                        final IPenaltyList penaltyList) {

        final Entity damager = event.getDamager();
        final int tick = TickTask.getTick();
        if (damagedPlayer != null && !damagedIsDead) {
            // TODO: check once more when to set this (!) in terms of order.
            damagedData.damageTakenByEntityTick = tick;
            // Legacy workaround: Before thorns damage cause existed (orchid).
            // TODO: Disable efficiently, if the damage cause exists.
            // TODO: Remove workaround anyway, if the issue only exists on a minor CB version.
            if (BridgeEnchant.hasThorns(damagedPlayer)) {
                // Remember the id of the attacker to allow counter damage.
                damagedData.thornsId = damager.getEntityId();
            }
            else damagedData.thornsId = Integer.MIN_VALUE;
        }

        final DamageCause damageCause = event.getCause();
        final Player player = damager instanceof Player ? (Player) damager : null;
        Player attacker = player;
        // TODO: deobfuscate.
        if (damager instanceof TNTPrimed) {
            final Entity source = ((TNTPrimed) damager).getSource();
            if (source instanceof Player) {
                attacker = (Player) source;
            }
        }

        final FightData attackerData;
        final IPlayerData attackerPData = attacker == null ? null : DataManager.getPlayerData(attacker);
        if (attacker != null) {

            attackerData = attackerPData.getGenericInstance(FightData.class);
            // TODO: TEST: Check unused velocity for the attacker. (Needs more efficient pre condition checks.)
            if (attackerPData.isDebugActive(checkType)) {
                // TODO: Pass result to further checks for reference?
                // TODO: attackerData.debug flag.
                // TODO: Fake players likely have unused velocity, just clear unused?
                UnusedVelocity.checkUnusedVelocity(attacker, CheckType.FIGHT, attackerPData);
            }
            // Workaround for subsequent melee damage eventsfor explosions. TODO: Legacy or not, need a KB.
            if (damageCause == DamageCause.BLOCK_EXPLOSION  || damageCause == DamageCause.ENTITY_EXPLOSION) {
                // NOTE: Pigs don't have data.
                attackerData.lastExplosionEntityId = damaged.getEntityId();
                attackerData.lastExplosionDamageTick = tick;
                return;
            }
        }
        else attackerData = null;
        
        if (player != null) {
            // Actual fight checks.
            if (damageCause == DamageCause.ENTITY_ATTACK) {
                // TODO: Might/should skip the damage comparison, though checking on lowest priority.
                if (damaged.getEntityId() == attackerData.lastExplosionEntityId && tick == attackerData.lastExplosionDamageTick) {
                    attackerData.lastExplosionDamageTick = -1;
                    attackerData.lastExplosionEntityId = Integer.MAX_VALUE;
                }
                // Prevent attacking if a set back is scheduled.
                else if (MovingUtil.hasScheduledPlayerSetBack(player)) {
                    if (attackerPData.isDebugActive(checkType)) {
                        // Use fight data flag for efficiency.
                        debug(attacker, "Prevent melee attack, due to a scheduled set back.");
                    }
                    event.setCancelled(true);
                }
                // Ordinary melee damage handling.
                else if (handleNormalDamage(player, !crossPlugin.getHandle().isNativePlayer(player),
                                            damaged, damagedIsFake, BridgeHealth.getOriginalDamage(event), 
                                            BridgeHealth.getFinalDamage(event), tick, attackerData, 
                                            attackerPData, penaltyList)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageMonitor(final EntityDamageEvent event) {

        final Entity damaged = event.getEntity();
        if (damaged instanceof Player) {
            final Player damagedPlayer = (Player) damaged;
            final IPlayerData damagedPData = DataManager.getPlayerData(damagedPlayer);
            final FightData damagedData = damagedPData.getGenericInstance(FightData.class);
            final int ndt = damagedPlayer.getNoDamageTicks();

            if (damagedData.lastDamageTick == TickTask.getTick() && damagedData.lastNoDamageTicks != ndt) {
                // Plugin compatibility thing.
                damagedData.lastNoDamageTicks = ndt;
            }
            // Knock-back calculation (1.8: events only fire if they would count by ndt).
            switch (event.getCause()) {
                case ENTITY_ATTACK:
                    if (event instanceof EntityDamageByEntityEvent) {
                        final Entity entity = ((EntityDamageByEntityEvent) event).getDamager();
                        if ((entity instanceof Player) && !damagedPlayer.isInsideVehicle() 
                             && damagedPData.getGenericInstance(FightConfig.class).knockBackVelocityPvP) {
                            // TODO: Use the velocity event that is sent anyway and replace x/z if 0 (queue max. values).
                            applyKnockBack((Player) entity, damagedPlayer, damagedData, damagedPData);
                        }
                    }
                default:
                    break;
            }
        }
    }

    /**
     * Knock-back accounting: Add velocity.
     * @param attacker
     * @param damagedPlayer
     * @param damagedData
     */
    private void applyKnockBack(final Player attacker, final Player damagedPlayer, 
                                final FightData damagedData, final IPlayerData pData) {

        final double knockbackLvl = getKnockBackLevel(attacker);
        final MovingData mData = pData.getGenericInstance(MovingData.class);
        final MovingConfig mCC = pData.getGenericInstance(MovingConfig.class);
        final double[] vel2Dvec = calculateVelocity(attacker, damagedPlayer);
        final double xVelocity = vel2Dvec[0];
        final double zVelocity = vel2Dvec[2];
        final double yVelocity = vel2Dvec[1];
        useLoc1.setWorld(null); // Cleanup.
        if (pData.isDebugActive(checkType) || pData.isDebugActive(CheckType.MOVING)) {
            debug(damagedPlayer, "Received knockback level: " + knockbackLvl);
        }
        mData.addVelocity(damagedPlayer, mCC, xVelocity, yVelocity, zVelocity, VelocityFlags.ORIGIN_PVP);
    }

    /**
     * Get the knock-back "level", a player can deal based on sprinting +
     * item(s) in hand. The minimum knock-back level is 1.0 (1 + 1 for sprinting
     * + knock-back level), currently capped at 20. Since detecting relevance of
     * items in main vs. off hand, we use the maximum of both, for now.
     * 
     * @param player
     * @return
     */
    private double getKnockBackLevel(final Player player) {

        double level = 1.0; // 1.0 is the minimum knock-back value.
        // TODO: Get the RELEVANT item (...).
        final ItemStack stack = Bridge1_9.getItemInMainHand(player);
        if (!BlockProperties.isAir(stack)) {
            level = (double) stack.getEnchantmentLevel(Enchantment.KNOCKBACK);
        }
        if (player.isSprinting()) {
            // TODO: Lost sprint?
            level += 1.0;
        }
        // Cap the level to something reasonable. TODO: Config / cap the velocity anyway.
        return Math.min(20.0, level);
    }

    /**
     * Better method to calculate velocity including direction!
     * 
     * @param attacker
     * @param damagedPlayer
     * @return velocityX, velocityY, velocityZ
     */
    private double[] calculateVelocity(final Player attacker, final Player damagedPlayer) {

        final Location aLoc = attacker.getLocation();
        final Location dLoc = damagedPlayer.getLocation();
        final double xDiff = dLoc.getX() - aLoc.getX();
        final double zDiff = dLoc.getZ() - aLoc.getZ();
        final double distance = MathUtil.dist(xDiff, zDiff);
        double xVelocity = 0.0;
        double zVelocity = 0.0;
        int incKnockbackLvl = 0;
        // TODO: Get the RELEVANT item (...).
        final ItemStack stack = Bridge1_9.getItemInMainHand(attacker);
        if (!BlockProperties.isAir(stack)) {
            incKnockbackLvl = stack.getEnchantmentLevel(Enchantment.KNOCKBACK);
        }
        if (attacker.isSprinting()) {
            // TODO: Lost sprint?
            incKnockbackLvl++;
        }
        // Cap the level to something reasonable. TODO: Config / cap the velocity anyway. 
        incKnockbackLvl = Math.min(20, incKnockbackLvl);

        if (MathUtil.dist(xDiff, zDiff) < 1.0E-4D) {
            if (incKnockbackLvl <= 0) {
                incKnockbackLvl = -~0;
            }
            xVelocity = zVelocity = incKnockbackLvl / Math.sqrt(8.0);
            final double yVelocity = incKnockbackLvl > 0 ? 0.465 : 0.365;
            return new double[] {xVelocity, yVelocity, zVelocity};
        } 
        else {
            xVelocity = xDiff / distance * 0.4;
            zVelocity = zDiff / distance * 0.4;
        }

        if (incKnockbackLvl > 0) {
            xVelocity *= 1.0 + 1.25 * incKnockbackLvl;
            zVelocity *= 1.0 + 1.25 * incKnockbackLvl;
            // Still not exact direction since yaw difference between packet and Location#getYaw();
            // with incknockbacklevel = 0, it still the precise direction
            //xVelocity -= TrigUtil.sin(aLoc.getYaw() * Math.PI / 180.0F) * incKnockbackLvl * 0.5F;
            //zVelocity += TrigUtil.cos(aLoc.getYaw() * Math.PI / 180.0F) * incKnockbackLvl * 0.5F;
        }
        final double yVelocity = incKnockbackLvl > 0 ? 0.465 : 0.365;
        return new double[] {xVelocity, yVelocity, zVelocity};
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeathEvent(final EntityDeathEvent event) {
        // Only interested in dying players.
        final Entity entity = event.getEntity();
        if (entity instanceof Player) {
            final Player player = (Player) entity;
            if (godMode.isEnabled(player)) {
                godMode.death(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAnimation(final PlayerAnimationEvent event) {
        final FightData data = DataManager.getGenericInstance(event.getPlayer(), FightData.class);
        data.noSwingCount = Math.max(data.noSwingCount - 1, 0);
        
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityRegainHealthLow(final EntityRegainHealthEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        final Player player = (Player) entity;
        if (player.isDead() && BridgeHealth.getHealth(player) <= 0.0) {
            // Heal after death.
            // TODO: Problematic. At least skip CUSTOM.
            event.setCancelled(true);
            counters.addPrimaryThread(idCancelDead, 1);
            return;
        }
        if (event.getRegainReason() != RegainReason.SATIATED) {
            return;
        }
        // TODO: EATING reason / peaceful difficulty / regen potion - byCaptain SpigotMC
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (pData.isCheckActive(CheckType.FIGHT_FASTHEAL, player) 
            && fastHeal.check(player, pData)) {
            // TODO: Can clients force events with 0-re-gain ?
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityRegainHealth(final EntityRegainHealthEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        final Player player = (Player) entity;
        final FightData data = DataManager.getGenericInstance(player, FightData.class);
        // Adjust god mode data:
        // Remember the time.
        data.regainHealthTime = System.currentTimeMillis();
        // Set god-mode health to maximum.
        // TODO: Mind that health regain might half the ndt.
        final double health = Math.min(BridgeHealth.getHealth(player) + BridgeHealth.getAmount(event), BridgeHealth.getMaxHealth(player));
        data.godModeHealth = Math.max(data.godModeHealth, health);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void entityInteract(PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        final Player player = e.getPlayer();
        final FightData data = DataManager.getGenericInstance(player, FightData.class);
        data.exemptArmSwing = entity != null && entity.getType().name().equals("PARROT");
    }

    @Override
    public void playerJoins(final Player player) {}
    
    @Override
    public void playerLeaves(final Player player) {
        final FightData data = DataManager.getGenericInstance(player, FightData.class);
        data.angleHits.clear();
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    public void onItemHeld(final PlayerItemHeldEvent event) {
        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        final long penalty = pData.getGenericInstance(FightConfig.class).toolChangeAttackPenalty;
        if (penalty > 0) {
            pData.getGenericInstance(FightData.class).attackPenalty.applyPenalty(penalty);
        }
    }
}