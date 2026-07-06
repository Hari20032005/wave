package com.stillwater.app.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stillwater.app.data.PlanRepository
import com.stillwater.app.domain.model.IfThenPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanUiState(
    val plan: IfThenPlan? = null,
)

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val planRepository: PlanRepository,
) : ViewModel() {

    val uiState: StateFlow<PlanUiState> = planRepository.activePlans
        .map { PlanUiState(plan = it.firstOrNull()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlanUiState(),
        )

    fun rehearse() {
        val plan = uiState.value.plan ?: return
        viewModelScope.launch { planRepository.recordRehearsal(plan.id) }
    }
}
