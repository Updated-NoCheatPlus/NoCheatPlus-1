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
package fr.neatmonster.nocheatplus.compat.bukkit;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.compat.AlmostBoolean;
import fr.neatmonster.nocheatplus.compat.BridgeHealth;
import fr.neatmonster.nocheatplus.compat.BridgeMaterial;
import fr.neatmonster.nocheatplus.compat.blocks.init.BlockInit;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitAnvil;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitBamboo;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitBell;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitCake;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitCauldron;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitChorusPlant;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitCocoa;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitDirectionalCentered;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitDoor;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitDripLeaf;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitDripStone;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitEndPortalFrame;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitFence;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitFetchableBound;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitFetchableBounds;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitGate;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitGrindStone;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitHopper;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitLadder;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitLantern;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitLevelled;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitPiston;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitPistonHead;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitRail;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitSeaPickle;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitShapeModel;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitShulkerBox;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitSlab;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitSnow;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitStairs;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitStatic;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitTrapDoor;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitTurtleEgg;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitWall;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitWallHead;
import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitWaterPlant;
import fr.neatmonster.nocheatplus.compat.cbreflect.reflect.ReflectBase;
import fr.neatmonster.nocheatplus.compat.cbreflect.reflect.ReflectDamageSource;
import fr.neatmonster.nocheatplus.compat.cbreflect.reflect.ReflectDamageSources;
import fr.neatmonster.nocheatplus.compat.cbreflect.reflect.ReflectLivingEntity;
import fr.neatmonster.nocheatplus.config.WorldConfigProvider;
import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;

public class MCAccessBukkitModern extends MCAccessBukkit {

    protected ReflectBase reflectBase = null;
    protected ReflectDamageSource reflectDamageSource = null;
    protected ReflectDamageSources reflectDamageSources = null;
    protected ReflectLivingEntity reflectLivingEntity = null;
    protected final Map<Material, BukkitShapeModel> shapeModels = new HashMap<Material, BukkitShapeModel>();

    // Blocks that can be fetched automatically from the Bukkit API
    private static final BukkitShapeModel MODEL_AUTO_FETCH = new BukkitFetchableBounds();
    private static final BukkitShapeModel MODEL_AUTO_FETCH_LEGACY = new BukkitFetchableBound();

    // Blocks that are formed from multiple bounding boxes
    private static final BukkitShapeModel MODEL_BREWING_STAND = new BukkitStatic(
        // Bottom rod
        0.0625, 0.0, 0.0625, 0.9375, 0.125, 0.9375,
        // Rod
        0.4375, 0.125, 0.4375, 0.5625, 0.875, 0.5625
    );
    private static final BukkitShapeModel MODEL_CANDLE_CAKE = new BukkitStatic(
        // Cake
        0.0625, 0.0, 0.0625, 0.9375, 0.5, 0.9375,
        // Candle
        0.4375, 0.5, 0.4375, 0.5625, 0.875, 0.5625
    );
    private static final BukkitShapeModel MODEL_LECTERN = new BukkitStatic(
        // Post
        0.0, 0.0, 0.0, 1.0, 0.125, 1.0,
        // Lectern
        0.25, 0.125, 0.25, 0.75, 0.875, 0.75
    );
    private static final BukkitShapeModel MODEL_HOPPER = new BukkitHopper();
    private static final BukkitShapeModel MODEL_CAULDRON = new BukkitCauldron(0.1875, 0.125, 0.8125, 0.0625);
    private static final BukkitShapeModel MODEL_COMPOSTER = new BukkitCauldron(0.0, 0.125, 1.0, 0.125);
    private static final BukkitShapeModel MODEL_PISTON_HEAD = new BukkitPistonHead();
    private static final BukkitShapeModel MODEL_BELL = new BukkitBell();
    private static final BukkitShapeModel MODEL_ANVIL = new BukkitAnvil();
    private static final BukkitShapeModel MODEL_GRIND_STONE = new BukkitGrindStone();

