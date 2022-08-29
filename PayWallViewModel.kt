package ac.android.sdk.visual.paywall

import ac.android.sdk.*
import ac.android.sdk.model.Logger
import ac.android.sdk.model.ShowStatus
import ac.android.sdk.model.config.ConfigRepo
import ac.android.sdk.model.data.ScreenStyleConfig
import ac.android.sdk.model.iap.IAPRepo
import ac.android.sdk.model.paywall.PaywallInteractor
import ac.android.sdk.model.paywall.UserRepo
import ac.android.sdk.model.paywall.data.IimageState
import ac.android.sdk.model.paywall.data.IlabelState
import ac.android.sdk.model.paywall.useCases.*
import ac.android.sdk.model.repositories.interfaces.HelperRepo
import ac.android.sdk.visual.baseFunsAndClasses.BaseViewModel
import android.app.Activity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentManager
import com.adapty.models.ProductModel
import com.google.gson.Gson
import kotlinx.coroutines.flow.*

internal class PayWallViewModel(
    private val paywallInteractor: PaywallInteractor,
    private val userRepo: UserRepo,
    private val iapRepo: IAPRepo,
    private val helper: HelperRepo,
    private val configRepo: ConfigRepo,
    private val gson: Gson,
    mainImageUseCase: MainImageUseCase,
    closeImageUseCase: CloseImageUseCase,
    restoreBtnUseCase: RestoreBtnUseCase,
    titleUseCase: TitleUseCase,
    featureListUseCase: FeatureListUseCase,
    private val iapBlockUseCase: IAPBlockUseCase,
    purchaseButtonUseCase: PurchaseButtonUseCase
) : BaseViewModel() {

    private var clickCounter: Int = 0

    val mainImage: StateFlow<IimageState> = mainImageUseCase.mainImage
    val closeImage: StateFlow<IimageState> = closeImageUseCase.closeImage
    val restore: StateFlow<IlabelState> = restoreBtnUseCase.restore
    val title: StateFlow<IlabelState> = titleUseCase.title
    val featuresList: StateFlow<List<FeaturesState>> = featureListUseCase.featureList
    val iapBlock: StateFlow<List<IapBlockState>> = iapBlockUseCase.iapBlock
    val button: StateFlow<PurchaseButtonState> = purchaseButtonUseCase.button

    override val styleConfig: ScreenStyleConfig by lazy {
        val json = getJsonFromFile("organicScreen.json")
        val defSsConfig: ScreenStyleConfig = gson.fromJson(json, ScreenStyleConfig::class.java)
        val styleConfig: ScreenStyleConfig? = paywallInteractor.ssConfig.value
        styleConfig ?: defSsConfig
    }
    val skippable: Boolean by lazy { userRepo.skippable }
    val paywallId = paywallInteractor.paywallConfig.value?.paywallID

    val products: StateFlow<List<ProductModel>> get() = iapRepo.products
    val selectedProduct: MutableState<ProductModel?> = mutableStateOf(null)

    private val _shouldCloseScreen: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val shouldCloseScreen get() = _shouldCloseScreen.asStateFlow()

    private val _termsOfUse: MutableStateFlow<String> = MutableStateFlow("")
    val termsOfUse get() = _termsOfUse.asStateFlow()

    private val _privacyPolicy: MutableStateFlow<String> = MutableStateFlow("")
    val privacyPolicy get() = _privacyPolicy.asStateFlow()

    var skipUrl = false
        set(value) {
            if (value) {
                _termsOfUse.value = ""
                _privacyPolicy.value = ""
            }
            field = value
        }

    init {
        paywallId?.let {
            val map = mapOf("paywallID" to it)
            Logger.logEvent(AC_SHOW_PAYWALL, map)
        }
        iapRepo.adaptyLogShowPaywall()
    }

    val images by lazy { paywallInteractor.imageMap.value }

    fun closePaywall(hook: String?, manager: FragmentManager) {
        _shouldCloseScreen.value = false
        userRepo.checkCustomFreeTrialStatusAfterClosePaywall()
        val status = paywallInteractor.getAfterClosePaywallStatus(hook)
        if (status.first == ShowStatus.DoNotOpen) return
        paywallId?.let {
            val map = mapOf("paywallID" to it)
            Logger.logEvent(AC_SHOW_SPECIAL_OFFER, map)
        }
        AcSdk.openPaywall(hook, manager) {}
    }

    fun restorePurchase(callback: (String?) -> Unit) {
        iapRepo.restorePurchase {
            callback(it)
        }
    }

    fun makePurchase(activity: Activity, callback: () -> Unit) {
        iapRepo.makePurchase(activity, selectedProduct.value) {
            callback()
        }
    }

    fun productClick(index: Int) {
        clickCounter++
        iapBlockUseCase.setSelectedProduct(index)
        if (clickCounter == 2) {
            clickCounter = 0
        }
    }

    fun termsOfUseClick() {
        skipUrl = false
        _termsOfUse.value = helper.getTermsAndConditionsURL(configRepo.config.companyDomain)
    }

    fun privacyPolicyClick() {
        skipUrl = false
        _privacyPolicy.value = helper.getPrivacyPolicyURL(configRepo.config.companyDomain)
    }

    fun logClosePaywall() {
        paywallId?.let {
            val map = mapOf("paywallID" to it)
            Logger.logEvent(AC_CLOSE_PAYWALL, map)
        }
    }

    override fun closeBtnClick() {
        _shouldCloseScreen.value = true
    }
}
