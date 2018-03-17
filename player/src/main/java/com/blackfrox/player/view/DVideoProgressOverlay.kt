package com.blackfrox.player.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.blackfrox.player.R
import com.blackfrox.player.util.StringUtils
import kotlinx.android.synthetic.main.video_overlay_progress.view.*

/**
 * Created by Administrator on 2018/2/15 0015.
 */
class DVideoProgressOverlay @JvmOverloads constructor(context: Context, attributeSet: AttributeSet?=null, def: Int=0)
    : FrameLayout(context,attributeSet,def) {


    private var mDuration=-1
    private var mDelSeekDialogProgress=-1
    private var mSeekDialogStartProgress=-1

    init {
        LayoutInflater.from(context)
                .inflate(R.layout.video_overlay_progress,this)
    }

    /**
     * 显示进度框
     *
     * @param delProgress 进度变化值
     * @param curPosition  player 当前进度
     * @param duration player 总长度
     */
    fun show(delProgress: Int,curProgress: Int,duration: Int){
        if(duration<=0) return

        //获取第一次显示时的开始进度
        if (mSeekDialogStartProgress==-1){
            mSeekDialogStartProgress=curProgress
        }

        if (visibility!= View.VISIBLE)
            visibility= View.VISIBLE

        mDuration=duration
        mDelSeekDialogProgress-=delProgress
        val targetProgress=getTargetProgress()
        if (delProgress>0){
            //回退
            tv_add_time.text="-${duration}秒"
        }else{
            //前进
            tv_add_time.text="+${duration}秒"
        }
        mSeekCurProgress.text=StringUtils.stringForTime(targetProgress)
        mSeekDuration.text=StringUtils.stringForTime(duration)
    }

    /**
     * 滑动获取结束后的目标进度
     */
    fun getTargetProgress(): Int {
        if (mDuration==-1){
            return -1
        }

        var newSeekProgress=mSeekDialogStartProgress+mDelSeekDialogProgress
        if (newSeekProgress<=0) newSeekProgress=0
        if (newSeekProgress>mDuration) newSeekProgress=mDuration
        return newSeekProgress
    }

    fun hide(){
        mDuration=-1
        mSeekDialogStartProgress=-1
        mDelSeekDialogProgress=-1
        visibility=GONE
    }
}