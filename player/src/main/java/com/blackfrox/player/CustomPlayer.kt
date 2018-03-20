package com.blackfrox.player

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.blackfrox.player.base.StandardMPlayer
import kotlinx.android.synthetic.main.layout_dialog_loading.view.*
import kotlinx.android.synthetic.main.video_dialog_brightness.view.*
import kotlinx.android.synthetic.main.video_dialog_progress.view.*
import kotlinx.android.synthetic.main.video_dialog_volume.view.*
import kotlinx.android.synthetic.main.video_media_controller_portrait.view.*
import kotlinx.android.synthetic.main.video_player.view.*

/**
 * Created by Administrator on 2018/3/18 0018.
 * 参考bilibili播放器 1 竖屏  显示toolBar和下控制层_portrait
 *                    2 横屏  显示上下控制层
 */
class CustomPlayer @JvmOverloads constructor(context: Context, attributeSet: AttributeSet?=null, def: Int=0)
    : StandardMPlayer(context,attributeSet,def) {
    private val TAG="CustomPlayer"

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
    //竖屏状态的Ui
    protected lateinit var mControlBottomPortrait: ViewGroup

    override fun initView(context: Activity) {
        super.initView(context)
        mControlBottomPortrait =findViewById(R.id.control_bottom_portrait)
        seekBar_portrait.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                show(3600000)
                mDragging = true
                removeCallbacks(mShowProgressPortrait)
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser)
                    return
                val newPosition = (getDuration() * progress / 1000L)
                mCurrentTimeTv?.text = stringForTime(newPosition)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mDragging = false
                val newPosition = (getDuration() * seekBar.progress / 1000L)
                seekTo(newPosition)
                setProgressPortrait()
                updatePausePlayPortrait()
                show()
                post(mShowProgressPortrait)
            }
        })
        img_play_portrait.setOnClickListener {
            if (isPlaying()) {
                pause()
                img_play_portrait.setImageResource(R.drawable.bili_player_play_can_pause
                )
            } else {
                start()
                img_play_portrait.setImageResource(R.drawable.bili_player_play_can_play)
            }
            postDelayed({show()},300)
        }
    }

    //我也不想写下面这段话，但是两个UI的处理解决办法就是这样
    // 横屏 ->  根据isShowing 显示UI/隐藏UI
    // 竖屏 -> 显示UI
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isFullScreen){
           if(isShowing){
               hide()
               show()
           }else{
               hide()
           }
        }else{
            hide()
            show()
        }
    }

    //根据横竖屏，显示不同的ui
    // 1 竖屏     显示 toolBar 和 下controller_portrait
    // 2 横屏     显示 上controller 和 下controller
    //TODO 横竖屏状态下  控制Ui需要写两个 因为横屏状态的操作功能会增多
    override fun show(timeout: Long) {
        if (isFullScreen){
            super.show(timeout)
            toolBar?.visibility = View.GONE
            mControlBottomPortrait.visibility = View.GONE
        }else{ //竖屏状态
            if (!isShowing){
                setProgressPortrait()

                toolBar?.visibility = View.VISIBLE
                mControlBottomPortrait.visibility = View.VISIBLE
                ////////////////////////////////////
                isShowing=true
            }
            updatePausePlayPortrait()
            post(mShowProgressPortrait)
            if (timeout!=0L){
                removeCallbacks(mFadeOut)
                postDelayed(mFadeOut,timeout)
            }
        }
    }

    protected val mShowProgressPortrait=object : Runnable{
        override fun run() {
            val pos=setProgressPortrait()
            if (!mDragging && isShowing && isPlaying())
                postDelayed(this, (1000-(pos%1000)))
        }
    }

    private fun updatePausePlayPortrait() {
        img_play_portrait.setImageResource(if (isPlaying()) R.drawable.bili_player_play_can_pause
                                            else            R.drawable.bili_player_play_can_play)
    }

    private fun setProgressPortrait(): Long {
        if (mMediaPlayer==null || mDragging)
            return 0
        if (getDuration()>0){
            //use long to avoid overflow
            val pos=1000L * getCurrentPosition() /getDuration()
            seekBar_portrait?.progress= pos.toInt()
        }
        seekBar_portrait?.secondaryProgress=getBufferPercentage()

        tv_current_portrait?.text=stringForTime(getCurrentPosition())
        tv_total_portrait?.text=stringForTime(getDuration())
        return getCurrentPosition()
    }
    override fun hide() {
        super.hide()
        //只有在竖屏并且正在播放时隐藏actionBar，否则没有必要
        if(isPlaying()&&!isFullScreen) toolBar?.visibility= View.GONE
        if (!isFullScreen)
            control_bottom_portrait.visibility= View.GONE
    }

    override fun dismissLockDialog() {

    }

    override fun showLockDialog() {

    }
    override fun showBrightnessDialog(brightnessPercent: Float) {
        layout_brightness.visibility= View.VISIBLE
        progressBar_brightness.progress= brightnessPercent.toInt()
    }

    override fun showVolumeDialog(deltaY: Float, volumePercent: Float) {
        layout_volume.visibility= View.VISIBLE
        progressBar_volume.progress=volumePercent.toInt()
    }

    override fun showProgressDialog(deltaX: Float, newPosition: Long) {
        layout_progress.visibility= View.VISIBLE
        mSeekDuration.text=stringForTime(getDuration())
        mSeekCurProgress.text=stringForTime(newPosition)
        val de=newPosition-getCurrentPosition()
        val addTime=de/1000
        tv_add_time.text=if (addTime>0) "+${addTime}秒" else "${addTime}秒"
        if (!isFullScreen) {
            val progress=newPosition*1000/(if (getDuration()==0L) 1 else getDuration())
            seekBar_portrait.progress= progress.toInt()
        }
    }

    override fun dismissBrightnessDialog() {
        layout_brightness.visibility= View.GONE
    }

    override fun dismissVolumeDialog() {
        layout_volume.visibility=GONE
    }

    override fun dismissProgressDialog() {
        layout_progress.visibility= View.GONE
    }

    override fun showLoadingDialog() {
        layout_loading.visibility= View.VISIBLE
    }

    override fun dismissLoadingDialog() {
        layout_loading.visibility= View.GONE
    }

    override fun showNetWorkErrorDialog() {
       AlertDialog.Builder(getContext())
                .setMessage(R.string.error_network)
                .setPositiveButton(R.string.VideoView_error_button,
                        DialogInterface.OnClickListener { dialog, whichButton ->

                        })
                .setCancelable(false)
                .show()
    }

}