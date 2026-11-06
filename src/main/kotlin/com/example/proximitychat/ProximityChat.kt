package com.example.proximitychat

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin

@Suppress("UnstableApiUsage")
class ProximityChat : JavaPlugin() {

    @Volatile
    var range = ChatRange(ChatRange.DEFAULT_VERY_CLOSE, ChatRange.DEFAULT_CLOSE, ChatRange.DEFAULT_MAX_RADIUS)
        private set

    lateinit var messages: Messages
        private set

    private val configLock = Any()

    override fun onEnable() {
        saveDefaultConfig()
        messages = Messages(this)
        messages.reload()
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
                                messages.broadcast("announce", "message" to StringArgumentType.getString(ctx, "message"))
                                Command.SINGLE_SUCCESS
                            },
                    )
                    .executes { ctx ->
                        messages.send(ctx.source.sender, "announce-usage")
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
                                messages.broadcast("radius-changed", "radius" to radius.toString())
                                Command.SINGLE_SUCCESS
                            },
                    )
                    .executes { ctx ->
                        messages.send(ctx.source.sender, "setradius-usage")
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
                        messages.send(sender, "status-radius", "radius" to current.maxRadius.toString())
                        messages.send(
                            sender,
                            "status-tiers",
                            "very_close" to current.veryClose.toString(),
                            "close" to current.close.toString(),
                            "radius" to current.maxRadius.toString(),
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
                        messages.reload()
                        messages.send(ctx.source.sender, "reloaded")
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
