package com.blackfrox.player

import android.content.Context
import android.util.AttributeSet

/**
 *
 * Created by Administrator on 2018/2/22 0022.
 */
class BaseVideoPlayer @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, def: Int = 0)
    : DVideoControlView(context, attributeSet, def) {
   override fun onBackFullscreen() {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun startPlayLogic() {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun hideAllWidget() {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun changeUiToPlayingBufferingShow() {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun changeUiToError() {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun changeUiToPauseView() {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun changeUiToPlayingShow() {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun changeUiToPreparingView() {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun changeUiToNormal() {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun onClickUiToggle() {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun dismissBrightnessDialog() {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun dismissVolumeDialog() {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun dismissProgressDialog() {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun showBrightnessDialog(screenBrightness: Float) {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun showVolumeDialog(deltaY: Float, volumePercent: Float) {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun showProgressDialog(deltaX: Float, seekTime: String?, mSeekTimePosition: Long, totalTime: String?, totalTimeDuration: Long) {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override fun showWifiDialog() {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }


}