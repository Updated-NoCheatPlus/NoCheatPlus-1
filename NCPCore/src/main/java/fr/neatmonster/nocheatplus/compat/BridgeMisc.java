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
package fr.neatmonster.nocheatplus.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.entity.Projectile;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;

import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.compat.bukkit.BridgeMaterial;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;


/**
 * Various bridge methods not enough for an own class.
 * @author asofold
 *
 */
public class BridgeMisc {

    private static GameMode getSpectatorGameMode() {
        try {
            return GameMode.SPECTATOR;
        } 
        catch (Throwable t) {}
        return null;
    }

    public static final GameMode GAME_MODE_SPECTATOR = getSpectatorGameMode();

    private static final Method Bukkit_getOnlinePlayers = ReflectionUtil.getMethodNoArgs(Bukkit.class, "getOnlinePlayers");

    private static final boolean hasIsFrozen = ReflectionUtil.getMethodNoArgs(LivingEntity.class, "isFrozen", boolean.class) != null;

    private static final boolean hasGravityMethod = ReflectionUtil.getMethodNoArgs(LivingEntity.class, "hasGravity", boolean.class) != null;
    // introduced roughly in 1.12, javadocs are quite confusing. If you check the code (CraftHumanEntity.java), you'll see that this just calls getHandle.isUsingItem()
    private static final boolean hasIsHandRaisedMethod = ReflectionUtil.getMethodNoArgs(HumanEntity.class, "isHandRaised", boolean.class) != null;
    // introduced in 1.17, roughly.
    private static final boolean hasGetItemInUseMethod = ReflectionUtil.getMethodNoArgs(HumanEntity.class, "getItemInUse", ItemStack.class) != null;
    // introduced in 1.14
    private static final boolean hasEntityChangePoseEvent = ReflectionUtil.getClass("org.bukkit.event.entity.EntityPoseChangeEvent") != null;
    // After 15 or so years, we finally get to know when players press WASD keys... Better late than never I guess.
    private static final boolean hasPlayerInputEvent = ReflectionUtil.getClass("org.bukkit.event.player.PlayerInputEvent") != null;
    private static final boolean hasInputGetterMethod = ReflectionUtil.getMethodNoArgs(Player.class, "getCurrentInput", boolean.class) != null;
    
    public static final boolean hasInputGetterMethod() {
        return hasInputGetterMethod;
    }
    
    /**
     * Test if the player's input (WASD) can be known (server has the getter method and the client is at or above 1.21.2)
     * @param player
     * @return
     */
    public static final boolean isInputKnown(final Player player) {
        return hasInputGetterMethod() && DataManager.getPlayerData(player).getClientVersion().isAtLeast(ClientVersion.V_1_21_2);
    }
    public static boolean hasPlayerInputEvent() {
        return hasPlayerInputEvent;
    }

    public static boolean hasEntityChangePoseEvent() {
        return hasEntityChangePoseEvent;
    }

    public static boolean hasGetItemInUseMethod() {
        return hasGetItemInUseMethod;
    }

    public static boolean hasIsHandRaisedMethod() {
        return hasIsHandRaisedMethod;
    }
    
    public static boolean hasAnyUsingItemMethod() {
        return hasGetItemInUseMethod || hasIsHandRaisedMethod;
    }
    
    /**
     * Check if the player is riptiding and gliding at the same time.
     * 
     * @param player
     * @return True if so.
     */
    public static boolean isRipgliding(final Player player) {
        return Bridge1_9.isGliding(player) && Bridge1_13.isRiptiding(player);
    }
    
    /**
     * Check if the player is in crawl mode according to Minecraft's definition
     * (In swimming pose while not in water).
     * 
     * @param player
     * @return True if so.
     */
    public static boolean isVisuallyCrawling(Player player) {
        if (!hasEntityChangePoseEvent()) {
            // Can't know without accessing NMS.
            return false;
        }
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (pData.getClientVersion().isLowerThan(ClientVersion.V_1_14)) {
            // Can't possibly be crawling.
            return false;
        }
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        return player.getPose().equals(Pose.SWIMMING) && !BlockProperties.isInWater(player, player.getLocation(), cc.yOnGround);
    }
    
    /**
     * Compatibility method to check for item-use, by using the first available method:
     * 
     * <li>{@link HumanEntity#isBlocking()}</li>
     * <li>{@link HumanEntity#getItemInUse()}</li> 
     * <li>{@link HumanEntity#isHandRaised()}</li>
     * <p>Otherwise, fall back to our adapter (see UseItemAdapter)</p>
     * <li>{@link IPlayerData#getItemInUse()}.</li>
     * 
     * @param player
     */
    public static boolean isUsingItem(final Player player) {
    	if (player.isBlocking()) {
            // Test blocking first, since this method has been available since forever.
    		return true;
    	}
        if (hasGetItemInUseMethod()) {
            // 1.17+ 
            return player.getItemInUse() != null;
        }
        if (hasIsHandRaisedMethod()) {
            // 1.12+ test using isHandRaised (naming convention is quite bad... Why not simply naming it isUsingItem() ?)
            return player.isHandRaised();
        }
        // Very old server (1.11 and below), use NCP's adapter.
        final IPlayerData pData = DataManager.getPlayerData(player);
        return pData.getItemInUse() != null;
    }

