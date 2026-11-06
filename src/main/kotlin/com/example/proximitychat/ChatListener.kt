package com.example.proximitychat

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class ChatListener(private val plugin: ProximityChat) : Listener {

    /**
     * チャットを止めて距離に応じて配り直す。
     * ルール未同意やミュートなど、他のプラグインが止めたチャットを配らないよう、判定が出そろう HIGHEST で動かす。
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        event.isCancelled = true

        val sender = event.player
        val playerName = sender.name
        val rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message())
        plugin.logger.info("$playerName: $rawMessage")

        val range = plugin.range

        // 位置はそのプレイヤーのリージョンでしか正しく読めないので、送信者 → 各受信者の順に移って判定する。
        sender.scheduler.run(plugin, { _ ->
            val origin = sender.location
            for (receiver in plugin.server.onlinePlayers) {
                receiver.scheduler.run(plugin, { _ ->
                    deliver(receiver, origin, range, playerName, rawMessage)
                }, null)
            }
        }, null)
    }

    private fun deliver(receiver: Player, origin: Location, range: ChatRange, playerName: String, message: String) {
        val location = receiver.location
        if (location.world != origin.world) return
        val tier = range.tierFor(origin.distanceSquared(location)) ?: return
        val key = when (tier) {
            ChatTier.VERY_CLOSE -> "chat-very-close"
            ChatTier.CLOSE -> "chat-close"
            ChatTier.AROUND -> "chat-around"
        }
        plugin.messages.send(receiver, key, "player" to playerName, "message" to message)
    }
}
