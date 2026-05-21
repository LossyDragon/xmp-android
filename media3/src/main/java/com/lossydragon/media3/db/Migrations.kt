package com.lossydragon.media3.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS download_history (
                id        INTEGER NOT NULL PRIMARY KEY,
                filename  TEXT NOT NULL,
                songTitle TEXT NOT NULL,
                format    TEXT NOT NULL,
                bytes     INTEGER NOT NULL,
                artist    TEXT NOT NULL,
                viewedAt  INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
