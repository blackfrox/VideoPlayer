package com.blackfrox.videoplayer.media

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.*
import com.blackfrox.videoplayer.R
import java.util.*

/**
 * Created by Administrator on 2017/11/16 0016.
 */
class MediaController : FrameLayout {

    companion object {
        val SHOW_PROGRESS=1
        val FADE_OUT=2
    }
    //接口的作用可能是为了看起来明显点，和MVP中的V接口一模一样
    private lateinit var mPlayer: MediaPlayerController //作用，这边能使用接口创建的实际方法
    private lateinit var mAnchor: View
    private lateinit var mRoot:View
    private lateinit var mWindowManager: WindowManager
    private var mUseFastForward=false
    private var mFromXml=false
    private var mShowing =false
    private var mDragging=false

    private lateinit var mPauseButton: ImageView
    private lateinit var  mFullButton: ImageView
    private lateinit var  mProgress: SeekBar
    private lateinit var  mCurrentTime: TextView
    private lateinit var  mEndTime: TextView
    private lateinit var  mTitle   : TextView


    private lateinit var mFormatBuilder: StringBuilder
    private lateinit var mFormatter: Formatter
    private lateinit var mWindow: PopupWindow
    private var mAnimStyle=android.R.style.Animation

    private val mHandler= @SuppressLint("HandlerLeak")
    object :Handler(){
        override fun handleMessage(msg: Message) {
            when(msg.what){
                FADE_OUT -> hide()
                SHOW_PROGRESS ->{
                    //进度更新，并返回进度值
                    val pos=setProgress()
                    sendEmptyMessageDelayed(SHOW_PROGRESS, (1000-(pos%1000)).toLong())
                }
            }
            super.handleMessage(msg)
        }
    }

    constructor(context: Context): super(context){
        if (!mFromXml){
            initFloatingWindow()
        }
    }
    //xml中使用
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mRoot=LayoutInflater.from(context).inflate(R.layout.layout_video_player,this,true)
//        mUseFastForward=true
        mFromXml=true
//        LayoutInflater.from(context).inflate(R.layout.layout_video_player,this)

//        主要是一些View获得点击、焦点、文字改变等事件的分发管理，对整个系统的调试、问题定位等，
//        AccessibilityManager.getInstance()

    }

//    @SuppressLint("MissingSuperCall")
//    override fun onFinishInflate() {
////        if (mRoot!=null){
////            initControllerView(mRoot)
////        }
//        super.onFinishInflate()
//        initControllerView(mRoot)
//    }

    //实例化
//    @JvmOverloads constructor(context: Context, useFastForward: Boolean = true) : super(context) {
//        mUseFastForward=useFastForward
////        initFloatingWindowLayout()
//        initFloatingWindow()
////        mRoot=this
////        initControllerView(mRoot)
//    }

    private fun initFloatingWindow(){
        mWindow = PopupWindow(context)
        mWindow.setFocusable(false)
        mWindow.setBackgroundDrawable(null)
        mWindow.setOutsideTouchable(true)
        mAnimStyle = android.R.style.Animation
//        mWindowManager=context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        mWindow=PopupWindow()
//        mWindow.setWidowManager(mWindowManager,null,null)
//        mWindow.requestFeature(Window.FEATURE_NO_TITLE)
//        mDecor=mWindow.getDecorView()
//        mDecor.setOnTouchListener(mTouchListener)
//        mWindow.setContentView(this)
//        mWindow.setBackgroundDrawablesource(android.R.color.transparent)
//
//        //While the media controller is up,the volume control keys should
//        //affect the media stream type
//        mWindow.setVolumeControlStream(AudioManager.STREAM_MUSIC)
//
//        isFocusable=true

    }

