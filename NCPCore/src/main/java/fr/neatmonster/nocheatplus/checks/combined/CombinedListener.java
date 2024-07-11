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
                    data.addVelocity((Player)event.getEntity(), pData.getGenericInstance(MovingConfig.class), lastMove.xAllowedDistance, lastMove.yAllowedDistance, lastMove.zAllowedDistance, VelocityFlags.ORIGIN_INTERNAL);
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
                data.addVelocity(player, pData.getGenericInstance(MovingConfig.class), lastMove.xAllowedDistance, lastMove.yAllowedDistance, lastMove.zAllowedDistance, VelocityFlags.ORIGIN_INTERNAL);
            }
            if (res && pData.isDebugActive(CheckType.MOVING)) {
                debug(player, "Prevent toggle glide on (cheat prevention, " + (isToggleGlideEvent ? "ToggleGlideEvent)" : "PlayerMoveEvent)"));
            }
            return res;
        }
        // Did not toggle on.
        return false;
    }

    
    /** Check if this gliding phase should be aborted (We validate both toggle glide and gliding). */
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
        if (MovingUtil.canGlide(event.getPlayer(), info.from, data)) {
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
            data.addVelocity(event.getPlayer(), pData.getGenericInstance(MovingConfig.class), lastMove.xAllowedDistance, lastMove.yAllowedDistance, lastMove.zAllowedDistance, VelocityFlags.ORIGIN_INTERNAL);
        }
        if (pData.isDebugActive(CheckType.MOVING)) {
            debug(event.getPlayer(), "Abort gliding phase.");
        }
    }
    
    /** NOTE: this is updated at the end of the tick */
    private void handlePoseChangeEvent(final Entity entity, final Pose newPose) {
        if (!(entity instanceof Player)) {
            return;
        }
        final Player player = (Player) entity;
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (pData.getClientVersion().isOlderThan(ClientVersion.V_1_14)) {
            // Sneaking status is set on PlayerToggleSneakEvents.
            return;
        }
        // This is needed because of the discrepancy on what "sneaking" means between Bukkit and Minecraft. And we need to know the exact status to determine if we should enforce slower speed on players.
        // For Bukkit: sneaking= shift key press. Both player#isSneaking() and PlayerToggleSneakEvents are fired with action packets (PRESS/RELEASE_SHIFT_KEY).
        // For Minecraft: sneaking= being in crouch pose or in "crawl" pose (the latter added in 1.14. Check "isMovingSlowly()" method in client code).
        // Historically (up until Minecraft 1.9), a player could have entered the crouching pose only by tapping the shift key, thus, Bukkit's coincidence of SNEAKING == SHIFTING was okay.
        // This coincidence however is no longer true, because of Mojang adding several player actions that can change the pose.
        //  -> Gliding with an elytra will change the pose of the player.
        //  -> Same goes for swimming and riptiding (1.13+)
        //  -> On 1.14+ the bounding box is contracted if sneaking, allowing players to enter 1.5 blocks-high areas and STAY in crouch pose, REGARDLESS of shift key presses (until they get out).
        //     (Thus, a player can enter such an area and proceed to spam shift key presses without ever leaving the pose)
        //  -> 1.14 also added "crawl mode", which automatically activates if the player is somehow constricted in areas with a ceiling lower than 1.5 blocks (i.e.: with trapdoors).
        //     When players are in this mode, inputs are always slowed down, regardless of shift key presses (once again).
        //     [Since crawling conveniently shares the same pose of swimming, Minecraft simply checks if the player is in SWIMMING pose and not in water (Check client code in: LocalPlayer.java -> aiStep() -> isMovingSlowly() -> isVisuallyCrawling())]
        // In other words, Bukkit's status check/event can no longer be relied upon to know if the player needs to be slowed down, because they are both associated with shift key presses, not player poses. 
        // Thus, on 1.14 and higher, sneaking setting is done via EntityPoseChangeEvent, not PlayerToggleSneakEvent. For legacy clients, we can simply ignore all other poses, and set sneaking status if the shift key was pressed, since the mechanics above don't exist.
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
        // Needed because a player may log in and be already crouching or crawling.
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
        // Sprinting/Sneaking is reset in PlayerData
    }
    
    /** NOTE: Cancelling does nothing. It won't stop players from sneaking.*/
    @EventHandler(priority = EventPriority.MONITOR)
    public void onToggleShiftKey(final PlayerToggleSneakEvent event) {
        final IPlayerData pData = DataManager.getPlayerData(event.getPlayer());
        // Always set whenever the player presses the shift key.
        pData.setIsShiftKeyPressed(event.isSneaking());
        if (pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
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
        if (!event.isSprinting()) {
            // Player was sprinting and they now toggled it off.
            pData.setSprintingState(false);
            return;
        }
        if (pData.getClientVersion().isOlderThan(ClientVersion.V_1_14) && pData.isInCrouchingPose()) {
            // In 1.14 and lower, players cannot sprint and sneak at the same time.
            // Source: Minecraft Parkour wiki, sneaking section.
            pData.setSprintingState(false);
            return;
        }
        // TODO: This stuff might need to be latency compensated.
        // TODO: Wall collision
        // TODO: Attack slow-down
        if (event.getPlayer().getFoodLevel() <= 5) {
            // Cannot sprint with low hunger.
            pData.setSprintingState(false);
            return;
        }
        if (event.getPlayer().hasPotionEffect(PotionEffectType.BLINDNESS)) {
            // Blindness does not break sprinting if the player receives it while already sprinting.
            // The next toggle sprint event however will set it to false.
            pData.setSprintingState(false);
            return;
        }
        if (BridgeMisc.isVisuallyCrawling(event.getPlayer())) {
           // You cannot toggle sprint in a crawling area, unless you enter it while already sprinting.
           if (event.isSprinting()) {
               // Cheat! Set to false.
               pData.setSprintingState(false);
               return;
           }
        }
        pData.setSprintingState(true);
    }
    
    /** Cancelled events can still affect movement speed, since the mechanic we're checking is client-sided, so don't skip this listener */
    @EventHandler(priority = EventPriority.MONITOR) 
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
