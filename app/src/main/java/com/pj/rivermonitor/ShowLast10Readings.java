package com.pj.rivermonitor;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.opengl.EGLExt;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import static android.R.id.list;
import static android.R.id.message;
import static com.pj.rivermonitor.R.id.spinTerminal;
import static com.pj.rivermonitor.R.id.tbTerminal;

public class ShowLast10Readings extends AppCompatActivity implements Spinner.OnItemSelectedListener {

    private String DEBUG_TAG = "MySMSMonitor";
    private GestureDetectorCompat mDetector;
    private GestureDetector mGestureDetector;

    public static SensorEntryDbHelper mDbHelper;
    public static SQLiteDatabase db;
    public final static String INTENT_NEW_READING = "NEW READING";
    public static  String SensorReported;
    public static String NO_DATA = "No Data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_last10_readings);

        Log.d("MySMSMonitor", "River Started with Last x readings View.");

        final ListView listviewEntries = (ListView) findViewById(R.id.listViewmio);
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);

        // Get the selected Terminal from previous screen
        Bundle extras = getIntent().getExtras();
        SensorReported = "";
        if(getIntent().hasExtra(INTENT_NEW_READING)) {
            SensorReported = extras.getString(INTENT_NEW_READING);
        }

        // Get Database
        mDbHelper = new SensorEntryDbHelper(this);
        db = mDbHelper.getWritableDatabase();

        // Create an object of our Custom Gesture Detector Class
        final CustomGestureDetector customGestureDetector = new CustomGestureDetector();
        // Create a GestureDetector
        mGestureDetector = new GestureDetector(this, customGestureDetector);
        // Attach listeners that'll be called for double-tap and related gestures
        mGestureDetector.setOnDoubleTapListener(customGestureDetector);

        listviewEntries.setOnTouchListener(new ListView.OnTouchListener(){

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    mGestureDetector.onTouchEvent(event);
                    return false;
                }
        }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Get Screen Objects
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);
        spinTerminal.setOnItemSelectedListener(this);

        // Update reading if exists
        if(mDbHelper.getLastEntryID()==0) {
            // There is no reading so set fields to no reading

            // Upate spinner Terminal list with no data
            ArrayList sensorFilterList = new ArrayList();
            sensorFilterList.add(NO_DATA);
            ArrayAdapter spinSensorFilterAdapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item,sensorFilterList);
            spinSensorFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinTerminal.setAdapter(spinSensorFilterAdapter);

        }
        else {  // Populate information with last reading
            updateSensorFilter();
            if(SensorReported.length()> 0)
                selectValue(spinTerminal,SensorReported);
            updateData();
         }

    }

    void updateData()
    {
        final ListView listviewEntries = (ListView) findViewById(R.id.listViewmio);
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);

        // Retrieve information from the database
        String selectedTerminal = spinTerminal.getSelectedItem().toString();
        Cursor cursorEntries = mDbHelper.getLast10Readings(selectedTerminal);

        // Populate the listview with the database information
        SimpleCursorAdapter myAdapter = new SimpleCursorAdapter(this,R.layout.list_template,cursorEntries,new String[] {SensorReadings.SensorEntry.SENSOR_DATE, SensorReadings.SensorEntry.SENSOR_DISTANCE,SensorReadings.SensorEntry.SENSOR_TEMPERATURE, SensorReadings.SensorEntry.SENSOR_HUMIDITY},new int[] {R.id.Date_entrylistview,R.id.Distance_entrylistview,R.id.temperature_entrylistview,R.id.Humidity_entrylistview});
        listviewEntries.setAdapter(myAdapter);
    }
    private void updateSensorFilter() {
        // This will retrieve all sensors and show it in the spinner
        final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);
        String strSelectedValue = NO_DATA;
        if(spinTerminal.getCount() > 0) {
            strSelectedValue = spinTerminal.getSelectedItem().toString();
        }

        ArrayList sensorFilterList = new ArrayList();
        Cursor cursorEntry = mDbHelper.getSensors();
        while(cursorEntry.moveToNext()) {
            sensorFilterList.add(cursorEntry.getString(0));
        }
        ArrayAdapter spinSensorFilterAdapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item,sensorFilterList);
        spinSensorFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinTerminal.setAdapter(spinSensorFilterAdapter);
        if(strSelectedValue != NO_DATA)
        {
            selectValue(spinTerminal,strSelectedValue );
        }

    }
    private void selectValue(Spinner spinner, Object value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);

        return super.onTouchEvent(event);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.spinTerminal:
                updateData();
                break;
            default:
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    class CustomGestureDetector implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
        private String DEBUG_TAG = "MySMSMonitor";

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.d(DEBUG_TAG, "onSingleTapConfirmed");
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d(DEBUG_TAG, "onDoubleTap");
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {

            Log.d(DEBUG_TAG, "onDoubleTapEvent");
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(DEBUG_TAG, "onDown");
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            Log.d(DEBUG_TAG, "onShowPress");
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.d(DEBUG_TAG, "onSingleTapUp");
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(DEBUG_TAG, "onScroll");
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.d(DEBUG_TAG, "onLongPress");

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(DEBUG_TAG, "onFling");
            if (e1.getX() < e2.getX()) {
                Log.d(DEBUG_TAG, "Left to Right swipe performed");
                ShowGraphicalView();
            }

            if (e1.getX() > e2.getX()) {
                Log.d(DEBUG_TAG, "Right to Left swipe performed");

            }

            if (e1.getY() < e2.getY()) {
                Log.d(DEBUG_TAG, "Up to Down swipe performed");
            }

            if (e1.getY() > e2.getY()) {
                Log.d(DEBUG_TAG, "Down to Up swipe performed");
            }

            return true;
        }

        private void ShowGraphicalView() {
            String INTENT_NEW_READING = "NEW READING";
            final Spinner spinTerminal = (Spinner) findViewById(R.id.spinTerminal);

            Intent intent = new Intent(ShowLast10Readings.this, SensorGraphLayout.class);
            String message = spinTerminal.getSelectedItem().toString();
            intent.putExtra(INTENT_NEW_READING, message);
            startActivity(intent);
        }


    }
}
