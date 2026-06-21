@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.LocalizedStrings
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PayScreen
import com.example.ui.viewmodel.PayViewModel
import com.example.data.model.TransactionEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.SupportMessageEntity
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: PayViewModel by viewModels {
        PayViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                val showPinAuth by viewModel.showPinAuth.collectAsStateWithLifecycle()
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (currentScreen) {
                        PayScreen.LOGIN -> LoginScreen(viewModel)
                        PayScreen.REGISTER -> RegisterScreen(viewModel)
                        else -> AppMainContainer(viewModel, currentScreen)
                    }

                    // Security PIN modal prompt
                    if (showPinAuth) {
                        PinAuthorizationDialog(viewModel)
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-VIEWS: AUTHENTICATION
// ==========================================

@Composable
fun LoginScreen(viewModel: PayViewModel) {
    val phone by viewModel.inputPhone.collectAsStateWithLifecycle()
    val pin by viewModel.inputPin.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val lang by viewModel.language.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Identity Header
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(FintechEmerald, FintechAccentGreen)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = "Wallet Logo",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = LocalizedStrings.getString("app_name", lang),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Your secure smart gateway to mobile money & banking",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Phone input text field
        OutlinedTextField(
            value = phone,
            onValueChange = { viewModel.inputPhone.value = it },
            label = { Text(LocalizedStrings.getString("phone_number", lang)) },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_phone_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // PIN passcode text field
        OutlinedTextField(
            value = pin,
            onValueChange = { viewModel.inputPin.value = it.take(4) },
            label = { Text(LocalizedStrings.getString("pin_code", lang)) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_pin_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (loginError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = loginError ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary SignIn Button
        Button(
            onClick = { viewModel.performLogin(isBiometricSim = false) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("login_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = LocalizedStrings.getString("sign_in", lang),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Simulated Biometric/Fingerprint trigger row
        Card(
            onClick = { viewModel.performLogin(isBiometricSim = true) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("biometric_login_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Fingerprint sensor",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = LocalizedStrings.getString("biometrics", lang),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Toggle language button on auth screen
        TextButton(
            onClick = { viewModel.toggleLanguage() },
            modifier = Modifier.testTag("toggle_lang_auth")
        ) {
            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (lang == "sw") "English" else "Kiswahili",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom registration link
        TextButton(
            onClick = { viewModel.navigateTo(PayScreen.REGISTER) },
            modifier = Modifier.testTag("register_link_button")
        ) {
            Text(
                text = LocalizedStrings.getString("no_account", lang),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun RegisterScreen(viewModel: PayViewModel) {
    val name by viewModel.inputRegName.collectAsStateWithLifecycle()
    val phone by viewModel.inputRegPhone.collectAsStateWithLifecycle()
    val email by viewModel.inputRegEmail.collectAsStateWithLifecycle()
    val pin by viewModel.inputRegPin.collectAsStateWithLifecycle()
    val pinConfirm by viewModel.inputRegPinConfirm.collectAsStateWithLifecycle()
    val registerError by viewModel.registerError.collectAsStateWithLifecycle()
    val lang by viewModel.language.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = LocalizedStrings.getString("sign_up", lang),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Create a PayLink digital wallet instantly",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Input forms
        OutlinedTextField(
            value = name,
            onValueChange = { viewModel.inputRegName.value = it },
            label = { Text(LocalizedStrings.getString("full_name", lang)) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_name_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { viewModel.inputRegPhone.value = it },
            label = { Text(LocalizedStrings.getString("phone_number", lang)) },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_phone_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.inputRegEmail.value = it },
            label = { Text(LocalizedStrings.getString("email_address", lang)) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_email_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { viewModel.inputRegPin.value = it.take(4) },
            label = { Text(LocalizedStrings.getString("pin_code", lang)) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_pin_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = pinConfirm,
            onValueChange = { viewModel.inputRegPinConfirm.value = it.take(4) },
            label = { Text(LocalizedStrings.getString("pin_confirm", lang)) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_pin_confirm_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (registerError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = registerError ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.performRegister() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("submit_register_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = LocalizedStrings.getString("sign_up", lang),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = { viewModel.navigateTo(PayScreen.LOGIN) },
            modifier = Modifier.testTag("login_link_button")
        ) {
            Text(
                text = LocalizedStrings.getString("have_account", lang),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ==========================================
// PIN VERIFY MODAL DIALOG
// ==========================================

@Composable
fun PinAuthorizationDialog(viewModel: PayViewModel) {
    val lang by viewModel.language.collectAsStateWithLifecycle()
    var pinText by remember { mutableStateOf("") }
    val errorMsg by viewModel.pinAuthError.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = { viewModel.cancelPinAuth() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("pin_auth_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = LocalizedStrings.getString("security_pin", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = pinText,
                    onValueChange = { pinText = it.take(4) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .testTag("pin_auth_input"),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = { viewModel.cancelPinAuth() },
                        modifier = Modifier.testTag("pin_cancel_btn")
                    ) {
                        Text(
                            text = LocalizedStrings.getString("cancel", lang),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    Button(
                        onClick = { viewModel.submitPinAuth(pinText) },
                        modifier = Modifier.testTag("pin_confirm_btn")
                    ) {
                        Text(LocalizedStrings.getString("confirm", lang))
                    }
                }
            }
        }
    }
}

// ==========================================
// CORE LAYOUT SCHEME (APP MAIN CONTAINER)
// ==========================================

@Composable
fun AppMainContainer(viewModel: PayViewModel, activeScreen: PayScreen) {
    val lang by viewModel.language.collectAsStateWithLifecycle()
    val notificationList by viewModel.notifications.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val isFraudFlagged by viewModel.isFraudFlagged.collectAsStateWithLifecycle()

    val unreadNotes = notificationList.count { !it.isRead }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LocalizedStrings.getString("app_name", lang),
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (user?.isKycVerified == true) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Gold Badge",
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Chat Support Trigger
                    IconButton(
                        onClick = { viewModel.navigateTo(PayScreen.SUPPORT) },
                        modifier = Modifier.testTag("toolbar_support_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = "Contact support"
                        )
                    }

                    // Notification Trigger
                    IconButton(
                        onClick = { viewModel.navigateTo(PayScreen.NOTIFICATIONS) },
                        modifier = Modifier.testTag("toolbar_notif_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotes > 0) {
                                    Badge { Text(unreadNotes.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "System notifications"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val screens = listOf(
                    Triple(PayScreen.DASHBOARD, Icons.Default.Home, "home"),
                    Triple(PayScreen.WALLET, Icons.Default.Wallet, "wallet"),
                    Triple(PayScreen.TRANSFER, Icons.Default.SwapHoriz, "transfer"),
                    Triple(PayScreen.PAYMENTS, Icons.Default.Payment, "payments"),
                    Triple(PayScreen.PROFILE, Icons.Default.Person, "profile")
                )

                screens.forEach { (screenType, iconVec, stringKey) ->
                    val isSelected = activeScreen == screenType
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.navigateTo(screenType) },
                        icon = { Icon(iconVec, contentDescription = null) },
                        label = {
                            Text(
                                text = LocalizedStrings.getString(stringKey, lang),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.testTag("nav_item_${stringKey}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen transition layout
            AnimatedContent(
                targetState = activeScreen,
                label = "ScreenNavigator",
                transitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                }
            ) { screen ->
                when (screen) {
                    PayScreen.DASHBOARD -> DashboardScreen(viewModel)
                    PayScreen.WALLET -> WalletScreen(viewModel)
                    PayScreen.TRANSFER -> TransferScreen(viewModel)
                    PayScreen.PAYMENTS -> PaymentsScreen(viewModel)
                    PayScreen.PROFILE -> ProfileScreen(viewModel)
                    PayScreen.NOTIFICATIONS -> NotificationsScreen(viewModel)
                    PayScreen.SUPPORT -> SupportScreen(viewModel)
                    else -> DashboardScreen(viewModel)
                }
            }

            // Realtime Security/Fraud Risk banner alert
            if (isFraudFlagged) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                        .testTag("fraud_warning_banner"),
                    colors = CardDefaults.cardColors(
                        containerColor = ErrorRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Security Action Required",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = LocalizedStrings.getString("fraud_alert", lang),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = { viewModel.dismissFraudWarning() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close warning",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. DASHBOARD SCREEN
// ==========================================

@Composable
fun DashboardScreen(viewModel: PayViewModel) {
    val lang by viewModel.language.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val transactionsList by viewModel.transactions.collectAsStateWithLifecycle()
    
    // UI details
    val finalBalanceText = if (user != null) {
        "${String.format("%,.2f", user?.balance)} TZS"
    } else {
        "0.00 TZS"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header Greeting
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Habari, ${user?.fullName ?: "User"} 👋",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "PayLink " + if (user?.isKycVerified == true) "Verified Core" else "Standard Account",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // 1. Sleek Gradient Balance Card Mesh
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_balance_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(FintechEmerald, Color(0xFF065F46))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = LocalizedStrings.getString("balance_title", lang),
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = finalBalanceText,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Wallet,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = user?.phone ?: "",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (user?.isKycVerified == true) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "KYC ACTIVE",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Dynamic circular actions menu
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MainMenuActionButton(
                    iconVec = Icons.Default.ArrowOutward,
                    label = LocalizedStrings.getString("send_money", lang),
                    onClick = { viewModel.navigateTo(PayScreen.TRANSFER) },
                    testTag = "dashboard_action_send"
                )
                MainMenuActionButton(
                    iconVec = Icons.Default.QrCode,
                    label = LocalizedStrings.getString("receive_money", lang),
                    onClick = { viewModel.navigateTo(PayScreen.WALLET) },
                    testTag = "dashboard_action_receive"
                )
                MainMenuActionButton(
                    iconVec = Icons.Default.ReceiptLong,
                    label = LocalizedStrings.getString("bill_payments", lang),
                    onClick = { viewModel.navigateTo(PayScreen.PAYMENTS) },
                    testTag = "dashboard_action_bills"
                )
                MainMenuActionButton(
                    iconVec = Icons.Default.PhonelinkRing,
                    label = "Airtime",
                    onClick = { viewModel.navigateTo(PayScreen.PAYMENTS) },
                    testTag = "dashboard_action_airtime"
                )
            }
        }

        // 3. Simple Analytics / Tonal Budget Stats
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LocalizedStrings.getString("quick_stats", lang),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(LocalizedStrings.getString("money_in", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            Text("TZS 400,000.00", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Divider(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)))
                        Column {
                            Text(LocalizedStrings.getString("money_out", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            Text("TZS 155,000.00", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // 4. Recent Transactions List
        item {
            Text(
                text = LocalizedStrings.getString("recent_transactions", lang),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (transactionsList.isEmpty()) {
            item {
                Text(
                    text = LocalizedStrings.getString("empty_transactions", lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(transactionsList) { transaction ->
                TransactionListItem(transaction)
            }
        }
    }
}

@Composable
fun MainMenuActionButton(
    iconVec: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVec,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TransactionListItem(tx: TransactionEntity) {
    val isCredit = tx.type == "RECEIVE" || tx.type == "DEPOSIT"
    val sign = if (isCredit) "+" else "-"
    val color = if (tx.status == "SUSPENDED") {
        WarningOrange
    } else if (isCredit) {
        SuccessGreen
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color.copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (tx.type) {
                        "DEPOSIT" -> Icons.Default.ArrowDownward
                        "WITHDRAW" -> Icons.Default.ArrowUpward
                        "BILL" -> Icons.Default.Receipt
                        "AIRTIME" -> Icons.Default.PhoneAndroid
                        else -> Icons.Default.SwapHoriz
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = tx.recipientName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${tx.provider} • ${tx.referenceCode}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign${String.format("%,.2f", tx.amount)} TZS",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = color
                )
                Text(
                    text = tx.status,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ==========================================
// 2. WALLET SCREEN (DEPOSITS & WITHDRAWALS)
// ==========================================

@Composable
fun WalletScreen(viewModel: PayViewModel) {
    val lang by viewModel.language.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    
    var showDepositForm by remember { mutableStateOf(false) }
    var showWithdrawForm by remember { mutableStateOf(false) }

    var inputAmount by remember { mutableStateOf("") }
    var inputDetail by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf("M-Pesa") }

    val providers = listOf("M-Pesa", "Tigo Pesa", "Airtel Money", "Halopesa", "Visa/Mastercard")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mock Cards View
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(FintechEmeraldLight, Color(0xFF0284C7))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "PayLink Digital Card",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.White)
                        }

                        Column {
                            Text(
                                "•••• •••• •••• 4242",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    user?.fullName ?: "Aisha Juma",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    "05/29",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Options Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        showDepositForm = true
                        showWithdrawForm = false
                        inputAmount = ""
                        inputDetail = ""
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("action_show_deposit"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(LocalizedStrings.getString("deposit", lang), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        showWithdrawForm = true
                        showDepositForm = false
                        inputAmount = ""
                        inputDetail = ""
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("action_show_withdraw"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Default.CallReceived, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = LocalizedStrings.getString("withdraw", lang),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Expanded Forms Container
        if (showDepositForm) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_deposit_form"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = LocalizedStrings.getString("deposit", lang),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Amount field
                        OutlinedTextField(
                            value = inputAmount,
                            onValueChange = { inputAmount = it },
                            label = { Text(LocalizedStrings.getString("amount", lang)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wallet_amount"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Provider Phone / Target Details
                        OutlinedTextField(
                            value = inputDetail,
                            onValueChange = { inputDetail = it },
                            label = { Text("Sender Phone / Account Details") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wallet_deposit_detail"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(LocalizedStrings.getString("bank_select", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            providers.take(4).forEach { p ->
                                val active = selectedProvider == p
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedProvider = p }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(p, color = if (active) Color.White else MaterialTheme.colorScheme.onBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val amtNum = inputAmount.toDoubleOrNull() ?: 0.0
                                if (amtNum > 0 && inputDetail.isNotEmpty()) {
                                    viewModel.performDeposit(amtNum, selectedProvider, inputDetail)
                                    showDepositForm = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_deposit"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Complete Wallet Upload", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showWithdrawForm) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_withdraw_form"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = LocalizedStrings.getString("withdraw", lang),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Amount field
                        OutlinedTextField(
                            value = inputAmount,
                            onValueChange = { inputAmount = it },
                            label = { Text(LocalizedStrings.getString("amount", lang)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wallet_withdraw_amount"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Target Phone/Agent ID number field
                        OutlinedTextField(
                            value = inputDetail,
                            onValueChange = { inputDetail = it },
                            label = { Text("Agent Number / Recipient Phone") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wallet_withdraw_detail"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(LocalizedStrings.getString("bank_select", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            providers.take(4).forEach { p ->
                                val active = selectedProvider == p
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedProvider = p }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(p, color = if (active) Color.White else MaterialTheme.colorScheme.onBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val amtNum = inputAmount.toDoubleOrNull() ?: 0.0
                                if (amtNum > 0 && inputDetail.isNotEmpty()) {
                                    viewModel.performWithdraw(amtNum, selectedProvider, inputDetail)
                                    showWithdrawForm = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_withdraw"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Authorize Cash Out", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Receive payment QR code generation panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = LocalizedStrings.getString("receive_money", lang),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = LocalizedStrings.getString("qr_explanation", lang),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Representing QR Code Grid visually
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Wallet QR ID",
                            tint = Color.Black,
                            modifier = Modifier.size(128.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "MID: PLK-${user?.phone ?: "0712345678"}",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. TRANSFER SCREEN (PEER TO PEER & BANKS)
// ==========================================

@Composable
fun TransferScreen(viewModel: PayViewModel) {
    val lang by viewModel.language.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    var inputAmount by remember { mutableStateOf("") }
    var inputPhoneVal by remember { mutableStateOf("") }
    var inputNameVal by remember { mutableStateOf("") }
    var inputNoteVal by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf("M-Pesa") }

    val serviceProviders = listOf(
        "M-Pesa", "Tigo Pesa", "Airtel Money", "Halopesa",
        "CRDB Bank", "NMB Bank", "Standard Chartered", "PayLink Wallet"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = LocalizedStrings.getString("send_money", lang) + " Multi-Gateway",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Selection Gateway Scroll Row
                    Text("Gateway Provider Category", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Simple wrap rows for responsive selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        serviceProviders.take(4).forEach { p ->
                            val active = selectedService == p
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedService = p }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(p, color = if (active) Color.White else MaterialTheme.colorScheme.onBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        serviceProviders.drop(4).take(4).forEach { p ->
                            val active = selectedService == p
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedService = p }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(p, color = if (active) Color.White else MaterialTheme.colorScheme.onBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Forms
                    OutlinedTextField(
                        value = inputPhoneVal,
                        onValueChange = { inputPhoneVal = it },
                        label = { Text(LocalizedStrings.getString("account_number", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_phone"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputNameVal,
                        onValueChange = { inputNameVal = it },
                        label = { Text("Recipient Native Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_name"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = { inputAmount = it },
                        label = { Text(LocalizedStrings.getString("amount", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_amount"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Quick input amount chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("10000", "50000", "150000", "500000").forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                    .clickable { inputAmount = preset }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$preset TZS", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputNoteVal,
                        onValueChange = { inputNoteVal = it },
                        label = { Text(LocalizedStrings.getString("notes", lang)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_notes"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val amtNum = inputAmount.toDoubleOrNull() ?: 0.0
                            if (amtNum > 0 && inputPhoneVal.isNotEmpty()) {
                                viewModel.performTransfer(
                                    amount = amtNum,
                                    provider = selectedService,
                                    recipientPhone = inputPhoneVal,
                                    notes = inputNoteVal,
                                    name = inputNameVal
                                )
                                // clear
                                inputAmount = ""
                                inputPhoneVal = ""
                                inputNameVal = ""
                                inputNoteVal = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_transfer"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(LocalizedStrings.getString("confirm", lang), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. PAYMENTS SCREEN (UTILITY BILLS)
// ==========================================

@Composable
fun PaymentsScreen(viewModel: PayViewModel) {
    val lang by viewModel.language.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    var selectedIndex by remember { mutableStateOf(0) } // 0: Bills, 1: Airtime

    var inputAmount by remember { mutableStateOf("") }
    var inputBillerAccount by remember { mutableStateOf("") }
    var selectedBillerName by remember { mutableStateOf("LUKU Electricity") }
    var selectedAirtimeProvider by remember { mutableStateOf("Vodacom") }

    val billers = listOf("LUKU Electricity", "Dawasco Water", "AzamTV Subscription", "DSTV Compact", "Tanzania Taxes TRA")
    val networkOperators = listOf("Vodacom", "Tigo", "Airtel", "Halotel")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Tab Header Choice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selectedIndex == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedIndex = 0 }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = LocalizedStrings.getString("bill_payments", lang),
                        color = if (selectedIndex == 0) Color.White else MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selectedIndex == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedIndex = 1 }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = LocalizedStrings.getString("buy_airtime", lang),
                        color = if (selectedIndex == 1) Color.White else MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (selectedIndex == 0) {
            // BILL PAYMENTS FORM
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Select PayLink Biller Agency",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Select biller grid
                        billers.forEach { biller ->
                            val active = selectedBillerName == biller
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedBillerName = biller }
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(biller, fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                                if (active) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = inputBillerAccount,
                            onValueChange = { inputBillerAccount = it },
                            label = { Text("Account Meter / Reference Code") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_biller_acc"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = inputAmount,
                            onValueChange = { inputAmount = it },
                            label = { Text(LocalizedStrings.getString("amount", lang)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_biller_amount"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val amtNum = inputAmount.toDoubleOrNull() ?: 0.0
                                if (amtNum > 0 && inputBillerAccount.isNotEmpty()) {
                                    viewModel.performBillPayment(amtNum, selectedBillerName, inputBillerAccount)
                                    inputAmount = ""
                                    inputBillerAccount = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_bill_payment_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Settle Biller Invoice", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // AIRTIME PURCHASES FORM
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Select Network Operator",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            networkOperators.forEach { op ->
                                val active = selectedAirtimeProvider == op
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedAirtimeProvider = op }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(op, color = if (active) Color.White else MaterialTheme.colorScheme.onBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = inputBillerAccount,
                            onValueChange = { inputBillerAccount = it },
                            label = { Text("Recipient Phone Key") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_airtime_phone"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = inputAmount,
                            onValueChange = { inputAmount = it },
                            label = { Text(LocalizedStrings.getString("amount", lang)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_airtime_amount"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val amtNum = inputAmount.toDoubleOrNull() ?: 0.0
                                if (amtNum > 0 && inputBillerAccount.isNotEmpty()) {
                                    viewModel.performBuyAirtime(amtNum, selectedAirtimeProvider, inputBillerAccount)
                                    inputAmount = ""
                                    inputBillerAccount = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_airtime_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Complete Instant Airtime Recharge", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. PROFILE SCREEN (KYC, STATS & PREFS)
// ==========================================

@Composable
fun ProfileScreen(viewModel: PayViewModel) {
    val lang by viewModel.language.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    var docType by remember { mutableStateOf("National ID") }
    var docNoVal by remember { mutableStateOf("") }
    
    val documents = listOf("National ID", "Driver's License", "Passport Voter ID")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User primary ID stats
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.fullName?.take(2)?.uppercase() ?: "US",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(user?.fullName ?: "Fintech Customer", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(user?.email ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        Text(user?.phone ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // KYC Status Verification Panel
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kyc_verification_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (user?.isKycVerified == true) SuccessGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LocalizedStrings.getString("kyc_status", lang),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val badgeColor = if (user?.isKycVerified == true) SuccessGreen else ErrorRed
                        val badgeText = if (user?.isKycVerified == true) {
                            LocalizedStrings.getString("kyc_verified", lang)
                        } else {
                            LocalizedStrings.getString("kyc_unverified", lang)
                        }

                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (user?.isKycVerified == true) {
                        Text(
                            text = LocalizedStrings.getString("under_review", lang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Verified Document: ${user?.kycDocumentType} (${user?.kycIdNumber})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    } else {
                        Text("Verify your identity with standard documents to transition wallet limitations in compliant level.", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Selection Document Row
                        Text("ID type document selection", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            documents.forEach { doc ->
                                val active = docType == doc
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { docType = doc }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(doc, color = if (active) Color.White else MaterialTheme.colorScheme.onBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = docNoVal,
                            onValueChange = { docNoVal = it },
                            label = { Text("ID Document Registration String") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kyc_id_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (docNoVal.isNotEmpty()) {
                                    viewModel.submitKyc(docType, docNoVal)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kyc_verify_submit"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(LocalizedStrings.getString("submit_kyc", lang), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Theme and Security preferences
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Preferences & Security Settings",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Biometrics toggle switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(LocalizedStrings.getString("biometrics", lang), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Use fingerprint scanner for fast sign-on", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = user?.biometricEnabled == true,
                            onCheckedChange = { viewModel.setBiometricsEnabled(it) },
                            modifier = Modifier.testTag("toggle_bio_switch")
                        )
                    }

                    Divider()

                    // Language Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(LocalizedStrings.getString("language_select", lang), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Active: ${if (lang == "sw") "Kiswahili / Swahili" else "English"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }
                        Button(
                            onClick = { viewModel.toggleLanguage() },
                            modifier = Modifier.testTag("toggle_lang_profile_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (lang == "sw") "English" else "Kiswahili")
                        }
                    }
                }
            }
        }

        // Support Agent Credits and App exit trigger
        item {
            Button(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("logout_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Secure Log Out", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 6. NOTIFICATIONS SCREEN
// ==========================================

@Composable
fun NotificationsScreen(viewModel: PayViewModel) {
    val lang by viewModel.language.collectAsStateWithLifecycle()
    val list by viewModel.notifications.collectAsStateWithLifecycle()

    // Mark as Read on render/view transition
    LaunchedEffect(Unit) {
        viewModel.markNotificationsAsRead()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = LocalizedStrings.getString("notifications", lang),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = { viewModel.navigateTo(PayScreen.DASHBOARD) },
                modifier = Modifier.testTag("notif_back_home")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (list.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "You have no historic notification alerts.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list) { notification ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notification.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = notification.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (!notification.isRead) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(SuccessGreen, CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = notification.body,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. SUPPORT CHAT SCREEN (AI VIRTUAL DESK)
// ==========================================

@Composable
fun SupportScreen(viewModel: PayViewModel) {
    val lang by viewModel.language.collectAsStateWithLifecycle()
    val chatFlow by viewModel.supportMessages.collectAsStateWithLifecycle()
    val loading by viewModel.supportLoading.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }

    val suggestionChips = listOf(
        "Is PayLink secure?",
        "Jinsi ya kuweka pesa?",
        "Transaction fees rates",
        "How to verify KYC?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.ime)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PayLink Live AI Desk",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Connected to Intelligent Virtual Assistant",
                    fontSize = 11.sp,
                    color = SuccessGreen
                )
            }
            Row {
                IconButton(
                    onClick = { viewModel.clearSupportMessages() },
                    modifier = Modifier.testTag("support_sweep_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Chat history", tint = ErrorRed)
                }
                IconButton(
                    onClick = { viewModel.navigateTo(PayScreen.DASHBOARD) },
                    modifier = Modifier.testTag("support_back_home")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chat Bubble list stream
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatFlow) { msg ->
                ChatBubble(msg)
            }
            if (loading) {
                item {
                    Text(
                        text = "PayLink Agent is typing replies...",
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }

        // Quick query suggestions row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            suggestionChips.forEach { chip ->
                Card(
                    modifier = Modifier
                        .clickable { viewModel.sendSupportMessage(chip) },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        chip,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Text input dock
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text(LocalizedStrings.getString("support_hint", lang)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("support_chat_input"),
                singleLine = true,
                shape = RoundedCornerShape(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable {
                        if (textInput.trim().isNotEmpty()) {
                            viewModel.sendSupportMessage(textInput)
                            textInput = ""
                        }
                    }
                    .testTag("support_send_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send msg", tint = Color.White)
            }
        }
    }
}

@Composable
fun ChatBubble(msg: SupportMessageEntity) {
    val align = if (msg.isUser) Alignment.End else Alignment.Start
    val containerColor = if (msg.isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (msg.isUser) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Box(
            modifier = Modifier
                .background(
                    containerColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (msg.isUser) 16.dp else 0.dp,
                        bottomEnd = if (msg.isUser) 0.dp else 16.dp
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = msg.text,
                color = contentColor,
                fontSize = 13.sp
            )
        }
        Text(
            text = if (msg.isUser) "You" else "PayLink Assistant",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
