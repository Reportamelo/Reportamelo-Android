package app.reportamelo.services

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object SubscriptionManager : UpdatedCustomerInfoListener {

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _customerInfo = MutableStateFlow<CustomerInfo?>(null)
    val customerInfo: StateFlow<CustomerInfo?> = _customerInfo.asStateFlow()

    private var created: Boolean = false

    fun init(context: Context) {
        val apiKey = getApiKey(context)
        if (apiKey.isNullOrEmpty()) {
            Log.w("SubscriptionManager", "REVENUECAT_API_KEY not found in Manifest; skipping RevenueCat initialization.")
            return
        }

        Purchases.logLevel = com.revenuecat.purchases.LogLevel.DEBUG
        Purchases.configure(PurchasesConfiguration.Builder(context, apiKey).build())
        Purchases.sharedInstance.updatedCustomerInfoListener = this
    }

    private fun getApiKey(context: Context): String? {
        return try {
            val applicationInfo = context.packageManager.getApplicationInfo(
                context.packageName, PackageManager.GET_META_DATA
            )
            applicationInfo.metaData?.getString("REVENUECAT_API_KEY")
        } catch (e: Exception) {
            null
        }
    }

    override fun onReceived(customerInfo: CustomerInfo) {
        updateState(customerInfo)
    }

    private fun updateState(customerInfo: CustomerInfo?) {
        _customerInfo.value = customerInfo
        _isPro.value = customerInfo?.entitlements?.all?.get("Reportamelo Pro")?.isActive == true
        setPlanType()
    }

    private fun setPlanType() {
        val planType = if (_isPro.value) "paid" else "freemium"
        // KeychainService equivalent on Android to save planType
        // e.g., SharedPreferences
    }

    fun checkEntitlement() {
        if (!Purchases.isConfigured) {
            runCatching { Log.w("SubscriptionManager", "Purchases is not configured; skipping checkEntitlement.") }
            return
        }
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                updateState(customerInfo)
            }

            override fun onError(error: PurchasesError) {
                runCatching { Log.e("SubscriptionManager", "Error fetching customer info: ${error.message}") }
            }
        })
    }

    suspend fun performUserLogin(userId: String): Boolean = suspendCoroutine { continuation ->
        if (!Purchases.isConfigured) {
            runCatching { Log.w("SubscriptionManager", "Purchases is not configured; skipping performUserLogin.") }
            continuation.resume(false)
            return@suspendCoroutine
        }
        try {
            Purchases.sharedInstance.logIn(userId, object : LogInCallback {
                override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {
                    SubscriptionManager.created = created
                    updateState(customerInfo)
                    continuation.resume(true)
                }

                override fun onError(error: PurchasesError) {
                    runCatching { Log.e("SubscriptionManager", "RevenueCat login failed: ${error.message}") }
                    continuation.resume(false)
                }
            })
        } catch (e: Exception) {
            runCatching { Log.e("SubscriptionManager", "RevenueCat login error: ${e.message}", e) }
            continuation.resume(false)
        }
    }

    fun purchase(activity: Activity, packageToPurchase: Package) {
        if (!Purchases.isConfigured) {
            Log.w("SubscriptionManager", "Purchases is not configured; skipping purchase.")
            return
        }
        Purchases.sharedInstance.purchase(
            com.revenuecat.purchases.PurchaseParams.Builder(activity, packageToPurchase).build(),
            object : com.revenuecat.purchases.interfaces.PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                    updateState(customerInfo)
                }

                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    Log.e("SubscriptionManager", "Purchase error: ${error.message}")
                }
            }
        )
    }

    suspend fun restorePurchases(): Boolean = suspendCoroutine { continuation ->
        if (!Purchases.isConfigured) {
            Log.w("SubscriptionManager", "Purchases is not configured; skipping restorePurchases.")
            continuation.resume(false)
            return@suspendCoroutine
        }
        Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                updateState(customerInfo)
                continuation.resume(true)
            }

            override fun onError(error: PurchasesError) {
                Log.e("SubscriptionManager", "Restore error: ${error.message}")
                continuation.resume(false)
            }
        })
    }

    suspend fun handleUserLogout(): Boolean = suspendCoroutine { continuation ->
        if (!Purchases.isConfigured) {
            continuation.resume(false)
            return@suspendCoroutine
        }
        try {
            Purchases.sharedInstance.logOut(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    updateState(customerInfo)
                    continuation.resume(true)
                }

                override fun onError(error: PurchasesError) {
                    Log.e("SubscriptionManager", "RevenueCat logout error: ${error.message}")
                    continuation.resume(false)
                }
            })
        } catch (e: Exception) {
            continuation.resume(false)
        }
    }
}
