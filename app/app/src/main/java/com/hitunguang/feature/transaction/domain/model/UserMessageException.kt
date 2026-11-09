package com.hitunguang.feature.transaction.domain.model

import android.content.Context
import androidx.annotation.StringRes

interface UserMessage {
    val resId: Int
    val args: List<Any>
}

class UserArgumentException(
    @StringRes override val resId: Int,
    override val args: List<Any> = emptyList(),
    fallbackMessage: String
) : IllegalArgumentException(fallbackMessage), UserMessage

class UserStateException(
    @StringRes override val resId: Int,
    override val args: List<Any> = emptyList(),
    fallbackMessage: String
) : IllegalStateException(fallbackMessage), UserMessage

fun Throwable.userMessage(context: Context): String? =
    if (this is UserMessage) context.getString(resId, *args.toTypedArray()) else message
