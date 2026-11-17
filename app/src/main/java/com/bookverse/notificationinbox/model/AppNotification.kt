package com.bookverse.notificationinbox.model

import com.google.firebase.Timestamp

data class AppNotification(
    val id:String,
    val title:String,
    val description:String,
    val bannerImg:String?,
    val timestamp:Long,
    val isRead: Boolean
)