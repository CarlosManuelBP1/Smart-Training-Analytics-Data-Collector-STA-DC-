package com.sta.datacollector.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `training_sessions` (
                `id` TEXT NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `endedAt` INTEGER,
                `status` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_training_sessions_startedAt` " +
                "ON `training_sessions` (`startedAt`)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_training_sessions_status` " +
                "ON `training_sessions` (`status`)"
        )

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `session_participants` (
                `sessionId` TEXT NOT NULL,
                `participantId` TEXT NOT NULL,
                PRIMARY KEY(`sessionId`, `participantId`),
                FOREIGN KEY(`sessionId`) REFERENCES `training_sessions`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`participantId`) REFERENCES `participants`(`id`)
                    ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent()
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_session_participants_participantId` " +
                "ON `session_participants` (`participantId`)"
        )

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercise_sets` (
                `id` TEXT NOT NULL,
                `sessionId` TEXT NOT NULL,
                `participantId` TEXT NOT NULL,
                `exercise` TEXT NOT NULL,
                `loadKg` REAL NOT NULL,
                `repetitions` INTEGER NOT NULL,
                `setOrder` INTEGER NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `completedAt` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`sessionId`, `participantId`)
                    REFERENCES `session_participants`(`sessionId`, `participantId`)
                    ON UPDATE CASCADE ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercise_sets_sessionId_participantId` " +
                "ON `exercise_sets` (`sessionId`, `participantId`)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercise_sets_sessionId_startedAt` " +
                "ON `exercise_sets` (`sessionId`, `startedAt`)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercise_sets_participantId` " +
                "ON `exercise_sets` (`participantId`)"
        )
    }
}
