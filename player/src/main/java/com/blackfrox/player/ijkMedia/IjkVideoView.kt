package com.blackfrox.player.ijkMedia

import android.annotation.TargetApi
import android.content.Context
import android.content.DialogInterface
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import com.blackfrox.player.R
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import tv.danmaku.ijk.media.player.IjkTimedText
import java.io.File
import java.io.IOException

/**
 * Created by Administrator on 2018/3/5 0005.
 */
abstract class IjkVideoView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet?=null, def: Int=0)
    : FrameLayout(context,attributeSet,def) {
    private val TAG="IjkVideoView"
    //settable by the client
    protected var mUri: Uri?=null
    private var mHeaders: Map<String,String>?=null


    //all possible internal states
    protected val STATE_ERROR = -1
    protected val STATE_IDLE = 0
    protected val STATE_PREPARING = 1
    protected val STATE_PREPARED = 2
    protected val STATE_PLAYING = 3
    protected val STATE_PAUSED = 4
    protected val STATE_PLAYBACK_COMPLETED = 5

    // mCurrentState is a VideoView object's current state.
    // mTargetState is the state that a method caller intends to reach.
    // For instance, regardless the VideoView object's current state,
    // calling pause() intends to bring the object to a target state
    // of STATE_PAUSED.
    protected var mCurrentState = STATE_IDLE
    private var mTargetState = STATE_IDLE

    //All the stuff we need for playing and showing a video
    protected var mSurfaceHolder: IRenderView.ISurfaceHolder?=null
    private var mSurfaceWidth=0
    private var mSurfaceHeight=0
    protected var mMediaPlayer: IMediaPlayer?=null
    private var mVideoWidth:Int
    private var mVideoHeight:Int
    private var mVideoRotationDegree: Int=0

    var mOnPreparedListener: IMediaPlayer.OnPreparedListener?=null
    var mOnCompletionListener: IMediaPlayer.OnCompletionListener?=null
    var mOnErrorListener: IMediaPlayer.OnErrorListener?=null
    var mOnInfoListener: IMediaPlayer.OnInfoListener?=null
    private var mCurrentBufferPercentage: Int=0
    private var mSeekWhenPrepared =0L//recording the seek position while preparing

    protected  var mAppContext: Context
    private lateinit var mSettings: Settings
    protected lateinit var mRenderView: IRenderView
    private var mVideoSarNum: Int=0
    private var mVideoSarDen: Int=0

//    private var mHudViewHolder: InfoHudViewHolder?=null

    private var mPrepareStartTime=0L
    private var mPrepareEndTime=0L

    private var mSeekStartTime=0L
    private var mSeekEndTime=0L

    init {
        mAppContext=context.applicationContext

//        initBackground()
        initRenders()

        mVideoWidth=0
        mVideoHeight=0

        isFocusable=true
        isFocusableInTouchMode=true
        requestFocus()

        mCurrentState= STATE_IDLE
        mTargetState= STATE_IDLE

    }

    //不知道为什么这个变量用不了，所以在rendView中用lamabda又写了一个就正常了
    private val mSHCallback=object :IRenderView.IRenderCallback{

        //调用mediaPlayer的方法
        override fun onSurfaceChanged(holder: IRenderView.ISurfaceHolder, format: Int, width: Int, height: Int) {
          Log.d(TAG,"suraceChanged")
            if (holder.renderView!=mRenderView)
                return

            mSurfaceWidth=width
            mSurfaceHeight=height
            val isValidState= (mTargetState== STATE_PLAYING)
            val hasValidSize = !mRenderView.shouldWaitForResize()||(mVideoWidth==width&&mVideoHeight==height)
            if (mMediaPlayer!=null&&isValidState&&hasValidSize){
                if (mSeekWhenPrepared!=0L)
                    seekTo(mSeekWhenPrepared)
                //Todo: 这里实现了自动播放，以后需要优化的时候可以在这里更改
                start()
            }
        }
        //将surfaceView和mediaPlayer绑定
        override fun onSurfaceCreated(holder: IRenderView.ISurfaceHolder, width: Int, height: Int) {
//            Log.d(TAG,"suraceCreated")

            if(holder.renderView!=mRenderView)
                return

            mSurfaceHolder=holder
            //判断mMediaPlayer是否已经实例化了
            if (mMediaPlayer!=null){
                bindSurfaceHolder(mMediaPlayer!!,holder)
            }else{
                openVideo()
            }
        }

        //mediaPlayer解绑
        override fun onSurfaceDestroyed(holder: IRenderView.ISurfaceHolder) {
            Log.d(TAG,"suraceDestoryed")

            if (holder.renderView!=mRenderView)
                return

            //after we return from this we can't use the surface any more
            mSurfaceHolder=null
            mMediaPlayer?.setDisplay(null)
        }
    }

    //布局中设置的宽高
    protected var initWidth: Int =0
    protected  var initHeight: Int =0
   open protected fun setRenderView(renderView: IRenderView) {

//        if (mRenderView!=null){
//            mMediaPlayer?.setDisplay(null)
//
//            val renderUIView= mRenderView!!.view
//            mRenderView!!.removeRenderCallback(mSHCallback)
//            mRenderView=null
//            removeView(renderUIView)
//        }

        mRenderView=renderView
//        renderView.setAspectRatio(mCurrentAspectRatio)
        if (mVideoWidth>0&&mVideoHeight>0)
            renderView.setVideoSize(mVideoWidth,mVideoHeight)
        if (mVideoSarNum>0&&mVideoSarDen>0)
            renderView.setVideoSampleAspectRatio(mVideoSarNum,mVideoSarDen)

        //其实就是surfaceView
        val renderUIView= mRenderView.view
        val lp=LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER)
        renderUIView.layoutParams=lp
        addView(renderUIView)

        //这个Callback的作用就相当于SurfaceHolder.Callback
//        mRenderView.addRenderCallback(mSHCallback)
        mRenderView.addRenderCallback(object :IRenderView.IRenderCallback{

            override fun onSurfaceChanged(holder: IRenderView.ISurfaceHolder, format: Int, width: Int, height: Int) {

                if (holder.renderView!=mRenderView)
                    return

                mSurfaceWidth=width
                mSurfaceHeight=height
                val isValidState= (mTargetState== STATE_PLAYING)
                val hasValidSize = !mRenderView.shouldWaitForResize()||(mVideoWidth==width&&mVideoHeight==height)
                if (mMediaPlayer!=null&&isValidState&&hasValidSize){
                    if (mSeekWhenPrepared!=0L)
                        seekTo(mSeekWhenPrepared)

                    //todo 实现了自动播放
                    start()
                }
            }
            override fun onSurfaceCreated(holder: IRenderView.ISurfaceHolder, width: Int, height: Int) {
                initWidth=this@IjkVideoView.width
                initHeight=this@IjkVideoView.height

                if(holder.renderView!=mRenderView)
                    return

                mSurfaceHolder=holder
                //判断mMediaPlayer是否已经实例化了
                if (mMediaPlayer!=null){
                    bindSurfaceHolder(mMediaPlayer!!,holder)
                }else{
                    openVideo()
                }
            }

            override fun onSurfaceDestroyed(holder: IRenderView.ISurfaceHolder) {
                if (holder.renderView!=mRenderView)
                    return

                //after we return from this we can't use the surface any more
                mSurfaceHolder=null
                mMediaPlayer?.setDisplay(null)
            }
        })
        mRenderView.setVideoRotation(mVideoRotationDegree)
    }
    /**
     * Sets video path
     *
     */
    protected var isNetworkUri = false
    open fun setVideoPath(path: String){
        if (path.startsWith("http"))
            isNetworkUri=true
        setVideoURI(Uri.parse(path))
    }

    /**
     * Sets video URI
     *
     * @param uri the URI of the video
     * @param headers the headers for the URI request.
     *                  Note that the cross domain redirection is allowed by default,but that can be
     *                  changed with key/value pairs through the headers paramethe with
     *                  "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     *                  to disallow or allow cross domain redirection.
     */
    open fun setVideoURI(uri: Uri,headers: Map<String,String>?=null){
        mUri=uri
        mHeaders=headers
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
//            mHudViewHolder?.setMediaPlayer(null)
            mCurrentState= STATE_IDLE
            mTargetState= STATE_IDLE
            val am=mAppContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.abandonAudioFocus(null)
        }
    }

    /**
     * 初始化MediaPlayer
     */
    @TargetApi(Build.VERSION_CODES.M)
    private fun openVideo(){
        if (mUri==null||mSurfaceHolder==null){
            //not ready for playback just yet, will try again later
            return
        }
        //we shouldn't clear the target state, because somebody might have
        //called start() previously
        release(false)

        val am=mAppContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.requestAudioFocus(null,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN)

        try {
            mMediaPlayer=createPlayer()

            with(mMediaPlayer!!){
                setOnPreparedListener(mPreparedListener)
                setOnVideoSizeChangedListener(mSizeChangedListener)
                setOnCompletionListener(mCompletionListener)
                setOnErrorListener(mErrorListener)
                setOnInfoListener(mInfoListener)
                setOnBufferingUpdateListener(mBufferingUpdateListener)
                setOnSeekCompleteListener(mSeekCompleteListener)
                setOnTimedTextListener(mOnTimedTextListener)
                mCurrentBufferPercentage=0
                val scheme= mUri!!.scheme
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M&&
                        TextUtils.isEmpty(scheme)||scheme.equals("file",ignoreCase = true)){
                    val datasource=FileMediaDataSource(File(mUri.toString()))
                    setDataSource(datasource)
                }else if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH&&mHeaders!=null){
//                    Log.d(TAG,"URI    $mUri")
                    setDataSource(mAppContext,mUri,mHeaders)
                }else{
//                    Log.d(TAG,"URI     $mUri")
                    setDataSource(mUri.toString())
                }
                bindSurfaceHolder(mMediaPlayer!!, mSurfaceHolder!!)
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                setScreenOnWhilePlaying(true)
                mPrepareStartTime=System.currentTimeMillis()
                prepareAsync()
//                mHudViewHolder?.setMediaPlayer(mMediaPlayer)
            }

            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING
