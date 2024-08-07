package rocks.blackblock.fluxchat.hooks;

import com.google.common.io.ByteArrayDataInput;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.ServerInfo;
import rocks.blackblock.fluxchat.FluxChatPlayer;
import rocks.blackblock.fluxchat.FluxChatPlugin;
import rocks.blackblock.nbt.Nbt;
import rocks.blackblock.nbt.api.NbtElement;
import rocks.blackblock.nbt.elements.collection.NbtCompound;
import rocks.blackblock.nbt.elements.collection.NbtList;
import rocks.blackblock.nbt.elements.primitive.NbtByte;


import java.util.UUID;

public class PluginMessageHook {

    public static final Nbt NBT = new Nbt();
    public final FluxChatPlugin plugin;

    public PluginMessageHook(FluxChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {

        ChannelIdentifier identifier = e.getIdentifier();

        if (identifier.equals(FluxChatPlugin.FLUXCHAT_CHANNEL) || identifier.equals(FluxChatPlugin.GCHAT_CHANNEL)) {
            this.handleFluxChatPacket(e);
            return;
        }

        if (identifier.equals(FluxChatPlugin.SERVER_MOVE_CHANNEL)) {
            this.handleServerMove(e);
            return;
        }
    }

    /**
     * Handle a `fluxchat` packet, sent by the Blackblock-core server plugin.
     *
     * @author   Jelle De Loecker
     * @since    3.1.0
     */
    private void handleFluxChatPacket(PluginMessageEvent e) {

        if (!(e.getSource() instanceof ServerConnection server)) {
            return;
        }

        NbtElement packet;
        byte[] byte_data = e.getData();

        try {
            packet = NBT.fromByteArray(byte_data);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        if (!(packet instanceof NbtCompound nbtCompound)) {
            return;
        }

        int version = nbtCompound.getInt("version").getValue();
        float mspt = nbtCompound.getFloat("mspt").getValue();
        float tps = nbtCompound.getFloat("tps").getValue();
        int load = nbtCompound.getInt("load").getValue();

        FluxChatPlugin.instance.registerTicks(server, mspt, tps, load);

        this.parseFluxchatPlayerUpdate(server.getServerInfo(), nbtCompound);
    }

    public static UUID intArrayToUuid(int[] array) {
        return new UUID((long)array[0] << 32 | (long)array[1] & 0xFFFFFFFFL, (long)array[2] << 32 | (long)array[3] & 0xFFFFFFFFL);
    }

    /**
     * Parse the player update packet
     *
     * @author   Jelle De Loecker
     * @since    3.2.0
     */
    private void parseFluxchatPlayerUpdate(ServerInfo server_info, NbtCompound packet) {

        if (!packet.contains("players")) {
            return;
        }

        NbtList<NbtCompound> players = packet.getList("players");

        for (NbtCompound player_nbt : players) {

            int[] uuid_ints = player_nbt.getIntArray("uuid").getValue();

            if (uuid_ints.length != 4) {
                continue;
            }

            UUID uuid = intArrayToUuid(uuid_ints);
            FluxChatPlayer player = FluxChatPlayer.get(uuid);

            if (player == null) {
                continue;
            }

            if (player_nbt.containsInt("ticks_since_movement")) {
                int ticks_since_movement = player_nbt.getInt("ticks_since_movement").getValue();
                player.setTicksSinceMovement(ticks_since_movement);
            }

            if (player_nbt.containsString("dimension")) {
                String dimension = player_nbt.getString("dimension").getValue();
                player.setDimension(dimension);
            }

            if (player_nbt.containsByte("alive")) {
                byte alive = player_nbt.getByte("alive").getValue();
                if (alive == 0) {
                    player.setIsAlive(false);
                } else {
                    player.setIsAlive(true);
                }
            }

            if (player_nbt.containsByte("invisible")) {
                byte value = player_nbt.getByte("invisible").getValue();
                if (value == 0) {
                    player.setIsInvisible(false);
                } else {
                    player.setIsInvisible(true);
                }
            }

            if (player_nbt.containsByte("creative")) {
                byte value = player_nbt.getByte("creative").getValue();
                if (value == 0) {
                    player.setIsCreative(false);
                } else {
                    player.setIsCreative(true);
                }
            }

            if (player_nbt.containsByte("spectator")) {
                byte value = player_nbt.getByte("spectator").getValue();
                if (value == 0) {
                    player.setIsSpectator(false);
                } else {
                    player.setIsSpectator(true);
                }
            }

            if (player_nbt.containsByte("stationary")) {
                NbtByte stationary_tag = player_nbt.getByte("stationary");

                if (stationary_tag.getValue() == 1) {
                    player.setIsStationary(true);
                } else {
                    player.setIsStationary(false);
                }
            }

            if (player_nbt.containsCompound("placeholders")) {

                NbtCompound placeholders = player_nbt.getCompound("placeholders");

                for (String key : placeholders.keySet()) {
                    String value = placeholders.getString(key).getValue();
                    player.setServerPlaceholder(server_info, key, value);
                }
            }
        }
    }

    private void handleServerMove(PluginMessageEvent e) {

        if (!(e.getSource() instanceof ServerConnection server)) {
            return;
        }

        byte[] data = e.getData();
        ByteArrayDataInput packet = e.dataAsDataStream();

        int version = packet.readInt();
        long uuid_most = packet.readLong();
        long uuid_least = packet.readLong();
        UUID uuid = new UUID(uuid_most, uuid_least);

        int array_length = packet.readInt();
        String target_server = packet.readUTF();
        //String username = packet.readUTF();

        System.out.println("Server move: " + target_server + " for " + uuid);

        //((ServerConnection) e.getSource()).getPlayer().
    }

    private boolean isBlackblockMessage(ChannelIdentifier identifier) {

        if (identifier.equals(FluxChatPlugin.FLUXCHAT_CHANNEL)) {
            return true;
        }

        if (identifier.equals(FluxChatPlugin.SERVER_MOVE_CHANNEL)) {
            return true;
        }

        if (identifier.equals(FluxChatPlugin.GCHAT_CHANNEL)) {
            return true;
        }

        return false;
    }
}
