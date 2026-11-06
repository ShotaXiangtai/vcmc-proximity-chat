package com.example.proximitychat.i18n

import kotlin.test.Test
import kotlin.test.assertNotNull

class TranslationFilesTest {
    @Test
    fun `どの言語にも日本語と同じ文面がある`() {
        val messages = assertNotNull(TranslationFilesCheck.resource("config.yml").getConfigurationSection("messages"))
        TranslationFilesCheck.check(TranslationFilesCheck.texts(messages))
    }
}
