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
package fr.neatmonster.nocheatplus.compat.versions;

/**
 * A library to translate client protocol IDs to Minecraft versions.  
 * The protocol ID is retrieved through CompatNoCheatPlus currently.<br>
 * Supports clients from 1.7.2 and onwards (protocol indexing was changed with 1.7).
 */
public enum ClientVersion {
    /**
     * 1.7.2 / 1.7.4 / 1.7.5 have the same protocol version.
     */
    V_1_7_2(4),
    /**
     * 1.7.6 / 1.7.7 / 1.7.8 / 1.7.9 / 1.7.10 have the same protocol version.
     */
    V_1_7_10(5),
    
    /**
     * All 1.8 versions share the same protocol ID.
     */
    V_1_8(47),

    V_1_9(107), V_1_9_1(108), V_1_9_2(109),
    /**
     * 1.9.3 / 1.9.4 have the same protocol version.
     */
    V_1_9_3(110),

    V_1_10(210),

    V_1_11(315),
    /**
     * 1.11.1 / 1.11.2 have the same protocol version.
     */
    V_1_11_1(316), 

    V_1_12(335), V_1_12_1(338), V_1_12_2(340),

    V_1_13(393), V_1_13_1(401), V_1_13_2(404),

    V_1_14(477), V_1_14_1(480), V_1_14_2(485), V_1_14_3(490), V_1_14_4(498),

    V_1_15(573), V_1_15_1(575), V_1_15_2(578),

    V_1_16(735), V_1_16_1(736), V_1_16_2(751), V_1_16_3(753),
    /**
     * 1.16.4 / 1.16.5 have the same protocol version.
     */
    V_1_16_4(754),

    V_1_17(755), V_1_17_1(756),

    /**
     * 1.18 / 1.18.1 have the same protocol version.
     */
    V_1_18(757), 
    
    V_1_18_2(758),

    V_1_19(759), V_1_19_1(760), V_1_19_3(761), V_1_19_4(762),
    /**
     * 1.20 / 1.20.1 have the same protocol version.
     */
    V_1_20(763), V_1_20_2(764),
    /**
     * 1.20.3 / 1.20.4 have the same protocol version.
     */
    V_1_20_3(765),
    /**
     * 1.20.5 / 1.20.6 have the same protocol version
     */
     V_1_20_6(766),
    /**
     * 1.21 / 1.21.1 have the same protocol version
     */
    V_1_21(767),
    /**
     * 1.21.2 / 1.21.3 have the same protocol version.
     */
    V_1_21_2(768),
    V_1_21_4(769),


    LOWER_THAN_KNOWN_VERSIONS(V_1_7_2.protocolID - 1, false),
    HIGHER_THAN_KNOWN_VERSIONS(V_1_21_4.protocolID + 1, false),
    UNKNOWN(-1, false);

    private final int protocolID;
    private final String name;

    ClientVersion(int protocolVersion) {
        this.protocolID = protocolVersion;
        this.name = name().substring(2).replace("_", ".");
    }

    ClientVersion(int protocolVersion, boolean knownVersion) {
        this.protocolID = protocolVersion;
        if (!knownVersion) {
            this.name = name();
        } else {
            this.name = name().substring(2).replace("_", ".");
        }
    }

    private static final ClientVersion[] VALUES = values();
    private static final int LOWEST_KNOWN_PROTOCOL_VERSION = LOWER_THAN_KNOWN_VERSIONS.protocolID + 1;
    private static final int HIGHEST_KNOWN_PROTOCOL_VERSION = HIGHER_THAN_KNOWN_VERSIONS.protocolID - 1;

    /**
     * Get a ClientVersion enum by the given protocol version ID.
     *
     * @param protocolVersion the protocol version ID.
     * @return The ClientVersion. UNKNOWN, if unable to determine
     */
    public static ClientVersion getById(int protocolVersion) {
        if (protocolVersion < LOWEST_KNOWN_PROTOCOL_VERSION) {
            return LOWER_THAN_KNOWN_VERSIONS;
        } else if (protocolVersion > HIGHEST_KNOWN_PROTOCOL_VERSION) {
            return HIGHER_THAN_KNOWN_VERSIONS;
        } else {
            for (ClientVersion version : VALUES) {
                if (version.protocolID > protocolVersion) {
                    break;
                } else if (version.protocolID == protocolVersion) {
                    return version;
                }
            }
            return UNKNOWN;
        }
    }
    
