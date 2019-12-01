package com.lpc.tools.download.apk;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;

/**
 * projectName: ForLike
 * user：Lpc
 * date: 2019-11-29
 * descriptions：文件下载管理器 采用Android自带的DownloaderManager 用广播监听下载完成
 * 权限申请 <uses-permission android:name="android.permission.INTERNET" />
 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
 */
class FileDownLoadManager {

    private DownloadManager mDownloadManager;
    private DownloadSuccess mDownloadSuccess;
    private DownloadReceiver mDownloadReceiver;
    private Context mContext;

    /**
     * 初始化下载管理器对象
     *
     * @param context 上下文
     */
    FileDownLoadManager init(Context context, DownloadSuccess downloadSuccess) {
        this.mDownloadSuccess = downloadSuccess;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            mDownloadManager = context.getSystemService(DownloadManager.class);
        } else {
            mDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        }
        mContext = context.getApplicationContext();
        return getInstance();
    }

    /**
     * 解除注册广播
     */
    void unRegister() {
        mContext.unregisterReceiver(mDownloadReceiver);
        mDownloadReceiver = null;
    }

    /**
     * 单例模式 holder
     * 优点： 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例
     * 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
     */
    private static class SingleHolder {
        @SuppressLint("StaticFieldLeak")
        private static FileDownLoadManager INSTANCE = new FileDownLoadManager();
    }

    /**
     * 私有构造方法 用于单例模式创建对象
     */
    private FileDownLoadManager() {

    }

    /**
     * 用于获取当前类的单例对象
     *
     * @return 单例对象
     */
    static FileDownLoadManager getInstance() {
        return SingleHolder.INSTANCE;
    }

    /**
     * 开启下载任务
     *
     * @param loadUri     下载链接
     * @param loadTitle   下载标题
     * @param loadAppName 下载APP的名称
     * @return 当前下载任务的队列id
     */
    long startDownLoad(String loadUri, String loadTitle, String loadAppName) {
        //设置下载地址 包含下载必要信息
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(loadUri));
        //设置下载时 下载显示的文字
        request.setTitle(loadTitle);
        //设置描述信息
//        request.setDescription("***正在下载");

        //设置文件的保存的位置[三种方式]
        //第一种
        //file:///storage/emulated/0/Android/data/your-package/files/Download/update.apk
//        request.setDestinationInExternalFilesDir(mContext, Environment.DIRECTORY_DOWNLOADS,
//                loadAppName + ".apk");
        //第二种
        //file:///storage/emulated/0/Download/update.apk
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                loadAppName + ".apk");
        //第三种 自定义文件路径
        //req.setDestinationUri()

        //设置一些非必要的属性
        //设置下载网络 流量|WiFi
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE |
                DownloadManager.Request.NETWORK_WIFI);
        //设置漫游网络状态下是否可以下载
        request.setAllowedOverRoaming(false);
        //设置下载时或者下载完成时，通知栏是否显示
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);


        //设置下载文件的mineType
//        request.setMimeType("application/vnd.android.package-archive");
        //将下载请求任务加入队列中 返回当前任务的id

        //注册广播监听
        mDownloadReceiver = new DownloadReceiver();
        mContext.registerReceiver(mDownloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        return mDownloadManager.enqueue(request);
    }

    /**
     * 获取指定下载任务id的下载内容存储位置
     *
     * @param downloadId 下载任务id
     * @return 下载内容的存储位置
     */
    String getDownloadSavePath(long downloadId) {
        //通过id过滤出指定的任务
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        //通过指定任务过滤出游标
        Cursor cursor = mDownloadManager.query(query);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * 通过指定的任务id获取下载文件的Uri地址 用于Android6以下
     *
     * @param downloadId 下载任务id
     * @return 文件uri
     */
    Uri getDownloadUri(long downloadId) {
        return mDownloadManager.getUriForDownloadedFile(downloadId);
    }

    /**
     * 通过指定的任务id获取下载文件的Uri地址 用于Android6 - 7
     *
     * @param downloadId 下载任务id
     * @return 文件uri
     */
    File queryDownloadApk(long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL);
        Cursor cursor = mDownloadManager.query(query);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String uri = cursor.getString(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                    if (!TextUtils.isEmpty(uri)) {
                        return new File(Uri.parse(uri).getPath());
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * 获取指定下载任务id的下载状态
     *
     * @param downloadId 下载任务的id
     * @return 当前的下载状态
     * @see DownloadManager#STATUS_PENDING 下载准备
     * @see DownloadManager#STATUS_RUNNING 下载进行中
     * @see DownloadManager#STATUS_PAUSED 下载暂停
     * @see DownloadManager#STATUS_FAILED 下载失败
     * @see DownloadManager#STATUS_SUCCESSFUL 下载成功
     */
    int getDownloadStatus(long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor cursor = mDownloadManager.query(query);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                }
            } finally {
                cursor.close();
            }
        }
        return -1;
    }


    /**
     * 获取下载器对象
     *
     * @return 下载器对象
     */
    DownloadManager getDownloadManager() {
        return mDownloadManager;
    }

    class DownloadReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mDownloadSuccess != null) {
                mDownloadSuccess.onSuccess(intent);
            }
        }
    }

    /**
     * 下载完成接口回调
     */
    interface DownloadSuccess {
        /**
         * 下载完成
         *
         * @param intent 携带信息
         */
        void onSuccess(Intent intent);
    }

}
