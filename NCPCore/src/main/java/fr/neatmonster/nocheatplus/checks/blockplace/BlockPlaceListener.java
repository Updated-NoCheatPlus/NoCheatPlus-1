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

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSignOpenEvent;
import org.bukkit.event.player.PlayerSignOpenEvent.Cause;
import org.bukkit.inventory.ItemStack;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.CheckListener;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.blockinteract.BlockInteractData;
import fr.neatmonster.nocheatplus.checks.blockinteract.BlockInteractListener;
import fr.neatmonster.nocheatplus.checks.combined.Combined;
import fr.neatmonster.nocheatplus.checks.combined.CombinedConfig;
import fr.neatmonster.nocheatplus.checks.combined.Improbable;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.checks.net.FlyingQueueHandle;
import fr.neatmonster.nocheatplus.checks.net.model.DataPacketFlying;
import fr.neatmonster.nocheatplus.compat.BridgeEntityType;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.components.NoCheatPlusAPI;
import fr.neatmonster.nocheatplus.components.data.ICheckData;
import fr.neatmonster.nocheatplus.components.data.IData;
import fr.neatmonster.nocheatplus.components.registry.factory.IFactoryOne;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.players.PlayerFactoryArgument;
import fr.neatmonster.nocheatplus.stats.Counters;
import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;
import fr.neatmonster.nocheatplus.utilities.TickTask;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;
import fr.neatmonster.nocheatplus.worlds.WorldFactoryArgument;

/**
 * Central location to listen to events that are relevant for the block place checks.
 * 
 * @see BlockPlaceEvent
 */
public class BlockPlaceListener extends CheckListener {

    /** Surrounding material check. */
    private final Against against = addCheck(new Against());

    /** AutoSign hack check. */
    private final AutoSign autoSign = addCheck(new AutoSign());

    /** The direction check. */
    private final Direction direction = addCheck(new Direction());

    /** The fast place check. */
    private final FastPlace fastPlace = addCheck(new FastPlace());

    /** The no swing check. */
    private final NoSwing noSwing = addCheck(new NoSwing());

    /** The reach check. */
    private final Reach reach = addCheck(new Reach());

    /** The scaffold check. */
    private final Scaffold scaffold = addCheck(new Scaffold());

    /** The speed check. */
    private final Speed speed = addCheck(new Speed());

    /** For temporary use: LocUtil.clone before passing deeply, call setWorld(null) after use. */
    private final Location useLoc = new Location(null, 0, 0, 0);
    private final Location useLoc2 = new Location(null, 0, 0, 0);
    
    // Counter/debug stuff
    private final Counters counters = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(Counters.class);
    private final int idBoatsOnWaterOnly = counters.registerKey("boatsonwateronly");
    private final int idEnderPearl = counters.registerKey("throwenderpearl");
    
    // Reflection stuff
    private final Class<?> blockMultiPlaceEvent = ReflectionUtil.getClass("org.bukkit.event.block.BlockMultiPlaceEvent");
    private final boolean hasPlayerSignOpenEvent = ReflectionUtil.getClass("org.bukkit.event.player.PlayerSignOpenEvent") != null;
    private final boolean hasGetReplacedState = ReflectionUtil.getMethodNoArgs(BlockPlaceEvent.class, "getReplacedState", BlockState.class) != null;

    public final List<BlockFace> faces = Arrays.asList(new BlockFace[] {BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH});

    @SuppressWarnings("unchecked")
    public BlockPlaceListener() {
        super(CheckType.BLOCKPLACE);
        final NoCheatPlusAPI api = NCPAPIProvider.getNoCheatPlusAPI();
        if (hasPlayerSignOpenEvent) {
            queuedComponents.add(new Listener() {
                @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
                public void onOpeningSign1_20(final PlayerSignOpenEvent event) {
                    handleSignOpenEvent(event.getPlayer(), event.getCause());
                }
            });
        }
        api.register(api.newRegistrationContext()
                // BlockPlaceConfig
                .registerConfigWorld(BlockPlaceConfig.class)
                .factory(new IFactoryOne<WorldFactoryArgument, BlockPlaceConfig>() {
                    @Override
                    public BlockPlaceConfig getNewInstance(WorldFactoryArgument arg) {
                        return new BlockPlaceConfig(arg.worldData);
                    }
                })
                .registerConfigTypesPlayer()
                .context() //
                // BlockPlaceData
                .registerDataPlayer(BlockPlaceData.class)
                .factory(new IFactoryOne<PlayerFactoryArgument, BlockPlaceData>() {
                    @Override
                    public BlockPlaceData getNewInstance(
                            PlayerFactoryArgument arg) {
                        return new BlockPlaceData();
                    }
                })
                .addToGroups(CheckType.BLOCKPLACE, true, IData.class, ICheckData.class)
                .context() //
                );
    }

