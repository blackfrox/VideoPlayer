package com.blackfrox.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.support.v7.app.ActionBar
import android.support.v7.widget.ContentFrameLayout
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import com.blackfrox.player.ijkMedia.IjkVideoView
import android.widget.ImageView
import android.util.Log
import android.view.*
import android.widget.SeekBar
import kotlinx.android.synthetic.main.video_media_controller.view.*
import java.util.*


/**
 * Created by Administrator on 2018/3/7 0007.
 * 手势处理， 控制层的显示和隐藏
 *
 * 1 竖屏显示 和横屏显示
 */
class MPlayer @JvmOverloads constructor(context: Context, attributeSet: AttributeSet?=null, def: Int=0)
    : IjkVideoView(context,attributeSet,def), View.OnTouchListener {

    private var TAG="MPlayer"
    protected val imageView by lazy { ImageView(context) }

//    var sDefaultTimeout=3000L

                         /*可供外部调用的变量值*/
    //actionBar必须是toolBar，并且player必须在toolBarLayout里布局
    //不然会很难看，还不如不用
    var toolBar: Toolbar ?=null
    set(value) {
        field=value
        if (isShowing)
            field?.visibility= View.VISIBLE
        else field?.visibility= View.GONE
    }
    var isShowing = false
                         /*可供外部调用的变量值*/


    //拖动ing
    private var mDragging = false
    //    There are two scenarios that can trigger the seekbar listener to trigger :
//
//    The first is the user using the touchpad to adjust the position of the
//    seekbar's thumb.In this case onStartTrackingTouch is called followed by
//    a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
//    We're setting the field "mDragging" to true for the duration of the dragging
//    session to avoid jumps in the position in case of ongoing playback.
//
//    The second scenario involves the user operatring the scroll ball, in this
//    case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
//    we will simply apply the updated position without suspending regular updates.
     val mSeekListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            show(3600000)

            mDragging = true

//        By removing these pending progress messages we make sure
//        that a) we won't update the progress while the user adjusts
//        the seekbar and b) once the user is done dragging the thumb
//        we will post one of these messages to the queue again and
//        this ensures that there will be exactly one message queued up.
            removeCallbacks(mShowProgress)
        }

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (!fromUser)
//            We're not interested in programmatically generated changes to
//            the progress bar's position.
                return

            val newPosition = (getDuration() * progress / 1000L)
            tv_current.text = stringForTime(newPosition)
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            mDragging = false
            val newPosition = (getDuration() * seekBar.progress / 1000L)
            seekTo(newPosition)
            setProgress()
            updatePausePlay()
            show()

