package com.example.gaoshenlai.faceapp.offloading_setting;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.gaoshenlai.faceapp.R;
import com.example.gaoshenlai.faceapp.utils.databasehelper.DatabaseHelper;
import com.example.gaoshenlai.faceapp.utils.p2pconnection.WiFiP2pListener;

/**
 * Created by gaoshenlai on 22/2/17.
 */

public class setting_menu extends AppCompatActivity {
    boolean status = true;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_menu);

        DatabaseHelper helper = new DatabaseHelper(getApplicationContext());
        int coins = helper.getFlipCoinsStatus()[0];
        TextView coin_number = (TextView) findViewById(R.id.coin);
        coin_number.setText(Integer.toString(coins));

        Button status_btn = (Button) findViewById(R.id.status_btn);
        Button sharing_bounds = (Button) findViewById(R.id.sharing_bounds);
        Button statistics = (Button) findViewById(R.id.statistics);
        status_btn.setOnClickListener(setting_listener);
        sharing_bounds.setOnClickListener(setting_listener);
        statistics.setOnClickListener(setting_listener);
    }

    View.OnClickListener setting_listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()){
                case R.id.status_btn:
                    status = !status;
                    if(status){
                        ((Button) v).setText("ON");
                    }else{
                        ((Button) v).setText("OFF");
                    }
                    break;
                case R.id.sharing_bounds:
                    Intent first_intent = new Intent(setting_menu.this,sharing_bounds.class);
                    startActivity(first_intent);
                    break;
                case R.id.statistics:
                    Intent second_intent = new Intent(setting_menu.this,statistics.class);
                    // pass parameters
                    startActivity(second_intent);
                    break;
            }
        }
    };
}
