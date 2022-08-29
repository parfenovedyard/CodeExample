package ac.android.sdk.model.iap

import android.app.Activity
import com.adapty.models.PaywallModel
import com.adapty.models.ProductModel
import kotlinx.coroutines.flow.StateFlow

internal interface IAPRepo {
    val paywalls: StateFlow<List<PaywallModel>>
    val products: StateFlow<List<ProductModel>>

    fun fetchProducts()
    fun adaptyLogShowPaywall()
    fun restorePurchase(callback: (String?) -> Unit)
    fun makePurchase(activity: Activity, selectedProduct: ProductModel?, callback: () -> Unit)
    fun getConfigText(input: String?): String?
    fun getAdaptyString(index: Int): String?
}
