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

package me.lucko.gchat;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import me.lucko.gchat.api.ChatFormat;
import me.lucko.gchat.api.GChatApi;
import me.lucko.gchat.api.Placeholder;
import me.lucko.gchat.commands.*;
import me.lucko.gchat.config.GChatConfig;
import me.lucko.gchat.hooks.LuckPermsHook;
import me.lucko.gchat.hooks.NeutronN3FSHook;
import me.lucko.gchat.hooks.PluginMessageHook;
import me.lucko.gchat.hooks.TimerHook;
import me.lucko.gchat.placeholder.SplittedStringConverter;
import me.lucko.gchat.placeholder.SplittedStringList;
import me.lucko.gchat.placeholder.StandardPlaceholders;
import me.lucko.gchat.placeholder.StringSplitter;
import me.lucko.gchat.tab.GChatTabList;
import net.kyori.adventure.serializer.configurate3.ConfigurateComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin(
        id = "@ID@",
        name = "@NAME@",
        description = "@DESCRIPTION@",
        authors = {"Luck", "md678685"},
        version = "@VERSION@", // filled in during build
        dependencies = {
                @Dependency(id = "luckperms", optional = true),
                @Dependency(id = "neutron-n3fs", optional = true)
        }
)
public class GChatPlugin implements GChatApi {
    public static final LegacyComponentSerializer LEGACY_LINKING_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .extractUrls()
            .build();
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^\\{\\}]+)\\}");

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private final Set<Placeholder> placeholders = ConcurrentHashMap.newKeySet();
    private GChatTabList tab_list = null;
    private final DateFormat date_format;
    private final DateFormat time_format;
    private final DateFormat tz_time_format;
    private final Map<ServerInfo, Integer> load_map;
    private final Map<ServerInfo, Float> mspt_map;
    private final Map<ServerInfo, Float> tps_map;
    private final Map<ServerInfo, Integer> counter_map;

    public static final MinecraftChannelIdentifier GCHAT_CHANNEL = MinecraftChannelIdentifier.create("blackblock", "gchat");
    public static final MinecraftChannelIdentifier SERVER_MOVE_CHANNEL = MinecraftChannelIdentifier.create("blackblock", "servermove");
    public static GChatPlugin instance;

    private GChatConfig config;

    @Inject
    public GChatPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        this.date_format = new SimpleDateFormat("yyyy-MM-dd");
        this.time_format = new SimpleDateFormat("HH:mm");
        this.tz_time_format = new SimpleDateFormat("HH:mm");

        this.mspt_map = new HashMap<>();
        this.tps_map = new HashMap<>();
        this.load_map = new HashMap<>();
        this.counter_map = new HashMap<>();

        GChatPlugin.instance = this;
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        logger.info("Enabling gChat v" + getDescription().getVersion().orElse("Unknown"));

        // load configuration
        try {
            this.config = loadConfig();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config", e);
        }

        // init placeholder hooks
        placeholders.add(new StandardPlaceholders());

        // hook with luckperms
        if (proxy.getPluginManager().getPlugin("luckperms").isPresent()) {
            placeholders.add(new LuckPermsHook());
        }

        if (proxy.getPluginManager().getPlugin("neutron-n3fs").isPresent()) {
            proxy.getEventManager().register(this, new NeutronN3FSHook());
        }

        // register chat listener
        proxy.getEventManager().register(this, new GChatListener(this));

        CommandManager commandManager = proxy.getCommandManager();

        // register command
        commandManager.register("gchat", new GChatCommand(this), "globalchat");

        // register the timezone command
        commandManager.register("timezone", new TimezoneCommand());

        // register the pronouns command
        commandManager.register("pronouns", new PronounsCommand());

        // register the nickname command
        commandManager.register("nickname", new NicknameCommand());

        commandManager.register("msg", new WhisperCommand(), "whisper", "w", "tell");

        commandManager.register("me", new MeCommand("me"));
        commandManager.register("say", new MeCommand("say"));

        proxy.getEventManager().register(this, new PluginMessageHook(this));
        proxy.getChannelRegistrar().register(GCHAT_CHANNEL);
        proxy.getChannelRegistrar().register(SERVER_MOVE_CHANNEL);

        this.tab_list = new GChatTabList(this, proxy);
        proxy.getEventManager().register(this, this.tab_list);

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerHook(), 1000, 1000);

        // init api singleton
        GChat.setApi(this);
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        // null the api singleton
        GChat.setApi(null);
    }

    @Override
    public boolean registerPlaceholder(Placeholder placeholder) {
        return placeholders.add(placeholder);
    }

    @Override
    public boolean unregisterPlaceholder(Placeholder placeholder) {
        return placeholders.remove(placeholder);
    }

    @Override
    public ImmutableSet<Placeholder> getPlaceholders() {
        return ImmutableSet.copyOf(placeholders);
    }

    @Override
    public List<ChatFormat> getFormats() {
        return config.getFormats();
    }

    public String replaceGenericPlaceholders(String text) {

        if (text == null || text.isEmpty() || placeholders.isEmpty()) {
            return text;
        }

        Date now = new Date();

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            String definition = matcher.group(1);
            String name = definition.toLowerCase(Locale.ROOT);
            String replacement = null;

            switch (name) {
                case "playercount":
                    replacement = String.valueOf(proxy.getPlayerCount());
                    break;

                case "server_date":
                    replacement = this.date_format.format(now);
                    break;

                case "server_time":
                    replacement = this.time_format.format(now);
                    break;

                case "local_time_nz":
                    tz_time_format.setTimeZone(TimeZone.getTimeZone("NZ"));
                    replacement = tz_time_format.format(now);
                    break;

                case "local_time_cet":
                    tz_time_format.setTimeZone(TimeZone.getTimeZone("CET"));
                    replacement = tz_time_format.format(now);
                    break;

                case "local_time_est":
                case "local_time_ny":
                    tz_time_format.setTimeZone(TimeZone.getTimeZone("America/New_York"));
                    replacement = tz_time_format.format(now);
                    break;

                case "local_time_pst":
                case "local_time_la":
                    tz_time_format.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
                    replacement = tz_time_format.format(now);
                    break;
            }

            if (replacement != null) {
                text = text.replace("{" + definition + "}", replacement);
            }
        }

        return text;
    }

    @Override
    public String replacePlaceholders(Player player, String text) {
        if (text == null || text.isEmpty() || placeholders.isEmpty()) {
            return text;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            String definition = matcher.group(1);
            String replacement = null;

            for (Placeholder placeholder : placeholders) {
                replacement = placeholder.lookupStringReplacement(player, definition);
                if (replacement != null) {
                    break;
                }
            }

            if (replacement != null) {
                text = text.replace("{" + definition + "}", replacement);
            }
        }

        return text;
    }

    @Override
    public Optional<ChatFormat> getFormat(Player player) {
        return this.getFormat(player, "chat");
    }

    public Optional<ChatFormat> getFormat(Player player, String type) {
        return config.getFormats().stream()
                .filter(f -> {
                    if (!f.canUse(player)) {
                        return false;
                    }

                    return f.getType().equals(type);
                })
                .findFirst();
    }

    @Subscribe
    public boolean onReload(ProxyReloadEvent event) {
        return reloadConfig();
    }

    public boolean reloadConfig() {
        try {
            config = loadConfig();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private GChatConfig loadConfig() throws Exception {
        TypeSerializerCollection serializerCollection = TypeSerializerCollection.create();
        ConfigurateComponentSerializer.configurate().addSerializersTo(serializerCollection);

        ConfigurationOptions options = ConfigurationOptions.defaults()
                .withSerializers(serializerCollection);

        ConfigurationNode configNode = YAMLConfigurationLoader.builder()
                .setDefaultOptions(options)
                .setFile(getBundledFile("config.yml"))
                .build()
                .load();
        return new GChatConfig(configNode);
    }

    private File getBundledFile(String name) {
        File file = new File(dataDirectory.toFile(), name);

        if (!file.exists()) {
            dataDirectory.toFile().mkdir();
            try (InputStream in = GChatPlugin.class.getResourceAsStream("/" + name)) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    PluginDescription getDescription() {
        return proxy.getPluginManager().getPlugin("gchat-velocity").map(PluginContainer::getDescription).orElse(null);
    }

    public Logger getLogger() {
        return this.logger;
    }

    public GChatConfig getConfig() {
        return this.config;
    }

    /**
     * Register a server's MSPT and TPS
     */
    public void registerTicks(ServerConnection server_connection, float mspt, float tps, int load) {

        RegisteredServer server = server_connection.getServer();
        ServerInfo server_info = server.getServerInfo();

        Integer tick_counter = this.counter_map.getOrDefault(server_info, 0);

        if (tick_counter == 0) {
            System.out.println("Server '" + server_info.getName() + "' TPS: " + ((int) tps) + " MSPT: " + ((int) mspt) + " Load: " + (int) load);
        }

        tick_counter++;

        // This will make the server TPS only print every +/- 40 seconds
        // (Depends on how often the server reports its tps)
        if (tick_counter > 20) {
            tick_counter = 0;
        }

        this.counter_map.put(server_info, tick_counter);

        Float existing_mspt = this.mspt_map.get(server_info);

        if (existing_mspt != null) {
            mspt = (existing_mspt + mspt) / 2;
        }

        Float existing_tps = this.tps_map.get(server_info);

        if (existing_tps != null) {
            tps = (existing_tps + tps) / 2;
        }

        Integer existing_load = this.load_map.get(server_info);

        if (existing_load != null) {
            load = (existing_load + load) / 2;
        }

        //System.out.println(" -- Normalized " + tps + " - " + mspt + " - " + load);

        this.mspt_map.put(server_info, mspt);
        this.tps_map.put(server_info, tps);
        this.load_map.put(server_info, load);
    }

    /**
     * Get a server's load
     */
    public int getServerLoad(ServerConnection server_connection) {
        RegisteredServer server = server_connection.getServer();

        if (server != null && this.load_map.containsKey(server.getServerInfo())) {
            return this.load_map.get(server.getServerInfo());
        }

        return -1;
    }

    /**
     * Get a server's MSPT
     */
    public float getServerMSPT(ServerConnection server_connection) {

        RegisteredServer server = server_connection.getServer();

        if (server != null && this.mspt_map.containsKey(server.getServerInfo())) {
            return this.mspt_map.get(server.getServerInfo());
        }

        return -1f;
    }

    /**
     * Get a server's TPS
     */
    public float getServerTPS(ServerConnection server_connection) {

        RegisteredServer server = server_connection.getServer();

        if (server != null && this.tps_map.containsKey(server.getServerInfo())) {
            return this.tps_map.get(server.getServerInfo());
        }

        return -1f;
    }

    /**
     * Lookup in the registered placeholders
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    @Nullable
    public TextComponent lookupRegisteredPlaceholders(Player player, StringSplitter.Entry placeholder_entry) {

        if (placeholder_entry == null) {
            return null;
        }

        String key = placeholder_entry.getContent();

        if (key == null) {
            return null;
        }

        TextComponent result;

        for (Placeholder placeholder : this.placeholders) {
            result = placeholder.getTextComponentReplacement(player, placeholder_entry);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * Convert a string into a TextComponent with help from the given resolver.
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    public static TextComponent convertString(String string, GChatPlayer.PlaceholderResolver resolver) {

        SplittedStringList entries = StringSplitter.parse(string);
        SplittedStringConverter converter = new SplittedStringConverter(entries, resolver);

        return converter.toTextComponent();
    }

    /**
     * Should events be pushed to a remote serveR?
     */
    public static boolean shouldPushEvents() {

        if (instance == null) {
            return false;
        }

        return instance.getConfig().shouldPushEvents();
    }

    /**
     * Get the remote endpoint for events
     */
    public static URI getPushEndpoint() {

        if (instance == null) {
            return null;
        }

        return instance.getConfig().getPushEndpoint();
    }

    /**
     * Create an object
     */
    public static JsonObject createObject(String type) {
        JsonObject result = new JsonObject();
        result.addProperty("type", type);
        return result;
    }

    /**
     * Create an object with player data
     */
    public static JsonObject createObject(String type, Player player) {
        JsonObject result = createObject(type);

        JsonObject player_obj = new JsonObject();

        player_obj.addProperty("uuid", player.getUniqueId().toString());
        player_obj.addProperty("username", player.getUsername());
        player_obj.addProperty("ping", player.getPing());

        ServerConnection connection = player.getCurrentServer().orElse(null);

        if (connection != null) {
            player_obj.addProperty("server", connection.getServer().getServerInfo().getName());
        }

        result.add("player", player_obj);

        return result;
    }

    /**
     * Push the given object to the endpoint
     */
    public static void pushEvent(JsonObject data) {

        if (data == null || instance == null || !shouldPushEvents()) {
            return;
        }

        try {
            _pushEvent(data);
        } catch (Exception e) {
            // Ignore
        }
    }

    private static void _pushEvent(JsonObject data) {

        URI uri = getPushEndpoint();

        if (uri == null) {
            return;
        }

        String body = (new Gson()).toJson(data);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }
}
