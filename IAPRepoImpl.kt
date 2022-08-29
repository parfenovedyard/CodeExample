package ac.android.sdk.model.iap

import ac.android.sdk.*
import ac.android.sdk.di.IAPModule
import ac.android.sdk.di.RepoModule
import ac.android.sdk.model.Logger
import ac.android.sdk.model.paywall.data.PaywallConfig
import android.app.Activity
import android.content.Context
import com.adapty.errors.AdaptyErrorCode
import com.adapty.models.*
import com.android.sdk.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date

internal class IAPRepoImpl(
    private val context: Context
) : IAPRepo {

    private val adapty by lazy { IAPModule.adapty }
    private val paywallInteractor by lazy { IAPModule.paywallInteractor }
    private val userRepo by lazy { RepoModule.userRepo }

    private val _paywalls: MutableStateFlow<List<PaywallModel>> = MutableStateFlow(emptyList())
    override val paywalls get() = _paywalls.asStateFlow()

    private val _products: MutableStateFlow<List<ProductModel>> = MutableStateFlow(emptyList())
    override val products get() = _products.asStateFlow()

    private val ssConfig: StateFlow<PaywallConfig?> = paywallInteractor.paywallConfig

    private fun fetchPaywalls() {
        adapty.getPaywalls { paywalls, _, error ->
            error?.let {
                Logger.logError("fetchPaywalls failed, error: ${error.message}")
            } ?: run {
                paywalls?.let {
                    _paywalls.value = it
                }
            }
        }
    }

    override fun fetchProducts() {
        val paywallId = ssConfig.value?.paywallID.toString()
        val paywall = paywalls.value.firstOrNull { it.developerId == paywallId }
        paywall?.let { pw ->
            _products.value = pw.products.also {
                it.ifEmpty {
                    Logger.logEvent(AC_LOG_ERROR_ADAPTY_PRODUCT_NOT_FOUND, mapOf("paywallID" to paywallId))
                }
            }
        } ?: Logger.logEvent(AC_LOG_ERROR_ADAPTY_PAYWALL_NOT_FOUND, mapOf("paywallID" to paywallId))

    }

    private fun fetchPurchaserInfo() {
        adapty.getPurchaserInfo { purchaserInfo, error ->
            error?.let {
                Logger.logError("fetchPurchaserInfo failed, error: ${error.message}")
            } ?: handleAccessLevel(purchaserInfo?.accessLevels?.get(PREMIUM)) {}
        }
    }

    init {
        CoroutineScope(Dispatchers.Main).launch {
            fetchPaywalls()
            fetchPurchaserInfo()
        }
    }

    override fun adaptyLogShowPaywall() {
        val paywallId = ssConfig.value?.paywallID
        val paywall = paywalls.value.firstOrNull { it.developerId == paywallId }
        paywall?.let { adapty.logShowPaywall(it) }
    }


    override fun restorePurchase(callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            adapty.restorePurchases { purchaserInfo, _, error ->
                error?.let {
                    val map = mapOf("paywallId" to ssConfig.value?.paywallID.orEmpty())
                    Logger.logEvent(AC_LOG_ERROR_RESTORE, map)
                    callback(it.message)
                } ?: handleAccessLevel(purchaserInfo?.accessLevels?.get(PREMIUM)) { callback(it) }
            }
        }
    }

    override fun makePurchase(activity: Activity, selectedProduct: ProductModel?, callback: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val map = mapOf(
                "paywallId" to ssConfig.value?.paywallID.orEmpty(),
                "productId" to selectedProduct?.vendorProductId.orEmpty()
            )
            Logger.logEvent(AC_START_PAYMENT, map)
            selectedProduct?.let {
                adapty.makePurchase(
                    activity,
                    it
                ) { purchaserInfo, _, _, _, error ->
                    error?.let {
                        if (error.adaptyErrorCode == AdaptyErrorCode.USER_CANCELED) {
                            Logger.logEvent(AC_CANCEL_PAYMENT, map)
                        } else {
                            Logger.logEvent(AC_LOG_ERROR_PAYMENT, map)
                        }
                    } ?: handleAccessLevel(purchaserInfo?.accessLevels?.get(PREMIUM)) { callback() }
                }
            } ?: Logger.logError("makePurchase, selected product null")
        }
    }

    override fun getConfigText(input: String?): String? {
        return input?.let { notNullText ->
            var text: String = notNullText
            products.value.forEachIndexed { index, product ->
                val regex = "\\{(.*?)\\}".toRegex().findAll(text).iterator().forEach { result ->
                    if (result.value.contains(index.toString())) {
                        if (!product.introductoryOfferEligibility
                            && result.value.contains("introductoryDiscount.localizedPrice")
                        ) {
                            product.introductoryDiscount?.localizedPrice?.let {
                                text = text.replace(result.value, it)
                            }
                        }
                        if (!product.introductoryOfferEligibility
                            && result.value.contains("introductoryDiscount.localizedSubscriptionPeriod")
                        ) {
                            product.introductoryDiscount?.localizedSubscriptionPeriod?.let {
                                text = text.replace(result.value, it)
                            }
                        }
                        if (result.value.contains("localizedPrice")) {
                            product.localizedPrice?.let {
                                text = text.replace(result.value, it)
                            }
                        }
                        if (result.value.contains("localizedSubscriptionPeriod")) {
                            product.localizedSubscriptionPeriod?.let {
                                text = text.replace(result.value, it)
                            }
                        }
                    }
                }
            }
            text
        }
    }

    override fun getAdaptyString(index: Int): String? {
        val product = products.value[index]
        val period = product.localizedSubscriptionPeriod
        val price = product.localizedPrice
        return if (period != null && price != null) {
            context.getString(R.string.adapty_string, period, price)
        } else{
            null
        }
    }

    private fun handleAccessLevel(level: AccessLevelInfoModel?, callback: (String?) -> Unit) {
        if (level?.isActive == true && !level.isInGracePeriod) {
            userRepo.subStatus = ACTIVE
            callback(null) // for make/ restore purchase
        } else if (level?.isInGracePeriod == true) {
            userRepo.subStatus = IN_GRACE_PERIOD
            callback(null) // for make/ restore purchase
        } else if (useExpiredStatus(level?.expiresAt)) {
            userRepo.subStatus = EXPIRED
            callback("") // for make/ restore purchase
        }
    }

    private fun useExpiredStatus(date: String?): Boolean {
        if (date == null) return false
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ", Locale.getDefault())
            val expiredDate = dateFormat.parse(date)
            val currentDate = Date(System.currentTimeMillis())
            currentDate.after(expiredDate)
        } catch (e: ParseException) {
            e.printStackTrace()
            Logger.logError("Compare dates failed: ${e.message}")
            false
        }
    }
}
