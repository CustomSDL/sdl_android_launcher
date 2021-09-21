package org.luxoft.sdl_core;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class BleCentralService extends Service {
        public static final String TAG = BleCentralService.class.getSimpleName();
        public final static String ACTION_START_BLE = "ACTION_START_BLE";
        public final static String ACTION_SCAN_BLE = "ACTION_SCAN_BLE";
        public final static String ACTION_STOP_BLE = "ACTION_STOP_BLE";
        public final static String ON_BLE_PERIPHERAL_READY = "ON_BLE_PERIPHERAL_READY";
        public final static String ON_BLE_SCAN_STARTED = "ON_BLE_SCAN_STARTED";
        public final static String ON_NATIVE_BLE_READY = "ON_NATIVE_BLE_READY";
        public final static String ON_NATIVE_BLE_CONTROL_READY = "ON_NATIVE_BLE_CONTROL_READY";
        public final static String ON_MOBILE_MESSAGE_RECEIVED = "ON_MOBILE_MESSAGE_RECEIVED";
        public final static String MOBILE_DATA_EXTRA = "MOBILE_DATA_EXTRA";
        public final static String MOBILE_CONTROL_DATA_EXTRA = "MOBILE_CONTROL_DATA_EXTRA";
        public final static String MOBILE_DEVICE_DISCONNECTED_EXTRA = "MOBILE_DEVICE_DISCONNECTED_EXTRA";
        public final static String ON_MOBILE_CONTROL_MESSAGE_RECEIVED = "ON_MOBILE_CONTROL_MESSAGE_RECEIVED";
        public final static String ACTION_START_CLASSIC_BT ="ACTION_START_CLASSIC_BT";

        BLEHandler mBLEHandler;
        ClassicBTHandler mClassicBtHandler;
        JavaToNativeBleAdapter mNativeBleAdapterThread;
        BleAdapterWriteMessageCallback mCallback;

        @Override
        public void onCreate() {
            registerReceiver(centralServiceReceiver, makeCentralServiceIntentFilter());
            super.onCreate();
        }

        @Override
        public void onDestroy() {
            unregisterReceiver(centralServiceReceiver);
            super.onDestroy();
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        private void initBluetoothHandler(){
            mBLEHandler = BLEHandler.getInstance(this);
        }

        private void initClassicBTHandler(){
            mClassicBtHandler = ClassicBTHandler.getInstance(this);
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            return super.onStartCommand(intent, flags, startId);
        }

        private final BroadcastReceiver centralServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) {
                    return;
                }

                switch (intent.getAction()) {
                    case ACTION_START_BLE:
                        Log.i(TAG, "ACTION_START_BLE received by centralServiceReceiver");
                        mNativeBleAdapterThread = new JavaToNativeBleAdapter(BleCentralService.this);
                        mNativeBleAdapterThread.start();
                        break;

                    case ACTION_SCAN_BLE:
                        Log.i(TAG, "ACTION_SCAN_BLE received by centralServiceReceiver");
                        if (mBLEHandler != null) {
                            mBLEHandler.connect();
                        }

                        final Intent scan_started_intent = new Intent(ON_BLE_SCAN_STARTED);
                        context.sendBroadcast(scan_started_intent);
                        break;

                    case ACTION_STOP_BLE:
                        Log.i(TAG, "ACTION_STOP_BLE received by centralServiceReceiver");
                        mBLEHandler.disconnect();

                        try {
                            mNativeBleAdapterThread.setStopThread();
                            mNativeBleAdapterThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mNativeBleAdapterThread = null;

                        break;

                    case ON_NATIVE_BLE_READY:
                        Log.i(TAG, "ON_NATIVE_BLE_READY received by centralServiceReceiver");
                        mCallback = new BleAdapterWriteMessageCallback();
                        if (mNativeBleAdapterThread != null) {
                            mNativeBleAdapterThread.ReadMessageFromNative(mCallback);
                        }

                        break;

                    case ON_NATIVE_BLE_CONTROL_READY:
                        Log.i(TAG, "ON_NATIVE_BLE_CONTROL_READY received by centralServiceReceiver");
                        initBluetoothHandler();
                        break;

                    case ON_BLE_PERIPHERAL_READY:
                        Log.i(TAG, "ON_BLE_PERIPHERAL_READY received by centralServiceReceiver");
                        if (mNativeBleAdapterThread != null) {
                            mNativeBleAdapterThread.EstablishConnectionWithNative();
                        }

                        break;

                    case ON_MOBILE_MESSAGE_RECEIVED:
                        Log.i(TAG, "ON_MOBILE_MESSAGE_RECEIVED received by centralServiceReceiver");
                        byte[] mobile_message = intent.getByteArrayExtra(MOBILE_DATA_EXTRA);
                        if (mNativeBleAdapterThread != null) {
                            mNativeBleAdapterThread.ForwardMessageToNative(mobile_message);
                        }

                        break;

                    case ON_MOBILE_CONTROL_MESSAGE_RECEIVED:
                        Log.i(TAG, "ON_MOBILE_CONTROL_MESSAGE_RECEIVED received by centralServiceReceiver");
                        byte[] mobile_control_message = intent.getByteArrayExtra(MOBILE_CONTROL_DATA_EXTRA);
                        if (mNativeBleAdapterThread != null) {
                            mNativeBleAdapterThread.SendControlMessageToNative(mobile_control_message);
                            if (intent.getBooleanExtra(MOBILE_DEVICE_DISCONNECTED_EXTRA, false)) {
                                mNativeBleAdapterThread.CloseConnectionWithNative();
                            }
                        }
                        break;

                    case ACTION_START_CLASSIC_BT:
                        Log.i(TAG, "ACTION_START_CLASSIC_BT received by centralServiceReceiver");
                        mBLEHandler.disconnect();
                        initClassicBTHandler();
                        mClassicBtHandler.DoDiscovery();
                        break;

                    default:
                        Log.e(TAG, "Unexpected value: " + intent.getAction());
                }
            }
        };

    private static IntentFilter makeCentralServiceIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_START_BLE);
        intentFilter.addAction(ACTION_SCAN_BLE);
        intentFilter.addAction(ACTION_STOP_BLE);
        intentFilter.addAction(ON_NATIVE_BLE_READY);
        intentFilter.addAction(ON_NATIVE_BLE_CONTROL_READY);
        intentFilter.addAction(ON_BLE_PERIPHERAL_READY);
        intentFilter.addAction(ON_MOBILE_MESSAGE_RECEIVED);
        intentFilter.addAction(ON_MOBILE_CONTROL_MESSAGE_RECEIVED);
        intentFilter.addAction(ACTION_START_CLASSIC_BT);
        return intentFilter;
    }

    class BleAdapterWriteMessageCallback implements BleAdapterMessageCallback{
        public void OnMessageReceived(byte[] rawMessage) {
            mBLEHandler.writeMessage(rawMessage);
        }
    };
}

