package bmstu.curs_os;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;


public class SettingsActivity extends ActionBarActivity {
    private String host;
    private int port;
    private ConnectType type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(R.drawable.cursor_new);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);

        toolbar.setTitle(R.string.title_activity_settings);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(SettingsActivity.this);
            }
        });

        SharedPreferences settings = getSharedPreferences(getResources().getString(R.string.prefs_file), 0);
        type = ConnectType.valueOf(settings.getString("type", ConnectType.SOCKET.name()));
        host = settings.getString("host", getResources().getString(R.string.default_host));
        port = settings.getInt("port", getResources().getInteger(R.integer.default_port));
        setEditListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences settings = getSharedPreferences(getResources().getString(R.string.prefs_file), 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("type", type.name());
        editor.putString("host", host);
        editor.putInt("port", port);

        Intent intent = new Intent();
        intent.putExtra("type", type.name());
        intent.putExtra("host", host);
        intent.putExtra("port", port);
        setResult(RESULT_OK, intent);

        editor.commit();
    }

    private void setEditListeners() {
        EditText hostEdit = (EditText) findViewById(R.id.edt_host);
        hostEdit.setText(host);

        hostEdit.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                host = s.toString();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        EditText portEdit = (EditText) findViewById(R.id.edt_port);
        portEdit.setText(Integer.toString(port));
        portEdit.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                try {
                    port = Integer.valueOf(s.toString());
                }
                catch (NumberFormatException e) {
                    Toast.makeText(SettingsActivity.this,
                            "The port must be integer", Toast.LENGTH_SHORT).show();
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        switch(view.getId()) {
            case R.id.type_usb:
                if (checked)
                    type = ConnectType.USB;
                    break;
            case R.id.type_socket:
                if (checked)
                    type = ConnectType.SOCKET;
                    break;
        }
    }
}
