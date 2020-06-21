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
package fr.neatmonster.nocheatplus.checks.blockinteract;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.CheckListener;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.combined.CombinedConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.util.MovingUtil;
import fr.neatmonster.nocheatplus.checks.net.FlyingQueueHandle;
import fr.neatmonster.nocheatplus.checks.net.model.DataPacketFlying;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.BridgeHealth;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.components.NoCheatPlusAPI;
import fr.neatmonster.nocheatplus.components.data.ICheckData;
import fr.neatmonster.nocheatplus.components.data.IData;
import fr.neatmonster.nocheatplus.components.registry.factory.IFactoryOne;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.players.PlayerFactoryArgument;
import fr.neatmonster.nocheatplus.stats.Counters;
import fr.neatmonster.nocheatplus.utilities.CheckUtils;
import fr.neatmonster.nocheatplus.utilities.InventoryUtil;
import fr.neatmonster.nocheatplus.utilities.TickTask;
import fr.neatmonster.nocheatplus.utilities.location.LocUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.worlds.WorldFactoryArgument;

/**
 * Central location to listen to events that are relevant for the block interact checks.
 * 
 * @see BlockInteractEvent
 */
public class BlockInteractListener extends CheckListener {

    public static void debugBlockVSBlockInteract(final Player player, final CheckType checkType, 
            final Block block, final String prefix, final Action expectedAction,
            final IPlayerData pData) {
        final BlockInteractData bdata = pData.getGenericInstance(BlockInteractData.class);
        final int manhattan = bdata.manhattanLastBlock(block);
        String msg;
        if (manhattan == Integer.MAX_VALUE) {
            msg =  "no last block set!";
        }
        else {
            msg = manhattan == 0 ? "same as last block." 
                    : ("last block differs, Manhattan: " + manhattan);
            if (bdata.getLastIsCancelled()) {
                msg += " / cancelled";
            }
            if (bdata.getLastAction() != expectedAction) {
                msg += " / action=" + bdata.getLastAction();
            }
        }
        CheckUtils.debug(player, checkType, prefix + " BlockInteract: " + msg);
    }

    // INSTANCE ----

    /** The looking-direction check. */
    private final Direction direction = addCheck(new Direction());

    /** The reach-distance check. */
    private final Reach     reach     = addCheck(new Reach());

    /** Interact with visible blocks. */
    private final Visible visible = addCheck(new Visible());

    /** Speed of interaction. */
    private final Speed speed = addCheck(new Speed());

    /** For temporary use: LocUtil.clone before passing deeply, call setWorld(null) after use. */
    private final Location useLoc = new Location(null, 0, 0, 0);

    private final Counters counters = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(Counters.class);
    private final int idCancelDead = counters.registerKey("cancel.dead");
    private final int idCancelOffline = counters.registerKey("cancel.offline");
    private final int idInteractLookCurrent = counters.registerKey("block.interact.look.current");
    private final int idInteractLookFlyingFirst = counters.registerKey("block.interact.look.flying.first");
    private final int idInteractLookFlyingOther = counters.registerKey("block.interact.look.flying.other");

    @SuppressWarnings("unchecked")
    public BlockInteractListener() {
        super(CheckType.BLOCKINTERACT);
        final NoCheatPlusAPI api = NCPAPIProvider.getNoCheatPlusAPI();
        api.register(api.newRegistrationContext() //
                // BlockInteractConfig
                .registerConfigWorld(BlockInteractConfig.class)
                .factory(new IFactoryOne<WorldFactoryArgument, BlockInteractConfig>() {
                    @Override
                    public BlockInteractConfig getNewInstance(WorldFactoryArgument arg) {
                        return new BlockInteractConfig(arg.worldData);
                    }
                })
                .registerConfigTypesPlayer(CheckType.BLOCKINTERACT, true)
                .context() //
                // BlockinteractData
                .registerDataPlayer(BlockInteractData.class)
                .factory(new IFactoryOne<PlayerFactoryArgument, BlockInteractData>() {
                    @Override
                    public BlockInteractData getNewInstance(
                            PlayerFactoryArgument arg) {
                        return new BlockInteractData();
                    }
                })
                .addToGroups(CheckType.BLOCKINTERACT, true, IData.class, ICheckData.class)
                .context() //
                );
    }

