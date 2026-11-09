package com.hitunguang.feature.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.backup.BackupUserException
import com.hitunguang.core.backup.RestoreFiles
import com.hitunguang.core.backup.ZipHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RestoreFilesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun makeDb(file: File, version: Int, marker: String) {
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use {
            it.execSQL("CREATE TABLE t (v TEXT)")
            it.execSQL("INSERT INTO t VALUES ('$marker')")
            it.execSQL("PRAGMA user_version = $version")
        }
    }

    private fun readMarker(file: File): String =
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use {
            it.rawQuery("SELECT v FROM t", null).use { c -> c.moveToFirst(); c.getString(0) }
        }

    @Test
    fun validateAcceptsGoodDatabase() {
        val db = File(tmp.root, "ok.db")
        makeDb(db, 3, "x")
        RestoreFiles.validateDatabase(db)
    }

    @Test
    fun validateRejectsNewerSchema() {
        val db = File(tmp.root, "new.db")
        makeDb(db, 4, "x")
        try {
            RestoreFiles.validateDatabase(db)
            fail("expected failure")
        } catch (e: IllegalArgumentException) {
            assertTrue(context.getString((e as BackupUserException).messageRes).contains("versi aplikasi"))
        }
    }

    @Test
    fun validateRejectsGarbageFile() {
        val db = File(tmp.root, "bad.db")
        db.writeText("this is not a sqlite database at all, just text padding".repeat(20))
        try {
            RestoreFiles.validateDatabase(db)
            fail("expected failure")
        } catch (e: Exception) {
            assertTrue(e is IllegalArgumentException)
        }
    }

    @Test
    fun swapReplacesLiveDataAndDropsWal() {
        val staging = tmp.newFolder("staging")
        val dbDir = tmp.newFolder("databases")
        val filesDir = tmp.newFolder("files")
        val rollback = File(tmp.root, "rollback")
        makeDb(File(staging, "database/hitunguang.db"), 3, "new")
        File(staging, "attachments").mkdirs()
        File(staging, "attachments/a.txt").writeText("new-att")
        makeDb(File(dbDir, "hitunguang.db"), 3, "old")
        File(dbDir, "hitunguang.db-wal").writeText("stale")
        File(filesDir, "receipts").mkdirs()
        File(filesDir, "receipts/old.jpg").writeText("old")

        RestoreFiles.swap(staging, dbDir, filesDir, rollback)

        assertEquals("new", readMarker(File(dbDir, "hitunguang.db")))
        assertFalse(File(dbDir, "hitunguang.db-wal").exists())
        assertEquals("new-att", File(filesDir, "attachments/a.txt").readText())
        assertFalse(File(filesDir, "receipts").exists())
    }

    @Test
    fun swapRollsBackWhenCopyFails() {
        val staging = tmp.newFolder("staging")
        val dbDir = tmp.newFolder("databases")
        val filesDir = tmp.newFolder("files")
        val rollback = File(tmp.root, "rollback")
        makeDb(File(dbDir, "hitunguang.db"), 3, "old")
        File(filesDir, "attachments").mkdirs()
        File(filesDir, "attachments/keep.txt").writeText("keep")

        try {
            RestoreFiles.swap(staging, dbDir, filesDir, rollback)
            fail("expected failure")
        } catch (e: Exception) {
        }

        assertEquals("old", readMarker(File(dbDir, "hitunguang.db")))
        assertEquals("keep", File(filesDir, "attachments/keep.txt").readText())
    }

    @Test
    fun extractZipSkipsZipSlipEntries() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry("../evil.txt"))
            zip.write("evil".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("good/ok.txt"))
            zip.write("ok".toByteArray())
            zip.closeEntry()
        }
        val parent = tmp.newFolder("parent")
        val out = File(parent, "out")
        ZipHelper.extractZip(ByteArrayInputStream(baos.toByteArray()), out)

        assertFalse(File(parent, "evil.txt").exists())
        assertEquals("ok", File(out, "good/ok.txt").readText())
    }
}
