package bmstu.curs_os;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import bmstu.curs_os.services.ConnectService;
import bmstu.curs_os.services.SocketService;
import bmstu.curs_os.services.UsbService;

import static android.util.FloatMath.cos;
import static android.util.FloatMath.sin;
import static android.util.FloatMath.sqrt;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";

    private ConnectType type;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final float EPSILON = 0.1f;

    private final float[] deltaRotationVector = new float[3];
    private float timestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(R.drawable.cursor_new);
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        type = ConnectType.valueOf(settings.getString("type", ConnectType.SOCKET.name()));

        registerReceiver(connectReceiver, new IntentFilter(ConnectService.ACTION_RESPONSE));

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                MainActivity.this.onSensorChanged(event);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        }, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        ListView touchArea = (ListView) findViewById(R.id.swipe_view);
        touchArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return MainActivity.this.onSwipeEvent(event);
            }
        });
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(connectReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivityForResult(intent, RESULT_OK);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            return;
        }
        String typeName = data.getStringExtra("type");
        if (typeName == null)
            typeName = ConnectType.SOCKET.name();

        type = ConnectType.valueOf(typeName);
    }

    public void onLeftClick(View view) {
        switch (type) {
            case SOCKET:
                SocketService.startActionClick(MainActivity.this, bmstu.curs_os.Button.LEFT);
                break;
            case USB:
                UsbService.startActionClick(MainActivity.this, bmstu.curs_os.Button.LEFT);
                break;
        }
    }

    public void onRightClick(View view) {
        switch (type) {
            case SOCKET:
                SocketService.startActionClick(MainActivity.this, bmstu.curs_os.Button.RIGHT);
                break;
            case USB:
                UsbService.startActionClick(MainActivity.this, bmstu.curs_os.Button.RIGHT);
                break;
        }
    }

    private BroadcastReceiver connectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(ConnectService.EXTRA_MSG);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    };

    public void onSensorChanged(SensorEvent event) {
        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];

            float omegaMagnitude = sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

            if (omegaMagnitude > EPSILON) {
                axisX /= omegaMagnitude;
                axisY /= omegaMagnitude;
                axisZ /= omegaMagnitude;

                deltaRotationVector[0] = axisX;
                deltaRotationVector[1] = axisY;
                deltaRotationVector[2] = axisZ;

                String vector = matrixToString(deltaRotationVector);
                sendGyroDelta(vector);
            }
        }
        timestamp = event.timestamp;
    }

    public String matrixToString(float [] matrix) {
        boolean start = true;
        StringBuilder vector = new StringBuilder();
        for (float item : matrix) {
            if (! start)
                vector.append(',');
            vector.append(Float.toString(item));
            start = false;
        }
        return vector.toString();
    }

    public void sendGyroDelta(String vector) {
        switch (type) {
            case SOCKET:
                SocketService.startActionGyro(MainActivity.this, vector);
                break;
            case USB:
                UsbService.startActionGyro(MainActivity.this, vector);
                break;
        }
    }

    Float prevX = null;
    Float prevY = null;

    public boolean onSwipeEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (prevX != null && prevY != null)
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:

                float deltaX = prevX - x;
                float deltaY = prevY - y;
                String vector = Float.toString(deltaX) + "," + deltaY;

                switch (type) {
                    case SOCKET:
                        SocketService.startActionSwipe(MainActivity.this, vector);
                        break;
                    case USB:
                        UsbService.startActionSwipe(MainActivity.this, vector);
                        break;
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        prevX = x;
        prevY = y;
        return true;
    }
}
