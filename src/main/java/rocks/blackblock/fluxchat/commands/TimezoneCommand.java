package rocks.blackblock.fluxchat.commands;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import rocks.blackblock.fluxchat.FluxChatPlayer;
import rocks.blackblock.fluxchat.FluxChatPlugin;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class TimezoneCommand implements SimpleCommand {

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

        String timezone = args[0];

        List<String> timezones = Arrays.asList(TimeZone.getAvailableIDs());

        if (!timezones.contains(timezone)) {
            source.sendMessage(Component.text("Failed to find that timezone!").color(NamedTextColor.RED));
            return;
        }

        FluxChatPlayer fluxChatPlayer = FluxChatPlayer.get(player);
        fluxChatPlayer.setTimezone(timezone);

        if (FluxChatPlugin.shouldPushEvents()) {
            JsonObject object = FluxChatPlugin.createObject("timezone", player);
            object.addProperty("timezone", timezone);
            FluxChatPlugin.pushEvent(object);
        }

        source.sendMessage(Component.text("Your timezone has been set to " + timezone).color(NamedTextColor.AQUA));
    }

    public List<String> suggest(final SimpleCommand.Invocation invocation) {
        List<String> args = Arrays.asList(invocation.arguments());
        List<String> timezones = Arrays.asList(TimeZone.getAvailableIDs());

        int size = args.size();

        if (size > 1) {
            return ImmutableList.of();
        }

        if (size > 0) {
            String query = args.get(0);
            List<ExtractedResult> filtered = FuzzySearch.extractSorted(query, timezones, 80);

            timezones = new ArrayList<>();

            if (filtered.size() > 0) {
                for (ExtractedResult entry : filtered) {
                    timezones.add(entry.getString());
                }
            }
        }

        return timezones;
    }
}
