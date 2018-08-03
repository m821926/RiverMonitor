package com.pj.rivermonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.provider.BaseColumns;
import android.util.Log;

import com.pj.rivermonitor.SensorReadings;

/**
 * Created by Patrik on 23/08/2016.
 */

public class SensorEntryDbHelper extends SQLiteOpenHelper {

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + SensorReadings.SensorEntry.TABLE_NAME + " (" +
                    SensorReadings.SensorEntry.ENTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    SensorReadings.SensorEntry.SENSOR_NAME + TEXT_TYPE + COMMA_SEP +
                    SensorReadings.SensorEntry.SENSOR_DATE + TEXT_TYPE + COMMA_SEP +
                    SensorReadings.SensorEntry.SENSOR_DISTANCE + INTEGER_TYPE + COMMA_SEP +
                    SensorReadings.SensorEntry.SENSOR_TEMPERATURE + INTEGER_TYPE + COMMA_SEP +
                    SensorReadings.SensorEntry.SENSOR_HUMIDITY + INTEGER_TYPE +
                    " )";
    private static final String SQL_CREATE_SENSOR_ALARM =
            "CREATE TABLE " + SensorReadings.SensorEntry.TABLE_NAME_SENSOR_ALARM + " (" +
                    SensorReadings.SensorEntry.SENSOR_ALARM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    SensorReadings.SensorEntry.SENSOR_ALARM_NAME + TEXT_TYPE + COMMA_SEP +
                    SensorReadings.SensorEntry.SENSOR_ALARM_ALERT_VALUE + INTEGER_TYPE +
                    " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SensorReadings.SensorEntry.TABLE_NAME;

    private static final String SQL_DELETE_SENSOR_ALARM =
            "DROP TABLE IF EXISTS " + SensorReadings.SensorEntry.TABLE_NAME_SENSOR_ALARM;


    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 6;
    public static final String DATABASE_NAME = "Sensor.db";

