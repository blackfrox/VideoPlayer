package com.blackfrox.videoplayer

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.widget.MediaController
import com.blackfrox.player.util.OrientationUtils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        initTest()
    }



    private lateinit var orientaionUtil: OrientationUtils
    private fun initTest() {
        val path= Environment.getExternalStorageDirectory().absolutePath+"/miku片尾福利.mp4"

        val controller= MediaController(this)
//        controller.setAnchorView(videoView)
//        val controller=AndroidMediaController(this)
//        videoView.setMediaController(controller)
//        videoView.setVideoPath(path)
//        videoView.start()

//        val url = "http://baobab.wdjcdn.com/14564977406580.mp4"
//        videoView.setVideoPath(url)
//        val controller=MediaController(this)
//        videoView.setMediaController(controller)

//        orientaionUtil= OrientationUtils(this)
//
//        videoPlayer.getBackButton().setOnClickListener {
//               onBackPressed()
//         }
//        videoPlayer.getFullButton().setOnClickListener {
//            orientaionUtil.resolveByClick()
//        }
    }


    override fun onBackPressed() {

        //先返回正常状态
        if (orientaionUtil.getScreenType() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            //点击了控件
//            controller.getFullButton().performClick();
            orientaionUtil.resolveByClick()
            return;
        }else{
            return super.onBackPressed()
        }
    }

//    override fun onConfigurationChanged(newConfig: Configuration) {
//
//        when(resources.configuration.orientation){
//           Configuration.ORIENTATION_LANDSCAPE ->{
//               val param=videoPlayer.layoutParams
//               param.width=ViewGroup.LayoutParams.MATCH_PARENT
//               param.height=ViewGroup.LayoutParams.MATCH_PARENT
//               videoPlayer.layoutParams=param
//            }
//            else ->{
//                val param=videoPlayer.layoutParams
//                param.width=ViewGroup.LayoutParams.MATCH_PARENT
//                param.height=DisplayUtil.dp2px(this,240F)
//                videoPlayer.layoutParams=param
//            }
//        }
//        super.onConfigurationChanged(newConfig)
//    }
    private var lastPosition=0
    override fun onPause() {
        super.onPause()
//        videoView.pause()
//        lastPosition=videoView.getCurrentPosition()
    }

    override fun onResume() {
        super.onResume()
        //我在想resume方法到底有什么用?
        //恢复进度的办法是用下面三行代码实现的
        //加if判断是因为resume比onPause方法更早执行，第一次lastPosition是0
//        if (lastPosition>0){
//            videoView.seekTo(lastPosition)
//            videoView.start()
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        orientaionUtil.releaseListener()
    }
}
