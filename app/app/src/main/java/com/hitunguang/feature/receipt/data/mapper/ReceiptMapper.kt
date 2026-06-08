package com.hitunguang.feature.receipt.data.mapper

import com.hitunguang.core.database.entity.ReceiptEntity
import com.hitunguang.core.database.entity.ReceiptItemEntity
import com.hitunguang.feature.receipt.domain.model.Receipt
import com.hitunguang.feature.receipt.domain.model.ReceiptItem

object ReceiptMapper {

    fun toDomain(entity: ReceiptEntity): Receipt {
        return Receipt(
            id = entity.id,
            imagePath = entity.imagePath,
            merchantName = entity.merchantName,
            receiptDate = entity.receiptDate,
            subtotal = entity.subtotal,
            tax = entity.tax,
            total = entity.total,
            ocrRawText = entity.ocrRawText,
            createdAt = entity.createdAt
        )
    }

    fun toDomainList(entities: List<ReceiptEntity>): List<Receipt> {
        return entities.map { toDomain(it) }
    }

    fun toEntity(domain: Receipt): ReceiptEntity {
        return ReceiptEntity(
            id = domain.id,
            imagePath = domain.imagePath,
            merchantName = domain.merchantName,
            receiptDate = domain.receiptDate,
            subtotal = domain.subtotal,
            tax = domain.tax,
            total = domain.total,
            ocrRawText = domain.ocrRawText,
            createdAt = domain.createdAt
        )
    }

    fun toDomainItem(entity: ReceiptItemEntity): ReceiptItem {
        return ReceiptItem(
            id = entity.id,
            receiptId = entity.receiptId,
            itemName = entity.itemName,
            quantity = entity.quantity,
            unitPrice = entity.unitPrice,
            subtotal = entity.subtotal,
            createdAt = entity.createdAt
        )
    }

    fun toDomainItemList(entities: List<ReceiptItemEntity>): List<ReceiptItem> {
        return entities.map { toDomainItem(it) }
    }

    fun toEntityItem(domain: ReceiptItem): ReceiptItemEntity {
        return ReceiptItemEntity(
            id = domain.id,
            receiptId = domain.receiptId,
            itemName = domain.itemName,
            quantity = domain.quantity,
            unitPrice = domain.unitPrice,
            subtotal = domain.subtotal,
            createdAt = domain.createdAt
        )
    }

    fun toEntityItemList(domains: List<ReceiptItem>): List<ReceiptItemEntity> {
        return domains.map { toEntityItem(it) }
    }
}
