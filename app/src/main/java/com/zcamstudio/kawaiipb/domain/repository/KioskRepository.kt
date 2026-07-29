package com.zcamstudio.kawaiipb.domain.repository

import com.zcamstudio.kawaiipb.domain.model.AdminDashboardSummary
import com.zcamstudio.kawaiipb.domain.model.KioskSessionCatalog
import com.zcamstudio.kawaiipb.domain.model.LandingConfig
import com.zcamstudio.kawaiipb.domain.model.FlowStep

interface KioskRepository {
    suspend fun getLandingConfig(): LandingConfig
    suspend fun isValidAdminPin(pin: String): Boolean
    suspend fun getAdminDashboardSummary(): AdminDashboardSummary
    suspend fun getFlowSteps(): List<FlowStep>
    suspend fun getKioskSessionCatalog(): KioskSessionCatalog
}
