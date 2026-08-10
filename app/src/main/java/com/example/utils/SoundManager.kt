package com.example.utils

import android.content.Context
import android.media.MediaPlayer
import com.example.R

class SoundManager(private val context: Context) {

    private var whistlePlayer: MediaPlayer? = null
    private var basketPlayer: MediaPlayer? = null
    private var buzzerPlayer: MediaPlayer? = null

    init {
        try {
            whistlePlayer = MediaPlayer.create(context, R.raw.whistle)
            basketPlayer = MediaPlayer.create(context, R.raw.basket)
            buzzerPlayer = MediaPlayer.create(context, R.raw.buzzer)
        } catch (e: Exception) {
            // Ignorar se os arquivos não existirem
        }
    }

    fun playWhistle() {
        try {
            whistlePlayer?.start()
        } catch (e: Exception) {
            // Ignorar erro
        }
    }

    fun playBasket() {
        try {
            basketPlayer?.start()
        } catch (e: Exception) {
            // Ignorar erro
        }
    }

    fun playBuzzer() {
        try {
            buzzerPlayer?.start()
        } catch (e: Exception) {
            // Ignorar erro
        }
    }

    fun release() {
        try {
            whistlePlayer?.release()
            basketPlayer?.release()
            buzzerPlayer?.release()
        } catch (e: Exception) {
            // Ignorar erro
        }
    }
}
