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

import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPoseChangeEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.CheckListener;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveInfo;
import fr.neatmonster.nocheatplus.checks.moving.velocity.VelocityFlags;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.components.NoCheatPlusAPI;
import fr.neatmonster.nocheatplus.components.data.ICheckData;
import fr.neatmonster.nocheatplus.components.data.IData;
import fr.neatmonster.nocheatplus.components.registry.factory.IFactoryOne;
import fr.neatmonster.nocheatplus.components.registry.feature.JoinLeaveListener;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.players.PlayerFactoryArgument;
import fr.neatmonster.nocheatplus.stats.Counters;
import fr.neatmonster.nocheatplus.utilities.TickTask;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.moving.AuxMoving;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;
import fr.neatmonster.nocheatplus.worlds.WorldFactoryArgument;

/**
 * Class to combine some things, make available for other checks, or just because they don't fit into another section.<br>
 * This is registered before the FightListener.
 * Do note the registration order in fr.neatmonster.nocheatplus.NoCheatPlus.onEnable (within NCPPlugin).
 * 
 * @author asofold
 *
 */
public class CombinedListener extends CheckListener implements JoinLeaveListener {

    protected final Improbable improbable = addCheck(new Improbable());

    protected final MunchHausen munchHausen = addCheck(new MunchHausen());

    private final Counters counters = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(Counters.class);

    private final int idFakeInvulnerable = counters.registerKey("fakeinvulnerable");

    /** Location for temporary use with getLocation(useLoc). Always call setWorld(null) after use. Use LocUtil.clone before passing to other API. */
    final Location useLoc = new Location(null, 0, 0, 0); 

    /** Auxiliary functionality. */
    private final AuxMoving aux = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(AuxMoving.class);

    @SuppressWarnings("unchecked")
    public CombinedListener() {
        super(CheckType.COMBINED);

        final NoCheatPlusAPI api = NCPAPIProvider.getNoCheatPlusAPI();
        // Register version/context-specific events.
        if (Bridge1_9.hasEntityToggleGlideEvent()) {
            queuedComponents.add(new Listener() {
                // Don't ignore cancelled events here (gliding is client-sided, correct me if I'm wrong).
                @EventHandler(priority = EventPriority.LOWEST)
                public void onToggleGlide(final EntityToggleGlideEvent event) {
                    final IPlayerData pData = DataManager.getPlayerData((Player)event.getEntity());
                    final MovingData data = pData.getGenericInstance(MovingData.class);
                    final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
                    // Always fake use velocity here to smoothen the transition between glide->no glide or no glide->glide transitions.
                    data.addVelocity((Player)event.getEntity(), pData.getGenericInstance(MovingConfig.class), lastMove.xAllowedDistance, lastMove.yAllowedDistance, lastMove.zAllowedDistance, VelocityFlags.FAKED);
                    if (shouldDenyGlidingStart((Player)event.getEntity(), event.isGliding(), true)) {
                        event.setCancelled(true);
                    }
                }
            });
        }
        else {
            // If -for whatever reason- the event is not available, handle toggle gliding with PMEs
            queuedComponents.add(new Listener() {
                // We can ignore the event here, if cancelled, since the player will not be able to move anyway
                @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
                public void onEventlessToggleGlide(final PlayerMoveEvent event) {
                    final PlayerMoveData lastMove = DataManager.getPlayerData(event.getPlayer()).getGenericInstance(MovingData.class).playerMoves.getFirstPastMove();
                    final PlayerMoveData thisMove = DataManager.getPlayerData(event.getPlayer()).getGenericInstance(MovingData.class).playerMoves.getCurrentMove();
                    // Assumption: we consider players toggle gliding on if they were not gliding before, and they now are.
                    if (shouldDenyGlidingStart(event.getPlayer(), thisMove.isGliding && !lastMove.isGliding, false)) {
                        // Force-stop. 
                        // IMPORTANT: DO NOT CANCEL THE EVENT HERE!
                        event.getPlayer().setGliding(false);
                    } 
                }
            });
        }
        if (BridgeMisc.hasEntityChangePoseEvent()) {
            queuedComponents.add(new Listener() {
                @EventHandler(priority = EventPriority.LOWEST)
                public void onChangingPose(final EntityPoseChangeEvent event) {
                    handlePoseChangeEvent(event.getEntity(), event.getPose());
                }
            });
        }

        // Register Data, Listener and Config.
        api.register(api.newRegistrationContext()
                // CombinedConfig
                .registerConfigWorld(CombinedConfig.class)
                .factory(new IFactoryOne<WorldFactoryArgument, CombinedConfig>() {
                    @Override
                    public CombinedConfig getNewInstance(WorldFactoryArgument arg) {
                        return new CombinedConfig(arg.worldData);
                    }
                })
                .registerConfigTypesPlayer()
                .context() //
                // CombinedData
                .registerDataPlayer(CombinedData.class)
                .factory(new IFactoryOne<PlayerFactoryArgument, CombinedData>() {
                    @Override
                    public CombinedData getNewInstance(PlayerFactoryArgument arg) {
                        return new CombinedData();
                    }
                })
                .addToGroups(CheckType.MOVING, false, IData.class, ICheckData.class)
                .removeSubCheckData(CheckType.COMBINED, true)
                .context() //
                );
    }

