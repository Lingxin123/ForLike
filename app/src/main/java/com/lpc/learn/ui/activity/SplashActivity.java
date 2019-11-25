package com.lpc.learn.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.lpc.learn.MainActivity;
import com.lpc.learn.R;
import com.lpc.tools.common.CustomCountDownTimer;
import com.lpc.tools.res.view.video.FullScreenVideoView;

import java.io.File;

public class SplashActivity extends AppCompatActivity {

    private FullScreenVideoView mFullScreenVideoView;
    private TextView mTvTimer;
    private CustomCountDownTimer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        initView();
        initVideo();
        initTimer();
    }

    private void initView() {
        mFullScreenVideoView = findViewById(R.id.vv_play);
        mTvTimer = findViewById(R.id.tv_splash_timer);
        mTvTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //点击跳过，进入主页
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            }
        });
    }

    private void initVideo() {
        //指定文件路径
        mFullScreenVideoView.setVideoURI(Uri.parse("android.resource://" +
                getPackageName() + File.separator + R.raw.splash));
        mFullScreenVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mFullScreenVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.start();
            }
        });
    }

    private void initTimer() {
        mTimer = new CustomCountDownTimer(5, new CustomCountDownTimer.ICountDownHander() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onTicker(int currentTime) {
                mTvTimer.setText(currentTime + "秒");
            }

            @Override
            public void onFinish() {
                mTvTimer.setText("跳过");
            }
        });
        mTimer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTimer.cancel();
    }

    @Override
    public void onBackPressed() {
        //取消在闪屏页面时点击返回键
    }
}
