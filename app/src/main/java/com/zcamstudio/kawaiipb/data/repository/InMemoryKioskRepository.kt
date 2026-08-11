package com.zcamstudio.kawaiipb.data.repository

import com.zcamstudio.kawaiipb.domain.model.AdminDashboardSummary
import com.zcamstudio.kawaiipb.domain.model.CameraMode
import com.zcamstudio.kawaiipb.domain.model.ChartPoint
import com.zcamstudio.kawaiipb.domain.model.KioskSessionCatalog
import com.zcamstudio.kawaiipb.domain.model.DashboardMetric
import com.zcamstudio.kawaiipb.domain.model.FlowStep
import com.zcamstudio.kawaiipb.domain.model.LandingConfig
// StickerOption removed
import com.zcamstudio.kawaiipb.domain.model.StripSize
// TemplateOption removed
import com.zcamstudio.kawaiipb.domain.model.RankedItem
import com.zcamstudio.kawaiipb.domain.model.StatusBadge
import com.zcamstudio.kawaiipb.domain.repository.KioskRepository
import kotlinx.coroutines.delay

class InMemoryKioskRepository : KioskRepository {

    override suspend fun getLandingConfig(): LandingConfig {
        delay(120)
        return LandingConfig()
    }

    override suspend fun isValidAdminPin(pin: String): Boolean {
        delay(90)
        return false
    }

    override suspend fun getAdminDashboardSummary(): AdminDashboardSummary {
        delay(150)
        return AdminDashboardSummary(
            metrics = listOf(
                DashboardMetric("Today's Sessions", "128", "+12%"),
                DashboardMetric("Photos Printed", "346", "+18%"),
                DashboardMetric("Storage Used", "32.4 GB", "+6%"),
                DashboardMetric("Remaining Storage", "187 GB", "Healthy")
            ),
            sessionsTrend = listOf(
                ChartPoint("Mon", 38),
                ChartPoint("Tue", 52),
                ChartPoint("Wed", 47),
                ChartPoint("Thu", 63),
                ChartPoint("Fri", 81),
                ChartPoint("Sat", 74),
                ChartPoint("Sun", 90)
            ),
            topTemplates = listOf(
                RankedItem("Sakura Strip", 40),
                RankedItem("Classic Duo", 30),
                RankedItem("Elevator Glow", 20),
                RankedItem("Others", 10)
            ),
            topStickers = listOf(
                RankedItem("Heart", 26),
                RankedItem("Blossom", 22),
                RankedItem("Ribbon", 18),
                RankedItem("Cat", 14),
                RankedItem("Cake", 10)
            ),
            statuses = listOf(
                StatusBadge("Printer", true, "Online and ready"),
                StatusBadge("Camera", true, "Front camera detected"),
                StatusBadge("Storage", true, "Auto-clean enabled"),
                StatusBadge("Last Sync", true, "Local cache is current")
            )
        )
    }

    override suspend fun getFlowSteps(): List<FlowStep> {
        delay(80)
        return listOf(
            FlowStep(1, "Landing", "Instant", "Warm welcome with hidden admin access"),
            FlowStep(2, "Choose Camera", "20s", "Classic and elevator modes"),
            FlowStep(3, "Capture Session", "90s", "Eight-photo guided capture"),
            FlowStep(4, "Strip Size", "10s", "4x1, 3x1, 2x1 presets"),
            // Template/Drawing/Sticker stages removed
            FlowStep(8, "Preview", "Immediate", "Final composition check"),
            FlowStep(9, "Printing", "Queued", "Retry-safe local printer pipeline"),
            FlowStep(10, "QR Code", "5 min", "Download and session expiry")
        )
    }

    override suspend fun getKioskSessionCatalog(): KioskSessionCatalog {
        delay(100)
        return KioskSessionCatalog(
            cameraModes = listOf(CameraMode.Classic, CameraMode.Elevator),
            stripSizes = StripSize.values().toList(),
            // templates and stickers removed
        )
    }
}
