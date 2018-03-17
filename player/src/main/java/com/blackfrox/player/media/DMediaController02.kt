package com.blackfrox.player.media

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.*
import android.widget.MediaController
import com.blackfrox.player.R
import java.util.*

/**
 * Created by Administrator on 2018/1/8 0008.
 *
 *
 */
class DMediaController02 :FrameLayout,MediaController.MediaPlayerControl{


    companion object {
        val TAG="DMediaController02"
        val FADE_OUT=1
        val SHOW_PROGRESS=2
    }
    private lateinit var mPlayer: MediaController.MediaPlayerControl
    private lateinit var mWindow: PopupWindow
    private lateinit var mTitleView:TextView
    private lateinit var mPauseButton: ImageView
    private lateinit var mCurrentTime:TextView
    private lateinit var mEndTime: TextView
    private lateinit var mProgress: SeekBar
    private lateinit var mTitle: String
    private lateinit var mAnchor: VideoView

    private lateinit var mRoot: View

    private var mFromXml=false
    private var mDragging=false
    private var mShowing=false
    private var mAnimStyle=android.R.style.Animation

    private val mAm by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val mHandler= @SuppressLint("HandlerLeak")
    object : Handler(){
        override fun handleMessage(msg: Message?) {
            when(msg?.what){
                FADE_OUT -> hide()
                SHOW_PROGRESS ->{
                    val pos=setProgress()
                    if (!mDragging&&mShowing){
                        val message=obtainMessage(SHOW_PROGRESS)
                        sendMessageDelayed(message, (1000-(pos%1000)).toLong())
                        updatePausePlay()
                    }
                }
            }
        }
    }

    constructor(context: Context,attributeSet: AttributeSet):super(context,attributeSet){
        mRoot=this
        mFromXml=true
    }

    constructor(context: Context):super(context){
        if (!mFromXml){
            initFloatingWindow()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        initControllerView(mRoot)
    }

    private fun initFloatingWindow() {
        mWindow= PopupWindow(context)
        with(mWindow){
            isFocusable=false
            setBackgroundDrawable(null)
            isOutsideTouchable=true
        }
    }

    /**
     * 设置VideoView
     */
    fun setAnchorView(view: VideoView){
        mAnchor=view
        if (!mFromXml){
            removeAllViews()
            mRoot=makeControllerView()
            with(mWindow){
                contentView=mRoot
                width=LayoutParams.MATCH_PARENT
                height=LayoutParams.WRAP_CONTENT
            }
            initControllerView(mRoot)
        }

    }

    private fun makeControllerView(): View {
        return (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
                .inflate(R.layout.video_media_controller,this)
    }

    private fun initControllerView(view: View) {
        with(view){
            mPauseButton=findViewById<ImageView>(R.id.img_pause)
            mPauseButton.setOnClickListener{
                doPauseResume()
                show()
            }

            mProgress=findViewById<SeekBar>(R.id.seekBar)
            with(mProgress){
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    }

                })
                thumbOffset=1
                max=1000
            }

             mEndTime=findViewById<TextView>(R.id.tv_total)
             mCurrentTime=findViewById<TextView>(R.id.mSeekCurProgress)
             mTitleView=findViewById<TextView>(R.id.tv_title)
//            mTitleView.text=mTitle

            val mBack=findViewById<ImageView>(R.id.img_back)

        }

        mAnchor.setOnClickListener {
            if (mShowing){
                Log.d(TAG,"click hide")
                hide()
            }else{
                show()
                Log.d(TAG,"click show")
            }
        }
    }


    /**
     * 设置播放的文件名称
     */
    fun setTitle(name: String){
        mTitle=name
        mTitleView.text=mTitle
    }

    /**
     * 改变控制器的动画风格的资源
     */
    fun setAnimationStyle(animationStyle: Int){
        mAnimStyle=animationStyle
    }


    /**
     * 在屏幕上显示控制器
     */
    @SuppressLint("ObsoleteSdkInt")
    fun show(timeOut: Int=3000){
        if (!mShowing&&mAnchor.windowToken!=null){
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH){
                mAnchor.systemUiVisibility=View.SYSTEM_UI_FLAG_VISIBLE
            }
            mPauseButton.requestFocus()
//            disableUnSupportedButtons()

            if (mFromXml){
                visibility= View.VISIBLE
            }else{
                val location=IntArray(2)
                mAnchor.getLocationOnScreen(location)
                val anchorRect=Rect(location[0],location[1],
                        location[0]+mAnchor.width,location[1]+mAnchor.height)
                mWindow.animationStyle=mAnimStyle
                mWindow.showAtLocation(mAnchor,Gravity.BOTTOM,
                        anchorRect.left,0)
            }
            mShowing=true
            updatePausePlay()
            if (timeOut!=0){
                mHandler.removeMessages(FADE_OUT)
                mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT), timeOut.toLong())
            }
        }
    }

    fun isShowing()=mShowing

    @SuppressLint("ObsoleteSdkInt")
    fun hide(){

        if (mShowing){
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH){
                mAnchor.systemUiVisibility=View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            }
            try {
                mHandler.removeMessages(SHOW_PROGRESS)
                if (mFromXml){
                    visibility= View.GONE
                }else{
                    mWindow.dismiss()
                }
            }catch (e: IllegalArgumentException){
                e.printStackTrace()
            }
            mShowing=false

        }
    }

    fun setProgress(): Int {
        if (mDragging)
            return 0

        val position=mAnchor.currentPosition
        val duration=mAnchor.duration
        if (duration>0){
            val pos=1000*position/duration
            mProgress.progress=pos
        }
        val percent=mAnchor.bufferPercentage
        mProgress.setSecondaryProgress(percent)

        mEndTime.text=generateTime(duration)
        mCurrentTime.text=generateTime(position)

        return position
    }

    private fun generateTime(position: Int): CharSequence? {
        val seconds=position%60
        val minutes=(position/60)%60
        val hours=position/3600

        if (hours>0){
            return String.format(Locale.US,"%02d:%02d:%02d",hours,minutes,seconds)
        }else{
            return String.format(Locale.US,"%02d:%02d",minutes,seconds)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        show()
        return true
    }

    override fun onTrackballEvent(event: MotionEvent?): Boolean {
        show()
        return false
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        val keyCode=event?.keyCode
        if (event?.repeatCount==0){
          when(keyCode){
              KeyEvent.KEYCODE_HEADSETHOOK,
                      KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                      KeyEvent.KEYCODE_SPACE ->{
                  doPauseResume()
                  show()
                  mPauseButton.requestFocus()
                  return true
              }
              KeyEvent.KEYCODE_MEDIA_STOP ->{
                  if (mAnchor.isPlaying){
                      mAnchor.pause()
                      updatePausePlay()
                  }
                  return true
              }

          }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun updatePausePlay() {
        if (mAnchor.isPlaying){
            mPauseButton.setImageResource(R.drawable.bili_player_play_can_pause)
        }else{
            mPauseButton.setImageResource(R.drawable.bili_player_play_can_play)
        }
    }

    private fun doPauseResume(){
        if (mAnchor.isPlaying){
            mAnchor.pause()
        }else{
            mAnchor.start()
        }
        updatePausePlay()
    }
    override fun isPlaying(): Boolean {
        return mAnchor.isPlaying
    }

    override fun canSeekForward(): Boolean {
       return  false
    }

    override fun getDuration(): Int {
        return mAnchor.duration
    }

    override fun pause() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBufferPercentage(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun seekTo(mesc: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCurrentPosition(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canSeekBackward(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun start() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAudioSessionId(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canPause(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}