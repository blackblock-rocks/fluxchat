package me.lucko.gchat;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import me.lucko.gchat.api.ChatFormat;
import me.lucko.gchat.monitoring.ErrorSentry;
import me.lucko.gchat.placeholder.StringSplitter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class GChatPlayer {

    public Player player;
    public final LuckPerms luckperms;
    public final User user;

    protected String pronouns = null;
    protected String timezone = null;
    protected String nickname = null;
    protected String nickname_color = null;
    protected DateFormat date_format = null;
    public static final Map<String, String> COLOR_MAP = new HashMap<>();

    public static final HashMap<UUID, GChatPlayer> CACHE = new HashMap<>();
    public static LegacyComponentSerializer legacyLinkingSerializer = null;
    public static MiniMessage miniMessage = null;

    // Custom placeholders per server
    private Map<ServerInfo, Map<String, TextComponent>> server_placeholders = new HashMap<>();

    /**
     * Initialize the instance
     *
     * @author   Jelle De Loecker
     * @since    3.0.2
     */
    public GChatPlayer(Player player) {
        this.player = player;
        this.luckperms = LuckPermsProvider.get();
        this.user = luckperms.getPlayerAdapter(Player.class).getUser(player);
        this.init();
    }

    /**
     * Initialize some basic data
     *
     * @author   Jelle De Loecker
     * @since    3.1.0
     */
    private void init() {
        this.pronouns = this.getMetaValue("pronouns");
        this.timezone = this.getMetaValue("timezone");
        this.nickname = this.getMetaValue("nickname");
        this.nickname_color = this.getMetaValue("nickname_color");
        this.fixNickname();
    }

    /**
     * Set a server-specific placeholder
     *
     * @author   Jelle De Loecker
     * @since    3.1.0
     */
    public void setServerPlaceholder(ServerInfo server_info, String key, String value) {
        Map<String, TextComponent> placeholders = this.server_placeholders.computeIfAbsent(server_info, k -> new HashMap<>());

        Component component = null;
        TextComponent result = null;

        try {
            component = GsonComponentSerializer.gson().deserialize(value);
        } catch (Exception e) {
            ErrorSentry.capture(e);
            return;
        }

        if (component instanceof TextComponent text_component) {
            result = text_component;
        }

        System.out.println("Got placeholder " + key + " revived to " + component + " and got a text " + result);

        placeholders.put(key, result);
    }

    /**
     * Get a server-specific placeholder
     *
     * @author   Jelle De Loecker
     * @since    3.1.0
     */
    @Nullable
    public TextComponent getServerPlaceholder(ServerInfo server_info, String key) {
        Map<String, TextComponent> placeholders = this.server_placeholders.get(server_info);

        if (placeholders == null) {
            return null;
        }

        return placeholders.get(key);
    }


    /**
     * Get the coloured displayname
     *
     * @author   Jelle De Loecker
     * @since    3.1.0
     */
    public String getColouredDisplayName() {

        String display_name = this.getDisplayName();

        if (this.nickname_color != null) {
            String color_tag = COLOR_MAP.get(this.nickname_color);

            if (color_tag != null) {
                display_name = "<" + color_tag + ">" + display_name + "</" + color_tag + ">";
            }
        }

        return display_name;
    }

    /**
     * Get the displayname
     *
     * @author   Jelle De Loecker
     * @since    3.0.2
     */
    public String getDisplayName() {

        if (this.nickname == null || this.nickname.isBlank()) {
            return this.player.getUsername();
        }

        return this.nickname;
    }

    /**
     * Get the nickname color
     *
     * @author   Jelle De Loecker
     * @since    3.0.2
     */
    public String getNicknameColor() {
        return this.nickname_color;
    }

    public void setNicknameColor(String color_code) {
        this.nickname_color = color_code;
        this.setMetaNode("nickname_color", color_code);
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
        this.fixNickname();
        this.setMetaNode("nickname", this.nickname);
    }

    private void fixNickname() {
        if (this.nickname != null && this.nickname.contains("§")) {
            this.nickname = this.nickname.replace("§", "");
        }
    }

    public String getPronouns() {
        return this.pronouns;
    }

    public void setPronouns(String pronouns) {
        this.pronouns = pronouns;
        this.setMetaNode("pronouns", pronouns);
    }

    public String getTimezone() {
        return this.timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
        this.setMetaNode("timezone", timezone);
        this.updateTimeZone();
    }

    protected void updateTimeZone() {
        if (this.timezone != null) {
            try {
                this.date_format.setTimeZone(TimeZone.getTimeZone(this.timezone));
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    public DateFormat getDateFormat() {

        if (this.date_format == null) {
            this.date_format = new SimpleDateFormat("HH:mm");
            this.updateTimeZone();
        }

        return this.date_format;
    }

    public String getCurrentTime() {
        Date now = new Date();
        return this.getDateFormat().format(now);
    }

    /**
     * Get a metadata node
     * @param   name   The name of the value to get
     */
    public String getMetaValue(String name) {
        return luckperms.getPlayerAdapter(Player.class).getMetaData(this.player).getMetaValue(name);
    }

    /**
     * Remove a meta node (always needed when a value should be set)
     * @param   name   The name of the value to remove
     */
    protected void removeMetaNode(String name) {
        // clear any existing meta nodes with the same key - we want to override
        user.data().clear(NodeType.META.predicate(mn -> mn.getMetaKey().equals(name)));
    }

    /**
     * Set a meta node value
     *
     * @param name   The name of the node to set
     * @param value  The actual value
     */
    public void setMetaNode(String name, String value) {
        this.removeMetaNode(name);

        if (value != null) {
            MetaNode value_node = MetaNode.builder(name, value).build();
            user.data().add(value_node);
        }

        luckperms.getUserManager().saveUser(user);
    }

    /**
     * Get this player's server
     */
    public ServerConnection getServer() {

        ServerConnection server = null;

        if (this.player != null) {
            server = this.player.getCurrentServer().orElse(null);
        }

        if (server == null) {
            System.out.println("[GChat] Player " + this.player.getUsername() + " is not connected to a server?");

            Optional<Player> player = GChatPlugin.instance.getProxy().getPlayer(this.player.getUniqueId());

            if (player.isPresent()) {
                this.player = player.get();
                System.out.println("[GChat] Refetching player " + this.player.getUsername() + " server.");
                server = this.player.getCurrentServer().orElse(null);
            } else {
                GChatPlayer.remove(this.player);
            }
        }

        return server;
    }

    /**
     * Get the serverload of the server this player is on
     */
    public int getServerLoad() {

        ServerConnection server = this.getServer();

        if (server == null) {
            return -1;
        }

        return GChatPlugin.instance.getServerLoad(server);
    }

    /**
     * Get the MSPT of the server this player is on
     */
    public float getMSPT() {

        ServerConnection server = this.getServer();

        if (server == null) {
            return -1f;
        }

        return GChatPlugin.instance.getServerMSPT(server);
    }

    /**
     * Get the TPS of the server this player is on
     */
    public float getTPS() {

        ServerConnection server = this.getServer();

        if (server == null) {
            return -1f;
        }

        return GChatPlugin.instance.getServerTPS(server);
    }

    /**
     * Get the ping of this player as a string
     */
    public String getPingString() {

        long ping = this.player.getPing();

        if (ping == -1) {
            return "?";
        }

        return "" + ping;
    }

    /**
     * Send a message to this player
     *
     * @author   Jelle De Loecker
     * @since    3.1.0
     */
    public void sendMessage(TextComponent message) {
        this.player.sendMessage(message);
    }

    /**
     * Broadcast the message to everyone
     * (Except this player)
     *
     * @author   Jelle De Loecker
     * @since    3.1.0
     */
    public void broadcast(TextComponent message) {
        this.broadcast(message, false);
    }

    /**
     * Broadcast the message to everyone
     *
     * @author   Jelle De Loecker
     * @since    3.1.0
     */
    public void broadcast(TextComponent message, boolean include_self) {
        for (Player player : GChatPlugin.instance.getProxy().getAllPlayers()) {

            if (player == this.player && !include_self) {
                continue;
            }

            GChatPlayer gplayer = GChatPlayer.get(player);
            gplayer.sendMessage(message);
        }
    }

    /**
     * Format the given message,
     * but use server-specific placeholders first.
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    public TextComponent formatForServer(ServerInfo server_info, String format_name, @Nullable Map<String, String> parameters) {

        // Get the format to use for this player
        ChatFormat format = GChatPlugin.instance.getFormat(this.player, format_name).orElse(null);

        if (format == null) {
            ErrorSentry.logWarning("Failed to find format '" + format_name + "'");
            return null;
        }

        return this.formatForServer(server_info, format, parameters);
    }

    /**
     * Format the given message,
     * but use server-specific placeholders first.
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    public TextComponent formatForServer(ServerInfo server_info, @NotNull ChatFormat format, @Nullable Map<String, String> parameters) {

        // Get the actual text pattern. This might contain placeholders.
        String main_source = format.getFormatText();

        if (main_source == null) {
            return null;
        }

        TextComponent main_text = this.convertString(server_info, main_source, parameters);

        if (main_text == null) {
            return null;
        }

        String hover = format.getHoverText();
        String click_value = format.getClickValue();

        if (hover == null && click_value == null) {
            return main_text;
        }

        ClickEvent.Action click_type = format.getClickType();

        if (click_type != null) {
            click_value = GChatPlugin.instance.replacePlaceholders(player, click_value);
        }

        HoverEvent<Component> hover_event;

        if (hover != null) {
            TextComponent hover_text = this.convertString(server_info, hover, parameters);

            if (hover_text != null) {
                hover_event = HoverEvent.showText(hover_text);
            } else {
                hover_event = null;
            }
        } else {
            hover_event = null;
        }

        ClickEvent click_event;

        if (click_type != null) {
            click_event = ClickEvent.clickEvent(click_type, click_value);
        } else {
            click_event = null;
        }

        if (click_event == null && hover_event == null) {
            return main_text;
        }

        TextComponent.Builder builder = main_text.toBuilder();

        TextComponent result = builder.applyDeep(componentBuilder -> {

            Component component = componentBuilder.build();

            if (hover_event != null && component.hoverEvent() == null) {
                componentBuilder.hoverEvent(hover_event);
            }

            if (click_event != null && component.clickEvent() == null) {
                componentBuilder.clickEvent(click_event);
            }
        }).build();

        return result;
    }

    /**
     * Convert the given text to a TextComponent
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    public TextComponent convertString(ServerInfo server_info, String source, @Nullable Map<String, String> parameters) {

        // Create the empty result. We'll append to this.
        TextComponent result = GChatPlugin.convertString(source, placeholder_entry -> {

            String key = placeholder_entry.getContent();

            if (parameters != null) {
                String value = parameters.get(key);

                if (value != null) {

                    // If the placeholder allows decoration, parse it again
                    if (placeholder_entry.allowsDecoration() && !value.equals(key)) {
                        return this.convertString(server_info, value, null);
                    }

                    // Return the value as-is
                    return Component.text(value);
                }
            }

            // Get the value from the server-specific placeholders
            TextComponent replacement = this.getServerPlaceholder(server_info, key);

            if (replacement != null) {
                return replacement;
            }

            return GChatPlugin.instance.lookupRegisteredPlaceholders(this.player, placeholder_entry);
        });

        return result;
    }

    /**
     * Format the given message
     *
     * @author   Jelle De Loecker
     * @since    3.1.0
     */
    public TextComponent format(String format_name, @Nullable Map<String, String> parameters) {
        return this.formatForServer(null, format_name, parameters);
    }

    /**
     * Deserialize text
     *
     * @author   Jelle De Loecker
     * @since    3.1.0
     */
    public TextComponent deserialize(String input) {

        if (input.contains("§")) {
            input = input.replace("§", "");
        }

        Component component;

        try {
            component = getMiniMessage().deserialize(input);
        } catch (Exception e) {
            try {
                component = Component.text(input);
            } catch (Exception e2) {
                return null;
            }
        }

        if (component instanceof TextComponent text_component) {
            return text_component;
        }

        return null;
    }

    /**
     * Convert a Player instance into a GChatPlayer
     *
     * @author   Jelle De Loecker
     * @since    3.0.2
     */
    public static GChatPlayer get(Player player) {

        GChatPlayer result = GChatPlayer.get(player.getUniqueId());

        if (result == null) {
            result = new GChatPlayer(player);
            CACHE.put(player.getUniqueId(), result);
        }

        return result;
    }

    /**
     * Get a player by its uuid
     *
     * @author   Jelle De Loecker
     * @since    3.0.2
     */
    public static GChatPlayer get(UUID player_uuid) {

        if (CACHE.containsKey(player_uuid)) {
            return CACHE.get(player_uuid);
        }

        return null;
    }

    /**
     * Try to get a player from a CommandSource
     *
     * @author   Jelle De Loecker
     * @since    3.0.2
     */
    public static GChatPlayer get(CommandSource source) {

        if (source instanceof Player player) {
            return get(player);
        }

        return null;
    }

    /**
     * Get a player by its display name
     *
     * @author   Jelle De Loecker
     * @since    3.1.0
     */
    public static GChatPlayer getByDisplayName(String display_name) {
        display_name = display_name.toLowerCase().trim();

        for (Player player : GChatPlugin.instance.getProxy().getAllPlayers()) {
            GChatPlayer gplayer = GChatPlayer.get(player);

            String nickname = gplayer.getNickname();

            if (nickname != null) {
                if (display_name.equals(nickname)) {
                    return gplayer;
                }

                nickname = nickname.toLowerCase().trim();

                if (display_name.equals(nickname)) {
                    return gplayer;
                }
            }

            String username = player.getUsername();

            if (username != null) {
                username = username.toLowerCase().trim();

                if (display_name.equals(username)) {
                    return gplayer;
                }
            }
        }

        return null;
    }

    /**
     * Get a minimessage parser
     *
     * @author   Jelle De Loecker
     * @since    3.1.0
     */
    public static MiniMessage getMiniMessage() {

        if (GChatPlayer.miniMessage == null) {
            GChatPlayer.miniMessage = MiniMessage.miniMessage();
        }

        return GChatPlayer.miniMessage;
    }

    /**
     * Get a legacy serializer
     *
     * @author   Jelle De Loecker
     * @since    3.1.0
     */
    public static LegacyComponentSerializer getComponentSerializer() {

        if (legacyLinkingSerializer == null) {
            legacyLinkingSerializer = LegacyComponentSerializer.builder()
                    .character('&')
                    .extractUrls(GChatPlugin.instance.getConfig().getLinkStyle())
                    .build();
        }

        return legacyLinkingSerializer;
    }

    /**
     * Remove the given player from the cache
     *
     * @author   Jelle De Loecker
     * @since    3.0.2
     */
    public static void remove(Player player) {
        CACHE.remove(player.getUniqueId());
    }

    /**
     * The interface used to callback for replacement
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    @FunctionalInterface
    public interface PlaceholderResolver {
        TextComponent replace(StringSplitter.Entry placeholder_entry);
    }

    static {
        COLOR_MAP.put("&0", "black");
        COLOR_MAP.put("&1", "dark_blue");
        COLOR_MAP.put("&2", "dark_green");
        COLOR_MAP.put("&3", "dark_aqua");
        COLOR_MAP.put("&4", "dark_red");
        COLOR_MAP.put("&5", "dark_purple");

        COLOR_MAP.put("&9", "blue");
        COLOR_MAP.put("&a", "green");

        COLOR_MAP.put("&d", "light_purple");
        COLOR_MAP.put("&e", "yellow");
        COLOR_MAP.put("&f", "white");
    }
}
