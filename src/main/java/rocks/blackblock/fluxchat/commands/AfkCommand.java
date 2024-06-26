package rocks.blackblock.fluxchat.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import rocks.blackblock.fluxchat.FluxChatPlayer;

/**
 * Allow players to set their AFK status
 *
 * @author   Jelle De Loecker
 * @since    3.2.0
 */
public class AfkCommand implements SimpleCommand {

    /**
     * Execute the command
     *
     * @since    3.2.0
     */
    @Override
    public void execute(Invocation invocation) {

        CommandSource source = invocation.source();

        if (!(source instanceof Player player)) {
            return;
        }

        FluxChatPlayer fluxchat_player = FluxChatPlayer.get(player);

        if (fluxchat_player == null) {
            return;
        }

        // The AFK command always sets the player to AFK
        fluxchat_player.setAfk(true);
    }
}