    public static Material getItemInUse(final Player player) {
    	final IPlayerData pData = DataManager.getPlayerData(player);
        return hasGetItemInUseMethod() ? player.getItemInUse().getType() : pData.getItemInUse();
    }
    
    /**
     * By default, this returns true if the {@link LivingEntity#hasGravity()} method isn't available.
     * @param entity
     * @return
     */
    public static boolean hasGravity(final LivingEntity entity) {
        return hasGravityMethod ? entity.hasGravity() : true;
    }

    public static boolean hasIsFrozen() {
        return hasIsFrozen;
    }

    public static float getPercentFrozen(Player player) {
        if (!hasIsFrozen()) {
            return 0.0f;
        }
        if (!canFreeze(player)) {
            return 0.0f;
        }
        int MAX_TICKS = player.getMaxFreezeTicks();

        return (float) Math.min(player.getFreezeTicks(), MAX_TICKS) / (float) MAX_TICKS;
    }

    public static float getPowderSnowSlowdown(Player player) {
        return (-0.05F * getPercentFrozen(player));
    }

    public static boolean isFullyFrozen(Player player) {
        return player.getFreezeTicks() >= player.getMaxFreezeTicks();
    }

    /**
     * Test if the player has any piece of leather armor on 
     * which will prevent freezing.
     * 
     * @param player
     * @return
     */
    public static boolean canFreeze(final Player player) {
        if (!hasIsFrozen()) {
            return false;
        }
        final PlayerInventory inv = player.getInventory();
        final ItemStack[] contents = inv.getArmorContents();
        for (final ItemStack armor : contents) {
            if (armor != null && armor.getType().toString().startsWith("LEATHER")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test if the player is equipped with leather boots.
     * 
     * @param player
     * @return
     */
    public static boolean canStandOnPowderSnow(final Player player) {
        if (!hasIsFrozen()) {
            return false;
        }
        if (DataManager.getPlayerData(player).getClientVersion().isLowerThan(ClientVersion.V_1_17)) {
            return false;
        }
        final ItemStack boots = player.getInventory().getBoots();
        return boots != null && boots.getType() == Material.LEATHER_BOOTS;
    }

    /**
     * Correction of position: Used for ordinary setting back. <br>
     * NOTE: Currently it's not distinguished, if we do it as a proactive
     * measure, or due to an actual check violation.
     */
    public static final TeleportCause TELEPORT_CAUSE_CORRECTION_OF_POSITION = TeleportCause.UNKNOWN;

    /**
     * Return a shooter of a projectile if we get an entity, null otherwise.
     */
    public static Player getShooterPlayer(Projectile projectile) {
        Object source;
        try {
            source = projectile.getClass().getMethod("getShooter").invoke(projectile);
        } 
        catch (IllegalArgumentException | SecurityException | IllegalAccessException 
                | InvocationTargetException | NoSuchMethodException ignored) {
            return null;
        }
        if (source instanceof Player) {
            return (Player) source;
        } else {
            return null;
        }
    }

    /**
     * Retrieve a player from projectiles or cast to player, if possible.
     * @param damager
     * @return
     */
    public static Player getAttackingPlayer(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        } else if (damager instanceof Projectile) {
            return getShooterPlayer((Projectile) damager);
        } else {
            return null;
        }
    }

    /**
     * Get online players as an array (convenience for reducing IDE markers :p).
     * @return
     */
    public static Player[] getOnlinePlayers() {
        try {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            return players.isEmpty() ? new Player[0] : players.toArray(new Player[players.size()]);
        }
        catch (NoSuchMethodError e) {}
        if (Bukkit_getOnlinePlayers != null) {
            Object obj = ReflectionUtil.invokeMethodNoArgs(Bukkit_getOnlinePlayers, null);
            if (obj != null && (obj instanceof Player[])) {
                return (Player[]) obj;
            }
        }
        return new Player[0];
    }

    /**
     * Test side conditions for fireworks boost with elytra on, for interaction
     * with the item in hand. Added with Minecraft 1.11.2.
     * 
     * @param player
     * @param materialInHand
     *            The type of the item used with interaction.
     * @return
     */
    public static boolean maybeElytraBoost(final Player player, final Material materialInHand) {
        return DataManager.getPlayerData(player).getClientVersion().isAtLeast(ClientVersion.V_1_11_1) && BridgeMaterial.FIREWORK_ROCKET != null && materialInHand == BridgeMaterial.FIREWORK_ROCKET && Bridge1_9.isGlidingWithElytra(player);
    }

    /**
     * Get the power for a firework(s) item (stack).
     * 
     * @param item
     * @return The power. Should be between 0 and 127 including edges, in case
     *         of valid items, according to javadocs (Spigot MC 1.11.2). In case
     *         the item is null or can't be judged, -1 is returned.
     */
    public static int getFireworksPower(final ItemStack item) {
        if (item == null || item.getType() != BridgeMaterial.FIREWORK_ROCKET) {
            return -1;
        }
        final ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof FireworkMeta)) { // INDIRECT: With elytra, this already exists.
            return -1;
        }
        final FireworkMeta fwMeta = (FireworkMeta) meta;
        return fwMeta.getPower();
    }
}
