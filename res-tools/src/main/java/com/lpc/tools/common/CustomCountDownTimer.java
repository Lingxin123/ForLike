package com.lpc.tools.common;

import android.os.Handler;

/**
 * projectName: ForLike
 * user：Lpc
 * date: 2019-11-25
 * descriptions：倒计时器
 */
public class CustomCountDownTimer implements Runnable {

    private int mCurrentTime;
    private int mCountDownTime;
    private final ICountDownHander mICountDownHander;
    private final Handler mHandler;
    private boolean isRun;

    /**
     * 1、时间每隔一秒回调 确定当前时间 观察者模式
     * 2、支持动态传入总时间
     * 3、倒计时为0时，触发完成回调状态
     */
    public CustomCountDownTimer(int countTime, ICountDownHander iCountDownHander) {
        mHandler = new Handler();
        this.mCurrentTime = countTime;
        this.mCountDownTime = countTime;
        this.mICountDownHander = iCountDownHander;
    }

    @Override
    public void run() {
        if (isRun) {
            if (mICountDownHander != null) {
                mICountDownHander.onTicker(mCurrentTime);
            }
            if (mCurrentTime == 0) {
                cancel();
                if (mICountDownHander != null) {
                    mICountDownHander.onFinish();
                }
            } else {
                mCurrentTime--;
                mHandler.postDelayed(this, 1000);
            }
        }
    }

    public void start() {
        isRun = true;
        mHandler.post(this);
    }

    public void cancel() {
        isRun = false;
        mHandler.removeCallbacks(this);
    }

    public interface ICountDownHander {
        /**
         * 回调当前时间
         *
         * @param currentTime 当前时间
         */
        void onTicker(int currentTime);

        /**
         * 回调完成
         */
        void onFinish();
    }
}
