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

import com.google.gson.JsonObject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import rocks.blackblock.fluxchat.api.FluxChatFormat;
import rocks.blackblock.fluxchat.api.events.FluxChatEvent;
import rocks.blackblock.fluxchat.api.events.FluxChatMessageFormedEvent;
import rocks.blackblock.fluxchat.api.events.FluxChatMessageSendEvent;
import rocks.blackblock.fluxchat.config.FluxChatConfig;
import rocks.blackblock.fluxchat.placeholder.PlaceholderParameters;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class FluxChatListener {
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)([§&])[0-9A-FK-OR]");

    private final FluxChatPlugin plugin;
    private final LegacyComponentSerializer legacyLinkingSerializer;

    public FluxChatListener(FluxChatPlugin plugin) {
        this.plugin = plugin;
        this.legacyLinkingSerializer = LegacyComponentSerializer.builder()
                .character('&')
                .extractUrls(plugin.getConfig().getLinkStyle())
                .build();

    }

    /**
     * Someone has connected to the network
     */
    @Subscribe(order = PostOrder.NORMAL)
    public void onLogin(LoginEvent e) {
        Player player = e.getPlayer();

        if (FluxChatPlugin.shouldPushEvents()) {
            JsonObject object = FluxChatPlugin.createObject("login", player);
            FluxChatPlugin.pushEvent(object);
        }

        FluxChatPlayer gplayer = FluxChatPlayer.get(player);
        TextComponent message = gplayer.format("login", null);

        if (message == null) {
            return;
        }

        this.broadcastMessage(message);
    }

    public PlaceholderParameters getServerParameters(ServerInfo info) {
        PlaceholderParameters parameters = new PlaceholderParameters();
        parameters.set("server", info.getName());
        parameters.set("server_name", info.getName());
        return parameters;
    }

    @Subscribe(order = PostOrder.NORMAL)
    public void onJoinServer(ServerConnectedEvent e) {
        Player player = e.getPlayer();

        // Force clear the cached player
        FluxChatPlayer.remove(player);

        RegisteredServer server = e.getServer();
        ServerInfo info = server.getServerInfo();

        if (FluxChatPlugin.shouldPushEvents()) {
            JsonObject object = FluxChatPlugin.createObject("join", player, e.getServer().getServerInfo());
            FluxChatPlugin.pushEvent(object);
        }

        FluxChatPlayer gplayer = FluxChatPlayer.get(player);
        TextComponent message = gplayer.format("join", this.getServerParameters(info));

        if (message == null) {
            return;
        }

        this.broadcastMessage(message);
    }

    @Subscribe(order = PostOrder.NORMAL)
    public void onLogout(DisconnectEvent e) {
        Player player = e.getPlayer();

        if (FluxChatPlugin.shouldPushEvents()) {
            JsonObject object = FluxChatPlugin.createObject("logout", player);
            FluxChatPlugin.pushEvent(object);
        }

        FluxChatPlayer gplayer = FluxChatPlayer.get(player);
        TextComponent message = gplayer.format("logout", null);

        if (message == null) {
            return;
        }

        this.broadcastMessage(message);
    }

    /**
     * Listen for PlayerChat events and broadcast them to every server
     */
    @Subscribe(order = PostOrder.NORMAL)
    public void onChat(PlayerChatEvent e) {
        Player player = e.getPlayer();
        ProxyServer proxy = plugin.getProxy();

        FluxChatEvent fluxChatEvent = new FluxChatEvent(player, e);
        proxy.getEventManager().fire(fluxChatEvent).join();

        if (!fluxChatEvent.getResult().isAllowed()) {
            return;
        }

        FluxChatConfig config = plugin.getConfig();

        // are permissions required to send chat messages?
        // does the player have perms to send the message
        if (config.isRequireSendPermission() && !player.hasPermission("FluxChat.send")) {

            // if the message should be passed through when the player doesn't have the permission
            if (config.isRequirePermissionPassthrough()) {
                // just return. the default behaviour is for the message to be passed to the backend.
                return;
            }

            // they don't have permission, and the message shouldn't be passed to the backend.
            e.setResult(PlayerChatEvent.ChatResult.denied());

            Component failMessage = config.getRequireSendPermissionFailMessage();
            if (failMessage != null) {
                player.sendMessage(failMessage);
            }

            return;
        }

        FluxChatPlayer gplayer = FluxChatPlayer.get(player);

        // Simple chat messages takes a player out of AFK
        gplayer.setAfk(false);

        TextComponent message = Component.text(e.getMessage());

        PlaceholderParameters parameters = new PlaceholderParameters();
        parameters.set("message", message);

        TextComponent outgoing_message = gplayer.format("chat", parameters);

        // couldn't find a format for the player
        if (outgoing_message == null) {
            if (!config.isPassthrough()) {
                e.setResult(PlayerChatEvent.ChatResult.denied());
            }

            return;
        }

        // we have a format, so cancel the event.
        e.setResult(PlayerChatEvent.ChatResult.denied());

        PlaceholderParameters self_parameters = new PlaceholderParameters();
        message = message.color(NamedTextColor.AQUA);
        self_parameters.set("message", message);

        TextComponent self_message = gplayer.format("chat", self_parameters);

        FluxChatFormat format = plugin.getFormat(player, "chat").orElse(null);

        FluxChatMessageFormedEvent formedEvent = new FluxChatMessageFormedEvent(player, format, e.getMessage(), outgoing_message);
        proxy.getEventManager().fireAndForget(formedEvent);

        if (config.isLogChatGlobal()) {
            plugin.getLogger().info(PlainTextComponentSerializer.plainText().serialize(outgoing_message));
        }

        Map<ServerInfo, TextComponent> server_output = new HashMap<>();
        TextComponent current_message = null;

        // send the message to online players
        for (Player p : proxy.getAllPlayers()) {
            boolean cancelled = plugin.getConfig().isRequireReceivePermission() && !player.hasPermission("FluxChat.receive");
            FluxChatMessageSendEvent sendEvent = new FluxChatMessageSendEvent(player, p, format, e.getMessage(), cancelled);
            proxy.getEventManager().fire(sendEvent).join();

            if (!sendEvent.getResult().isAllowed()) {
                continue;
            }

            ServerConnection server_connection = p.getCurrentServer().orElse(null);

            if (server_connection == null) {
                continue;
            }

            ServerInfo server_info = server_connection.getServerInfo();

            if (server_info == null) {
                continue;
            }

            if (player.getUniqueId().equals(p.getUniqueId())) {
                current_message = gplayer.formatForServer(server_info, "chat", self_parameters);
            } else {
                current_message = gplayer.formatForServer(server_info, "chat", parameters);
            }

            p.sendMessage(player, current_message);
        }
    }

    /**
     * Send to all players
     */
    public void broadcastMessage(TextComponent message) {

        ProxyServer proxy = plugin.getProxy();

        for (Player p : proxy.getAllPlayers()) {
            p.sendMessage(message);
        }
    }
}
