package com.blackfrox.player.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo

/**
 * Created by Administrator on 2017/11/21 0021.
 */
object Common {

    /**
     * Get activity from context object
     *
     * @param context something
     * @return object of Activity or null if it is not Activity
     */

    fun scanForActivity(context: Context): Activity? {
        if (context is Activity){
            return context as Activity
        }else if (context is ContextWrapper){
            return scanForActivity((context as ContextWrapper).baseContext)
        }
        return null
    }

    fun getActivityContext(context: Context?): Activity? {
        if (context == null)
            return null
        else if (context is Activity)
            return context
        else if (context is ContextWrapper)
            return scanForActivity(context.baseContext)

        return null
    }
}