package com.example.videoplayerdemo

import android.content.Context

/**
 * Created by Administrator on 2017/9/3/003.
 * 屏幕像素转换工具
 */
object DisplayUtil {

    fun px2dp(context: Context,pxValue: Float):Int{
        val scale=context.resources.displayMetrics.density
        return (pxValue/scale+0.5f).toInt()
    }

    fun dp2px(context: Context,dipValue: Float): Int{
        val scale=context.resources.displayMetrics.density
        return (dipValue*scale+0.5f).toInt()
    }
}