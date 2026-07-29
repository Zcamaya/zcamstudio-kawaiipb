package com.zcamstudio.kawaiipb.domain.usecase

import com.zcamstudio.kawaiipb.domain.model.FlowStep
import com.zcamstudio.kawaiipb.domain.repository.KioskRepository

class GetFlowStepsUseCase(
    private val repository: KioskRepository
) {
    suspend operator fun invoke(): List<FlowStep> = repository.getFlowSteps()
}
