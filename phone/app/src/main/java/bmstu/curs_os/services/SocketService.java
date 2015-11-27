package bmstu.curs_os.services;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.BufferedWriter;
import java.io.IOException;
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

    private void sendToHost(String command, boolean withReports) {
        Socket socket = null;
        PrintWriter out = null;
        try {
            InetAddress hostAddress = InetAddress.getByName(host);
            socket = new Socket(hostAddress, port);
            out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())),
                    true);

            out.println(command);
            out.flush();
        }
        catch (IOException e) {
            if (withReports)
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
                if (withReports)
                    sendResponse(e.toString());
            }
        }
    }

    public void handleClick(Button button) {
        String command = "click:" + button.name();
        sendToHost(command, true);
    }

    public void handleGyro(String vector) {
        String command = "gyro:" + vector;
        sendToHost(command, false);
    }

    public void handleSwipe(String vector) {
        String command = "scroll:" + vector;
        sendToHost(command, false);
    }
}
