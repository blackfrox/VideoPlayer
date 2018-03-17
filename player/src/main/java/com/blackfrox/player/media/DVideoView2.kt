package com.blackfrox.player.media

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.support.v7.app.AlertDialog
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.TextView
import com.blackfrox.player.R
import java.io.IOException

/**
 * Created by Administrator on 2017/11/24 0024.
 *
 * 自定义VideoView
 */
class DVideoView2 @JvmOverloads constructor(context: Context, attributeSet: AttributeSet?=null, def: Int=0)
    : FrameLayout(context,attributeSet,def),MediaController.MediaPlayerControl,SurfaceHolder.Callback {
    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            mSurfaceWidth=width
            mSurfaceHeight=height
            val isValidState=(mTargetState== STATE_PLAYING)
            val hasValidSize=(mVideoWidth==width&&mVideoHeight==height)
            if (mMediaPlayer!=null&&isValidState&&hasValidSize){
                if (mSeekWhenPrepared!=0){
                    seekTo(mSeekWhenPrepared)
                }
                start()
            }
            Log.d("DVideoView2","changed")
        }
        //第一次创建时调用，
        // 从后台恢复到前台也会调用
        override fun surfaceCreated(holder: SurfaceHolder?) {
            mSurfaceHolder=holder
            openVideo()

            Log.d("DVideoView2","created")
        }
        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            //after we return from this we can't use the surface any more
            mSurfaceHolder=null
            mMediaController?.hide()
            release(true)
            Log.d("DVideoView2","destroy")
        }

    override fun canSeekForward(): Boolean {
        return true
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun getAudioSessionId(): Int {
        return 0
    }

    override fun canPause(): Boolean {
        return true
    }

    private val TAG="DVideoView2"

    //settable by the client
    private var mUri: Uri?=null
    companion object {
        //all possible internal states
        private val STATE_ERROR = -1
        private val STATE_IDLE = 0
        private val STATE_PREPARING = 1
        private val STATE_PREPARED = 2
        private val STATE_PLAYING = 3
        private val STATE_PAUSED = 4
        private val STATE_PLAYBACK_COMPLETED = 5
    }

    //    mCurrentState is a VideoView object's current state.
    // mTargetState is the state that a method caller intends to reach.
    // For instance, regardless the VideoView object's current state,
    // calling pause() intends to bring the object to a target state
    // of STATE_PAUSED.
    //mCurrentState 是videoView的当前状态
    //mTargetState是调用的方法的状态
    private var mCurrentState = STATE_IDLE
    private var mTargetState = STATE_IDLE

    //All the stuff we need for playing and showing a video
    private var mSurfaceHolder:SurfaceHolder?=null
    private var mMediaPlayer: MediaPlayer?=null
    private val  surfaceView: SurfaceView by lazy { SurfaceView(context) }
    private var mVideoWidth: Int=0
    private var mVideoHeight:Int=0
    private var mSurfaceWidth: Int=0
    private var mSurfaceHeight: Int=0
    private var mVideoRotationDegree: Int=0
    private var mMediaController: DMediaControl?=null
    private var mCurrentBufferPercentage: Int=0
    private var mOnInfoListener: MediaPlayer.OnInfoListener?=null
    private var mSeekWhenPrepared: Int=0 //recording the seek position while preparing

    private var mAppContext: Context
//    private var mVideoSarNum: Int=0
//    private var mVideoSarDen: Int=0


    private var mPrepareStartTime=0L
    private var mPrepareEndTime=0L

    private var mSeekStartTime=0L
    private var mSeekEndTime=0L

    private lateinit var subtitleDisplay: TextView

//    private val mSHCallback=object : SurfaceHolder.Callback{
//        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
//            mSurfaceWidth=width
//            mSurfaceHeight=height
//            val isValidState=(mTargetState==STATE_PLAYING)
//            val hasValidSize=(mVideoWidth==width&&mVideoHeight==height)
//            if (mMediaPlayer!=null&&isValidState&&hasValidSize){
//                if (mSeekWhenPrepared!=0){
//                    seekTo(mSeekWhenPrepared)
//                }
//                start()
//            }
//            Log.d("DVideoView2","changed")
//        }
//        //第一次创建时调用，
//        // 从后台恢复到前台也会调用
//        override fun surfaceCreated(holder: SurfaceHolder?) {
//            mSurfaceHolder=holder
//            openVideo()
//
//            Log.d("DVideoView2","created")
//        }
//        override fun surfaceDestroyed(holder: SurfaceHolder?) {
//            //after we return from this we can't use the surface any more
//            mSurfaceHolder=null
//            mMediaController?.hide()
//            release(true)
//            Log.d("DVideoView2","destroy")
//        }
//    }


    init {
        mAppContext=context.applicationContext

        //1
        //将一个surfaceView或者textureView添加到当前view中
//        initSurfaceView()
//        Log.d(TAG,"${if (mSurfaceHolder!=null) "is not null" else " is null"}")
        addSurfaceView(context)
        //2
        //不知道为什么还是不能播放，黑屏
//        surfaceView=this@DVideoView2
//        holder.addCallback(mSHCallback)
//        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        mVideoWidth=0
        mVideoHeight=0

        isFocusable=true
        isFocusableInTouchMode=true
        requestFocus()
        mCurrentState= STATE_IDLE
        mTargetState= STATE_IDLE

    }

    private fun addSurfaceView(context: Context) {
//        surfaceView = SurfaceView(context)
          surfaceView.holder.addCallback(this)
//        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
//            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
//                mSurfaceWidth = width
//                mSurfaceHeight = height
//                val isValidState = (mTargetState == STATE_PLAYING)
//                val hasValidSize = (mVideoWidth == width && mVideoHeight == height)
//                if (mMediaPlayer != null && isValidState && hasValidSize) {
//                    if (mSeekWhenPrepared != 0) {
//                        seekTo(mSeekWhenPrepared)
//                    }
//                    start()
//                }
//                Log.d("DVideoView2", "changed")
//            }
//
//            //第一次创建时调用，
//            // 从后台恢复到前台也会调用
//            override fun surfaceCreated(holder: SurfaceHolder) {
//                mSurfaceHolder = holder
//                openVideo()
//                Log.d("DVideoView2", "created")
//            }
//
//            override fun surfaceDestroyed(holder: SurfaceHolder?) {
//                //after we return from this we can't use the surface any more
//                mSurfaceHolder = null
//                mMediaController?.hide()
//                release(true)
//                Log.d("DVideoView2", "destroy")
//            }
//        })
        val lp = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
                Gravity.CENTER)
        surfaceView!!.layoutParams = lp
        addView(surfaceView)
    }

