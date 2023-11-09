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
package fr.neatmonster.nocheatplus.checks.net.protocollib;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerDigType;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.combined.CombinedData;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.BridgeEnchant;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
import fr.neatmonster.nocheatplus.components.NoCheatPlusAPI;
import fr.neatmonster.nocheatplus.components.registry.event.IGenericInstanceHandle;
import fr.neatmonster.nocheatplus.components.registry.order.RegistrationOrder.RegisterMethodWithOrder;
import fr.neatmonster.nocheatplus.event.mini.MiniListener;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.InventoryUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;


/**
 * Adapter for listening to packets and events relevant for item use.
 * (Minecraft nor Bukkit provide a method to know if the player is using an item, so we have to do it ourselves)
 */
public class UseItemAdapter extends BaseAdapter {

    private final boolean ServerIsAtLeast1_9 = ServerVersion.compareMinecraftVersion("1.9") >= 0;
    
    private final static String dftag = "system.nocheatplus.useitemadapter";
    
    private final static IGenericInstanceHandle<MCAccess> mcAccess = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstanceHandle(MCAccess.class);

    private final static MiniListener<?>[] miniListeners = new MiniListener<?>[] {
        new MiniListener<PlayerItemConsumeEvent>() {
            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            @RegisterMethodWithOrder(tag = dftag)
            @Override
            public void onEvent(final PlayerItemConsumeEvent event) {
                onItemConsume(event);
            }
        },
        new MiniListener<PlayerInteractEvent>() {
            @EventHandler(priority = EventPriority.MONITOR)
            @RegisterMethodWithOrder(tag = dftag)
            @Override
            public void onEvent(final PlayerInteractEvent event) {
                onItemInteract(event);
            }
        },
        new MiniListener<InventoryOpenEvent>() {
            @EventHandler(priority = EventPriority.LOWEST)
            @RegisterMethodWithOrder(tag = dftag)
            @Override
            public void onEvent(final InventoryOpenEvent event) {
                onInventoryOpen(event);
            }
        },
        new MiniListener<PlayerDeathEvent>() {
            @EventHandler(priority = EventPriority.MONITOR)
            @RegisterMethodWithOrder(tag = dftag)
            @Override
            public void onEvent(final PlayerDeathEvent event) {
                onDeath(event);
            }
        },
        new MiniListener<PlayerItemHeldEvent>() {
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            @RegisterMethodWithOrder(tag = dftag)
            @Override
            public void onEvent(final PlayerItemHeldEvent event) {
                onChangeSlot(event);
            }
        },
        //        new MiniListener<WeatherChangeEvent>() {
        //            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        //            @RegisterMethodWithOrder(tag = dftag)
        //            @Override
        //            public void onEvent(final WeatherChangeEvent event) {
        //                onWeatherChangeEvent(event);
        //            }
        //        },
         new MiniListener<PlayerRespawnEvent>() {
            @EventHandler(priority = EventPriority.MONITOR)
            @RegisterMethodWithOrder(tag = dftag)
            @Override
            public void onEvent(final PlayerRespawnEvent event) {
                onRespawning(event);
            }
        }
    };

    private static int timeBetweenRL = 70;

    private static PacketType[] initPacketTypes() {
        final List<PacketType> types = new LinkedList<PacketType>(Arrays.asList(PacketType.Play.Client.BLOCK_DIG, PacketType.Play.Client.BLOCK_PLACE));
        return types.toArray(new PacketType[types.size()]);
    }

    public UseItemAdapter(Plugin plugin) {
        super(plugin, ListenerPriority.MONITOR, initPacketTypes());
        final NoCheatPlusAPI api = NCPAPIProvider.getNoCheatPlusAPI();
        if (Bridge1_13.hasPlayerRiptideEvent()) {
        	// Do register this event as well, provided it is available
            final MiniListener<?> riptideListener = new MiniListener<PlayerRiptideEvent>() {
                @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
                @RegisterMethodWithOrder(tag = dftag)
                @Override
                public void onEvent(final PlayerRiptideEvent event) {
                   onTridentRelease(event);
               }
            };
            api.addComponent(riptideListener, false);
        }
        for (final MiniListener<?> listener : miniListeners) {
            api.addComponent(listener, false);
        }
    }

    @Override
    public void onPacketReceiving(final PacketEvent event) {
        try {
            if (event.isPlayerTemporary()) return;
        } 
        catch (NoSuchMethodError e) {
            if (event.getPlayer() == null) {
                counters.add(ProtocolLibComponent.idNullPlayer, 1);
                return;
            }
            if (DataManager.getPlayerDataSafe(event.getPlayer()) == null) {
                StaticLog.logWarning("Failed to fetch player data with " + event.getPacketType() + " for: " + event.getPlayer().toString());
                return;
            }
        }
        if (event.getPacketType().equals(PacketType.Play.Client.BLOCK_DIG)) {
            handleDiggingPacket(event);
        } 
        else {
            handleBlockPlacePacket(event);
        }
    }

