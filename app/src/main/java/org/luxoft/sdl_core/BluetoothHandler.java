package org.luxoft.sdl_core;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.GattStatus;
import com.welie.blessed.HciStatus;
import com.welie.blessed.WriteType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static org.luxoft.sdl_core.BleCentralService.ACTION_SCAN_BLE;
import static org.luxoft.sdl_core.BleCentralService.MOBILE_DATA_EXTRA;
import static org.luxoft.sdl_core.BleCentralService.MOBILE_DEVICE_DISCONNECTED_EXTRA;
import static org.luxoft.sdl_core.BleCentralService.ON_MOBILE_MESSAGE_RECEIVED;
import static org.luxoft.sdl_core.BleCentralService.ON_BLE_PERIPHERAL_READY;
import static org.luxoft.sdl_core.BleCentralService.ON_MOBILE_CONTROL_MESSAGE_RECEIVED;
import static org.luxoft.sdl_core.BleCentralService.MOBILE_CONTROL_DATA_EXTRA;

import static org.luxoft.sdl_core.BluetoothBleContract.PARAM_ACTION;
import static org.luxoft.sdl_core.BluetoothBleContract.PARAM_NAME;
import static org.luxoft.sdl_core.BluetoothBleContract.PARAM_ADDRESS;
import static org.luxoft.sdl_core.BluetoothBleContract.PARAMS;

class BluetoothHandler {
    public BluetoothCentralManager central;
    private static BluetoothHandler instance = null;
    private BluetoothPeripheral mPeripheral = null;
    private final Context context;
    private final Handler handler = new Handler();
    private final BluetoothLongReader mLongReader = new BluetoothLongReader();
    private final BluetoothLongWriter mLongWriter = new BluetoothLongWriter();

    public static final String TAG = BluetoothHandler.class.getSimpleName();

    // To request a maximum MTU
    public static final int PREFERRED_MTU = 512;

    // Service with ability to notify and write
    private static final UUID SDL_TESTER_SERVICE_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805f9b34fb");

    // Characteristic for notifications
    private static final UUID MOBILE_NOTIFICATION_CHARACTERISTIC = UUID
            .fromString("00001102-0000-1000-8000-00805f9b34fb");

    // Characteristic with permissions to write
    private static final UUID MOBILE_RESPONSE_CHARACTERISTIC = UUID
            .fromString("00001104-0000-1000-8000-00805f9b34fb");

    private String GenerateDisconnectMessage(BluetoothPeripheral peripheral) {

        try {
            JSONObject message = new JSONObject();
            message.put(PARAM_ACTION, "ON_DEVICE_DISCONNECTED");

            JSONObject params = new JSONObject();
            params.put(PARAM_ADDRESS, peripheral.getAddress());
            message.put(PARAMS, params);
            return message.toString();
        } catch (JSONException ex) {
            Log.i(TAG, "ON_DEVICE_DISCONNECTED msg Failed", ex);
        }
        return null;
    }

    private String GenerateConnectedMessage(BluetoothPeripheral peripheral) {

        try {
            JSONObject message = new JSONObject();
            message.put(PARAM_ACTION, "ON_DEVICE_CONNECTED");
            JSONObject params = new JSONObject();
            params.put(PARAM_NAME, peripheral.getName());
            params.put(PARAM_ADDRESS, peripheral.getAddress());
            message.put(PARAMS, params);
            return message.toString();
        } catch (JSONException ex) {
            Log.i(TAG, "ON_DEVICE_CONNECTED msg Failed", ex);
        }
        return null;
    }

