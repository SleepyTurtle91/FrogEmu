package com.lemonsquad.froggba.link;

import android.util.Log;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the Link Cable session, orchestrating data exchange between
 * EmulationThread and the active LinkTransport.
 *
 * Invariant:
 * - Transport events (incoming network packets) publish data to LinkManager.
 * - ONLY EmulationThread polls LinkManager and invokes completeLinkTransferJNI().
 */
public class LinkManager implements LinkTransport.Listener {

    private static final String TAG = "FroggBA_Link";

    public enum Mode {
        DISCONNECTED,
        MASTER,  // Player 0
        SLAVE    // Player 1..3
    }

    private volatile Mode mMode = Mode.DISCONNECTED;
    private final AtomicInteger mPlayerId = new AtomicInteger(0);
    private final AtomicInteger mConnectedDevices = new AtomicInteger(1);
    private final AtomicInteger mSequence = new AtomicInteger(0);

    private volatile LinkTransport mTransport;

    // Resolved transfer waiting to be injected into the core by EmulationThread
    private final AtomicReference<short[]> mPendingResolvedData = new AtomicReference<>(null);

    public LinkManager() {}

    public void attachTransport(LinkTransport transport, Mode mode, int playerId, int numDevices) {
        if (mTransport != null) {
            mTransport.stop();
        }
        mTransport = transport;
        mMode = mode;
        mPlayerId.set(playerId);
        mConnectedDevices.set(numDevices);

        if (mTransport != null) {
            mTransport.setListener(this);
            mTransport.start();
        }
        Log.i(TAG, "LinkManager attached: mode=" + mode + ", playerId=" + playerId + ", numDevices=" + numDevices);
    }

    public void detachTransport() {
        if (mTransport != null) {
            mTransport.stop();
            mTransport = null;
        }
        mMode = Mode.DISCONNECTED;
        mPendingResolvedData.set(null);
        Log.i(TAG, "LinkManager detached.");
    }

    public boolean isConnected() {
        return mMode != Mode.DISCONNECTED && mTransport != null;
    }

    public int getPlayerId() {
        return mPlayerId.get();
    }

    public int getConnectedDevices() {
        return mConnectedDevices.get();
    }

    /**
     * Called by EmulationThread when mGBA initiates a transfer.
     */
    public void onLocalTransferRequest(short localWord) {
        if (!isConnected()) return;
        int seq = mSequence.incrementAndGet();
        mTransport.sendTransfer(mPlayerId.get(), seq, localWord);
    }

    /**
     * Called by EmulationThread to retrieve resolved 4-console payload.
     * Returns null if no resolved transfer is waiting.
     */
    public short[] pollResolvedTransfer() {
        return mPendingResolvedData.getAndSet(null);
    }

    // ── LinkTransport.Listener callbacks (runs on transport/network thread) ──

    @Override
    public void onTransferResolved(int sequence, short[] multiData4) {
        if (multiData4 != null && multiData4.length == 4) {
            mPendingResolvedData.set(multiData4);
        }
    }

    @Override
    public void onPeerConnected(int playerId) {
        Log.i(TAG, "Peer connected: Player " + playerId);
    }

    @Override
    public void onPeerDisconnected(int playerId) {
        Log.i(TAG, "Peer disconnected: Player " + playerId);
    }
}
