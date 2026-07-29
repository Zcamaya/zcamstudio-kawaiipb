package com.zcamstudio.kawaiipb.domain.usecase

import com.zcamstudio.kawaiipb.domain.repository.KioskRepository

class ValidateAdminPinUseCase(
    private val repository: KioskRepository
) {
    suspend operator fun invoke(pin: String): Boolean = repository.isValidAdminPin(pin)
}
