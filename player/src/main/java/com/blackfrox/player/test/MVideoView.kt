package com.blackfrox.player.test

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Created by Administrator on 2018/3/5 0005.
 */
class MVideoView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet?=null, def: Int=0)
    : FrameLayout(context,attributeSet,def){

    private val mVideoManager by lazy{ MVideoManager.instance}
    private val surfaceView by lazy { SurfaceView(context) }
    init {
        if (context !is Activity) throw Exception("context must be activity")

        val layoutParams= ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT)
        addView(surfaceView,layoutParams)
        //当变为用户不可见时，holder会自动销毁，以节约资源
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {

            override fun surfaceCreated(holder: SurfaceHolder) {

                mVideoManager.mSurfaceHolder=holder
                mVideoManager.openVideo()
            }

            //当画面大小发生改变的时候调用
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {

            }
        })
        isFocusable=true
        isFocusableInTouchMode=true
        requestFocus()
    }

    fun setVideoPath(path: String){
        if (!path.isEmpty())
            setVideoURI(Uri.parse(path))
    }

    fun setVideoURI(uri: Uri){
        with(mVideoManager){
            mUri=uri
            mSeekWhenPrepared=0
            openVideo()
//            requestLayout()
//            invalidate()
        }
    }

    fun start()=
            mVideoManager.start()

    fun pause()=
            mVideoManager.pause()

    fun suspend()=
            mVideoManager.suspend()

    fun resume()=
            mVideoManager.resume()

    fun getDuration()=
            mVideoManager.getDuration()

    fun getCurrentPosition()=
            mVideoManager.getCurrentPosition()

    fun seekTo(mesc: Long)=
            mVideoManager.seekTo(mesc)

    fun isPlaying()=
            mVideoManager.isPlaying()

    fun getBufferPercentage()=
            mVideoManager.getBufferPercentage()


}