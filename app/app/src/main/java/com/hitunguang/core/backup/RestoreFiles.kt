package com.hitunguang.core.backup

import android.database.sqlite.SQLiteDatabase
import java.io.File

object RestoreFiles {
    const val DB_NAME = "hitunguang.db"
    const val MAX_SCHEMA_VERSION = 3
    private val DB_SIDE_FILES = listOf("$DB_NAME-wal", "$DB_NAME-shm")
    private val DATA_DIRS = listOf("attachments", "receipts")

    fun validateDatabase(dbFile: File) {
        if (!dbFile.isFile) throw IllegalArgumentException("Isi cadangan nggak lengkap: databasenya nggak ketemu.")
        val db = try {
            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: Exception) {
            throw IllegalArgumentException("Cadangan ini rusak atau bukan cadangan yang benar.", e)
        }
        try {
            db.use { checkOpenDatabase(it) }
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Cadangan ini rusak.", e)
        }
    }

    private fun checkOpenDatabase(db: SQLiteDatabase) {
        val version = db.rawQuery("PRAGMA user_version", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
        if (version > MAX_SCHEMA_VERSION) {
            throw IllegalArgumentException("Cadangan ini dibuat dari versi aplikasi yang lebih baru. Perbarui aplikasinya dulu, ya.")
        }
        val integrity = db.rawQuery("PRAGMA integrity_check", null).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
        if (integrity != "ok") {
            throw IllegalArgumentException("Cadangan ini rusak.")
        }
    }

    fun swap(stagingDir: File, dbDir: File, filesDir: File, rollbackDir: File) {
        val liveDb = File(dbDir, DB_NAME)
        val rollbackDb = File(rollbackDir, "database")
        rollbackDir.deleteRecursively()
        rollbackDb.mkdirs()
        listOf(DB_NAME).plus(DB_SIDE_FILES).forEach { name ->
            val f = File(dbDir, name)
            if (f.exists()) f.copyTo(File(rollbackDb, name), overwrite = true)
        }
        DATA_DIRS.forEach { name ->
            val dir = File(filesDir, name)
            if (dir.exists()) dir.copyRecursively(File(rollbackDir, name), overwrite = true)
        }

        try {
            dbDir.mkdirs()
            DB_SIDE_FILES.forEach { File(dbDir, it).delete() }
            File(stagingDir, "database/$DB_NAME").copyTo(liveDb, overwrite = true)
            DATA_DIRS.forEach { name ->
                val target = File(filesDir, name)
                target.deleteRecursively()
                val source = File(stagingDir, name)
                if (source.isDirectory) source.copyRecursively(target, overwrite = true)
            }
        } catch (e: Exception) {
            try {
                rollback(rollbackDir, dbDir, filesDir)
            } catch (rollbackError: Exception) {
                e.addSuppressed(rollbackError)
            }
            throw e
        }
    }

    private fun rollback(rollbackDir: File, dbDir: File, filesDir: File) {
        val rollbackDb = File(rollbackDir, "database")
        listOf(DB_NAME).plus(DB_SIDE_FILES).forEach { name ->
            val live = File(dbDir, name)
            live.delete()
            val saved = File(rollbackDb, name)
            if (saved.exists()) saved.copyTo(live, overwrite = true)
        }
        DATA_DIRS.forEach { name ->
            val target = File(filesDir, name)
            target.deleteRecursively()
            val saved = File(rollbackDir, name)
            if (saved.isDirectory) saved.copyRecursively(target, overwrite = true)
        }
    }
}
