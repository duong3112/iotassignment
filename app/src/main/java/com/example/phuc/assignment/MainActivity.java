package com.example.phuc.assignment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.example.phuc.assignment.model.Data;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {
    private static final String TAG = "Gateway Upload";

    private static final int BAUD_RATE = 115200;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = 1;

    private static final int CHUNK_SIZE = 512;
    private UartDevice mUartDevice;

    private static final String UART_PIN_NAME = "UART0";

    private Handler mHandler;
    private DatabaseReference mDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        initUart();

        mHandler.post(mRunnable);


    }
    private void initUart()
    {
        try {
            openUart(UART_PIN_NAME, BAUD_RATE);
        }catch (IOException e) {
            Log.d(TAG, "Error on UART API");
        }
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            uploadUartData();
        }
    };

    private UartDeviceCallback mCallback = new UartDeviceCallback() {
        @Override
        public boolean onUartDeviceDataAvailable(UartDevice uartDevice) {
            uploadUartData();
            return true;
        }
    };

    private void openUart(String name, int baudRate) throws IOException {
        mUartDevice = PeripheralManager.getInstance().openUartDevice(name);
        // Configure the UART
        mUartDevice.setBaudrate(baudRate);
        mUartDevice.setDataSize(DATA_BITS);
        mUartDevice.setParity(UartDevice.PARITY_NONE);
        mUartDevice.setStopBits(STOP_BITS);

        mUartDevice.registerUartDeviceCallback(mCallback);
    }

    private void uploadUartData(){
        if(mUartDevice == null)
            return;
        try{
            byte[] buffer = new byte[CHUNK_SIZE];
            int noBytes = -1;
            String strRecv = "";
            while((noBytes = mUartDevice.read(buffer, buffer.length)) > 0){
                strRecv += new String(buffer, 0, noBytes, "UTF-8");
                processRecvData(strRecv);
            }
        }catch (IOException e){
            Log.w(TAG, "Unable to receive data over UART", e);
        }
    }

    private void processRecvData(String strRecv){
        int indexStart = strRecv.indexOf('[');
        int indexEnd = -1;
        if(indexStart >= 0){
            indexEnd = strRecv.indexOf(']', indexStart + 2);
            if(indexEnd >= 0){
                String data = strRecv.substring(indexStart + 1, indexEnd);
                Log.d(TAG, "Processing data...:" + data);
                String[] splitData = data.split(Pattern.quote(";"));
                if(splitData.length != 2)
                    return;

                float temperature = Float.parseFloat(splitData[0]);
                float humidity = Float.parseFloat(splitData[1]);
                Data  uploadData = new Data(temperature, humidity);
                uploadDatatoFireBase(uploadData);
            }
        }
    }

    private void uploadDatatoFireBase(Data uploadData){
        String key = mDatabaseReference.child("data").push().getKey();
        Map<String, Object> dataValues = uploadData.toMap();
        Map<String, Object> childUpdate = new HashMap<>();

        childUpdate.put("/data/" + key, dataValues);
        mDatabaseReference.updateChildren(childUpdate);
    }

    private void closeUart() throws IOException {
        if (mUartDevice != null) {
            mUartDevice.unregisterUartDeviceCallback(mCallback);
            try {
                mUartDevice.close();
            } finally {
                mUartDevice = null;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            closeUart();
            mUartDevice.unregisterUartDeviceCallback(mCallback);
        } catch (IOException e) {
            Log.e(TAG, "Error closing UART device:", e);
        }
    }
}