    // Blocks that change shape based on interaction or redstone.
    private static final BukkitShapeModel MODEL_DOOR = new BukkitDoor();
    private static final BukkitShapeModel MODEL_TRAP_DOOR = new BukkitTrapDoor();
    private static final BukkitShapeModel MODEL_GATE = new BukkitGate(0.375, 1.5);
    private static final BukkitShapeModel MODEL_SHULKER_BOX = new BukkitShulkerBox();
    private static final BukkitShapeModel MODEL_CHORUS_PLANT = new BukkitChorusPlant();
    private static final BukkitShapeModel MODEL_DRIP_LEAF = new BukkitDripLeaf();

    // Blocks with different heights based on whatever.
    private static final BukkitShapeModel MODEL_END_PORTAL_FRAME = new BukkitEndPortalFrame();
    private static final BukkitShapeModel MODEL_SEA_PICKLE = new BukkitSeaPickle();
    private static final BukkitShapeModel MODEL_COCOA = new BukkitCocoa();
    private static final BukkitShapeModel MODEL_TURTLE_EGG = new BukkitTurtleEgg();

    // Blocks that have a different shape, based on how they have been placed.
    private static final BukkitShapeModel MODEL_CAKE = new BukkitCake();
    private static final BukkitShapeModel MODEL_SLAB = new BukkitSlab();
    private static final BukkitShapeModel MODEL_STAIRS = new BukkitStairs();
    private static final BukkitShapeModel MODEL_SNOW = new BukkitSnow();
    private static final BukkitShapeModel MODEL_PISTON = new BukkitPiston();
    private static final BukkitShapeModel MODEL_LEVELLED = new BukkitLevelled();
    private static final BukkitShapeModel MODEL_LADDER = new BukkitLadder();
    private static final BukkitShapeModel MODEL_RAIL = new BukkitRail();
    private static final BukkitShapeModel MODEL_END_ROD = new BukkitDirectionalCentered(0.375, 1.0, false);

    // Blocks that have a different shape with neighbor blocks (bukkit takes care though).
    private static final BukkitShapeModel MODEL_THIN_FENCE = new BukkitFence(0.4375, 1.0);
    private static final BukkitShapeModel MODEL_THICK_FENCE = new BukkitFence(0.375, 1.5);
    private static final BukkitShapeModel MODEL_THICK_FENCE2 = new BukkitWall(0.25, 1.5, 0.3125); // .75 .25 0 max: .25 .75 .5
    private static final BukkitShapeModel MODEL_WALL_HEAD = new BukkitWallHead();

    // Static blocks (various height and inset values).
    private static final BukkitShapeModel MODEL_CAMPFIRE = new BukkitStatic(0.0, 0.4375);
    private static final BukkitShapeModel MODEL_BAMBOO = new BukkitBamboo();
    private static final BukkitShapeModel MODEL_POINTED_DRIPSTONE = new BukkitDripStone();
    private static final BukkitShapeModel MODEL_WATER_PLANTS = new BukkitWaterPlant();
    private static final BukkitShapeModel MODEL_LILY_PAD = new BukkitStatic(0.0625, 0.09375);
    private static final BukkitShapeModel MODEL_FLOWER_POT = new BukkitStatic(0.3125, 0.375);
    private static final BukkitShapeModel MODEL_LANTERN = new BukkitLantern();
    private static final BukkitShapeModel MODEL_CONDUIT = new BukkitStatic(0.3125, 0.3125, 0.3125, 0.6875, 0.6875, 0.6875);
    private static final BukkitShapeModel MODEL_GROUND_HEAD = new BukkitStatic(0.25, 0.5);
    private static final BukkitShapeModel MODEL_SINGLE_CHEST = new BukkitStatic(0.0625, 0.875);
    private static final BukkitShapeModel MODEL_HONEY_BLOCK = new BukkitStatic(0.0625, 0.9375);
    private static final BukkitShapeModel MODEL_SCULK_SHRIEKER = new BukkitStatic(0.0, 0.5);

    // Static blocks with full height sorted by inset.
    private static final BukkitShapeModel MODEL_INSET16_1_HEIGHT100 = new BukkitStatic(0.0625, 1.0);

