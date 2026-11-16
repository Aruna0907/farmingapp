package com.example.farming.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.farming.data.db.FarmEntity
import com.example.farming.data.model.Advice
import com.example.farming.data.model.ForecastDay
import com.example.farming.data.model.IrrigationLog
import com.example.farming.data.repository.RecommendationStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FarmViewModel : ViewModel() {

    private val _allFarms = MutableLiveData<List<FarmEntity>>(emptyList())
    val allFarms: LiveData<List<FarmEntity>> = _allFarms

    private val _selectedFarm = MutableLiveData<FarmEntity?>()
    val selectedFarm: LiveData<FarmEntity?> = _selectedFarm

    private val _adviceState = MutableLiveData<Result<Advice>?>()
    val adviceState: LiveData<Result<Advice>?> = _adviceState

    private val _irrigationLogs = MutableLiveData<List<IrrigationLog>>(emptyList())
    val irrigationLogs: LiveData<List<IrrigationLog>> = _irrigationLogs

    fun addFarm(name: String, crop: String, latitude: Double, longitude: Double) {
        val newFarm = FarmEntity(
            id = System.currentTimeMillis(),
            name = name,
            crop = crop,
            latitude = latitude,
            longitude = longitude
        )
        val updatedList = (_allFarms.value ?: emptyList()) + newFarm
        _allFarms.value = updatedList
        selectFarm(newFarm)
    }

    fun selectFarm(farm: FarmEntity) {
        _selectedFarm.value = farm
        updateAdvice(farm)
    }

    private fun updateAdvice(farm: FarmEntity) {
        val et0 = 4.5
        val rainfall = 1.0
        val soilMoisture = 0.25
        val netDeficit = (et0 - rainfall).coerceAtLeast(0.0)

        val status = if (netDeficit > 3.0) RecommendationStatus.IRRIGATE else RecommendationStatus.NOT_NEEDED

        val forecastList = listOf(
            ForecastDay("Mon", 28.0, 18.0, 4.2, 0.0, RecommendationStatus.IRRIGATE),
            ForecastDay("Tue", 30.0, 19.0, 4.8, 0.0, RecommendationStatus.IRRIGATE),
            ForecastDay("Wed", 26.0, 17.0, 3.5, 5.0, RecommendationStatus.NOT_NEEDED),
            ForecastDay("Thu", 25.0, 16.0, 3.0, 12.0, RecommendationStatus.NOT_NEEDED),
            ForecastDay("Fri", 27.0, 17.0, 4.0, 0.0, RecommendationStatus.IRRIGATE),
            ForecastDay("Sat", 29.0, 18.0, 4.6, 0.0, RecommendationStatus.IRRIGATE),
            ForecastDay("Sun", 28.0, 18.0, 4.3, 1.0, RecommendationStatus.NOT_NEEDED)
        )

        val advice = Advice(
            status = status,
            et0Today = et0,
            rainfallToday = rainfall,
            soilMoistureToday = soilMoisture,
            netDeficit = netDeficit,
            forecastItems = forecastList
        )

        _adviceState.value = Result.success(advice)
    }

    fun logIrrigation(durationMinutes: Int) {
        val currentFarm = _selectedFarm.value ?: return
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date())

        val newLog = IrrigationLog(
            farmId = currentFarm.id,
            durationMinutes = durationMinutes,
            timestamp = dateStr
        )
        val currentLogs = _irrigationLogs.value ?: emptyList()
        _irrigationLogs.value = listOf(newLog) + currentLogs
    }
}
