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
package fr.neatmonster.nocheatplus.compat.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.NoCheatPlus;
import fr.neatmonster.nocheatplus.checks.inventory.Gutenberg;
import fr.neatmonster.nocheatplus.checks.inventory.HotFixFallingBlockPortalEnter;
import fr.neatmonster.nocheatplus.checks.net.protocollib.ProtocolLibComponent;
import fr.neatmonster.nocheatplus.components.registry.activation.Activation;
import fr.neatmonster.nocheatplus.components.registry.activation.IActivation;
import fr.neatmonster.nocheatplus.components.registry.activation.IDescriptiveActivation;
import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigManager;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.logging.Streams;
import fr.neatmonster.nocheatplus.utilities.StringUtil;

/**
 * Default factory for add-in components which might only be available under certain circumstances.
 * 
 * @author asofold
 */
public class DefaultComponentFactory {

    private final IActivation protocolLibPresent = new Activation().pluginExist("ProtocolLib");
    private final List<IDescriptiveActivation> protocolLibActivation = new ArrayList<IDescriptiveActivation>();

    public DefaultComponentFactory() {
        for (Activation condition : Arrays.asList(
                new Activation()
                .neutralDescription("ProtocolLib 5.3.0 or later for Minecraft 1.21.x")
                .pluginVersionGT("ProtocolLib", "5.3.0", true)
                .minecraftVersionBetween("1.21", true, "1.22", false)
                .advertise(true)
                ,
                new Activation()
                .neutralDescription("ProtocolLib 5.1.0 or later for Minecraft 1.20.x")
                .pluginVersionGT("ProtocolLib", "5.1.0", true)
                .minecraftVersionBetween("1.20", true, "1.21", false)
                .advertise(true)
                ,
                new Activation()
                .neutralDescription("ProtocolLib 5.0.0 or later for Minecraft 1.19.x")
                .pluginVersionGT("ProtocolLib", "5.0.0", true)
                .minecraftVersionBetween("1.19", true, "1.20", false)
                .advertise(true)
                ,
                new Activation()
                .neutralDescription("ProtocolLib 4.8.0 or later for Minecraft 1.18.x")
                .pluginVersionGT("ProtocolLib", "4.8.0", true)
                .minecraftVersionBetween("1.18", true, "1.19", false)
                .advertise(true)
                ,
                new Activation()
                .neutralDescription("ProtocolLib 4.7.0 to 4.7.2 for Minecraft 1.17.x")
                .pluginVersionGT("ProtocolLib", "4.7.0", true)
                .minecraftVersionBetween("1.17", true, "1.18", false)
                .advertise(true)
                ,
                new Activation()
                .neutralDescription("ProtocolLib 4.6.0 or later for Minecraft 1.16.x")
                .pluginVersionGT("ProtocolLib", "4.6.0", true)
                .minecraftVersionBetween("1.16", true, "1.17", false)
                .advertise(true)
                ,
                new Activation()
                .neutralDescription("ProtocolLib 4.5.0 or later for Minecraft 1.14.x to 1.15.x")
                .pluginVersionGT("ProtocolLib", "4.5.0", true)
                .minecraftVersionBetween("1.14", true, "1.16", false)
                .advertise(true)
                ,
                new Activation()
                .neutralDescription("ProtocolLib 4.4.0 or later for Minecraft 1.13.x")
                .pluginVersionGT("ProtocolLib", "4.4.0", true)
                .minecraftVersionBetween("1.13", true, "1.14", false)
                .advertise(true)
                ,
                new Activation()
                .neutralDescription("ProtocolLib 4.3.0 or later for Minecraft 1.12.x")
                .pluginVersionGT("ProtocolLib", "4.3.0", true)
                .minecraftVersionBetween("1.12", true, "1.13", false)
                .advertise(true)
                ,
                new Activation()
                .neutralDescription("ProtocolLib 4.2.0 or later for Minecraft 1.11.x")
                .pluginVersionGT("ProtocolLib", "4.2.0", true)
                .minecraftVersionBetween("1.11", true, "1.12", false)
                .advertise(true)
                ,
                new Activation()
                .neutralDescription("ProtocolLib 4.1 or later for Minecraft 1.8.x to 1.10.x")
                .pluginVersionGT("ProtocolLib", "4.1", true)
                .minecraftVersionBetween("1.8", true, "1.11", false)
                .advertise(true)
                ,
                new Activation()
                .neutralDescription("ProtocolLib 4.0.2 for Minecraft 1.10.x")
                .pluginVersionBetween("ProtocolLib", "4.0.2", true, "4.1", false)
                .minecraftVersionBetween("1.10", true, "1.11", false)
                ,
                new Activation()
                .neutralDescription("ProtocolLib 4.0.1 or 4.0.0 for Minecraft 1.9.x")
                .pluginVersionBetween("ProtocolLib", "4.0.0", true, "4.0.1", true)
                .minecraftVersionBetween("1.9", true, "1.10", false)
                ,
                new Activation()
                .neutralDescription("ProtocolLib 3.7.0 for Minecraft 1.7.x and earlier")
                .pluginVersionBetween("ProtocolLib", "3.7", true, "3.7.0", true)
                .minecraftVersionLT("1.10", false) // Allowed, but not necessarily recommended.
                .advertise(true)
                ,
                new Activation()
                .neutralDescription("ProtocolLib 3.6.5 or 3.6.4 for Minecraft 1.8.x")
                .pluginVersionBetween("ProtocolLib", "3.6.4", true, "3.6.5", true)
                .minecraftVersionBetween("1.8", true, "1.9", false)
                ,
                new Activation()
                .neutralDescription("ProtocolLib 3.6.6 for PaperSpigot 1.8.x")
                .pluginVersionEQ("ProtocolLib", "3.6.6")
                .serverVersionContainsIgnoreCase("paperspigot")
                .minecraftVersionBetween("1.8", true, "1.9", false)
                .advertise(true)
                ,
                new Activation()
                .neutralDescription("ProtocolLib 3.6.4 before Minecraft 1.9")
                .pluginVersionEQ("ProtocolLib", "3.6.4")
                .minecraftVersionBetween("1.2.5", true, "1.9", false)
                )) {
            protocolLibActivation.add(condition);
        };
    }

