package chiefnavigator.compat;

import peerless.api.PeerlessSkills;

/** Reached only after checking that the optional mod is enabled. */
final class PeerlessRewardsBridge {
    private PeerlessRewardsBridge() { }

    static boolean awardOnce(String receipt) {
        return PeerlessSkills.awardOnce(receipt);
    }
}
