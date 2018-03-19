package com.blackfrox.player

import android.app.Activity
import android.content.Context
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.blackfrox.player.base.StandardMPlayer

/**
 * Created by Administrator on 2018/3/18 0018.
 * 参考bilibili播放器 1 竖屏  显示toolBar和下控制层_portrait
 *                    2 横屏  显示上下控制层
 */
class CustomPlayer @JvmOverloads constructor(context: Context, attributeSet: AttributeSet?=null, def: Int=0)
    : StandardMPlayer(context,attributeSet,def) {


    override fun getLayoutId() = R.layout.video_player

    //actionBar必须是toolBar，并且player必须在toolBarLayout布局里
    //不然会很难看，还不如不用
    var toolBar: Toolbar?=null
        set(value) {
            field=value
            if (isShowing)
                field?.visibility= View.VISIBLE
            else field?.visibility= View.GONE
        }
    //竖屏ui
    protected lateinit var mControlBottomPortrait: ViewGroup

    override fun initView(context: Activity) {
        super.initView(context)
        mControlBottomPortrait =findViewById(R.id.control_bottom_portrait)
    }
    override fun show(timeout: Long) {
        if (!isShowing){
            setProgress()

            ////////////这段是修改过的///////
            //根据横竖屏，显示不同的ui
            // 1 竖屏     显示 toolBar 和 下controller_portrait
            // 2 横屏     显示 上controller 和 下controller
            //TODO 横竖屏状态下  控制Ui需要写两个 因为横屏状态的操作功能会增多
            if (!isFullScreen) {
                toolBar?.visibility = View.VISIBLE
                mControlBottomPortrait.visibility = View.VISIBLE
                mControlTop?.visibility = GONE
                mControlBottom?.visibility = View.GONE
            } else {
                toolBar?.visibility= View.GONE
                mControlBottomPortrait.visibility= View.GONE
                mControlTop?.visibility= View.VISIBLE
                mControlBottom?.visibility= View.VISIBLE
            }
            ////////////////////////////////////

            isShowing=true
        }
        updatePausePlay()
        post(mShowProgress)
        if (timeout!=0L){
            removeCallbacks(mFadeOut)
            postDelayed(mFadeOut,timeout)
        }

    }

    override fun hide() {
        super.hide()
        if (isShowing){
            //只有在竖屏并且正在播放时隐藏actionBar，否则没有必要
            if(isPlaying()&&!isFullScreen) toolBar?.visibility= View.GONE
        }
    }

    override fun dismissLockDialog() {

    }

    override fun showLockDialog() {

    }
    override fun showBrightnessDialog(brightnessPercent: Float) {

    }

    override fun showVolumeDialog(deltaY: Float, volumePercent: Float) {

    }

    override fun showProgressDialog(deltaX: Float, newPosition: Long) {

    }

    override fun dismissBrightnessDialog() {

    }

    override fun dismissVolumeDialog() {

    }

    override fun dismissProgressDialog() {

    }


}