    /** 
     * Judge if the player can effectively start to glide.
     * 
     * @param player
     * @param toggledOn Based on an assumption, if the proper event isn't there.
     * @param isToggleGlideEvent Whether this method gets called from the actual ToggleGlideEvent or from a PlayerMoveEvent.
     * @return True, if the player cannot start glide.
     */
    private boolean shouldDenyGlidingStart(final Player player, boolean toggledOn, boolean isToggleGlideEvent) {
        if (toggledOn) {
            final PlayerMoveInfo info = aux.usePlayerMoveInfo();
            info.set(player, player.getLocation(info.useLoc), null, 0.0001); // Only restrict very near ground.
            final IPlayerData pData = DataManager.getPlayerData(player);
            final MovingData data = pData.getGenericInstance(MovingData.class);
            final boolean res = !MovingUtil.canLiftOffWithElytra(player, info.from, data);
            final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
            info.cleanup();
            aux.returnPlayerMoveInfo(info);
            // Smoothen the transition by fake using velocity.
            if (!isToggleGlideEvent && res) {
                data.addVelocity(player, pData.getGenericInstance(MovingConfig.class), lastMove.xAllowedDistance, lastMove.yAllowedDistance, lastMove.zAllowedDistance, VelocityFlags.FAKED);
            }
            if (res && pData.isDebugActive(CheckType.MOVING)) {
                debug(player, "Prevent toggle glide on (cheat prevention, " + (isToggleGlideEvent ? "ToggleGlideEvent)" : "PlayerMoveEvent)"));
            }
            return res;
        }
        // Did not toggle on.
        return false;
    }
    
