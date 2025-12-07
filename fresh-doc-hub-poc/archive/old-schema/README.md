# Archived Schema Files

These schema files were archived on 2024-12-07 because they contain outdated schema definitions that don't match the current production database.

## Files Archived

| File | Description | Why Archived |
|------|-------------|--------------|
| `database.sql` | Original schema draft | Outdated column names, missing columns |
| `schema.sql` | R2DBC schema attempt | Incorrect syntax, not used |
| `schema-postgres.sql` | PostgreSQL-specific schema | Replaced by data.sql |

## Current Schema Location

The **current and authoritative schema** is defined in:
- `src/main/resources/data.sql` - Contains both schema (CREATE TABLE) and seed data (INSERT)

## Key Differences

The old schemas had issues like:
- `is_shared` instead of `shared_flag`
- `is_accessible` instead of `accessible_flag`
- Missing `template_config` column
- Incorrect BIT vs BOOLEAN types
- Missing `reference_key_config` in template_config

## Do Not Use

These files are kept for reference only. Do not run them against the database.
