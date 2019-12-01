package com.lpc.tools.download.apk;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.widget.Toast;

import com.lpc.tools.file.FileUtils;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * projectName: ForLike
 * user：Lpc
 * date: 2019-11-29
 * descriptions：下载apk 安装
 */
public class DownloadApk implements FileDownLoadManager.DownloadSuccess {

    private FileDownLoadManager mDownloadManager;
    private String mAppName;
    private String mProviderPath;
    private WeakReference<Activity> mWeakReference;
    private int mIcon;
    private String mMessage;

    private static class SingleHolder {
        private static DownloadApk INSTANCE = new DownloadApk();
    }

    private DownloadApk() {
    }

    public static DownloadApk getInstance() {
        return SingleHolder.INSTANCE;
    }

    /**
     * 下载apk
     *
     * @param context                  上下文
     * @param appIcon                  app icon
     * @param requestPermissionMessage 申请权限的描述 eg:
     *                                 为了正常升级***APP，请点击设置按钮，允许安装未知来源应用，" +
     *                                 "本功能只限用于知识库APP版本升级
     * @param apkUrl                   下载链接
     * @param appName                  下载APP名称
     * @param providerPath             用于Android7.0文件共享处理
     *                                 此处必须同于AndroidManifest中的 <!--android:authorities="${applicationId}.provider"-->
     */
    public void download(Context context, int appIcon, String requestPermissionMessage,
                         String apkUrl, String title, String appName, String providerPath) {
        mWeakReference = new WeakReference<>((Activity) context);
        this.mIcon = appIcon;
        this.mMessage = requestPermissionMessage;
        this.mAppName = appName;
        this.mProviderPath = providerPath;

        mDownloadManager = FileDownLoadManager.getInstance().
                init(mWeakReference.get(), this);

        //获取存储下载apk任务的id
        long downloadApkId = getDownloadId();
        if (downloadApkId != -1) {
            //此情况发生于已下载过 则先比较下载的文件 再决定是否下载
            int downloadStatus = mDownloadManager.getDownloadStatus(downloadApkId);
            //成功就比较两个版本 若正常则安装 若不正常则重新下载
            if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                //获取下载文件的uri
                Uri downloadUri = mDownloadManager.getDownloadUri(downloadApkId);
                if (downloadUri != null) {
                    if (compare(getApkInfo(mWeakReference.get(), downloadUri.getPath()), mWeakReference.get())) {
                        //已存在的文件满足条件 则安装
                        androidInstallPermission(mWeakReference.get(), appIcon, requestPermissionMessage,
                                downloadUri, appName, providerPath);
                        return;
                    } else {
                        //不满足条件 则从管理器中移除该任务id下的文件
                        mDownloadManager.getDownloadManager().remove(downloadApkId);
                    }
                }
            } else {
                FileUtils.deleteFile(mDownloadManager.getDownloadSavePath(downloadApkId));
            }
        }
        //若第一次下载 或者下载未满足要求 则进行重新下载
        startDownload(mWeakReference.get(), apkUrl, title, appName);
    }

    /**
     * 开启下载任务
     *
     * @param context 上下文
     * @param apkUrl  apk下载链接
     * @param appName apk名称
     * @param title   标题
     */
    private void startDownload(Context context, String apkUrl, String title, String appName) {
        long loadId = mDownloadManager.startDownLoad(apkUrl, title, appName);
        getPreferences(context).edit().putLong(DownloadManager.EXTRA_DOWNLOAD_ID, loadId).apply();
    }

    /**
     * Android安装apk权限检测
     *
     * @param context      上下文
     * @param downloadUri  下载文件uri
     * @param appName      APP名称
     * @param providerPath 共享文件路径
     */
    private void androidInstallPermission(final Context context, int appIcon, String requestPermissionMessage,
                                          final Uri downloadUri, final String appName, final String providerPath) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //检测Android8.0上是否有安装权限
            boolean haveInstallPermission = context.getPackageManager().canRequestPackageInstalls();
            if (!haveInstallPermission) {
                //请求安装权限
                AndroidOPermissionActivity.mIconId = appIcon;
                AndroidOPermissionActivity.mMessage = requestPermissionMessage;
                AndroidOPermissionActivity.sListener = new AndroidOInstallPermissionListener() {
                    @Override
                    public void permissionSuccess() {
                        startInstall(context, downloadUri, appName, providerPath);
                    }

                    @Override
                    public void permissionFail() {
                        Toast.makeText(context.getApplicationContext(), "授权失败，无法安装应用", Toast.LENGTH_SHORT).show();
                    }
                };
                Intent intent1 = new Intent(context, AndroidOPermissionActivity.class);
                context.startActivity(intent1);
            } else {
                startInstall(context, downloadUri, appName, providerPath);
            }
        } else {
            startInstall(context, downloadUri, appName, providerPath);
        }
    }

    /**
     * 安装apk
     *
     * @param context      上下文
     * @param downloadUri  下载文件uri
     * @param appName      app name
     * @param providerPath 共享文件路径
     */
    private void startInstall(Context context, Uri downloadUri, String appName, String providerPath) {
        Uri apkUri = downloadUri;
        Intent install = new Intent(Intent.ACTION_VIEW);
        // 由于没有在Activity环境下启动Activity,设置下面的标签
        install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File file = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    , appName + ".apk");
            //判读版本是否在7.0以上
            //参数1 上下文, 参数2 Provider主机地址 和配置文件中保持一致   参数3  共享的文件
            apkUri = FileProvider.getUriForFile(context, providerPath, file);
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            install.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            install.setDataAndType(Uri.fromFile(mDownloadManager.queryDownloadApk(getDownloadId())),
                    "application/vnd.android.package-archive");
        } else {
            install.setDataAndType(Uri.fromFile(new File(apkUri.getPath())),
                    "application/vnd.android.package-archive");
        }
        context.startActivity(install);
    }

    /**
     * 获取apk程序信息[packageName,versionName...]
     *
     * @param context Context
     * @param path    apk path
     */
    private PackageInfo getApkInfo(Context context, String path) {
        PackageInfo info = context.getPackageManager()
                .getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
        if (info != null) {
            return info;
        }
        return null;
    }

    /**
     * 下载的apk和当前程序版本比较
     *
     * @param apkInfo apk file's packageInfo
     * @param context Context
     * @return 如果当前应用版本小于apk的版本则返回true
     */
    private boolean compare(PackageInfo apkInfo, Context context) {
        if (apkInfo == null) {
            return false;
        }
        String localPackage = context.getPackageName();
        if (apkInfo.packageName.equals(localPackage)) {
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(localPackage, 0);
                if (apkInfo.versionCode > packageInfo.versionCode) {
                    return true;
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    private long getDownloadId() {
        return getPreferences(mWeakReference.get()).getLong(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
    }

    /**
     * Android8.0权限申请回调接口
     */
    public interface AndroidOInstallPermissionListener {
        /**
         * 获取权限成功
         */
        void permissionSuccess();

        /**
         * 获取权限失败
         */
        void permissionFail();
    }

    @Override
    public void onSuccess(Intent intent) {
        //  监听下载完成回调
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            Uri downloadUri = mDownloadManager.getDownloadUri(getDownloadId());
            if (downloadUri != null && !TextUtils.isEmpty(downloadUri.getPath())) {
                androidInstallPermission(mWeakReference.get(), mIcon, mMessage, downloadUri, mAppName, mProviderPath);
            }
            mDownloadManager.unRegister();
        } else if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(intent.getAction())) {
            //处理 如果还未完成下载，用户点击Notification ，跳转到下载中心
            Intent viewDownloadIntent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
            viewDownloadIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mWeakReference.get().startActivity(viewDownloadIntent);
        }
    }
}
