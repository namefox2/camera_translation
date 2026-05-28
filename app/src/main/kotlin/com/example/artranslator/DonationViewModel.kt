package com.example.artranslator

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DonationProduct(
    val productId: String,
    val label: String,
    val details: ProductDetails? = null
)

@HiltViewModel
class DonationViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        val TIERS = listOf(
            DonationProduct("donation_400",  "400원"),
            DonationProduct("donation_900",  "900원"),
            DonationProduct("donation_1500", "1,500원"),
            DonationProduct("donation_2000", "2,000원")
        )
    }

    private val _products = MutableStateFlow(TIERS)
    val products: StateFlow<List<DonationProduct>> = _products.asStateFlow()

    private val _toast = MutableSharedFlow<String>()
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { consume(it) }
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // 사용자가 취소한 경우 — 아무 처리 없음
        } else {
            viewModelScope.launch { _toast.emit("결제를 완료할 수 없습니다 (${result.responseCode})") }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    init {
        connectAndQueryProducts()
    }

    private fun connectAndQueryProducts() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    viewModelScope.launch { queryProducts() }
                }
            }
            override fun onBillingServiceDisconnected() {
                // 필요 시 재연결 — billingClient가 내부적으로 관리
            }
        })
    }

    private suspend fun queryProducts() {
        val productList = TIERS.map { tier ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(tier.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val detailsMap = result.productDetailsList?.associateBy { it.productId } ?: emptyMap()
            _products.value = TIERS.map { tier ->
                tier.copy(details = detailsMap[tier.productId])
            }
        }
    }

    fun donate(activity: Activity, product: DonationProduct) {
        val details = product.details ?: run {
            viewModelScope.launch { _toast.emit("상품 정보를 불러오는 중입니다. 잠시 후 다시 시도해 주세요.") }
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    private fun consume(purchase: Purchase) {
        viewModelScope.launch {
            val params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            val result = billingClient.consumePurchase(params)
            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _toast.emit("후원해 주셔서 감사합니다 🙏")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        billingClient.endConnection()
    }
}
