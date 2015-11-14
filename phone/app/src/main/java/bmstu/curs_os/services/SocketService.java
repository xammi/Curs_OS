package bmstu.curs_os.services;

import android.content.SharedPreferences;
import android.widget.Toast;

import java.io.IOException;
import java.io.DataOutputStream;
import java.net.Socket;

import bmstu.curs_os.Button;
import bmstu.curs_os.R;


public class SocketService extends ConnectService {
    private String host;
    private int port;

    public SocketService() {
        super("SocketService");
        SharedPreferences settings = getSharedPreferences(getResources().getString(R.string.prefs_file), 0);
        host = settings.getString("host", "192.168.0.1");
        port = settings.getInt("port", 8000);
    }

    public void handleClick(Button button) {
        try {
            Socket socket = new Socket(host, port);
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            String command = "click:" + button.name();
            output.writeBytes(command);
            output.close();
        }
        catch (IOException e) {
            Toast.makeText(SocketService.this, "Can not connect", Toast.LENGTH_LONG).show();
        }
    }

    public void handleGyro(String vector) {
        // TODO: Handle action Baz
    }

    public void handleSwipe(String vector) {
        // TODO: Handle action Baz
    }
}
