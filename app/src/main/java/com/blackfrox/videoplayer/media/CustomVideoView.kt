package com.blackfrox.videoplayer.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.support.v7.app.AlertDialog
import android.util.AttributeSet
import android.view.*
import com.blackfrox.videoplayer.R
import com.blackfrox.videoplayer.toast


/**
 * Created by Administrator on 2017/11/16 0016.
 */
class CustomVideoView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet?=null, def: Int=0,defRes: Int=0)
    :SurfaceView(context,attributeSet,def,defRes), MediaController.MediaPlayerController {

    //all possible internal states
    private val STATE_ERROR = -1
    private val STATE_IDLE = 0
    private val STATE_PREPARING = 1
    private val STATE_PREPARED = 2
    private val STATE_PLAYING = 3
    private val STATE_PAUSED = 4
    private val STATE_PLAYBACK_COMPLETED = 5

    //原本还有一个mHeaders不知道干嘛用的
    //settable by the client
    private var mUri: Uri? = null

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
    //好像不能懒加载，是因为持有context的原因吗？
    //下面两个原本是有null属性，出问题可能会在这里
    //加null是为了释放资源还是其他原因。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。
    private var mSurfaceHolder: SurfaceHolder? = null //因为在销毁时需要解除引用
    private var mMediaPlayer: MediaPlayer?=null  //崩溃了，因为release()
    private var mAudioSession = 0
    private var mVideoHeight = 0
    private var mVideoWidth = 0
    private var mSurfaceWidth = 0
    private var mSurfaceHeight = 0
    private lateinit var mMediaController: MediaController
    //一堆回调listener
    private var mSeekWhenPrepared = 0  //recording the seek position while preparing
    private var mCurrentBuffferPercentage=0
    //下面三个实际使用时还没用到过
    private var mCanPause = false
    private var mCanSeekBack = false
    private var mCanSeekForward = false
//    private lateinit var mAudioManager: AudioManager
    //我觉得可以去除mAudioFocusType变量，因为是配合一个方法使用的，但是那个方法用额暂时用不到的样子
//    private var mAudioFocusType = AudioManager.AUDIOFOCUS_GAIN //legacy focus again
    private lateinit var mAudioAttributes: AudioAttributes

    /** Subtitle rendering widget overlaid on top of the video**/
//    private lateinit var mSubtitleWidget:RenderingWidget

    init {

        mVideoWidth=0
        mVideoHeight=0

//        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        //我靠，是api21才有的属性 ，封装音频的各种属性
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mAudioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build()
        }
        holder.addCallback(object : SurfaceHolder.Callback{
            //当Surface的状态（大小和格式）发生变化的时候会调用该函数，在surfaceCreated调用后该函数至少会被调用一次。
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
                mSurfaceWidth = width
                mSurfaceHeight = height
                val isValidState = (mTargetState == STATE_PLAYING)
                val hasValidSize = (mVideoWidth == width && mVideoHeight == height)
                if (mMediaPlayer!=null&&isValidState && hasValidSize) {
                    if (mSeekWhenPrepared != 0) {
                        seekTo(mSeekWhenPrepared)
                    }
                    start()
                }
            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                mSurfaceHolder = holder
                openVideo()
            }

            //当Surface被摧毁前会调用该函数，该函数被调用后就不能继续使用Surface了，一般在该函数中来清理使用的资源。
            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                mSurfaceHolder = null
                mMediaController.hide()
                release(true)
            }
        })
        //源码说现在是忽略的，会自动设置的，以后优化的时候删除试试看/斜眼笑
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()

        mCurrentState=STATE_IDLE
        mCurrentState=STATE_IDLE
    }

    //问题:为什么高是固定的，但是突然为了适应视频大小，变小了?
    //这种情况是发生在layout_height设为=400dp的时候
    //但是在使用240dp时就没有问题，可能是本身存在问题吧，还是我要修改代码???
    //不，改成240dp，本身大小也变动了
