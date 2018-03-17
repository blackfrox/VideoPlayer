package com.blackfrox.player.test

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.SurfaceHolder
import com.blackfrox.player.MyApp
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer

/**
 * Created by Administrator on 2018/3/4 0004.
 * 将VideoView中关于MediaPlayer的方法抽出来
 */
class MVideoManager(var context: Context=MyApp.instance) {

    companion object {
        val instance by lazy { MVideoManager() }

        private val STATE_ERROR = -1
        private val STATE_IDLE = 0
        private val STATE_PREPARING = 1 //openVideo()
        private val STATE_PREPARED = 2
        private val STATE_PLAYING = 3 //调用start（）
        private val STATE_PAUSED = 4
        private val STATE_PLAYBACK_COMPLETED = 5
    }
    // mCurrentState is VideoView object's current state.
    //mTargetState is the state that a method caller intends of reach.
    //For instance, regardless the VideoView object's current state,
    //calling pause() intends to bring the object to a target state
    //of STATE_PAUSED.
    private var mCurrentState= STATE_IDLE
    private var mTargetState= STATE_IDLE

    //当前缓冲进度比
    private var mCurrentBufferPercentage: Int=0

    var mMediaPlayer: IMediaPlayer?= null
    var mSurfaceHolder: SurfaceHolder?=null
    var mUri: Uri?=null
    var mSeekWhenPrepared=0L

    var mVideoWidth: Int
    var mVideoHeight: Int
    var mOnPreparedListener: IMediaPlayer.OnPreparedListener?=null
    var mOnCompletionListener: IMediaPlayer.OnCompletionListener?=null
    var mOnErrorListener: IMediaPlayer.OnErrorListener?=null
    var mOnInfoListener: IMediaPlayer.OnInfoListener?=null

    init {
        mVideoWidth = 0
        mVideoHeight = 0
        mCurrentState= STATE_IDLE
        mTargetState= STATE_IDLE
    }

    fun stopPlayback(){
        if(mMediaPlayer!=null){
            with(mMediaPlayer!!){
                stop()
                release()
                mCurrentState= STATE_IDLE
                mTargetState= STATE_IDLE
            }
        }
    }

    fun openVideo(){
        if (mUri==null&&mSurfaceHolder==null){
            //not ready for playback just yet,will try again later
            return
        }

        //we shouldn't clear the target state,because somebody might have
        //called start() previously
        release(false)

        try {
            mMediaPlayer=IjkMediaPlayer()
            //解决进度跳转不正确
//            (mMediaPlayer as IjkMediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1)
            with(mMediaPlayer!!){

                setOnPreparedListener(mPreparedListener)
                setOnVideoSizeChangedListener(mSizeChangedListener)
                setOnCompletionListener(mCompletionListener)
                setOnErrorListener(mErrorListener)
                setOnInfoListener(mInfoListener)
                setOnBufferingUpdateListener(mBufferingUpdateListener)

                setDataSource(context,mUri)
                setDisplay(mSurfaceHolder)
                setScreenOnWhilePlaying(true)
                prepareAsync()
            }
            //We don't set the target state here either, but preserve the
            //target state that was there before
            mCurrentState= STATE_PREPARING

        }catch (e: Exception){
            e.printStackTrace()
            mCurrentState= STATE_ERROR
            mTargetState= STATE_ERROR
            mErrorListener.onError(mMediaPlayer,IMediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
            return
        }catch (e: IllegalArgumentException){
            e.printStackTrace()
            mCurrentState= STATE_ERROR
            mTargetState= STATE_ERROR
            mErrorListener.onError(mMediaPlayer,IMediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
            return
        }
    }


    private val mSizeChangedListener=IMediaPlayer.OnVideoSizeChangedListener{ mp: IMediaPlayer, width: Int, height: Int, sar: Int, dum: Int ->
        mVideoWidth=mp.videoWidth
        mVideoHeight=mp.videoHeight

    }

    private val mPreparedListener=IMediaPlayer.OnPreparedListener{
        mCurrentState= STATE_PREPARED

        mVideoWidth=it.videoWidth
        mVideoHeight=it.videoHeight

        mOnPreparedListener?.onPrepared(mMediaPlayer)

        //mSeekWhenPrepared may be changed after seekTo() call
        val seekToPosition=mSeekWhenPrepared
        //判断是否调用了seekTo()
        if (seekToPosition!=0L)
            seekTo(seekToPosition)

        if (mTargetState== STATE_PLAYING){
            start()
        }

    }

    private val mCompletionListener=IMediaPlayer.OnCompletionListener{
        mCurrentState= STATE_PLAYBACK_COMPLETED
        mTargetState= STATE_PLAYBACK_COMPLETED

        mOnCompletionListener?.onCompletion(mMediaPlayer)
    }

    private val mInfoListener=IMediaPlayer.OnInfoListener{ _: IMediaPlayer, _: Int, _: Int ->
        true
    }

    private val mErrorListener=IMediaPlayer.OnErrorListener{ iMediaPlayer: IMediaPlayer, i: Int, i1: Int ->
        mCurrentState= STATE_ERROR
        mTargetState= STATE_ERROR

        mOnErrorListener?.onError(iMediaPlayer,i,i1)
        true
    }

    private val mBufferingUpdateListener=IMediaPlayer.OnBufferingUpdateListener{ iMediaPlayer: IMediaPlayer, percent: Int ->
        mCurrentBufferPercentage=percent
    }

    private fun release(clearTargetState: Boolean){
        if(mMediaPlayer!=null){
            mMediaPlayer!!.reset()
            mMediaPlayer!!.release()
            mMediaPlayer=null
            mCurrentState= STATE_IDLE
            if (clearTargetState)
                mTargetState= STATE_IDLE

        }
    }

    fun start(){
        if (isInPlaybackState()){
            mMediaPlayer!!.start()
            mCurrentState= STATE_PLAYING
        }
        mTargetState= STATE_PLAYING
    }

    fun pause(){
        if (isInPlaybackState()){
            if (mMediaPlayer!!.isPlaying){
                mMediaPlayer!!.pause()
                mCurrentState= STATE_PAUSED
            }
        }
        mTargetState= STATE_PAUSED
    }

    fun suspend()=release(false)

    fun resume()=
            openVideo()

    fun getDuration(): Long {
        if (isInPlaybackState())
            return mMediaPlayer!!.duration

        return -1
    }

    fun getCurrentPosition(): Long {
        if (isInPlaybackState())
            return mMediaPlayer!!.currentPosition

        return 0L
    }

    fun seekTo(mesc: Long){
        if (isInPlaybackState()){
            mMediaPlayer!!.seekTo(mesc)
            mSeekWhenPrepared=0
        }else{//当mMediaplayer还没有准备好时，保存mesc，准备好后会在onPreparedListener中自动调用seek()
            mSeekWhenPrepared=mesc
        }
    }

    fun isPlaying()=
            isInPlaybackState()&&mMediaPlayer!!.isPlaying



    fun getBufferPercentage(): Int {
        if (mMediaPlayer!=null){
            return mCurrentBufferPercentage
        }
        return 0
    }

    private fun isInPlaybackState()=
            mMediaPlayer!=null&&
                    mCurrentState!= STATE_ERROR&&
                    mCurrentState!= STATE_IDLE&&
                    mCurrentState!= STATE_PREPARING


}