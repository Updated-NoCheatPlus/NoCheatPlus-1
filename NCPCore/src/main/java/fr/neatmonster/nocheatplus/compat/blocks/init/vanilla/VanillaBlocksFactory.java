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
package fr.neatmonster.nocheatplus.compat.blocks.init.vanilla;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import fr.neatmonster.nocheatplus.compat.blocks.BlockPropertiesSetup;
import fr.neatmonster.nocheatplus.compat.blocks.IPatchBlockPropertiesSetup;
import fr.neatmonster.nocheatplus.compat.blocks.init.vanilla.special.MultiClientProtocolBlockShapePatch;
import fr.neatmonster.nocheatplus.config.WorldConfigProvider;
import fr.neatmonster.nocheatplus.logging.StaticLog;

public class VanillaBlocksFactory {

    public Collection<String> setupVanillaBlocks(final WorldConfigProvider<?> worldConfigProvider) {
        // Vanilla blocks (abort with first failure, low to high MC version).
        final List<BlockPropertiesSetup> setups = new LinkedList<BlockPropertiesSetup>();
        final List<String> success = new LinkedList<String>();
        try{
            setups.add(new BlocksMC1_5());
            setups.add(new BlocksMC1_6_1());
            setups.add(new BlocksMC1_7_2());
            setups.add(new BlocksMC1_8());
            setups.add(new BlocksMC1_9());
            setups.add(new BlocksMC1_10());
            setups.add(new BlocksMC1_11());
            setups.add(new BlocksMC1_12());
            setups.add(new BlocksMC1_13());
            setups.add(new BlocksMC1_14());
            setups.add(new BlocksMC1_15());
            setups.add(new BlocksMC1_16());
            setups.add(new BlocksMC1_17());
        }
        catch(Throwable t){}
        for (final BlockPropertiesSetup setup : setups){
            try{
                // Assume the blocks setup to message success.
                setup.setupBlockProperties(worldConfigProvider);
                success.add(setup.getClass().getSimpleName());
                // TODO: Do logging from here ?
            }
            catch(Throwable t){
                StaticLog.logSevere(setup.getClass().getSimpleName() + ".setupBlockProperties could not execute properly: " + t.getClass().getSimpleName() + " - " + t.getMessage());
                StaticLog.logSevere(t);
                // Abort further processing.
                break;
            }
        }
        // Patches for special circumstances.
        for (IPatchBlockPropertiesSetup patch : new IPatchBlockPropertiesSetup[]{
                new MultiClientProtocolBlockShapePatch(),
        }) {
            try {
                if (patch.isAvailable()) {
                    patch.setupBlockProperties(worldConfigProvider);
                    String description = patch.getNeutralDescription();
                    if (description == null || description.isEmpty()) {
                        description = patch.getClass().getSimpleName();
                    }
                    StaticLog.logInfo("Update block-info: " + description);
                }
            }
            catch (Throwable t) {
                StaticLog.logSevere(patch.getClass().getSimpleName() + " could not be processed: " + t.getClass().getSimpleName() + " - " + t.getMessage());
                StaticLog.logSevere(t);
            }
        }
        return success;
    }

}