//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        var width = View.getDefaultSize(mVideoWidth, widthMeasureSpec)
//        var height = View.getDefaultSize(mVideoHeight, heightMeasureSpec)
//        if (mVideoWidth > 0 && mVideoHeight > 0) {
//
//            val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
//            val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
//            val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
//            val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)
//
//            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
//                //the size is fixed
//                width = widthSpecSize
//                height = heightSpecSize
//
//                //for compatibility, we adjust size based on aspect ratio
//                //mVideoWidth/mVideoHeight<width/height
//                //如果视频比给定的尺寸小
//                if (mVideoWidth * height < width * mVideoHeight) {
//                    width = height * mVideoWidth / mVideoHeight
//
//                } else if (mVideoWidth * height > width * mVideoHeight) {
//                    height = width  * mVideoHeight / mVideoWidth
//                }
//            } else if (widthSpecMode == MeasureSpec.EXACTLY) {
//                //only the width is fixed,adjust the height to match aspect ratio if possible
//                width = widthSpecSize
//                height = width * mVideoHeight / mVideoWidth
//                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
//                    //couldn't match aspect ratio within the constraints
//                    height = heightSpecSize
//                }
//            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
//                //only the height is fixed,adjust the width to match aspect ratio if possible
//                height = heightSpecSize
//                width = height * mVideoWidth / mVideoHeight
//                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
//                    //couldn't match aspect ratio within the cocnstraints
//                    width = widthSpecSize
//                }
//            } else {
//                //neither the width or the height are fixed,try to use actual video size
//                width = mVideoWidth
//                height = mVideoHeight
//                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
//                    //too tall,decrease both width and height
//                    height = heightSpecSize
//                    width = height * mVideoWidth / mVideoHeight
//                }
//                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
//                    //too width,decrease both width and height
//                    width = widthSpecSize
//                    height = width * mVideoHeight / mVideoWidth
//                }
//            }
//        } else {
//            //no size yet, just adopt the given spec size
//        }
//        setMeasuredDimension(width, height)
//    }

    override fun getAccessibilityClassName(): CharSequence {
        return CustomVideoView::class.java.name
    }


    //1 实现后台保存进度，返回恢复进度
    fun setVideoPath(path: String) {

        setVideoURI(Uri.parse(path))
    }

    fun setVideoURI(uri: Uri) {
        mUri = uri
        mSeekWhenPrepared = 0
        openVideo()
        requestLayout()
        invalidate()
    }

