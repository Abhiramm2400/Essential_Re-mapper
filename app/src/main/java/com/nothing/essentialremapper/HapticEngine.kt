
package com.nothing.essentialremapper
import android.content.Context, android.os.*
object HapticEngine {
    enum class Type { LIGHT,MEDIUM,HEAVY,DOUBLE,GLYPH }
    fun vibrate(context:Context, intensityPercent:Int, type:Type){
        val vib = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){ (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator } else { @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
        if(!vib.hasVibrator()) return
        val amp = ((intensityPercent.coerceIn(0,100)/100f)*255).toInt().coerceIn(1,255)
        val effect = when(type){ Type.LIGHT->VibrationEffect.createOneShot(20,amp); Type.MEDIUM->VibrationEffect.createOneShot(40,amp); Type.HEAVY->VibrationEffect.createOneShot(80,amp); Type.DOUBLE->VibrationEffect.createWaveform(longArrayOf(0,30,40,30), intArrayOf(0,amp,0,amp), -1); Type.GLYPH->VibrationEffect.createWaveform(longArrayOf(0,20,30,20,30,60), intArrayOf(0,amp,0,amp,0,amp), -1) }
        vib.vibrate(effect)
    }
}
