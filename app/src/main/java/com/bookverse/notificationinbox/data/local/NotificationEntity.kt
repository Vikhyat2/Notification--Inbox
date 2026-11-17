package com.bookverse.notificationinbox.data.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.bookverse.notificationinbox.model.AppNotification

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id:String,
    val title:String,
    val description:String,
    @ColumnInfo(name = "Banner_img")
    val bannerImg:String?,
    val timestamp:Long,
    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false

)

fun NotificationEntity.toModel() = AppNotification(
    id = id,
    title = title,
    description = description,
    bannerImg = bannerImg,
    timestamp = timestamp,
    isRead = isRead
)