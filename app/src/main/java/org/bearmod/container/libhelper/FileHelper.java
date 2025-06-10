package org.bearmod.container.libhelper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import org.bearmod.container.BoxApplication;
import org.bearmod.container.R;
import org.bearmod.container.activity.MainActivity;
import org.bearmod.container.utils.ActivityCompat;
import org.bearmod.container.utils.FLog;
import com.blankj.molihuan.utilcode.util.FileUtils;
import com.google.android.material.progressindicator.LinearProgressIndicator;


import java.io.File;

public class FileHelper {

    public static void tryInstallWithCopyObb(MainActivity activity, LinearProgressIndicator prog, String packageName) {
        new Thread(() -> {
            PackageInfo info = null;
            try {
                info = activity.getPackageManager().getPackageInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException err) {
                FLog.error(err.getMessage());
            }

            if (info == null) {
                handleInstallationError(activity, prog, "请先安装游戏");
                return;
            }

           if (!info.applicationInfo.nativeLibraryDir.contains("64")) {
                handleInstallationError(activity, prog, "请安装游戏64Bit版本");
                return;
            }

            String gameObb = "main." + info.versionCode + "." + info.packageName + ".obb";
            File obbDest = new File("storage/emulated/0/Android/obb/" + packageName, gameObb);

            if (!obbDest.exists()) {
                handleInstallationError(activity, prog, "找不到Obb文件/请检查");
                return;
            }

            File virObbDir = ApkEnv.getInstance().getObbContainerPath(packageName);
            if (!virObbDir.exists()) virObbDir.mkdirs();

            File virObbDest = new File(virObbDir, gameObb);

            activity.runOnUiThread(() -> {
                activity.doHideProgress();
                activity.doShowProgress(true);
            });

            try {
                FileUtils.copy(obbDest.toString(), virObbDest.toString());
            } catch (Exception err) {
                FLog.error(err.getMessage());
                return;
            }

            if (!ApkEnv.getInstance().isInstalled(packageName)) {
                boolean installResult = ApkEnv.getInstance().installByFile(packageName);
                if (!installResult) {
                    handleInstallationError(activity, prog, "添加游戏失败");
                    return;
                }
            }

            ApplicationInfo applicationInfo = ApkEnv.getInstance().getApplicationInfoContainer(packageName);
            if (applicationInfo == null) {
                handleInstallationError(activity, prog, "错误，应用程序信息");
                return;
            }

            activity.runOnUiThread(() -> {
                MainActivity.get().doInitRecycler();
                prog.setIndeterminate(false);
                activity.doHideProgress();
                ActivityCompat.getActivityCompat().toastImage(R.drawable.ic_check, "安装完成请重启程序");
            });

            File listAbi = new File(applicationInfo.nativeLibraryDir);
            for (File abi : listAbi.listFiles()) {
                BoxApplication.get().doChmod(abi.toString(), 755);
            }
        }).start();
    }
    public static void tryInstallWithCopyObb32(MainActivity activity, LinearProgressIndicator prog, String packageName) {
        new Thread(() -> {
            PackageInfo info = null;
            try {
                info = activity.getPackageManager().getPackageInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException err) {
                FLog.error(err.getMessage());
            }

            if (info == null) {
                handleInstallationError(activity, prog, "Please Install Game first.");
                return;
            }

            if (!info.applicationInfo.nativeLibraryDir.contains("32")) {
                handleInstallationError(activity, prog, "Please Install Game 32 Bit version.");
                return;
            }

            String gameObb = "main." + info.versionCode + "." + info.packageName + ".obb";
            File obbDest = new File("storage/emulated/0/Android/obb/" + packageName, gameObb);

            if (!obbDest.exists()) {
                handleInstallationError(activity, prog, "Obb File not found");
                return;
            }

            File virObbDir = ApkEnv.getInstance().getObbContainerPath(packageName);
            if (!virObbDir.exists()) virObbDir.mkdirs();

            File virObbDest = new File(virObbDir, gameObb);

            activity.runOnUiThread(() -> {
                activity.doHideProgress();
                activity.doShowProgress(true);
            });

            try {
                FileUtils.copy(obbDest.toString(), virObbDest.toString());
            } catch (Exception err) {
                FLog.error(err.getMessage());
                return;
            }

            if (!ApkEnv.getInstance().isInstalled(packageName)) {
                boolean installResult = ApkEnv.getInstance().installByFile(packageName);
                if (!installResult) {
                    handleInstallationError(activity, prog, "Failed Add Games");
                    return;
                }
            }

            ApplicationInfo applicationInfo = ApkEnv.getInstance().getApplicationInfoContainer(packageName);
            if (applicationInfo == null) {
                handleInstallationError(activity, prog, "Error, Application Info");
                return;
            }

            activity.runOnUiThread(() -> {
                MainActivity.get().doInitRecycler();
                prog.setIndeterminate(false);
                activity.doHideProgress();
                ActivityCompat.getActivityCompat().toastImage(R.drawable.ic_check, "Installation is complete.");
            });

            File listAbi = new File(applicationInfo.nativeLibraryDir);
            for (File abi : listAbi.listFiles()) {
                BoxApplication.get().doChmod(abi.toString(), 755);
            }
        }).start();
    }


    private static void handleInstallationError(MainActivity activity, LinearProgressIndicator prog, String errorMessage) {
        activity.runOnUiThread(() -> {
            activity.doHideProgress();
            ActivityCompat.getActivityCompat().toastImage(R.drawable.ic_error, errorMessage);
        });
    }
}
