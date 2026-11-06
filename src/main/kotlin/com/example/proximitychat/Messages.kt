package com.example.proximitychat

import com.example.proximitychat.i18n.Language
import com.example.proximitychat.i18n.Translations
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.plugin.java.JavaPlugin

/**
 * 受け取る人の言語で文面を作る。日本語は config.yml の messages、ほかの言語は lang フォルダから読む。
 * プレースホルダーの値はタグとして解釈しない。プレイヤーの発言や名前をそのまま渡せるようにするため。
 */
class Messages(private val plugin: JavaPlugin) {
    private val translations = Translations(plugin)
    private val miniMessage = MiniMessage.miniMessage()

    fun reload() = translations.reload()

    fun text(language: Language, key: String, vararg args: Pair<String, String>): Component {
        val template = translations.string(language, key) ?: plugin.config.getString("messages.$key") ?: key
        return miniMessage.deserialize(template, TagResolver.resolver(args.map { (name, value) -> Placeholder.unparsed(name, value) }))
    }

    fun send(target: Audience, key: String, vararg args: Pair<String, String>) {
        target.sendMessage(text(Language.of(target), key, *args))
    }

    /** 全員とコンソールへ、それぞれの言語で送る。 */
    fun broadcast(key: String, vararg args: Pair<String, String>) {
        plugin.server.onlinePlayers.forEach { send(it, key, *args) }
        send(plugin.server.consoleSender, key, *args)
    }
}