    // Static blocks with full xz-bounds sorted by height.
    private static final BukkitShapeModel MODEL_XZ100_HEIGHT16_1 = new BukkitStatic(0.0625);
    private static final BukkitShapeModel MODEL_XZ100_HEIGHT8_1 = new BukkitStatic(0.125);
    private static final BukkitShapeModel MODEL_XZ100_HEIGHT8_3 = new BukkitStatic(0.375);
    private static final BukkitShapeModel MODEL_XZ100_HEIGHT16_9 = new BukkitStatic(0.5625);
    private static final BukkitShapeModel MODEL_XZ100_HEIGHT4_3 = new BukkitStatic(0.75);
    private static final BukkitShapeModel MODEL_XZ100_HEIGHT16_15 = new BukkitStatic(0.9375);
    private static final BukkitShapeModel MODEL_XZ100_HEIGHT8_7 = new BukkitStatic(0.875);

    public MCAccessBukkitModern() {
        super();
        // TODO: Generic setup via Bukkit interface existence/relations, +- fetching methods.
        BlockInit.assertMaterialExists("OAK_LOG");
        BlockInit.assertMaterialExists("CAVE_AIR");
        try {
            this.reflectBase = new ReflectBase();
            this.reflectDamageSource = new ReflectDamageSource(this.reflectBase);
            this.reflectLivingEntity = new ReflectLivingEntity(this.reflectBase, null, this.reflectDamageSource);
            // Can be null
            this.reflectDamageSources = new ReflectDamageSources(this.reflectBase, this.reflectDamageSource);
        } 
        catch (ClassNotFoundException ex) {}
    }

    @Override
    public String getMCVersion() {
        return "1.13-1.20";
    }

    @Override
    public BlockCache getBlockCache() {
        return new BlockCacheBukkitModern(shapeModels);
    }

    public void addModel(Material mat, BukkitShapeModel model) {
        processedBlocks.add(mat);
        shapeModels.put(mat, model);
    }

