package com.blackfrox.player.util

import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Administrator on 2017/9/19.
 */



fun formatDate(date: Date): String {
        val formatter= SimpleDateFormat(
                "yyyy年MM月dd日  HH:mm")
        return formatter.format(date)
 }

fun Context.toast(text:String){
    Toast.makeText(this,text,Toast.LENGTH_SHORT).show()
}

fun Context.longToast(text:String){
    Toast.makeText(this,text,Toast.LENGTH_LONG).show()
}


