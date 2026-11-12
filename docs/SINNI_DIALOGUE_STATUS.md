# Sinni dialogue status

## Open issue — October 23, 2026

**Sinni's dialogue is NOT dynamically changing in the reported local test.**

The Eventide route/preparation scene shows the 50-deployment-point limit and
the fixed narration that many of the player's largest ships are automatically
greyed out. The preparation advice and this fleet-assessment narration are
authored text; they do not currently evaluate the player's actual fleet.

The existing configurable route-limit token and the optional response at
240 DP or above remain in the source. Their presence does not resolve the
reported live behavior. The dialogue and presentation update does not claim
to fix dynamic adaptation.

Follow up by reproducing the briefing with different LunaLib fleet limits
and fleet compositions, then checking the active build, setting values,
token replacement, conditional options, and fleet-specific narration.
