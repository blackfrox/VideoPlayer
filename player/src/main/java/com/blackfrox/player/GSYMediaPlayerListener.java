package com.blackfrox.player;

public interface GSYMediaPlayerListener {
    void onPrepared();

    void onAutoCompletion();

    void onCompletion();

    void onBufferingUpdate(int percent);

    void onSeekComplete();

    void onError(int what, int extra);

    void onInfo(int what, int extra);

    void onVideoSizeChanged();

    //增加的回调，mediaPlayer中是没有写进去调用的
    void onBackFullscreen();

    void onVideoPause();

    void onVideoResume();
}
