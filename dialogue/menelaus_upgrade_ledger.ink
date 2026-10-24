// Authoring mirror only. Runtime prose and options live in data/campaign/rules.csv.

=== menelaus_upgrade_ledger ===
Menelaus opens Ithaca's defense-integration ledger.

“What have you brought for Ithaca's defense?”

Installed upgrades: {installed} of {total}.
Recovered and awaiting installation: {available}.

// Runtime presentation: highlight each Installed status green and each
// Not recovered status red; leave other ledger text and pending installs default.
{upgrade_ledger}

+ [Deliver and install all recovered Ithaca wall upgrades.] -> menelaus_upgrade_ledger
+ [Back.] -> END
