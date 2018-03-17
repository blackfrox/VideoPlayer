package com.blackfrox.player;

import android.net.Uri;

import java.net.URI;

import javax.xml.validation.Schema;

/**
 * Created by Administrator on 2018/3/6 0006.
 */

public class S {


    void sd(){
        Uri mURI= Uri.parse("www.230.com");
        String mSchema=mURI.getScheme();
        mSchema.equalsIgnoreCase("file");

    }

}