    @Override
    public void setupBlockProperties(final WorldConfigProvider<?> worldConfigProvider) {
        /*
         * TODO: Incomplete blocks bounds still use workarounds(not affect movement):
         * All fences, glass pane, iron bar, chorus plant, hopper
         * 
         * Legacy blocks still use workarounds(not affect movement):
         * All fences, glass pane, iron bar, chorus plant, hopper, cauldron
         *
         * Scaffolding(affect fall dmg)
         * 
         */

        // Variables for repeated flags (Temporary flags, these should be fixed later so that they are not added here)
        final long blockFix = BlockFlags.SOLID_GROUND;
        // Adjust flags for individual blocks.
        BlockFlags.setBlockFlags(Material.COCOA, blockFix);
        BlockFlags.setBlockFlags(Material.TURTLE_EGG, blockFix);
        BlockFlags.setBlockFlags(Material.CHORUS_PLANT, blockFix);
        BlockFlags.setBlockFlags(Material.CREEPER_WALL_HEAD, blockFix);
        BlockFlags.setBlockFlags(Material.ZOMBIE_WALL_HEAD, blockFix);
        BlockFlags.setBlockFlags(Material.PLAYER_WALL_HEAD, blockFix);
        BlockFlags.setBlockFlags(Material.DRAGON_WALL_HEAD, blockFix);
        BlockFlags.setBlockFlags(Material.WITHER_SKELETON_WALL_SKULL, blockFix);
        BlockFlags.setBlockFlags(Material.SKELETON_WALL_SKULL, blockFix);

        // Directly keep blocks as is.
        for (final Material mat : new Material[] {
            BridgeMaterial.COBWEB,
            BridgeMaterial.MOVING_PISTON,
            Material.SNOW,
            Material.BEACON,
            Material.VINE,
            Material.CHORUS_FLOWER}) {
            processedBlocks.add(mat);
        }

        for (final Material mat : BridgeMaterial.getAllBlocks(
            "light", "glow_lichen", "big_dripleaf_stem",
            // TODO: Not fully tested
            "scaffolding", "powder_snow")) {
            processedBlocks.add(mat);
        }

        // Candle
        for (Material mat : MaterialUtil.ALL_CANDLES) {
            addModel(mat, MODEL_AUTO_FETCH);
        }

        // Amethyst
        for (Material mat : MaterialUtil.AMETHYST) {
            addModel(mat, MODEL_AUTO_FETCH);
        }

        // new flower, and others
        for (Material mat : BridgeMaterial.getAllBlocks(
            "azalea", "flowering_azalea",
            "sculk_sensor", "frogspawn",
            "sniffer_egg", "decorated_pot", "pitcher_crop", "calibrated_sculk_sensor")) {
            addModel(mat, MODEL_AUTO_FETCH);
        }
        
        // Wall hanging signs have a collision box. Hanging signs don't, so they are treated as an ordinary sign.
        for (Material mat : MaterialUtil.WALL_HANGING_SIGNS) {
            // Only the post has an actual collision box, the rest is all hit box. 
            addModel(mat, MODEL_AUTO_FETCH);
        }

        for (Material mat : BridgeMaterial.getAllBlocks(
            "stonecutter", "chain")) {
            addModel(mat, MODEL_AUTO_FETCH_LEGACY);
        }

        // Camp fire
        for (Material mat : BridgeMaterial.getAllBlocks(
            "campfire", "soul_campfire")) {
            addModel(mat, MODEL_CAMPFIRE);
        }

        // Cauldron
        for (Material mat : MaterialUtil.CAULDRON) {
            BlockFlags.setBlockFlags(mat, BlockFlags.SOLID_GROUND);
            addModel(mat, MODEL_CAULDRON);
        }

        //Anvil
        for (Material mat : new Material[] {
            Material.ANVIL,
            Material.CHIPPED_ANVIL,
            Material.DAMAGED_ANVIL}) {
            addModel(mat, MODEL_ANVIL);
        }
        
        // Lily pad
        addModel(BridgeMaterial.LILY_PAD, MODEL_LILY_PAD);

        // End portal frame.
        addModel(BridgeMaterial.END_PORTAL_FRAME, MODEL_END_PORTAL_FRAME);

        // Cake
        addModel(BridgeMaterial.CAKE, MODEL_CAKE);

        // End Rod / Lightning Rod.
        for (Material mat : MaterialUtil.RODS) {
            addModel(mat, MODEL_END_ROD);
        }

        // Hoppers - min height changed in 1.13+
        addModel(Material.HOPPER, MODEL_HOPPER);

        // Ladder
        addModel(Material.LADDER, MODEL_LADDER);

        // BrewingStand
        addModel(Material.BREWING_STAND, MODEL_BREWING_STAND);

        // 1/16 inset at full height.
        addModel(Material.DRAGON_EGG, MODEL_INSET16_1_HEIGHT100);
        addModel(Material.CACTUS, MODEL_HONEY_BLOCK);

        // 1/8 height.
        for (Material mat : new Material[] {
            BridgeMaterial.REPEATER,
            Material.COMPARATOR }) {
            addModel(mat, MODEL_XZ100_HEIGHT8_1);
        }

        // 3/8 height.
        for (Material mat : new Material[] {
            Material.DAYLIGHT_DETECTOR}) {
            addModel(mat, MODEL_XZ100_HEIGHT8_3);
        }

        // 3/4 height.
        for (Material mat : new Material[] {
            BridgeMaterial.ENCHANTING_TABLE}) {
            addModel(mat, MODEL_XZ100_HEIGHT4_3);
        }

        for (Material mat : MaterialUtil.ALL_CANDLE_CAKE) {
            addModel(mat, MODEL_CANDLE_CAKE);
        }

        // 7/8 height.
        for (Material mat : new Material[] {
            Material.SOUL_SAND}) {
            addModel(mat, MODEL_XZ100_HEIGHT8_7);
        }

        // 16/15 height, full xz bounds.
        for (Material mat : new Material[] {
            BridgeMaterial.GRASS_PATH, 
            BridgeMaterial.FARMLAND}) {
            addModel(mat, MODEL_XZ100_HEIGHT16_15);
        }

        // Thin fence: Glass panes, iron bars.
        for (final Material mat : MaterialUtil.addBlocks(
            MaterialUtil.GLASS_PANES, 
            BridgeMaterial.IRON_BARS)) {
            addModel(mat, MODEL_THIN_FENCE);
        }

        // Slabs
        for (final Material mat : MaterialUtil.SLABS) {
            addModel(mat, MODEL_SLAB);
        }

        // Shulker boxes.
        for (final Material mat : MaterialUtil.SHULKER_BOXES) {
            addModel(mat, MODEL_SHULKER_BOX);
        }

        // Chests.
        // TOOD: Might add a facing/directional extension for double chests.
        for (Material mat : BridgeMaterial.getAllBlocks(
            "chest", "trapped_chest", "ender_chest")) {
            addModel(mat, MODEL_SINGLE_CHEST);
        }

        // Beds
        for (Material mat : MaterialUtil.BEDS) {
            addModel(mat, MODEL_XZ100_HEIGHT16_9);
        }

        // Flower pots.
        for (Material mat : MaterialUtil.FLOWER_POTS) {
            addModel(mat, MODEL_FLOWER_POT);
        }
        
        // Turtle Eggs.
        for (Material mat : new Material[] {
            Material.TURTLE_EGG}) {
            addModel(mat, MODEL_TURTLE_EGG);
        }
        
        // Conduit
        for (Material mat : new Material[] {
            Material.CONDUIT}) {
            addModel(mat, MODEL_CONDUIT);
        }
        
        // Cocoa
        for (Material mat : new Material[] {
            Material.COCOA}) {
            addModel(mat, MODEL_COCOA);
        }
        
        // Sea Pickles
        for (Material mat : new Material[] {
            Material.SEA_PICKLE}) {
            addModel(mat, MODEL_SEA_PICKLE);
        }
        
        // Carpets.
        for (final Material mat : MaterialUtil.CARPETS) {
            addModel(mat, MODEL_XZ100_HEIGHT16_1);
        }

        // Ground heads.
        for (final Material mat : MaterialUtil.HEADS_GROUND) {
            addModel(mat, MODEL_GROUND_HEAD);
        }

        // Heads on walls.
        for (final Material mat : MaterialUtil.HEADS_WALL) {
            addModel(mat, MODEL_WALL_HEAD);
        }

        // Doors.
        for (final Material mat : MaterialUtil.ALL_DOORS) {
            addModel(mat, MODEL_DOOR);
        }

        // Trapdoors.
        for (final Material mat : MaterialUtil.ALL_TRAP_DOORS) {
            addModel(mat, MODEL_TRAP_DOOR);
        }
        
        // Chorus Plant.
        for (Material mat : new Material[] {
            Material.CHORUS_PLANT}) {
            addModel(mat, MODEL_CHORUS_PLANT);
        }

        // Lantern.
        for (Material mat : BridgeMaterial.getAllBlocks(
            "lantern", "soul_lantern")) {
            addModel(mat, MODEL_LANTERN);
        }

        // Piston.
        for (Material mat : BridgeMaterial.getAllBlocks(
            "piston", "sticky_piston", "piston_base", "piston_sticky_base")) {
            addModel(mat, MODEL_PISTON);
        }

        // Piston Head.
        addModel(BridgeMaterial.PISTON_HEAD, MODEL_PISTON_HEAD);

        // Snow.
        addModel(Material.SNOW, MODEL_SNOW);

        // Levelled blocks.
        for (Material mat : MaterialUtil.WATER) {
            addModel(mat, MODEL_LEVELLED);
        }
        for (Material mat : MaterialUtil.LAVA) {
            addModel(mat, MODEL_LEVELLED);
        }

        for (Material mat : MaterialUtil.WATER_PLANTS) {
            addModel(mat, MODEL_WATER_PLANTS);
        }

        // Rails.
        for (final Material mat : MaterialUtil.RAILS) {
            addModel(mat, MODEL_RAIL);
        }
        
        // Walls.
        for (Material mat : MaterialUtil.ALL_WALLS) {
            BlockFlags.setBlockFlags(mat, BlockFlags.SOLID_GROUND | BlockFlags.F_VARIABLE);
            addModel(mat, MODEL_THICK_FENCE2);
        }

        // Lectern.
        Material mt = BridgeMaterial.getBlock("lectern");
        if (mt != null) addModel(mt, MODEL_LECTERN);

        // Bamboo.      
        mt = BridgeMaterial.getBlock("bamboo");
        if (mt != null) addModel(mt, MODEL_BAMBOO);

        // Dripstone.      
        mt = BridgeMaterial.getBlock("pointed_dripstone");
        if (mt != null) addModel(mt, MODEL_POINTED_DRIPSTONE);

        // Bell.
        mt = BridgeMaterial.getBlock("bell");
        if (mt != null) addModel(mt, MODEL_BELL);

        // Composter.
        mt = BridgeMaterial.getBlock("composter");
        if (mt != null) addModel(mt, MODEL_COMPOSTER);

        // Honey Block.
        mt = BridgeMaterial.getBlock("honey_block");
        if (mt != null) addModel(mt, MODEL_HONEY_BLOCK);

        // Big DripLeaf.
        mt = BridgeMaterial.getBlock("big_dripleaf");
        if (mt != null) addModel(mt, MODEL_DRIP_LEAF);

        // Grindstone.
        mt = BridgeMaterial.getBlock("grindstone");
        if (mt != null) addModel(mt, MODEL_GRIND_STONE);

        // Sculk Shrieker
        mt = BridgeMaterial.getBlock("sculk_shrieker");
        if (mt != null) addModel(mt, MODEL_SCULK_SHRIEKER);

        // Mud
        mt = BridgeMaterial.getBlock("mud");
        if (mt != null) addModel(mt, MODEL_XZ100_HEIGHT8_7);

        // Heavy core
        mt = BridgeMaterial.getBlock("heavy_core");
        if (mt != null) addModel(mt, MODEL_GROUND_HEAD);

        // Sort to processed by flags.
        for (final Material mat : Material.values()) {
            final long flags = BlockFlags.getBlockFlags(mat);
            // Stairs.
            if (BlockFlags.hasAnyFlag(flags, BlockFlags.F_STAIRS)) {
                addModel(mat, MODEL_STAIRS);
            }
            // Fences.
            if (BlockFlags.hasAnyFlag(flags, BlockFlags.F_THICK_FENCE)) {
                if (BlockFlags.hasAnyFlag(flags, BlockFlags.F_PASSABLE_X4)) {
                    // TODO: Perhaps another model flag.
                    addModel(mat, MODEL_GATE);
                }
                else addModel(mat, MODEL_THICK_FENCE);
            }
        }
        super.setupBlockProperties(worldConfigProvider);
    }

