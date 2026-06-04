package com.example.proximitychat;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public class ProximityChat extends JavaPlugin {

    private volatile int veryClose;
    private volatile int close;
    private volatile int maxRadius;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register(
                Commands.literal("announce")
                    .requires(src -> src.getSender().hasPermission("proximitychat.announce"))
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String msg = StringArgumentType.getString(ctx, "message");
                            Component announcement = Component.text("[ANNOUNCEMENT] ", NamedTextColor.RED)
                                    .append(Component.text(msg, NamedTextColor.WHITE));
                            ctx.getSource().getSender().getServer().broadcast(announcement);
                            return Command.SINGLE_SUCCESS;
                        }))
                    .executes(ctx -> {
                        ctx.getSource().getSender().sendMessage(
                            Component.text("使い方: /announce <メッセージ>", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    })
                    .build(),
                "全体アナウンスを送信します（OP専用）"
            );

            commands.register(
                Commands.literal("setradius")
                    .requires(src -> src.getSender().hasPermission("proximitychat.setradius"))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            int radius = IntegerArgumentType.getInteger(ctx, "radius");
                            setMaxRadius(radius);
                            Component msg = Component.text("[System] チャットの最大距離が ", NamedTextColor.YELLOW)
                                    .append(Component.text(radius + " ", NamedTextColor.GOLD))
                                    .append(Component.text("ブロックに変更されました。", NamedTextColor.YELLOW));
                            ctx.getSource().getSender().getServer().broadcast(msg);
                            return Command.SINGLE_SUCCESS;
                        }))
                    .executes(ctx -> {
                        ctx.getSource().getSender().sendMessage(
                            Component.text("使い方: /setradius <数値>", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    })
                    .build(),
                "チャットの最大可視距離を変更します（OP専用）"
            );

            commands.register(
                Commands.literal("pcstatus")
                    .requires(src -> src.getSender().hasPermission("proximitychat.status"))
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        sender.sendMessage(Component.text("[ProximityChat] 最大距離: ", NamedTextColor.YELLOW)
                                .append(Component.text(maxRadius + " ブロック", NamedTextColor.GOLD)));
                        sender.sendMessage(
                            Component.text("段階: ", NamedTextColor.GRAY)
                                .append(Component.text("とても近く <=" + veryClose + "ブロック", NamedTextColor.GREEN))
                                .append(Component.text(" / ", NamedTextColor.GRAY))
                                .append(Component.text("近く <=" + close + "ブロック", NamedTextColor.YELLOW))
                                .append(Component.text(" / ", NamedTextColor.GRAY))
                                .append(Component.text("周辺 <=" + maxRadius + "ブロック", NamedTextColor.GRAY))
                        );
                        return Command.SINGLE_SUCCESS;
                    })
                    .build(),
                "現在の近距離チャット設定を確認します"
            );
        });

        getLogger().info("ProximityChat が有効になりました。最大距離: " + maxRadius + " ブロック");
    }

    @Override
    public void onDisable() {
        getLogger().info("ProximityChat が無効になりました。");
    }

    public void loadConfigValues() {
        veryClose = getConfig().getInt("very-close", 5);
        close = getConfig().getInt("close", 15);
        maxRadius = getConfig().getInt("max-radius", 25);
    }

    public int getVeryClose() {
        return veryClose;
    }

    public int getClose() {
        return close;
    }

    public int getMaxRadius() {
        return maxRadius;
    }

    public void setMaxRadius(int radius) {
        maxRadius = radius;
        getServer().getGlobalRegionScheduler().run(this, task -> {
            getConfig().set("max-radius", radius);
            saveConfig();
        });
    }
}
