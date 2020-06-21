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

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.CheckListener;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.blockinteract.BlockInteractData;
import fr.neatmonster.nocheatplus.checks.blockinteract.BlockInteractListener;
import fr.neatmonster.nocheatplus.checks.inventory.Items;
import fr.neatmonster.nocheatplus.checks.moving.util.MovingUtil;
import fr.neatmonster.nocheatplus.checks.net.FlyingQueueHandle;
import fr.neatmonster.nocheatplus.checks.net.model.DataPacketFlying;
import fr.neatmonster.nocheatplus.compat.AlmostBoolean;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.components.NoCheatPlusAPI;
import fr.neatmonster.nocheatplus.components.data.ICheckData;
import fr.neatmonster.nocheatplus.components.data.IData;
import fr.neatmonster.nocheatplus.components.registry.factory.IFactoryOne;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.players.PlayerFactoryArgument;
import fr.neatmonster.nocheatplus.stats.Counters;
import fr.neatmonster.nocheatplus.utilities.TickTask;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.worlds.WorldFactoryArgument;

/**
 * Central location to listen to events that are relevant for the block break checks.
 * 
 * @see BlockBreakEvent
 */
public class BlockBreakListener extends CheckListener {

    /** The direction check. */
    private final Direction direction = addCheck(new Direction());

    /** The fast break check (per block breaking speed). */
    private final FastBreak fastBreak = addCheck(new FastBreak());

    /** The frequency check (number of blocks broken) */
    private final Frequency frequency = addCheck(new Frequency());

    /** The no swing check. */
    private final NoSwing   noSwing   = addCheck(new NoSwing());

    /** The reach check. */
    private final Reach     reach     = addCheck(new Reach());

    /** The wrong block check. */
    private final WrongBlock wrongBlock = addCheck(new WrongBlock());

    private AlmostBoolean isInstaBreak = AlmostBoolean.NO;

    private final Counters counters = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(Counters.class);
    private final int idCancelDIllegalItem = counters.registerKey("illegalitem");

    /** For temporary use: LocUtil.clone before passing deeply, call setWorld(null) after use. */
    private final Location useLoc = new Location(null, 0, 0, 0);

    @SuppressWarnings("unchecked")
    public BlockBreakListener(){
        super(CheckType.BLOCKBREAK);
        final NoCheatPlusAPI api = NCPAPIProvider.getNoCheatPlusAPI();
        // Register config and data.
        api.register(api.newRegistrationContext()
                // BlockBreakConfig
                .registerConfigWorld(BlockBreakConfig.class)
                .factory(new IFactoryOne<WorldFactoryArgument, BlockBreakConfig>() {
                    @Override
                    public BlockBreakConfig getNewInstance(WorldFactoryArgument arg) {
                        return new BlockBreakConfig(arg.worldData);
                    }
                })
                .registerConfigTypesPlayer(CheckType.BLOCKBREAK, true)
                .context() //
                // BlockBreakData
                .registerDataPlayer(BlockBreakData.class)
                .factory(new IFactoryOne<PlayerFactoryArgument, BlockBreakData>() {
                    @Override
                    public BlockBreakData getNewInstance(
                            PlayerFactoryArgument arg) {
                        return new BlockBreakData(
                                arg.playerData.getGenericInstance(BlockBreakConfig.class));
                    }
                })
                // (Complete data removal for now.)
                .addToGroups(CheckType.BLOCKBREAK, true, IData.class, ICheckData.class)
                .context() //
                );
    }

    /**
     * We listen to BlockBreak events for obvious reasons.
     * 
     * @param event
     *            the event
     */
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onBlockBreak(final BlockBreakEvent event) {
        final long now = System.currentTimeMillis();
        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);

        if (!pData.isCheckActive(CheckType.BLOCKBREAK, player)) return;

        // Illegal enchantments hotfix check.
        // TODO: Legacy / encapsulate fully there.
        if (Items.checkIllegalEnchantmentsAllHands(player, pData)) {
            event.setCancelled(true);
            counters.addPrimaryThread(idCancelDIllegalItem, 1);
        }
        else if (MovingUtil.hasScheduledPlayerSetBack(player)) {
            event.setCancelled(true);
        }

        // Cancelled events only leads to resetting insta break.
        if (event.isCancelled()) {
            isInstaBreak = AlmostBoolean.NO;
            return;
        }

        // TODO: maybe invalidate instaBreak on some occasions.


        final Block block = event.getBlock();
        boolean cancelled = false;
        if (block.getType().toString().equals("SCAFFOLDING")) return;
        // Do the actual checks, if still needed. It's a good idea to make computationally cheap checks first, because
        // it may save us from doing the computationally expensive checks.

        final BlockBreakConfig cc = pData.getGenericInstance(BlockBreakConfig.class);
        final BlockBreakData data = pData.getGenericInstance(BlockBreakData.class);
        final BlockInteractData bdata = pData.getGenericInstance(BlockInteractData.class);
        /*
         * Re-check if this is a block interacted with before. With instantly
         * broken blocks, this may be off by one orthogonally.
         */
        final int tick = TickTask.getTick();
        final boolean isInteractBlock = !bdata.getLastIsCancelled() && bdata.matchesLastBlock(tick, block);
        int skippedRedundantChecks = 0;


