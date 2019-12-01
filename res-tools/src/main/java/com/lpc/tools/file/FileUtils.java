package com.lpc.tools.file;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * projectName: ForLike
 * user：Lpc
 * date: 2019-11-29
 * descriptions：文件处理类
 */
public class FileUtils {
    public static void savePhoto(final Context context, final Bitmap bmp, final String imgName,
                                 final SaveResultCallback saveResultCallback) {
        final File sdDir = getSDPath();
        if (sdDir == null) {
            Toast.makeText(context, "设备自带的存储不可用", Toast.LENGTH_LONG).show();
            return;
        }
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1,
                new ThreadFactory() {
                    @Override
                    public Thread newThread(@NonNull Runnable runnable) {
                        return new Thread();
                    }
                });
        scheduledThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                File appDir = new File(sdDir, "knowledge_base");
                if (!appDir.exists()) {
                    appDir.mkdir();
                }
                String fileName;
                if (TextUtils.isEmpty(imgName)) {
                    //设置以当前时间格式为图片名称
                    @SuppressLint("SimpleDateFormat")
                    SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
                    fileName = df.format(new Date()) + ".png";
                } else {
                    fileName = imgName;
                }
                File file = new File(appDir, fileName);
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    fos.close();
                    saveResultCallback.onSavedSuccess(file.getAbsolutePath());
                } catch (FileNotFoundException e) {
                    saveResultCallback.onSavedFailed();
                    e.printStackTrace();
                } catch (IOException e) {
                    saveResultCallback.onSavedFailed();
                    e.printStackTrace();
                }

                //保存图片后发送广播通知更新数据库
                Uri uri = Uri.fromFile(file);
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            }
        });
    }


    private static File getSDPath() {
        File sdDir = null;
        //判断sd卡是否存在
        boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            //获取跟目录
            sdDir = Environment.getExternalStorageDirectory();
        }
        return sdDir;
    }

    public interface SaveResultCallback {
        /**
         * 保存成功
         *
         * @param filePath 路径
         */
        void onSavedSuccess(String filePath);

        /**
         * 保存失败
         */
        void onSavedFailed();
    }

    /**
     * 删除文件
     *
     * @param srcFilePath 文件路径
     * @return {@code true}: 删除成功<br>{@code false}: 删除失败
     */
    public static void deleteFile(String srcFilePath) {
        deleteFile(getFileByPath(srcFilePath));
    }

    /**
     * 删除文件
     *
     * @param file 文件
     * @return {@code true}: 删除成功<br>{@code false}: 删除失败
     */
    public static boolean deleteFile(File file) {
        return file != null && (!file.exists() || file.isFile() && file.delete());
    }

    /**
     * 根据文件路径获取文件
     *
     * @param filePath 文件路径
     * @return 文件
     */
    public static File getFileByPath(String filePath) {
        return TextUtils.isEmpty(filePath) ? null : new File(filePath);
    }
}
