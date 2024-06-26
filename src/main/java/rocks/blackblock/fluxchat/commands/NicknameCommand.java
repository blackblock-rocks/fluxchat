package rocks.blackblock.fluxchat.commands;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import rocks.blackblock.fluxchat.FluxChatPlayer;
import rocks.blackblock.fluxchat.FluxChatPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.*;

public class NicknameCommand implements SimpleCommand {

    public static final Map<String, String> COLORS = new HashMap<>();

    static {
        //COLORS.put("&0", "BLACK");
        //COLORS.put("&1", "DARK_BLUE");
        COLORS.put("&2", "DARK_GREEN");
        COLORS.put("&3", "DARK_AQUA");
        COLORS.put("&4", "DARK_RED");
        COLORS.put("&5", "DARK_PURPLE");

        COLORS.put("&9", "BLUE");
        COLORS.put("&a", "GREEN");

        COLORS.put("&d", "LIGHT_PURPLE");
        COLORS.put("&e", "YELLOW");
        COLORS.put("&f", "WHITE");
    }

    @Override
    public void execute(Invocation invocation) {

        CommandSource source = invocation.source();

        if (!(source instanceof Player player)) {
            return;
        }

        // Get the arguments after the command alias
        String[] args = invocation.arguments();

        if (args.length == 0) {
            return;
        }

        String original_nickname = args[0];
        String nickname = original_nickname;
        String color_name = null;

        FluxChatPlayer fluxChatPlayer = FluxChatPlayer.get(player);
        fluxChatPlayer.setNickname(nickname);

        if (args.length == 2) {
            color_name = args[1];
            String code = null;

            if (invocation.source().hasPermission("FluxChat.all_colors")) {
                if (color_name.equals("GOLD")) {
                    code = "&6";
                }
            }

            if (code == null) {
                code = getKey(color_name, COLORS);
            }

            if (code == null) {
                color_name = null;
                fluxChatPlayer.setNicknameColor(null);
            } else {
                fluxChatPlayer.setNicknameColor(code);
                nickname = code + nickname;
            }
        }

        if (FluxChatPlugin.shouldPushEvents()) {
            JsonObject object = FluxChatPlugin.createObject("nickname", player);
            object.addProperty("nickname", nickname);
            object.addProperty("color", color_name);
            FluxChatPlugin.pushEvent(object);
        }

        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
        Component success = Component.text("Your nickname has been set to ").color(NamedTextColor.AQUA);
        success = success.append(legacy.deserialize(nickname));

        source.sendMessage(success);
    }

    public static String getKey(String value, Map<String, String> map) {

        for (String key : map.keySet()) {
            String entry_value = map.get(key);

            if (value.equals(entry_value)) {
                return key;
            }
        }

        return null;
    }

    public List<String> suggest(final SimpleCommand.Invocation invocation) {

        List<String> args = Arrays.asList(invocation.arguments());

        if (args.size() > 1) {

            List<String> color_list = COLORS.values().stream().toList();

            if (invocation.source().hasPermission("FluxChat.all_colors")) {
                color_list = new ArrayList<>(color_list);

                color_list.add("GOLD");
            }

            return color_list;
        }

        return ImmutableList.of();
    }
}
