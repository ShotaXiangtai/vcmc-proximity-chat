package com.example.proximitychat

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class ChatListener(private val plugin: ProximityChat) : Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        event.isCancelled = true

        val sender = event.player
        val playerName = sender.name
        val rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message())
        plugin.logger.info("[ProximityChat] $playerName: $rawMessage")

        val veryClose = plugin.veryClose
        val close = plugin.close
        val maxRadius = plugin.maxRadius

        // Folia: sender のリージョンスレッドで位置とワールドを取得
        sender.scheduler.run(plugin, { _ ->
            val senderLoc = sender.location
            val senderWorld = sender.world

            for (receiver in plugin.server.onlinePlayers) {
                // Folia: receiver のリージョンスレッドで距離計算とメッセージ送信
                receiver.scheduler.run(plugin, { _ ->
                    if (receiver.world != senderWorld) return@run

                    val distance = senderLoc.distance(receiver.location)
                    if (distance > maxRadius) return@run

                    val prefix = when {
                        distance <= veryClose -> Component.text("[とても近く] ", NamedTextColor.GREEN)
                        distance <= close -> Component.text("[近く] ", NamedTextColor.YELLOW)
                        else -> Component.text("[周辺] ", NamedTextColor.GRAY)
                    }

                    val formatted = prefix
                        .append(Component.text("$playerName: ", NamedTextColor.WHITE))
                        .append(Component.text(rawMessage, NamedTextColor.WHITE))

                    receiver.sendMessage(formatted)
                }, null)
            }
        }, null)
    }
}
