package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val phone: String, // Phone serves as unique key/login identifier
    val fullName: String,
    val email: String,
    val balance: Double,
    val pin: String, // Secured transaction PIN
    val isKycVerified: Boolean = false,
    val kycDocumentType: String = "",
    val kycIdNumber: String = "",
    val language: String = "en", // "en" or "sw"
    val biometricEnabled: Boolean = false,
    val isLoggedIn: Boolean = false
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // SEND, RECEIVE, DEPOSIT, WITHDRAW, BILL, AIRTIME
    val amount: Double,
    val recipientName: String,
    val recipientDetail: String, // Phone, account, or reference number
    val provider: String, // M-Pesa, Airtel Money, Tigo Pesa, Halopesa, CRDB, NMB, KCB, PayLink Wallet
    val timestamp: Long = System.currentTimeMillis(),
    val referenceCode: String,
    val status: String = "COMPLETED", // COMPLETED, FAILED, SUSPENDED
    val notes: String = "",
    val isFlagged: Boolean = false // Fraud-detection filter
)

@Entity(tableName = "support_messages")
data class SupportMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