    /**
     * We listen to PlayerInteractEvent events for obvious reasons.
     * 
     * @param event
     *            the event
     */
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        final BlockInteractData data = pData.getGenericInstance(BlockInteractData.class);

        if (!pData.isCheckActive(CheckType.BLOCKINTERACT, player)) return;

        data.resetLastBlock();
        // Early cancel for interact events with dead players and other.
        final int cancelId;
        if (player.isDead() && BridgeHealth.getHealth(player) <= 0.0) { // TODO: Should be dead !?.
            // Auto-soup after death.
            /*
             * TODO: Allow physical interact after death? Risks could be command
             * blocks used etc.
             */
            cancelId = idCancelDead;
        }
        else if (!player.isOnline()) {
            cancelId = idCancelOffline;
        }
        else if (MovingUtil.hasScheduledPlayerSetBack(player)) {
            // Might log.
            cancelId = -1; // No counters yet, but do prevent.
        }
        else {
            cancelId = Integer.MIN_VALUE;
        }
        if (cancelId != Integer.MIN_VALUE) {
            event.setUseInteractedBlock(Result.DENY);
            event.setUseItemInHand(Result.DENY);
            event.setCancelled(true);
            data.setPlayerInteractEventResolution(event);
            if (cancelId >= 0) {
                counters.addPrimaryThread(cancelId, 1);
            }
            return;
        }

        // TODO: Re-arrange for interact spamming. (With ProtocolLib something else is in place as well.)
        final Action action = event.getAction();
        final Block block = event.getClickedBlock();
        final int previousLastTick = data.getLastTick();
        // TODO: Last block setting: better on monitor !?.
        boolean blockChecks = true;
        if (block == null) {
            data.resetLastBlock();
            blockChecks = false;
        } else if (block.getType().toString().equals("SCAFFOLDING")) {
        	blockChecks = false;
        }
        else {
            data.setLastBlock(block, action);
        }
        final BlockFace face = event.getBlockFace();
        final ItemStack stack;
        switch(action) {
            case RIGHT_CLICK_AIR:
                // TODO: What else to adapt?
            case LEFT_CLICK_AIR:
                // TODO: What else to adapt?
            case LEFT_CLICK_BLOCK:
                stack = null;
                break;
            case RIGHT_CLICK_BLOCK:
                stack = Bridge1_9.getUsedItem(player, event);
                if (stack != null && stack.getType() == Material.ENDER_PEARL) {
                    checkEnderPearlRightClickBlock(player, block, face, event, previousLastTick, data, pData);
                }
                if (stack != null && stack.getType().toString().equals("SCAFFOLDING")) {
                	blockChecks = false;
                }
                break;
            default:
                data.setPlayerInteractEventResolution(event);
                return;
        }

        boolean cancelled = false;
        if (event.isCancelled() && event.useInteractedBlock() != Result.ALLOW) {
            if (event.useItemInHand() == Result.ALLOW) {
                blockChecks = false;
                // TODO: Some potential for plugin features...
            }
            else {
                // Can't do more than prevent all (could: set to prevent on highest, if desired).
                data.setPlayerInteractEventResolution(event);
                return;
            }
        }

        final BlockInteractConfig cc = pData.getGenericInstance(BlockInteractConfig.class);
        boolean preventUseItem = false;

        final Location loc = player.getLocation(useLoc);
        final FlyingQueueHandle flyingHandle = new FlyingQueueHandle(pData);

        // TODO: Always run all checks, also for !isBlock ?

        // Interaction speed.
        if (!cancelled && speed.isEnabled(player, pData) 
                && speed.check(player, data, cc)) {
            cancelled = true;
            preventUseItem = true;
        }

