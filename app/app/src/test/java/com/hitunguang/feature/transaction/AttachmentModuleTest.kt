package com.hitunguang.feature.transaction

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.database.entity.AttachmentEntity
import com.hitunguang.core.database.entity.TransactionEntity
import com.hitunguang.core.filemanager.AttachmentFileManager
import com.hitunguang.feature.transaction.data.repository.AttachmentRepositoryImpl
import com.hitunguang.feature.transaction.domain.model.Attachment
import com.hitunguang.feature.transaction.domain.repository.AttachmentRepository
import com.hitunguang.feature.transaction.domain.usecase.AddAttachmentUseCase
import com.hitunguang.feature.transaction.domain.usecase.DeleteAttachmentUseCase
import com.hitunguang.feature.transaction.domain.usecase.GetAttachmentsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class AttachmentModuleTest {

    private lateinit var context: Context
    private lateinit var db: HitungUangDatabase
    private lateinit var fileManager: AttachmentFileManager
    private lateinit var repository: AttachmentRepository
    
    private lateinit var addAttachmentUseCase: AddAttachmentUseCase
    private lateinit var deleteAttachmentUseCase: DeleteAttachmentUseCase
    private lateinit var getAttachmentsUseCase: GetAttachmentsUseCase

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        fileManager = AttachmentFileManager(context)
        repository = AttachmentRepositoryImpl(db.attachmentDao())
        
        addAttachmentUseCase = AddAttachmentUseCase(repository, fileManager)
        deleteAttachmentUseCase = DeleteAttachmentUseCase(repository, fileManager)
        getAttachmentsUseCase = GetAttachmentsUseCase(repository)

        // Setup common account
        db.accountDao().insertAccount(
            AccountEntity("acc-1", "Dompet Tunai", "CASH", null, 100000L, 100000L, 1000L, 1000L)
        )
        // Setup common transaction
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = "tx-1",
                accountId = "acc-1",
                categoryId = null,
                receiptId = null,
                transactionType = "EXPENSE",
                title = "Makan",
                note = "Enak",
                amount = 20000L,
                transactionDate = System.currentTimeMillis(),
                isDeleted = false,
                deletedAt = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
        // Clean up files in attachments directory
        val attachmentsDir = fileManager.attachmentsDir
        attachmentsDir.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun testAttachmentDao_CRUD() {
        runBlocking {
            val dao = db.attachmentDao()
            val attachment = AttachmentEntity(
                id = "att-1",
                transactionId = "tx-1",
                filePath = "/fake/path/1.jpg",
                mimeType = "image/jpeg",
                fileSize = 1024L,
                createdAt = System.currentTimeMillis()
            )

            // Insert
            dao.insertAttachment(attachment)

            // Count
            val count = dao.getAttachmentCountForTransaction("tx-1")
            assertEquals(1, count)

            // Read
            val list = dao.getAttachmentsByTransactionId("tx-1").first()
            assertEquals(1, list.size)
            assertEquals("att-1", list[0].id)
            assertEquals("/fake/path/1.jpg", list[0].filePath)

            // Delete
            dao.deleteAttachment(attachment)
            val countAfterDelete = dao.getAttachmentCountForTransaction("tx-1")
            assertEquals(0, countAfterDelete)
        }
    }

    @Test
    fun testAttachmentDao_CascadeDelete() {
        runBlocking {
            val dao = db.attachmentDao()
            val attachment = AttachmentEntity(
                id = "att-1",
                transactionId = "tx-1",
                filePath = "/fake/path/1.jpg",
                mimeType = "image/jpeg",
                fileSize = 1024L,
                createdAt = System.currentTimeMillis()
            )
            dao.insertAttachment(attachment)

            // Delete the transaction
            val tx = db.transactionDao().getTransactionById("tx-1").first()
            assertNotNull(tx)
            db.transactionDao().deleteTransaction(tx!!)

            // Check if attachment is deleted due to foreign key cascade
            val count = dao.getAttachmentCountForTransaction("tx-1")
            assertEquals(0, count)
        }
    }

    @Test
    fun testAddAttachmentUseCase_LimitOf5() {
        runBlocking {
            // Create a fake bitmap file to use as source Uri
            val sourceFile = File(context.cacheDir, "test_source.jpg")
            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            FileOutputStream(sourceFile).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
            val sourceUri = Uri.fromFile(sourceFile)

            // Add 5 attachments
            val addedAttachments = mutableListOf<Attachment>()
            repeat(5) {
                val attachment = addAttachmentUseCase("tx-1", sourceUri)
                addedAttachments.add(attachment)
                
                // Check file exists
                val file = File(attachment.filePath)
                assertTrue(file.exists())
            }

            // Verify total count is 5
            val count = repository.getAttachmentCount("tx-1")
            assertEquals(5, count)

            // The 6th should throw Exception
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    addAttachmentUseCase("tx-1", sourceUri)
                }
            }

            // Clean up source file
            sourceFile.delete()
        }
    }

    @Test
    fun testDeleteAttachmentUseCase() {
        runBlocking {
            // Create fake file
            val sourceFile = File(context.cacheDir, "test_source.jpg")
            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            FileOutputStream(sourceFile).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
            val sourceUri = Uri.fromFile(sourceFile)

            // Add attachment
            val attachment = addAttachmentUseCase("tx-1", sourceUri)
            val savedFile = File(attachment.filePath)
            assertTrue(savedFile.exists())

            // Delete attachment
            deleteAttachmentUseCase(attachment)

            // Verify record is gone from DB
            val count = repository.getAttachmentCount("tx-1")
            assertEquals(0, count)

            // Verify file is deleted from disk
            assertFalse(savedFile.exists())

            // Clean up source file
            sourceFile.delete()
        }
    }

    @Test
    fun testAttachmentFileManager_Compression() {
        runBlocking {
            // Create a large 500x500 source bitmap
            val sourceFile = File(context.cacheDir, "large_source.jpg")
            val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
            FileOutputStream(sourceFile).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
            val sourceUri = Uri.fromFile(sourceFile)

            val uncompressedSize = sourceFile.length()

            // Compress and save using FileManager
            val compressedFile = fileManager.saveAttachment(sourceUri)
            assertTrue(compressedFile.exists())
            assertEquals("jpg", compressedFile.extension)

            val compressedSize = compressedFile.length()

            // Size should be smaller or equal
            assertTrue("Compressed size ($compressedSize) should be less than or equal to uncompressed size ($uncompressedSize)", compressedSize <= uncompressedSize)

            // Clean up files
            sourceFile.delete()
            compressedFile.delete()
        }
    }
}
