package com.example.data.service

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun generateSupportResponse(userPrompt: String, isSwahili: Boolean): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        // System prompt instructing the model to behave as PayLink Fintech AI Agent
        val systemContext = if (isSwahili) {
            "Wewe ni PayLink Fintech AI Msaidizi wa Huduma kwa Wateja. Kazi yako ni kuwasaidia wateja juu ya akaunti zao za kifedha, kutoa maelezo kwa Kiswahili fasaha. Baki katika mfumo wa fintech pekee. Majibu yako yawe mafupi, ya kueleweka, na ya kirafiki sana."
        } else {
            "You are the PayLink Fintech AI Customer Support Assistant. Your job is to assist users about mobile money, banking transfers, transaction issues, and wallet security with helpful, concise, and professional answers. Keep replies under 3 sentences and stay strictly within fintech boundaries."
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("placeholder", ignoreCase = true)) {
            Log.d(TAG, "Using smart local fallback agent.")
            return@withContext getLocalSmartResponse(userPrompt, isSwahili)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            
            // Construct request JSON
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val systemPartObj = JSONObject().apply {
                                put("text", "$systemContext\n\nUser Question: $userPrompt")
                            }
                            put(systemPartObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API error: ${response.code} ${response.message}")
                    return@withContext getLocalSmartResponse(userPrompt, isSwahili)
                }

                val responseBodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "").trim()
                        }
                    }
                }
                return@withContext getLocalSmartResponse(userPrompt, isSwahili)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed with exception: ${e.message}", e)
            return@withContext getLocalSmartResponse(userPrompt, isSwahili)
        }
    }

    private fun getLocalSmartResponse(prompt: String, isSwahili: Boolean): String {
        val clean = prompt.lowercase()
        if (isSwahili) {
            return when {
                clean.contains("bili") || clean.contains("lipa") || clean.contains("luku") || clean.contains("maji") -> {
                    "Kukulipia bili, nenda kwenye sehemu ya 'Malipo' (Payments), chagua huduma husika (kama LUKU, Dawasco, au TV), weka namba ya kumbukumbu na kiasi, kisha weka PIN yako ili kuthibitisha."
                }
                clean.contains("weka") || clean.contains("deposit") || clean.contains("ongeza") -> {
                    "Ili kuweka pesa kwenye wallet yako ya PayLink, nenda kwenye sehemu ya 'Wallet', bonyeza 'Weka Pesa' (Deposit), chagua mtandao wako wa simu (M-Pesa, Airtel, Tigo) na kiasi unachotaka kuhamisha."
                }
                clean.contains("toa") || clean.contains("withdraw") -> {
                    "Kutoa pesa, nenda kwenye sehemu ya 'Wallet', chagua 'Toa Pesa' (Withdraw), taja wakala au namba ya simu / akaunti ya benki ya kutoa, pamoja na kiasi."
                }
                clean.contains("makato") || clean.contains("gharama") || clean.contains("fee") || clean.contains("asili") -> {
                    "Miamala yote ya P2P (kutuma ndani ya PayLink) ni bure kabisa! Gharama za kutuma kwenda mitandao ya simu au kutoa benki ni asilimia 0.5 hadi 1.2 tu ya kiasi cha muamala."
                }
                clean.contains("usalama") || clean.contains("siri") || clean.contains("pin") || clean.contains("linda") -> {
                    "PayLink inalinda wallet yako kwa njia ya Advanced Encryption, Biometric Fingerprint lock, na transaction PIN ya siri. Hakikisha huonyeshi PIN yako kwa mtu yeyote."
                }
                clean.contains("kyc") || clean.contains("kitambulisho") || clean.contains("nida") || clean.contains("thibitisha") -> {
                    "Unaweza kuthibitisha akaunti yako (KYC Verify) kwa kwenda sehemu ya 'Profile'. Chagua Kitambulisho (NIDA, Utambulisho wa Mpiga kura, au Pasipoti), weka namba yake na uthabitishe papo hapo."
                }
                else -> {
                    "Asante kwa wasifu wako! Unaongea na msaidizi wa PayLink. Unaweza kuandika maswali kuhusu: 'Weka pesa', 'Lipia bili', 'Makato ya miamala', au 'Ulinzi' nanyi nitakusaidia haraka."
                }
            }
        } else {
            return when {
                clean.contains("bill") || clean.contains("pay") || clean.contains("electricity") || clean.contains("luku") || clean.contains("water") -> {
                    "To pay utilities, tap on 'Payments' tab in the navigation bar, choose your biller (like LUKU Electricity, Water utilities, or Cable TV), enter accounts ID & amount, and tap confirm using your security PIN."
                }
                clean.contains("deposit") || clean.contains("add money") || clean.contains("fund") -> {
                    "To add money, navigate to the 'Wallet' tab, choose 'Deposit', select your desired source (mobile money M-Pesa/Tigo/Airtel or Bank cards) and input transaction amount to transfer instantly."
                }
                clean.contains("withdraw") || clean.contains("cash out") -> {
                    "To cash out from your electronic wallet, open the 'Wallet' dashboard, click 'Withdraw', input the standard agent details or mobile phone number, and declare the transfer sum."
                }
                clean.contains("fee") || clean.contains("charge") || clean.contains("cost") || clean.contains("rates") -> {
                    "Peer-to-peer (P2P) transfers inside PayLink are entirely free! Outbound transfers to banks or mobile services incur highly competitive rates of 0.5% - 1.2%."
                }
                clean.contains("security") || clean.contains("pin") || clean.contains("fingerprint") || clean.contains("safe") -> {
                    "PayLink builds advanced encryption, biometric locking, and secondary 4-digit security authorization PINs. Never share your passcode with support agents or external parties."
                }
                clean.contains("kyc") || clean.contains("verify") || clean.contains("document") || clean.contains("nida") -> {
                    "Submit your KYC validation anytime under 'Profile' tab, selecting dynamic options (National ID NIDA, Driver License, or Passport) and entering ID string keys for instant activation."
                }
                else -> {
                    "Thank you for reaching out! I am your PayLink virtual support agent. You can query me on 'how to deposit', 'paying utility bills', 'low rates', 'system safety', or 'KYC requirements'."
                }
            }
        }
    }
}
