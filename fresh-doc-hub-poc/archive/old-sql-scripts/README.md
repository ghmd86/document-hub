# Archived SQL Scripts

These SQL scripts were archived on 2024-12-07 as they are no longer needed for the main application.

## Files Archived

### Fix Scripts (one-time fixes, already applied)
| File | Purpose |
|------|---------|
| `fix-api-paths.sql` | Fixed API endpoint paths |
| `fix-url-quick.sql` | Quick URL fixes |

### Test Data Scripts (superseded by data.sql)
| File | Purpose |
|------|---------|
| `test-data.sql` | Original test data |
| `test-data-postgres.sql` | PostgreSQL-specific test data |
| `test-data-disclosure-example-postgres.sql` | Disclosure matching test data |
| `test-3step-chain.sql` | 3-step chain test scenarios |
| `test-conditional-matching.sql` | Conditional matching tests |
| `sample-data-extraction-configs.sql` | Sample extraction configurations |

## Current Data Location

All schema and seed data is now consolidated in:
- `src/main/resources/data.sql`

## Note

These scripts are kept for reference. The fixes have already been applied, and the test data has been consolidated into the main data.sql file.