    public static int getBlockPlaceHash(final Block block, final Material mat) {
        int hash = fr.neatmonster.nocheatplus.utilities.ds.map.CoordHash.hashCode3DPrimes(block.getX(), block.getY(), block.getZ());
        if (mat != null) {
            hash |= mat.name().hashCode();
        }
        hash |= block.getWorld().getName().hashCode();
        return hash;
    }
    
    /** Mechanic introduced with 1.20. The event itself was introduced around 1.20.1 */
    private void handleSignOpenEvent(final Player player, Cause cause) {
        if (cause != Cause.INTERACT) {
            return;
        }
        // Always set this to 0 on opening a sign. We don't need to check for hashing here, only time.
        final IPlayerData pData = DataManager.getPlayerData(player);
        final BlockPlaceData data = pData.getGenericInstance(BlockPlaceData.class);
        data.autoSignPlacedHash = 0;
        data.signOpenTime = System.currentTimeMillis();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPlace(final BlockPlaceEvent event) {
        if (!DataManager.getPlayerData(event.getPlayer()).isCheckActive(CheckType.BLOCKPLACE, event.getPlayer())) {
            return;
        }
        final Block block = event.getBlockPlaced();
        final Block blockAgainst = event.getBlockAgainst();
        // Skip any null blocks.
        if (block == null || blockAgainst == null) {
            return;
        }
        final Player player = event.getPlayer();
        final Material placedMat;
        if (hasGetReplacedState) {
            placedMat = event.getBlockPlaced().getType();
        }
        else if (Bridge1_9.hasGetItemInOffHand()) {
            placedMat = BlockProperties.isAir(event.getItemInHand()) ? Material.AIR : event.getItemInHand().getType();
        }
        else placedMat = Bridge1_9.getItemInMainHand(player).getType(); // Safety first.
    
        boolean cancelled = false;
        int skippedRedundantChecks = 0;
        final IPlayerData pData = DataManager.getPlayerData(player);
        final BlockPlaceData data = pData.getGenericInstance(BlockPlaceData.class);
        final BlockPlaceConfig cc = pData.getGenericInstance(BlockPlaceConfig.class);
        final BlockInteractData bdata = pData.getGenericInstance(BlockInteractData.class);
        // isInteractBlock - the block placed against is the block last interacted with.
        final boolean isInteractBlock = !bdata.getLastIsCancelled() && bdata.matchesLastBlock(TickTask.getTick(), blockAgainst);
        final BlockFace placedFace = event.getBlock().getFace(blockAgainst);
        final Block blockPlaced = event.getBlockPlaced();
        final boolean shouldSkipSome;
        if (blockMultiPlaceEvent != null && event.getClass() == blockMultiPlaceEvent) {
            if (placedMat == Material.BEDROCK || Bridge1_9.hasEndCrystalItem() && placedMat == Bridge1_9.END_CRYSTAL_ITEM) {
                shouldSkipSome = true;
            }
            else {
                if (pData.isDebugActive(CheckType.BLOCKPLACE)) {
                    debug(player, "Block place " + event.getClass().getName() + " " + placedMat);
                }
                shouldSkipSome = false;
            }
        } 
        else shouldSkipSome = BlockProperties.isScaffolding(placedMat);

        if (MaterialUtil.isAnySign(placedMat)) {
            // To be checkde on SignChangeEvents
            data.signOpenTime = System.currentTimeMillis();
            data.autoSignPlacedHash = getBlockPlaceHash(block, placedMat);
            if (pData.isDebugActive(CheckType.BLOCKPLACE_AUTOSIGN)) {
                debug(player, "Register time and hash for this placed sign: h= " + data.autoSignPlacedHash + " / t= " + data.signOpenTime);
            }
        }

        // Don't run checks, if a set back is scheduled.
        if (pData.isPlayerSetBackScheduled()) {
            cancelled = true;
            if (pData.isDebugActive(CheckType.BLOCKPLACE)) {
                debug(player, "Prevent block place due to a scheduled set back.");
            }
        }
        
        // Surrounding material first.
        if (!cancelled && against.isEnabled(player, pData) && !BlockProperties.isScaffolding(placedMat)
            && against.check(player, block, placedMat, blockAgainst, isInteractBlock, data, cc, pData)) {
            cancelled = true;
        }

        // No swing check (player doesn't swing their arm when placing a lily pad).
        if (!cancelled && !cc.noSwingExceptions.contains(placedMat) 
            && noSwing.isEnabled(player, pData) && noSwing.check(player, data, cc)) {
            cancelled = true;
        }

        // Fast place check.
        if (!cancelled && fastPlace.isEnabled(player, pData)) {
            if (fastPlace.check(player, block, TickTask.getTick(), data, cc, pData)) {
                cancelled = true;
            }
            // Check for Improbable, whatever FastPlace says, provided the feature is enabled at all.
            if (cc.fastPlaceImprobableWeight > 0.0f) {
                // Check only if frequency is decently high.
                if (data.fastPlaceVL > 20) {
                    // Don't check if set to only feed.
                    if (!cc.fastPlaceImprobableFeedOnly) {
                        if (Improbable.check(player, cc.fastPlaceImprobableWeight, System.currentTimeMillis(), "blockplace.fastplace", pData)) {
                            cancelled = true;
                        }
                    }
                    else Improbable.feed(player, cc.fastPlaceImprobableWeight, System.currentTimeMillis()); 
                }
                // Feed only for lower frequencies.
                else Improbable.feed(player, cc.fastPlaceImprobableWeight, System.currentTimeMillis()); 
            }
        }

        // Scaffold Check
        if (scaffold.isEnabled(player, pData) && placedFace != null) {
            final PlayerMoveData thisMove = pData.getGenericInstance(MovingData.class).playerMoves.getCurrentMove();
            if (faces.contains(placedFace) 
                && thisMove.from.getY() - blockPlaced.getY() < 2.0
                && thisMove.from.getY() - blockPlaced.getY() >= 1.0
                && blockPlaced.getType().isSolid() 
                && TrigUtil.distance(player.getLocation(), blockPlaced.getLocation()) < 2.0) {

                // Monitor yawrate before feeding Improbable or checking for Scaffold
                if (Combined.checkYawRate(player, thisMove.from.getYaw(), System.currentTimeMillis(), thisMove.from.getWorldName(), pData)) {
                    cancelled = true;
                }
                // Always check for Scaffold whatever yawrate says. 
                if (data.cancelNextPlace && (Math.abs(data.currentTick - TickTask.getTick()) < 10)
                    || scaffold.check(player, placedFace, pData, data, cc, event.isCancelled(), thisMove.yDistance, pData.getGenericInstance(MovingData.class).sfJumpPhase)) {
                    cancelled = true;
                }
                // If not cancelled, do feed the Improbable.
                else if (cc.scaffoldImprobableWeight > 0.0f) {
                    if (cc.scaffoldImprobableFeedOnly) {
                        Improbable.feed(player, cc.scaffoldImprobableWeight, System.currentTimeMillis());
                    } 
                    else if (Improbable.check(player, cc.scaffoldImprobableWeight, System.currentTimeMillis(), "blockplace.scaffold", pData)) {
                        cancelled = true;
                    }
                }
                if (!cancelled) data.scaffoldVL *= 0.98;
            }
            // Cleanup
            data.cancelNextPlace = false;
        }

        final FlyingQueueHandle flyingHandle = new FlyingQueueHandle(pData);
        final boolean reachCheck = pData.isCheckActive(CheckType.BLOCKPLACE_REACH, player);
        final boolean directionCheck = pData.isCheckActive(CheckType.BLOCKPLACE_DIRECTION, player);
        if (reachCheck || directionCheck) {
            final Location loc = player.getLocation(useLoc);
            final double eyeHeight = MovingUtil.getEyeHeight(player);
            // Reach check (distance).
            if (!cancelled && !shouldSkipSome) {
                if (isInteractBlock && bdata.isPassedCheck(CheckType.BLOCKINTERACT_REACH)) {
                    skippedRedundantChecks++;
                }
                else if (reachCheck && reach.check(player, eyeHeight, block, data, cc)) {
                    cancelled = true;
                }
            }
            // Direction check.
            if (!cancelled && !shouldSkipSome) {
                if (isInteractBlock && bdata.isPassedCheck(CheckType.BLOCKINTERACT_DIRECTION)) {
                    skippedRedundantChecks++;
                }
                else if (directionCheck) {
                    if (blockAgainst.getType() == Material.LADDER || BlockProperties.isCarpet(blockAgainst.getType())) {
                        // TODO: Actually fix this, don't just ignore it.
                        // ISSUE: https://github.com/NoCheatPlus/Issues/issues/524
                    } 
                    else if (direction.check(player, loc, eyeHeight, block, null, flyingHandle, data, cc, pData)) {
                        cancelled = true;
                    }
                }
            }
            useLoc.setWorld(null);
        }

        // If one of the checks requested to cancel the event, do so.
        if (cancelled) {
            event.setCancelled(true);
            // We need to do this because block-place cancelling can easily desync the player.
            pData.requestUpdateInventory();
        }
        if (pData.isDebugActive(CheckType.BLOCKPLACE)) {
            debugBlockPlace(player, placedMat, block, blockAgainst, skippedRedundantChecks, flyingHandle, pData);
        }
        // Cleanup
        // Reminder(currently unused): useLoc.setWorld(null);
    }
    
    /** We listen to sign change events for the autosign check */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSignChange(final SignChangeEvent event) {
        if (!DataManager.getPlayerData(event.getPlayer()).isCheckActive(CheckType.BLOCKPLACE, event.getPlayer())) {
            return;
        }
        if (event.getClass() != SignChangeEvent.class) {
            // Built in plugin compatibility.
            // (Don't understand why two consecutive events editing the same block are a problem)
            return;
        }
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final String[] lines = event.getLines();
        if (block == null || lines == null || player == null) {
            return;
        }
        final IPlayerData pData = DataManager.getPlayerData(player);
        final BlockPlaceData data = pData.getGenericInstance(BlockPlaceData.class);
        if (autoSign.isEnabled(player, pData) && autoSign.check(player, block, lines, pData)) {
            event.setCancelled(true);
        }
    }
    
    /** We listen to this event for the swining animation */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAnimation(final PlayerAnimationEvent event) {
        final BlockPlaceData data = DataManager.getGenericInstance(event.getPlayer(), BlockPlaceData.class);
        data.noSwingCount = Math.max(data.noSwingCount - 1, 0);
    } 
    
    /** We listen to interact events for some misc checks */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (!DataManager.getPlayerData(event.getPlayer()).isCheckActive(CheckType.BLOCKPLACE, event.getPlayer())) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final Player player = event.getPlayer();
        final ItemStack stack = Bridge1_9.getUsedItem(player, event);
        if (stack == null) {
            return;
        }
        final IPlayerData pData = DataManager.getPlayerData(player);
        final BlockPlaceConfig cc = pData.getGenericInstance(BlockPlaceConfig.class);
        final Material type = stack.getType();
        if (MaterialUtil.isBoat(type)) {
            if (cc.boatsOnWaterOnly) {
                denyOrAllowBoatPlacement(player, event, pData);
            }
        }
        else if (MaterialUtil.isSpawnEgg(type)) {
            if (speed.isEnabled(player, pData) && speed.check(player, cc, pData)) {
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * Check if the boat can be placed on ground, provided CachedConfig.boatsOnWaterOnly returned true.
     * 
     * @param player
     * @param event
     * @param pData
     */
    private void denyOrAllowBoatPlacement(final Player player, final PlayerInteractEvent event, final IPlayerData pData) {
        final Block block = event.getClickedBlock();
        final Material mat = block.getType();
        if (BlockProperties.isWater(mat)) {
            // Allow placement
            return;
        }
        // TODO: Shouldn't this be the opposite face?
        final BlockFace blockFace = event.getBlockFace();
        final Block relBlock = block.getRelative(blockFace);
        final Material relMat = relBlock.getType();
        // TODO: Placing inside of water, but not "against" ?
        if (BlockProperties.isWater(relMat)) {
            // Allow placement
            return;
        }
        // Interacted with a non-water block, deny.
        if (!pData.hasPermission(Permissions.BLOCKPLACE_BOATSONWATERONLY, player)) {
            final Result previousUseBlock = event.useInteractedBlock();
            event.setCancelled(true);
            event.setUseItemInHand(Result.DENY);
            event.setUseInteractedBlock(previousUseBlock == Result.DEFAULT ? Result.ALLOW : previousUseBlock);
            counters.addPrimaryThread(idBoatsOnWaterOnly, 1);
            // Attempt to refresh the inventory in order to workaround possible ghost items.
            pData.requestUpdateInventory();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onProjectileLaunch(final ProjectileLaunchEvent event) {
        // The shooter needs to be a player.
        final Projectile projectile = event.getEntity();
        final Player player = BridgeMisc.getShooterPlayer(projectile);
        if (player == null) {
            return;
        }
        if (!DataManager.getPlayerData(player).isCheckActive(CheckType.BLOCKPLACE, player)) {
            return;
        }
        if (MovingUtil.hasScheduledPlayerSetBack(player)) {
            event.setCancelled(true);
            return;
        }
        // And the projectile must be one the following:
        EntityType type = event.getEntityType();
        if (!BridgeEntityType.PROJECTILE_CHECK_LIST.contains(type)) return;

        // Do the actual check...
        final IPlayerData pData = DataManager.getPlayerData(player);
        final BlockPlaceConfig cc = pData.getGenericInstance(BlockPlaceConfig.class);
        boolean cancel = false;
        if (speed.isEnabled(player, pData)) {
            final long now = System.currentTimeMillis();
            final Location loc = player.getLocation(useLoc2);
            if (Combined.checkYawRate(player, loc.getYaw(), now, loc.getWorld().getName(), pData)) {
                // Yawrate (checked extra).
                cancel = true;
            }
            if (speed.check(player, cc, pData)) {
                // If the check was positive, cancel the event.
                cancel = true;
            }
            else if (cc.speedImprobableWeight > 0.0f) {
                if (cc.speedImprobableFeedOnly) {
                    Improbable.feed(player, cc.speedImprobableWeight, now);
                } 
                else if (Improbable.check(player, cc.speedImprobableWeight, now, "blockplace.speed", pData)) {
                    cancel = true;
                }
            }
        }

        // Ender pearl glitch (ab-) use.
        if (!cancel && type == EntityType.ENDER_PEARL && pData.getGenericInstance(CombinedConfig.class).enderPearlCheck) {
            if (!BlockProperties.isPassable(projectile.getLocation(useLoc2))) {
                // Launch into a block.
                cancel = true;
            }
            else {
                if (!BlockProperties.isPassable(player.getEyeLocation(), projectile.getLocation(useLoc2))) {
                    // (Spare a useLoc2, for this is seldom rather.)
                    // Something between player 
                    cancel = true;
                }
                else {
                    final Material mat = player.getLocation(useLoc2).getBlock().getType();
                    final long flags = BlockFlags.F_CLIMBABLE | BlockFlags.F_LIQUID | BlockFlags.F_IGN_PASSABLE;
                    if (!BlockProperties.isAir(mat) && (BlockFlags.getBlockFlags(mat) & flags) == 0 && !mcAccess.getHandle().hasGravity(mat)) {
                        // Still fails on piston traps etc.
                        if (!BlockProperties.isPassable(player.getLocation(), projectile.getLocation()) 
                            && !BlockProperties.isOnGroundOrResetCond(player, player.getLocation(), pData.getGenericInstance(MovingConfig.class).yOnGround)) {
                            cancel = true;
                        }
                    }
                }
            }
            if (cancel) {
                counters.addPrimaryThread(idEnderPearl, 1);
            }
        }

        if (cancel) {
            event.setCancelled(true);
        }
        // Cleanup.
        useLoc2.setWorld(null);
    }

    // TODO: remove this
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        final BlockPlaceData data = pData.getGenericInstance(BlockPlaceData.class);

        if (!pData.isCheckActive(CheckType.BLOCKPLACE, player)) return;

        if (player.isSprinting()) {
            data.sprintTime = TickTask.getTick();
        } else if (player.isSneaking()) {
            data.sneakTime = TickTask.getTick();
        }
    }

    private void debugBlockPlace(final Player player, final Material placedMat, final Block block, final Block blockAgainst, 
                                 final int skippedRedundantChecks, final FlyingQueueHandle flyingHandle, final IPlayerData pData) {
        debug(player, "Block place(" + placedMat + "): " + block.getX() + ", " + block.getY() + ", " + block.getZ());
        BlockInteractListener.debugBlockVSBlockInteract(player, checkType, blockAgainst, "onBlockPlace(blockAgainst)", Action.RIGHT_CLICK_BLOCK, pData);
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
}