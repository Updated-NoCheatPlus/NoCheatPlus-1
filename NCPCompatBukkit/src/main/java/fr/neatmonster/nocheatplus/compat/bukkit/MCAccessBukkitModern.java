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
import fr.neatmonster.nocheatplus.compat.bukkit.model.*;
import fr.neatmonster.nocheatplus.compat.cbreflect.reflect.ReflectBase;
import fr.neatmonster.nocheatplus.compat.cbreflect.reflect.ReflectDamageSource;
import fr.neatmonster.nocheatplus.compat.cbreflect.reflect.ReflectPlayer;
import fr.neatmonster.nocheatplus.config.WorldConfigProvider;
import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;

public class MCAccessBukkitModern extends MCAccessBukkit {

    protected ReflectBase reflectBase = null;
    protected ReflectDamageSource reflectDamageSource = null;
    protected ReflectPlayer reflectPlayer = null;

    protected final Map<Material, BukkitShapeModel> shapeModels = new HashMap<Material, BukkitShapeModel>();

    // Blocks that change shape based on interaction or redstone.
    private static final BukkitShapeModel MODEL_DOOR = new BukkitDoor();
    private static final BukkitShapeModel MODEL_TRAP_DOOR = new BukkitTrapDoor();
    private static final BukkitShapeModel MODEL_GATE = new BukkitGate(
            0.375, 1.5);
    private static final BukkitShapeModel MODEL_SHULKER_BOX = new BukkitShulkerBox();
    private static final BukkitShapeModel MODEL_CHORUS_PLANT = new BukkitChorusPlant();

    // Blocks with different heights based on whatever.
    private static final BukkitShapeModel MODEL_END_PORTAL_FRAME = new BukkitEndPortalFrame();
    private static final BukkitShapeModel MODEL_SEA_PICKLE = new BukkitSeaPickle();
    private static final BukkitShapeModel MODEL_COCOA = new BukkitCocoa();
    private static final BukkitShapeModel MODEL_TURTLE_EGG = new BukkitTurtleEgg();

    // Blocks that have a different shape, based on how they have been placed.
    private static final BukkitShapeModel MODEL_SLAB = new BukkitSlab();
    private static final BukkitShapeModel MODEL_STAIRS = new BukkitStairs();
    private static final BukkitShapeModel MODEL_PISTON = new BukkitPiston();
    private static final BukkitShapeModel MODEL_LEVELLED = new BukkitLevelled();
    private static final BukkitShapeModel MODEL_LADDER = new BukkitLadder();
    private static final BukkitShapeModel MODEL_RAIL = new BukkitRail();
    private static final BukkitShapeModel MODEL_END_ROD = new BukkitDirectionalCentered(
            0.375, 1.0, false);

    // Blocks that have a different shape with neighbor blocks (bukkit takes care though).
    private static final BukkitShapeModel MODEL_THIN_FENCE = new BukkitFence(
            0.4375, 1.0);
    private static final BukkitShapeModel MODEL_THICK_FENCE = new BukkitFence(
            0.375, 1.5);
    private static final BukkitShapeModel MODEL_THICK_FENCE2 = new BukkitWall(
            0.25, 1.5);
    // .75 .25 0 max: .25 .75 .5
    private static final BukkitShapeModel MODEL_WALL_HEAD = new BukkitWallHead();


