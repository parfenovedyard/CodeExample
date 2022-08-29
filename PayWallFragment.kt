package ac.android.sdk.visual.paywall

import ac.android.sdk.*
import ac.android.sdk.di.ViewModelModule
import ac.android.sdk.model.Logger
import ac.android.sdk.visual.paywall.oneOptionWithFeedbacks.OneOptionWithFeedbacks
import ac.android.sdk.visual.paywall.severalOptionsWithFeatures.SeveralOptionsWithFeatures
import ac.android.sdk.visual.paywall.threeOptionsWithFeedbacks.ThreeOptionsWithFeedbacks
import ac.android.sdk.visual.theme.SdkTheme
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.coroutineScope
import com.android.sdk.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch


internal class PayWallFragment : BottomSheetDialogFragment() {

    private val viewModel: PayWallViewModel = ViewModelModule.payWallViewModel
    private var hook: String? = null

    // hide statusBar
    override fun getTheme(): Int = R.style.BottomDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent { ShowScreen(viewModel) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hook = arguments?.getString(HOOK)
        (dialog as BottomSheetDialog).apply {
            setCancelable(!viewModel.skippable)
            behavior.isDraggable = !viewModel.skippable
            behavior.state = BottomSheetBehavior.STATE_EXPANDED

            // different variants to hide statusBar in 12 android
            /*@Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window?.setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )

                window?.insetsController?.hide(WindowInsets.Type.statusBars())
                window?.insetsController?.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                window?.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            }*/
        }
        initActionsListeners()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        hook = "after$hook"
        viewModel.logClosePaywall()
        viewModel.closePaywall(hook, parentFragmentManager)
    }

    private fun initActionsListeners() {
        viewLifecycleOwner.lifecycle.coroutineScope.launch {
            viewModel.termsOfUse.collect { url ->
                if (url.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    try {
                        requireActivity().startActivity(intent)
                    } catch (e: Exception) {
                        Logger.logError("open termsOfUse failed")
                        e.printStackTrace()
                        Toast.makeText(requireContext(), getString(R.string.error_open_links), Toast.LENGTH_LONG).show()
                    } finally {
                        viewModel.skipUrl = true
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycle.coroutineScope.launch {
            viewModel.privacyPolicy.collect { url ->
                if (url.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    try {
                        requireActivity().startActivity(intent)
                    } catch (e: Exception) {
                        Logger.logError("open privacyPolicy failed")
                        e.printStackTrace()
                        Toast.makeText(requireContext(), getString(R.string.error_open_links), Toast.LENGTH_LONG).show()
                    } finally {
                        viewModel.skipUrl = true
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycle.coroutineScope.launch{
            viewModel.shouldCloseScreen.collect{
                if (it)dismiss()
            }
        }
    }

    @Composable
    private fun ShowScreen(viewModel: PayWallViewModel) {
        //val coroutine = rememberCoroutineScope()
        SdkTheme(viewModel = viewModel) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                color = MaterialTheme.colors.background
            ) {
                ChooseAndShow(viewModel)
            }
        }
    }

    @Composable
    private fun ChooseAndShow(viewModel: PayWallViewModel) {
        when (arguments?.getString(SCREEN_NAME)?.lowercase()) {
            SEVERAL_OPTIONS_WITH_FEATURES.lowercase() -> {
                if (viewModel.products.value.isEmpty()) {
                    viewModel.paywallId?.let {
                        val map = mapOf("paywallID" to it)
                        Logger.logEvent(AC_LOG_ERROR_PRODUCTS_NUMBER_INCORRECT, map)
                    }
                }
                SeveralOptionsWithFeatures(
                    viewModel = viewModel,
                    activity = requireActivity()
                ) {
                    dismiss()
                }
            }
            ONE_OPTION_WITH_FEEDBACKS.lowercase() -> {
                if (viewModel.products.value.isEmpty()) {
                    viewModel.paywallId?.let {
                        val map = mapOf("paywallID" to it)
                        Logger.logEvent(AC_LOG_ERROR_PRODUCTS_NUMBER_INCORRECT, map)
                    }
                }
                OneOptionWithFeedbacks(
                    viewModel = viewModel,
                    activity = requireActivity()
                ) {
                    dismiss()
                }
            }
            THREE_OPTIONS_WITH_FEATURES.lowercase() -> {
                if (viewModel.products.value.size < 3) {
                    viewModel.paywallId?.let {
                        val map = mapOf("paywallID" to it)
                        Logger.logEvent(AC_LOG_ERROR_PRODUCTS_NUMBER_INCORRECT, map)
                    }
                }
                ThreeOptionsWithFeedbacks(
                    viewModel = viewModel,
                    activity = requireActivity()
                ) {
                    dismiss()
                }
            }
        }
    }
}
