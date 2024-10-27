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
import org.bukkit.entity.HumanEntity;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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
import fr.neatmonster.nocheatplus.compat.BridgeMaterial;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
import fr.neatmonster.nocheatplus.components.NoCheatPlusAPI;
import fr.neatmonster.nocheatplus.components.registry.order.RegistrationOrder.RegisterMethodWithOrder;
import fr.neatmonster.nocheatplus.event.mini.MiniListener;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.InventoryUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;

/**
 * <p>Adapter for listening to packets and Bukkit events relevant to item use.</p>
 *
 * On versions 1.17 and above, the adapter will only register events related to a specific item-use exploit, 
 * as Bukkit provides the {@link HumanEntity#getItemInUse()} method.<br>
 *
 * <p>On versions 1.12 and above, up to 1.17, all listeners will be registered because Bukkit does not offer a way to 
 * determine <em>which</em> item is in use; we can only detect if a player is using an item through {@link HumanEntity#isHandRaised()}.</p>
 *
 * The {@link PlayerRespawnEvent} is always listened to in order to fix a specific item desynchronization issue.
 */
public class UseItemAdapter extends BaseAdapter {
    
    private final static String dftag = "system.nocheatplus.useitemadapter";
        
    // All relevant Bukkit events for item-use.
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
    };

    private static int timeBetweenRL = 70;

    private static PacketType[] initPacketTypes() {
        // Only interested in these two packets.
        final List<PacketType> types = new LinkedList<PacketType>(Arrays.asList(PacketType.Play.Client.BLOCK_DIG, PacketType.Play.Client.BLOCK_PLACE));
        return types.toArray(new PacketType[types.size()]);
    }

    public UseItemAdapter(Plugin plugin) {
        super(plugin, ListenerPriority.MONITOR, initPacketTypes());
        final NoCheatPlusAPI api = NCPAPIProvider.getNoCheatPlusAPI();
        if (!BridgeMisc.hasGetItemInUseMethod()) {
            // getItemInUse() isn't there, register all events
            if (Bridge1_13.hasPlayerRiptideEvent()) {
                // Only register this one if available.
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
            // ...All the other listeners
            for (final MiniListener<?> listener : miniListeners) {
                api.addComponent(listener, false);
            }
        }
        // Always register this event, because it is used to fix an MC/Bukkit glitch, not for item-use setting.
        final MiniListener<?> respawningListener = new MiniListener<PlayerRespawnEvent>() {
            @EventHandler(priority = EventPriority.MONITOR)
            @RegisterMethodWithOrder(tag = dftag)
            @Override
            public void onEvent(final PlayerRespawnEvent event) {
                onRespawning(event);
            }
        };
        api.addComponent(respawningListener, false);
    }

    @Override
    public void onPacketReceiving(final PacketEvent event) {
        // Fast return tests first.
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
        if (BridgeMisc.hasGetItemInUseMethod()) {
            // getItemInUse() is already there. Only check the digging packet for exploitation prevention purposes.
            if (event.getPacketType().equals(PacketType.Play.Client.BLOCK_DIG)) {
                handleDiggingPacket(event);
            }
            // Don't need to do anything else.
            return;
        }
        
        // getItemInUse() ins't there: do the ordinary stuff.
        if (event.getPacketType().equals(PacketType.Play.Client.BLOCK_DIG)) {
            handleDiggingPacket(event);
        }
        else handleBlockPlacePacket(event);
    }
    
    /** Attempt to fix server-side-only blocking after respawn. */
    private static void onRespawning(final PlayerRespawnEvent e) {
        final IPlayerData pData = DataManager.getPlayerData(e.getPlayer());
        // Patch up issues on Bukkit's (Minecraft's ?) side (item has been reset already on death).
        if (Bridge1_9.hasGetItemInOffHand() && e.getPlayer().isBlocking()) {
            pData.requestItemUseResync();
        }
    }

    private static void onTridentRelease(final PlayerRiptideEvent e) {
        final IPlayerData pData = DataManager.getPlayerData(e.getPlayer());
        // This event is called on releasing the trident (See ItemTrident.java), so the item is not in use anymore.
        pData.setItemInUse(null);
    }

    private static void onItemConsume(final PlayerItemConsumeEvent e) {
        final Player p = e.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(p);
        // Consume(d) (!!), so the player isn't using the item anymore
        pData.setItemInUse(null);   
    }

    private static void onInventoryOpen(final InventoryOpenEvent e) {
        if (e.isCancelled()) return;
        final Player p = (Player) e.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(p);
        // Can't use item with an inventory open.
        pData.setItemInUse(null);
    }

    private static void onDeath(final PlayerDeathEvent e) {
        final IPlayerData pData = DataManager.getPlayerData((Player) e.getEntity());
        // Can't use item if dead
        pData.setItemInUse(null);  
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
        // Do reset the flag at the start (!)
        data.mightUseItem = false;

        if (e.useItemInHand().equals(Event.Result.DENY)) {
            // A plugin denied the item-use.
            return;
        }

        if (p.getGameMode() == GameMode.CREATIVE) {
            // TODO: If merging SurvivalFly and CreativeFly, creative mode needs to be taken into consideration as well.
            pData.setItemInUse(null);
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
                    // Can be consumed regardless of hunger level
                    pData.setItemInUse(m);
                    data.offHandUse = Bridge1_9.hasGetItemInOffHand() && e.getHand() == EquipmentSlot.OFF_HAND;
                    return;
                }
                if (item.getType().isEdible() && p.getFoodLevel() < 20) {
                    // Hunger level dependant consumables.
                    pData.setItemInUse(m); 
                    data.offHandUse = Bridge1_9.hasGetItemInOffHand() && e.getHand() == EquipmentSlot.OFF_HAND;
                    return;
                }
            }
            // Bows...
            if (m == Material.BOW && InventoryUtil.hasArrow(p.getInventory(), false)) {
                pData.setItemInUse(m);
                data.offHandUse = Bridge1_9.hasGetItemInOffHand() && e.getHand() == EquipmentSlot.OFF_HAND;
                return;
            }
            // Shields and swords (legacy)... Bukkit takes care here, but set anyway.
            if (ServerVersion.isAtLeast("1.9") && pData.getClientVersion().isAtLeast(ClientVersion.V_1_9)) {
                // Both server and client are on a version that support shield (thus, sword-blocking is not possible)
            	if (m == Material.SHIELD) {
            	    pData.setItemInUse(m);
                    data.offHandUse = e.getHand() == EquipmentSlot.OFF_HAND;
                    return;
            	}
            }
            else if (MaterialUtil.isSword(m) && (ServerVersion.isLowerThan("1.9") || pData.getClientVersion().isAtMost(ClientVersion.V_1_8))) {
            	// Legacy server. Or legacy client on a modern server. Blocking is done with swords in both cases.
            	pData.setItemInUse(m);
            	// (Off hand doesn't exist)
            	return;
            }
            
            // Crosswbows...
            if (m.toString().equals("CROSSBOW")) {
                if (!((CrossbowMeta) item.getItemMeta()).hasChargedProjectiles() && InventoryUtil.hasArrow(p.getInventory(), true)) {
                    pData.setItemInUse(m);
                    data.offHandUse = e.getHand() == EquipmentSlot.OFF_HAND;
                }
            }
            // Tridents (1.13)... 
            if (m == BridgeMaterial.TRIDENT && item.getDurability() < m.getMaxDurability() - 1 && pData.getClientVersion().isAtLeast(ClientVersion.V_1_13)
            	&& (
                    // 1: If the trident has riptide enchant, it can only be used in rain or water 
            		BridgeEnchant.getRiptideLevel(p) > 0.0
                    && (
                        // 2: In water
                        BlockProperties.isInWater(p, p.getLocation(), mCC.yOnGround)
                        // 2: Always check raining first, since canSeeSky is a much more expensive call. Needs optimization.
                        || p.getLocation().getWorld().hasStorm() && BlockProperties.canSeeSky(p, p.getLocation(), mCC.yOnGround) 
                    ) 
                    // 1: Otherwise, the trident can be used and thrown anywhere
                    || BridgeEnchant.getRiptideLevel(p) <= 0.0
                )) {
                pData.setItemInUse(m);
                data.offHandUse = e.getHand() == EquipmentSlot.OFF_HAND;
                return;
            }
            // Spyglass (1.17)...
            if (BridgeMisc.hasIsFrozen() && m == Material.SPYGLASS) {
                pData.setItemInUse(m);
                data.offHandUse = e.getHand() == EquipmentSlot.OFF_HAND;
                return;
            }
            // Lastly, goat horns (1.19)...
            if (m.toString().equals("GOAT_HORN")) {
                pData.setItemInUse(m);
                data.offHandUse = e.getHand() == EquipmentSlot.OFF_HAND;
                return;
            }
        } 
        else {
            // No item in hands, no deal.
            pData.setItemInUse(null);  
        }
    }
    
    // NOTE: Bukkit takes care of invalid slot values (< 0 or > 8. PlayerConnection.java), so we don't need to check for this as well.
    private static void onChangeSlot(final PlayerItemHeldEvent e) {
        final Player p = e.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(p);
        // Switching a slot, so the item use status is reset.
        final MovingData mData = pData.getGenericInstance(MovingData.class);
        final CombinedData cData = pData.getGenericInstance(CombinedData.class);
        if (e.getPreviousSlot() != e.getNewSlot()) {
            /*if ((pData.getItemInUse() != null || p.isBlocking()) && mData.playerMoves.getCurrentMove() != null) {
                p.getInventory().setHeldItemSlot(e.getPreviousSlot());
                cData.invalidItemUse = true;
            }*/
            // To avoid any more potential bypasses, let Minecraft forcibly release the item.
            pData.requestItemUseResync();
        }
    }

    private void handleBlockPlacePacket(PacketEvent event) {
        final Player p = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(p);
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        final PacketContainer packet = event.getPacket();
        final StructureModifier<Integer> ints = packet.getIntegers();
        // Legacy: pre 1.9
        if (ints.size() > 0 && !ServerVersion.isAtLeast("1.9")) {
            final int faceIndex = ints.read(0); // arg 3 if 1.7.10 below
            if (faceIndex <= 5) {
                data.mightUseItem = false;
                return;
            }
        }
        // Player sends a block-placement item when right-clicking, so they might be actually using an item. Set the flag.
        if (!event.isCancelled()) data.mightUseItem = true;
    }

    private void handleDiggingPacket(PacketEvent event) {
        Player p = event.getPlayer();       
        final IPlayerData pData = DataManager.getPlayerDataSafe(p);
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        PlayerDigType digtype = event.getPacket().getPlayerDigTypes().read(0);
        if (!BridgeMisc.hasGetItemInUseMethod()) {
            // DROP_ALL_ITEMS when dead?
            if (digtype == PlayerDigType.DROP_ALL_ITEMS || digtype == PlayerDigType.DROP_ITEM) {
                pData.setItemInUse(null);
            }
        }
        
        if (digtype == PlayerDigType.RELEASE_USE_ITEM) {
            pData.setItemInUse(null);
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
