package bmstu.curs_os;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;


public class MainActivity extends ActionBarActivity {

    private TextView lgView;
    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private UsbEndpoint mEndpointIntr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lgView = (TextView)findViewById(R.id.log_view);
        mUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
    }

    public void onResume() {
        super.onResume();
        //заполняем контейнер списком устройств
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        lgView.setText( "Devices Count:" + deviceList.size() );

        while (deviceIterator.hasNext()) {
            UsbDevice device = (UsbDevice) deviceIterator.next();

            //пример определения ProductID устройства
            lgView.setText( lgView.getText() + "\n" + "Device ProductID: " + device.getProductId() );
            lgView.setText( lgView.getText() + "\n" + "Device VendorID: " + device.getVendorId() );
        }
        //определяем намерение, описанное в фильтре
        // намерений AndroidManifest.xml
        Intent intent = getIntent();
        lgView.setText( lgView.getText() + "\n" + "intent: " + intent);
        String action = intent.getAction();

        //если устройство подключено, передаем ссылку в
        //в функцию setDevice()
        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            setDevice(device);
            lgView.setText( lgView.getText() + "\n" + "UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) is TRUE");
            sendMessage("Hello world");
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            if (mDevice != null && mDevice.equals(device)) {
                setDevice(null);
                lgView.setText( lgView.getText() + "\n" + "UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action) is TRUE");
            }
        }
    }

    private void setDevice(UsbDevice device) {
        lgView.setText( lgView.getText() + "\n" + "setDevice " + device);
        //определяем доступные интерфейсы устройства
        if (device.getInterfaceCount() != 1) {

            lgView.setText( lgView.getText() + "\n" + "could not find interface");
            return;
        }
        UsbInterface intf = device.getInterface(0);

        //определяем конечные точки устройства
        if (intf.getEndpointCount() == 0) {

            lgView.setText( lgView.getText() + "\n" +  "could not find endpoint");
            return;
        } else {
            lgView.setText( lgView.getText() + "\n" + "Endpoints Count: " + intf.getEndpointCount() );
        }

        UsbEndpoint epIN = null;
        UsbEndpoint epOUT = null;

        //ищем конечные точки для передачи по прерываниям
        for (int i = 0; i < intf.getEndpointCount(); i++) {
            if (intf.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                if (intf.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_IN) {
                    epIN = intf.getEndpoint(i);
                    lgView.setText( lgView.getText() + "\n" + "IN endpoint: " + intf.getEndpoint(i) );
                }
                else {
                    epOUT = intf.getEndpoint(i);
                    lgView.setText( lgView.getText() + "\n" + "OUT endpoint: " + intf.getEndpoint(i) );
                }
            } else { lgView.setText( lgView.getText() + "\n" + "no endpoints for INTERRUPT_TRANSFER"); }
        }

        mDevice = device;
        mEndpointIntr = epOUT;

        //открываем устройство для передачи данных
        if (device != null) {
            UsbDeviceConnection connection = mUsbManager.openDevice(device);
            if (connection != null && connection.claimInterface(intf, true)) {

                lgView.setText( lgView.getText() + "\n" + "open device SUCCESS!");
                mConnection = connection;

            } else {

                lgView.setText( lgView.getText() + "\n" + "open device FAIL!");
                mConnection = null;
            }
        }
    }

    void sendMessage(String message) {
        //определение размера буфера для отправки
        //исходя из максимального размера пакета
        int bufferDataLength = mEndpointIntr.getMaxPacketSize();
        lgView.setText( lgView.getText() + "\n" + mEndpointIntr.getMaxPacketSize() );

        ByteBuffer buffer = ByteBuffer.allocate(bufferDataLength + 1);
        UsbRequest request = new UsbRequest();
        buffer.put(message.getBytes());
        request.initialize(mConnection, mEndpointIntr);
        request.queue(buffer, bufferDataLength);

        try {
            if (request.equals(mConnection.requestWait())) {
                //отправка прошла успешно
                lgView.setText( lgView.getText() + "\n" +  "sending CLEAR!!!");
            }
        }
        catch (Exception ex) {
            //что-то не так...
            lgView.setText( lgView.getText() + "\n" +  "sending not clear...");
        }
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