//    private fun initRenders() {
//        val surfaceView
//        setRenderView(renderView)
//    }

    fun initSurfaceView(){
//        if (mRenderView!=null){
//            if (mMediaPlayer!=null)
//                mMediaPlayer!!.setDisplay(null)
//
//            val renderUIView= mRenderView!!.getView()
//            mRenderView!!.removeRenderCallback(mSHCallback)
//            mRenderView=null
//            removeView(renderUIView)
//        }
//
//        if (renderView==null)
//            return

//        surfaceView=SurfaceView(context)
////        surfaceView.setAspectRatio(mCurrentAspectRatio)
////        if (mVideoWidth>0&&mVideoHeight>0)
////            renderView.setVideoSize(mVideoWidth,mVideoHeight)
////        if (mVideoSarDen>0&&mVideoSarNum>0)
////            renderView.setVideoSampleAspectRatio(mVideoSarNum,mVideoSarDen)
//
////        val renderUIView= mRenderView!!.getView() //getView() 是一个单例
//        val lp=LayoutParams(
//                LayoutParams.WRAP_CONTENT,
//                LayoutParams.WRAP_CONTENT,
//                Gravity.CENTER)
//        surfaceView!!.layoutParams=lp
//        addView(surfaceView)
//        surfaceView!!.holder.addCallback(mSHCallback)
//        mRenderView!!.addRenderCallback(mSHCallback)
//        mRenderView!!.setVideoRotation(mVideoRotationDegree)
    }

    /**
     * 根据render类型 创建surfaceView或者TextureView
     * 并且添加到当前view中
     */


    /**
     * Sets video path
     */
    fun setVideoPath(path: String){
        setVideoURI(Uri.parse(path))
    }

    fun setVideoURI(uri: Uri){
        mUri=uri
        mSeekWhenPrepared=0

        openVideo()
        requestLayout()
        invalidate()
    }

    fun stopPlayback(){
        if (mMediaPlayer!=null){
            mMediaPlayer!!.stop()
            mMediaPlayer!!.release()
            mMediaPlayer=null
//            if (mHudViewHolder!=null)
//                mHudViewHolder.setMediaPlayer(null)
            mCurrentState= STATE_IDLE
            mTargetState= STATE_IDLE
            val am=mAppContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.abandonAudioFocus(null)
        }
    }


    private fun openVideo(){
        if (mUri==null||mSurfaceHolder==null){
            //not ready for playback just yet,will try again later
            return
        }
        //we shouldn't clear the target state,because somebody might
        //called start() previously
        release(false)
//        val am=mAppContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        am.requestAudioFocus(null,AudioManager.STREAM_MUSIC,AudioManager.AUDIO_SESSION_ID_GENERATE)

        try {
//            mMediaPlayer=createPlayer()
            mMediaPlayer= MediaPlayer()
            with(mMediaPlayer!!){
                setOnPreparedListener(mPreparedListener)
                //将holder设置video的宽高
                setOnVideoSizeChangedListener(mSizeChangedListener)
                setOnCompletionListener(mCompletionListener)
                setOnErrorListener(mErrorListener)
                setOnInfoListener(mInfoListener)
                setOnBufferingUpdateListener(mBufferingUpdateListener)
                setOnSeekCompleteListener(mSeekCompleteListener)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    setOnTimedTextListener(mOnTimeTextListener)
                }

                mCurrentBufferPercentage=0
               setDataSource(context,mUri)
                setDisplay(mSurfaceHolder)
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                setScreenOnWhilePlaying(true)
//                mPrepareStartTime=System.currentTimeMillis()
               prepareAsync()
            }
            //we don't set the target state here either,but preserve the
            //target state that was there before
            mCurrentState= STATE_PREPARING
            attachMediaController()
        }catch (e: IOException){
            mCurrentState= STATE_ERROR
            mTargetState= STATE_ERROR
            return
            mOnErrorListener?.onError(mMediaPlayer,MediaPlayer.MEDIA_ERROR_UNKNOWN,0)
        }catch (e: IllegalArgumentException){
            mCurrentState= STATE_ERROR
            mTargetState= STATE_ERROR
            return
            mOnErrorListener?.onError(mMediaPlayer,MediaPlayer.MEDIA_ERROR_UNKNOWN,0)
        }
    }

    fun setMediaController(controller: DMediaControl){
        mMediaController?.hide()

        mMediaController=controller
        attachMediaController()
    }

    //mediaController传递mMediaplayer和当前view，
    // 根据当前mMediaplayer状态设置enable
    private fun attachMediaController() {
        if (mMediaPlayer!=null&&mMediaController!=null){
            mMediaController!!.setMediaPlayer(this)
            val anchorView=if (parent is View)
                parent as View else this
            mMediaController!!.setAnchorView(anchorView)
            mMediaController!!.setEnabled(isInPlaybackState())
        }
    }

   private val mSizeChangedListener= MediaPlayer.OnVideoSizeChangedListener { mediaPlayer: MediaPlayer, i: Int, i1: Int ->
       with(mediaPlayer){
           mVideoWidth=width
           mVideoHeight=height
       }
//       if (mVideoWidth!=0&&mVideoHeight!=0){
//           if (mRenderView!=null){
//               mRenderView!!.setVideoSize(mVideoWidth,mVideoHeight)
//               mRenderView!!.setVideoSampleAspectRatio(mVideoSarNum,mVideoSarDen)
//           }
//           requestLayout()
//       }
   }
    private val mPreparedListener=MediaPlayer.OnPreparedListener { mp ->
        mPrepareEndTime=System.currentTimeMillis()
//        mHudViewHolder.updateLoadCost(mPrepareEndTime-mPrepareStartTime)
        mCurrentState= STATE_PREPARED

        mOnPreparedListener?.onPrepared(mMediaPlayer)
        mMediaController?.setEnabled(true)

        mVideoWidth=mp.videoWidth
        mVideoHeight=mp.videoHeight

        val seekToPosition=mSeekWhenPrepared //mSeekWhenPrepared may be changed after seekTo()
        if (seekToPosition!=0){
            seekTo(seekToPosition)
        }
        if (mVideoWidth!=0&&mVideoHeight!=0){
                if (mSurfaceWidth==mVideoWidth&& mSurfaceHeight ==mVideoHeight){
                    //we din't actually change the size (it was already at the size
                    //we need), so we won't get a "surface changed" callback,so
                    //start the video here instead of in the callback
                    if (mTargetState== STATE_PLAYING){
                        start()
                        mMediaController?.show()
                    }else if (!isPlaying()&&
                            (seekToPosition!=0||getCurrentPosition()>0)){
                        //show the media controls when we're paused into a video and make 'em stick.
                        mMediaController?.show(0)
                    }
                }
        }else{
            //we don't know the video size yet,but should start
            //The video size might be reported to us later.
            if (mTargetState== STATE_PLAYING){
                start()
            }
        }
    }

    private val mCompletionListener=MediaPlayer.OnCompletionListener {mp ->
        mCurrentState= STATE_PLAYBACK_COMPLETED
        mTargetState= STATE_PLAYBACK_COMPLETED
        mMediaController?.hide()
        mOnCompletionListener?.onCompletion(mMediaPlayer)
    }
    private val mInfoListener=MediaPlayer.OnInfoListener { mp, arg1, arg2 ->
        mOnInfoListener?.onInfo(mp,arg1,arg2)
//        when(arg1){
//            MediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED->{
//                mVideoRotationDegree=arg2
//                mRenderView?.setVideoRotation(arg2)
//            }
//        }
        true
    }

    private val mErrorListener=MediaPlayer.OnErrorListener { iMediaPlayer, framework_er, impl_err ->
        mCurrentState= STATE_ERROR
        mTargetState= STATE_ERROR
        mMediaController?.hide()

        /*if an error handler has been supplied, use it and finish*/
        if (mOnErrorListener!=null){
            if (mOnErrorListener!!.onError(mMediaPlayer,framework_er,impl_err)){
                true
            }
        }

        /**
         * Otherwise, pop up an error dialog so the user knows that
         * something bad has happened.Only try and pop up the dialog
         * if we're attached to a window.When we're going away and no
         * longer have a window,don't bother showing the user an error.
         */
        if (windowToken!=null){
            val r=mAppContext.resources
            val messageId: Int
            if (framework_er==MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK){
                messageId= R.string.VideoView_error_text_invalid_progressive_playback
            }else{
                messageId= R.string.VideoView_error_text_unknown
            }
            AlertDialog.Builder(context)
                    .setMessage(messageId)
                    .setPositiveButton(R.string.VideoView_error_button){
                        dialog, which ->
                        mOnCompletionListener?.onCompletion(mMediaPlayer)
                    }
                    .setCancelable(false)
                    .show()
        }
        true
    }

    private val mBufferingUpdateListener=MediaPlayer.OnBufferingUpdateListener { iMediaPlayer, percent ->
        mCurrentBufferPercentage=percent
    }

    private val mSeekCompleteListener=MediaPlayer.OnSeekCompleteListener { mp->
        mSeekEndTime=System.currentTimeMillis()
//        mHudViewHolder.updateSeekCost(mSeekEndTime-mSeekStartTime)
    }

    private val mOnTimeTextListener=MediaPlayer.OnTimedTextListener { iMediaPlayer, text ->
        if (text!=null){
            subtitleDisplay.text= text.toString()
        }
    }

    /**
     * Register a callback to be invoked when the media file
     * is loaded and ready to go.
     *
     * @param l The callback that will be run
     */
    fun setOnPreparedListener(l: MediaPlayer.OnPreparedListener) {
        mOnPreparedListener = l
    }

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param l The callback that will be run
     */
    fun setOnCompletionListener(l: MediaPlayer.OnCompletionListener) {
        mOnCompletionListener = l
    }

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no listener is specified,
     * or if the listener returned false, VideoView will inform
     * the user of any errors.
     *
     * @param l The callback that will be run
     */
    fun setOnErrorListener(l: MediaPlayer.OnErrorListener) {
        mOnErrorListener = l
    }

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     *
     * @param l The callback that will be run
     */
    fun setOnInfoListener(l: MediaPlayer.OnInfoListener) {
        mOnInfoListener = l
    }





    /**
     * release the media player in any state
     */
    fun release(cleartargetstate: Boolean){
        if (mMediaPlayer!=null){
            mMediaPlayer!!.reset()
            mMediaPlayer!!.release()
            mMediaPlayer==null
            mCurrentState= STATE_IDLE
            if (cleartargetstate){
                mTargetState= STATE_IDLE
            }
            val am=mAppContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.abandonAudioFocus(null)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isInPlaybackState()&&mMediaController!=null){
            toggleMediaControlsVisiblity()
        }
        return false
    }

    override fun onTrackballEvent(event: MotionEvent?): Boolean {
        if (isInPlaybackState()&&mMediaController!=null){
            toggleMediaControlsVisiblity()
        }
        return false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
                keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                keyCode != KeyEvent.KEYCODE_VOLUME_MUTE &&
                keyCode != KeyEvent.KEYCODE_MENU &&
                keyCode != KeyEvent.KEYCODE_CALL &&
                keyCode != KeyEvent.KEYCODE_ENDCALL
        if (isInPlaybackState() && isKeyCodeSupported && mMediaController != null) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (mMediaPlayer!!.isPlaying()) {
                    pause()
                    mMediaController?.show()
                } else {
                    start()
                    mMediaController?.hide()
                }
                return true
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                if (!mMediaPlayer!!.isPlaying()) {
                    start()
                    mMediaController?.hide()
                }
                return true
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                if (mMediaPlayer!!.isPlaying()) {
                    pause()
                    mMediaController?.show()
                }
                return true
            } else {
                toggleMediaControlsVisiblity()
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun toggleMediaControlsVisiblity() {
        if (mMediaController!!.isShowing()){
            mMediaController!!.hide()
        }else{
            mMediaController!!.show()
        }
    }

    override fun start() {
        if (isInPlaybackState()){
            mMediaPlayer!!.start()
            mCurrentState= STATE_PLAYING
        }
        mTargetState= STATE_PLAYING
    }

    override fun pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer!!.isPlaying()) {
                mMediaPlayer!!.pause()
//                mSeekWhenPrepared= mMediaPlayer!!.currentPosition.toInt()
                mCurrentState = STATE_PAUSED
            }
        }
        mTargetState = STATE_PAUSED
    }

    fun suspend(){
        release(false)
    }
     fun resume(){
        openVideo()
    }

    override fun getDuration(): Int {
        if (isInPlaybackState()){
            return mMediaPlayer!!.duration
        }
        return -1
    }

    override fun getCurrentPosition(): Int {
        if (isInPlaybackState()){
            return mMediaPlayer!!.currentPosition
        }
        return 0
    }
    override fun seekTo(mesc: Int) {
        if (isInPlaybackState()){
            mSeekStartTime=System.currentTimeMillis()
            mMediaPlayer!!.seekTo(mesc)
            mSeekWhenPrepared=0
        }else{
            mSeekWhenPrepared= mesc
        }
    }
    override fun isPlaying(): Boolean {
        return isInPlaybackState()&&mMediaPlayer!!.isPlaying
    }

    override fun getBufferPercentage(): Int {

        if (mMediaPlayer!=null){
            return mCurrentBufferPercentage
        }
        return 0
    }

    private fun isInPlaybackState()=
            mMediaPlayer!=null&&
                    mCurrentState!= STATE_ERROR &&
                    mCurrentState!= STATE_IDLE &&
                    mCurrentState!= STATE_PREPARING



//    fun togglePlayer(): Int {
//        mMediaPlayer?.release()
//
//        mRenderView?.getView()!!.invalidate()
//        openVideo()
//        return 0
//    }

    private fun createPlayer(): MediaPlayer{
//        mMediaPlayer=IjkExoMediaPlayer(mAppContext)
        mMediaPlayer=MediaPlayer()
        return mMediaPlayer as MediaPlayer
    }


    var mOnCompletionListener:MediaPlayer.OnCompletionListener?=null
    var mOnPreparedListener: MediaPlayer.OnPreparedListener?=null
    var mOnErrorListener: MediaPlayer.OnErrorListener?=null

}