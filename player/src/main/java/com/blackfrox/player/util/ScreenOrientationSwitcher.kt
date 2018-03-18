package com.blackfrox.player.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.ActionMode
import android.view.OrientationEventListener
import java.lang.ref.WeakReference

/**
 * Created by Administrator on 2018/3/18 0018.
 * 屏幕旋转处理工具
 */
class ScreenOrientationSwitcher(context: Context,rate: Int): OrientationEventListener(context) {

    private val MAX_CHECK_INTERVAL=3000

    interface OnChangeListener{
        fun onChanged(requestedOrientation: Int)
    }

    private var mContextRef: WeakReference<Context>
    private var mIsSupportGravity=false //是否自动旋转
    private var mCurrentOrientation= ORIENTATION_UNKNOWN
    private var mLastCheckTimestamp=0L

    var mChangeListener: OnChangeListener?=null

    init {
        mContextRef=WeakReference(context)
    }

    @SuppressLint("WrongConstant")
    override fun onOrientationChanged(orientation: Int) {
        val context=mContextRef.get()
        if (context!=null||!(context is Activity))
            return

        val currTimesTamp=System.currentTimeMillis()
        if (currTimesTamp-mLastCheckTimestamp>MAX_CHECK_INTERVAL){
            mIsSupportGravity=isScreenAutoRotate(context)
            mLastCheckTimestamp=currTimesTamp
        }

        if (!mIsSupportGravity)
            return

        if (orientation== ORIENTATION_UNKNOWN)
            return

        var requestOrientation = ORIENTATION_UNKNOWN
        requestOrientation = when {
            orientation > 350 || orientation < 10 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            orientation > 80 && orientation < 100 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            orientation > 260 && orientation < 280 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> return
        }
        if (requestOrientation==mCurrentOrientation)
            return

        val needNotify=mCurrentOrientation!= ORIENTATION_UNKNOWN
        mCurrentOrientation=requestOrientation
        if (needNotify){
            if (mChangeListener!=null){
               mChangeListener!!.onChanged(requestOrientation)
            } else{
                val activity=context as Activity
                activity.requestedOrientation=requestOrientation
            }
        }
    }
}