package com.example.utils

import android.content.Context
import android.widget.Toast

object ToastUtils {
    private var currentToast: Toast? = null

    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        try {
            currentToast?.cancel()
            val newToast = Toast.makeText(context.applicationContext, message, duration)
            currentToast = newToast
            newToast.show()
        } catch (_: Exception) {
            // Fallback for safety
        }
    }
}
