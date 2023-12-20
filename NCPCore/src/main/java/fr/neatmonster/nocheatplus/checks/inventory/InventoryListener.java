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
package fr.neatmonster.nocheatplus.checks.inventory;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.CheckListener;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.combined.Combined;
import fr.neatmonster.nocheatplus.checks.combined.CombinedData;
import fr.neatmonster.nocheatplus.checks.combined.Improbable;
import fr.neatmonster.nocheatplus.compat.BridgeHealth;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.components.NoCheatPlusAPI;
import fr.neatmonster.nocheatplus.components.data.ICheckData;
import fr.neatmonster.nocheatplus.components.data.IData;
import fr.neatmonster.nocheatplus.components.entity.IEntityAccessVehicle;
import fr.neatmonster.nocheatplus.components.registry.event.IGenericInstanceHandle;
import fr.neatmonster.nocheatplus.components.registry.factory.IFactoryOne;
import fr.neatmonster.nocheatplus.components.registry.feature.JoinLeaveListener;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.players.PlayerFactoryArgument;
import fr.neatmonster.nocheatplus.stats.Counters;
import fr.neatmonster.nocheatplus.utilities.InventoryUtil;
import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;
import fr.neatmonster.nocheatplus.worlds.WorldFactoryArgument;

/**
 * Central location to listen to events that are relevant for the inventory checks.
 * 
 * @see InventoryEvent
 */
public class InventoryListener  extends CheckListener implements JoinLeaveListener {
    
    // Checks
    /** More Inventory check */
    private final MoreInventory moreInv = addCheck(new MoreInventory());

    /** The fast click check. */
    private final FastClick fastClick  = addCheck(new FastClick());

    /** The instant bow check. */
    private final InstantBow instantBow = addCheck(new InstantBow());

    /** The fast consume check. */
    private final FastConsume fastConsume = addCheck(new FastConsume());
    
    private final Gutenberg gutenberg = addCheck(new Gutenberg());
    
    /** The open check */
    private final Open open = addCheck(new Open());
    
    // Other/Auxiliary stuff
    private boolean keepCancel = false;

    private final boolean hasInventoryAction;

    /** For temporary use: LocUtil.clone before passing deeply, call setWorld(null) after use. */
    private final Location useLoc = new Location(null, 0, 0, 0);

    private final Counters counters = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(Counters.class);

    private final int idCancelDead = counters.registerKey("cancel.dead");

    private final IGenericInstanceHandle<IEntityAccessVehicle> handleVehicles = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstanceHandle(IEntityAccessVehicle.class);