    // Static blocks (various height and inset values).
    private static final BukkitShapeModel MODEL_BELL = new BukkitBell();
    private static final BukkitShapeModel MODEL_LECTERN = new BukkitStatic(0.25, 0.875);
    private static final BukkitShapeModel MODEL_BAMBOO = new BukkitBamboo();
    private static final BukkitShapeModel MODEL_ANVIL = new BukkitAnvil();
    private static final BukkitShapeModel MODEL_LILY_PAD = new BukkitStatic(
            0.09375);
    private static final BukkitShapeModel MODEL_FLOWER_POT = new BukkitStatic(
            0.33, 0.375); // TODO: XZ really?
    private static final BukkitShapeModel MODEL_LANTERN = new BukkitLantern();
    private static final BukkitShapeModel MODEL_CONDUIT = new BukkitStatic(
    	    0.33, 0.6875);
    private static final BukkitShapeModel MODEL_GROUND_HEAD= new BukkitStatic(
            0.25, 0.5); // TODO: XZ-really? 275 ?
    private static final BukkitShapeModel MODEL_SINGLE_CHEST = new BukkitStatic(
            0.0625, 0.875);
    private static final BukkitShapeModel MODEL_HONEY_BLOCK = new BukkitStatic(
            0.0625, 0.9375);
    private static final BukkitShapeModel MODEL_HOPPER = new BukkitStatic(
            0, 0.25, 0, 1, 1, 1);
    private static final BukkitShapeModel MODEL_CHAIN = new BukkitChain();
    // Static blocks with full height sorted by inset.
    private static final BukkitShapeModel MODEL_INSET16_1_HEIGHT100 = new BukkitStatic(
            0.0625, 1.0);

    // Static blocks with full xz-bounds sorted by height.
    private static final BukkitShapeModel MODEL_XZ100_HEIGHT16_1 = new BukkitStatic(
            0.0625);
    private static final BukkitShapeModel MODEL_XZ100_HEIGHT8_1 = new BukkitStatic(
            0.125);
    private static final BukkitShapeModel MODEL_XZ100_HEIGHT8_3 = new BukkitStatic(
            0.375);
    private static final BukkitShapeModel MODEL_XZ100_HEIGHT16_9 = new BukkitStatic(
            0.5625);
    private static final BukkitShapeModel MODEL_XZ100_HEIGHT4_3 = new BukkitStatic(
            0.75);
    private static final BukkitShapeModel MODEL_XZ100_HEIGHT8_7 = new BukkitStatic(
            0.875);
    private static final BukkitShapeModel MODEL_XZ100_HEIGHT16_15 = new BukkitStatic(
            0.9375);

    /*
     * TODO:
     * LADDER,
     * CONDUIT, 
     * CHORUS_FLOWER, CHORUS_PLANT, COCOA, 
     * TURTLE_EGG, SEA_PICKLE, 
     * VINE, 
     * CAKE,
     */
    // TODO: Liquid (all leveled).

    public MCAccessBukkitModern() {
        super();
        // TODO: Generic setup via Bukkit interface existence/relations, +- fetching methods.
        BlockInit.assertMaterialExists("OAK_LOG");
        BlockInit.assertMaterialExists("CAVE_AIR");
        try {
            this.reflectBase = new ReflectBase();
            this.reflectDamageSource = new ReflectDamageSource(this.reflectBase);
            this.reflectPlayer = new ReflectPlayer(this.reflectBase, null, this.reflectDamageSource);
        } catch(ClassNotFoundException ex) {

        }
    }

