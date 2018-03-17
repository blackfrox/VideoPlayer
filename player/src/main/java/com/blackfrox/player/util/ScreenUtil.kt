package com.example.a2048.util

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Created by Administrator on 2017/11/10 0010.
 */
object ScreenUtil {
    /**
     * 获取屏幕相关参数
     */
    fun getScreenSize(context: Context) : DisplayMetrics{
        val metrics=DisplayMetrics()
        val wm=context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getMetrics(metrics)
        return metrics
    }

    /**
     * 获取屏幕density
     */
    fun getDeviceDensity(context: Context): Float {
       return getScreenSize(context).density
    }
}