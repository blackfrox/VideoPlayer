package com.blackfrox.player

import android.text.TextUtils
import android.view.Surface
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.lang.ref.WeakReference

/**
 * Created by Administrator on 2018/2/19 0019.
 */
class DVideoManager {
    companion object {
        val instance by lazy { DVideoManager() }
    }

    //具体逻辑都写在videoView里，弱引用在这里的作用是什么?是为了在后台播放节省资源?
    private var listener: WeakReference<GSYMediaPlayerListener>? = null
    private lateinit var url:String

    private var screenWidth=0
    private var screenHeight=0

    fun setDisplay(surface: Surface?) {
        if (mediaPlayerIsCreated){
            if (surface==null)
                mediaPlayer.setSurface(null)
            else if (surface.isValid)
                mediaPlayer.setSurface(surface)
        }
    }

    fun prepare(url: String){
        if (TextUtils.isEmpty(url)) return
        this@DVideoManager.url=url

        initVideo()
    }

    fun pause(){
        listener()?.onVideoPause()
    }

    fun resume()=listener()?.onVideoResume()

    private fun initVideo() {
        try {
            screenWidth=0
            screenHeight=0
            if (mediaPlayerIsCreated)
                mediaPlayer.release()

            //每次使用都必须重新创建MediaPlayer
            mediaPlayer=IjkMediaPlayer()
            mediaPlayerIsCreated=true
            mediaPlayer.setDataSource(url)
            with(mediaPlayer){
                setOnPreparedListener {
                    listener()?.onPrepared()
                }
                setOnCompletionListener { listener()?.onAutoCompletion()}
                setOnBufferingUpdateListener { iMediaPlayer, percent ->
                    if (listener!=null){
                        if (percent>buffterPoint){
                            listener()?.onBufferingUpdate(percent)
                        }else{
                            listener()?.onBufferingUpdate(buffterPoint)
                        }
                    }
                }
                setOnSeekCompleteListener {
                    listener()?.onSeekComplete()
                }
                setOnErrorListener { iMediaPlayer, what, extra ->
                    listener()?.onError(what,extra)
                    true
                }
                setOnInfoListener { iMediaPlayer, what, extra ->

                    listener()?.onInfo(what, extra)
                    false
                }
                setOnVideoSizeChangedListener { iMediaPlayer, _, _, _, _ ->
                    currentVideoWidth=iMediaPlayer.videoWidth
                    currentVideoHeight=iMediaPlayer.videoHeight
                    listener()?.onVideoSizeChanged()
                }
                setScreenOnWhilePlaying(true)
                prepareAsync()
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    //怎么实现下面变量对外只读，不能改写
    lateinit var mediaPlayer: IMediaPlayer
    var mediaPlayerIsCreated=false //替代java中对于mediaPlayer的null判断（原因：kotlin中null比java中麻烦了）

    var currentVideoHeight = 0

    var currentVideoWidth = 0

    fun listener() = if (listener == null)
        null else listener!!.get()

    fun setListener(listener: GSYMediaPlayerListener?) =
            if (listener == null)
                this@DVideoManager.listener = null
            else this@DVideoManager.listener = WeakReference(listener)

    var playTag =""

    var playPosition =-22

    var buffterPoint=0
    fun setLastListener(nothing: Nothing?) {

    }

    fun releaseMediaPlayer() {
        playTag=""
        playPosition=-22
        mediaPlayer.release()
    }
}