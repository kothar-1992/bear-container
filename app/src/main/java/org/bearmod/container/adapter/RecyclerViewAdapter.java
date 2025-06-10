package org.bearmod.container.adapter;

import static android.app.PendingIntent.getActivity;
import static com.blankj.utilcode.util.StringUtils.getString;
import static org.bearmod.container.activity.MainActivity.bitversi;
import static org.bearmod.container.activity.MainActivity.fixinstallint;
import static org.bearmod.container.activity.MainActivity.game;
import static org.bearmod.container.activity.MainActivity.gameint;
import static org.bearmod.container.utils.ActivityCompat.toastImage;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.blankj.molihuan.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.AppUtils;
import org.bearmod.container.activity.MainActivity;
import org.bearmod.container.floating.FloatingService;
import org.bearmod.container.floating.ToggleAim;
import org.bearmod.container.floating.ToggleBullet;
import org.bearmod.container.floating.ToggleSimulation;
import org.bearmod.container.libhelper.FileHelper;
import org.bearmod.container.utils.FLog;
import org.bearmod.container.utils.PermissionUtils;
import org.bearmod.container.utils.UiKit;



import java.util.ArrayList;

import android.content.Intent;

import org.bearmod.container.floating.Overlay;
import org.bearmod.container.R;
import org.bearmod.container.libhelper.ApkEnv;