//    private val mLayoutChangeListener= OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
//        updateFloatingWindowLayout()
//        if (mShowing){
//            mWindowManager.updateViewLayout(mDecor,mDecorLayoutPrams)
//        }
//    }

    private val mTouchListener= OnTouchListener { v, event ->
        if (event.action==MotionEvent.ACTION_DOWN){
            if (mShowing){
                hide()
            }
        }
        false
    }

    fun setMediaPlayer(player: MediaPlayerController) {
        mPlayer=player
        updatePausePlay()
    }

    /**
     * Set the view that acts as the anchor for the control view.
     * This can for example be a VideoView,or your Activity's main view.
     * When VideoView calls this method,it will use the VideoView's parent
     * as the anchor
     * @param anchorView The view to which to anchor the controller when it is visible
     */
    fun setAnchorView(anchorView: View) {
        mAnchor=anchorView
        if (!mFromXml){
            removeAllViews()
            mRoot=makeControllerView()
            mWindow.contentView=mRoot
            mWindow.width=ViewGroup.LayoutParams.MATCH_PARENT
            mWindow.height=ViewGroup.LayoutParams.WRAP_CONTENT
        }
        //mRoot是从构造方法获取的
        initControllerView(mRoot)
//        //猜: 将videoView的layout和controller进行了同步
//        mAnchor.addOnLayoutChangeListener(mLayoutChangeListener)
//        val frameParams=FrameLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT)
//
//        removeAllViews()
//        val v =makeControllerView()
//        addView(v,frameParams)
    }

    /**
     * Create the view that holds the widgets that control playback.
     * Derived classed can override this to create their own.
     * @return The controller view.
     * @hide This doesn't work as advertised
     */
    private fun makeControllerView(): View {
        val inflater=context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mRoot= inflater.inflate(R.layout.layout_video_player,null)

        initControllerView(mRoot)
        return mRoot
    }

    private fun initControllerView(mRoot: View) {

         mPauseButton=mRoot.findViewById<ImageView>(R.id.iv_pause)
         mFullButton=mRoot.findViewById<ImageView>(R.id.change_screen)
         mProgress=mRoot.findViewById<SeekBar>(R.id.video_seekbar)
         mCurrentTime=mRoot.findViewById<TextView>(R.id.tv_current_time)
         mEndTime=mRoot.findViewById<TextView>(R.id.tv_time)
         mTitle=mRoot.findViewById<TextView>(R.id.tv_title)
//        val res=context.resources
        //play 和pause 的 description
//        mPauseButton.requestFocus()
        mPauseButton.setOnClickListener(mPauseListener)

        if (mProgress is SeekBar){
            val seeker=mProgress as SeekBar
            seeker.setOnSeekBarChangeListener(mSeekListener)
        }
//        mProgress.max=100

        mFormatBuilder=StringBuilder()
        mFormatter=Formatter(mFormatBuilder,Locale.getDefault())
//        installPrevNextListeners()
    }

    fun isShowing()= mShowing

    @SuppressLint("ObsoleteSdkInt")
            /**
     * Show the controller on screen.It will go away
     * automatically after 3 seconds of inactivity
     */
    fun show(timeout: Int=3000){
        if (!mShowing){
            if (mFromXml){
                visibility= View.VISIBLE
            }else{
                val location= IntArray(2)
                mAnchor.getLocationOnScreen(location)
                val anchorRect=Rect(location[0],location[1],
                        location[0]+mAnchor.width,location[1]+mAnchor.height)
                mWindow.animationStyle=mAnimStyle
                mWindow.showAtLocation(mAnchor,Gravity.BOTTOM,
                        anchorRect.left,0)
            }

            requestFocus()

//            disableUnsupportButtons() 没用了，至少我不知道怎么替换
            //作用是当出现未知情况关闭控件的focus
            mShowing=true
        }
        updatePausePlay()

        //cause the progress bar to be updated even if mShowing
        //was ready true.This happens, for example,if we're
        //paused with the progress bar showing the user hits play.
        mHandler.sendEmptyMessage(SHOW_PROGRESS)
        if (timeout!=0){
            with(mHandler){
                removeMessages(FADE_OUT)
                sendEmptyMessageDelayed(FADE_OUT, timeout.toLong())
            }
        }
    }

    /**
     * Remove the controller from the screen
     */
    fun hide(){
        if (mShowing){
            try {
                mHandler.removeMessages(SHOW_PROGRESS)
                if (mFromXml){
                    visibility= View.GONE
                }else{
                    mWindow.dismiss()
                }
            }catch (ex: IllegalArgumentException){
                Log.w("MediaController","already removed")
            }
            mShowing=false
        }
    }

    fun setTitle(name: String){
        mTitle.text=name
    }

    /**
     * 改变控制器的动画风格的资源
     */
    fun setAnimationStyle(animstyle: Int){
        mAnimStyle= animstyle
    }
    private fun stringForTime(timeMs: Int): CharSequence{
        val totalSeconds=timeMs/1000

        val seconds=totalSeconds%60
        val minutes=(totalSeconds/60)%60
        val hours=totalSeconds/3600

        if (hours>0){
            return String.format("%d:%02d:%02d",hours,minutes,seconds)
        }else{
            return String.format("%02d:%02d",minutes,seconds)
        }
    }

    fun setProgress(): Int {
        if (mDragging){
            return 0
        }
        val position=mPlayer.getCurrentPosition()
        val duration=mPlayer.getDuration()
        if (duration>0){
            //use long to avoid overflow
//            val pos=1000*position/duration
//            mProgress.progress=pos
            //PS:这是我自己改的,顺便吧intiView中的mProgress.max注释了
            if (mProgress.max!=duration){
                mProgress.max=duration
            }
            mProgress.progress=position
            val percent=mPlayer.getBufferPercentage()
            mProgress.secondaryProgress=percent
        }
        mCurrentTime.text=stringForTime(position)
        mEndTime.text=stringForTime(duration)
        return position
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
//                context.longToast("点击了mediaController")
//                show(0) //show until hide is called
//                //改为
                if (mShowing){
                    hide()
                }else{
                    show()
                }
            }