    /**
     * This will be called from within the plugin in onEnable, after registration of all core listeners and components. After each components addition processQueuedSubComponentHolders() will be called to allow registries for further optional components.
     * @param plugin 
     * @return
     */
    public Collection<Object> getAvailableComponentsOnEnable(NoCheatPlus plugin){
        final List<Object> available = new LinkedList<Object>();

        // Add components (try-catch).
        // TODO: catch ClassNotFound, incompatibleXY rather !?
        // Hot fix: falling block end portal.
        try {
            HotFixFallingBlockPortalEnter.testAvailability();
            available.add(new HotFixFallingBlockPortalEnter());
            NCPAPIProvider.getNoCheatPlusAPI().addFeatureTags("checks", Arrays.asList(HotFixFallingBlockPortalEnter.class.getSimpleName()));
        }
        catch (RuntimeException e) {}

        // ProtocolLib dependencies.
        if (protocolLibPresent.isAvailable()) {
            // Check conditions.
            boolean protocolLibAvailable = false;
            for (final IActivation condition : protocolLibActivation) {
                if (condition.isAvailable()) {
                    protocolLibAvailable = true;
                    break;
                }
            }
            // Attempt to react.
            if (protocolLibAvailable) {
                try {
                    available.add(new ProtocolLibComponent(plugin));
                } catch (Throwable t){
                    StaticLog.logWarning("Failed to set up packet level access with ProtocolLib.");
                    if (ConfigManager.getConfigFile().getBoolean(ConfPaths.LOGGING_EXTENDED_STATUS)) {
                        NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.INIT, t);
                    }
                }
            }
            else {
                List<String> parts = new LinkedList<String>();
                parts.add("Packet level access via ProtocolLib not available, supported configurations: ");
                for (IDescriptiveActivation cond : protocolLibActivation) {
                    if (cond.advertise()) {
                        parts.add(cond.getNeutralDescription());
                    }
                }
                StaticLog.logWarning(StringUtil.join(parts, " | "));
            }
        }
        else {
            StaticLog.logInfo("Packet level access: ProtocolLib is not available.");
        }

        return available;
    }
}