    @SuppressWarnings("unchecked")
    public InventoryListener() {
        super(CheckType.INVENTORY);
        final NoCheatPlusAPI api = NCPAPIProvider.getNoCheatPlusAPI();
        api.register(api.newRegistrationContext()
                // InventoryConfig
                .registerConfigWorld(InventoryConfig.class)
                .factory(new IFactoryOne<WorldFactoryArgument, InventoryConfig>() {
                    @Override
                    public InventoryConfig getNewInstance(
                            WorldFactoryArgument arg) {
                        return new InventoryConfig(arg.worldData);
                    }
                })
                .registerConfigTypesPlayer()
                .context() //
                // InventoryData
                .registerDataPlayer(InventoryData.class)
                .factory(new IFactoryOne<PlayerFactoryArgument, InventoryData>() {
                    @Override
                    public InventoryData getNewInstance(
                            PlayerFactoryArgument arg) {
                        return new InventoryData();
                    }
                })
                .addToGroups(CheckType.INVENTORY, true, IData.class, ICheckData.class)
                .context() //
                );
        hasInventoryAction = ReflectionUtil.getClass("org.bukkit.event.inventory.InventoryAction") != null;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityShootBow(final EntityShootBowEvent event) {
        // Only if a player shot the arrow.
        if (event.getEntity() instanceof Player) {
            final Player player = (Player) event.getEntity();
            final IPlayerData pData = DataManager.getPlayerData(player);
            if (instantBow.isEnabled(player, pData)) {
                final long now = System.currentTimeMillis();
                final Location loc = player.getLocation(useLoc);
                if (Combined.checkYawRate(player, loc.getYaw(), now, loc.getWorld().getName(), pData)) {
                    // No else if with this, could be cancelled due to other checks feeding, does not have actions.
                    event.setCancelled(true);
                }

                final InventoryConfig cc = pData.getGenericInstance(InventoryConfig.class);
                // Still check instantBow, whatever yawrate says.
                if (instantBow.check(player, event.getForce(), now)) {
                    event.setCancelled(true);
                }
                else if (cc.instantBowImprobableWeight > 0.0f) {
                    if (cc.instantBowImprobableFeedOnly) {
                        Improbable.feed(player, cc.instantBowImprobableWeight, now);
                    }
                    else if (Improbable.check(player, cc.instantBowImprobableWeight, now, "inventory.instantbow", pData)) {
                        // Combined fighting speed (Else if: Matter of taste, preventing extreme cascading and actions spam).
                        event.setCancelled(true);
                    }
                }
                useLoc.setWorld(null);
            }  
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onFoodLevelChange(final FoodLevelChangeEvent event) {
        // Only if a player ate food.
        if (event.getEntity() instanceof Player) {
            final Player player = (Player) event.getEntity();
            if (player.isDead() && BridgeHealth.getHealth(player) <= 0.0) {
                // Eat after death.
                event.setCancelled(true);
                counters.addPrimaryThread(idCancelDead, 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        final long now = System.currentTimeMillis();
        final HumanEntity entity = event.getWhoClicked();
        if (!(entity instanceof Player)) {
            return;
        }
        final Player player = (Player) entity;
        final IPlayerData pData = DataManager.getPlayerData(player);
        final InventoryData data = pData.getGenericInstance(InventoryData.class);
        final int slot = event.getSlot();
        final String inventoryAction = hasInventoryAction ? event.getAction().name() : null;
        final InventoryConfig cc = pData.getGenericInstance(InventoryConfig.class);
        if (pData.isDebugActive(checkType)) {
            outputDebugInventoryClick(player, slot, event, inventoryAction);
        }
        // Set this one as soon as we can and regardless if the click was cancelled or not.
        if (data.inventoryOpenTime == 0) {
            data.inventoryOpenTime = now;
            if (pData.isDebugActive(CheckType.INVENTORY)) {
                debug(player, "InventoryClickEvent: inventory wasn't explicitly open previously but an inventory click was sent; register time of the 1st click (assume inventory is open from this moment on)");
            }
        }
        if (event.isCancelled()) {
            // Previously: ignoreCancelled = true
            // We still want to know if the player clicked in the inventory (even if cancelled) for the inventory-open estimate above.
            return;
        }
        if (slot == InventoryView.OUTSIDE || slot < 0) {
            // Set and return, not interested in these clicks.
            data.lastClickTime = now;
            return;
        }

        final ItemStack cursor = event.getCursor();
        final ItemStack clicked = event.getCurrentItem();
        boolean cancel = false;
        // Fast inventory manipulation check.
        if (fastClick.isEnabled(player, pData)) {
            if (!((event.getView().getType().equals(InventoryType.CREATIVE) || player.getGameMode() == GameMode.CREATIVE) && cc.fastClickSpareCreative)) {
                boolean check = true;
                try {
                    // Exempted inventories are not checked.
                    check = !cc.inventoryExemptions.contains(ChatColor.stripColor(event.getView().getTitle()));
                }
                catch (IllegalStateException e) {
                    // Uhm... Can this ISE be fixed?
                    check = true; 
                }
                
                if (check) {
                    // Check for too quick interactions first (we don't need to check for fast clicking if the interaction is inhumanly fast)
                    if (InventoryUtil.isContainerInventory(event.getInventory().getType())
                        && fastClick.checkContainerInteraction(player, data, cc)) {
                        cancel = true;
                        keepCancel = true;
                    }
                    // Then check for too fast inventory clicking
                    if (!cancel && fastClick.check(player, now, event.getView(), slot, cursor, clicked, event.isShiftClick(), inventoryAction, data, cc, pData)) {  
                        cancel = true;
                    }
                }
            }
        }
        
        data.lastClickTime = now;
        data.clickedSlotType = event.getSlotType();
        // Cancel the event.
        if (cancel || keepCancel) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEditingABook(final PlayerEditBookEvent event) {
        final Player player = event.getPlayer();
        if (!gutenberg.isEnabled(player)) {
            return;
        }
        final IPlayerData pData = DataManager.getPlayerData(player);
        final InventoryData data = pData.getGenericInstance(InventoryData.class);
        final BookMeta newMeta = event.getNewBookMeta();
        final int pages = newMeta.getPageCount();
        if (gutenberg.check(player, data, pData, pages)) {
        	event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onConsumingFoodOrPotions(final PlayerItemConsumeEvent event){
        final Player player = event.getPlayer();
        if (player.isDead() && BridgeHealth.getHealth(player) <= 0.0) {
            // Eat after death.
            event.setCancelled(true);
            counters.addPrimaryThread(idCancelDead, 1);
            return;
        }
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (!fastConsume.isEnabled(player)) {
            return;
        }
        final InventoryData data = pData.getGenericInstance(InventoryData.class);
        final long time = System.currentTimeMillis();
        if (fastConsume.check(player, event.getItem(), time, data, pData)) {
            event.setCancelled(true);
            DataManager.getPlayerData(player).requestUpdateInventory();
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onClosingAnyInventory(final InventoryCloseEvent event) {
        // NOTE: ignoreCancelled is kept to true here. Cancelled events won't reset data.
        // NOTE: Priority level -> same as opening time.
        final HumanEntity entity = event.getPlayer();
        if (entity instanceof Player) {
            final Player player = (Player) entity;
            if (player != null) {
                final IPlayerData pData = DataManager.getPlayerData(player);
                final InventoryData data = pData.getGenericInstance(InventoryData.class);
                data.inventoryOpenTime = 0;
                data.containerInteractTime = 0;
                if (pData.isDebugActive(CheckType.INVENTORY)) {
                    debug(player, "InventoryCloseEvent: reset timing data.");
                }
            }
        }
        keepCancel = false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onPlayerInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        final InventoryData data = pData.getGenericInstance(InventoryData.class);
        final CombinedData cData = pData.getGenericInstance(CombinedData.class);
        // Set the container opening time.
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
            // Sneaking and right clicking with a block in hand will cause the player to place the block down, not to open the container.
            && !(BridgeMisc.isUsingItem(player) || player.isSneaking() && event.isBlockInHand())) {
            if (BlockProperties.isContainer(event.getClickedBlock().getType())) {
                data.containerInteractTime = System.currentTimeMillis();
                if (pData.isDebugActive(CheckType.INVENTORY)) {
                    debug(player, "Interacted with a container: register the interaction time.");
                }
            }
        } 
        // Only interested in right-clicks while holding an item.
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        boolean resetAll = false;
        if (event.hasItem()) {
            final ItemStack item = event.getItem();
            final Material type = item.getType();
            // TODO: Get Magic values (800) from the config.
            // TODO: Cancelled / deny use item -> reset all?
            if (type == Material.BOW) {
                final long now = System.currentTimeMillis();
                // It was a bow, the player starts to pull the string, remember this time.
                data.instantBowInteract = (data.instantBowInteract > 0 && now - data.instantBowInteract < 800) ? Math.min(System.currentTimeMillis(), data.instantBowInteract) : System.currentTimeMillis();
            }
            else if (InventoryUtil.isConsumable(type)) {
                final long now = System.currentTimeMillis();
                // It was food, the player starts to eat some food, remember this time and the type of food.
                data.fastConsumeFood = type;
                data.fastConsumeInteract = (data.fastConsumeInteract > 0 && now - data.fastConsumeInteract < 800) ? Math.min(System.currentTimeMillis(), data.fastConsumeInteract) : System.currentTimeMillis();
                data.instantBowInteract = 0; 
            } 
            else resetAll = true;
        }
        else resetAll = true;

        if (resetAll) {
            // Nothing that we are interested in, reset data.
            data.instantBowInteract = 0;
            data.fastConsumeInteract = 0;
            data.fastConsumeFood = null;
        }
    }
    
    // TODO: Why is this handler inside the INVENTORY listener!?
    @EventHandler(priority = EventPriority.LOWEST)
    public final void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || !DataManager.getPlayerData(player).isCheckActive(CheckType.INVENTORY, player)) {
            return;
        }
        if (player.isDead() && BridgeHealth.getHealth(player) <= 0.0) {
            // No zombies.
            event.setCancelled(true);
            counters.addPrimaryThread(idCancelDead, 1);
            return;
        }
        else if (MovingUtil.hasScheduledPlayerSetBack(player)) {
            event.setCancelled(true);
            return;
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public final void onContainerOpen(final InventoryOpenEvent event) {
        // Possibly already prevented by block + entity interaction.
        // NOTE: ignoreCancelled is kept true. Denied openings won't register timing data.
        final long now = System.currentTimeMillis();
        final HumanEntity entity = event.getPlayer();
        if (entity instanceof Player) {
            final Player player = (Player) entity;
            if (player != null) {
                final IPlayerData pData = DataManager.getPlayerData(player);
                final InventoryData data = pData.getGenericInstance(InventoryData.class);
                if (MovingUtil.hasScheduledPlayerSetBack(player)) {
                    // Don't allow players to open inventories on set-backs.
                    event.setCancelled(true);
                    data.inventoryOpenTime = 0; // Just to be sure
                    if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
                        debug(player, "InventoryOpenEvent: attempted to open a container during set back processing; reset timing data and prevent opening.");
                    }
                }
                else if (data.inventoryOpenTime == 0) {
                    // Only set the inventory opening time, if a setback is not scheduled.
                    data.inventoryOpenTime = now;
                     if (pData.isDebugActive(CheckType.INVENTORY)) {
                        debug(player, "Fired an explicit InventoryOpenEvent; inventory is now open (no assumptions): register time.");
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeldChange(final PlayerItemHeldEvent event) {
        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        final InventoryData data = pData.getGenericInstance(InventoryData.class);
        if (pData.isDebugActive(checkType) && data.fastConsumeFood != null) {
            debug(player, "PlayerItemHeldEvent, reset fastconsume.");
        }
        data.instantBowInteract = 0;
        data.fastConsumeInteract = System.currentTimeMillis();
        data.fastConsumeFood = null;
        if (event.getPreviousSlot() != event.getNewSlot()) {
            if (open.check(player)) {
                if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
                    debug(player, "Force-close inventory on changing slots.");
                }
            }
        }   
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {
        // Data is reset regardless of cancellation state (better safe than sorry)
        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (open.check(event.getPlayer())) {
            if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
                debug(player, "Force-close inventory on changing worlds.");
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBreakingBlocks(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        // Can't break blocks with inventory open.
        if (open.check(event.getPlayer())) {
            if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
                debug(player, "Force-close inventory on breaking blocks (cheat prevention).");
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlacingBlocks(final BlockPlaceEvent event) {
        final IPlayerData pData = DataManager.getPlayerData(event.getPlayer());
        // Can't place blocks with inventory open.
        if (open.check(event.getPlayer())) {
            if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
                debug(event.getPlayer(), "Force-close inventory on placing blocks (cheat prevention).");
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerPortal(final PlayerPortalEvent event) {
        // Note: ignore cancelother setting.
        final IPlayerData pData = DataManager.getPlayerData(event.getPlayer());
        if (open.check(event.getPlayer())) {
            if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
                debug(event.getPlayer(), "Force-close inventory on using a portal.");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        // NOTE: Data is reset regardless of cancellation state (better safe than sorry)
        final IPlayerData pData = DataManager.getPlayerData(event.getPlayer());
        if (open.check(event.getPlayer())) {
            if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
                debug(event.getPlayer(), "Force-close inventory on respawning.");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(final EntityDeathEvent event) {
        // NOTE: Data is reset regardless of cancellation state (better safe than sorry)
        final LivingEntity entity = event.getEntity();
        if (entity instanceof Player) {
            final Player player = (Player) entity;
            if (player != null) {
                final IPlayerData pData = DataManager.getPlayerData(player);
                if (open.check(player)) {
                    if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
                        debug(player, "Force-close inventory on death.");
                    }
                }
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerSleep(final PlayerBedEnterEvent event) {
        final IPlayerData pData = DataManager.getPlayerData(event.getPlayer());
        if (open.check(event.getPlayer())) {
            if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
                debug(event.getPlayer(), "Force-close inventory on sleeping.");
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerWake(final PlayerBedLeaveEvent event) {
        final IPlayerData pData = DataManager.getPlayerData(event.getPlayer());
        if (open.check(event.getPlayer())) {
            if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
                debug(event.getPlayer(), "Force-close inventory on waking up.");
            }
        }
    }
    
    // Check even if cancelled
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwingingArm(final PlayerAnimationEvent event) {
        final IPlayerData pData = DataManager.getPlayerData(event.getPlayer());
        if (open.check(event.getPlayer())) {
            if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
                debug(event.getPlayer(), "Force-close inventory on swinging arm (cheat prevention).");
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityPortal(final EntityPortalEnterEvent event) {
        // Check passengers flat for now.
        final Entity entity = event.getEntity();
        if (entity instanceof Player) {
            final IPlayerData pData = DataManager.getPlayerData((Player) entity);
            if (open.check((Player) entity)) {
                if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
                    debug((Player) entity, "Force-close inventory on using a portal (entity).");
                }
            }
        }
        else {
            for (final Entity passenger : handleVehicles.getHandle().getEntityPassengers(entity)) {
                if (passenger instanceof Player) {
                	final IPlayerData pData = DataManager.getPlayerData((Player) entity);
                    if (open.check((Player) passenger)) {
                        if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
                            debug((Player) passenger, "Force-close inventory of passenger on using a portal, passenger: " + passenger.toString());
                        }
                    }
                }
            }
        }
    }
    
    /** Event priority must be set to priority used for moving checks due using moving data from there */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerMove(final PlayerMoveEvent event) {
        final Location from = event.getFrom();
        final Location to = event.getTo();
        final boolean PoYdiff = from.getPitch() != to.getPitch() || from.getYaw() != to.getYaw();
        final IPlayerData pData = DataManager.getPlayerData(event.getPlayer());
        if (MovingUtil.hasScheduledPlayerSetBack(event.getPlayer())) {
            // On 1.8 set back technique is different, as we do not cancel the event.
            return;
        }
        final CombinedData cData = pData.getGenericInstance(CombinedData.class);
        final InventoryConfig cc = pData.getGenericInstance(InventoryConfig.class);
        final Inventory inv = event.getPlayer().getOpenInventory().getTopInventory();
        if (moreInv.isEnabled(event.getPlayer(), pData) 
            && moreInv.check(event.getPlayer(), cData, pData, inv.getType(), inv, PoYdiff)) {
            for (int i = 1; i <= 4; i++) {
                final ItemStack item = inv.getItem(i);
                // Ensure air-clicking is not detected... :)
                if (item != null && !BlockProperties.isAir(item.getType())) {
                    // NOTE: dropItemsNaturally does not fire InvDrop events, so don't use it here. Simply close the inventory,
                    event.getPlayer().closeInventory();
                    if (pData.isDebugActive(CheckType.INVENTORY_MOREINVENTORY)) {
                        debug(event.getPlayer(), "On PlayerMoveEvent: force-close inventory on MoreInv detection.");
                    }
                    break;
                }
            }
        }
        // Determine if the inventory should be closed.
        if (cc.openCancelOnMove && !pData.hasBypass(CheckType.INVENTORY_OPEN, event.getPlayer())) {
            if (InventoryUtil.hasAnyInventoryOpen(event.getPlayer()) && open.checkOnMove(event.getPlayer(), pData)) {
                event.getPlayer().closeInventory(); // Do not call open.check() here.
                if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
                    debug(event.getPlayer(), "Player is actively moving: force-close open inventory.");
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        // NOTE: Data is reset regardless of cancellation state (better safe than sorry)
        final IPlayerData pData = DataManager.getPlayerData(event.getPlayer());
        if (open.check(event.getPlayer())) {
            if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
                debug(event.getPlayer(), "Force-close inventory on teleporting.");
            }
        }
    }

    @Override
    public void playerJoins(Player player) {
        // Just to be sure...
        final IPlayerData pData = DataManager.getPlayerData(player);
        final InventoryData data = pData.getGenericInstance(InventoryData.class);
        data.inventoryOpenTime = 0;
        if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
            debug(player, "Reset inventory timings data on join.");
        }
    }

    @Override
    public void playerLeaves(Player player) {
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (open.check(player)) {
            if (pData.isDebugActive(CheckType.INVENTORY_OPEN)) {
                debug(player, "Force-close inventory on leaving the server.");
            }
        }
    }

    /**
     * Debug inventory classes. Contains information about classes, to indicate
     * if cross-plugin compatibility issues can be dealt with easily.
     * 
     * @param player
     * @param slot
     * @param event
     */
    private void outputDebugInventoryClick(final Player player, final int slot, final InventoryClickEvent event, 
                                           final String action) {
        // TODO: Consider only logging where different from expected (CraftXY, more/other viewer than player). 

        final StringBuilder builder = new StringBuilder(512);
        final InventoryData data = DataManager.getPlayerData(player).getGenericInstance(InventoryData.class);
        builder.append("Inventory click: slot: " + slot);
        builder.append(" , Inventory has been opened for: " + MathUtil.toSeconds(System.currentTimeMillis() - data.inventoryOpenTime) + " secs");
        builder.append(" , Time between inventory click and last interaction time: " + MathUtil.toSeconds(data.lastClickTime - data.containerInteractTime) + " secs");

        // Viewers.
        builder.append(" , Viewers: ");
        for (final HumanEntity entity : event.getViewers()) {
            builder.append(entity.getName());
            builder.append("(");
            builder.append(entity.getClass().getName());
            builder.append(")");
        }

        // Inventory view.
        builder.append(" , View: ");
        final InventoryView view = event.getView();
        builder.append(view.getClass().getName());

        // Bottom inventory.
        addInventory(view.getBottomInventory(), view, " , Bottom: ", builder);

        // Top inventory.
        addInventory(view.getBottomInventory(), view, " , Top: ", builder);
        
        if (action != null) {
            builder.append(" , Action: ");
            builder.append(action);
        }

        // Event class.
        builder.append(" , Event: ");
        builder.append(event.getClass().getName());

        // Log debug.
        debug(player, builder.toString());
    }

    private void addInventory(final Inventory inventory, final InventoryView view, final String prefix, final StringBuilder builder) {
        builder.append(prefix);
        if (inventory == null) {
            builder.append("(none)");
        }
        else {
            String name = view.getTitle();
            builder.append(name);
            builder.append("/");
            builder.append(inventory.getClass().getName());
        }
    }
}