    /** 
     * Check if this gliding phase should be aborted (We validate both toggle glide and gliding). 
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onGlidingPhase(final PlayerMoveEvent event) {
        final IPlayerData pData = DataManager.getPlayerData(event.getPlayer());
        final MovingData data = pData.getGenericInstance(MovingData.class);
        if (!Bridge1_9.isGliding(event.getPlayer())) {
            // No gliding, no deal.
            return;
        }
        final PlayerMoveInfo info = aux.usePlayerMoveInfo();
        info.set(event.getPlayer(), event.getPlayer().getLocation(info.useLoc), null, 0.0001);
        if (MovingUtil.canStillGlide(event.getPlayer(), info.from, data)) {
            // Nothing to do.
            info.cleanup();
            aux.returnPlayerMoveInfo(info);
            return;
        }
        // Abort this gliding phase (i.e.: the player collided with water, elytra broke mid-flight etc...).
        event.getPlayer().setGliding(false);
        info.cleanup();
        aux.returnPlayerMoveInfo(info);
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        // Smoothen the transition by fake using velocity.
        if (!Bridge1_9.hasEntityToggleGlideEvent()) {
            data.addVelocity(event.getPlayer(), pData.getGenericInstance(MovingConfig.class), lastMove.xAllowedDistance, lastMove.yAllowedDistance, lastMove.zAllowedDistance, VelocityFlags.FAKED);
        }
        if (pData.isDebugActive(CheckType.MOVING)) {
            debug(event.getPlayer(), "Abort gliding phase.");
        }
    }
    
    /**
     * Handles the discrepancy between the definitions of "sneaking" in Bukkit and Minecraft.
     * 
     * <p>
     * In Bukkit, sneaking is defined as simply pressing the shift key. Both {@link Player#isSneaking()}
     * and {@link PlayerToggleSneakEvent} are triggered by action packets ({@code PRESS/RELEASE_SHIFT_KEY}). 
     * However, in Minecraft, sneaking refers to being in the crouch or crawl <b>pose</b> 
     * (the latter introduced in 1.14. Check {@code isMovingSlowly()} method in client code).
     * </p>
     * <p>
     * Historically (up to Minecraft 1.9), a player could enter the crouching pose 
     * only by pressing the shift key. Thus, Bukkit's assumption that 
     * <b>sneaking</b> equals <b>shifting</b> was acceptable. However, this equivalence is no longer true 
     * due to Mojang's introduction of additional player actions that can alter poses:
     * </p>
     * <ul>
     *   <li><b>Elytra gliding (1.9+):</b> changes the player's pose to gliding.</li>
     *   <li><b>Same goes for swimming and riptiding (1.13+).</li>
     *   <li><b>On 1.14+, the player's bounding box is contracted when sneaking</b>, 
     *       allowing to enter into 1.5-block-high spaces and stay in the crouching pose, regardless of key presses, until the player exits such an area. 
     *       This means, a player can spam the shift key without ever leaving the pose in these areas.</li>
     *   <li><b>Crawl mode (1.14+):</b> automatically activates if the player is constricted in 
     *       areas with a ceiling lower than 1.5 blocks (e.g., under trapdoors). 
     *       When crawling, movement is always slowed down regardless of shift key presses. 
     *       Moreover, crawling shares the same pose as swimming, and Minecraft determines 
     *       crawling status by checking if the player is in the swimming pose and not in water 
     *       (see: {@code LocalPlayer.java -> aiStep() -> isMovingSlowly() -> isVisuallyCrawling()}).</li>
     * </ul>
     * <p>
     * Therefore, Bukkit's methods/event can no longer be relied upon to determine if a player should have reduced movement speed, as 
     * they are associated with shift key presses and not player poses; and we need to know this for the horizontal speed prediction
     * </p>
     * <p>
     * On Minecraft 1.14 and higher, sneaking status should be set using 
     * {@code EntityPoseChangeEvent} instead of {@code PlayerToggleSneakEvent}. 
     * For legacy clients, poses like crawling do not exist, so sneaking can be determined 
     * solely by checking for shift key presses.
     * </p>
     * <hr><br>NOTE: the pose is updated at the end of the tick</hr>
     */
    private void handlePoseChangeEvent(final Entity entity, final Pose newPose) {
        if (!(entity instanceof Player)) {
            return;
        }
        final Player player = (Player) entity;
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (pData.getClientVersion().isLowerThan(ClientVersion.V_1_14)) {
            // Sneaking status is set on PlayerToggleSneakEvents.
            return;
        }
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        if (newPose.equals(Pose.SWIMMING) && !BlockProperties.isInWater(player, player.getLocation(), cc.yOnGround)) {
            // isVisuallyCrawling()...
            pData.setIsInCrouchingPoseState(true);
            return;
        }
        if (newPose.equals(Pose.SNEAKING)) {
            // Sneaking...
            pData.setIsInCrouchingPoseState(true);
            return;
        }
        // Entered another pose...
        pData.setIsInCrouchingPoseState(false);
    }
    
