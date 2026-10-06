package com.taka.personalfinance.data

import androidx.room.migration.Migration

/**
 * Database upgrade steps.
 *
 * HOW TO CHANGE THE DATABASE IN A FUTURE VERSION (so users never lose their data):
 *  1. Change the entity classes in Entities.kt.
 *  2. Increase `version` in AppDatabase.kt by one (e.g. 1 -> 2).
 *  3. Add a Migration(1, 2) below that changes the tables with SQL (ALTER TABLE ...).
 *  4. Add it to ALL_MIGRATIONS.
 * The app never uses "destructive" migration, so existing data is always kept.
 * Room also writes a schema file to app/schemas/ on every build; keep those files in git.
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    // Version 1 is the first public database layout, so there is nothing to migrate yet.
)
