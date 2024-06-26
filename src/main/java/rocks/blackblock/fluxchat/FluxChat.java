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

package rocks.blackblock.fluxchat;

import rocks.blackblock.fluxchat.api.FluxChatApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton for the {@link FluxChatApi}.
 */
public final class FluxChat {

    private static FluxChatApi api = null;

    private FluxChat() {
    }

    public static FluxChatApi getApi() {
        return api;
    }

    static void setApi(FluxChatApi api) {
        FluxChat.api = api;
    }

    public static List<String> searchList(String query, List<String> entries) {

        query = query.toLowerCase().trim();

        if (query.isBlank()) {
            return entries;
        }

        List<String> result = new ArrayList<>();

        for (String entry : entries) {
            if (entry.toLowerCase().contains(query)) {
                result.add(entry);
            }
        }

        return result;
    }

}