        if (blockChecks) {
            final double eyeHeight = MovingUtil.getEyeHeight(player);
            // First the reach check.
            if (!cancelled && reach.isEnabled(player, pData) 
                    && reach.check(player, loc, eyeHeight, block, data, cc)) {
                cancelled = true;
            }

            // Second the direction check
            if (!cancelled && direction.isEnabled(player, pData) 
                    && direction.check(player, loc, eyeHeight, block, face, flyingHandle, 
                            data, cc, pData)) {
                cancelled = true;
            }

            // Ray tracing for freecam use etc.
            if (!cancelled && visible.isEnabled(player, pData) 
                    && visible.check(player, loc, eyeHeight, block, face, action, flyingHandle, 
                            data, cc, pData)) {
                cancelled = true;
            }
        }

        // If one of the checks requested to cancel the event, do so.
        if (cancelled) {
            onCancelInteract(player, block, face, event, previousLastTick, preventUseItem, 
                    data, cc, pData);
        }
        else {
            if (flyingHandle.isFlyingQueueFetched()) {
                // TODO: Update flying queue removing failed entries? At least store index for subsequent checks.
                final int flyingIndex = flyingHandle.getFirstIndexWithContentIfFetched();
                final Integer cId;
                if (flyingIndex == 0) {
                    cId = idInteractLookFlyingFirst;
                }
                else {
                    cId = idInteractLookFlyingOther;
                }
                counters.add(cId, 1);
                if (pData.isDebugActive(CheckType.BLOCKINTERACT)) {
                    // Log which entry was used.
                    logUsedFlyingPacket(player, flyingHandle, flyingIndex);
                }
            }
            else {
                counters.addPrimaryThread(idInteractLookCurrent, 1);
            }
        }
        // Set resolution here already:
        data.setPlayerInteractEventResolution(event);
        useLoc.setWorld(null);
    }

    private void logUsedFlyingPacket(final Player player, final FlyingQueueHandle flyingHandle, 
            final int flyingIndex) {
        final DataPacketFlying packet = flyingHandle.getIfFetched(flyingIndex);
        if (packet != null) {
            debug(player, "Flying packet queue used at index " + flyingIndex + ": pitch=" + packet.getPitch() + ",yaw=" + packet.getYaw());
            return;
        }
    }

    private void onCancelInteract(final Player player, final Block block, final BlockFace face, 
            final PlayerInteractEvent event, final int previousLastTick, final boolean preventUseItem, 
            final BlockInteractData data, final BlockInteractConfig cc, final IPlayerData pData) {
        final boolean debug = pData.isDebugActive(CheckType.BLOCKINTERACT);
        if (event.isCancelled()) {
            // Just prevent using the block.
            event.setUseInteractedBlock(Result.DENY);
            if (debug) {
                genericDebug(player, block, face, event, "already cancelled: deny use block", previousLastTick, data, cc);
            }
        }
        else {
            final Result previousUseItem = event.useItemInHand();
            event.setCancelled(true);
            event.setUseInteractedBlock(Result.DENY);
            if (
                    previousUseItem == Result.DENY || preventUseItem
                    // Allow consumption still.
                    || !InventoryUtil.isConsumable(Bridge1_9.getUsedItem(player, event))
                    ) {
                event.setUseItemInHand(Result.DENY);
                if (debug) {
                    genericDebug(player, block, face, event, "deny item use", previousLastTick, data, cc);
                }
            }
            else {
                // Consumable and not prevented otherwise.
                // TODO: Ender pearl?
                event.setUseItemInHand(Result.ALLOW);
                if (debug) {
                    genericDebug(player, block, face, event, "allow edible item use", previousLastTick, data, cc);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    public void onPlayerInteractMonitor(final PlayerInteractEvent event) {
        // Set event resolution.
        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        final BlockInteractData data = pData.getGenericInstance(BlockInteractData.class);

        if (!pData.isCheckActive(CheckType.BLOCKINTERACT, player)) return;

        data.setPlayerInteractEventResolution(event);
        /*
         * TODO: BlockDamageEvent fires before BlockInteract/MONITOR level,
         * BlockBreak after (!). Thus resolution is set on LOWEST already,
         * probably should be HIGHEST to account for other plugins.
         */
        // Elytra boost.
        /*
         * TODO: Cross check with the next incoming move: has an item been used,
         * is gliding, reset if necessary.
         */
        //final Block block = event.getClickedBlock();
        //        if (data.debug) {
        //            debug(player, "BlockInteractResolution: cancelled=" + event.isCancelled() 
        //            + " action=" + event.getAction() + " block=" + block + " item=" + Bridge1_9.getUsedItem(player, event));
        //        }
        if (
                (
                        event.getAction() == Action.RIGHT_CLICK_AIR 
                        // Water doesn't happen, block typically is null.
                        //                        || event.getAction() == Action.RIGHT_CLICK_BLOCK 
                        //                        && block != null && BlockProperties.isLiquid(block.getType())
                        // TODO: web ?
                        )
                && event.isCancelled() && event.useItemInHand() != Result.DENY) {
            final ItemStack stack = Bridge1_9.getUsedItem(player, event);
            if (stack != null && BridgeMisc.maybeElytraBoost(player, stack.getType())) {
                final int power = BridgeMisc.getFireworksPower(stack);
                final MovingData mData = pData.getGenericInstance(MovingData.class);
                final int ticks = Math.max((1 + power) * (power < 2 ? 20 : 15), 30);
                mData.fireworksBoostDuration = ticks;
				mData.fireworksBoostTickNeedCheck = ticks - 1;
                // Expiration tick: not general latency, rather a minimum margin for sudden congestion.
                mData.fireworksBoostTickExpire = TickTask.getTick() + ticks;
                // TODO: Invalidation mechanics: by tick/time well ?
                // TODO: Implement using it in CreativeFly.
                if (pData.isDebugActive(CheckType.MOVING)) {
                    debug(player, "Elytra boost (power " + power + "): " + stack);
                }
            }
        }
    }

    private void checkEnderPearlRightClickBlock(final Player player, final Block block, 
            final BlockFace face, final PlayerInteractEvent event, 
            final int previousLastTick, final BlockInteractData data,
            final IPlayerData pData) {
        if (block == null || !BlockProperties.isPassable(block.getType())) {
            final CombinedConfig ccc = pData.getGenericInstance(CombinedConfig.class);
            if (ccc.enderPearlCheck && ccc.enderPearlPreventClickBlock) {
                event.setUseItemInHand(Result.DENY);
                if (pData.isDebugActive(CheckType.BLOCKINTERACT)) {
                    final BlockInteractConfig cc = pData.getGenericInstance(BlockInteractConfig.class);
                    genericDebug(player, block, face, event, "click block: deny use ender pearl", previousLastTick, data, cc);
                }
            }
        }
    }

    private void genericDebug(final Player player, final Block block, final BlockFace face, 
            final PlayerInteractEvent event, final String tag, final int previousLastTick, 
            final BlockInteractData data, final BlockInteractConfig cc) {
        final StringBuilder builder = new StringBuilder(512);
        // Rate limit.
        if (data.getLastTick() == previousLastTick && data.subsequentCancel > 0) {
            data.rateLimitSkip ++;
            return;
        }
        // Debug log.
        builder.append("Interact cancel: " + event.isCancelled());
        builder.append(" (");
        builder.append(tag);
        if (block == null) {
            builder.append(") block: null");
        }
        else {
            builder.append(") block: ");
            builder.append(block.getWorld().getName() + "/" + LocUtil.simpleFormat(block));
            builder.append(" type: " + block.getType());
            builder.append(" data: " + BlockProperties.getData(block));
            builder.append(" face: " + face);
        }

        if (data.rateLimitSkip > 0) {
            builder.append(" skipped(rate-limit: " + data.rateLimitSkip);
            data.rateLimitSkip = 0;
        }
        debug(player, builder.toString());
    }

}