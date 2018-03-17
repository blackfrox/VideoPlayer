package com.blackfrox.player.util

import android.app.Activity
import android.content.pm.ActivityInfo
import android.provider.Settings
import android.view.OrientationEventListener

/**
 * Created by Administrator on 2017/11/20 0020.
 * 处理屏幕旋转的逻辑
 *
 */
class OrientationUtils(val activity: Activity) {

//    private lateinit var activity: Activity
//    private lateinit var videoPlayer: MediaController

    private var screenType=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    //值 0: 竖屏，  1: 横屏  2: 反向横屏
    private var mIsLand=0

    private var mClick=false
    private var mClickLand=false
    private var mClickPort=false
    private var mEnable=true
    private var mRotateWithSystem=true //是否跟随系统
    private lateinit var orientaionEventListener: OrientationEventListener
    init {
         orientaionEventListener=object : OrientationEventListener(activity) {
            override fun onOrientationChanged(orientation: Int){
                val autoRotateOn=(Settings.System.getInt(activity.contentResolver,
                        Settings.System.ACCELEROMETER_ROTATION,0)==1)
                if (!autoRotateOn&&mRotateWithSystem){
                    return
                }
                //设置竖屏
                if ((orientation>=0&&orientation<=30)||orientation>=330){
                    if (mClick){
                        if (mIsLand>0&&!mClickLand){
                            return
                        }else{
                            mClickPort=true
                            mClick=false
                            mIsLand=0
                        }
                    }else{
                        if (mIsLand>0){
                            screenType=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//                           //根据横竖屏的状态来动态显示fullscreenButton
//                            videoPlayer.setFullscreenButtonVisiblity(videoPlayer.isFullScreen())
                            mIsLand=0
                            mClick=false
                        }
                    }
                }
                //设置横屏
                else if (orientation>=230&&orientation<=330){

                    if (mClick){
                        if (!(mIsLand==1)&&!mClickPort){
                            return
                        }else{
                            mClickLand=true
                            mClick=false
                            mIsLand=1
                        }
                    }else{
                        if (!(mIsLand==1)){
                            screenType=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            mIsLand=1
                            mClick=false
                        }
                    }
                }
                //设置反向横屏
                else if (orientation>30&&orientation<95){
                    if (mClick){
                        if (!(mIsLand==2)&&!mClickPort){
                            return
                        }else{
                            mClickLand=true
                            mClick=false
                            mIsLand=2
                        }
                    }else if (!(mIsLand==2)){
                        screenType=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        mIsLand=2
                        mClick=false
                    }
                }
            }
        }
        orientaionEventListener.enable()
    }

    /**
     * 点击切换的逻辑，比如竖屏的时候点击了就是切换到横屏不会受屏幕的影响
     */
    fun resolveByClick(){
        mClick=true
        if (mIsLand==0){
            screenType=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            mIsLand=1
            mClickLand=false
        }else{
            screenType=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            mIsLand=0
            mClickPort=false
        }
    }

    /**
     * 列表返回的样式判断，因为立即旋转会导致界面跳动的问题
     */
    fun backToProtVideo():Int{
        if (mIsLand>0){
            mClick=true
            activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            mIsLand=0
            mClickPort=false
            return 500
        }
        return 0
    }

    fun isEnable()=mEnable

    fun setEnable(enable: Boolean){
        mEnable=enable
        if (mEnable){
            orientaionEventListener.enable()
        }else{
            orientaionEventListener.disable()
        }
    }

    fun releaseListener()=orientaionEventListener.disable()

    fun isClick()=mClick

    fun isClickLand()=mClickLand

    fun setClickLand(b: Boolean){
        mClickLand= b
    }

    fun isClickPort()=mClickPort

    fun setClickPort(b: Boolean){
        mClickPort=b
    }

    fun getScreenType()=screenType

    fun setScreenType(type :Int){
        screenType=type
    }

    fun isRotateWithSystem()=mRotateWithSystem

    /**
     * 是否更新系统旋转，false的话，系统禁止旋转也会跟着旋转
     *
     */
    fun setRotateWithSystem(boolean: Boolean){
        mRotateWithSystem=boolean
    }
}