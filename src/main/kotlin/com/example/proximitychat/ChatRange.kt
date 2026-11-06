package com.example.proximitychat

enum class ChatTier { VERY_CLOSE, CLOSE, AROUND }

/**
 * チャットの届く距離。変更するときは新しいインスタンスに置き換えるので、どのスレッドから読んでもよい。
 */
data class ChatRange(val veryClose: Int, val close: Int, val maxRadius: Int) {

    /** 距離の 2 乗から段階を返す。届かなければ null。 */
    fun tierFor(distanceSquared: Double): ChatTier? = when {
        distanceSquared > square(maxRadius) -> null
        distanceSquared <= square(veryClose) -> ChatTier.VERY_CLOSE
        distanceSquared <= square(close) -> ChatTier.CLOSE
        else -> ChatTier.AROUND
    }

    private fun square(value: Int): Double = value.toDouble() * value

    companion object {
        const val DEFAULT_VERY_CLOSE = 5
        const val DEFAULT_CLOSE = 15
        const val DEFAULT_MAX_RADIUS = 25

        /**
         * 設定値を確認して使える値にする。
         * close より max-radius が小さいのは /setradius で正しく起きるので、問題にしない。
         */
        fun sanitize(veryClose: Int, close: Int, maxRadius: Int, warn: (String) -> Unit = {}): ChatRange {
            val radius = if (maxRadius >= 1) {
                maxRadius
            } else {
                warn("max-radius は 1 以上にしてください: $maxRadius（$DEFAULT_MAX_RADIUS を使います）")
                DEFAULT_MAX_RADIUS
            }
            val nearest = if (veryClose >= 0) {
                veryClose
            } else {
                warn("very-close は 0 以上にしてください: $veryClose（0 を使います）")
                0
            }
            val near = if (close >= nearest) {
                close
            } else {
                warn("close は very-close 以上にしてください: $close（$nearest を使います）")
                nearest
            }
            return ChatRange(nearest, near, radius)
        }
    }
}
