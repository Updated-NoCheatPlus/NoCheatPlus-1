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
package fr.neatmonster.nocheatplus.checks.chat;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.ColorUtil;

/**
 * NOTE: EARLY REFACTORING STATE, MOST METHODS NEED SYNC OVER DATA !
 * @author mc_dev
 *
 */
public class Captcha extends Check implements ICaptcha{

    public Captcha() {
        super(CheckType.CHAT_CAPTCHA);
    }

    @Override
    public void checkCaptcha(Player player, String message, ChatConfig cc, ChatData data, boolean isMainThread) {
        // Correct answer to the captcha?
        if (message.equals(data.captchaGenerated)) {
            // Yes, clear their data and do not worry anymore about them.
            data.reset();
            data.captchaStarted = false;
            player.sendMessage(ColorUtil.replaceColors(cc.captchaSuccess));
        } else {
            // Increment their tries number counter.
            data.captchTries++;
            data.captchaVL ++;
            // Have they failed too man times?
            if (data.captchTries > cc.captchaTries) {
                // Find out if we need to kick the player or not.
                executeActions(player, data.captchaVL, 1, cc.captchaActions);
                // (Resetting captcha tries is done on quit/kick).
            }

            // Display the question again (if not kicked).
            if (player.isOnline()) {
                sendCaptcha(player, cc, data);
            }
        }
    }

    @Override
    public void sendNewCaptcha(Player player, ChatConfig cc, ChatData data) {
        // Display a captcha to the player.
        generateCaptcha(cc, data, true);
        sendCaptcha(player, cc, data);
        data.captchaStarted = true;
    }

    @Override
    public void generateCaptcha(ChatConfig cc, ChatData data, boolean reset) {
        if (reset) data.captchTries = 0;
        final char[] chars = new char[cc.captchaLength];
        for (int i = 0; i < cc.captchaLength; i++) {
            chars[i] = cc.captchaCharacters.charAt(
                ThreadLocalRandom.current().nextInt(cc.captchaCharacters.length()));
        }
        data.captchaGenerated = new String(chars);
    }

    @Override
    public void resetCaptcha(Player player){
        final IPlayerData pData = DataManager.getPlayerData(player);
        ChatData data = pData.getGenericInstance(ChatData.class);
        synchronized (data) {
            resetCaptcha(player, pData.getGenericInstance(ChatConfig.class), data, pData);
        }
    }

    @Override
    public void resetCaptcha(Player player, ChatConfig cc, ChatData data, IPlayerData pData){
        data.captchTries = 0;
        if (shouldCheckCaptcha(player, cc, data, pData) 
                || shouldStartCaptcha(player, cc, data, pData)){
            generateCaptcha(cc, data, true);
        }
    }

    @Override
    public void sendCaptcha(Player player, ChatConfig cc, ChatData data) {
        player.sendMessage(ColorUtil.replaceColors(cc.captchaQuestion.replace("[captcha]",
                data.captchaGenerated)));
    }

    @Override
    public boolean shouldStartCaptcha(Player player, ChatConfig cc, ChatData data, IPlayerData pData) {
        // TODO: Only call if IWorldData.isCheckActive(CHAT_CAPTCHA) has returned true?
        return !data.captchaStarted && pData.isCheckActive(CheckType.CHAT_CAPTCHA, player);
    }

    @Override
    public boolean shouldCheckCaptcha(Player player, ChatConfig cc, ChatData data, IPlayerData pData) {
        // TODO: Only call if IWorldData.isCheckActive(CHAT_CAPTCHA) has returned true?
        return data.captchaStarted  && pData.isCheckActive(CheckType.CHAT_CAPTCHA, player);
    }

}