    public static synchronized BluetoothHandler getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothHandler(context);
        }
        return instance;
    }

    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        @Override
        public void onServicesDiscovered(BluetoothPeripheral peripheral) {

            peripheral.requestMtu(PREFERRED_MTU);

            // Try to turn on notification
            peripheral.setNotify(SDL_TESTER_SERVICE_UUID, MOBILE_NOTIFICATION_CHARACTERISTIC, true);

            final Intent intent = new Intent(ON_BLE_PERIPHERAL_READY);
            context.sendBroadcast(intent);
        }

        @Override
        public void onMtuChanged(BluetoothPeripheral peripheral, int mtu, GattStatus status) {
            if (status == GattStatus.SUCCESS) {
                mLongReader.setMtu(mtu);
                mLongWriter.setMtu(mtu);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, GattStatus status) {
            if (status == GattStatus.SUCCESS) {
                Log.i(TAG, "SUCCESS: Writing to " + characteristic.getUuid());
                mLongWriter.onLongMessageSent();
            } else {
                Log.i(TAG, "ERROR: Failed writing to " + characteristic.getUuid() + " with " + status);
            }
        }

        @Override
        public void onCharacteristicUpdate(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, GattStatus status) {
            if (status != GattStatus.SUCCESS) return;

            UUID characteristicUUID = characteristic.getUuid();
            if (characteristicUUID.equals(MOBILE_NOTIFICATION_CHARACTERISTIC)) {
                mLongReader.processReadOperation(value);
            }
        }
    };

    // Callback for central
    private final BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {

        @Override
        public void onConnectedPeripheral(BluetoothPeripheral peripheral) {
            Log.i(TAG, "connected to " + peripheral.getName());
            mPeripheral = peripheral;

            String ctrl_msg = GenerateConnectedMessage(peripheral);
            if(ctrl_msg != null) {
                final Intent intent = new Intent(ON_MOBILE_CONTROL_MESSAGE_RECEIVED);
                intent.putExtra(MOBILE_CONTROL_DATA_EXTRA, ctrl_msg.getBytes());
                context.sendBroadcast(intent);
            }
        }

        @Override
        public void onConnectionFailed(BluetoothPeripheral peripheral, final HciStatus status) {
            Log.e(TAG, "connection " + peripheral.getName() + " failed with status " + status);
        }

        @Override
        public void onDisconnectedPeripheral(final BluetoothPeripheral peripheral, final HciStatus status) {
            Log.d(TAG, "Disconnected from " + peripheral.getName());

            if (mPeripheral != null && peripheral.getAddress().equals(mPeripheral.getAddress())) {

                String ctrl_msg = GenerateDisconnectMessage(peripheral);
                if(ctrl_msg != null) {
                    final Intent intent = new Intent(ON_MOBILE_CONTROL_MESSAGE_RECEIVED);
                    intent.putExtra(MOBILE_CONTROL_DATA_EXTRA, ctrl_msg.getBytes());
                    intent.putExtra(MOBILE_DEVICE_DISCONNECTED_EXTRA, true);
                    context.sendBroadcast(intent);
                }

                mLongReader.resetBuffer();
                mLongWriter.resetBuffer();

                // Restart devices scanning
                final Intent scan_ble = new Intent(ACTION_SCAN_BLE);
                context.sendBroadcast(scan_ble);
            }
        }

        @Override
        public void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
            Log.v(TAG, "Found peripheral " + peripheral.getName());
            central.stopScan();
            central.connectPeripheral(peripheral, peripheralCallback);
        }
    };

    public void writeMessage(byte[] message){

        if (mPeripheral == null) {
            Log.e(TAG, "mPeripheral is null");
            return;
        }


        mLongWriter.processWriteOperation(message);
    }

    private BluetoothHandler(Context context) {
        this.context = context;

        mLongReader.setCallback(new BluetoothLongReader.LongReaderCallback() {
            @Override
            public void OnLongMessageReceived(byte[] message) {
                final Intent intent = new Intent(ON_MOBILE_MESSAGE_RECEIVED);
                intent.putExtra(MOBILE_DATA_EXTRA, message);
                BluetoothHandler.this.context.sendBroadcast(intent);
            }
        });

        mLongWriter.setCallback(new BluetoothLongWriter.LongWriterCallback() {
            @Override
            public void OnLongMessageReady(byte[] message) {
                BluetoothGattCharacteristic responseCharacteristic = mPeripheral.getCharacteristic(SDL_TESTER_SERVICE_UUID, MOBILE_RESPONSE_CHARACTERISTIC);
                if (responseCharacteristic != null) {
                    if ((responseCharacteristic.getProperties() & PROPERTY_WRITE) > 0) {
                        mPeripheral.writeCharacteristic(responseCharacteristic, message, WriteType.WITH_RESPONSE);
                    }
                }
            }
        });
    }

    public void disconnect() {
        Log.d(TAG, "Closing bluetooth handler...");
        handler.removeCallbacksAndMessages(null);
        if (central != null) {
            if (mPeripheral != null) {
                central.cancelConnection(mPeripheral);
                mPeripheral = null;
            }
            central.close();
            central = null;
        }
    }

    public void connect() {
        Log.d(TAG, "Prepare to start scanning...");
        central = new BluetoothCentralManager(context, bluetoothCentralManagerCallback, new Handler());

        // Scan for peripherals with a certain service UUIDs
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Searching for SDL-compatible peripherals...");
                UUID[] servicesToSearch = new UUID[1];
                servicesToSearch[0] = SDL_TESTER_SERVICE_UUID;
                central.scanForPeripheralsWithServices(servicesToSearch);
            }
        }, 1000);
    }
}
