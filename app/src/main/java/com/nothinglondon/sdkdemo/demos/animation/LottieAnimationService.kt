package com.nothinglondon.sdkdemo.demos.animation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.util.Log
import com.nothing.ketchum.GlyphMatrixManager
import com.nothinglondon.sdkdemo.demos.GlyphMatrixService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

class LottieAnimationService : GlyphMatrixService("Sharingan") {

    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        backgroundScope.launch {
            val frames = loadFrames(context)
            if (frames.isEmpty()) {
                Log.e("LottieService", "No frames loaded!")
                return@launch
            }
            Log.d("LottieService", "Loaded ${frames.size} frames")
            while (isActive) {
                for (frame in frames) {
                    if (!isActive) break
                    val frameData = frame
                    uiScope.launch {
                        glyphMatrixManager.setMatrixFrame(frameData)
                    }
                    delay(100)
                }
            }
        }
    }

    override fun performOnServiceDisconnected(context: Context) {
        backgroundScope.cancel()
    }

    private fun loadFrames(context: Context): List<IntArray> {
        val result = mutableListOf<IntArray>()
        try {
            val json = context.assets.open("animation.json")
                .bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val assets = root.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val p = asset.optString("p", "")
                if (p.startsWith("data:image/png;base64,")) {
                    val b64 = p.removePrefix("data:image/png;base64,")
                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        val scaled = Bitmap.createScaledBitmap(bmp, 25, 25, true)
                        val arr = IntArray(625)
                        for (row in 0 until 25) {
                            for (col in 0 until 25) {
                                val pixel = scaled.getPixel(col, row)
                                val gray = (Color.red(pixel) * 0.299 +
                                        Color.green(pixel) * 0.587 +
                                        Color.blue(pixel) * 0.114).toInt()
                                arr[row * 25 + col] = gray
                            }
                        }
                        result.add(arr)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LottieService", "Error: ${e.message}")
        }
        return result
    }
}