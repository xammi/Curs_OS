package bmstu.curs_os;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import bmstu.curs_os.services.SocketService;
import bmstu.curs_os.services.UsbService;


public class MainActivity extends ActionBarActivity {

    private ConnectType type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(R.drawable.cursor);
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

        SharedPreferences settings = getSharedPreferences(getResources().getString(R.string.prefs_file), 0);
        type = ConnectType.valueOf(settings.getString("type", ConnectType.SOCKET.name()));
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
}
