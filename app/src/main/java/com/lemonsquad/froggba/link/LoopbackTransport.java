package com.lemonsquad.froggba.link;

import android.util.Log;

/**
 * In-process Loopback transport for testing Link Cable multiplayer
 * without physical network hardware.
 *
 * Simulates a 2-player link cable session:
 * - Player 0 = Local Console (Master)
 * - Player 1 = Virtual Console (Slave)
 * - Players 2 & 3 = Disconnected (0xFFFF)
 */
public class LoopbackTransport implements LinkTransport {

    private static final String TAG = "FrogEmu_Loopback";

    private volatile Listener mListener;
    private volatile boolean mRunning = false;
    private final int mConnectedDevices;

    public LoopbackTransport(int connectedDevices) {
        mConnectedDevices = Math.max(2, Math.min(4, connectedDevices));
    }

    @Override
    public void start() {
        mRunning = true;
        Log.i(TAG, "LoopbackTransport started with " + mConnectedDevices + " virtual players.");
        if (mListener != null) {
            for (int i = 1; i < mConnectedDevices; ++i) {
                mListener.onPeerConnected(i);
            }
        }
    }

    @Override
    public void stop() {
        mRunning = false;
        Log.i(TAG, "LoopbackTransport stopped.");
    }

    @Override
    public void sendTransfer(int playerId, int sequence, short localWord) {
        if (!mRunning || mListener == null) return;

        // Build simulated 4-console multiData array
        short[] multiData = new short[4];
        multiData[0] = (playerId == 0) ? localWord : (short) 0x1234;
        multiData[1] = (playerId == 1) ? localWord : (short) 0x5678;
        multiData[2] = (short) 0xFFFF; // Disconnected
        multiData[3] = (short) 0xFFFF; // Disconnected

        // In multiplayer, each console sends its own word. For loopback testing,
        // we echo a deterministic response for virtual slaves:
        if (playerId == 0) {
            multiData[1] = (short) (~localWord & 0xFFFF);
        }

        // Deliver resolved multi-payload back to LinkManager
        mListener.onTransferResolved(sequence, multiData);
    }

    @Override
    public void setListener(Listener listener) {
        mListener = listener;
    }
}
