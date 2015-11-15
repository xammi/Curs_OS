package bmstu.curs_os.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import bmstu.curs_os.Button;
import bmstu.curs_os.R;


public class SocketService extends ConnectService {
    private String host;
    private int port;

    public SocketService() {
        super("SocketService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        host = settings.getString("host", getResources().getString(R.string.default_host));
        port = settings.getInt("port", getResources().getInteger(R.integer.default_port));
        return super.onStartCommand(intent, flags, startId);
    }

    public void handleClick(Button button) {
        Socket socket = null;
        PrintWriter out = null;
        try {
            InetAddress hostAddress = InetAddress.getByName(host);
            socket = new Socket(hostAddress, port);
            out = new PrintWriter(new BufferedWriter(
                  new OutputStreamWriter(socket.getOutputStream())),
                  true);

            String command = "click:" + button.name();
            out.println(command);
            out.flush();
        }
        catch (IOException e) {
            sendResponse(e.toString());
        }
        finally {
            try {
                if (socket != null) {
                    socket.close();
                }
                if (out != null) {
                    out.close();
                }
            }
            catch (IOException e) {
                sendResponse(e.toString());
            }
        }
    }

    public void handleGyro(String vector) {
        // TODO: Handle action Baz
    }

    public void handleSwipe(String vector) {
        // TODO: Handle action Baz
    }
}
