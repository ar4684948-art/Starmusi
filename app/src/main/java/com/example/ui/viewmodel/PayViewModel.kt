package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.PayLinkDatabase
import com.example.data.model.*
import com.example.data.repository.PayLinkRepository
import com.example.data.service.GeminiService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class PayScreen {
    LOGIN,
    REGISTER,
    DASHBOARD,
    WALLET,
    TRANSFER,
    PAYMENTS,
    PROFILE,
    NOTIFICATIONS,
    SUPPORT
}

class PayViewModel(
    application: Application,
    private val repository: PayLinkRepository
) : AndroidViewModel(application) {

    private val TAG = "PayViewModel"

    // Lang State: defaults to English ("en") or Swahili ("sw")
    private val _language = MutableStateFlow("en")
    val language: StateFlow<String> = _language.asStateFlow()

    // Screen navigation
    private val _currentScreen = MutableStateFlow(PayScreen.LOGIN)
    val currentScreen: StateFlow<PayScreen> = _currentScreen.asStateFlow()

    // Database backed state flows
    val currentUser: StateFlow<UserEntity?> = repository.loggedInUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supportMessages: StateFlow<List<SupportMessageEntity>> = repository.supportMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Local UI states
    private val _registerError = MutableStateFlow<String?>(null)
    val registerError: StateFlow<String?> = _registerError.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isFraudFlagged = MutableStateFlow(false)
    val isFraudFlagged: StateFlow<Boolean> = _isFraudFlagged.asStateFlow()

    private val _supportLoading = MutableStateFlow(false)
    val supportLoading: StateFlow<Boolean> = _supportLoading.asStateFlow()

    // Bottom sheets / PIN authorization triggers
    private val _showPinAuth = MutableStateFlow(false)
    val showPinAuth: StateFlow<Boolean> = _showPinAuth.asStateFlow()

    private var pinSuccessAction: (() -> Unit)? = null
    // Callback variables for forms
    val pinAuthError = MutableStateFlow<String?>(null)

    // Live state tracking inputs
    val inputPhone = MutableStateFlow("0712345678") // seeded for easy testing
    val inputPin = MutableStateFlow("1234") // seeded PIN
    val inputRegName = MutableStateFlow("")
    val inputRegPhone = MutableStateFlow("")
    val inputRegEmail = MutableStateFlow("")
    val inputRegPin = MutableStateFlow("")
    val inputRegPinConfirm = MutableStateFlow("")

    init {
        seedInitialDataIfNeeded()
    }

    private fun seedInitialDataIfNeeded() {
        viewModelScope.launch {
            repository.getUserByPhone("0712345678").first()?.let {
                // User already exists, set default language from user profile
                _language.value = it.language
                if (it.isLoggedIn) {
                    _currentScreen.value = PayScreen.DASHBOARD
                }
                return@launch
            }

            Log.d(TAG, "Database is empty. Seeding realistic fintech data...")
            // Create default user Aisha Juma
            val defaultUser = UserEntity(
                phone = "0712345678",
                fullName = "Aisha Juma",
                email = "aisha.juma@paylink.com",
                balance = 350000.0,
                pin = "1234",
                isKycVerified = true,
                kycDocumentType = "National ID",
                kycIdNumber = "NIDA-29283-PLK",
                language = "en",
                biometricEnabled = true,
                isLoggedIn = false // start at login screen so they see face/biometric unlock simulation
            )
            repository.registerOrUpdateUser(defaultUser)

            // Seed Notifications
            repository.addNotification(
                NotificationEntity(
                    title = "Welcome first-time user",
                    body = "Welcome to PayLink! Secure your fintech wallet by enabling Biometric Login and verifying your KYC details in your Profile section."
                )
            )
            repository.addNotification(
                NotificationEntity(
                    title = "KYC Verified Successfully",
                    body = "Your identity document (National ID) has been verified. Daily wallet transaction limit limits have been upgraded to 5,000,000 TZS."
                )
            )

            // Seed Transactions
            repository.addTransaction(
                TransactionEntity(
                    type = "DEPOSIT",
                    amount = 350000.0,
                    recipientName = "Aisha Juma",
                    recipientDetail = "Visa Debit Card (*4242)",
                    provider = "Visa/Mastercard",
                    referenceCode = "TXN-739281-PLK",
                    notes = "Standard card deposit"
                )
            )
            repository.addTransaction(
                TransactionEntity(
                    type = "BILL",
                    amount = 35000.0,
                    recipientName = "LUKU Electricity",
                    recipientDetail = "Meter: 2938472910",
                    provider = "Tanesco LUKU",
                    referenceCode = "TXN-293847-PLK",
                    notes = "Emergency tokens"
                )
            )
            repository.addTransaction(
                TransactionEntity(
                    type = "RECEIVE",
                    amount = 50000.0,
                    recipientName = "Baraka Joseph",
                    recipientDetail = "0784112233",
                    provider = "M-Pesa",
                    referenceCode = "TXN-102947-PLK",
                    notes = "P2P refund"
                )
            )
            repository.addTransaction(
                TransactionEntity(
                    type = "SEND",
                    amount = 120000.0,
                    recipientName = "John Doe",
                    recipientDetail = "ACC-9283749",
                    provider = "CRDB Bank",
                    referenceCode = "TXN-839201-PLK",
                    notes = "Hostel fee booking"
                )
            )

            // Seed support agent initial greet
            repository.addSupportMessage(
                SupportMessageEntity(
                    text = "Habari! Welcome to PayLink Live Support. Ask me anything about mobile money transfers, transaction fees, and account security. I can assist in English and Swahili!",
                    isUser = false
                )
            )
        }
    }

    // Language Toggle
    fun toggleLanguage() {
        viewModelScope.launch {
            val nextLang = if (_language.value == "en") "sw" else "en"
            _language.value = nextLang
            currentUser.value?.let {
                repository.saveUser(it.copy(language = nextLang))
            }
        }
    }

    // Screen navigation
    fun navigateTo(screen: PayScreen) {
        _currentScreen.value = screen
    }

    // Secure Register Account
    fun performRegister() {
        val name = inputRegName.value.trim()
        val phone = inputRegPhone.value.trim()
        val email = inputRegEmail.value.trim()
        val pin = inputRegPin.value
        val pinConfirm = inputRegPinConfirm.value

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || pin.isEmpty()) {
            _registerError.value = "All fields are required"
            return
        }
        if (pin.length != 4) {
            _registerError.value = "PIN must be exactly 4 digits"
            return
        }
        if (pin != pinConfirm) {
            _registerError.value = "PINs do note match"
            return
        }

        viewModelScope.launch {
            // Check if user already exists
            val existing = repository.getUserByPhone(phone).first()
            if (existing != null) {
                _registerError.value = "User with this phone number already exists"
                return@launch
            }

            val newUser = UserEntity(
                phone = phone,
                fullName = name,
                email = email,
                balance = 10000.0, // registration token bonus!
                pin = pin,
                isKycVerified = false,
                language = _language.value,
                biometricEnabled = false,
                isLoggedIn = true
            )
            repository.registerOrUpdateUser(newUser)
            _registerError.value = null
            
            // Seed Register Notification
            repository.addNotification(
                NotificationEntity(
                    title = "Welcome to PayLink!",
                    body = "We have credited your wallet TZS 10,000 registration welcome bonus. Finish KYC to unlock higher deposits and bank transfers."
                )
            )
            navigateTo(PayScreen.DASHBOARD)
            // Reset input fields
            inputRegName.value = ""
            inputRegPhone.value = ""
            inputRegEmail.value = ""
            inputRegPin.value = ""
            inputRegPinConfirm.value = ""
        }
    }

    // Login Auth
    fun performLogin(isBiometricSim: Boolean = false) {
        _loginError.value = null
        val phone = inputPhone.value.trim()
        val pin = inputPin.value.trim()

        viewModelScope.launch {
            val user = repository.getUserByPhone(phone).first()
            if (user == null) {
                _loginError.value = "User not registered"
                return@launch
            }

            if (isBiometricSim) {
                if (!user.biometricEnabled) {
                    _loginError.value = "Biometric login is not enabled for this user"
                    return@launch
                }
                // Simulated Biometric success
                val loggedUser = user.copy(isLoggedIn = true)
                repository.saveUser(loggedUser)
                _language.value = loggedUser.language
                navigateTo(PayScreen.DASHBOARD)
                repository.addNotification(
                    NotificationEntity(
                        title = "Secure Biometric Login",
                        body = "You have logged into your dashboard securely using Fingerprint recognition."
                    )
                )
            } else {
                if (user.pin != pin) {
                    _loginError.value = "Invalid phone or PIN"
                    return@launch
                }
                val loggedUser = user.copy(isLoggedIn = true)
                repository.saveUser(loggedUser)
                _language.value = loggedUser.language
                navigateTo(PayScreen.DASHBOARD)
            }
        }
    }

    // Clear session
    fun logout() {
        viewModelScope.launch {
            repository.logoutAll()
            navigateTo(PayScreen.LOGIN)
            inputPin.value = "" // clear security PIN
        }
    }

    // Toggle Biometric Lock
    fun setBiometricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            currentUser.value?.let {
                val updated = it.copy(biometricEnabled = enabled)
                repository.saveUser(updated)
            }
        }
    }

    // Mark all notifications read
    fun markNotificationsAsRead() {
        viewModelScope.launch {
            repository.markNotificationsAsRead()
        }
    }

    // Verify KYC Model
    fun submitKyc(docType: String, docNo: String) {
        if (docNo.trim().isEmpty()) return
        viewModelScope.launch {
            currentUser.value?.let {
                val updated = it.copy(
                    isKycVerified = true,
                    kycDocumentType = docType,
                    kycIdNumber = docNo
                )
                repository.saveUser(updated)
                repository.addNotification(
                    NotificationEntity(
                        title = "KYC Verification Successful",
                        body = "Your account has been upgraded to Verified level. Daily transactional constraints are lifted."
                    )
                )
            }
        }
    }

    // Quick authorization utility
    fun requirePinAuth(action: () -> Unit) {
        pinSuccessAction = action
        pinAuthError.value = null
        _showPinAuth.value = true
    }

    fun submitPinAuth(enteredPin: String) {
        val activeUser = currentUser.value ?: return
        if (enteredPin == activeUser.pin) {
            _showPinAuth.value = false
            pinAuthError.value = null
            pinSuccessAction?.invoke()
            pinSuccessAction = null
        } else {
            pinAuthError.value = "Incorrect transaction PIN code"
        }
    }

    fun cancelPinAuth() {
        _showPinAuth.value = false
        pinSuccessAction = null
    }

    // Advanced Fraud Detection Scanner (Internal Rule Engine)
    private fun assessFraudRisk(amount: Double, detail: String): Boolean {
        // High random amounts, repetitive transfers, or specific flag lists
        return (amount >= 500000.0) || detail.contains("419") || detail.contains("911")
    }

    // Transactions API wrappers
    fun performDeposit(amount: Double, provider: String, details: String) {
        val user = currentUser.value ?: return
        requirePinAuth {
            viewModelScope.launch {
                val updatedBalance = user.balance + amount
                val ref = "PAY-DEP-${Random.nextInt(100000, 999999)}"
                
                repository.saveUser(user.copy(balance = updatedBalance))
                repository.addTransaction(
                    TransactionEntity(
                        type = "DEPOSIT",
                        amount = amount,
                        recipientName = "Wallet Upload",
                        recipientDetail = details,
                        provider = provider,
                        referenceCode = ref,
                        notes = "Loaded funds securely"
                    )
                )
                repository.addNotification(
                    NotificationEntity(
                        title = "Wallet Deposit Received",
                        body = "You successfully loaded ${String.format("%,.2f", amount)} TZS to your digital wallet using $provider ($details)."
                    )
                )
                _currentScreen.value = PayScreen.DASHBOARD
            }
        }
    }

    fun performWithdraw(amount: Double, provider: String, targetNum: String) {
        val user = currentUser.value ?: return
        if (user.balance < amount) {
            return
        }

        requirePinAuth {
            viewModelScope.launch {
                val updatedBalance = user.balance - amount
                val ref = "PAY-WTH-${Random.nextInt(100000, 999999)}"
                
                repository.saveUser(user.copy(balance = updatedBalance))
                repository.addTransaction(
                    TransactionEntity(
                        type = "WITHDRAW",
                        amount = amount,
                        recipientName = "Cash Withdrawal",
                        recipientDetail = targetNum,
                        provider = provider,
                        referenceCode = ref,
                        notes = "Cash out request completed"
                    )
                )
                repository.addNotification(
                    NotificationEntity(
                        title = "Cash Out Transaction",
                        body = "Withdrawn ${String.format("%,.2f", amount)} TZS from wallet to mobile agent $targetNum ($provider)."
                    )
                )
                _currentScreen.value = PayScreen.DASHBOARD
            }
        }
    }

    fun performTransfer(amount: Double, provider: String, recipientPhone: String, notes: String, name: String) {
        val user = currentUser.value ?: return
        if (user.balance < amount) {
            return
        }

        // Run security/fraud validation check
        val fraudDetected = assessFraudRisk(amount, recipientPhone)
        _isFraudFlagged.value = fraudDetected

        requirePinAuth {
            viewModelScope.launch {
                val updatedBalance = user.balance - amount
                val ref = "PAY-TXN-${Random.nextInt(100000, 999999)}"
                
                val finalStatus = if (fraudDetected) "SUSPENDED" else "COMPLETED"

                if (!fraudDetected) {
                    repository.saveUser(user.copy(balance = updatedBalance))
                }

                repository.addTransaction(
                    TransactionEntity(
                        type = "SEND",
                        amount = amount,
                        recipientName = name.ifEmpty { "Recipient Name" },
                        recipientDetail = recipientPhone,
                        provider = provider,
                        referenceCode = ref,
                        status = finalStatus,
                        notes = notes,
                        isFlagged = fraudDetected
                    )
                )

                if (fraudDetected) {
                    repository.addNotification(
                        NotificationEntity(
                            title = "Security Compliance Suspense",
                            body = "Your transfer of ${String.format("%,.2f", amount)} TZS to $recipientPhone was blocked by our real-time PayLink AML-Fraud detection scanner."
                        )
                    )
                } else {
                    repository.addNotification(
                        NotificationEntity(
                            title = "Money Sent Successfully",
                            body = "Sent ${String.format("%,.2f", amount)} TZS from wallet to $name ($recipientPhone) via $provider."
                        )
                    )
                }

                _currentScreen.value = PayScreen.DASHBOARD
            }
        }
    }

    fun performBillPayment(amount: Double, utilityName: String, accountId: String) {
        val user = currentUser.value ?: return
        if (user.balance < amount) {
            return
        }

        requirePinAuth {
            viewModelScope.launch {
                val updatedBalance = user.balance - amount
                val ref = "PAY-BIL-${Random.nextInt(100000, 999999)}"

                repository.saveUser(user.copy(balance = updatedBalance))
                repository.addTransaction(
                    TransactionEntity(
                        type = "BILL",
                        amount = amount,
                        recipientName = utilityName,
                        recipientDetail = accountId,
                        provider = "Bill Payment",
                        referenceCode = ref,
                        notes = "Bill Settlement"
                    )
                )
                repository.addNotification(
                    NotificationEntity(
                        title = "Utility Bill Cleared",
                        body = "Successfully settled bill of ${String.format("%,.2f", amount)} TZS for account $accountId ($utilityName)."
                    )
                )
                _currentScreen.value = PayScreen.DASHBOARD
            }
        }
    }

    fun performBuyAirtime(amount: Double, provider: String, targetPhone: String) {
        val user = currentUser.value ?: return
        if (user.balance < amount) {
            return
        }

        requirePinAuth {
            viewModelScope.launch {
                val updatedBalance = user.balance - amount
                val ref = "PAY-AIR-${Random.nextInt(100000, 999999)}"

                repository.saveUser(user.copy(balance = updatedBalance))
                repository.addTransaction(
                    TransactionEntity(
                        type = "AIRTIME",
                        amount = amount,
                        recipientName = "Airtime Purchase",
                        recipientDetail = targetPhone,
                        provider = provider,
                        referenceCode = ref,
                        notes = "Airtime pin top-up"
                    )
                )
                repository.addNotification(
                    NotificationEntity(
                        title = "Airtime Multi-load",
                        body = "Loaded ${String.format("%,.2f", amount)} TZS airtime successfully onto $targetPhone ($provider)."
                    )
                )
                _currentScreen.value = PayScreen.DASHBOARD
            }
        }
    }

    // Support Chat AI Responder
    fun sendSupportMessage(text: String) {
        if (text.trim().isEmpty()) return
        val activeLang = _language.value == "sw"

        viewModelScope.launch {
            // Save user message
            repository.addSupportMessage(
                SupportMessageEntity(text = text, isUser = true)
            )
            _supportLoading.value = true

            // Get generative response
            val reply = GeminiService.generateSupportResponse(text, activeLang)

            _supportLoading.value = false
            repository.addSupportMessage(
                SupportMessageEntity(text = reply, isUser = false)
            )
        }
    }

    fun clearSupportMessages() {
        viewModelScope.launch {
            repository.clearSupportHistory()
            repository.addSupportMessage(
                SupportMessageEntity(
                    text = if (_language.value == "sw") {
                        "Habari! Karibu kwenye huduma ya usaidizi wa PayLink. Unaweza kuniuliza kuhusu jinsi ya kutuma pesa, makato ya miamala, au kuongeza usalama wa mkoba wako. Karibu kujadili!"
                    } else {
                        "Habari! Welcome to PayLink Live Support. Ask me anything about mobile money transfers, transaction fees, and account security. I can assist in English and Swahili!"
                    },
                    isUser = false
                )
            )
        }
    }

    // Dismiss fraud warn flag
    fun dismissFraudWarning() {
        _isFraudFlagged.value = false
    }

    // Helper Factory
    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = PayLinkDatabase.getDatabase(app)
            val repo = PayLinkRepository(db.payLinkDao())
            return PayViewModel(app, repo) as T
        }
    }
}
