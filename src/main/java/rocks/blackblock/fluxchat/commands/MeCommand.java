package rocks.blackblock.fluxchat.commands;

import com.velocitypowered.api.command.SimpleCommand;
import rocks.blackblock.fluxchat.FluxChatPlayer;
import rocks.blackblock.fluxchat.placeholder.PlaceholderParameters;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.Arrays;

/**
 * The custom `me` command
 *
 * @author   Jelle De Loecker
 * @since    3.1.0
 */
public class MeCommand implements SimpleCommand {

    private final String format;

    public MeCommand(String format) {
        this.format = format;
    }

    @Override
    public void execute(Invocation invocation) {

        FluxChatPlayer player = FluxChatPlayer.get(invocation.source());

        if (player == null) {
            System.out.println("Unable to do MeCommand of format " + this.format + ", player is null");
            return;
        }

        // Get the arguments after the command alias
        String[] args = invocation.arguments();

        String message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));

        PlaceholderParameters parameters = new PlaceholderParameters();
        parameters.set("message", Component.text(message));
        TextComponent me_message = player.format(this.format, parameters);

        if (me_message == null) {
            return;
        }

        player.broadcast(me_message, true);
    }
}
