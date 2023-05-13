package me.lucko.gchat.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import me.lucko.gchat.GChatPlayer;

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

        GChatPlayer gchat_player = GChatPlayer.get(player);

        if (gchat_player == null) {
            return;
        }

        // The AFK command always sets the player to AFK
        gchat_player.setAfk(true);
    }
}
