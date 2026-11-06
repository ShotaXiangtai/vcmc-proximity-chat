package com.example.proximitychat

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.plugin.java.JavaPlugin

@Suppress("UnstableApiUsage")
class ProximityChat : JavaPlugin() {

    @Volatile
    var range = ChatRange(ChatRange.DEFAULT_VERY_CLOSE, ChatRange.DEFAULT_CLOSE, ChatRange.DEFAULT_MAX_RADIUS)
        private set

    private val configLock = Any()

    override fun onEnable() {
        saveDefaultConfig()
        loadRange()

        server.pluginManager.registerEvents(ChatListener(this), this)

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            // /announce <message>
            commands.register(
                Commands.literal("announce")
                    .requires { src -> src.sender.hasPermission(PERM_ANNOUNCE) }
                    .then(
                        Commands.argument("message", StringArgumentType.greedyString())
                            .executes { ctx ->
                                val msg = StringArgumentType.getString(ctx, "message")
                                val announcement = Component.text("[ANNOUNCEMENT] ", NamedTextColor.RED)
                                    .append(Component.text(msg, NamedTextColor.WHITE))
                                ctx.source.sender.server.broadcast(announcement)
                                Command.SINGLE_SUCCESS
                            },
                    )
                    .executes { ctx ->
                        ctx.source.sender.sendMessage(
                            Component.text("使用方法: /announce <メッセージ>", NamedTextColor.RED),
                        )
                        Command.SINGLE_SUCCESS
                    }
                    .build(),
                "全プレイヤーにアナウンスを送信します",
            )

            // /setradius <radius>
            commands.register(
                Commands.literal("setradius")
                    .requires { src -> src.sender.hasPermission(PERM_SET_RADIUS) }
                    .then(
                        Commands.argument("radius", IntegerArgumentType.integer(1))
                            .executes { ctx ->
                                val radius = IntegerArgumentType.getInteger(ctx, "radius")
                                setMaxRadius(radius)
                                val msg = Component.text("[System] チャット可視距離を ", NamedTextColor.YELLOW)
                                    .append(Component.text("$radius ", NamedTextColor.GOLD))
                                    .append(Component.text("ブロックに変更しました。", NamedTextColor.YELLOW))
                                ctx.source.sender.server.broadcast(msg)
                                Command.SINGLE_SUCCESS
                            },
                    )
                    .executes { ctx ->
                        ctx.source.sender.sendMessage(
                            Component.text("使用方法: /setradius <数値>", NamedTextColor.RED),
                        )
                        Command.SINGLE_SUCCESS
                    }
                    .build(),
                "チャット可視距離を変更します",
            )

            // /pcstatus
            commands.register(
                Commands.literal("pcstatus")
                    .requires { src -> src.sender.hasPermission(PERM_STATUS) }
                    .executes { ctx ->
                        val sender = ctx.source.sender
                        val current = range
                        sender.sendMessage(
                            Component.text("[ProximityChat] 現在の最大距離: ", NamedTextColor.YELLOW)
                                .append(Component.text("${current.maxRadius} ブロック", NamedTextColor.GOLD)),
                        )
                        sender.sendMessage(
                            Component.text("段階: ", NamedTextColor.GRAY)
                                .append(Component.text("とても近く <= ${current.veryClose}ブロック", NamedTextColor.GREEN))
                                .append(Component.text(" / ", NamedTextColor.GRAY))
                                .append(Component.text("近く <= ${current.close}ブロック", NamedTextColor.YELLOW))
                                .append(Component.text(" / ", NamedTextColor.GRAY))
                                .append(Component.text("周辺 <= ${current.maxRadius}ブロック", NamedTextColor.GRAY)),
                        )
                        Command.SINGLE_SUCCESS
                    }
                    .build(),
                "近距離チャットの設定を確認します",
            )

            // /pcreload
            commands.register(
                Commands.literal("pcreload")
                    .requires { src -> src.sender.hasPermission(PERM_RELOAD) }
                    .executes { ctx ->
                        loadRange()
                        ctx.source.sender.sendMessage(
                            Component.text("[ProximityChat] 設定を再読み込みしました。", NamedTextColor.GREEN),
                        )
                        Command.SINGLE_SUCCESS
                    }
                    .build(),
                "近距離チャットの設定を再読み込みします",
            )
        }

        logger.info("ProximityChat 有効化完了。最大可視距離: ${range.maxRadius} ブロック")
    }

    override fun onDisable() {
        logger.info("ProximityChat 無効化完了。")
    }

    private fun loadRange() {
        synchronized(configLock) {
            reloadConfig()
            range = ChatRange.sanitize(
                config.getInt("very-close", ChatRange.DEFAULT_VERY_CLOSE),
                config.getInt("close", ChatRange.DEFAULT_CLOSE),
                config.getInt("max-radius", ChatRange.DEFAULT_MAX_RADIUS),
                logger::warning,
            )
        }
    }

    private fun setMaxRadius(radius: Int) {
        range = range.copy(maxRadius = radius)
        // ファイルの書き込みでリージョンを止めない。
        server.asyncScheduler.runNow(this) { _ ->
            synchronized(configLock) {
                config.set("max-radius", radius)
                saveConfig()
            }
        }
    }

    private companion object {
        const val PERM_ANNOUNCE = "proximitychat.announce"
        const val PERM_SET_RADIUS = "proximitychat.setradius"
        const val PERM_STATUS = "proximitychat.status"
        const val PERM_RELOAD = "proximitychat.reload"
    }
}
