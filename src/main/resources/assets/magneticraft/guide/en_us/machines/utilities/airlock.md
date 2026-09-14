# Airlock

The airlock periodically scans a bounded loaded area, keeps a stable bubble boundary against surrounding water, and removes water from the powered interior.

The server snapshots the area before applying an all-or-nothing energy plan. If power is insufficient, no partial mutation is performed; after power loss the maintained bubble decays safely.

The scan never force-loads chunks and ignores positions outside its active radius.