//        Ensure that progress is properly updated in the future,
//        the call to show() does not guarantee this because it is a
//        no-op if we are already showing.
            post(mShowProgress)
        }
    }

    //是否全屏
    protected var isFullScreen = false
    private var newPosition =0L //滑动之后的当前进度
    protected val mAudioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    protected var activity: Activity
    init {

        if (context is Activity){
           activity=context
        } else throw Exception("The context must be Activity")

        LayoutInflater.from(context)
                .inflate(R.layout.video_media_controller,this)

        img_pause.setOnClickListener {
            doPauseResume()
            //解决: 不知道为什么图标会失灵，所以将下面的方法延迟了
            postDelayed({show()},300)
        }


        //解决: seekBar与滑动事件冲突
        seekBar.setOnTouchListener(this)
        rl_video.setOnTouchListener(this)

        img_fullScreen.setOnClickListener {
            //横屏的时候
            if (resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE){
                //变成竖屏
                context.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                context.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_USER
            }else{
                context.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        isFullScreen = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        //TODO 画面旋转的时候需要一秒，体验不好
        if (!isFullScreen){
            layoutParams.width=initWidth
            layoutParams.height=initHeight
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }else{
            layoutParams.width = LayoutParams.MATCH_PARENT
            layoutParams.height =LayoutParams.MATCH_PARENT
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        if(isShowing) show()
        super.onConfigurationChanged(newConfig)
    }

    constructor(context: Context,isFullScreen: Boolean): this(context){
        this@MPlayer.isFullScreen =isFullScreen
    }


    /**
     * 双击和点击有冲突(已解决，使用onSingleConfirm替代onSingerTap)
     */
    private var mFingerBehavior =-1
    private val FINGER_BEHAVIOR_PROGRESS =1
    private val FINGER_BEHAVIOR_BRIGHTNESS=2
    private val FINGER_BEHAVIOR_VOLUME =3
    /**
     * 亮度，进度音频
     */
    protected val gestureDetector=GestureDetector(context,object :GestureDetector.SimpleOnGestureListener() {
        //使用这个解决点击双击冲突 //怪不得之前还是有问题，原来是双击方法里我的代码有问题
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            if (mFingerBehavior<0)
                toggleMediaControlsVisiblity()
            return super.onSingleTapConfirmed(e)
        }

        //双击
        override fun onDoubleTap(e: MotionEvent?): Boolean {
            doPauseResume()
            return true
        }

        override fun onDown(e: MotionEvent): Boolean {
            //重置  手指行为
            mFingerBehavior = -1
            return super.onDown(e)
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (width<=0||height<=0) return false

            /**
             * 根据手势起始2个点断言 后续行为，规则如下:
             *   屏幕切分为:
             *     1.左右扇形区域为视频进度调节
             *     2.上下扇形区域  左半屏亮度调节  右半屏音量调节
             */
            if(mFingerBehavior<0){
                val moveX=e2.x-e1.x
                val moveY=e2.y-e1.y
                //如果横向滑动距离大于纵向滑动距离，则认为在调节速度
                if (Math.abs(distanceX)>=Math.abs(distanceY))
                    mFingerBehavior= FINGER_BEHAVIOR_PROGRESS
                //否则为调节音量或亮度
                //按下位置在屏幕左半边，则是调节亮度
                else if (e1.x<=width/2)
                    mFingerBehavior=FINGER_BEHAVIOR_BRIGHTNESS
                else
                    mFingerBehavior=FINGER_BEHAVIOR_VOLUME
            }

            when(mFingerBehavior){
                FINGER_BEHAVIOR_PROGRESS ->{ //进度变化         (ps: 日你哥，参考的代码都不能用，草，还不如自己写呢)
                    //滑动width长度的距离设定为76秒
                    val moveX=e2.x-e1.x
                    val scrollTime=(moveX/width*76000)
                    newPosition=(getCurrentPosition()+scrollTime).toLong()
//                    Log.d(TAG,"onScroll: $moveX")
//                    Log.d(TAG,"onScroll : width: $width")
                    val progress=newPosition*1000/(if (getDuration()==0L) 1 else getDuration())
                    seekBar.progress= progress.toInt()
                    showProgressDialog(distanceX,newPosition)
                }
                FINGER_BEHAVIOR_VOLUME ->{//音量变化
                    val max=mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val currentVolume=mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    var progress=currentVolume+distanceY/height*max
                    //控制调节临界范围
                    if(progress<=0) progress= 0F
                    if(progress>=max) progress=max.toFloat()

                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,Math.round(progress),0)
                    showVolumeDialog(distanceY,progress)

                }
                FINGER_BEHAVIOR_BRIGHTNESS ->{
                    val percent=(distanceY/height)
                    //吊吊吊,三个方法里处理逻辑各个不一样
                    var curBrightness=(context as Activity).window.attributes.screenBrightness
                    if (curBrightness<=0.0f)
                        curBrightness=0.50f
                    else if (curBrightness<0.01f)
                        curBrightness=0.01f
                    val lpa=(context as Activity).window.attributes
                    lpa.screenBrightness=curBrightness+percent
                    if (lpa.screenBrightness>1.0f)
                        lpa.screenBrightness=1.0f
                    else if (lpa.screenBrightness<0.01f)
                        lpa.screenBrightness=0.01f
                    (context as Activity).window.attributes=lpa

                    val brightnessPercent=lpa.screenBrightness*100
                    showBrightnessDialog(brightnessPercent)
                }
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }

    })

    //为什么return false 才是拦截事件，而不是return true
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when(v?.id){
            R.id.seekBar ->{
                //竖屏不拦截滑动事件，横屏拦截
                if (!isFullScreen)
                    return true
                //为什么别人的代码，我抄写就有问题，不起作用???
                //最后还是得自己写
                seekBar.setOnSeekBarChangeListener(mSeekListener)
                return false
            }
            //滑动事件
            R.id.rl_video ->{
                gestureDetector.onTouchEvent(event)
                when(event?.action){
                    MotionEvent.ACTION_UP ->{
                        dismissProgressDialog()
                        dismissVolumeDialog()
                        dismissBrightnessDialog()

                        //因为音量和亮度都是实时调节的，而进度是只有在手指抬起的时候才设置
                        when(mFingerBehavior){
                            FINGER_BEHAVIOR_PROGRESS ->{
//                        Log.d(TAG,"up:::${getCurrentPosition()}")
                                seekTo(newPosition)
                            }
                        }
                    }
                }
                return true
            }
        }
        return true
    }

    private fun toggleMediaControlsVisiblity() {
        if (isShowing){
            hide()
        }else{
            show()
        }
    }

    //TODO：视频刚开始播放的时候， 播放图标显示不正确
    //timeout的默认值：sDefaultTime写在ijkVideoView里了
    override fun show(timeout: Long) {
        if (!isShowing){
            setProgress()

            //根据横竖屏，显示不同的ui
            // 1 竖屏     显示 toolBar 和 下controller
            // 2 横屏     显示 上controller 和 下controller
            //TODO 横竖屏状态下  控制Ui需要写两个 因为横屏状态的操作功能会增多
            ll_bottom.visibility= View.VISIBLE
            if (!isFullScreen){
                toolBar?.visibility= View.VISIBLE
                ll_top.visibility=GONE
            }else{
                toolBar?.visibility= View.GONE
                ll_top.visibility= View.VISIBLE
            }

            isShowing=true
        }

        updatePausePlay()

        //cause the progress bar to be updated event if mShowing
        //was already true. This happens, for example, if we're
        //paused with the progress bar showing the user hits play.
        post(mShowProgress)

        if (timeout!=0L){
            removeCallbacks(mFadeOut)
            postDelayed(mFadeOut,timeout)
        }
    }

    override fun hide(){
        if (isShowing){
            try {
                removeCallbacks(mShowProgress)
                ll_top.visibility= View.GONE
                ll_bottom.visibility= View.GONE

                //只有在竖屏并且正在播放时隐藏actionBar，否则没有必要
                if(isPlaying()&&!isFullScreen) toolBar?.visibility= View.GONE
            }catch (e: IllegalArgumentException){
                Log.d(TAG,"alread removed")
            }
            isShowing=false
        }
    }

    private val mFadeOut= Runnable {
        hide()
    }

    private val mShowProgress=object : Runnable{
        override fun run() {
            val pos=setProgress()
            if (!mDragging && isShowing && isPlaying())
                postDelayed(this, (1000-(pos%1000)).toLong())
        }
    }

    private fun stringForTime(timeMs: Long): String {
        val totalSeconds =timeMs/1000

        val seconds = totalSeconds % 60
        val minutes=(totalSeconds/60) %60
        val hours=totalSeconds/3600

        if (hours>0){
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes,
                    seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    private fun setProgress(): Long {
        if (mMediaPlayer==null || mDragging)
            return 0
        if (getDuration()>0){
            //use long to avoid overflow
            val pos=1000L * getCurrentPosition() /getDuration()
            seekBar.progress= pos.toInt()
        }
        seekBar.secondaryProgress=getBufferPercentage()

        tv_current.text=stringForTime(getCurrentPosition())
        tv_total.text=stringForTime(getDuration())
        return getCurrentPosition()
    }

    override fun onTrackballEvent(ev: MotionEvent): Boolean {
        show(sDefaultTimeout)
        return false
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val uniqueDown = event.repeatCount == 0 && event.action == KeyEvent.ACTION_DOWN
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume()
                show(sDefaultTimeout)
                if (img_pause != null) {
                    img_pause.requestFocus()
                }
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !isPlaying()) {
                start()
                updatePausePlay()
                show(sDefaultTimeout)
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && isPlaying()) {
                pause()
                updatePausePlay()
                show(sDefaultTimeout)
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
                || keyCode == KeyEvent.KEYCODE_CAMERA) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event)
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide()
            }
            return true
        }

        show(sDefaultTimeout)
        return super.dispatchKeyEvent(event)
    }

    private fun doPauseResume(){
        if (isPlaying()){
            pause()
            img_pause.setImageResource( R.drawable.bili_player_play_can_play)
        }else{
            start()
            img_pause.setImageResource( R.drawable.bili_player_play_can_pause)
        }
    }

    private fun updatePausePlay() {
        if (isPlaying()){
            img_pause.setImageResource( R.drawable.bili_player_play_can_pause)
        }else{
            img_pause.setImageResource(R.drawable.bili_player_play_can_play)
        }
    }

    override fun start() {
        super.start()
    }

    ////////////////////
    private var isPaused =false
    fun onPause(){
        if(isPlaying()){
            pause()
            isPaused=true
//            showPauseBitmap()
        }
    }

    fun onResume(){
        if (isPaused){
            isPaused=false
//            hidePauseBitmap()
            start()
        }
    }

    private fun hidePauseBitmap() {
        imageView.visibility=GONE
    }

    fun showPauseBitmap(){
        if (imageView.visibility==GONE)
            imageView.visibility= View.VISIBLE

        //下面的方面截图只能用在本地文件上
        /**
         * MediaMetadataRetriever class provides a unified interface for retrieving
         * frame and meta data from an input media file.
         */
//        if (imageView.visibility==GONE)
//           imageView.visibility= View.VISIBLE
//        val mmr = MediaMetadataRetriever()
//        mmr.setDataSource(mUri!!.toString())
//
//        val bitmap = mmr.getFrameAtTime(if (getCurrentPosition()==0) -1 else getCurrentPosition().toLong())//获取第一帧图片
//        imageView.setImageBitmap(bitmap)
//        mmr.release()//释放资源
    }

            /*           需要实现           */
     fun showBrightnessDialog(brightnessPercent: Float){}

     fun showVolumeDialog(deltaY: Float, volumePercent: Float){}

     fun showProgressDialog(deltaX: Float, newPosition: Long){}

     fun dismissBrightnessDialog(){}

     fun dismissVolumeDialog(){}

     fun dismissProgressDialog(){}
}