package com.blackfrox.videoplayer

import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import tv.danmaku.ijk.media.player.IMediaPlayer


class MainActivity : AppCompatActivity() {


    private var TAG="MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val path= Environment.getExternalStorageDirectory().absolutePath+"/miku片尾福利.mp4"
        val url = "http://9890.vod.myqcloud.com/9890_4e292f9a3dd011e6b4078980237cc3d3.f30.mp4"

        setSupportActionBar(toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        with(player){
            toolBar =toolbar
           setVideoPath(url)
            title="测试"
        }

//        player.isLock=true
        startButton.setOnClickListener {
            if (player.isPlaying())
                player.pause()
            else player.start()
        }

    }

    override fun onPause() {
        super.onPause()

        player.onPause()
    }


    override fun onResume() {
        super.onResume()

        player.onResume()
    }
}
