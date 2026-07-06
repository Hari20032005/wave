package com.stillwater.app.data.billing

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.stillwater.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Play Billing wiring for the single "premium" subscription (annual + monthly
 * base plans; annual carries the 7-day trial). Entitlement is cached in
 * DataStore so gates work offline; Play remains the source of truth whenever
 * a connection succeeds.
 *
 * No INTERNET permission needed: BillingClient talks to the Play app by IPC.
 */
@Singleton
class BillingRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_ID = "stillwater_premium"
        private val PREMIUM_KEY = booleanPreferencesKey("premium_entitled")

        /**
         * Debug builds preview premium features so the whole app is testable
         * without Play (emulators, sideloads). Release builds always gate on
         * the real entitlement.
         */
        private val PREMIUM_PREVIEW = BuildConfig.DEBUG
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    sealed interface BillingState {
        data object Connecting : BillingState
        data class Ready(val product: ProductDetails?) : BillingState
        /** Play unavailable (no Play services, region, sideload). Not an error state for the user. */
        data object Unavailable : BillingState
    }

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Connecting)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    /** Entitlement gate used across the app. */
    val isPremium = dataStore.data.map { prefs ->
        PREMIUM_PREVIEW || (prefs[PREMIUM_KEY] ?: false)
    }

    init {
        connect()
    }

    private fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        refreshEntitlement()
                        queryProduct()
                    }
                } else {
                    _billingState.value = BillingState.Unavailable
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingState.value = BillingState.Unavailable
            }
        })
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()
        billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
            _billingState.value = if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                BillingState.Ready(detailsResult.productDetailsList.firstOrNull())
            } else {
                BillingState.Ready(null)
            }
        }
    }

    /** Re-check owned subscriptions (also the "restore purchases" action). */
    fun refreshEntitlement() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val entitled = purchases.any { purchase ->
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    PRODUCT_ID in purchase.products
            }
            purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
                .forEach(::acknowledge)
            scope.launch { setEntitled(entitled) }
        }
    }

    fun launchPurchase(activity: Activity, offerToken: String) {
        val product = (billingState.value as? BillingState.Ready)?.product ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) return
        val entitled = purchases.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                PRODUCT_ID in purchase.products
        }
        purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach(::acknowledge)
        if (entitled) scope.launch { setEntitled(true) }
    }

    private fun acknowledge(purchase: Purchase) {
        billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build(),
        ) { }
    }

    private suspend fun setEntitled(entitled: Boolean) {
        dataStore.edit { prefs -> prefs[PREMIUM_KEY] = entitled }
    }
}
