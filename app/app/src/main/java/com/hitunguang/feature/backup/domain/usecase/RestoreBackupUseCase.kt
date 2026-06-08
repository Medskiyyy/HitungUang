package com.hitunguang.feature.backup.domain.usecase

import android.net.Uri
import com.hitunguang.core.backup.RestoreManager
import javax.inject.Inject

class RestoreBackupUseCase @Inject constructor(
    private val restoreManager: RestoreManager
) {
    suspend operator fun invoke(zipUri: Uri): Result<Unit> {
        return restoreManager.restore(zipUri)
    }
}