//            attachMediaController()
        }catch (e: IOException) {
            Log.w(TAG, "Unable to open content: " + mUri, e);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Unable to open content: " + mUri, e)
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } finally {
            // REMOVED: mPendingSubtitleTracks.clear();
        }
    }

    private val mSizeChangedListener=IMediaPlayer.OnVideoSizeChangedListener{ mp: IMediaPlayer, width: Int, height: Int, sarNum: Int, sarDen: Int ->
        mVideoWidth=mp.videoWidth
        mVideoHeight=mp.videoHeight
        mVideoSarDen=mp.videoSarDen
        mVideoSarNum=mp.videoSarNum
        if (mVideoWidth!=0&&mVideoHeight!=0){
            if (mRenderView!=null){
                mRenderView.setVideoSize(mVideoWidth,mVideoHeight)
                mRenderView.setVideoSampleAspectRatio(mVideoSarNum,mVideoSarDen)
            }
            requestLayout()
        }
    }

    private val mPreparedListener=IMediaPlayer.OnPreparedListener{
        mPrepareEndTime=System.currentTimeMillis()
//        mHudViewHolder.updateLoadCost(mPrepareEndTime-mPrepareStartTime)
        mCurrentState= STATE_PREPARED

        mOnPreparedListener?.onPrepared(it)
        mVideoWidth=it.videoWidth
        mVideoHeight=it.videoHeight

        val seekToPosition=mSeekWhenPrepared //mSeekWhenPrepared may be changed after seekTo() call
        if (seekToPosition!=0L)
            seekTo(seekToPosition)

        if (mVideoWidth!=0&&mVideoHeight!=0){
            mRenderView.let{
                it.setVideoSize(mVideoWidth,mVideoHeight)
                it.setVideoSampleAspectRatio(mVideoSarNum,mVideoSarDen)
                if (!it.shouldWaitForResize()||mSurfaceWidth==mVideoWidth&&mSurfaceHeight==mVideoHeight){

                    //We didn't actually change the size(it was already at the size
                    //we need),so we won't get a "surface changed" callback,so
                    //start the video here instead of the callback.
                    if (mTargetState== STATE_PLAYING){
                        start()
                    }else if (!isPlaying()&&
                            (seekToPosition!=0L||getCurrentPosition()>0)){
                       show(5000)
                    }
                }
            }
        }else{
            // We don't know the video size yet, but should start anyway.
            // The video size might be reported to us later.
            if (mTargetState == STATE_PLAYING) {
                start()
            }
        }
        setStateAndUI(mCurrentState)
    }

    private val mCompletionListener=IMediaPlayer.OnCompletionListener{
        mCurrentState= STATE_PLAYBACK_COMPLETED
        mTargetState= STATE_PLAYBACK_COMPLETED
        hide()
        mOnCompletionListener?.onCompletion(it)
        setStateAndUI(mCurrentState)
    }

    private val mInfoListener=IMediaPlayer.OnInfoListener{ iMediaPlayer: IMediaPlayer, what: Int, i1: Int ->
        mOnInfoListener?.onInfo(iMediaPlayer,what,i1)
        when(what){
            IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED->{
                mVideoRotationDegree=i1
                mRenderView?.setVideoRotation(i1)
            }
        }
        setStateAndUI(what)
        true
    }

    private val mErrorListener=IMediaPlayer.OnErrorListener{ iMediaPlayer: IMediaPlayer, i: Int, i1: Int ->
        mCurrentState= STATE_ERROR
        mTargetState= STATE_ERROR
        hide()
        /*  If an error hander has been supplied, use it and finish.*/
        mOnErrorListener?.let {
            if (it.onError(iMediaPlayer,i,i1))
                true
        }
        /* Otherwise, pop up an error dialog so the user knows that
                     * something bad has happened. Only try and pop up the dialog
                     * if we're attached to a window. When we're going away and no
                     * longer have a window, don't bother showing the user an error.
                     */
        if (windowToken != null) {
            val r = mAppContext.resources
            val messageId: Int

            if (i == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
                messageId = R.string.VideoView_error_text_invalid_progressive_playback
            } else {
                messageId = R.string.VideoView_error_text_unknown
            }

            AlertDialog.Builder(getContext())
                    .setMessage(messageId)
                    .setPositiveButton(R.string.VideoView_error_button,
                            DialogInterface.OnClickListener { dialog, whichButton ->
                                /* If we get here, there is no onError listener, so
                                             * at least inform them that the video is over.
                                             */
                                mOnCompletionListener?.onCompletion(mMediaPlayer)
                            })
                    .setCancelable(false)
                    .show()
        }
        setStateAndUI(mCurrentState)
        true
    }

    private val mBufferingUpdateListener=IMediaPlayer.OnBufferingUpdateListener{ mediaPlayer: IMediaPlayer, percent: Int ->
        mCurrentBufferPercentage=percent

    }

    private val mSeekCompleteListener=IMediaPlayer.OnSeekCompleteListener{
        mSeekEndTime=System.currentTimeMillis()
//        mHudViewHolder.updateSeekCost(mSeekEndTime-mSeekStartTime)
    }

    private val mOnTimedTextListener=IMediaPlayer.OnTimedTextListener{ iMediaPlayer: IMediaPlayer, text: IjkTimedText ->
//        if (text!=null)
//            subtitleDisplay.setText(text.text)
    }

    private fun bindSurfaceHolder(mp: IMediaPlayer,holder: IRenderView.ISurfaceHolder){

        holder.bindToMediaPlayer(mp)
    }



    /**
     * release the media player in any state
     */
    fun release(cleaertargetstate: Boolean){
        if (mMediaPlayer!=null){
            mMediaPlayer!!.reset()
            mMediaPlayer!!.release()
            mMediaPlayer=null
            if (cleaertargetstate)
                mTargetState= STATE_IDLE
            val am=mAppContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.abandonAudioFocus(null)
        }
    }

    /**
     * 当前mMediaPlayer是否正常
     *         1 是： 直接播放
     *         2 否: 设置mTargetState为STATE_PLAYING,当mediaPlayer初始化会回调onPreparedListener，
     *          根据mTargetState==STATE_PLAYING 进行是否播放
     */
   open fun start(){
        if (isInPlaybackState()){
            mMediaPlayer!!.start()
            mCurrentState= STATE_PLAYING
        }
        mTargetState= STATE_PLAYING
    }

   open fun pause(){
        if(isInPlaybackState()){
            if (mMediaPlayer!!.isPlaying){
                mMediaPlayer!!.pause()
                mCurrentState= STATE_PAUSED
            }
        }
        mTargetState= STATE_PAUSED
    }

    fun suspend()=
            release(false)

    fun resume()=
            openVideo()

    //TODO: 将-1改成0了，不知道会有什么影响
    fun getDuration(): Long {
        if(isInPlaybackState()) return mMediaPlayer!!.duration
        return 0
    }

    fun getCurrentPosition(): Long {
        if (isInPlaybackState()){
            return mMediaPlayer!!.currentPosition
        }
        return 0L
    }

    fun seekTo(mesc: Long){
        if (isInPlaybackState()){
            mSeekStartTime=System.currentTimeMillis()
            mMediaPlayer!!.seekTo(mesc)
            mSeekWhenPrepared=0
        }else{
            mSeekWhenPrepared=mesc
        }
    }

    fun isPlaying()=
            isInPlaybackState()&&mMediaPlayer!!.isPlaying


    fun getBufferPercentage(): Int {
        if (mMediaPlayer!=null)
            return mCurrentBufferPercentage
        return 0
    }

    private fun isInPlaybackState()=
            mMediaPlayer!=null&&
                    mCurrentState!= STATE_ERROR&&
                    mCurrentState!= STATE_IDLE&&
                    mCurrentState!= STATE_PREPARING


    /**
     * Render
     */
    var enableTextureView = false
    private fun initRenders() {
        var renderView: IRenderView
       if (enableTextureView){
            renderView = TextureRenderView(getContext())
           if (mMediaPlayer != null) {
               renderView.getSurfaceHolder().bindToMediaPlayer(mMediaPlayer)
               renderView.setVideoSize(mMediaPlayer!!.getVideoWidth(), mMediaPlayer!!.getVideoHeight())
               renderView.setVideoSampleAspectRatio(mMediaPlayer!!.getVideoSarNum(), mMediaPlayer!!.getVideoSarDen())
//               renderView.setAspectRatio(mCurrentAspectRatio)
           }
       }else{
            renderView=SurfaceRenderView(context)
       }

        setRenderView(renderView)
    }

    //-------------------------
    // Extend: Player
    //-------------------------
    private fun createPlayer(): IMediaPlayer? {
        var mediaPlayer: IMediaPlayer?=null

        var ijkMediaPlayer: IjkMediaPlayer?=null
        if (mUri!=null){
            ijkMediaPlayer= IjkMediaPlayer()

//            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"mediacodec",0)
            mediaPlayer=ijkMediaPlayer
        }
        return mediaPlayer
    }

    /**               需要继承的方法           **/
    abstract  fun  show(timeout: Long)

    abstract  fun hide()

    abstract fun setStateAndUI(currentState: Int)

}