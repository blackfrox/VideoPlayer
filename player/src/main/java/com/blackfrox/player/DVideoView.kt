package com.blackfrox.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import com.blackfrox.player.util.Common
import tv.danmaku.ijk.media.player.IMediaPlayer

/**
 * Created by Administrator on 2018/2/17 0017.
 *
 * (VideoView自带的问题)从后台恢复前台 问题: 1 暂停的时候,画面是黑屏(点击播放是会有de的声音)
 *                       2 播放的时候，有de的声音
 *                       答:使用IjkMediaPlayer就可以解决这个问题
 *
 * 视频回调处理相关层
 */
abstract class DVideoView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet?=null, def: Int=0)
    :FrameLayout(context,attributeSet,def), GSYMediaPlayerListener {

    private val TAG="DVideoView"
    companion object {
        //当调用方法或者监听器调用时相应的状态
        //错误
        val STATE_ERROR=-1
        //初始化
        val STATE_NORMAL=0 //调用setUp()，onVideoReset(),onCompletion()
        //准备中
        val STATE_PREPARING=1
//        val STATE_PREPARED=2
        //播放中
        val STATE_PLAYING=3  //onVideoResume(),onPrepared()
        //开始缓冲
        val  STATE_PLAYING_BUFFERING_START=2
        //暂停
        val STATE_PAUSE=4
        val STATE_PLAYBACK_COMPLETED=5
    }

    //避免切换时频繁setup
    val CHANGE_DELAY_TIME=2000

   protected var mCurrentState=-1 //只有当前方法逻辑执行才会改变

    //屏幕宽高
    protected var mScreenWidth:Int
    protected var mScreenHeight:Int

    //保存暂停时的时间(暂停了多长时间在pause的时候开始算起)
    protected var mPauseTime=0L

    //当前的播放位置
    protected var mCurrentPosition=0L

    //保存切换时的时间,避免频繁切换
    protected var mSaveChangeViewTime=0

    //是否播放过 (这个不就是初始化的意思么，总感觉又是多余的变量)
    protected var mHadPlay=false

    //备份缓存前的播放状态
    protected var mBackUpPlayingBufferState: Int=-1

    //标题
    protected var mTitle=""
    protected var mUrl=""

    //(2.19)今天开始添加的
    protected val mVideoManager by lazy { DVideoManager.instance }
    protected lateinit var mSurface: Surface

    protected fun getActivityContext()= Common.getActivityContext(context)

    protected val mAudioManager by lazy {  context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    protected val surfaceView by lazy { SurfaceView(context) }
    init {

        if (context !is Activity) throw Exception("context must be activity")

        val layoutParams=ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT)
        addView(surfaceView,layoutParams)
        //当变为用户不可见时，holder会自动销毁，以节约资源
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {

            override fun surfaceCreated(holder: SurfaceHolder) {

                mSurface=holder.surface
                mVideoManager.setDisplay(mSurface)

            }

            //当画面大小发生改变的时候调用
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {

                //清空释放
                mVideoManager.setDisplay(null)
                holder?.surface?.release()
            }
        })

        initInflate(context)

        mScreenWidth=context.resources.displayMetrics.widthPixels
        mScreenHeight=context.resources.displayMetrics.heightPixels
    }

    private fun pauseLogic(surface: Surface, pauseLogic: Boolean) {
        mSurface=surface
        if (pauseLogic)
            showPauseCover()
        mVideoManager.setDisplay(mSurface)
    }

    open fun showPauseCover(){ }

    private fun initInflate(context: Activity) {
        try {
            View.inflate(context,getLayoutId(),this)
        }catch (e: InflateException){
            e.printStackTrace()
        }
    }

     //开始播放逻辑
    protected fun startButtonLogic(){
         prepareVideo()
     }

    /**
     * 开始状态视频播放逻辑，当前是回调onPrepared()
     */
    private fun prepareVideo() {
        mVideoManager.listener()?.onCompletion()

        with(mVideoManager){
            setListener(this@DVideoView)
            playTag=this@DVideoView.mPlayTag
            playPosition=this@DVideoView.mPlayPosition
            mAudioManager.requestAudioFocus(onAudioFocusChangeListener,AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            (context as Activity).window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            prepare(mUrl)
            setStateAndUi(STATE_PREPARING)
        }
    }

    /**
     * 监听是否有外部其他多媒体开始播放
     */
    protected val onAudioFocusChangeListener=AudioManager.OnAudioFocusChangeListener{
        when(it){
            AudioManager.AUDIOFOCUS_LOSS->{
                releaseAllVideos()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT->{
                try {
                    if (mVideoManager.mediaPlayer.isPlaying){
                        mVideoManager.mediaPlayer.pause()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }


    /**
     * 设置播放URL
     */
   open fun setUp(url: String,title: String): Boolean {
        mUrl=url
        if (isCurrentMediaListener()&&
                (System.currentTimeMillis()-mSaveChangeViewTime)<CHANGE_DELAY_TIME)
            return false

        mTitle=title
        setStateAndUi(STATE_NORMAL)
        return true
    }

    /**
     * 重置
     */
    fun onVideoReset(){
        setStateAndUi(STATE_NORMAL)
    }

    override fun onVideoPause() {
        try {
            if (mVideoManager.mediaPlayerIsCreated&&
                    mVideoManager.mediaPlayer.isPlaying){
                setStateAndUi(STATE_PAUSE)
                mPauseTime=System.currentTimeMillis()
                mCurrentPosition=mVideoManager.mediaPlayer.currentPosition
                mVideoManager.mediaPlayer.pause()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onVideoResume() {
        mPauseTime=0
        if (mCurrentState== STATE_PAUSE){
            try {
                if (mCurrentPosition>0&&mVideoManager.mediaPlayerIsCreated){
                    setStateAndUi(STATE_PLAYING)
                    mVideoManager.mediaPlayer.seekTo(mCurrentPosition)
                    mVideoManager.mediaPlayer.start()
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    override fun onPrepared() {
        if (mCurrentState!= STATE_PREPARING) return

        try {
            with(mVideoManager){
                mediaPlayer.start()

                setStateAndUi(STATE_PLAYING)

                if (mSeekOnStart>0){
                    mediaPlayer.seekTo(mSeekOnStart)
                    mSeekOnStart=0
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
        mHadPlay=true
    }

    override fun onAutoCompletion() {

    }
    override fun onCompletion() {
        //make me normal first
        setStateAndUi(STATE_NORMAL)

        mSaveChangeViewTime=0

        with(mVideoManager){
            if (!mIfCurrentIsFullscreen){
                setListener(null)
                setLastListener(null)
            }
            currentVideoWidth=0
            currentVideoHeight=0

            mAudioManager.abandonAudioFocus(onAudioFocusChangeListener)
            //不知道这句到底需不需要加，mediaPlayer.screenOnWhilePlaying(true)这句话就有这功能
            (context as Activity).window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onSeekComplete() {

    }

    override fun onError(what: Int, extra: Int) {

        if (what!=38&&what!=-38){
            setStateAndUi(STATE_ERROR)

        }
    }

    override fun onInfo(what: Int, extra: Int) {
        when(what){
            MediaPlayer.MEDIA_INFO_BUFFERING_START->{
                mBackUpPlayingBufferState=mCurrentState
                //避免在onPrepared之前就进入了buffering，导致一直loading
                if (mHadPlay&&mCurrentState!= STATE_PREPARING&&mCurrentState>0)
                    setStateAndUi(STATE_PLAYING_BUFFERING_START)
            }
            MediaPlayer.MEDIA_INFO_BUFFERING_END->{
                if (mBackUpPlayingBufferState!=-1){
                    if (mHadPlay&&mCurrentState!= STATE_PREPARING&&mCurrentState>0)
                        setStateAndUi(mBackUpPlayingBufferState)

                    mBackUpPlayingBufferState=-1
                }
            }
            IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED->{
//                mRotate=extra
//                surfaceView.rotation=mRotate
            }
        }
    }

    override fun onVideoSizeChanged() {
        val mVideoWidth=mVideoManager.currentVideoWidth
        val mVideoHeight=mVideoManager.currentVideoHeight
        if (mVideoWidth!=0&&mVideoHeight!=0)
            surfaceView.requestLayout()
    }

    /**
     * 获取当前播放进度
     */
    fun getCurrentPositionWhenPlaying(): Long {
        var position=0L
        if (mCurrentState== STATE_PLAYING||mCurrentState== STATE_PAUSE){
            try {
                position=mVideoManager.mediaPlayer.currentPosition
            }catch (e : Exception){
                e.printStackTrace()
                return position
            }
        }
        return position
    }

    /**
     * 获取当前总时长
     */
    fun getDuration(): Long {
        var duration=0L
        try{
            duration=mVideoManager.mediaPlayer.duration
        }catch (e: Exception){
            e.printStackTrace()
            return duration
        }
        return duration
    }

    /**
     * 页面销毁记得调用是否所有的video
     */
    fun releaseAllVideos(){
        mVideoManager.listener()?.onCompletion()
        mVideoManager.releaseMediaPlayer()
    }

    /**
     * 释放
     */
    fun release(){
        mSaveChangeViewTime=0
        if (isCurrentMediaListener()&&
                (System.currentTimeMillis()-mSaveChangeViewTime)>CHANGE_DELAY_TIME)
            releaseAllVideos()
    }

    protected fun isCurrentMediaListener()=
            mVideoManager.listener()!=null&&
                    mVideoManager.listener()==this


                                 //    需要继承的部分

    /***
     * 设置播放显示状态
     */
    protected abstract fun setStateAndUi(state: Int)

    /**
     * 当前UI
     */
    abstract fun getLayoutId():Int

    /**
     * 开始播放
     */
    abstract fun startPlayLogic()

                           //公开接口
    /**
     * 获取当前播放状态
     */
    fun getCurrentState()=mCurrentState

    var mPlayTag=""

    var mPlayPosition=-22

    //从哪个开始播放
    var mSeekOnStart=0L

    //当前是否全屏
    var mIfCurrentIsFullscreen=false

//    var mVideoAllCallBack: VideoAllCallback?=null

    fun seekTo(position: Long){
        try {
            if (position>0L){
                mVideoManager.mediaPlayer.seekTo(position)
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

}