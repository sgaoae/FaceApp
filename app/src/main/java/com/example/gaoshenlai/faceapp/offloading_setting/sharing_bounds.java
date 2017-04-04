package com.example.gaoshenlai.faceapp.offloading_setting;

import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.gaoshenlai.faceapp.R;
import com.example.gaoshenlai.faceapp.utils.databasehelper.DatabaseHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by gaoshenlai on 22/2/17.
 */

public class sharing_bounds extends AppCompatActivity {
    Thread update = null;
    boolean running=true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sharing_bounds);

        SeekBar cpu_share_seekbar = (SeekBar) findViewById(R.id.cpu_share);
        cpu_share_seekbar.setMax(100);
        cpu_share_seekbar.setOnSeekBarChangeListener(generateOnSeekBarChangeListener(R.id.cpu));
        cpu_share_seekbar.setEnabled(false);
        SeekBar memory_share_seekbar = (SeekBar) findViewById(R.id.memory_share);
        memory_share_seekbar.setMax(100);
        memory_share_seekbar.setOnSeekBarChangeListener(generateOnSeekBarChangeListener(R.id.memory));
        memory_share_seekbar.setEnabled(false);
        SeekBar cpu_bound_seekbar = (SeekBar) findViewById(R.id.cpu_bound_seekbar);
        cpu_bound_seekbar.setMax(100);
        cpu_bound_seekbar.setOnSeekBarChangeListener(generateOnSeekBarChangeListener(R.id.cpu_bound));
        SeekBar memory_bound_seekbar = (SeekBar) findViewById(R.id.memory_bound_seekbar);
        memory_bound_seekbar.setMax(100);
        memory_bound_seekbar.setOnSeekBarChangeListener(generateOnSeekBarChangeListener(R.id.memory_bound));
        SeekBar battery_bound_seekbar = (SeekBar) findViewById(R.id.battery_bound_seekbar);
        battery_bound_seekbar.setMax(100);
        battery_bound_seekbar.setOnSeekBarChangeListener(generateOnSeekBarChangeListener(R.id.battery_bound));
        SeekBar maxbudget_bound_seekbar = (SeekBar) findViewById(R.id.maxbudget_bound_seekbar);
        maxbudget_bound_seekbar.setMax(2000);
        maxbudget_bound_seekbar.setOnSeekBarChangeListener(generateOnSeekBarChangeListener(R.id.maxbudget_bound));

        DatabaseHelper helper = new DatabaseHelper(getApplicationContext());
        int[] bounds_setting = helper.getBoundsSetting();
        cpu_bound_seekbar.setProgress(bounds_setting[0]);
        memory_bound_seekbar.setProgress(bounds_setting[1]);
        battery_bound_seekbar.setProgress(bounds_setting[2]);
        maxbudget_bound_seekbar.setProgress(bounds_setting[3]);

        update = new Thread(new Runnable() {
            @Override
            public void run() {
                while(running) {
                    int[] cpuUsage = getCpuUsageStatistic();
                    int cpu = 0;
                    for (int i = 0; i < cpuUsage.length; ++i) cpu += cpuUsage[i];
                    ((SeekBar) findViewById(R.id.cpu_share)).setProgress(cpu);

                    StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
                    double free = statFs.getAvailableBlocksLong();
                    double total = statFs.getBlockCountLong();
                    double memory = free/total*100;
                    ((SeekBar) findViewById(R.id.memory_share)).setProgress((int)memory);

                    try {
                        Thread.sleep(1000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        });
        update.start();

        Button bounds_btn = (Button) findViewById(R.id.update_bounds_btn);
        bounds_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cpu_text = ((TextView) findViewById(R.id.cpu_bound)).getText().toString();
                int cpu = Integer.valueOf(cpu_text);
                String memory_text = ((TextView) findViewById(R.id.memory_bound)).getText().toString();
                int memory = Integer.valueOf(memory_text);
                String battery_text = ((TextView) findViewById(R.id.battery_bound)).getText().toString();
                int battery = Integer.valueOf(battery_text);
                String maxbudget_text = ((TextView) findViewById(R.id.maxbudget_bound)).getText().toString();
                int maxbudget = Integer.valueOf(maxbudget_text);

                DatabaseHelper helper = new DatabaseHelper(getApplicationContext());
                helper.updateBoundsSetting(cpu,memory,battery,maxbudget);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running=false;
        update.interrupt();
        update=null;
    }

    public static int[] getCpuUsageStatistic() {

        String tempString = executeTop();

        tempString = tempString.replaceAll(",", "");
        tempString = tempString.replaceAll("User", "");
        tempString = tempString.replaceAll("System", "");
        tempString = tempString.replaceAll("IOW", "");
        tempString = tempString.replaceAll("IRQ", "");
        tempString = tempString.replaceAll("%", "");
        for (int i = 0; i < 10; i++) {
            tempString = tempString.replaceAll("  ", " ");
        }
        tempString = tempString.trim();
        String[] myString = tempString.split(" ");
        int[] cpuUsageAsInt = new int[myString.length];
        for (int i = 0; i < myString.length; i++) {
            myString[i] = myString[i].trim();
            cpuUsageAsInt[i] = Integer.parseInt(myString[i]);
        }
        return cpuUsageAsInt;
    }

    private static String executeTop() {
        java.lang.Process p = null;
        BufferedReader in = null;
        String returnString = null;
        try {
            p = Runtime.getRuntime().exec("top -n 1");// top -n 1
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (returnString == null || returnString.contentEquals("")) {
                returnString = in.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                p.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return returnString;
    }

    SeekBar.OnSeekBarChangeListener generateOnSeekBarChangeListener(final int text_view_id){
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ((TextView) findViewById(text_view_id)).setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
        return listener;
    }
}
