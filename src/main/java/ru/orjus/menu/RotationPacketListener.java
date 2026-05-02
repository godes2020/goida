package ru.orjus.menu;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;

public final class RotationPacketListener {

    private final MenuPlugin plugin;
    private final MenuManager manager;

    public RotationPacketListener(MenuPlugin plugin, MenuManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void register() {
        ProtocolManager pm = ProtocolLibrary.getProtocolManager();

        pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (player != null && manager.hasSession(player)) {
                    event.setCancelled(true);
                }
            }
        });

        pm.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                PacketType.Play.Client.BLOCK_DIG,
                PacketType.Play.Client.BLOCK_PLACE,
                PacketType.Play.Client.USE_ITEM) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (player != null && manager.hasSession(player)) {
                    event.setCancelled(true);
                }
            }
        });
    }
}
