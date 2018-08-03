package com.pj.rivermonitor;

import android.Manifest;
import android.Manifest.permission;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.os.Build;
import android.provider.BaseColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.content.BroadcastReceiver;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;


import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static android.Manifest.permission.*;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PERMISSIONS_REQUEST_RECEIVE_SMS = 0;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    public static SensorEntryDbHelper mDbHelper;
    public static SQLiteDatabase db;
    public final static String EXTRA_MESSAGE = "com.pj.rivermonitor.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Request the permission immediately here for the first time run
        // requestPermissions(RECEIVE_SMS, PERMISSIONS_REQUEST_RECEIVE_SMS);
        //ActivityCompat.requestPermissions(this,new String[] {permission.RECEIVE_SMS}, REQUEST_CODE_ASK_PERMISSIONS);


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MySMSMonitor", "River Started.");

        // Define Buttons and actions
        final Button btnUpdate = (Button) findViewById(R.id.btnUpdate);
        btnUpdate.setOnClickListener(this);
        final Button btnShowSensor = (Button) findViewById(R.id.btnShowSensor);
        btnShowSensor.setOnClickListener(this);
        final Button btnLast10Readings = (Button) findViewById(R.id.btnLast10Readings);
        btnLast10Readings.setOnClickListener(this);
        final Button btnShowSensorPreviousValue = (Button) findViewById(R.id.btnShowSensorPreviousValue);
        btnShowSensorPreviousValue.setOnClickListener(this);
        final Button btnShowSensorNextValue = (Button) findViewById(R.id.btnShowSensorNextValue);
        btnShowSensorNextValue.setOnClickListener(this);
        final Button btnSwitch2GraphicView = (Button) findViewById(R.id.btnSwitch2GraphicView);
        btnSwitch2GraphicView.setOnClickListener(this);

        // Start Database Helper
        mDbHelper = new SensorEntryDbHelper(this);
        db = mDbHelper.getWritableDatabase();
        Log.d("MySMSMonitor", "Database activated.");

           //activity = this;
        //ActivityCompat.requestPermissions(activity,new String[] {permission.RECEIVE_SMS}, REQUEST_CODE_ASK_PERMISSIONS);
        Log.d("MySMSMonitor", "Check permissions");

        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(android.Manifest.permission.RECEIVE_SMS)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d("MySMSMonitor", "Permission is granted");


            } else {


               ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECEIVE_SMS}, 1);
                Log.d("MySMSMonitor", "Permission is revoked");
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.d("MySMSMonitor", "Permission is already granted");

        }

        // Update reading if exists
        if(mDbHelper.getLastEntryID()==0) {
            // There is no reading so set fields to no reading
            final EditText tbID = (EditText) findViewById(R.id.tbID);
            final EditText tbTerminal = (EditText) findViewById(R.id.tbTerminal);
            final EditText tbDate = (EditText) findViewById(R.id.tbDate);
            final EditText tbDistance = (EditText) findViewById(R.id.tbDistance);
            final EditText tbTemperature = (EditText) findViewById(R.id.tbTemperature);
            final EditText tbHumidity = (EditText) findViewById(R.id.tbHumidity);
            tbID.setText("");
            tbTerminal.setText("No Data is available");
            tbDate.setText("");
            tbDistance.setText("N/A");
            tbHumidity.setText("N/A");
            tbTemperature.setText("N/A");

            final Spinner spinSensorFilter = (Spinner) findViewById(R.id.spinSensorFilter);
            ArrayList sensorFilterList = new ArrayList();
            sensorFilterList.add("No Sensors Have Reported Yet");
            ArrayAdapter spinSensorFilterAdapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item,sensorFilterList);
            spinSensorFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinSensorFilter.setAdapter(spinSensorFilterAdapter);
        }
        else {  // Populate information with last reading
            updateSensorFilter();
            updateData();
        }
    }

    private void updateSensorFilter() {
        // This will retrieve all sensors and show it in the spinner
        final Spinner spinSensorFilter = (Spinner) findViewById(R.id.spinSensorFilter);
        ArrayList sensorFilterList = new ArrayList();
        Cursor cursorEntry = mDbHelper.getSensors();
        while(cursorEntry.moveToNext()) {
            sensorFilterList.add(cursorEntry.getString(0));
        }
        ArrayAdapter spinSensorFilterAdapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item,sensorFilterList);
        spinSensorFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinSensorFilter.setAdapter(spinSensorFilterAdapter);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId() /*to get clicked view id**/) {
            case R.id.btnUpdate:
                updateData();
                break;
            case R.id.btnLast10Readings:
                ShowLast10Readings();
                break;
            case R.id.btnShowSensor:
                UpdateDataSensor();
                break;
            case R.id.btnShowSensorPreviousValue:
                UpdateDataSensorPreviousValue();
                break;
            case R.id.btnShowSensorNextValue:
                UpdateDataSensorNextValue();
                break;
            case R.id.btnSwitch2GraphicView:
                Switch2GraphicView();
                break;
            default:
                break;
        }
    }

    private void Switch2GraphicView() {

        Intent intent = new Intent(this, SensorDrawingLayout.class);
//        EditText tbTerminal = (EditText) findViewById(R.id.tbTerminal);
//        String message = tbTerminal.getText().toString();
//        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    private void ShowLast10Readings() {

        Intent intent = new Intent(this, ShowLast10Readings.class);
        EditText tbTerminal = (EditText) findViewById(R.id.tbTerminal);
        String message = tbTerminal.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    private void UpdateDataSensorNextValue() {
        Log.d("MySMSMonitor", "Updating Data of Selected Sensor With Next Value");
        final EditText tbID = (EditText) findViewById(R.id.tbID);
        final EditText tbTerminal = (EditText) findViewById(R.id.tbTerminal);
        final EditText tbDate = (EditText) findViewById(R.id.tbDate);
        final EditText tbDistance = (EditText) findViewById(R.id.tbDistance);
        final EditText tbTemperature = (EditText) findViewById(R.id.tbTemperature);
        final EditText tbHumidity = (EditText) findViewById(R.id.tbHumidity);
        final Spinner spinSensorFilter = (Spinner) findViewById(R.id.spinSensorFilter);
        String strID = String.valueOf(tbID.getText());
        int LastEntry =  mDbHelper.getNextEntryIDTerminalbutHigherThan(tbTerminal.getText().toString(), Integer.parseInt(strID));
        if(LastEntry == 0)
        {
            // There are is no previous record
            Toast myToast = Toast.makeText(getApplicationContext(), "There is no next record.", Toast.LENGTH_SHORT);
            myToast.show();
        }
        else {
            Cursor cursorEntry = mDbHelper.getEntryByID(LastEntry);
            while (cursorEntry.moveToNext()) {
                tbID.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.ENTRY_ID)));
                tbTerminal.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_NAME)));
                tbDate.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DATE)));
                tbDistance.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DISTANCE)) + " cm");
                tbTemperature.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_TEMPERATURE)) + " ºC");
                tbHumidity.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_HUMIDITY)) + " %");
            }
        }
    }

    private void UpdateDataSensorPreviousValue() {
        Log.d("MySMSMonitor", "Updating Data of Selected Sensor With Previous Value");
        final EditText tbID = (EditText) findViewById(R.id.tbID);
        final EditText tbTerminal = (EditText) findViewById(R.id.tbTerminal);
        final EditText tbDate = (EditText) findViewById(R.id.tbDate);
        final EditText tbDistance = (EditText) findViewById(R.id.tbDistance);
        final EditText tbTemperature = (EditText) findViewById(R.id.tbTemperature);
        final EditText tbHumidity = (EditText) findViewById(R.id.tbHumidity);
        final Spinner spinSensorFilter = (Spinner) findViewById(R.id.spinSensorFilter);
        String strID = String.valueOf(tbID.getText());
        int LastEntry =  mDbHelper.getLastEntryIDTerminalbutLowerThan(tbTerminal.getText().toString(), Integer.parseInt(strID));
        if(LastEntry == 0)
        {
         // There are is no previous record
            Toast myToast = Toast.makeText(getApplicationContext(), "There is no previous record.", Toast.LENGTH_SHORT);
            myToast.show();
        }
        else {
            Cursor cursorEntry = mDbHelper.getEntryByID(LastEntry);
            while (cursorEntry.moveToNext()) {
                tbID.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.ENTRY_ID)));
                tbTerminal.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_NAME)));
                tbDate.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DATE)));
                tbDistance.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DISTANCE)) + " cm");
                tbTemperature.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_TEMPERATURE)) + " ºC");
                tbHumidity.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_HUMIDITY)) + " %");
            }
        }
    }

    private void UpdateDataSensor() {
        Log.d("MySMSMonitor", "Updating Data of Selected Sensor");
        final EditText tbID = (EditText) findViewById(R.id.tbID);
        final EditText tbTerminal = (EditText) findViewById(R.id.tbTerminal);
        final EditText tbDate = (EditText) findViewById(R.id.tbDate);
        final EditText tbDistance = (EditText) findViewById(R.id.tbDistance);
        final EditText tbTemperature = (EditText) findViewById(R.id.tbTemperature);
        final EditText tbHumidity = (EditText) findViewById(R.id.tbHumidity);
        final Spinner spinSensorFilter = (Spinner) findViewById(R.id.spinSensorFilter);

        int LastEntry =  mDbHelper.getLastEntryIDTerminal(spinSensorFilter.getSelectedItem().toString());
        Cursor cursorEntry = mDbHelper.getEntryByID(LastEntry);
        while(cursorEntry.moveToNext())
        {
            tbID.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.ENTRY_ID)));
            tbTerminal.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_NAME)));
            tbDate.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DATE)));
            tbDistance.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DISTANCE))+" cm");
            tbTemperature.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_TEMPERATURE))+ " ºC");
            tbHumidity.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_HUMIDITY))+" %");
        }


    }

    private void updateData()
    {
        Log.d("MySMSMonitor", "Updating Data");
        final EditText tbID = (EditText) findViewById(R.id.tbID);
        final EditText tbTerminal = (EditText) findViewById(R.id.tbTerminal);
        final EditText tbDate = (EditText) findViewById(R.id.tbDate);
        final EditText tbDistance = (EditText) findViewById(R.id.tbDistance);
        final EditText tbTemperature = (EditText) findViewById(R.id.tbTemperature);
        final EditText tbHumidity = (EditText) findViewById(R.id.tbHumidity);
        int lastEntry = mDbHelper.getLastEntryID();
        Cursor cursorEntry = mDbHelper.getEntryByID(lastEntry);
        while(cursorEntry.moveToNext())
        {
            tbID.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.ENTRY_ID)));
            tbTerminal.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_NAME)));
            tbDate.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DATE)));
            tbDistance.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_DISTANCE))+" cm");
            tbTemperature.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_TEMPERATURE))+ " ºC");
            tbHumidity.setText(cursorEntry.getString(cursorEntry.getColumnIndex(SensorReadings.SensorEntry.SENSOR_HUMIDITY))+" %");
        }

    }
// This file is MySMSMonitor.java
//   import android.content.BroadcastReceiver;
//    import android.content.Context;
//    import android.content.Intent;
//    import android.telephony.SmsMessage;
//    import android.util.Log;


}

