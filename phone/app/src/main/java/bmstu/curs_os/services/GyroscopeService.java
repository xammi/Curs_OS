package bmstu.curs_os.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class GyroscopeService extends Service {
    public GyroscopeService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
