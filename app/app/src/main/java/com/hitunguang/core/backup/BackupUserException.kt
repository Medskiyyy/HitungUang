package com.hitunguang.core.backup

import androidx.annotation.StringRes

open class BackupUserException(
    @StringRes val messageRes: Int,
    cause: Throwable? = null
) : IllegalArgumentException("Backup error (string resource $messageRes)", cause)
