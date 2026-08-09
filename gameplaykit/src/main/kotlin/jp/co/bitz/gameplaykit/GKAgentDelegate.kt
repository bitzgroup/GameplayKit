package jp.co.bitz.gameplaykit

// Synchronizes a GKAgent's simulated state with an external representation (e.g. a rendered node),
// mirroring GameplayKit's GKAgentDelegate. Both methods default to no-ops, matching Apple's optional
// protocol methods.
public interface GKAgentDelegate {
    public fun agentWillUpdate(agent: GKAgent) {}

    public fun agentDidUpdate(agent: GKAgent) {}
}
