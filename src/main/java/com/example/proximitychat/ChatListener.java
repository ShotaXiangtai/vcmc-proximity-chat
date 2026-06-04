package com.example.proximitychat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final ProximityChat plugin;

    public ChatListener(ProximityChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);

        Player sender = event.getPlayer();
        String playerName = sender.getName();
        // MiniMessage タグのインジェクションを防ぐためプレーンテキストとして取得
        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

        int veryClose = plugin.getVeryClose();
        int close = plugin.getClose();
        int maxRadius = plugin.getMaxRadius();

        // Folia: senderのロケーションはsenderのリージョンスレッドで取得する
        sender.getScheduler().run(plugin, senderTask -> {
            Location senderLoc = sender.getLocation();
            World senderWorld = sender.getWorld();

            for (Player receiver : plugin.getServer().getOnlinePlayers()) {
                // Folia: receiverのロケーションはreceiverのリージョンスレッドで取得する
                receiver.getScheduler().run(plugin, receiverTask -> {
                    if (!receiver.getWorld().equals(senderWorld)) return;

                    double distance = senderLoc.distance(receiver.getLocation());

                    Component formatted;
                    if (distance <= veryClose) {
                        formatted = Component.text("[とても近く] ", NamedTextColor.GREEN)
                                .append(Component.text(playerName + ": ", NamedTextColor.WHITE))
                                .append(Component.text(rawMessage, NamedTextColor.WHITE));
                    } else if (distance <= close) {
                        formatted = Component.text("[近く] ", NamedTextColor.YELLOW)
                                .append(Component.text(playerName + ": ", NamedTextColor.WHITE))
                                .append(Component.text(rawMessage, NamedTextColor.WHITE));
                    } else if (distance <= maxRadius) {
                        formatted = Component.text("[周辺] ", NamedTextColor.GRAY)
                                .append(Component.text(playerName + ": ", NamedTextColor.WHITE))
                                .append(Component.text(rawMessage, NamedTextColor.WHITE));
                    } else {
                        return;
                    }

                    receiver.sendMessage(formatted);
                }, null);
            }
        }, null);
    }
}
