package com.blackfrox.player.ijkMedia

import android.annotation.TargetApi
import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Build
import android.support.annotation.NonNull
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.blackfrox.player.ijkMedia.IRenderView.IRenderCallback
import com.blackfrox.player.util.formatDate
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.ISurfaceTextureHolder
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Administrator on 2018/3/5 0005.
 *
 * 使用ConcurrentHashMap保证线程安全，
 * 将callback都添加到这个并行集合中，有序执行,
 * 作用: 线程安全，可以添加多个callback(处理surfaceHolder.Callback)
 */
class SurfaceRenderView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet?=null, def: Int=0)
    : SurfaceView(context,attributeSet,def),IRenderView{

    private var mMeasureHelper: MeasureHelper
    private var mSurfaceCallback: SurfaceCallback
    init {
        mMeasureHelper= MeasureHelper(this)
        mSurfaceCallback=SurfaceCallback(this)
        holder.addCallback(mSurfaceCallback)
        //noinspection deprecation
        holder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL)
    }

    override fun getView(): View {
        return this
    }

    override fun shouldWaitForResize(): Boolean {
        return true
    }

    override fun setVideoSize(videoWidth: Int, videoHeight: Int) {
        if (videoWidth>0 && videoHeight>0){
            mMeasureHelper.setVideoSize(videoWidth,videoHeight)
            holder.setFixedSize(videoWidth,videoHeight)
            requestLayout()
        }
    }

    override fun setVideoSampleAspectRatio(videoSarNum: Int, videoSarDen: Int) {
        if (videoSarDen>0 && videoSarNum>0){
            mMeasureHelper.setVideoSampleAspectRatio(videoSarNum, videoSarDen)
            requestLayout()
        }
    }

    override fun setVideoRotation(degree: Int) {
        Log.e("","SurfaceView doesn't support rotation($degree)!\n")
    }

    override fun setAspectRatio(aspectRatio: Int) {
        mMeasureHelper.setAspectRatio(aspectRatio)
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mMeasureHelper.doMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(mMeasureHelper.measuredWidth,mMeasureHelper.measuredHeight)
    }


    //- - -- - - - -- -
    // SurfaceViewHolder
    //- - - - - - - - -

    //就是一个类，拥有成员mSurfaceView和mSurfaceHolder
    private class InternalSurfaceHolder(  private var mSurfaceView: IRenderView,
                                          private var mSurfaceHolder: SurfaceHolder?): IRenderView.ISurfaceHolder{

        override fun bindToMediaPlayer(mp: IMediaPlayer?){
            if (mp!=null){
                if ((Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN)&&
                        (mp is ISurfaceTextureHolder)){
                    val textureHolder=mp as ISurfaceTextureHolder
                    textureHolder.surfaceTexture=null
                }
                mp.setDisplay(mSurfaceHolder)
            }
        }

        override fun getRenderView(): IRenderView {
            return mSurfaceView
        }

        override fun getSurfaceHolder(): SurfaceHolder? {
            return mSurfaceHolder
        }

        override fun getSurfaceTexture(): SurfaceTexture? {
            return null
        }

        override fun openSurface(): Surface? {
            if (mSurfaceHolder==null)
                return null
            return mSurfaceHolder!!.surface
        }
    }
    //- - -- - - - -- -
    // SurfaceHolder.Callback
    //- - - - - - - - -

    override fun addRenderCallback(callback: IRenderCallback) {
        mSurfaceCallback.addRenderCallback(callback)
    }

    override fun removeRenderCallback(callback: IRenderCallback) {
        mSurfaceCallback.removeRenderCallback(callback)
    }

    //这个SurfaceCallback到底做了什么?
    private class SurfaceCallback(surfaceView: SurfaceRenderView): SurfaceHolder.Callback{
        private var mSurfaceHolder: SurfaceHolder?=null
        private var mIsFormatChanged: Boolean=false
        private var mFormat: Int=0
        private var mWidth: Int=0
        private var mHeight: Int=0

        private var mWeakSurfaceView: WeakReference<SurfaceRenderView>
        private val mRenderCallbackMap = ConcurrentHashMap<IRenderCallback,Any>()

        init {
            mWeakSurfaceView= WeakReference<SurfaceRenderView>(surfaceView)

        }

        fun addRenderCallback(@NonNull callback: IRenderCallback){
            mRenderCallbackMap.put(callback,callback)

            var surfaceHolder: IRenderView.ISurfaceHolder?=null
            if (mSurfaceHolder!=null){
                if (surfaceHolder==null)
                    surfaceHolder=InternalSurfaceHolder(mWeakSurfaceView.get()!!,mSurfaceHolder)
                callback.onSurfaceCreated(surfaceHolder,mWidth,mHeight)
            }

            if (mIsFormatChanged){
                if (surfaceHolder==null)
                    surfaceHolder=InternalSurfaceHolder(mWeakSurfaceView.get()!!,mSurfaceHolder)
                callback.onSurfaceChanged(surfaceHolder,mFormat,mWidth,mHeight)
            }
        }

        fun removeRenderCallback(@NonNull callback: IRenderCallback){
            mRenderCallbackMap.remove(callback)
        }

        override fun surfaceCreated(holder: SurfaceHolder?) {
            mSurfaceHolder=holder
            mIsFormatChanged=false
            mFormat=0
            mWidth=0
            mHeight=0

            val surfaceHolder=InternalSurfaceHolder(mWeakSurfaceView.get()!!,mSurfaceHolder)

            for (renderCallback in mRenderCallbackMap){
                renderCallback.key.onSurfaceCreated(surfaceHolder,0,0)
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            mSurfaceHolder=null
            mIsFormatChanged=false
            mFormat=0
            mWidth=0
            mHeight=0

            val surfaceHolder=InternalSurfaceHolder(mWeakSurfaceView.get()!!,mSurfaceHolder)

            for (renderCallback in mRenderCallbackMap){
                renderCallback.key.onSurfaceDestroyed(surfaceHolder)
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            mSurfaceHolder=holder
            mIsFormatChanged=true
            mWidth=width
            mHeight=height
            mFormat=format

            val surfaceHolder=InternalSurfaceHolder(mWeakSurfaceView.get()!!,mSurfaceHolder)
            for (renderCallback in mRenderCallbackMap){
                renderCallback.key.onSurfaceChanged(surfaceHolder,format,width,height)
            }
        }
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent?) {
        super.onInitializeAccessibilityEvent(event)
        event?.className=SurfaceRenderView::class.java.name
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo?) {
        super.onInitializeAccessibilityNodeInfo(info)
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            info?.className=SurfaceRenderView::class.java.name
    }
}