    /**
     * Get a ClientVersion enum by the given version name.
     *
     * @param versionName the version name (e.g., "1.19.4").
     * @return The ClientVersion. UNKNOWN, if unable to determine
     */
    public static ClientVersion getByString(String versionName) {
        for (ClientVersion version : VALUES) {
            if (version.name.equals(versionName)) {
                return version;
            }
        }
        return UNKNOWN;
    }

    /**
     * Get the release name of this ClientVersion.
     * For instance, the V_1_19_4 enum constant will parse as a "1.19.4" string.
     *
     * @return The release name
     */
    public String getReleaseName() {
        return name;
    }

    /**
     * Protocol ID of this client version.
     *
     * @return Protocol version ID.
     */
    public int getProtocolVersion() {
        return protocolID;
    }

    /**
     * Get the latest version
     *
     * @return Latest known version
     */
    public static ClientVersion getLatest() {
        return VALUES[VALUES.length-4]; // -3 to ignore unknown, lower_than, higher than; -1 for offset index=>-4 
    }

    /**
     * Get the oldest version
     *
     * @return Oldest known version
     */
    public static ClientVersion getOldest() {
        return VALUES[0];
    }

    /**
     * Test if the given version is between the two given ones.
     * 
     * @param versionLow
     *            Lower version
     * @param includeLow
     *            If to check for equality with the lower version (>=)
     * @param versionHigh
     *            Upper version
     * @param includeHigh
     *            If to check for equality with the higher version (<=)
     * @return true if the client version is in given range
     */
    public boolean isVersionBetween(ClientVersion versionLow, boolean includeLow, ClientVersion versionHigh, boolean includeHigh) {
        if (includeLow) {
            if (isLowerThan(versionLow)) {
                return false;
            }
        } 
        else {
            if (isAtMost(versionLow)) {
                return false;
            }
        }
        if (includeHigh) {
            if (isHigherThan(versionHigh)) {
                return false;
            }
        } 
        else {
            if (isAtLeast(versionHigh)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the player's client version is higher than the given ClientVersion (no equality check)
     *
     * @param target ClientVersion enum to test
     * @return True, if so.
     */
    public boolean isHigherThan(ClientVersion target) {
        return protocolID > target.protocolID;
    }

    /**
     * Check if the player's client version is higher than or equals the given ClientVersion
     *
     * @param target ClientVersion enum to test
     * @return True, if so.
     */
    public boolean isAtLeast(ClientVersion target) {
        return protocolID >= target.protocolID;
    }

     /**
     * Check if the player's client version is lower than the given ClientVersion (no equality check)
     *
     * @param target ClientVersion enum to test
     * @return True, if so.
     */
    public boolean isLowerThan(ClientVersion target) {
        return protocolID < target.protocolID;
    }

    /**
     * Check if the player's client version is lower than or equals the given ClientVersion
     *
     * @param target ClientVersion enum to test
     * @return True, if so.
     */
    public boolean isAtMost(ClientVersion target) {
        return protocolID <= target.protocolID;
    }
    
    /**
     * Test if the player's client version is in range of the given ones.
     * 
     * @param min Minimum client version
     * @param max Maximum client version
     * @return True, if the client version is in between or equals the given ones.
     */
    public boolean isInRange(ClientVersion min, ClientVersion max) {
        return isAtLeast(min) && isAtMost(max);
    }
    
    /**
     * Test if the player's client version is in between the given ones.
     *      
     * @param min
     * @param max
     * @return True, if the client version is in between the min and max versions (no equality)
     */
    public boolean isBetween(ClientVersion min, ClientVersion max) {
        return isHigherThan(min) && isLowerThan(max);
    }
}
