package com.blackfrox.player.util

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.DisplayMetrics

/**
 * 关于展示的工具类
 */
object DisplayUtils {
    /**
     * 将px值转换为dp值
     */
    fun px2dp(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    /**
     * 将dp值转换为px值
     */
    fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 将px值转换为sp值
     */
    fun px2sp(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.scaledDensity
        return (pxValue / scale + 0.5f).toInt()
    }

    /**
     * 将sp值转换为px值
     */
    fun sp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.scaledDensity
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 获取屏幕宽度
     */
    fun getScreenWidthPixels(context: Activity): Int {
        val metric = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(metric)
        return metric.widthPixels
    }

    /**
     * 获取屏幕高度
     */
    fun getScreenHeightPixels(context: Activity): Int {
        val metric = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(metric)
        return metric.heightPixels
    }

    /**
     * 切换屏幕的方向.
     */
    fun toggleScreenOrientation(activity: Activity) {
        activity.requestedOrientation = if (isPortrait(activity))
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    /**
     * 获得当前屏幕的方向.
     *
     * @return 是否竖屏.
     */
    fun isPortrait(context: Context): Boolean {
        val orientation = context.resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_PORTRAIT
    }
}
