package org.luxoft.sdl_core;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class BluetoothLongWriter {
    private static final String TAG = BluetoothLongWriter.class.getSimpleName();

    private int mMtu = 23;
    private final int mFramesCountBytes = 4;
    private final int mReserved = 3 + mFramesCountBytes; // 3 bytes for internal information and 4 bytes for frames length
    private boolean mSendInProgress = false;
    private ArrayList<byte[]> mMessagesToSend = new ArrayList<>();
    private LongWriterCallback mCallback = null;

    public interface LongWriterCallback {
        void OnLongMessageReady(byte[] message);
    }

    public void setMtu(final int value) {
        Log.d(TAG, "New MTU value is " + value);
        mMtu = value;
    }

    public void setCallback(LongWriterCallback callback) {
        Log.d(TAG, "New callback is set");
        mCallback = callback;
    }

    public void resetBuffer() {
        Log.d(TAG, "Resetting writer queue");
        mMessagesToSend.clear();
        mSendInProgress = false;
    }

    private byte[] prepareMessageToSend(final int frames_left, final byte[] message) {
        ByteBuffer buffer = ByteBuffer.allocate(message.length + mFramesCountBytes);
        buffer.putInt(frames_left);
        buffer.put(message);
        return buffer.array();
    }

    private void triggerNextMessageProcessing() {
        if (!mSendInProgress) {
            if (mMessagesToSend.isEmpty()) {
                Log.d(TAG, "No new messages in the queue");
                return;
            }

            Log.d(TAG, "Sending next message in the queue");
            mSendInProgress = true;
            byte[] message = mMessagesToSend.get(0);
            mMessagesToSend.remove(0);
            if (mCallback != null) {
                mCallback.OnLongMessageReady(message);
            }
        }
    }

    public void onLongMessageSent() {
        mSendInProgress = false;
        triggerNextMessageProcessing();
    }

    public void processWriteOperation(final byte[] message) {
        Log.d(TAG, "Going to send message of size " + message.length);

        final int max_message_size = mMtu - mReserved;
        if (message.length < max_message_size) {
            byte[] new_message = prepareMessageToSend(0, message);
            Log.d(TAG, "Entire message of size " + new_message.length + " will be sent");
            mMessagesToSend.add(new_message);
        } else {
            Log.d(TAG, "Splitting message to chunks");
            int frames_left = message.length / max_message_size;
            int mod = message.length % max_message_size;
            if(mod != 0){
                frames_left += 1;
            }
            for (int offset = 0, i = frames_left; i > 0; --i, offset += max_message_size) {
                byte[] new_message = prepareMessageToSend(i - 1, Arrays.copyOfRange(message, offset, offset + max_message_size));
                mMessagesToSend.add(new_message);
            }
            Log.d(TAG, "Message was split on " + frames_left + " frames");
        }

        triggerNextMessageProcessing();
    }
}
