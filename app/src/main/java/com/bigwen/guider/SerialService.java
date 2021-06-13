package com.bigwen.guider;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.bigwen.guider.util.HexDump;
import com.bigwen.guider.util.ThreadUtil;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;

/**
 * Created by bigwen on 6/13/21.
 */
public class SerialService implements SerialInputOutputManager.Listener {

    private static final String TAG = "SerialUtil";
    private UsbSerialPort curPort;
    private SerialInputOutputManager usbIoManager;
    private Context mContext;
    private ReceiverCallback receiverCallback;

    public SerialService(Context context, ReceiverCallback callback) {
        mContext = context;
        receiverCallback = callback;
    }

    public void connect() {
        disconnect();

        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            Log.i(TAG, "connect: availableDrivers is empty");
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            Log.i(TAG, "connect: connection is null");
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            return;
        }

        UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            port.open(connection);
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            usbIoManager = new SerialInputOutputManager(port, this);
            usbIoManager.start();
            curPort = port;
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
            reconnect();
        }
    }

    public void disconnect() {
        if (curPort != null) {
            try {
                curPort.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        curPort = null;

        if (usbIoManager != null) {
            usbIoManager.stop();
        }
        usbIoManager = null;
    }

    public boolean write(byte[] request) {
        if (usbIoManager == null) return false;
        usbIoManager.writeAsync(request);
        return true;
    }

    @Override
    public void onNewData(byte[] data) {
        if (receiverCallback != null) receiverCallback.onNewData(HexDump.dumpHexString(data));
    }

    @Override
    public void onRunError(Exception e) {
        e.printStackTrace();
        disconnect();
        reconnect();
    }

    private void reconnect() {
        ThreadUtil.postDelay(new Runnable() {
            @Override
            public void run() {
                connect();
            }
        }, 1000 * 3);
    }

    public interface ReceiverCallback {
        void onNewData(String msg);
    }
}
