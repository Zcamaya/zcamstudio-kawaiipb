package com.zcamstudio.kawaiipb.domain.usecase

import com.zcamstudio.kawaiipb.domain.model.LandingConfig
import com.zcamstudio.kawaiipb.domain.repository.KioskRepository

class GetLandingConfigUseCase(
    private val repository: KioskRepository
) {
    suspend operator fun invoke(): LandingConfig = repository.getLandingConfig()
}
