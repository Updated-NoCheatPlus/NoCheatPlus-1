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
package fr.neatmonster.nocheatplus.checks.net.model;

public class DataPacketFlying extends DataLocation {

    /**
     * Simplified packet content type.
     * @author asofold
     *
     */
    public static enum PACKET_CONTENT {
        /** Neither position nor look, only ground. */
        GROUND_ONLY,
        /** Position and ground. */
        POS,
        /** Look and ground. */
        LOOK,
        /** Position, look and ground. */
        POS_LOOK;
    }

    // TODO: Use MAX_VALUE for not set doubles/floats?
    // TODO: Consider private + access methods.
    // TODO: Consider AlmostBoolean for fault tolerance ?
    // TODO: hashCode + equals.

    public final boolean onGround;
    public final boolean horizontalCollision;
    public final boolean hasPos;
    public final boolean hasLook;
    public final long time;
    private long sequence = 0;

    public DataPacketFlying(boolean onGround, boolean horizontalCollision, long time) {
        super(0, 0, 0, 0, 0);
        this.onGround = onGround;
        this.horizontalCollision = horizontalCollision;
        hasPos = false;
        hasLook = false;
        this.time = time;
    }

    public DataPacketFlying(boolean onGround, boolean horizontalCollision, float yaw, float pitch, long time) {
        super(0, 0, 0, yaw, pitch);
        this.horizontalCollision = horizontalCollision;
        this.onGround = onGround;
        hasPos = false;
        hasLook = true;
        this.time = time;
    }

    public DataPacketFlying(boolean onGround, boolean horizontalCollision, double x, double y, double z, long time) {
        super(x, y, z, 0, 0);
        this.onGround = onGround;
        this.horizontalCollision = horizontalCollision;
        hasPos = true;
        hasLook = false;
        this.time = time;
    }

    public DataPacketFlying(boolean onGround, boolean horizontalCollision, double x, double y, double z, float yaw, float pitch, long time) {
        super(x, y, z, yaw, pitch);
        this.onGround = onGround;
        this.horizontalCollision = horizontalCollision;
        hasPos = true;
        hasLook = true;
        this.time = time;
    }

    /**
     * Test if this is the same location (coordinates + pitch + yaw) as the
     * other given data.
     * 
     * @param other
     * @return False if either packet is lacking hasPos or hasLook, or if any of
     *         x/y/z/pitch/yaw differ, true if all of those match.
     */
    public boolean containsSameLocation(final DataPacketFlying other) {
        return hasPos && other.hasPos && hasLook && other.hasLook && isSameLocation(other);
    }

    /**
     * Test if this packet has pos and look and has the same coordinates and looking direction as the other one.
     * @param other
     * @return
     */
    public boolean containsSameLocation(final DataLocation other) {
        return hasPos && hasLook && isSameLocation(other);
    }

    /**
     * Test if this packet has the same coordinates, disregarding looking direction.
     * @param other
     * @return
     */
    public boolean isSamePos(final DataPacketFlying other) {
        return hasPos && other.hasPos && isSameLocOnly(other);
    }

    /**
     * Quick test if position and look is contained and match.
     * 
     * @param x
     * @param y
     * @param z
     * @param yaw
     * @param pitch
     * @return
     */
    public boolean matches(final double x, final double y, final double z, final float yaw, final float pitch) {
        return hasPos && hasLook && isSameLocation(x, y, z, yaw, pitch);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(256);
        builder.append("Flying(ground=");
        builder.append(onGround);
        builder.append(",horcollide=");
        builder.append(horizontalCollision);
        if (hasPos) {
            builder.append(",x=");
            builder.append(getX());
            builder.append(",y=");
            builder.append(getY());
            builder.append(",z=");
            builder.append(getZ());
        }
        if (hasLook) {
            builder.append(",pitch=");
            builder.append(getPitch());
            builder.append(",yaw=");
            builder.append(getYaw());
        }
        // Skip time for now.
        builder.append(")");
        return builder.toString();
    }

    /**
     * Get the sequence number. This may or may not be set, depending on
     * context.
     * 
     * @return
     */
    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public PACKET_CONTENT getSimplifiedContentType() {
        return hasPos ? (hasLook ? PACKET_CONTENT.POS_LOOK : PACKET_CONTENT.POS) 
                : (hasLook ? PACKET_CONTENT.LOOK : PACKET_CONTENT.GROUND_ONLY);
    }

}
