package com.example.data.repository

import com.example.data.db.PayLinkDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class PayLinkRepository(private val dao: PayLinkDao) {

    val loggedInUser: Flow<UserEntity?> = dao.getLoggedInUser()
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allNotifications: Flow<List<NotificationEntity>> = dao.getAllNotifications()
    val supportMessages: Flow<List<SupportMessageEntity>> = dao.getAllSupportMessages()

    fun getUserByPhone(phone: String): Flow<UserEntity?> {
        return dao.getUserByPhone(phone)
    }

    suspend fun registerOrUpdateUser(user: UserEntity) {
        dao.insertUser(user)
    }

    suspend fun saveUser(user: UserEntity) {
        dao.updateUser(user)
    }

    suspend fun logoutAll() {
        dao.logoutAllUsers()
    }

    suspend fun addTransaction(transaction: TransactionEntity) {
        dao.insertTransaction(transaction)
    }

    suspend fun addNotification(notification: NotificationEntity) {
        dao.insertNotification(notification)
    }

    suspend fun markNotificationsAsRead() {
        dao.markAllNotificationsAsRead()
    }

    suspend fun addSupportMessage(msg: SupportMessageEntity) {
        dao.insertSupportMessage(msg)
    }

    suspend fun clearSupportHistory() {
        dao.clearSupportHistory()
    }
}
