package com.blackfrox.player

import android.app.Application
import kotlin.properties.Delegates

/**
 * Created by Administrator on 2018/3/4 0004.
 */
class MyApp: Application() {
    companion object {
        var instance by Delegates.notNull<MyApp>()
    }
    override fun onCreate() {
        super.onCreate()
        instance=this
    }
}