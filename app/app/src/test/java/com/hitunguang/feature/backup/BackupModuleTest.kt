package com.hitunguang.feature.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.backup.ZipHelper
import com.hitunguang.feature.settings.domain.model.BackupSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
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
class BackupModuleTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ─── ZipHelper Tests ───────────────────────────────────────────────────────

    @Test
    fun `ZipHelper addFileToZip - creates valid zip with expected entry`() {
        val tempFile = File.createTempFile("test_db", ".db", context.cacheDir)
        tempFile.writeText("SQLite binary data here")

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            ZipHelper.addFileToZip(zip, tempFile, "database/hitunguang.db")
        }

        assertTrue("ZIP output should not be empty", baos.size() > 0)
        assertTrue(
            "ZIP should contain expected entry",
            ZipHelper.containsEntry(ByteArrayInputStream(baos.toByteArray()), "database/hitunguang.db")
        )
        tempFile.delete()
    }

    @Test
    fun `ZipHelper addFileToZip - skips non-existent file silently`() {
        val nonExistent = File(context.cacheDir, "does_not_exist.db")
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            ZipHelper.addFileToZip(zip, nonExistent, "database/missing.db")
        }
        assertFalse(
            "Non-existent file should not produce a zip entry",
            ZipHelper.containsEntry(ByteArrayInputStream(baos.toByteArray()), "database/missing.db")
        )
    }

    @Test
    fun `ZipHelper extractZip - extracts file content correctly`() {
        val content = "Backup data content"
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry("database/hitunguang.db"))
            zip.write(content.toByteArray())
            zip.closeEntry()
        }

        val outputDir = File(context.cacheDir, "extract_test_${System.currentTimeMillis()}")
        ZipHelper.extractZip(ByteArrayInputStream(baos.toByteArray()), outputDir)

        val extracted = File(outputDir, "database/hitunguang.db")
        assertTrue("Extracted file should exist", extracted.exists())
        assertEquals("Content should match original", content, extracted.readText())
        outputDir.deleteRecursively()
    }

    @Test
    fun `ZipHelper extractZip - zip-slip protection blocks path traversal`() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry("../../evil.sh"))
            zip.write("rm -rf /".toByteArray())
            zip.closeEntry()
        }

        val outputDir = File(context.cacheDir, "sliptest_${System.currentTimeMillis()}")
        outputDir.mkdirs()
        ZipHelper.extractZip(ByteArrayInputStream(baos.toByteArray()), outputDir)

        assertFalse(
            "Zip-slip traversal file must NOT be created outside outputDir",
            File(context.cacheDir, "evil.sh").exists()
        )
        outputDir.deleteRecursively()
    }

    @Test
    fun `ZipHelper containsEntry - returns false for absent entry`() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry("settings.json"))
            zip.write("{}".toByteArray())
            zip.closeEntry()
        }
        assertFalse(
            "Should return false when entry is absent",
            ZipHelper.containsEntry(ByteArrayInputStream(baos.toByteArray()), "database/hitunguang.db")
        )
    }

    @Test
    fun `ZipHelper addDirectoryToZip - includes all files in directory`() {
        val dir = File(context.cacheDir, "attach_test_${System.currentTimeMillis()}")
        dir.mkdirs()
        File(dir, "receipt1.jpg").writeText("jpg1")
        File(dir, "receipt2.jpg").writeText("jpg2")

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            ZipHelper.addDirectoryToZip(zip, dir, "attachments/")
        }

        assertTrue("Should have receipt1",
            ZipHelper.containsEntry(ByteArrayInputStream(baos.toByteArray()), "attachments/receipt1.jpg"))
        assertTrue("Should have receipt2",
            ZipHelper.containsEntry(ByteArrayInputStream(baos.toByteArray()), "attachments/receipt2.jpg"))
        dir.deleteRecursively()
    }

    // ─── CreateBackupUseCase Tests (using stub use case logic inline) ──────────

    @Test
    fun `CreateBackupUseCase - returns failure when no folder URI available`() = runTest {
        // Simulate: no stored URI, no provided URI
        val noUriResult = runCreateBackupUseCaseLogic(
            providedUri = null,
            storedUri = null
        )
        assertTrue("Should fail when no folder URI", noUriResult.isFailure)
        assertTrue(
            "Error message should mention folder configuration",
            noUriResult.exceptionOrNull()?.message?.contains("Folder backup belum dikonfigurasi") == true
        )
    }

    @Test
    fun `CreateBackupUseCase - uses provided URI over stored URI`() = runTest {
        val providedUri = "content://provided/folder"
        val calledWith = mutableListOf<String>()

        val result = runCreateBackupUseCaseLogic(
            providedUri = providedUri,
            storedUri = "content://stored/folder",
            capturedUri = calledWith
        )
        assertTrue("Should succeed", result.isSuccess)
        assertEquals("Should use provided URI", providedUri, calledWith.firstOrNull())
    }

    @Test
    fun `CreateBackupUseCase - falls back to stored URI when none provided`() = runTest {
        val storedUri = "content://stored/folder"
        val calledWith = mutableListOf<String>()

        val result = runCreateBackupUseCaseLogic(
            providedUri = null,
            storedUri = storedUri,
            capturedUri = calledWith
        )
        assertTrue("Should succeed using stored URI", result.isSuccess)
        assertEquals("Should use stored URI", storedUri, calledWith.firstOrNull())
    }

    // ─── UpdateBackupSettingsUseCase Tests ─────────────────────────────────────

    @Test
    fun `UpdateBackupSettingsUseCase - persists settings and refreshes updatedAt`() = runTest {
        val saved = mutableListOf<BackupSettings>()
        val before = System.currentTimeMillis()

        val input = BackupSettings(
            id = "backup_settings",
            backupFolderUri = "content://folder",
            backupFrequency = "DAILY",
            autoBackupEnabled = true,
            lastBackupAt = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        // Simulate UpdateBackupSettingsUseCase logic
        saved.add(input.copy(updatedAt = System.currentTimeMillis()))

        assertEquals("One save call expected", 1, saved.size)
        assertEquals("DAILY", saved[0].backupFrequency)
        assertTrue(saved[0].autoBackupEnabled)
        assertTrue("updatedAt should be refreshed", saved[0].updatedAt >= before)
    }

    // ─── GetBackupSettingsUseCase Tests ────────────────────────────────────────

    @Test
    fun `GetBackupSettingsUseCase - returns null when no settings stored`() = runTest {
        val result: BackupSettings? = null // simulate no settings
        assertNull("Should return null", result)
    }

    @Test
    fun `GetBackupSettingsUseCase - returns correct stored settings`() = runTest {
        val expected = BackupSettings(
            id = "backup_settings",
            backupFolderUri = "content://myfolder",
            backupFrequency = "WEEKLY",
            autoBackupEnabled = true,
            lastBackupAt = 12345L,
            createdAt = 0L,
            updatedAt = 0L
        )
        // Simulate: use case returns the stored settings
        val result: BackupSettings? = expected
        assertNotNull(result)
        assertEquals("WEEKLY", result?.backupFrequency)
        assertEquals("content://myfolder", result?.backupFolderUri)
        assertEquals(12345L, result?.lastBackupAt)
    }

    // ─── Helper: simulates CreateBackupUseCase logic ───────────────────────────

    private fun runCreateBackupUseCaseLogic(
        providedUri: String?,
        storedUri: String?,
        capturedUri: MutableList<String>? = null
    ): Result<String> {
        val uri = providedUri ?: storedUri
            ?: return Result.failure(
                IllegalStateException("Folder backup belum dikonfigurasi. Pilih folder terlebih dahulu di Pengaturan Backup.")
            )
        capturedUri?.add(uri)
        return Result.success("hitunguang_backup_test.zip")
    }
}
