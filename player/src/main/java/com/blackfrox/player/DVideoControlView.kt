package com.blackfrox.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.text.TextUtils
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.SeekBar
import com.blackfrox.player.util.CommonUtil
import com.blackfrox.player.util.CommonUtil.hideNavKey
import com.blackfrox.player.util.DisplayUtils
import kotlinx.android.synthetic.main.video_media_controller.view.*

/**
 * Created by Administrator on 2018/2/21 0021.
 * 播放UI的显示、控制层、手势处理
 * //自定义UI，需要在这里使用findViewById，但是我懒得写了，直接用kotlin的view-extension，缺点是不能继承了
 * 毕竟原本的继承方法也是需要吧控件id设置为对应的才行
 *
 * 1 根据横竖屏控制toolBar和上部的layout的显示和隐藏
 */
abstract class DVideoControlView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, def: Int = 0)
    : DVideoView(context, attributeSet, def) {

    //总觉得一句话能说完的，要说三四句话，

    //手指放下的位置(手指触摸屏幕时的currentPosition，服了，这个变量有必要么?)
    protected var mDownPosition = 0L

    //手势调节音量的大小
    protected var mGestureDownVolume = 0

    //手势偏差值
    protected var mThreshold = 80

    //手动改变滑动的位置(手指滑动改变的距离)
    protected var mSeekTimePosition = 0L

    private var mChangeVolume = false
    private var mChangePosition = false
    private var mBrightness = false
    //是否触摸到了虚拟按键
    private var mShowVKey = false
    private var mHideKey = false

    override fun getLayoutId(): Int = R.layout.video_media_controller
    protected var mSeekEndOffset: Int

    //是否拖动seekBar
    protected var mDragging = false

    init {
        mSeekEndOffset = DisplayUtils.dp2px(context, 50F)

        img_pause.setOnClickListener {
            clickStartIcon()
        }

//        fullscreenButton.setOnClickListener
//        backButton.setOnClickListener

        seekBar.max = mVideoManager.mediaPlayer.duration.toInt()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStartTrackingTouch(seekBar: SeekBar) {

                mDragging = true
                removeCallbacks(mShowProgress)
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser)
                    return

                this@DVideoControlView.seekBar.progress = getCurrentPositionWhenPlaying().toInt()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mDragging = false
                //mHadPlay这个变量有什么用啊
                if (mHadPlay) {
                    mVideoManager.mediaPlayer.seekTo(seekBar.progress.toLong())
                }

                post(mShowProgress)
            }

        })

        if (mIfCurrentIsFullscreen && mHideKey)
            CommonUtil.hideNavKey(context)
    }

    //控制器是否显示
    protected var mShowing=false

    private val mShowProgress=object : Runnable{
        override fun run() {
            val pos=setProgress()
            if (mDragging&&mShowing&&mVideoManager.mediaPlayer.isPlaying){
                postDelayed(this,(1000-pos%1000))
            }
        }
    }

    protected fun setProgress(): Long {
        if (mDragging) {
            return 0
        }

        seekBar.progress = getCurrentPositionWhenPlaying().toInt()

        tv_current.text = CommonUtil.stringForTime(getCurrentPositionWhenPlaying().toInt())
        tv_total.text = CommonUtil.stringForTime(getDuration().toInt())

        return getCurrentPositionWhenPlaying()
    }
    /**
     * 双击
     */
    protected val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            touchDoubleUp()
            return super.onSingleTapUp(e)
        }
    })


    /**
     * 音量，亮度,进度
     */
    private var mDownX = 0F
    private var mDownY = 0F
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        //双击的逻辑
        gestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchSurfaceDown(x, y)
            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - mDownX
                val deltaY = y - mDownY
                val absDeltaX = Math.abs(deltaX)
                val absDeltaY = Math.abs(deltaY)

                //判断当前有效行为
                if (!mChangePosition && !mChangeVolume && !mBrightness)
                    touchMoveFullLogic(absDeltaX, absDeltaY)

                //根据有效行为，做出具体逻辑
                touchMove(deltaX, deltaY, y)
            }
            MotionEvent.ACTION_UP -> {
                touchUp()

                //不要和隐藏虚拟按键后，滑出虚拟按键冲突
                if (mHideKey && mShowVKey) {
                    return true
                }
            }
        }

        return super.onTouchEvent(event)
    }

    private fun touchMove(deltaX: Float, deltaY: Float, y: Float) {
        val curWidth = if (CommonUtil.getCurrentScreenLand(getActivityContext())) mScreenHeight else mScreenWidth
        val curHeight = if (CommonUtil.getCurrentScreenLand(getActivityContext())) mScreenWidth else mScreenHeight

        when {
            mChangePosition -> {
                val totalTimeDuration = getDuration()
//                mSeekTimePosition = (mDownPosition + (deltaX * totalTimeDuration / curWidth) / mSeekRatio)
                mSeekTimePosition=getCurrentPositionWhenPlaying()
                if (mSeekTimePosition > totalTimeDuration)
                    mSeekTimePosition = totalTimeDuration
                val seekTime = CommonUtil.stringForTime(mSeekTimePosition.toInt())
                val totalTime = CommonUtil.stringForTime(totalTimeDuration.toInt())
                showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration)
            }
            mChangeVolume -> {
                val deltaY = -deltaY
                val max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val deltaV = (max * deltaY * 3 / curHeight)
                mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (mGestureDownVolume + deltaV).toInt(), 0)
                val volumePercent = (mGestureDownVolume * 100 / max + deltaY * 3 / curHeight)

                showVolumeDialog(-deltaY, volumePercent)
            }
            !mChangePosition && mBrightness -> {
                if (Math.abs(deltaY) > mThreshold) {
                    val percent = -deltaY / curHeight
                    onBrightnessSlide(percent)
                    mDownY = y
                }
            }
        }
    }

    /**
     * 滑动改变亮度
     */
    private fun onBrightnessSlide(percent: Float) {
        var currentBrightness=(context as Activity).window.attributes.screenBrightness
        if (currentBrightness<=0.00f)
            currentBrightness=0.00f
        else if (currentBrightness<0.01f)
            currentBrightness=0.01f
        val lpa=(context as Activity).window.attributes
        lpa.screenBrightness=currentBrightness+percent
        if (lpa.screenBrightness>1.0f)
            lpa.screenBrightness=1.0f
        else if (lpa.screenBrightness<0.01f)
            lpa.screenBrightness=0.01f
        showBrightnessDialog(lpa.screenBrightness)
        getActivityContext()?.window?.attributes=lpa
    }


    override fun setUp(url: String, title: String): Boolean {
        return super.setUp(url, title)
    }

    protected fun touchUp() {
        dismissProgressDialog()
        dismissVolumeDialog()
        dismissBrightnessDialog()

        when {
            mChangePosition -> {
                if (mCurrentState == STATE_PLAYING || mCurrentState == STATE_PAUSE) {
                    mVideoManager.mediaPlayer.seekTo(mSeekTimePosition)
                    seekBar.progress = mSeekTimePosition.toInt()
                }
            }
            mBrightness -> {
                //回调
            }
            mChangeVolume -> {
                //回调
            }
            //控制器的显示和隐藏
            !mChangePosition && !mChangeVolume &&!mBrightness -> onClickUiToggle()
        }
    }




    /**
     * 双击暂停/播放
     * 如果不需要，重载为空方法即可
     */
    protected fun touchDoubleUp() {
        if (!mHadPlay)
            return
        clickStartIcon()
    }

     var mNeedShowWifiTip: Boolean=true

    /**
     * 播放按键点击
     */
    protected fun clickStartIcon() {
        if (TextUtils.isEmpty(mUrl))
            return
        when (mCurrentState) {
            STATE_NORMAL, STATE_ERROR -> {
                if (!mUrl.startsWith("file") && !CommonUtil.isWifiConnected(context)
                        && mNeedShowWifiTip) {
                    showWifiDialog()
                    return
                }
                startButtonLogic() //调用该方法后，接下来是STATE_PLAYING
            }
            STATE_PLAYING -> {
                if (mVideoManager.mediaPlayer.isPlaying) {
                    mVideoManager.mediaPlayer.pause()
                    setStateAndUi(STATE_PAUSE)
                }
            }
            STATE_PAUSE -> {
                if (!mVideoManager.mediaPlayer.isPlaying) {
                    mVideoManager.mediaPlayer.start()
                    setStateAndUi(STATE_PLAYING)
                }
            }
//            STATE_AUTO_COMPLETE-> startButtonLogic()
        }
    }

    /*
        处理控制显示
     */
    override fun setStateAndUi(state: Int) {
        when (state) {
            STATE_NORMAL -> {
                changeUiToNormal()
                cancelDismissControlViewTimer()
            }
            STATE_PREPARING -> {
                changeUiToPreparingView()
                startDismissControlViewTimer()
            }
            STATE_PLAYING -> {
                changeUiToPlayingShow()
                startDismissControlViewTimer()
            }
            STATE_PAUSE -> {
                changeUiToPauseView()
                cancelDismissControlViewTimer()
            }
            STATE_ERROR -> {
                changeUiToError()
            }
            STATE_PLAYING_BUFFERING_START -> {
                changeUiToPlayingBufferingShow()
            }
        }
    }

    var timeOut: Long=2500L

    private fun startDismissControlViewTimer() {
        postDelayed(mHideControllerView,timeOut)
    }

    private fun cancelDismissControlViewTimer() {
        removeCallbacks(mHideControllerView)
    }

    private val mHideControllerView=object: Runnable{
        override fun run() {

            if (mCurrentState != STATE_NORMAL
                    && mCurrentState != STATE_ERROR
//                    && mCurrentState !=STATE_AUTO_COMPLETE
                    ) {
                if (getActivityContext() != null) {
                    (getActivityContext() as Activity).runOnUiThread {
                        this@DVideoControlView.hideAllWidget()
//                        setViewShowState(mLockScreen, GONE)
                        if (mHideKey && mIfCurrentIsFullscreen && mShowVKey)
                            hideNavKey(context)
                    }
                }
            }
        }
    }

    abstract fun hideAllWidget()

    abstract fun changeUiToPlayingBufferingShow()

    abstract fun changeUiToError()

    abstract fun changeUiToPauseView()

    abstract fun changeUiToPlayingShow()

    abstract fun changeUiToPreparingView()

    abstract fun changeUiToNormal()

    protected fun startProgressTimer() {
        cancelProgressTimer()
//        updateProgressTimer = Timer()
//        mProgressTimerTask = ProgressTimerTask()
//        updateProgressTimer.schedule(mProgressTimerTask, 0, 300)
        //上面三句话可以用这一句话代替，呵呵
        post(mShowProgress)
    }


    protected fun cancelProgressTimer() {
//        if (updateProcessTimer != null) {
//            updateProcessTimer.cancel()
//            updateProcessTimer = null
//        }
//        if (mProgressTimerTask != null) {
//            mProgressTimerTask.cancel()
//            mProgressTimerTask = null
//        }
        //这上面所有的可以用一句话代替,呵呵*2
        removeCallbacks(mShowProgress)
    }

    /**
     * @param secProgress:缓冲进度(当前第二进度)
     * 这个方法真是多此一举
     */
    protected fun setTextAndProgress(secProgress: Int) {
        val position = getCurrentPositionWhenPlaying()
        val duration = getDuration()
        val progress = position //原本这里还要转换成百分比，有必要么?，多此一举
        setProgressAndTime(progress, secProgress, position, duration)
    }

    //这里的变量我是服的，三个重复获取的,醉了
    private fun setProgressAndTime(progress: Long, secProgress: Int, currentPosition: Long, totalTime: Long) {

        seekBar.max = getDuration().toInt()

        if (progress != 0L) seekBar.progress = progress.toInt()

        if (secProgress > 94) {
            val secProgress = 100
        }
        seekBar.secondaryProgress = secProgress

        tv_total.text = CommonUtil.stringForTime(getDuration().toInt())
        if (getCurrentPositionWhenPlaying() > 0)
            tv_current.text = CommonUtil.stringForTime(getCurrentPositionWhenPlaying().toInt())

        //bottomProgress,可以在这里写
    }

    override fun onPrepared() {
        super.onPrepared()
        if (mCurrentState != STATE_PREPARING) return
        seekBar.max = getDuration().toInt()
        startProgressTimer()
    }

    override fun onBufferingUpdate(percent: Int) {
        if (mCurrentState != STATE_NORMAL && mCurrentState != STATE_PREPARING) {
            if (percent > 0) {
                //这只是缓存进度,有必要吧textView的进度也更新?
                setTextAndProgress(percent)
                //这是我写的
                seekBar.secondaryProgress=(percent/100*getDuration()).toInt()
            }
        }
    }


    private var mMoveY: Int = 0

    private var mFirstTouch: Boolean = false

    private fun touchSurfaceDown(x: Float, y: Float) {
        mDownX = x
        mDownY = y
        mMoveY = 0
        mChangeVolume = false
        mChangePosition = false
        mBrightness = false
        mShowVKey = false
        mFirstTouch = true
    }

    //判断当前行为 是改变- > 音量、亮度、视频进度
    private fun touchMoveFullLogic(absDeltaX: Float, absDeltaY: Float) {

        val curWidth = if (CommonUtil.getCurrentScreenLand(context as Activity)) mScreenHeight else mScreenWidth

        if (absDeltaX > mThreshold || absDeltaY > mThreshold) {
            cancelProgressTimer()
            when {
            //X距离有效
                absDeltaX >= mThreshold -> {
                    //防止全屏虚拟按键
                    val screenWidth = CommonUtil.getScreenWidth(context)
                    //条件:没有碰到虚拟键(假设虚拟键的宽度是mSeekEndOffset)
                    if (Math.abs(screenWidth - mDownX) > mSeekEndOffset) {
                        mChangePosition = true
                        mDownPosition = getCurrentPositionWhenPlaying()
                    } else {
                        mShowVKey = true
                    }
                }
            //Y距离有效
                else -> {
                    //碎碎念：我真是服了，screenHeight拿过了，还得再用另一个方法拿?
                    val isRight = Math.abs(mScreenHeight - mDownY) > mSeekEndOffset
                    //mFirstTouch这个变量有什么用?
                    if (mFirstTouch) {//左边调亮度
                        mBrightness = (mDownX < curWidth * 0.5f) && isRight
                    }
                    if (!mBrightness) {
                        mChangeVolume = isRight
                        mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    }
                    mShowVKey = !isRight
                }
            }
        }
    }

    abstract fun onClickUiToggle()

    abstract fun dismissBrightnessDialog()

    abstract fun dismissVolumeDialog()

    abstract fun dismissProgressDialog()

    abstract fun showBrightnessDialog(screenBrightness: Float)

    abstract fun showVolumeDialog(deltaY: Float, volumePercent: Float)

    abstract fun showProgressDialog(deltaX: Float, seekTime: String?, mSeekTimePosition: Long, totalTime: String?, totalTimeDuration: Long)

    abstract fun showWifiDialog()
}