//            MotionEvent.ACTION_UP -> {
//                show()//start timeout
//                Log.d("MediaController","touchUp")
//            }
            MotionEvent.ACTION_CANCEL -> hide()
        }
        return true
    }

    override fun onTrackballEvent(event: MotionEvent?): Boolean {
        show()
        return false
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode=event.keyCode
        val uniqueDown=event.repeatCount==0
                  &&event.action==KeyEvent.ACTION_DOWN
        when(keyCode){
            KeyEvent.KEYCODE_HEADSETHOOK or
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE or
                    KeyEvent.KEYCODE_SPACE ->{
                if(uniqueDown){
                    doPauseResume()
                    show()
                    mPauseButton.requestFocus()
                }
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY ->{
                if (uniqueDown&&!mPlayer.isPlaying()){
                    mPlayer.start()
                    updatePausePlay()
                    show()
                }
                return true
            }
            KeyEvent.KEYCODE_MEDIA_STOP or
                    KeyEvent.KEYCODE_MEDIA_PAUSE ->{
                if (uniqueDown&&mPlayer.isPlaying()){
                    mPlayer.pause()
                    updatePausePlay()
                    show()
                }
                return true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN or
                    KeyEvent.KEYCODE_VOLUME_UP or
                    KeyEvent.KEYCODE_MUTE or
                    KeyEvent.KEYCODE_CAMERA ->{
                //don't show the controls for volume adjustment
                return super.dispatchKeyEvent(event)
            }
            KeyEvent.KEYCODE_BACK or KeyEvent.KEYCODE_MENU->{
                if (uniqueDown){
                    hide()
                }
                return true
            }
        }
        show()
        return super.dispatchKeyEvent(event)
    }
    private val mPauseListener= OnClickListener {
        doPauseResume()
        show()
    }

    private fun updatePausePlay() {

        if (mPlayer.isPlaying()){
            mPauseButton.setImageResource(R.drawable.bili_player_play_can_pause)
        }else{
            mPauseButton.setImageResource(R.drawable.bili_player_play_can_play)
        }
    }

    private fun doPauseResume() {
        if (mPlayer.isPlaying()){
            mPlayer.pause()
        }else{
            mPlayer.start()
        }
        updatePausePlay()
    }

    //Three are two scenarious that can trigger the seekbar listener to trigger;
    //
    //The first is the user using the touchpad to adjust the position of the
    //seekbar'thumb.In this case onStartTrackingTouch is called followed by
    //a number of onProgressChanged notifications,concluded by onStopTrackingTouch.
    //We're setting the field "mDragging" to true for the duration of the dragging
    //session to avoid jumps in the position in case of ongoing playback.
    //
    //The second scenario involves the user operating the scroll ball,in this
    //case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    //we will simply apply the updated position without suspending regular updates.
    private val mSeekListener=object :SeekBar.OnSeekBarChangeListener{
        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            show(360000)
            mDragging=true

            //By removing these pending progress messages we can sure
            //that a) we won't update the progress while the user adjusts
            //the seekbar and b) once the user is done dragging the thumb
            //we will post one of thes messages to the queue again and
            //this ensures that these will be exactly one message queued up.
            mHandler.removeMessages(SHOW_PROGRESS)
        }
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (!fromUser){
                //We're not interested in programmatically generated changes
                //the progress bar's position
                return
            }

//            val duration=mPlayer.getDuration()
//            val newPosit
            mPlayer.seekTo(progress)
            mCurrentTime.text=stringForTime(progress)
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            mDragging=false
            setProgress()
            updatePausePlay()
            show()

            //Ensure that progress is properly updated in the future,
            //the call to show() dose not guarantee this because it is a
            //no-op if we are already showing.
            mHandler.sendEmptyMessage(SHOW_PROGRESS)
        }
    }

    override fun setEnabled(enabled: Boolean){
        mPauseButton.isEnabled=enabled
        mProgress.isEnabled=enabled
        disableUnSupportedButtons()
        super.setEnabled(enabled)
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked .
     * This requires the control interface to be a MediaPlayerControlExt
     * 因为在CustomVideoView中有个方法失效了，所以判断不了
     */
    private fun disableUnSupportedButtons() {
//        try {
//        if (!mPlayer.canPause()){
//            mPauseButton.isEnabled=false
//        }
//    }
    }

    override fun getAccessibilityClassName(): CharSequence {
        return MediaController::class.java.name
    }




//    override fun onConfigurationChanged(newConfig: Configuration?) {
//        super.onConfigurationChanged(newConfig)
//        when(resources.configuration.orientation){
//        //横屏
//            Configuration.ORIENTATION_LANDSCAPE ->{
//                isFullScreen=true
//                setVideoScale(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
//                with(mActivity.window){
//                    clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
//                    addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
//                }
//            }
//            else ->{
//                isFullScreen=false
//                setVideoScale(ViewGroup.LayoutParams.MATCH_PARENT,DisplayUtil.dp2px(context,240F))
//                with(mActivity.window){
//                    clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
//                    addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
//                }
//            }
//        }
//    }

    interface MediaPlayerController {

        fun getDuration(): Int

        fun getCurrentPosition(): Int

        fun isPlaying(): Boolean

        fun getBufferPercentage(): Int
        fun start()

        fun pause()

        fun resume()
        fun seekTo(pos: Int)

        //因为方法找不到了，所以不写了
//    fun canPause(): Boolean

    }
}