//    setAudioFocusRequest
//    setAudioAttributes
//    addSUbtitleSource

    fun stopPlayback() {
        if (mMediaPlayer!=null){
            with(mMediaPlayer!!) {
                stop()
                release()
                mCurrentState = STATE_IDLE
                mTargetState = STATE_IDLE
//                mAudioManager.abandonAudioFocus(null)
            }
        }
    }


    private fun openVideo() {
        if (mUri == null && mSurfaceHolder != null) {
            //not ready for playback just yet,will try again later
            return
        }

        //we shouldn't clear the target state,because somebody might have
        //called start() previously
        release(false)

//        if (mAudioFocusType != AudioManager.AUDIOFOCUS_NONE) {
//            //TODO this should have a focus listener
//            //少写了一个audioAttributes 因为好像没有这个方法了
//            mAudioManager.requestAudioFocus(null, mAudioFocusType, 0 /*flags*/)
//        }
        //ijkPlayer中的下面两行代码
//        val am=context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        am.requestAudioFocus(null,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN )
//        //暂停其它音频播放
//        val i = Intent("com.android.music.musicservicecommand")
//        i.putExtra("command", "pause")
//        context.sendBroadcast(i)

        try {
            mMediaPlayer= MediaPlayer()
            with(mMediaPlayer!!) {
                if (mAudioSession != 0) {
                    mMediaPlayer!!.audioSessionId = mAudioSession
                } else {
                    mAudioSession = mMediaPlayer!!.audioSessionId
                }
                //各种listener
                setOnPreparedListener(mPreparedListener)
                //将holder设置video的宽高
                setOnVideoSizeChangedListener(mSizeChangedListener)
                setOnCompletionListener(mCompletionListener)
                setOnErrorListener(mErrorListener)
                //卧槽，看的源码都没加逻辑，
                //源码是不用显示controller的，但是国内都是显示的，所以造成了错觉
                setOnInfoListener(mInfoListener)
                setOnBufferingUpdateListener(mBufferingUpdateListener)
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//                    setOnTimedTextListener(mTimedTextListener)
//                }
                mCurrentBuffferPercentage = 0

                setDataSource(context, mUri)
                //问题:为什么要持有surfaceHolder的引用变量，而不是用自带的getHolder方法呢？
                setDisplay(mSurfaceHolder)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(mAudioAttributes)
                }
                setScreenOnWhilePlaying(true)
                prepareAsync()
            }
            //We don't set the target state here either,but preserve the
            //target state that was there before
            mCurrentState = STATE_PREPARING
            attachMediaController()
        } catch (e: Exception) {
            e.printStackTrace()
            mCurrentState = STATE_ERROR
            mTargetState = STATE_ERROR
//            mErrorListener.onError(mMediaPlayer,)
            return
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            mCurrentState = STATE_ERROR
            mTargetState = STATE_ERROR
//            mErrorListener.onError(mMediaPlayer,)
            return
        }
    }

    fun setMediaController(controller: MediaController) {
        mMediaController = controller
//        mMediaController.hide() //不知道为什么报没有初始化 因为我实例化是错误的，而且以前放了布局，还以为控件已经实例化了，囧
        attachMediaController()
    }

    private fun attachMediaController() {
       if (mMediaPlayer!=null){
           mMediaController.setMediaPlayer(this)
           val anchorView = if (this.parent is View)
               this.parent as View else this
           mMediaController.setAnchorView(anchorView)
           //临时修改，不知道能不能用
//           mMediaController.setEnabled(isInPlaybackState())
       }
    }

    private val mSizeChangedListener = MediaPlayer.OnVideoSizeChangedListener { mp, width, height ->
        mVideoWidth = mp.videoWidth
        mVideoHeight = mp.videoHeight
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            holder.setFixedSize(mVideoWidth, mVideoHeight)
            //改变位置，这里应该是尺寸吧
            requestLayout()
        }
    }

    private val mPreparedListener = MediaPlayer.OnPreparedListener { mp ->
        mCurrentState = STATE_PREPARED

        //Get the capabilitied of the player for this stream
//        val dataa=mp.getMetadata(...) 方法没有勒
        //难怪这几个参数的方法也没有了
//        if (data==null){
//            mCanPause=mCanSeekBack=mCanSeekForward=true
//        }

//        mOnPreparedListener?.onPrepared(mp)
        mMediaController.setEnabled(true)
        mVideoWidth = mp.videoWidth
        mVideoHeight = mp.videoHeight

        //mSeekWhenPrepared may be changed after seekTo() call
        if (mSeekWhenPrepared != 0) {
            seekTo(mSeekWhenPrepared)
        }
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            holder.setFixedSize(mVideoWidth, mVideoHeight)
            if (mSurfaceWidth == mVideoWidth || mSurfaceHeight == mVideoHeight) {
                //We din't actually change the size (it was already at the size
                //we need), so we won't get a "surface changed" callback, so
                //start the video here instead of in the callback
                if (mTargetState == STATE_PLAYING) {
                    start()
                    mMediaController.show()
                } else if (!isPlaying()
                        && mSeekWhenPrepared != 0 || getCurrentPosition() > 0) {
                    mMediaController.show()
                }
            }
        } else {
            //We don't know the video size yet,but should start anyway,
            //The video size might be reported to us later
            if (mTargetState == STATE_PLAYING) {
                start()
            }
        }
    }

    private val mCompletionListener = MediaPlayer.OnCompletionListener {
        mCurrentState = STATE_PLAYBACK_COMPLETED
        mTargetState = STATE_PLAYBACK_COMPLETED
        mMediaController.hide()
        context.toast("播放完毕")
//        if (mAudioFocusType != AudioManager.AUDIOFOCUS_NONE) {
//            mAudioManager.abandonAudioFocus(null)
//        }
    }

    private val mInfoListener = MediaPlayer.OnInfoListener { mp, what, extra ->
        mMediaController.show()
        true
    }

    private val mErrorListener = MediaPlayer.OnErrorListener { mp, what, extra ->
        mCurrentState = STATE_ERROR
        mTargetState = STATE_ERROR
        mMediaController.hide()
        /**
         * Otherwise, pop up an error dialog so the user knows that
         * something bad has happened.Only try and pop up the dialog
         * if we're attached to window.When we're going away and no
         * longer have a window,don't bother showing the user an error
         */
        if (windowToken != null) {
            val r = context.resources
            val messageId: Int
            if (what == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
                messageId = R.string.VideoView_error_text_invalid_progressive_playback
            } else {
                messageId = R.string.VideoView_error_text_unknown
            }

            AlertDialog.Builder(context)
                    .setMessage(messageId)
                    .setPositiveButton(R.string.VideoView_error_button) { dialog, which ->
                        /**If we get here, there is no onError listener,so
                         * at least inform them that the video is over
                         */
                        //回调
                    }
                    .setCancelable(false)
                    .show()
        }
        true
    }

    private val mBufferingUpdateListener = MediaPlayer.OnBufferingUpdateListener { mp, percent ->
        mCurrentBuffferPercentage = percent
    }

    /**
     * Register a callback to be invoked when the media file
     * 各种监听器回调
     */


    private fun release(cleartargetstate: Boolean) {
       if (mMediaPlayer!=null){
           mMediaPlayer!!.reset()
           mMediaPlayer!!.release()
           mMediaPlayer=null
           mCurrentState = STATE_IDLE
           if (cleartargetstate) {
               mTargetState = STATE_IDLE
           }
//           if (mAudioFocusType != AudioManager.AUDIOFOCUS_NONE) {
//               mAudioManager.abandonAudioFocus(null)
//           }
       }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN
                && isInPlaybackState()) {
            //不知道为什么有点不灵敏，要延迟一秒
            //答: 卧槽，给videoView增加了UIsystemVisible的缘故,特么居然连代码都不能全信，要自己试过
            toggleMediaControlsVisibility()
        }
        return super.onTouchEvent(event)
    }

    override fun onTrackballEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN
                && isInPlaybackState()) {
            toggleMediaControlsVisibility()
        }
        return super.onTrackballEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
                keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                keyCode != KeyEvent.KEYCODE_VOLUME_MUTE &&
                keyCode != KeyEvent.KEYCODE_MENU &&
                keyCode != KeyEvent.KEYCODE_CALL &&
                keyCode != KeyEvent.KEYCODE_ENDCALL
        if (isInPlaybackState() && isKeyCodeSupported) {
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE or
                        KeyEvent.KEYCODE_HEADSETHOOK -> {
                    if (mMediaPlayer!!.isPlaying) {
                        pause()
                        mMediaController.show()
                    } else {
                        start()
                        mMediaController.hide()
                    }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    if (!mMediaPlayer!!.isPlaying) {
                        start()
                        mMediaController.hide()
                    }
                    return true
                }
                else -> toggleMediaControlsVisibility()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun toggleMediaControlsVisibility() {
        if (mMediaController.isShowing()) {
            mMediaController.hide()
        } else {
            mMediaController.show()
        }
    }

    override fun start() {
        if (isInPlaybackState()) {
            mMediaPlayer!!.start()
            mCurrentState = STATE_PLAYING
        }
        mTargetState = STATE_PLAYING
    }

    override fun pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer!!.isPlaying) {
                mMediaPlayer!!.pause()
                mCurrentState = STATE_PAUSED
            }
        }
        mTargetState = STATE_PAUSED
    }

    fun suspend() {
        release(false)
    }

    override fun resume() {
        openVideo()
    }

    override fun getDuration(): Int {
        if (isInPlaybackState()) {
            return mMediaPlayer!!.duration
        }
        return -1
    }

    override fun getCurrentPosition(): Int {
        if (isInPlaybackState()) {
            return mMediaPlayer!!.currentPosition
        }
        return 0
    }

    override fun seekTo(mesc: Int) {
        if (isInPlaybackState()) {
            mMediaPlayer!!.seekTo(mesc)
            mSeekWhenPrepared = 0
        } else {
            mSeekWhenPrepared = mesc
        }
    }

    override fun isPlaying(): Boolean {
        return isInPlaybackState() && mMediaPlayer!!.isPlaying
    }

    override fun getBufferPercentage(): Int {
        if (mMediaPlayer!=null){
            return mCurrentBuffferPercentage
        }
        return 0
    }

    private fun isInPlaybackState(): Boolean {
        return mMediaPlayer!=null&&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING
    }
}