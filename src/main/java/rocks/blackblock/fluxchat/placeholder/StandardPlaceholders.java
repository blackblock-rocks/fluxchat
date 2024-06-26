/*
 * This file is part of FluxChat, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package rocks.blackblock.fluxchat.placeholder;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import rocks.blackblock.fluxchat.FluxChatPlayer;
import rocks.blackblock.fluxchat.api.FluxChatPlaceholder;
import net.kyori.adventure.text.TextComponent;

public class StandardPlaceholders implements FluxChatPlaceholder {

    private static final String TPS_FORMAT = "%.1f";
    private static final String MSPT_FORMAT = "%.1f";

    /**
     * Looks for a TextComponent replacement for the given definition.
     *
     * @param   player       The associated player
     * @param   definition   The placeholder definition, without the outer "{ }" brackets.
     *
     * @return   A replacement, or null if the definition cannot be satisfied by this {@link FluxChatPlaceholder}
     */
    @Override
    public TextComponent lookupTextComponentReplacement(Player player, String definition) {

        definition = definition.toLowerCase();

        switch (definition) {
            case "tab_display_name":
                return FluxChatPlayer.get(player).getTabDisplayName();
        };

        return null;
    }

    @Override
    public String lookupStringReplacement(Player player, String definition) {

        // dynamic placeholders
        if (definition.startsWith("has_perm_") && definition.length() > "has_perm_".length()) {
            String perm = definition.substring("has_perm_".length());
            return Boolean.toString(player.hasPermission(perm));
        }

        String result = null;

        // static placeholders
        switch (definition.toLowerCase()) {
            case "username":
                return player.getUsername();
            case "name":
            case "display_username": // Velocity doesn't have display names
            case "display_name":
                result = FluxChatPlayer.get(player).getDisplayName();
                break;
            case "coloured_display_name":
                result = FluxChatPlayer.get(player).getColouredDisplayName();
                break;
            case "server_name":
                ServerConnection connection = player.getCurrentServer().orElse(null);

                if (connection == null) {
                    return "unknown";
                }

                return connection.getServerInfo().getName();
            case "uuid":
                return player.getUniqueId().toString();
            case "pronouns":
                result = FluxChatPlayer.get(player).getPronouns();
                break;
            case "pronouns_suffix":
                result = FluxChatPlayer.get(player).getPronouns();

                if (result != null && !result.isBlank()) {
                    result = " (" + result + ")";
                }

                break;
            case "timezone":
                result = FluxChatPlayer.get(player).getTimezone();
                break;
            case "now":
                result = FluxChatPlayer.get(player).getCurrentTime();
                break;
            case "server_load":
                result = ""+ FluxChatPlayer.get(player).getServerLoad();
                break;
            case "server_load_coloured":
                int load = FluxChatPlayer.get(player).getServerLoad();

                if (load > 100) {
                    // Dark red
                    result = "&4" + load;
                } else if (load > 85) {
                    // Red
                    result = "&c" + load;
                } else if (load > 65) {
                    // Gold
                    result = "&6" + load;
                } else {
                    // Dark green
                    result = "&2" + load;
                }
                break;
            case "mspt":
                result = String.format(MSPT_FORMAT, FluxChatPlayer.get(player).getMSPT());
                break;
            case "tps":
                result = String.format(TPS_FORMAT, FluxChatPlayer.get(player).getTPS());
                break;
            case "ping":
                result = FluxChatPlayer.get(player).getPingString();
                break;
            default:
                return null;
        }

        if (result == null) {
            result = "";
        }

        return result;
    }
}
