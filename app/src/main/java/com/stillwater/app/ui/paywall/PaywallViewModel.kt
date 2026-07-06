package com.stillwater.app.ui.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.stillwater.app.data.billing.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class PlanChoice { ANNUAL, MONTHLY }

data class PlanOption(
    val choice: PlanChoice,
    val priceText: String,
    val periodText: String,
    val trialText: String?,
    val offerToken: String,
)

data class PaywallUiState(
    val isPremium: Boolean = false,
    val billingAvailable: Boolean = true,
    val options: List<PlanOption> = emptyList(),
    val selected: PlanChoice = PlanChoice.ANNUAL,
) {
    val selectedOption: PlanOption? get() = options.firstOrNull { it.choice == selected }
}

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
) : ViewModel() {

    private val selected = MutableStateFlow(PlanChoice.ANNUAL)

    val uiState: StateFlow<PaywallUiState> = combine(
        billingRepository.isPremium,
        billingRepository.billingState,
        selected.asStateFlow(),
    ) { premium, billing, choice ->
        val product = (billing as? BillingRepository.BillingState.Ready)?.product
        PaywallUiState(
            isPremium = premium,
            billingAvailable = billing !is BillingRepository.BillingState.Unavailable && product != null,
            options = product?.let(::toOptions) ?: emptyList(),
            selected = choice,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PaywallUiState())

    private fun toOptions(product: ProductDetails): List<PlanOption> {
        val offers = product.subscriptionOfferDetails.orEmpty()
        return offers.mapNotNull { offer ->
            val phases = offer.pricingPhases.pricingPhaseList
            val paidPhase = phases.lastOrNull() ?: return@mapNotNull null
            val trialPhase = phases.firstOrNull { it.priceAmountMicros == 0L }
            val choice = when (paidPhase.billingPeriod) {
                "P1Y" -> PlanChoice.ANNUAL
                "P1M" -> PlanChoice.MONTHLY
                else -> return@mapNotNull null
            }
            PlanOption(
                choice = choice,
                priceText = paidPhase.formattedPrice,
                periodText = if (choice == PlanChoice.ANNUAL) "per year" else "per month",
                trialText = trialPhase?.let { "7-day free trial" },
                offerToken = offer.offerToken,
            )
        }.distinctBy { it.choice }.sortedBy { it.choice.ordinal }
    }

    fun select(choice: PlanChoice) = selected.update { choice }

    fun purchase(activity: Activity) {
        val option = uiState.value.selectedOption ?: return
        billingRepository.launchPurchase(activity, option.offerToken)
    }

    fun restore() = billingRepository.refreshEntitlement()
}
