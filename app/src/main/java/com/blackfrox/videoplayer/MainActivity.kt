package com.blackfrox.videoplayer

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initTest()
    }
    private fun initTest() {
        val path= Environment.getExternalStorageDirectory().absolutePath+"/miku片尾福利.mp4"
        videoView.setVideoPath(path)

//        val url = "http://baobab.wdjcdn.com/14564977406580.mp4"
//        videoView.setVideoPath(url)

        //后期要做缓存之类的
        controller.setAnchorView(videoView)
        videoView.setMediaController(controller)
        videoView.start()
    }

    private var lastPosition=0
    override fun onPause() {
        super.onPause()
        videoView.pause()
        lastPosition=videoView.getCurrentPosition()
    }

    override fun onResume() {
        super.onResume()

//        Log.d("VideoView","${videoView.height}")
        //我在想resume方法到底有什么用?
        //恢复进度的办法是用下面三行代码实现的
        //加if判断是因为resume比onPause方法更早执行，第一次lastPosition是0
        if (lastPosition>0){
            videoView.start()
            videoView.seekTo(lastPosition)
        }
//        videoView.resume()
        //恢复需要两秒时间，也不知道bilibili是怎么做到的，可能是架构不同吧
        //明明MediaPlayer中恢复和继续都是start方法，为什么这里不行?
    }
}