    @Override
    public String getMCVersion() {
        return "1.13-1.17.x|?";
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

        // TODO: Also consider removing flags (passable_x4 etc).

    	// Variables for repeated flags (Temporary flags, these should be fixed later so that they are not added here)
    	final long blockFix = BlockFlags.SOLID_GROUND;
    	// Adjust flags for individual blocks.
        BlockProperties.setBlockFlags(Material.CAULDRON, 
                BlockFlags.SOLID_GROUND | BlockProperties.F_GROUND_HEIGHT 
                | BlockProperties.F_MIN_HEIGHT4_1);
        BlockProperties.setBlockFlags(Material.COCOA, blockFix);
        BlockProperties.setBlockFlags(Material.TURTLE_EGG, blockFix);
        BlockProperties.setBlockFlags(Material.CHORUS_PLANT, blockFix);
        BlockProperties.setBlockFlags(Material.CREEPER_WALL_HEAD, blockFix);
        BlockProperties.setBlockFlags(Material.ZOMBIE_WALL_HEAD, blockFix);
        BlockProperties.setBlockFlags(Material.PLAYER_WALL_HEAD, blockFix);
        BlockProperties.setBlockFlags(Material.DRAGON_WALL_HEAD, blockFix);
        BlockProperties.setBlockFlags(Material.WITHER_SKELETON_WALL_SKULL, blockFix);
        BlockProperties.setBlockFlags(Material.SKELETON_WALL_SKULL, blockFix);

        // Directly keep blocks as is.
        for (final Material mat : new Material[] {
                Material.CAULDRON,
                BridgeMaterial.COBWEB,
                BridgeMaterial.MOVING_PISTON,
                Material.SNOW,
                Material.CAKE,
                Material.BEACON,
                Material.VINE,
                Material.CHORUS_FLOWER
        }) {
            processedBlocks.add(mat);
        }

        //Anvil
        for (Material mat : new Material[] {
        		Material.ANVIL,
        		Material.CHIPPED_ANVIL,
        		Material.DAMAGED_ANVIL
        }) {
        	addModel(mat, MODEL_ANVIL);
        }
        
        // Lily pad
        addModel(BridgeMaterial.LILY_PAD, MODEL_LILY_PAD);

        // End portal frame.
        addModel(BridgeMaterial.END_PORTAL_FRAME, MODEL_END_PORTAL_FRAME);

        // End rod.
        addModel(Material.END_ROD, MODEL_END_ROD);

        // Hoppers - min height changed in 1.13+
        addModel(Material.HOPPER, MODEL_HOPPER);

        // Ladder
        addModel(Material.LADDER, MODEL_LADDER);

        // 1/16 inset at full height.
        for (Material mat : new Material[] {
                Material.CACTUS,
                Material.DRAGON_EGG
        }) {
            addModel(mat, MODEL_INSET16_1_HEIGHT100);
        }

        // 1/8 height.
        for (Material mat : new Material[] {
                BridgeMaterial.REPEATER,
                Material.COMPARATOR
        }) {
            addModel(mat, MODEL_XZ100_HEIGHT8_1);
        }

        // 3/8 height.
        for (Material mat : new Material[] {
                Material.DAYLIGHT_DETECTOR
        }) {
            addModel(mat, MODEL_XZ100_HEIGHT8_3);
        }

        // 3/4 height.
        for (Material mat : new Material[] {
                BridgeMaterial.ENCHANTING_TABLE
        }) {
            addModel(mat, MODEL_XZ100_HEIGHT4_3);
        }

        // 7/8 height.
        for (Material mat : new Material[] {
                Material.BREWING_STAND // TODO: base is 1/8, center 0.875 - needs multi-cuboid.
        }) {
            addModel(mat, MODEL_XZ100_HEIGHT8_7);
        }

        // 16/15 height, full xz bounds.
        for (Material mat : new Material[] {
                BridgeMaterial.GRASS_PATH, BridgeMaterial.FARMLAND
        }) {
            addModel(mat, MODEL_XZ100_HEIGHT16_15);
        }

        // Thin fence: Glass panes, iron bars.
        for (final Material mat : MaterialUtil.addBlocks(
                MaterialUtil.GLASS_PANES, BridgeMaterial.IRON_BARS)) {
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
                "chest", "trapped_chest",
                "ender_chest"
                )) {
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
        		Material.TURTLE_EGG
        }) {
        	addModel(mat, MODEL_TURTLE_EGG);
        }
        
        // Conduit
        for (Material mat : new Material[] {
        		Material.CONDUIT
        }) {
        	addModel(mat, MODEL_CONDUIT);
        }
        
        // Cocoa
        for (Material mat : new Material[] {
        		Material.COCOA
        }) {
        	addModel(mat, MODEL_COCOA);
        }
        
        // Sea Pickles
        for (Material mat : new Material[] {
        		Material.SEA_PICKLE
        }) {
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
        		Material.CHORUS_PLANT
        }) {
        	addModel(mat, MODEL_CHORUS_PLANT);
        }

        //Lantern
        for (Material mat : BridgeMaterial.getAllBlocks(
                "lantern", "soul_lantern"
            )) {
            addModel(mat, MODEL_LANTERN);
        }

        // Piston
        for (Material mat : BridgeMaterial.getAllBlocks(
                "piston", "sticky_piston", "piston_base", "piston_sticky_base"
                )) {
            addModel(mat, MODEL_PISTON);
        }
 
        // Levelled blocks
        for (Material mat : BridgeMaterial.getAllBlocks(
                "snow", "water", "lava"
                )) {
            addModel(mat, MODEL_LEVELLED);
        }

        // Rails
        for (final Material mat : MaterialUtil.RAILS) {
            addModel(mat, MODEL_RAIL);
        }

        // Lectern
        Material mt = BridgeMaterial.getBlock("lectern");
        if (mt != null) addModel(mt, MODEL_LECTERN);

        // Bamboo        
        mt = BridgeMaterial.getBlock("bamboo");
        if (mt != null) addModel(mt, MODEL_BAMBOO);
        
        // Bell
        mt = BridgeMaterial.getBlock("bell");
        if (mt != null) addModel(mt, MODEL_BELL);
        
        mt = BridgeMaterial.getBlock("honey_block");
        if (mt != null) addModel(mt, MODEL_HONEY_BLOCK);

        mt = BridgeMaterial.getBlock("chain");
        if (mt != null) addModel(mt, MODEL_CHAIN);

        // Sort to processed by flags.
        for (final Material mat : Material.values()) {
            final long flags = BlockProperties.getBlockFlags(mat);
            // Stairs.
            if (BlockFlags.hasAnyFlag(flags, BlockProperties.F_STAIRS)) {
                addModel(mat, MODEL_STAIRS);
            }
            // Fences.
            if (BlockFlags.hasAnyFlag(flags, BlockProperties.F_THICK_FENCE)) {
                if (BlockFlags.hasAnyFlag(flags, BlockProperties.F_PASSABLE_X4)) {
                    // TODO: Perhaps another model flag.
                    addModel(mat, MODEL_GATE);
                }
                else {
                    addModel(mat, MODEL_THICK_FENCE);
                }
            }
            // Walls
            if (BlockFlags.hasAnyFlag(flags, BlockProperties.F_THICK_FENCE2)) {
                    addModel(mat, MODEL_THICK_FENCE2);
            }
        }

        super.setupBlockProperties(worldConfigProvider);
    }

    private Object getHandle(Player player) {
        // TODO: CraftPlayer check (isAssignableFrom)?
        if (this.reflectPlayer.obcGetHandle == null) {
            return null;
        }
        Object handle = ReflectionUtil.invokeMethodNoArgs(this.reflectPlayer.obcGetHandle, player);
        return handle;
    }

    private boolean canDealFallDamage() {
        return this.reflectPlayer.nmsDamageEntity != null && this.reflectDamageSource.nmsFALL != null;
    }

    @Override
    public AlmostBoolean dealFallDamageFiresAnEvent() {
        return canDealFallDamage() ? AlmostBoolean.YES : AlmostBoolean.NO;
    }

    @Override
    public void dealFallDamage(Player player, double damage) {
        if (canDealFallDamage()) {
            Object handle = getHandle(player);

            if (handle != null)
            ReflectionUtil.invokeMethod(this.reflectPlayer.nmsDamageEntity, handle, this.reflectDamageSource.nmsFALL, (float) damage);
        } else BridgeHealth.damage(player, damage);
    }

    @Override
    public boolean resetActiveItem(Player player) {
        if (this.reflectPlayer.nmsclearActiveItem != null) {
             Object handle = getHandle(player);
             if (handle != null) {
                 ReflectionUtil.invokeMethodNoArgs(this.reflectPlayer.nmsclearActiveItem, handle);
                 return true;
             }
        }
        return false;
    }
}
