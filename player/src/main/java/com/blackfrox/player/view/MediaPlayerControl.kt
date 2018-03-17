package com.blackfrox.player.view

import android.view.View

/**
 * Created by Administrator on 2017/11/21 0021.
 * 类似MVP中的View接口，将像Presenter调用的View一样
 */
interface MediaPlayerControl {

    fun getDuration(): Int

    fun getCurrentPosition(): Long

    fun getBufferPercentage(): Int
    fun start()

    fun isPlaying(): Boolean

    fun pause()

    fun resume()
    fun seekTo(pos: Long)
//    fun show()
//    fun setEnabled(b: Boolean)
//    fun setMediaPlayer(player: MediaPlayerControl)

    //因为方法找不到了，所以不写了
//    fun canPause(): Boolean

}