    @Override
    public void playerJoins(final Player player) {
        final IPlayerData pData = DataManager.getPlayerData(player);
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        final CombinedConfig cc = pData.getGenericInstance(CombinedConfig.class);
        final boolean debug = pData.isDebugActive(checkType);
        if (cc.invulnerableCheck 
            && (cc.invulnerableTriggerAlways || cc.invulnerableTriggerFallDistance && player.getFallDistance() > 0)) {
            // TODO: maybe make a heuristic for small fall distances with ground under feet (prevents future abuse with jumping) ?
            final int invulnerableTicks = mcAccess.getHandle().getInvulnerableTicks(player);
            if (invulnerableTicks == Integer.MAX_VALUE) {
                if (debug) {
                    debug(player, "Invulnerable ticks could not be determined.");
                }
            } else {
                final int ticks = cc.invulnerableInitialTicksJoin >= 0 ? cc.invulnerableInitialTicksJoin : invulnerableTicks;
                data.invulnerableTick = TickTask.getTick() + ticks;
                mcAccess.getHandle().setInvulnerableTicks(player, 0);
            }
        }
        // Needed because a player may log in and be already crouching or crawling due to accessibility features..
        final MovingConfig mCC = pData.getGenericInstance(MovingConfig.class);
        if (BridgeMisc.hasEntityChangePoseEvent()) {
            if (player.getPose().equals(Pose.SWIMMING) && !BlockProperties.isInWater(player, player.getLocation(), mCC.yOnGround)) {
                // isVisuallyCrawling()...
                pData.setIsInCrouchingPoseState(true);
            } 
            else if (player.getPose().equals(Pose.SNEAKING)) {
                // Sneaking...
                pData.setIsInCrouchingPoseState(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        data.resetImprobableData();
        // (Let the player decide)
        // pData.setSprintingState(false);
        // pData.setCrouchingState(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final IPlayerData pData = DataManager.getPlayerData(player);
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        data.resetImprobableData();
        pData.setSprintingState(false);
        pData.setIsInCrouchingPoseState(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameModeChange(final PlayerGameModeChangeEvent event) {
        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        // If gamemode changes, then we can safely drop Improbable's data.
        data.resetImprobableData();
    }

    @Override
    public void playerLeaves(final Player player) {
        final IPlayerData pData = DataManager.getPlayerData(player);
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        data.resetImprobableData();
    }
    
    /** NOTE: Cancelling does nothing. It won't stop players from sneaking.*/
    @EventHandler(priority = EventPriority.MONITOR)
    public void onToggleShiftKey(final PlayerToggleSneakEvent event) {
        final IPlayerData pData = DataManager.getPlayerData(event.getPlayer());
        // Always set whenever the player presses the shift key.
        pData.setIsShiftKeyPressed(event.isSneaking());
        if (pData.getClientVersion().isAtLeast(ClientVersion.V_1_14) && BridgeMisc.hasEntityChangePoseEvent()) {
            // Handle via actual poses.
            return;
        }
        // Legacy client and/or server, poses are not available.
        // TODO: Actually, do legacy players (i.e.: 1.9 player) fire EntityPoseChangeEvent on newer servers?
        if (!event.isSneaking()) {
            // Player was sneaking and they now toggled it off.
            pData.setIsInCrouchingPoseState(false);
            return;
        }
        if (Bridge1_13.isSwimming(event.getPlayer()) || Bridge1_9.isGliding(event.getPlayer()) || Bridge1_13.isRiptiding(event.getPlayer())) {
            // Bukkit's ambiguous "isSneaking()" method would return true for all these cases, but like we've said above, sneaking is determined by player poses, not shift key presses. Just ignore.
            pData.setIsInCrouchingPoseState(false);
            return;
        }
        // Legacy clients can only enter the CROUCHING pose if they press the shift key, so in this case shifting does equal sneaking.
        pData.setIsInCrouchingPoseState(true);
    }

    /** NOTE: Cancelling does nothing. It won't stop players from sprinting. So, don't ignore cancelled events. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onToggleSprint(final PlayerToggleSprintEvent event) {
        final IPlayerData pData = DataManager.getPlayerData(event.getPlayer());
        final PlayerMoveData lastMove = pData.getGenericInstance(MovingData.class).playerMoves.getFirstPastMove();
        if (!event.isSprinting()) {
            // Player was sprinting and they now toggled it off.
            pData.setSprintingState(false);
            return;
        }
        ////////////////////////////////////////////////////////////////////////////////
        // Player toggled sprinting on: check if they can actually start to sprint... //
        ////////////////////////////////////////////////////////////////////////////////
        // TODO: This stuff might need to be latency compensated.
        // TODO: Wall collision
        // TODO: Attack slow-down
        if (pData.isInCrouchingPose()) {
            // ...In 1.14 and lower, players cannot sprint and sneak at the same time.
            // On 1.14 and higher, players can sneak while sprinting if they were sprinting beforehand.
            // NOTE: THE BUG ABOVE WAS FIXED WITH THE "WINTER DROP" (around 1.21.3).
            // We do not check for this as it is not worth it at all. Latency and desyncing issues make it too hard handle.
            // NOTE2: Mojang reverted the bug fix above!!
            pData.setSprintingState(false);
            return;
        }
        if (BridgeMisc.isUsingItem(event.getPlayer())) {
            // ...In 1.14 and lower, players cannot sprint and use an item at the same time.
            // On 1.14 and higher, players can use an item while sprinting if they were sprinting beforehand.
            // NOTE: THE BUG ABOVE WAS FIXED WITH THE "WINTER DROP" (around 1.21.3).
            // We do not check for this as it is not worth it at all. Latency and desyncing issues make it too hard handle.
            // NOTE2: Mojang reverted the bug fix above!!
            pData.setSprintingState(false);
            return;
        }
        if (event.getPlayer().getFoodLevel() <= 5) {
            // ...Cannot toggle sprint with low hunger.
            pData.setSprintingState(false);
            return;
        }
        if (event.getPlayer().hasPotionEffect(PotionEffectType.BLINDNESS)) {
            // ...Blindness does not break sprinting if the player receives it while already sprinting.
            // The next toggle sprint event however will set it to false.
            pData.setSprintingState(false);
            return;
        }
        if (BridgeMisc.isVisuallyCrawling(event.getPlayer())) {
            // ...You cannot toggle sprint in a crawling area, unless you enter it while already sprinting.
            pData.setSprintingState(false);
            return;
        }
        // Otherwise, legit.
        pData.setSprintingState(true);
    }
    
    /** Cancelled events can still affect movement speed, since the mechanic we're checking is client-sided, so don't skip this listener */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) 
    public void onAttackingEntities(final EntityDamageByEntityEvent event) {
        final Entity attacker = event.getDamager();
        final Entity damaged = event.getEntity();
        if (!(attacker instanceof Player)) {
            return;
        }
        if (!(damaged instanceof LivingEntity) || damaged.isDead() || !damaged.isValid()) {
            return;
        }
        // (uh... Maybe cut down boilerplate just a smidge...)
        final Player player = (Player) attacker;
        final IPlayerData pData = DataManager.getPlayerData(player);
        final MovingData data = pData.getGenericInstance(MovingData.class);
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        final ItemStack stack = Bridge1_9.getItemInMainHand(player);
        final PlayerMoveInfo moveInfo = aux.usePlayerMoveInfo();
        final Location loc =  player.getLocation(useLoc);
        // This (odd) vanilla mechanic can be found in Player/EntityHuman.java.attack()
        // If the player is sprint-attacking or is attacking with a knockback-equipped weapon, speed is slowed down and the sprinting status will reset.
        moveInfo.set(player, loc, null, cc.yOnGround);
        if (!MovingUtil.shouldCheckSurvivalFly(player, moveInfo.from, null, data, cc, pData)) {
            // Clean-up
            useLoc.setWorld(null);
            aux.returnPlayerMoveInfo(moveInfo);
            return;
        }
        if (pData.isSprinting() || !BlockProperties.isAir(stack) && stack.getEnchantmentLevel(Enchantment.KNOCKBACK) > 0) {
            final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
            thisMove.hasAttackSlowDown = true;
            if (pData.isDebugActive(CheckType.MOVING)) {
                debug(player, "Set attack slow down flag in this move.");
            }
        }
        // Clean-up
        useLoc.setWorld(null);
        aux.returnPlayerMoveInfo(moveInfo);
    }
    
    /** We listen to entity damage events for the Invulnerable feature.*/
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageEvent event) {
        final Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        final Player  player = (Player) entity;
        final IPlayerData pData = DataManager.getPlayerData(player);
        final CombinedConfig cc = pData.getGenericInstance(CombinedConfig.class);
        if (!cc.invulnerableCheck) {
            return;
        }
        final DamageCause cause = event.getCause();
        // Ignored causes.
        if (cc.invulnerableIgnore.contains(cause)) {
            return;
        }
        // Modified invulnerable ticks.
        Integer modifier = cc.invulnerableModifiers.get(cause);
        if (modifier == null) modifier = cc.invulnerableModifierDefault;
        final CombinedData data = pData.getGenericInstance(CombinedData.class);
        // TODO: account for tick task reset ? [it should not though, due to data resetting too, but API would allow it]
        if (TickTask.getTick() >= data.invulnerableTick + modifier) {
            return;
        }
        // Still invulnerable.
        event.setCancelled(true);
        counters.addPrimaryThread(idFakeInvulnerable, 1);
    }
    
    /** We listen for player fishing for the VERY critical and super important munch hausen check */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onFishing(final PlayerFishEvent event) {
        // Check also in case of cancelled events.
        final Player player = event.getPlayer();
        if (munchHausen.isEnabled(player) && munchHausen.checkFish(player, event.getCaught(), event.getState())) {
            event.setCancelled(true);
        }
    }
}
