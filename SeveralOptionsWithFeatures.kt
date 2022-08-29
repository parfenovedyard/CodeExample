package ac.android.sdk.visual.paywall.severalOptionsWithFeatures

import ac.android.sdk.visual.paywall.PayWallViewModel
import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SeveralOptionsWithFeatures(
    viewModel: PayWallViewModel,
    activity: Activity,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.weight(1f)) {

            Box {
                MainImageSOWF(viewModel)
                RestoreAndCloseButtonsSOWF(viewModel)
            }
            TitleSOWF(viewModel)
            Spacer(modifier = Modifier.height(12.dp))
            FeaturesListSOWF(viewModel)
            Spacer(modifier = Modifier.height(12.dp))
            IAPBlockSOWF(viewModel)
        }
        Column {
            Spacer(modifier = Modifier.height(12.dp))
            PurchaseButtonSOWF(viewModel, activity) {
                onClick()
            }
            Spacer(modifier = Modifier.height(8.dp))
            PrivacyAndTermsLinksSOWF(viewModel)
        }
    }
}
