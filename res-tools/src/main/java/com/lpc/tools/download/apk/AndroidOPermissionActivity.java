package com.lpc.tools.download.apk;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

/**
 * projectName: ForLike
 * user：Lpc
 * date: 2019-11-29
 * descriptions：android8.0在线更新版本权限申请
 */
public class AndroidOPermissionActivity extends AppCompatActivity {

    public static final int INSTALL_PACKAGES_REQUEST_CODE = 1;
    private AlertDialog mAlertDialog;
    public static DownloadApk.AndroidOInstallPermissionListener sListener;
    /**
     * 申请权限弹窗icon 一般为APP的icon
     */
    public static int mIconId;

    /**
     * 申请权限的描述
     */
    public static String mMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.REQUEST_INSTALL_PACKAGES},
                    INSTALL_PACKAGES_REQUEST_CODE);
        } else {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == INSTALL_PACKAGES_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (sListener != null) {
                    sListener.permissionSuccess();
                    finish();
                }
            } else {
                showRequestPermissionDialog();
            }
        }
    }

    private void showRequestPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("申请权限");
//        builder.setIcon(R.mipmap.public_ic_launcher);
//        builder.setMessage("为了正常升级知识库APP，请点击设置按钮，允许安装未知来源应用，" +
//                "本功能只限用于知识库APP版本升级");
        if (mIconId != 0) {
            builder.setIcon(mIconId);
        }
        if (!TextUtils.isEmpty(mMessage)) {
            builder.setMessage(mMessage);
        }
        builder.setPositiveButton("设置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startInstallPermissionSettingActivity();
                dialogDismiss();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sListener.permissionFail();
                dialogDismiss();
            }
        });
        mAlertDialog = builder.create();
        mAlertDialog.show();
    }

    private void startInstallPermissionSettingActivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (sListener != null) {
                sListener.permissionSuccess();
            }
        } else {
            if (sListener != null) {
                sListener.permissionFail();
            }
        }
        finish();
    }

    private void dialogDismiss() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sListener = null;
    }
}
