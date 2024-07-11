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
package fr.neatmonster.nocheatplus.utilities.map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.compat.AlmostBoolean;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.ds.map.CoordHashMap;
import fr.neatmonster.nocheatplus.utilities.ds.map.CoordMap;

/**
 * Access to block properties using caching technique (id, data, bounds).
 * 
 * @author asofold
 *
 */
public abstract class BlockCache {

    /**
     * Read access to a BlockCacheNode.
     * @author asofold
     *
     */
    public static interface IBlockCacheNode {

        public boolean isDataFetched();

        public boolean isBoundsFetched();

        /**
         * Test for (useful) bounds being there, i.e. fetched and bounds being
         * null.
         * 
         * @return YES, if bounds have been fetched and are not null. NO if
         *         bounds have been fetched and are null. MAYBE, if bounds have
         *         not been fetched.
         */
        public AlmostBoolean hasNonNullBounds();

        /**
         * Always set.
         * @return
         */
        public Material getType();

        /**
         * Ensure to test with isDataSet().
         * @return
         */
        public int getData();

        /**
         * Ensure to test with isBoundsSet().
         * @return
         */
        public double[] getBounds();

        /**
         * Convenience method to return either the set data, or return data
         * fetched from the given BlockCache instance. The internal state of
         * this node is not updated by this call, unless the BlockCache instance
         * does so.
         * 
         * @param blockCache
         * @param x
         * @param y
         * @param z
         * @return
         */
        public int getData(BlockCache blockCache, int x, int y, int z);

        /**
         * Convenience method to return either the set bounds, or return bounds
         * fetched from the given BlockCache instance. The internal state of
         * this node is not updated by this call, unless the BlockCache instance
         * does so.
         * 
         * @param blockCache
         * @param x
         * @param y
         * @param z
         * @return
         */
        public double[] getBounds(BlockCache blockCache, int x, int y, int z);


    }

    public static class BlockCacheNode implements IBlockCacheNode {

        private static final short FETCHED_ID = 0x01;
        private static final short FETCHED_DATA = 0x02;
        private static final short FETCHED_BOUNDS = 0x04;

        private short fetched;
        private Material id;
        private int data = 0;
        private double[] bounds = null;

        public BlockCacheNode(Material id) {
            this.id = id;
            fetched = FETCHED_ID;
        }

        @Override
        public boolean isDataFetched() {
            return (fetched & FETCHED_DATA) != 0;
        }

        @Override
        public boolean isBoundsFetched() {
            return (fetched & FETCHED_BOUNDS) != 0;
        }

        @Override
        public AlmostBoolean hasNonNullBounds() {
            return isBoundsFetched() ? (bounds == null ? AlmostBoolean.NO : AlmostBoolean.YES) : AlmostBoolean.MAYBE;
        }

        @Override
        public Material getType() {
            return id;
        }

        @Override
        public int getData() {
            return data;
        }

        @Override
        public double[] getBounds() {
            return bounds;
        }

        @Override
        public int getData(BlockCache blockCache, int x, int y, int z) {
            return isDataFetched() ? data : blockCache.getData(x, y, z);
        }

        @Override
        public double[] getBounds(BlockCache blockCache, int x, int y, int z) {
            return isBoundsFetched() ? bounds : blockCache.getBounds(x, y, z);
        }

        public void setData(int data) {
            this.data = data;
            fetched |= FETCHED_DATA;
        }

