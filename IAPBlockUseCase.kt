package ac.android.sdk.model.paywall.useCases

import ac.android.sdk.model.data.*
import ac.android.sdk.model.iap.IAPRepo
import ac.android.sdk.model.paywall.PaywallInteractor
import ac.android.sdk.model.paywall.data.*
import ac.android.sdk.toColorIntOrDefault
import ac.android.sdk.toMyColorInt
import ac.android.sdk.visual.theme.*
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.IdRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sdk.R
import kotlinx.coroutines.flow.*

internal interface IAPBlockUseCase {
    val iapBlock: StateFlow<List<IapBlockState>>
    fun setSelectedProduct(index: Int)
}

internal class IAPBlockUseCaseImpl(
    private val context: Context,
    private val iapRepo: IAPRepo,
    private val paywallInteractor: PaywallInteractor
) : IAPBlockUseCase {

    private var isPressed: Boolean = false
    private var selectedProduct: Int = -1
    private val _iapBlock: MutableStateFlow<MutableList<IapBlockState>> =
        MutableStateFlow(mutableListOf(getIapBlockState()))
    override val iapBlock: StateFlow<List<IapBlockState>> = _iapBlock.asStateFlow()

    init {
        paywallInteractor.ssConfig.value?.let {
            setDataFromSsConfig(it.iapBlock, it.theme.colors, it.theme.fonts)
            setImages(paywallInteractor.imageMap)
        }
    }

    private fun getIapBlockState(
        obj: Ibutton? = null,
        colors: IthemeColors? = null,
        fonts: IthemeFonts? = null,
        index: Int = 0
    ): IapBlockState {
        isPressed = selectedProduct == index
        return IapBlockState(
            button = getButtonState(obj, colors),
            icon = getIimageState(obj?.icon, colors),
            title = getIlabelState(obj?.title, colors, fonts, R.string.iap_block_title, true, index),
            subTitle = getIlabelState(obj?.subtitle, colors, fonts, R.string.iap_block_subTitle, false, index),
            checkBox = getCheckBoxState(obj?.icon, colors, obj?.border?.width, obj?.backgroundColor),
            index = index,
            isPressed = isPressed
        )
    }

    private fun getButtonState(obj: Ibutton? = null, colors: IthemeColors? = null): IbuttonState {
        val shape = RoundedCornerShape(obj?.cornerRadius?.dp ?: 0.dp)
        val modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            .alpha(obj?.opacity ?: 1f)
        val backgroundColor = Color(obj?.backgroundColor.toColorIntOrDefault(colors?.second ?: Second))
        val borderWidth = (obj?.border?.width ?: 2f)
        val border = BorderStroke(
            width = if (isPressed) borderWidth.dp else (borderWidth / 2).dp,
            color = Color(
                if (isPressed) obj?.border?.color.toColorIntOrDefault(
                    obj?.backgroundColor ?: colors?.first ?: First
                ) else colors?.textSecond.toColorIntOrDefault(TextSecond)
            )
        )
        return IbuttonState(
            modifier = modifier,
            opacity = obj?.opacity ?: 1f,
            isEnabled = obj?._isEnabled ?: true,
            borderStroke = border,
            backgroundColor = backgroundColor,
            shape = shape
        )
    }

    private fun getIlabelState(
        obj: Ilabel? = null,
        colors: IthemeColors? = null,
        fonts: IthemeFonts? = null,
        @IdRes resId: Int,
        isTitle: Boolean,
        index: Int
    ): IlabelState {
        val shape = RoundedCornerShape(obj?.cornerRadius?.dp ?: 0.dp)
        val modifier = Modifier
            .clip(shape)
            .alpha(obj?.opacity ?: 1f)
            .background(
                color = Color(obj?.backgroundColor.toMyColorInt()),
                shape = shape
            )
        val defFontSize = if (isTitle) fonts?.medium?.size?.sp ?: FontMedium.sp
        else fonts?.small?.size?.sp ?: FontSmall.sp
        val defFontWeight = if (isTitle) fonts?.medium?.weight ?: FontMedium
        else fonts?.small?.weight ?: WeightSmall
        val textStyle = TextStyle(
            fontWeight = FontWeight(obj?.font?.weight ?: defFontWeight),
            fontSize = obj?.font?.size?.sp ?: defFontSize,
            color = Color(
                obj?.color.toColorIntOrDefault(colors?.textFirst ?: TextFirst)
            ).copy(alpha = obj?.opacity ?: 1f)
        )
        val text = iapRepo.getConfigText(obj?.text)
            ?: iapRepo.getAdaptyString(index)
            ?: context.getString(resId)
        return IlabelState(
            modifier = modifier,
            opacity = obj?.opacity ?: 1f,
            isEnabled = obj?._isEnabled ?: isTitle,
            textStyle = textStyle,
            text = text,
            textAlign = null
        )
    }

    private fun getIimageState(
        obj: Iimage? = null,
        colors: IthemeColors? = null
    ): IimageState {
        val defIcon = context.getDrawable(R.drawable.ic_check)
        val placeholderIcon = context.getDrawable(R.drawable.ic_check)
        val errorIcon = context.getDrawable(R.drawable.ic_check)
        val shape = RoundedCornerShape(obj?.cornerRadius?.dp ?: 0.dp)
        val modifier = Modifier
            .clip(shape)
            .alpha(obj?.opacity ?: 1f)
            .border(
                width = obj?.border?.width?.dp ?: 0.dp,
                color = Color(obj?.border?.color.toMyColorInt()),
                shape = shape
            )
            .background(
                color = Color(obj?.backgroundColor.toMyColorInt()),
                shape = shape
            )
            .size(20.dp)
        val tint = ColorFilter.tint(
            Color(obj?.tintColor.toColorIntOrDefault(colors?.opposite ?: Opposite))
        )
        return IimageState(
            modifier = modifier,
            image = defIcon,
            placeholder = placeholderIcon,
            errorImage = errorIcon,
            opacity = obj?.opacity ?: 1f,
            contentDescription = null,
            isEnabled = obj?._isEnabled ?: true,
            tint = tint,
            imageId = obj?.id
        )
    }

    private fun getCheckBoxState(
        obj: Iimage? = null,
        colors: IthemeColors? = null,
        borderWidth: Float? = null,
        backgroundColor: String? = null
    ): CheckBoxState {
        val checkBoxWidth = (borderWidth ?: 2f)
        val shape = CircleShape
        val modifier = Modifier
            .clip(shape)
            .alpha(obj?.opacity ?: 1f)
            .border(
                width = if (isPressed) checkBoxWidth.dp else (checkBoxWidth / 2).dp,
                color = Color(
                    if (isPressed) obj?.border?.color.toColorIntOrDefault(
                        obj?.backgroundColor ?: colors?.first ?: First
                    ) else colors?.textSecond.toColorIntOrDefault(TextSecond)
                ),
                shape = shape
            )
            .background(
                color = Color(
                    if (isPressed) obj?.backgroundColor.toColorIntOrDefault(colors?.first ?: First)
                    else backgroundColor.toMyColorInt()
                ),
                shape = shape
            )
            .size(20.dp)
        return CheckBoxState(modifier = modifier)
    }

    private fun setDataFromSsConfig(obj: IIAPBlock?, colors: IthemeColors?, fonts: IthemeFonts?) {
        _iapBlock.value = iapRepo.products.value.mapIndexed { index, _ ->
            val defButton = obj?.defaultButton
            val buttonsList = obj?.customButtons
            getIapBlockState(buttonsList?.getOrNull(index) ?: defButton, colors, fonts, index)
        }.toMutableList()
    }

    private fun setImages(imageMap: StateFlow<Map<String, Drawable>>) {
        _iapBlock.value.forEach {
            it.icon = it.icon.copy(
                image = imageMap.value[it.icon.imageId]
            )
        }
    }

    override fun setSelectedProduct(index: Int) {
        selectedProduct = index
        paywallInteractor.ssConfig.value?.let {
            setDataFromSsConfig(it.iapBlock, it.theme.colors, it.theme.fonts)
            setImages(paywallInteractor.imageMap)
        }
    }
}

internal data class IapBlockState(
    var button: IbuttonState,
    var icon: IimageState,
    var title: IlabelState,
    var subTitle: IlabelState,
    var checkBox: CheckBoxState,
    val index: Int,
    var isPressed: Boolean
)

internal data class CheckBoxState(
    val modifier: Modifier
)