    private static void onRespawning(final PlayerRespawnEvent e) {
        final IPlayerData pData = DataManager.getPlayerData(e.getPlayer());
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        data.itemInUse = null;
        // Patch up issues on Bukkit's (Minecraft's ?) side (item has been reset already).
        if (Bridge1_9.hasGetItemInOffHand() && e.getPlayer().isBlocking()) {
            redoShield(e.getPlayer());
        }
    }

    private static void onTridentRelease(final PlayerRiptideEvent e) {
        final IPlayerData pData = DataManager.getPlayerData(e.getPlayer());
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        // This event is called on releasing the trident (See ItemTrident.java), so the item is not in use anymore.
        pData.setUsingItem(false);
        data.itemInUse = null;
    }

    private static void onItemConsume(final PlayerItemConsumeEvent e) {
        final Player p = e.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(p);
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        // Consume(d) (!!), so the player isn't using the item anymore
        pData.setUsingItem(false);   
        data.itemInUse = null;    
    }

    private static void onInventoryOpen(final InventoryOpenEvent e) {
        if (e.isCancelled()) return;
        final Player p = (Player) e.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(p);
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        // Can't use item with an inventory open.
        pData.setUsingItem(false);
        data.itemInUse = null;         
    }

    private static void onDeath(final PlayerDeathEvent e) {
        final IPlayerData pData = DataManager.getPlayerData((Player) e.getEntity());
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        // Can't use item if dead
        pData.setUsingItem(false);  
        data.itemInUse = null;  
    }

    @SuppressWarnings("deprecation")
	private static void onItemInteract(final PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final Player p = e.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(p);
        final MovingConfig mCC = pData.getGenericInstance(MovingConfig.class);
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        data.offHandUse = false;
        if (!data.mightUseItem) {
            // Player did not send the block-placement packet, so they are for sure not using anything, return.
            return;
        }
        data.mightUseItem = false;

        if (e.useItemInHand().equals(Event.Result.DENY)) {
            // A plugin denied the item-use.
            return;
        }

        if (p.getGameMode() == GameMode.CREATIVE) {
            // TODO: If merging SurvivalFly with CreativeFly, this needs to be removed.
            pData.setUsingItem(false);
            data.itemInUse = null;
            return;
        }
        
        // Proceed to check...
        if (e.hasItem()) {
            final ItemStack item = e.getItem();
            final Material m = item.getType();
            if (Bridge1_9.hasElytra() && p.hasCooldown(m)) {
                // Item is in cooldown so it cannot be used.
                return;
            }
            
            // Edible / consumable items...
            if (InventoryUtil.isConsumable(item)) {
                if (!Bridge1_9.hasElytra() && item.getDurability() > 16384) {
                    // pre1.9 splash potion edge case.
                    return;
                }
                if (m == Material.POTION || m == Material.MILK_BUCKET || m.toString().endsWith("_APPLE") || m.name().startsWith("HONEY_BOTTLE")) {
                    pData.setUsingItem(true);
                    data.offHandUse = Bridge1_9.hasGetItemInOffHand() && e.getHand() == EquipmentSlot.OFF_HAND;
                    data.itemInUse = m;
                    return;
                }
                if (item.getType().isEdible() && p.getFoodLevel() < 20) {
                    pData.setUsingItem(true); 
                    data.offHandUse = Bridge1_9.hasGetItemInOffHand() && e.getHand() == EquipmentSlot.OFF_HAND;
                    data.itemInUse = m;
                    return;
                }
            }
            // Bows...
            if (m == Material.BOW && InventoryUtil.hasArrow(p.getInventory(), false)) {
                pData.setUsingItem(true);
                data.offHandUse = Bridge1_9.hasGetItemInOffHand() && e.getHand() == EquipmentSlot.OFF_HAND;
                data.itemInUse = m;
                return;
            }
            // Shields... Bukkit takes care here, no need to set the flag.
            if (Bridge1_9.hasElytra() && m == Material.SHIELD) {
                //data.isUsingItem = true;
                data.offHandUse = e.getHand() == EquipmentSlot.OFF_HAND;
                data.itemInUse = m;
                return;
            }
            // Crosswbows...
            if (m.toString().equals("CROSSBOW")) {
                if (!((CrossbowMeta) item.getItemMeta()).hasChargedProjectiles() && InventoryUtil.hasArrow(p.getInventory(), true)) {
                    pData.setUsingItem(true);
                    data.offHandUse = e.getHand() == EquipmentSlot.OFF_HAND;
                    data.itemInUse = m;
                }
            }
            // Tridents... Only if the player is in water or exposed to rain.
            if (Bridge1_13.hasIsRiptiding() && m == Material.TRIDENT && BridgeEnchant.getRiptideLevel(p) > 0.0
                && (
                    BlockProperties.isInWater(p, p.getLocation(), mCC.yOnGround)
                    // Always check raining first, since canSeeSky is a much more expensive call. Needs optimiziation.
                    || p.getLocation().getWorld().hasStorm() && BlockProperties.canSeeSky(p, p.getLocation(), mCC.yOnGround) 
                )) {
                pData.setUsingItem(true);
                data.offHandUse = e.getHand() == EquipmentSlot.OFF_HAND;
                data.itemInUse = m;
                return;
            }
            // TODO: Goat horns... Those exist, apparently.
        } 
        // No item in hands, no deal.
        else {
            pData.setUsingItem(false);  
            data.itemInUse = null;     
        }
    }