        final GameMode gameMode = player.getGameMode();

        // Has the player broken a block that was not damaged before?
        final boolean wrongBlockEnabled = wrongBlock.isEnabled(player, pData);
        if (wrongBlockEnabled && wrongBlock.check(player, block, cc, data, pData, isInstaBreak)) {
            cancelled = true;
        }

        // Has the player broken more blocks per second than allowed?
        if (!cancelled && frequency.isEnabled(player, pData) 
                && frequency.check(player, tick, cc, data, pData)) {
            cancelled = true;
        }

        // Has the player broken blocks faster than possible?
        if (!cancelled && gameMode != GameMode.CREATIVE
                && fastBreak.isEnabled(player, pData) 
                && fastBreak.check(player, block, isInstaBreak, cc, data, pData)) {
            cancelled = true;
        }

        // Did the arm of the player move before breaking this block?
        if (!cancelled && noSwing.isEnabled(player, pData) 
                && noSwing.check(player, data, pData)) {
            cancelled = true;
        }

        final FlyingQueueHandle flyingHandle;
        final boolean reachEnabled = reach.isEnabled(player, pData);
        final boolean directionEnabled = direction.isEnabled(player, pData);
        if (reachEnabled || directionEnabled) {
            flyingHandle = new FlyingQueueHandle(pData);
            final Location loc = player.getLocation(useLoc);
            final double eyeHeight = MovingUtil.getEyeHeight(player);
            // Is the block really in reach distance?
            if (!cancelled) {
                if (isInteractBlock && bdata.isPassedCheck(CheckType.BLOCKINTERACT_REACH)) {
                    skippedRedundantChecks ++;
                }
                else if (reachEnabled && reach.check(player, eyeHeight, block, data, cc)) {
                    cancelled = true;
                }
            }

            // Did the player look at the block at all?
            // TODO: Skip if checks were run on this block (all sorts of hashes/conditions).
            if (!cancelled) {
                if (isInteractBlock && (bdata.isPassedCheck(CheckType.BLOCKINTERACT_DIRECTION)
                        || bdata.isPassedCheck(CheckType.BLOCKINTERACT_VISIBLE))) {
                    skippedRedundantChecks ++;
                }
                else if (directionEnabled && direction.check(player, loc, eyeHeight, block, null,
                        flyingHandle, data, cc, pData)) {
                    cancelled = true;
                }
            }
            useLoc.setWorld(null);
        }
        else {
            flyingHandle = null;
        }

        // Destroying liquid blocks.
        if (!cancelled && BlockProperties.isLiquid(block.getType()) && !BlockProperties.isNewLiq(block.getType())
                && !pData.hasPermission(Permissions.BLOCKBREAK_BREAK_LIQUID, player) 
                && !NCPExemptionManager.isExempted(player, CheckType.BLOCKBREAK_BREAK)){
            cancelled = true;
        }

        // On cancel...
        if (cancelled) {
            event.setCancelled(cancelled);
            // Reset damage position:
            // TODO: Review this (!), check if set at all !?
            data.clickedX = block.getX();
            data.clickedY = block.getY();
            data.clickedZ = block.getZ();
        }
        else {
            // Invalidate last damage position:
            //        	data.clickedX = Integer.MAX_VALUE;
            // Debug log (only if not cancelled, to avoid spam).
            if (pData.isDebugActive(CheckType.BLOCKBREAK)) {
                debugBlockBreakResult(player, block, skippedRedundantChecks, 
                        flyingHandle, pData);
            }
        }

        if (isInstaBreak.decideOptimistically()) {
            data.wasInstaBreak = now;
        }
        else {
            data.wasInstaBreak = 0;
        }

