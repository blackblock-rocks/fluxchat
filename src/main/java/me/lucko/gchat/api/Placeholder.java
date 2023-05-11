/*
 * This file is part of gChat, licensed under the MIT License.
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

package me.lucko.gchat.api;

import com.velocitypowered.api.proxy.Player;
import me.lucko.gchat.GChatPlayer;
import me.lucko.gchat.placeholder.StringSplitter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a placeholder replacement.
 */
public interface Placeholder {

    /**
     * Looks for a simple String replacement for the given definition.
     *
     * @param   player       The associated player
     * @param   definition   The placeholder definition, without the outer "{ }" brackets.
     *
     * @return   A replacement, or null if the definition cannot be satisfied by this {@link Placeholder}
     */
    String lookupStringReplacement(Player player, String definition);

    /**
     * Looks for a TextComponent replacement for the given definition.
     *
     * @param   player       The associated player
     * @param   definition   The placeholder definition, without the outer "{ }" brackets.
     *
     * @return   A replacement, or null if the definition cannot be satisfied by this {@link Placeholder}
     */
    default TextComponent lookupTextComponentReplacement(Player player, String definition) {
        return null;
    }

    /**
     * Get the replacement as a component.
     * This method will first attempt to use {@link #lookupTextComponentReplacement(Player, String)}.
     * If that returns null, it will fall back to {@link #lookupStringReplacement(Player, String)}.
     * It will not parse the result of the string lookup again.
     *
     * @param   player       The associated player
     * @param   definition   The placeholder definition, without the outer "{ }" brackets.
     *
     * @return   A replacement, or null if the definition cannot be satisfied by this {@link Placeholder}
     */
    default TextComponent getTextComponentReplacement(Player player, String definition) {

        // If a TextComponent already exists, use that one
        TextComponent result = this.lookupTextComponentReplacement(player, definition);

        if (result != null) {
            return result;
        }

        String replacement = this.lookupStringReplacement(player, definition);

        if (replacement == null) {
            return null;
        }

        return Component.text(replacement);
    }

    /**
     * Get the replacement as a component
     *
     * @param   player       The associated player
     * @param   entry        The placeholder definition, as a StringSplitter.Entry (containing extra info)
     *
     * @return   A replacement, or null if the definition cannot be satisfied by this {@link Placeholder}
     */
    default TextComponent getTextComponentReplacement(Player player, @NotNull StringSplitter.Entry entry) {

        String key = entry.getContent();

        // If a TextComponent already exists, use that one
        TextComponent result = this.lookupTextComponentReplacement(player, key);

        if (result != null) {
            return result;
        }

        String replacement = this.lookupStringReplacement(player, key);

        if (replacement == null) {
            return null;
        }

        if (entry.allowsDecoration()) {
            return GChatPlayer.get(player).convertString(null, replacement, null);
        }

        return Component.text(replacement);
    }
}