    private static void onChangeSlot(final PlayerItemHeldEvent e) {
        final Player p = e.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(p);
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        //if (data.changeslot) {
        //    p.getInventory().setHeldItemSlot(data.olditemslot);
        //    data.changeslot = false;
        //}
        // Switching a slot, so the item use status is reset. 
        // TODO: Check for abuses.
        if (e.getPreviousSlot() != e.getNewSlot()) {
            pData.setUsingItem(false);
            data.itemInUse = null;
        }
    }

    private void handleBlockPlacePacket(PacketEvent event) {
        final Player p = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(p);
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        final PacketContainer packet = event.getPacket();
        final StructureModifier<Integer> ints = packet.getIntegers();
        // Legacy: pre 1.9
        if (ints.size() > 0 && !ServerIsAtLeast1_9) {
            final int faceIndex = ints.read(0); // arg 3 if 1.7.10 below
            if (faceIndex <= 5) {
                data.mightUseItem = false;
                data.itemInUse = null;
                return;
            }
        }
        // Player sends a block-placement item when right clicking, so they might be actually using an item. Set the flag.
        if (!event.isCancelled()) data.mightUseItem = true;
    }

    private void handleDiggingPacket(PacketEvent event) {
        Player p = event.getPlayer();       
        final IPlayerData pData = DataManager.getPlayerDataSafe(p);
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        final MovingData mData = pData.getGenericInstance(MovingData.class);
        PlayerDigType digtype = event.getPacket().getPlayerDigTypes().read(0);
        // DROP_ALL_ITEMS when dead?
        if (digtype == PlayerDigType.DROP_ALL_ITEMS || digtype == PlayerDigType.DROP_ITEM) {
            pData.setUsingItem(false);
            data.itemInUse = null;
        }
        
        //Advanced check
        if (digtype == PlayerDigType.RELEASE_USE_ITEM) {
            pData.setUsingItem(false);
            data.itemInUse = null;
            long now = System.currentTimeMillis();
            if (data.releaseItemTime != 0) {
                if (now < data.releaseItemTime) {
                    data.releaseItemTime = now;
                    return;
                }
                if (data.releaseItemTime + timeBetweenRL > now) {
                    data.isHackingRI = true;
                }
            }
            data.releaseItemTime = now;
        }
    }

    /**
     * Attempt to fix server-side-only blocking after respawn.
     * 
     * @param player
     */
    private static void redoShield(final Player player) {
        if (mcAccess.getHandle().resetActiveItem(player)) {
            // Handle via NMS: force-release the item.
            return;
        }
        // Handle via Bukkit: attempt to refresh the item
        // Does not work: DataManager.getPlayerData(player).requestUpdateInventory();
        final PlayerInventory inv = player.getInventory();
        ItemStack stack = inv.getItemInOffHand();
        if (stack != null && stack.getType() == Material.SHIELD) {
            // Shield in off-hand.
            inv.setItemInOffHand(stack);
            return;
        }
        stack = inv.getItemInMainHand();
        if (stack != null && stack.getType() == Material.SHIELD) {
            // Shield in main-hand.
            inv.setItemInMainHand(stack);
            return;
        }
    }

    /**
     * Set Minimum time between RELEASE_USE_ITEM packet is sent.
     * If time lower this value, A check will flag
     * Should be set from 51-100. Larger number, more protection more false-positive
     * 
     * @param time milliseconds
     */ 
    public static void setuseRLThreshold(int time) {
        timeBetweenRL = time;
    }   
}
