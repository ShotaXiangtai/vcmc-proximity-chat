package com.example.proximitychat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatRangeTest {
    private val range = ChatRange(5, 15, 25)

    private fun tier(distance: Double) = range.tierFor(distance * distance)

    @Test
    fun `距離に応じた段階になる`() {
        assertEquals(ChatTier.VERY_CLOSE, tier(0.0))
        assertEquals(ChatTier.VERY_CLOSE, tier(5.0))
        assertEquals(ChatTier.CLOSE, tier(5.5))
        assertEquals(ChatTier.CLOSE, tier(15.0))
        assertEquals(ChatTier.AROUND, tier(15.5))
        assertEquals(ChatTier.AROUND, tier(25.0))
    }

    @Test
    fun `最大距離を超えると届かない`() {
        assertNull(tier(25.5))
    }

    @Test
    fun `最大距離を近くのしきい値より小さくしても最大距離で打ち切る`() {
        val narrowed = range.copy(maxRadius = 10)
        assertEquals(ChatTier.CLOSE, narrowed.tierFor(100.0))
        assertNull(narrowed.tierFor(121.0))
    }

    @Test
    fun `正しい設定はそのまま使い警告しない`() {
        val warnings = mutableListOf<String>()
        assertEquals(ChatRange(5, 15, 25), ChatRange.sanitize(5, 15, 25, warnings::add))
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `不正な設定は補正して警告する`() {
        val warnings = mutableListOf<String>()
        assertEquals(ChatRange(0, 3, ChatRange.DEFAULT_MAX_RADIUS), ChatRange.sanitize(-1, 3, 0, warnings::add))
        assertEquals(2, warnings.size)
    }

    @Test
    fun `close が very-close より小さければ揃える`() {
        val warnings = mutableListOf<String>()
        assertEquals(ChatRange(10, 10, 25), ChatRange.sanitize(10, 3, 25, warnings::add))
        assertEquals(1, warnings.size)
    }
}
