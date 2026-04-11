package com.example.weatherapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository()
    private val app = application
    private val workManager = WorkManager.getInstance(application)
    private val bulkWeatherObservers = mutableMapOf<UUID, Observer<WorkInfo>>()
    private val finishedBulkWorkIds = mutableSetOf<UUID>()
    private var expectedBulkWorkCount = 0

    private val _cities = MutableLiveData<MutableList<String>>(
        mutableListOf("Moscow", "London", "Paris", "New York", "Tokyo")
    )
    val cities: LiveData<MutableList<String>> = _cities

    private val _selectedCityIndex = MutableLiveData<Int>(0)
    val selectedCityIndex: LiveData<Int> = _selectedCityIndex

    private val _weatherCache = MutableLiveData<MutableMap<String, WeatherData>>(mutableMapOf())
    val weatherCache: LiveData<MutableMap<String, WeatherData>> = _weatherCache

    private val _weatherData = MutableLiveData<WeatherData?>()
    val weatherData: LiveData<WeatherData?> = _weatherData

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isCelsius = MutableLiveData<Boolean>(true)
    val isCelsius: LiveData<Boolean> = _isCelsius

    private val _showWindDirection = MutableLiveData<Boolean>(true)
    val showWindDirection: LiveData<Boolean> = _showWindDirection

    private val _citiesUpdated = MutableLiveData<Boolean>()
    val citiesUpdated: LiveData<Boolean> = _citiesUpdated

    private val _weatherUpdatedEvent = MutableLiveData<Pair<Int, WeatherData>?>()
    val weatherUpdatedEvent: LiveData<Pair<Int, WeatherData>?> = _weatherUpdatedEvent

    fun setTemperatureUnit(celsius: Boolean) {
        _isCelsius.value = celsius
    }

    fun setShowWindDirection(show: Boolean) {
        _showWindDirection.value = show
    }

    fun selectCity(position: Int) {
        if (position < 0 || position >= (_cities.value?.size ?: 0)) return
        _selectedCityIndex.value = position
        val city = _cities.value?.get(position) ?: return

        val cache = _weatherCache.value
        val cached = cache?.get(city.lowercase())
        if (cached != null) {
            _weatherData.value = cached
        } else {
            _weatherData.value = null
        }

        loadWeather(city, position)
    }

    fun addCity(cityName: String) {
        val trimmed = cityName.trim()
        if (trimmed.isEmpty()) return

        val currentList = _cities.value ?: mutableListOf()
        if (currentList.any { it.equals(trimmed, ignoreCase = true) }) {
            _errorMessage.value = app.getString(R.string.error_city_exists)
            return
        }

        currentList.add(trimmed)
        _cities.value = currentList
        _citiesUpdated.value = true

        selectCity(currentList.size - 1)
    }

    fun removeCity(position: Int) {
        val currentList = _cities.value ?: return
        if (currentList.size <= 1) {
            _errorMessage.value = app.getString(R.string.error_cannot_remove_last)
            return
        }
        if (position < 0 || position >= currentList.size) return

        val removedCity = currentList[position]
        _weatherCache.value?.remove(removedCity.lowercase())

        currentList.removeAt(position)
        _cities.value = currentList
        _citiesUpdated.value = true

        val selectedIndex = _selectedCityIndex.value ?: 0
        when {
            position == selectedIndex -> {
                val newIndex = if (selectedIndex >= currentList.size) currentList.size - 1 else selectedIndex
                selectCity(newIndex)
            }
            position < selectedIndex -> {
                _selectedCityIndex.value = selectedIndex - 1
            }
        }
    }

    fun loadWeather(city: String, position: Int = -1) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val lang = resolveLanguage()
                fetchAndStoreWeather(city, position, lang)
            } catch (e: Exception) {
                _errorMessage.value = mapExceptionToErrorMessage(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAllCitiesWeather() {
        val cityList = _cities.value?.toList() ?: return
        if (cityList.isEmpty()) {
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        clearBulkWeatherObservers()
        val lang = resolveLanguage()

        val requests = cityList.mapIndexed { index, city ->
            OneTimeWorkRequestBuilder<SequentialWeatherSyncWorker>()
                .setInputData(
                    workDataOf(
                        SequentialWeatherSyncWorker.KEY_CITY to city,
                        SequentialWeatherSyncWorker.KEY_CITY_INDEX to index,
                        SequentialWeatherSyncWorker.KEY_LANG to lang
                    )
                )
                .build()
        }

        if (requests.isEmpty()) {
            _isLoading.value = false
            return
        }

        expectedBulkWorkCount = requests.size
        requests.forEach { request ->
            observeBulkWeatherWork(request.id, cityList)
        }

        var continuation = workManager.beginUniqueWork(
            SequentialWeatherSyncWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            requests.first()
        )
        requests.drop(1).forEach { request ->
            continuation = continuation.then(request)
        }
        continuation.enqueue()
    }

    fun refreshCurrentCity() {
        val index = _selectedCityIndex.value ?: 0
        val city = _cities.value?.getOrNull(index) ?: return
        _isLoading.value = false
        loadWeather(city, index)
    }

    private fun resolveLanguage(): String {
        return when (Locale.getDefault().language) {
            "ru" -> "ru"
            "zh" -> "zh_cn"
            "ja" -> "ja"
            else -> "en"
        }
    }

    private suspend fun fetchAndStoreWeather(city: String, position: Int, lang: String) {
        val response = repository.getWeather(city, lang)
        if (response == null) {
            _errorMessage.value = app.getString(R.string.error_load_failed, city)
            return
        }

        val weatherData = WeatherData(
            city = response.name,
            temperature = response.main.temp,
            feelsLike = response.main.feels_like,
            description = response.weather.firstOrNull()?.description?.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            } ?: "N/A",
            humidity = response.main.humidity,
            pressure = response.main.pressure,
            windSpeed = response.wind.speed,
            windDegree = response.wind.deg,
            icon = response.weather.firstOrNull()?.icon ?: "",
            clouds = response.clouds.all
        )

        val cache = _weatherCache.value ?: mutableMapOf()
        cache[city.lowercase()] = weatherData
        _weatherCache.value = cache

        val currentCities = _cities.value ?: return
        val actualPosition = if (position >= 0) {
            position
        } else {
            currentCities.indexOfFirst { it.equals(city, ignoreCase = true) }
        }

        if (actualPosition == _selectedCityIndex.value) {
            _weatherData.value = weatherData
        }

        if (actualPosition >= 0) {
            _weatherUpdatedEvent.value = Pair(actualPosition, weatherData)
        }
    }

    private fun mapExceptionToErrorMessage(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("Unable to resolve host", ignoreCase = true) ||
                msg.contains("No address associated with hostname", ignoreCase = true) ->
                app.getString(R.string.error_no_internet)
            else -> app.getString(R.string.error_generic, msg)
        }
    }

    private fun observeBulkWeatherWork(workId: UUID, requestedCities: List<String>) {
        val observer = Observer<WorkInfo> { info ->
            when (info.state) {
                WorkInfo.State.SUCCEEDED -> {
                    applyBulkWeatherResult(info.outputData, requestedCities)
                    markBulkWorkFinished(workId)
                }
                WorkInfo.State.FAILED -> {
                    val requestedCity = info.outputData
                        .getString(SequentialWeatherSyncWorker.KEY_REQUESTED_CITY)
                        .orEmpty()
                    val error = info.outputData
                        .getString(SequentialWeatherSyncWorker.KEY_ERROR_MESSAGE)
                        .orEmpty()

                    _errorMessage.value = when {
                        requestedCity.isNotBlank() -> app.getString(R.string.error_load_failed, requestedCity)
                        error.isNotBlank() -> app.getString(R.string.error_generic, error)
                        else -> app.getString(R.string.error_generic, "WorkManager task failed")
                    }
                    markBulkWorkFinished(workId)
                }
                WorkInfo.State.CANCELLED -> {
                    markBulkWorkFinished(workId)
                }
                else -> Unit
            }
        }
        bulkWeatherObservers[workId] = observer
        workManager.getWorkInfoByIdLiveData(workId).observeForever(observer)
    }

    private fun applyBulkWeatherResult(outputData: androidx.work.Data, requestedCities: List<String>) {
        try {
            val error = outputData.getString(SequentialWeatherSyncWorker.KEY_ERROR_MESSAGE).orEmpty()
            if (error.isNotBlank()) {
                val failedCity = outputData.getString(SequentialWeatherSyncWorker.KEY_REQUESTED_CITY).orEmpty()
                if (failedCity.isNotBlank()) {
                    _errorMessage.value = app.getString(R.string.error_load_failed, failedCity)
                } else {
                    _errorMessage.value = app.getString(R.string.error_generic, error)
                }
                return
            }

            val resultJson = outputData
                .getString(SequentialWeatherSyncWorker.KEY_RESULT_JSON)
                .orEmpty()
            if (resultJson.isBlank()) return

            val item = JSONObject(resultJson)
            val requestedCity = item.optString("requestedCity")
                .ifBlank { outputData.getString(SequentialWeatherSyncWorker.KEY_REQUESTED_CITY).orEmpty() }
                .ifBlank {
                    requestedCities.getOrNull(
                        outputData.getInt(SequentialWeatherSyncWorker.KEY_CITY_INDEX, -1)
                    ).orEmpty()
                }
            if (requestedCity.isBlank()) return

            val weatherData = weatherDataFromJson(item)
            val cache = _weatherCache.value ?: mutableMapOf()
            cache[requestedCity.lowercase()] = weatherData
            if (weatherData.city.isNotBlank()) {
                cache[weatherData.city.lowercase()] = weatherData
            }
            _weatherCache.value = cache

            val positionFromData = outputData.getInt(SequentialWeatherSyncWorker.KEY_CITY_INDEX, -1)
            val actualPosition = if (positionFromData in requestedCities.indices) {
                positionFromData
            } else {
                requestedCities.indexOfFirst { it.equals(requestedCity, ignoreCase = true) }
            }

            if (actualPosition == _selectedCityIndex.value) {
                _weatherData.value = weatherData
            }
            if (actualPosition >= 0) {
                _weatherUpdatedEvent.value = Pair(actualPosition, weatherData)
            }
        } catch (e: Exception) {
            _errorMessage.value = mapExceptionToErrorMessage(e)
        }
    }

    private fun weatherDataFromJson(item: JSONObject): WeatherData {
        return WeatherData(
            city = item.optString("city"),
            temperature = item.optDouble("temperature", 0.0),
            feelsLike = item.optDouble("feelsLike", 0.0),
            description = item.optString("description", "N/A"),
            humidity = item.optInt("humidity", 0),
            pressure = item.optInt("pressure", 0),
            windSpeed = item.optDouble("windSpeed", 0.0),
            windDegree = item.optInt("windDegree", 0),
            icon = item.optString("icon"),
            clouds = item.optInt("clouds", 0)
        )
    }

    private fun markBulkWorkFinished(workId: UUID) {
        finishedBulkWorkIds.add(workId)
        if (finishedBulkWorkIds.size >= expectedBulkWorkCount) {
            _isLoading.value = false
            clearBulkWeatherObservers()
        }
    }

    private fun clearBulkWeatherObservers() {
        bulkWeatherObservers.forEach { (workId, observer) ->
            workManager.getWorkInfoByIdLiveData(workId).removeObserver(observer)
        }
        bulkWeatherObservers.clear()
        finishedBulkWorkIds.clear()
        expectedBulkWorkCount = 0
    }

    override fun onCleared() {
        clearBulkWeatherObservers()
        super.onCleared()
    }
}