        // Adjust data.
        data.fastBreakBreakTime = now;
        //        data.fastBreakfirstDamage = now;
        isInstaBreak = AlmostBoolean.NO;
    }

    private void debugBlockBreakResult(final Player player, final Block block, final int skippedRedundantChecks, 
            final FlyingQueueHandle flyingHandle, final IPlayerData pData) {
        debug(player, "Block break(" + block.getType() + "): " + block.getX() + ", " + block.getY() + ", " + block.getZ());
        BlockInteractListener.debugBlockVSBlockInteract(player, checkType, block, "onBlockBreak", 
                Action.LEFT_CLICK_BLOCK, pData);
        if (skippedRedundantChecks > 0) {
            debug(player, "Skipped redundant checks: " + skippedRedundantChecks);
        }
        if (flyingHandle != null && flyingHandle.isFlyingQueueFetched()) {
            final int flyingIndex = flyingHandle.getFirstIndexWithContentIfFetched();
            final DataPacketFlying packet = flyingHandle.getIfFetched(flyingIndex);
            if (packet != null) {
                debug(player, "Flying packet queue used at index " + flyingIndex + ": pitch=" + packet.getPitch() + ",yaw=" + packet.getYaw());
                return;
            }
        }
    }

    /**
     * We listen to PlayerAnimation events because it is (currently) equivalent to "player swings arm" and we want to
     * check if they did that between block breaks.
     * 
     * @param event
     *            the event
     */
    @EventHandler(
            priority = EventPriority.MONITOR)
    public void onPlayerAnimation(final PlayerAnimationEvent event) {
        // Just set a flag to true when the arm was swung.
        // debug(player, "Animation");
        DataManager.getPlayerData(event.getPlayer()).getGenericInstance(BlockBreakData.class).noSwingArmSwung = true;
    }

    /**
     * We listen to BlockInteract events to be (at least in many cases) able to distinguish between block break events
     * that were triggered by players actually digging and events that were artificially created by plugins.
     * 
     * @param event
     *            the event
     */
    @EventHandler(
            ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        // debug(player, "Interact("+event.isCancelled()+"): " + event.getClickedBlock());
        // The following is to set the "first damage time" for a block.

        // Return if it is not left clicking a block. 
        // (Allows right click to be ignored.)

        if (!DataManager.getPlayerData(event.getPlayer()).isCheckActive(CheckType.BLOCKBREAK, event.getPlayer())) return;

        isInstaBreak = AlmostBoolean.NO;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        checkBlockDamage(event.getPlayer(), event.getClickedBlock(), event);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onBlockDamageLowest(final BlockDamageEvent event) {
        /*
         * TODO: Add a check type BLOCKDAMAGE_CONFIRM (no permission):
         * Cancel if the block doesn't match (MC 1.11.2, other ...)?
         */
        if (MovingUtil.hasScheduledPlayerSetBack(event.getPlayer())) {
            event.setCancelled(true);
        }
        else if (event.getInstaBreak()) {
            // Indicate that this might have been set by CB/MC.
            // TODO: Set in BlockInteractListener !!
            isInstaBreak = AlmostBoolean.MAYBE;
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    public void onBlockDamage(final BlockDamageEvent event) {

        if (!DataManager.getPlayerData(event.getPlayer()).isCheckActive(CheckType.BLOCKBREAK, event.getPlayer())) return;

        if (!event.isCancelled() && event.getInstaBreak()) {
            // Keep MAYBE.
            if (isInstaBreak != AlmostBoolean.MAYBE) {
                isInstaBreak = AlmostBoolean.YES;
            }
        }
        else {
            isInstaBreak = AlmostBoolean.NO;
        }
        checkBlockDamage(event.getPlayer(), event.getBlock(), event);
    }

    private void checkBlockDamage(final Player player, final Block block, final Cancellable event){
        final long now = System.currentTimeMillis();
        final IPlayerData pData = DataManager.getPlayerData(player);
        final BlockBreakData data = pData.getGenericInstance(BlockBreakData.class);

        //        if (event.isCancelled()){
        //        	// Reset the time, to avoid certain kinds of cheating. => WHICH ?
        //        	data.fastBreakfirstDamage = now;
        //        	data.clickedX = Integer.MAX_VALUE; // Should be enough to reset that one.
        //        	return;
        //        }

        // Do not care about null blocks.
        if (block == null) {
            return;
        }

        final int tick = TickTask.getTick();
        // Skip if already set to the same block without breaking within one tick difference.
        final ItemStack stack = Bridge1_9.getItemInMainHand(player);
        final Material tool = stack == null ? null: stack.getType();

        if (data.toolChanged(tool)) {
            // Update.
        } else if (tick < data.clickedTick || now < data.fastBreakfirstDamage || now < data.fastBreakBreakTime) {
            // Time/tick ran backwards: Update.
            // Tick running backwards should not happen in the main thread unless for reload. A plugin could reset it (not intended).
        } else if (data.fastBreakBreakTime < data.fastBreakfirstDamage && data.clickedX == block.getX() &&  data.clickedZ == block.getZ() &&  data.clickedY == block.getY()){
            // Preserve first damage time.
            if (tick - data.clickedTick <= 1 ) {
                return;
            }
        }
        // (Always set, the interact event only fires once: the first time.)
        // Only record first damage:
        data.setClickedBlock(block, tick, now, tool);
        // Compare with BlockInteract data (debug first).
        if (pData.isDebugActive(CheckType.BLOCKBREAK)) {
            BlockInteractListener.debugBlockVSBlockInteract(player, this.checkType, 
                    block, "checkBlockDamage", Action.LEFT_CLICK_BLOCK, pData);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    public void onItemHeld(final PlayerItemHeldEvent event) {

        if (!DataManager.getPlayerData(event.getPlayer()).isCheckActive(CheckType.BLOCKBREAK, event.getPlayer())) return;

        // Reset clicked block.
        // TODO: Not for 1.5.2 and before?
        final Player player = event.getPlayer();
        final BlockBreakData data = DataManager.getPlayerData(player).getGenericInstance(BlockBreakData.class);
        if (data.toolChanged(player.getInventory().getItem(event.getNewSlot()))) {
            data.resetClickedBlock();
        }
    }

}