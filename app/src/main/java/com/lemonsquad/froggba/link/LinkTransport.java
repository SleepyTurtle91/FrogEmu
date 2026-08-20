package com.lemonsquad.froggba.link;

/**
 * Transport-agnostic interface for Link Cable data exchange.
 *
 * Invariant: Implementations of LinkTransport must NEVER touch mCore,
 * GBA registers, or mGBA SIO state. All interaction with the emulator
 * core is mediated by LinkManager and EmulationThread.
 */
public interface LinkTransport {

    void start();
    void stop();

    /** Send local 16-bit word for sequence. */
    void sendTransfer(int playerId, int sequence, short localWord);

    void setListener(Listener listener);

    interface Listener {
        /**
         * Fired when all active players' 16-bit words for sequence have been collected.
         * multiData4 contains the 4 words (SIOMULTI0..3).
         */
        void onTransferResolved(int sequence, short[] multiData4);

        void onPeerConnected(int playerId);
        void onPeerDisconnected(int playerId);
    }
}