        public void setBounds(double[] bounds) {
            this.bounds = bounds;
            fetched |= FETCHED_BOUNDS;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof IBlockCacheNode) {
                final IBlockCacheNode other = (IBlockCacheNode) obj;
                return id == other.getType() 
                        && (!isDataFetched() && !other.isDataFetched() 
                                || isDataFetched() && other.isDataFetched() && data == other.getData())
                        && (!isBoundsFetched() && !other.isBoundsFetched()
                                || isBoundsFetched() && other.isBoundsFetched() 
                                && BlockProperties.isSameShape(bounds, other.getBounds())
                                );
            }
            return false;
        }

    }

    // Instance

    /** Nodes for cached block properties. */
    private final CoordMap<BlockCacheNode> nodeMap = new CoordHashMap<BlockCacheNode>(23);

    /** The max block y. */
    protected int maxBlockY = 255;
    /** The min block y. */
    protected int minBlockY = 0;

    private IPlayerData pData = null;

    private final BlockCacheNode airNode = new BlockCacheNode(Material.AIR);
    // TODO: setBlockCacheConfig -> set static nodes (rather only by id).

    /**
     * Instantiates a new block cache.
     */
    public BlockCache() {
        airNode.setData(0);
        airNode.setBounds(null);
    }

    /**
     * Instantiates a new block cache.
     *
     * @param world
     *            the world
     */
    public BlockCache(final World world) {
        setAccess(world);
    }

    /**
     * Does not do cleanup.
     *
     * @param world
     *            the new access
     * @return This BlockCache instance for chaining.
     */
    public abstract BlockCache setAccess(final World world);

    /**
     * Fetch the type id from the underlying world.
     *
     * @param x
     *            the x
     * @param y
     *            the y
     * @param z
     *            the z
     * @return the int
     */
    public abstract Material fetchTypeId(int x, int y, int z);

    /**
     * Fetch the data from the underlying world.
     *
     * @param x
     *            the x
     * @param y
     *            the y
     * @param z
     *            the z
     * @return the int
     */
    public abstract int fetchData(int x, int y, int z);

    /**
     * Find out bounds for the block, this should not return null for
     * performance reasons.
     *
     * @param x
     *            the x
     * @param y
     *            the y
     * @param z
     *            the z
     * @return the double[]
     */
    public abstract double[] fetchBounds(int x, int y, int z);

    /**
     * This is a on-ground type check just for standing on minecarts / boats.
     *
     * @param entity
     *            the entity
     * @param minX
     *            the min x
     * @param minY
     *            the min y
     * @param minZ
     *            the min z
     * @param maxX
     *            the max x
     * @param maxY
     *            the max y
     * @param maxZ
     *            the max z
     * @return true, if successful
     */
    public abstract boolean standsOnEntity(Entity entity, final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ);


    /**
     * Remove references.<br>
     * NOTE: You must delete world references with this one.
     */
    public void cleanup() {
        nodeMap.clear();
        pData = null;
    }

    /**
     * If there is no node stored, create a new node only with the type id set.
     * 
     * @param x
     * @param y
     * @param z
     * @return
     */
    private BlockCacheNode getOrCreateNode(final int x, final int y, final int z) {
        BlockCacheNode node = nodeMap.get(x, y, z);
        if (node != null) {
            return node;
        }
        final Material id = (y < minBlockY || y > maxBlockY) ? Material.AIR : fetchTypeId(x, y, z);
        // (Later: Static id-node map from config.)
        if (BlockProperties.isAir(id)) {
            return airNode;
        }
        else {
            node = new BlockCacheNode(id);
            nodeMap.put(x, y, z, node);
            return node;
        }
    }

    /**
     * (Convenience method, uses cache).
     *
     * @param x
     *            the x
     * @param y
     *            the y
     * @param z
     *            the z
     * @return the type id
     */
    public Material getType(double x, double y, double z) {
        return getType(Location.locToBlock(x), Location.locToBlock(y), Location.locToBlock(z));
    }

    /**
     * (Convenience method, uses cache).
     * @param x
     * @param y
     * @param z
     * @param forceSetAll
     * @return
     */
    public IBlockCacheNode getOrCreateBlockCacheNode(double x, double y, double z, boolean forceSetAll) {
        return getOrCreateBlockCacheNode(Location.locToBlock(x), Location.locToBlock(y), Location.locToBlock(z), forceSetAll);
    }

    /**
     * (convenience method, uses cache).
     *
     * @param block
     *            the block
     * @return the type id
     */
    public Material getType(final Block block) {
        return getType(block.getX(), block.getY(), block.getZ());
    }

    /**
     * Get type id with cache access.
     *
     * @param x
     *            the x
     * @param y
     *            the y
     * @param z
     *            the z
     * @return the type id
     */
    public Material getType(final int x, final int y, final int z) {
        return getOrCreateNode(x, y, z).getType();
    }

    /**
     * Get data value with cache access.
     *
     * @param x
     *            the x
     * @param y
     *            the y
     * @param z
     *            the z
     * @return the data
     */
    public int getData(final int x, final int y, final int z) {
        final BlockCacheNode node = getOrCreateNode(x, y, z);
        if (node.isDataFetched()) {
            return node.getData();
        }
        final int nData = fetchData(x, y, z);
        node.setData(nData);
        return nData;
    }

    /**
     * Get block bounds - <b>Do not change these in-place, because the returned
     * array is cached internally.</b>
     *
     * @param x
     *            the x
     * @param y
     *            the y
     * @param z
     *            the z
     * @return Array of floats (minX, minY, minZ, maxX, maxY, maxZ), may be null
     *         theoretically. Do not change these in place, because they might
     *         get cached.
     */
    public double[] getBounds(final int x, final int y, final int z) {
        final BlockCacheNode node = getOrCreateNode(x, y, z);
        if (node.isBoundsFetched()) {
            return node.getBounds();
        }
        final double[] nBounds = fetchBounds(x, y, z);
        // TODO: Convention for null bounds -> full ?
        node.setBounds(nBounds);
        return nBounds;
    }

    /**
     * Get an IBlockCacheNode instance for the given coordinates. With
     * forceSetAll set to true, it will be ensured that all properties are set
     * for the returned node.
     * 
     * @param x
     * @param y
     * @param z
     * @param forceSetAll
     * @return If forceSetAll is true, a node will always be returned with all
     *         properties set. If forceSetAll is false, a node with at least the
     *         id set will be returned.
     */
    public IBlockCacheNode getOrCreateBlockCacheNode(int x, int y, int z, boolean forceSetAll) {
        final BlockCacheNode node = getOrCreateNode(x, y, z);
        if (forceSetAll) {
            // TODO: Consider a half-lazy variant (only force fetch bounds, which may or may not fetch data).
            if (!node.isDataFetched()) {
                node.setData(fetchData(x, y, z));
            }
            if (!node.isBoundsFetched()) {
                node.setBounds(fetchBounds(x, y, z));
            }
        }
        return node;
    }

    /**
     * Just return the internally stored node for these coordinates, or null if
     * none is there.
     * 
     * @param x
     * @param y
     * @param z
     * @return
     */
    public IBlockCacheNode getBlockCacheNode(int x, int y, int z) {
        return nodeMap.get(x, y, z);
    }

    /**
     * Convenience method to check if the bounds for a block cover the full
     * block.
     *
     * @param x
     *            the x
     * @param y
     *            the y
     * @param z
     *            the z
     * @return true, if is full bounds
     */
    public boolean isFullBounds(final int x, final int y, final int z) {
        return MapUtil.isFullBounds(getBounds(x, y, z));
    }

    /**
     * Get the maximal y coordinate a block can be at (non air).
     *
     * @return the max block y
     */
    public int getMaxBlockY() {
        return maxBlockY;
    }

    /**
     * Get the minimal y coordinate a block can be at (non air).
     *
     * @return the min block y
     */
    public int getMinBlockY() {
        return minBlockY;
    }

    public void setPlayerData(IPlayerData pData) {
        this.pData = pData;
    }

    public IPlayerData getPlayerData() {
        return pData;
    }
}
