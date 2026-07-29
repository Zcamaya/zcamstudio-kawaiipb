package com.zcamstudio.kawaiipb.domain.usecase

import com.zcamstudio.kawaiipb.domain.model.KioskSessionCatalog
import com.zcamstudio.kawaiipb.domain.repository.KioskRepository

class GetKioskSessionCatalogUseCase(
    private val repository: KioskRepository
) {
    suspend operator fun invoke(): KioskSessionCatalog = repository.getKioskSessionCatalog()
}
