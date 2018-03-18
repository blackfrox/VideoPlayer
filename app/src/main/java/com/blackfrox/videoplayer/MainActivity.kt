package com.blackfrox.videoplayer

import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {


    private var TAG="MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val path= Environment.getExternalStorageDirectory().absolutePath+"/miku片尾福利.mp4"
        val url = "http://9890.vod.myqcloud.com/9890_4e292f9a3dd011e6b4078980237cc3d3.f30.mp4"

        setSupportActionBar(toolBar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //        player.toolBar =toolBar

        player.setVideoPath(path)
        startButton.setOnClickListener {
            if (player.isPlaying())
                player.pause()
            else player.start()
        }

        seekBar.setOnSeekBarChangeListener(player.mSeekListener)
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
