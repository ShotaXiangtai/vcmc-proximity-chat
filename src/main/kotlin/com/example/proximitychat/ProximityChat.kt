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
    var veryClose: Int = 5
        private set

    @Volatile
    var close: Int = 15
        private set

    @Volatile
    var maxRadius: Int = 25
        private set

    override fun onEnable() {
        saveDefaultConfig()
        loadConfigValues()

        server.pluginManager.registerEvents(ChatListener(this), this)

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            // /announce <message>
            commands.register(
                Commands.literal("announce")
                    .requires { src -> src.sender.hasPermission("proximitychat.announce") }
                    .then(
                        Commands.argument("message", StringArgumentType.greedyString())
                            .executes { ctx ->
                                val msg = StringArgumentType.getString(ctx, "message")
                                val announcement = Component.text("[ANNOUNCEMENT] ", NamedTextColor.RED)
                                    .append(Component.text(msg, NamedTextColor.WHITE))
                                ctx.source.sender.server.broadcast(announcement)
                                Command.SINGLE_SUCCESS
                            }
                    )
                    .executes { ctx ->
                        ctx.source.sender.sendMessage(
                            Component.text("使用方法: /announce <メッセージ>", NamedTextColor.RED)
                        )
                        Command.SINGLE_SUCCESS
                    }
                    .build(),
                "全プレイヤーにアナウンスを送信します"
            )

            // /setradius <radius>
            commands.register(
                Commands.literal("setradius")
                    .requires { src -> src.sender.hasPermission("proximitychat.setradius") }
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
                            }
                    )
                    .executes { ctx ->
                        ctx.source.sender.sendMessage(
                            Component.text("使用方法: /setradius <数値>", NamedTextColor.RED)
                        )
                        Command.SINGLE_SUCCESS
                    }
                    .build(),
                "チャット可視距離を変更します"
            )

            // /pcstatus
            commands.register(
                Commands.literal("pcstatus")
                    .requires { src -> src.sender.hasPermission("proximitychat.status") }
                    .executes { ctx ->
                        val sender = ctx.source.sender
                        sender.sendMessage(
                            Component.text("[ProximityChat] 現在の最大距離: ", NamedTextColor.YELLOW)
                                .append(Component.text("$maxRadius ブロック", NamedTextColor.GOLD))
                        )
                        sender.sendMessage(
                            Component.text("段階: ", NamedTextColor.GRAY)
                                .append(Component.text("とても近く <= ${veryClose}ブロック", NamedTextColor.GREEN))
                                .append(Component.text(" / ", NamedTextColor.GRAY))
                                .append(Component.text("近く <= ${close}ブロック", NamedTextColor.YELLOW))
                                .append(Component.text(" / ", NamedTextColor.GRAY))
                                .append(Component.text("周辺 <= ${maxRadius}ブロック", NamedTextColor.GRAY))
                        )
                        Command.SINGLE_SUCCESS
                    }
                    .build(),
                "近距離チャットの設定を確認します"
            )
        }

        logger.info("ProximityChat 有効化完了。最大可視距離: $maxRadius ブロック")
    }

    override fun onDisable() {
        logger.info("ProximityChat 無効化完了。")
    }

    fun loadConfigValues() {
        veryClose = config.getInt("very-close", 5)
        close = config.getInt("close", 15)
        maxRadius = config.getInt("max-radius", 25)
    }

    fun setMaxRadius(radius: Int) {
        maxRadius = radius
        server.globalRegionScheduler.run(this) { _ ->
            config.set("max-radius", radius)
            saveConfig()
        }
    }
}