    private Object getHandle(Player player) {
        // TODO: CraftPlayer check (isAssignableFrom)?
        if (this.reflectLivingEntity == null || this.reflectLivingEntity.obcGetHandle == null) {
            return null;
        }
        return ReflectionUtil.invokeMethodNoArgs(this.reflectLivingEntity.obcGetHandle, player);
    }

    private boolean canDealFallDamage() {
        return this.reflectLivingEntity != null && this.reflectLivingEntity.nmsDamageEntity != null 
               && (this.reflectDamageSource.nmsFALL != null || this.reflectDamageSources != null);
    }

    @Override
    public AlmostBoolean dealFallDamageFiresAnEvent() {
        return canDealFallDamage() ? AlmostBoolean.YES : AlmostBoolean.NO;
    }

    @Override
    public void dealFallDamage(Player player, double damage) {
        if (canDealFallDamage()) {
            Object handle = getHandle(player);
            if (handle != null) {
                // Direct accessing DamageSource
                Object damageSource = this.reflectDamageSource.nmsFALL;
                if (damageSource == null) {
                    // Fail, second attempt
                    damageSource = this.reflectDamageSources.getDamageSource(handle);
                    if (damageSource == null) {
                        // Fail, deal generic damage
                        BridgeHealth.damage(player, damage);
                        // Disable class to prevent continuous damage
                        reflectDamageSources = null;
                        return;
                    }
                }
                ReflectionUtil.invokeMethod(this.reflectLivingEntity.nmsDamageEntity, handle, damageSource, (float) damage);
            }
        } 
        else BridgeHealth.damage(player, damage);
    }

    @Override
    public boolean resetActiveItem(Player player) {
        if (this.reflectLivingEntity != null && this.reflectLivingEntity.nmsclearActiveItem != null) {
            Object handle = getHandle(player);
            if (handle != null) {
                ReflectionUtil.invokeMethodNoArgs(this.reflectLivingEntity.nmsclearActiveItem, handle);
                return true;
            }
        }
        return false;
    }
}
