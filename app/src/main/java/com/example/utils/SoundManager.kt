package com.example.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

/** Lightweight game sound cues that do not depend on bundled media files. */
class SoundManager(@Suppress("UNUSED_PARAMETER") context: Context) {
    private var toneGenerator: ToneGenerator? = ToneGenerator(AudioManager.STREAM_MUSIC, 80)

    fun playWhistle() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
    }

    fun playBasket() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 90)
    }

    fun playBuzzer() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 220)
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
