package com.zcamstudio.kawaiipb.domain.usecase

import com.zcamstudio.kawaiipb.domain.model.AdminDashboardSummary
import com.zcamstudio.kawaiipb.domain.repository.KioskRepository

class GetAdminDashboardSummaryUseCase(
    private val repository: KioskRepository
) {
    suspend operator fun invoke(): AdminDashboardSummary = repository.getAdminDashboardSummary()
}