    public  SensorEntryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d("MySMSMonitor", "Database DBHelper activated. " + DATABASE_NAME + " Version: "+ DATABASE_VERSION);
        Log.d("MySMSMonitor", "Create String: " + SQL_CREATE_ENTRIES);
        SQLiteDatabase db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        db.execSQL(SQL_CREATE_SENSOR_ALARM);
        Log.d("MySMSMonitor", "Database Created.");

    }
    public boolean insertData(String sensorName, String sensorDate, int sensorDistance,int sensorTemperature, int sensorHumidity)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(SensorReadings.SensorEntry.SENSOR_NAME, sensorName);
        contentValues.put(SensorReadings.SensorEntry.SENSOR_DATE, sensorDate);
        contentValues.put(SensorReadings.SensorEntry.SENSOR_DISTANCE, sensorDistance);
        contentValues.put(SensorReadings.SensorEntry.SENSOR_HUMIDITY, sensorHumidity);
        contentValues.put(SensorReadings.SensorEntry.SENSOR_TEMPERATURE, sensorTemperature);
        long result = db.insert(SensorReadings.SensorEntry.TABLE_NAME,null,contentValues);
        if(result==-1)
        {
            return false;
        }
        else
        {
            return true;
        }
    }
    public boolean insertSensorAlarm(String sensorName, int alarm_alert_value)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(SensorReadings.SensorEntry.SENSOR_ALARM_NAME, sensorName);
        contentValues.put(SensorReadings.SensorEntry.SENSOR_ALARM_ALERT_VALUE, alarm_alert_value);
        long result = db.insert(SensorReadings.SensorEntry.TABLE_NAME_SENSOR_ALARM,null,contentValues);
        if(result==-1)
        {
            updateSensorAlarm(sensorName,alarm_alert_value);
            return true;
        }
        else
        {
            return true;
        }
    }
    public int getSensorAlarm(String sensorName) {
        // Gets the data repository in read mode
        SQLiteDatabase db = this.getReadableDatabase();
        int alarm_alert_value=0;
        Cursor res = db.rawQuery("SELECT " + SensorReadings.SensorEntry.SENSOR_ALARM_ALERT_VALUE +
                        " from " + SensorReadings.SensorEntry.TABLE_NAME_SENSOR_ALARM + " where " +
                        SensorReadings.SensorEntry.SENSOR_ALARM_NAME + " = ?",
                        new String[] {sensorName} );
        while (res.moveToNext())
        {
            alarm_alert_value = res.getInt(0);
        }
        return alarm_alert_value;
    }
    public int getLastEntryID() {
        // Write value in the database
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        int entryID=0;
        Cursor res = db.rawQuery("SELECT * from SQLITE_SEQUENCE where name = ?",new String[] {SensorReadings.SensorEntry.TABLE_NAME} );
        while (res.moveToNext())
        {
            entryID = res.getInt(1);
        }
        return entryID;
    }
    public int getLastEntryIDTerminal(String terminal) {
        // Write value in the database
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        int entryID=0;
        Cursor res = db.rawQuery("select Max(entryid) from "+SensorReadings.SensorEntry.TABLE_NAME+" where sensor=?",new String[] {terminal} );
        while (res.moveToNext())
        {
            entryID = res.getInt(0);
        }
        return entryID;
    }
    public int getLastEntryIDTerminalbutLowerThan(String terminal,int entryID) {
         // Write value in the database
            // Gets the data repository in write mode
            SQLiteDatabase db = this.getWritableDatabase();
            Cursor res = db.rawQuery("select Max(entryid) from "+SensorReadings.SensorEntry.TABLE_NAME+" where " + SensorReadings.SensorEntry.SENSOR_NAME + " =? and " + SensorReadings.SensorEntry.ENTRY_ID + " < ?",new String[] {terminal,String.valueOf(entryID)} );
            entryID = 0;
            while (res.moveToNext())
            {
                entryID = res.getInt(0);
            }
            return entryID;
        }
    public int getNextEntryIDTerminalbutHigherThan(String terminal,int entryID) {
        // Write value in the database
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select Min("+SensorReadings.SensorEntry.ENTRY_ID+") from "+SensorReadings.SensorEntry.TABLE_NAME+" where " + SensorReadings.SensorEntry.SENSOR_NAME + " =? and " + SensorReadings.SensorEntry.ENTRY_ID + " > ?",new String[] {terminal,String.valueOf(entryID)} );
        entryID = 0;
        while (res.moveToNext())
        {
            entryID = res.getInt(0);
        }
        return entryID;
    }
    public Cursor getLast10Readings(String terminal) {
        // Write value in the database
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select " + SensorReadings.SensorEntry.ENTRY_ID +" AS _id, "+SensorReadings.SensorEntry.SENSOR_DATE+","+ SensorReadings.SensorEntry.SENSOR_DISTANCE +","+SensorReadings.SensorEntry.SENSOR_TEMPERATURE+","+SensorReadings.SensorEntry.SENSOR_HUMIDITY+" from "+SensorReadings.SensorEntry.TABLE_NAME+" where " + SensorReadings.SensorEntry.SENSOR_NAME + " =? order by _id DESC  LIMIT 30",new String[] {terminal});
//        Cursor res = db.rawQuery("select * from "+SensorReadings.SensorEntry.TABLE_NAME,null);

        return res;
    }
    public Cursor getLastxReadings(String terminal,int AmountofReadings) {
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select " + SensorReadings.SensorEntry.ENTRY_ID +" AS _id, "+SensorReadings.SensorEntry.SENSOR_DATE+","+ SensorReadings.SensorEntry.SENSOR_DISTANCE +","+SensorReadings.SensorEntry.SENSOR_TEMPERATURE+","+SensorReadings.SensorEntry.SENSOR_HUMIDITY+" from "+SensorReadings.SensorEntry.TABLE_NAME+" where " + SensorReadings.SensorEntry.SENSOR_NAME + " =? order by _id DESC  LIMIT " + Integer.toString(AmountofReadings) ,new String[] {terminal});
        return res;
    }
    public Cursor getSensors()
    {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor res = db.rawQuery("select distinct(sensor) from "+SensorReadings.SensorEntry.TABLE_NAME,new String[] {} );
        return res;
    }
    public Cursor getSensorsAlarm()         // Returns all Sensors and their defined Alarm Values
    {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor res = db.rawQuery("select * from "+SensorReadings.SensorEntry.TABLE_NAME_SENSOR_ALARM,new String[] {} );
        return res;
    }
    public Cursor getEntryByID(int id) {
        // Write value in the database
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from " + SensorReadings.SensorEntry.TABLE_NAME + " Where "+ SensorReadings.SensorEntry.ENTRY_ID + " = ?",new String[] {String.valueOf(id)});
        return res;
    }
    public Cursor getAllData() {
        // Write value in the database
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor res = db.rawQuery("select * from " + SensorReadings.SensorEntry.TABLE_NAME, null);
        return res;
    }
    public boolean updateData(String id, String sensorName, String sensorDate, int sensorDistance,int sensorTemperature, int sensorHumidity)
    {
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(SensorReadings.SensorEntry.SENSOR_NAME, sensorName);
        contentValues.put(SensorReadings.SensorEntry.SENSOR_DATE, sensorDate);
        contentValues.put(SensorReadings.SensorEntry.SENSOR_DISTANCE, sensorDistance);
        contentValues.put(SensorReadings.SensorEntry.SENSOR_HUMIDITY, sensorHumidity);
        contentValues.put(SensorReadings.SensorEntry.SENSOR_TEMPERATURE, sensorTemperature);

        int rowsUpdated = db.update(SensorReadings.SensorEntry.TABLE_NAME,contentValues,"id=?",new String[] {id});

        if(rowsUpdated==0)
            return false;
        else
            return true;
    }
    public boolean updateSensorAlarm(String sensorName, int alarm_alert_value)
    {
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        //contentValues.put(SensorReadings.SensorEntry.SENSOR_NAME, sensorName);
        contentValues.put(SensorReadings.SensorEntry.SENSOR_ALARM_ALERT_VALUE, alarm_alert_value);

        int rowsUpdated = db.update(SensorReadings.SensorEntry.TABLE_NAME_SENSOR_ALARM,contentValues,
                                SensorReadings.SensorEntry.SENSOR_ALARM_NAME + "=?",new String[] {sensorName});

        if(rowsUpdated==0)
            return false;
        else
            return true;
    }
    public boolean deleteData(String id)
    {
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        int rowsDeleted = db.delete(SensorReadings.SensorEntry.TABLE_NAME,"id=?",new String[] {id});

        if(rowsDeleted==0)
            return false;
        else
            return true;
    }
    public boolean deleteSensorAlarm(String id)
    {
        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        int rowsDeleted = db.delete(SensorReadings.SensorEntry.TABLE_NAME_SENSOR_ALARM,
                            SensorReadings.SensorEntry.SENSOR_ALARM_ID + "=?",new String[] {id});

        if(rowsDeleted==0)
            return false;
        else
            return true;
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        db.execSQL(SQL_CREATE_ENTRIES);
        db.execSQL(SQL_DELETE_SENSOR_ALARM);
        db.execSQL(SQL_CREATE_SENSOR_ALARM);
        Log.d("MySMSMonitor", "Database Onupgrade.");
    }
    @Override public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
        Log.d("MySMSMonitor", "Database onDowngrade.");
    }
}