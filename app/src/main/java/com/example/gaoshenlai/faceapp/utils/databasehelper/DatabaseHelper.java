package com.example.gaoshenlai.faceapp.utils.databasehelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by gaoshenlai on 28/2/17.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    static String DATABASE_NAME = "FaceAppData.db";
    static int DATABASE_VERSION = 1;
    public DatabaseHelper(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }

    String CREATE_TABLE_FlipCoins = "CREATE TABLE FlipCoins(" +
            "CurrentFlipCoins INTEGER NOT NULL," +
            "TotalSpentFlipCoins INTEGER NOT NULL," +
            "TotalEarnFlipCoins INTEGER NOT NULL," +
            "LastSpentFlipCoins INETGER NOT NULL," +
            "LastEarnFlipCoins INTEGER NOT NULL);";
    String DROP_TABLE_FlipCoins = "DROP TABLE IF EXISTS FlipCoins";

    String CREATE_TABLE_BOUNDS = "CREATE TABLE BOUNDS(" +
            "CPU INTEGER NOT NULL," +
            "MEMORY INTEGER NOT NULL," +
            "BATTERY INTEGER NOT NULL," +
            "MAXBUDGET INTEGER NOT NULL);";
    String DROP_TABLE_BOUNDS = "DROP TABLE IF EXISTS BOUNDS";


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_FlipCoins);
        db.execSQL(CREATE_TABLE_BOUNDS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE_FlipCoins);
        db.execSQL(DROP_TABLE_BOUNDS);
        onCreate(db);
    }

    public int[] getBoundsSetting(){
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM BOUNDS",null);
        if(cursor==null || cursor.getCount()==0){
            ContentValues cv = new ContentValues();
            cv.put("CPU",100);
            cv.put("MEMORY",100);
            cv.put("BATTERY",100);
            cv.put("MAXBUDGET",2000);
            getWritableDatabase().insert("BOUNDS","CPU",cv);
        }
        int[] bounds_setting = new int[cursor.getColumnCount()];
        cursor.moveToFirst();
        for(int i=0;i<cursor.getColumnCount();++i){
            bounds_setting[i]=cursor.getInt(i);
        }
        return bounds_setting;
    }
    public void updateBoundsSetting(int cpu,int memory,int battery,int maxbudget){
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM BOUNDS",null);
        if(cursor!=null) {
            getWritableDatabase().execSQL("DELETE FROM BOUNDS");
        }
        ContentValues cv = new ContentValues();
        cv.put("CPU",cpu);
        cv.put("MEMORY",memory);
        cv.put("BATTERY",battery);
        cv.put("MAXBUDGET",maxbudget);
        getWritableDatabase().insert("BOUNDS","CPU",cv);
    }
    public int[] getFlipCoinsStatus(){
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM FlipCoins",null);
        if(cursor==null || cursor.getCount()==0){
            ContentValues cv = new ContentValues();
            cv.put("CurrentFlipCoins",100);
            cv.put("TotalSpentFlipCoins",0);
            cv.put("TotalEarnFlipCoins",0);
            cv.put("LastSpentFlipCoins",0);
            cv.put("LastEarnFlipCoins",0);
            getWritableDatabase().insert("FlipCoins","CurrentFlipCoins ",cv);
        }
        int[] bounds_setting = new int[cursor.getColumnCount()];
        cursor.moveToFirst();
        for(int i=0;i<cursor.getColumnCount();++i){
            bounds_setting[i]=cursor.getInt(i);
        }
        return bounds_setting;
    }
    public void earnFlipCoins(int flipCoins){
        int[] currentFlipCoins = getFlipCoinsStatus();
        currentFlipCoins[0] += flipCoins;
        currentFlipCoins[2] += flipCoins;
        currentFlipCoins[4] = flipCoins;
        updateFlipCoinSetting(currentFlipCoins);
    }
    public void spentFlipCoins(int flipCoins){
        int[] currentFlipCoins = getFlipCoinsStatus();
        currentFlipCoins[0] -= flipCoins;
        currentFlipCoins[1] += flipCoins;
        currentFlipCoins[3] = flipCoins;
        updateFlipCoinSetting(currentFlipCoins);
    }
    private void updateFlipCoinSetting(int[] status){
        ContentValues cv = new ContentValues();
        cv.put("CurrentFlipCoins",status[0]);
        cv.put("TotalSpentFlipCoins",status[1]);
        cv.put("TotalEarnFlipCoins",status[2]);
        cv.put("LastSpentFlipCoins",status[3]);
        cv.put("LastEarnFlipCoins",status[4]);
        getWritableDatabase().update("FlipCoins",cv,null,null);
    }
}