public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {
    public MainActivity activity;
    public ArrayList<Integer> imageValues;
    public ArrayList<String> titleValues;
    public ArrayList<String> versionValues;
    public ArrayList<String> statusValues;
    public ArrayList<String> packageValues;

    public RecyclerViewAdapter(MainActivity activity, ArrayList<Integer> imageValues, ArrayList<String> titleValues, ArrayList<String> versionValues, ArrayList<String> statusValues, ArrayList<String> packageValues) {
        this.activity = activity;
        this.imageValues = new ArrayList<>();
        this.titleValues = new ArrayList<>();
        this.versionValues = new ArrayList<>();
        this.statusValues = new ArrayList<>();
        this.packageValues = new ArrayList<>();
        filterInstalledGames(imageValues, titleValues, versionValues, statusValues, packageValues);
    }

    private void filterInstalledGames(ArrayList<Integer> imageValues, ArrayList<String> titleValues, ArrayList<String> versionValues, ArrayList<String> statusValues, ArrayList<String> packageValues) {
        for (int i = 0; i < packageValues.size(); i++) {
            if (AppUtils.isAppInstalled(packageValues.get(i))) {
                this.imageValues.add(imageValues.get(i));
                this.titleValues.add(titleValues.get(i));
                this.versionValues.add(versionValues.get(i));
                this.statusValues.add(statusValues.get(i));
                this.packageValues.add(packageValues.get(i));
            }
        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_games, parent, false);
        return new MyViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        holder.gameIcon.setImageResource(imageValues.get(position));
        holder.gameTitle.setText(titleValues.get(position));
        holder.gameVersion.setText(versionValues.get(position));

        testanjing(holder.okBtn, holder.status, packageValues.get(position));

        holder.okBtn.setOnClickListener(v -> {
            if (statusValues.get(position).equals("Maintenance") || statusValues.get(position).equals("Coming Soon")) {
                toastImage(R.drawable.icon, "App is currently under: " + statusValues.get(position));
            } else {
                activity.doShowProgress(true);
                doInstallAndRun(holder, position);
            }
        });

    }

    @Override
    public int getItemCount() {
        return imageValues.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private ImageView gameIcon;
        private TextView gameTitle;
        private TextView gameVersion;
        private TextView status;
        private FrameLayout okBtn;

        public MyViewHolder(View itemView) {
            super(itemView);
            gameIcon = (ImageView)itemView.findViewById(R.id.gameIcon);
            gameTitle = (TextView)itemView.findViewById(R.id.gameTitle);
            gameVersion = (TextView)itemView.findViewById(R.id.gameVersion);
            okBtn = (FrameLayout)itemView.findViewById(R.id.okBtn);
            status = (TextView)itemView.findViewById(R.id.status);
        }
    }

    public void testanjing(FrameLayout game,TextView status, String pkg){

        activity.runOnUiThread(()-> {
            if (ApkEnv.getInstance().isInstalled(pkg)){
                if (ApkEnv.getInstance().isRunning(pkg)){
                    status.setText("Kill Game");
                }else{
                    status.setText("已安装");
                }
            }else{
                status.setText("未安装");
            }
        });

        game.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                activity.showBottomSheetDialog(activity.getResources().getDrawable(R.drawable.icon_toast_alert), activity.getString(R.string.confirm), activity.getString(R.string.want_remove_it), false, sv -> {
                    activity.doShowProgress(true);
                    unInstallWithDellay(pkg);
                    activity.dismissBottomSheetDialog();
                }, v1 -> {
                    activity.dismissBottomSheetDialog();
                });
                return false;
            }
        });
    }

    private void doInstallAndRun(MyViewHolder holder, int position) {
        if (activity == null) {
            ToastUtils.showLong("Null Activity");
            return;
        }

        activity.CURRENT_PACKAGE = packageValues.get(position);
    	Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (ApkEnv.getInstance().isInstalled(packageValues.get(position))) {
                    activity.doHideProgress();
                    if (ApkEnv.getInstance().isRunning(packageValues.get(position))) {
                        ApkEnv.getInstance().stopRunningApp(packageValues.get(position));
                        holder.status.setText("Open Game");
                        activity.stopService(new Intent(MainActivity.get(), FloatingService.class));
                        activity.stopService(new Intent(MainActivity.get(), Overlay.class));
                        activity.stopService(new Intent(MainActivity.get(), ToggleBullet.class));
                        activity.stopService(new Intent(MainActivity.get(), ToggleAim.class));
                        activity.stopService(new Intent(MainActivity.get(), ToggleSimulation.class));
                    } else {
                        if (ApkEnv.getInstance().tryAddLoader(packageValues.get(position))) {
                            activity.launchSplash(packageValues.get(position));
                            testanjing(holder.okBtn, holder.status, packageValues.get(position));
                        }

                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (gameint >= 1 && gameint <= 4) {
                                    Exec("/Simmu 9898");
                                    Exec("/Yadav RR 0 true");

                                } else if (gameint == 5) {
                                    Exec("/Simmu 9898");
                                    Exec("/Yadav RR 0 true");
                                }
                                activity.startService(new Intent(MainActivity.get(), FloatingService.class));
                            }
                        }, 15000);
                    }
                } else {
                        try {
                                activity.showBottomSheetDialog(activity.getResources().getDrawable(imageValues.get(position)), "Client: " + titleValues.get(position), "此过程可能需要1-3分钟，请在过程完成之前不要关闭应用程序", false, v -> {
                                    activity.dismissBottomSheetDialog();
                                    if(bitversi == 64) {
                                        FileHelper.tryInstallWithCopyObb(activity, activity.getProgresBar(), packageValues.get(position));
                                    }else if(bitversi == 32) {
                                        FileHelper.tryInstallWithCopyObb32(activity, activity.getProgresBar(), packageValues.get(position));

                                    }
                                }, v1 -> {
                                    activity.doHideProgress();
                                    activity.dismissBottomSheetDialog();
                                });


                        } catch(Exception err) {
                            FLog.error(err.getMessage());
                      }
                }
            }
        });
    }

    public void Exec(String path) {
        try {
            ExecuteElf("chmod 777 " + activity.getFilesDir() + path);
            ExecuteElf(activity.getFilesDir() + path);
        } catch (Exception e) {
        }
    }

    private void ExecuteElf(String shell) {
        try {
            Runtime.getRuntime().exec(shell, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unInstallWithDellay(String packageName) {
        UiKit.defer().when(() -> {
            long time = System.currentTimeMillis();
            ApkEnv.getInstance().unInstallApp(packageName);
            time = System.currentTimeMillis() - time;
            long delta = 500L - time;
            if (delta > 0) {
                UiKit.sleep(delta);
            }
        }).done((res) -> {
            activity.doInitRecycler();
            activity.doHideProgress();
            toastImage(R.drawable.ic_check, packageName + " was successfully uninstalled.");
        });
    }
}
