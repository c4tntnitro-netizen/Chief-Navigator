// Authoring mirror only. Runtime prose and options live in data/campaign/rules.csv.

=== menelaus_upgrade_ledger ===
Menelaus opens Ithaca's defense-integration ledger.

“Recovered components are inert cargo. Transfer authorization is final: Ithaca consumes each physical unit while installing it in the defense wall, and the corresponding upgrade becomes permanent.”

Labor II requires four installed upgrades. All six components remain independently tracked and may be recovered and installed.

Installed upgrades: {installed} of {total}.
Recovered and awaiting installation: {available}.

// Runtime presentation: highlight each Installed status green and each
// Not recovered status red; leave other ledger text and pending installs default.
{upgrade_ledger}

+ [Deliver and install all recovered Ithaca wall upgrades.] -> menelaus_upgrade_ledger
+ [Back.] -> END
