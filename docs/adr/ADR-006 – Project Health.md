Health is calculated using pending cases and ETD:

Green: default
Amber: pending cases and ETD within 5 days
Red: pending cases and ETD within 2 days

| Condition                         | Health   |
| --------------------------------- | -------- |
| Pending = 0                       | 🟢 Green |
| Pending > 0 AND daysRemaining > 5 | 🟢 Green |
| Pending > 0 AND daysRemaining ≤ 5 | 🟡 Amber |
| Pending > 0 AND daysRemaining ≤ 2 | 🔴 Red   |


Removed Projects

Display Notes column as Current Status.

Deletion logic deferred.
