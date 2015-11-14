package bmstu.curs_os.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import bmstu.curs_os.Button;

/**
 * Created by max on 14.11.15.
 */
abstract public class ConnectService extends IntentService {
    private static final String ACTION_CLICK = "bmstu.curs_os.services.action.CLICK";
    private static final String ACTION_GYRO = "bmstu.curs_os.services.action.GYRO";
    private static final String ACTION_SWIPE = "bmstu.curs_os.services.action.SWIPE";

    private static final String EXTRA_BUTTON = "bmstu.curs_os.services.extra.BUTTON";
    private static final String EXTRA_VECTOR = "bmstu.curs_os.services.extra.VECTOR";


    public static void startActionClick(Context context, Button button) {
        Intent intent = new Intent(context, SocketService.class);
        intent.setAction(ACTION_CLICK);
        intent.putExtra(EXTRA_BUTTON, button);
        context.startService(intent);
    }

    public static void startActionGyro(Context context, String vector) {
        Intent intent = new Intent(context, SocketService.class);
        intent.setAction(ACTION_GYRO);
        intent.putExtra(EXTRA_VECTOR, vector);
        context.startService(intent);
    }

    public static void startActionSwipe(Context context, String vector) {
        Intent intent = new Intent(context, SocketService.class);
        intent.setAction(ACTION_SWIPE);
        intent.putExtra(EXTRA_VECTOR, vector);
        context.startService(intent);
    }

    public ConnectService(String tag) { super(tag); }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_CLICK.equals(action)) {
                final String button_name = intent.getStringExtra(EXTRA_BUTTON);
                handleClick(Button.valueOf(button_name));
            }
            else if (ACTION_GYRO.equals(action)) {
                final String vector = intent.getStringExtra(EXTRA_VECTOR);
                handleGyro(vector);
            }
            else if (ACTION_SWIPE.equals(action)) {
                final String vector = intent.getStringExtra(EXTRA_VECTOR);
                handleSwipe(vector);
            }
        }
    }

    public abstract void handleClick(Button button);
    public abstract void handleGyro(String vector);
    public abstract void handleSwipe(String vector);
}
