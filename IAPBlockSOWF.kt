package ac.android.sdk.visual.paywall.severalOptionsWithFeatures

import ac.android.sdk.model.paywall.useCases.IapBlockState
import ac.android.sdk.visual.paywall.PayWallViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter

@Composable
internal fun IAPBlockSOWF(viewModel: PayWallViewModel) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        viewModel.iapBlock.collectAsState().value.forEach {
            IAPButton(
                viewModel = viewModel,
                iapBlock = it
            )
        }
    }
}

@SuppressWarnings("squid:S3776")
@Composable
private fun IAPButton(
    viewModel: PayWallViewModel,
    iapBlock: IapBlockState
) {
    val button = iapBlock.button
    if (button.isEnabled) {
        Button(
            modifier = button.modifier,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = button.backgroundColor,
                disabledBackgroundColor = button.backgroundColor
            ),
            shape = button.shape,
            border = button.borderStroke,
            onClick = { viewModel.productClick(iapBlock.index) }
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                val icon = iapBlock.icon
                val checkBox = iapBlock.checkBox
                if (icon.isEnabled) {
                    Box(
                        modifier = checkBox.modifier.align(Alignment.CenterStart)
                    ) {
                        val placeholder = rememberAsyncImagePainter(model = icon.placeholder)
                        val errorImage = rememberAsyncImagePainter(model = icon.errorImage)
                        if (iapBlock.isPressed) {
                            AsyncImage(
                                modifier = icon.modifier,
                                model = icon.image,
                                alpha = icon.opacity,
                                placeholder = placeholder,
                                error = errorImage,
                                contentDescription = icon.contentDescription,
                                colorFilter = icon.tint,
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.padding(start = 36.dp)
                ) {
                    val title = iapBlock.title
                    if (title.isEnabled) {
                        Text(
                            modifier = title.modifier,
                            text = title.text,
                            style = title.textStyle
                        )
                    }
                    val subTitle = iapBlock.subTitle
                    if (subTitle.isEnabled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            modifier = subTitle.modifier,
                            text = subTitle.text,
                            style = subTitle.textStyle
                        )
                    }
                }
            }
        }
    }
}
