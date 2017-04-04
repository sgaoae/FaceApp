package com.example.gaoshenlai.faceapp.offloading_setting;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.example.gaoshenlai.faceapp.R;
import com.example.gaoshenlai.faceapp.utils.databasehelper.DatabaseHelper;

/**
 * Created by gaoshenlai on 22/2/17.
 */

public class statistics extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statistics);

        DatabaseHelper helper = new DatabaseHelper(getApplicationContext());
        int[] coins = helper.getFlipCoinsStatus();
        ((TextView) findViewById(R.id.current_coins)).setText(String.valueOf(coins[0]));
        ((TextView) findViewById(R.id.total_spent_coins)).setText(String.valueOf(coins[1]));
        ((TextView) findViewById(R.id.total_earn_coins)).setText(String.valueOf(coins[2]));
    }
}
