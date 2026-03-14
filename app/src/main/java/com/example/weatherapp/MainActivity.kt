package com.example.weatherapp

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: WeatherViewModel by viewModels()
    private lateinit var cityChipAdapter: CityChipAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setupRecyclerView()
        setupButtons()
        setupTemperatureRadioGroup()
        setupWindDirectionCheckBox()
        observeViewModel()

        viewModel.loadAllCitiesWeather()
    }

    private fun switchLanguage(languageCode: String) {
        if (LocaleHelper.getSavedLanguage(this) == languageCode) return
        LocaleHelper.setLanguage(this, languageCode)
        recreate()
    }

    private fun setupRecyclerView() {
        val cities = viewModel.cities.value ?: mutableListOf()
        val cache = viewModel.weatherCache.value ?: mutableMapOf()
        val initialItems = cities.map { CityItem(it, cache[it.lowercase()]) }

        cityChipAdapter = CityChipAdapter(
            items = initialItems,
            selectedIndex = viewModel.selectedCityIndex.value ?: 0,
            onCityClick = { position ->
                viewModel.selectCity(position)
            },
            onDeleteClick = { position ->
                viewModel.removeCity(position)
            }
        )

        layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        binding.recyclerCities.apply {
            this.layoutManager = this@MainActivity.layoutManager
            adapter = cityChipAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupButtons() {
        binding.etSearchCity.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val cityName = binding.etSearchCity.text?.toString()?.trim()
                if (!cityName.isNullOrEmpty()) {
                    viewModel.addCity(cityName)
                    binding.etSearchCity.text?.clear()
                }
                true
            } else false
        }

        binding.btnRefresh.setOnClickListener {
            viewModel.refreshCurrentCity()
        }

        binding.btnMenu.setOnClickListener { v ->
            PopupMenu(this, v).apply {
                menuInflater.inflate(R.menu.main_menu, menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_language_english -> { switchLanguage("en"); true }
                        R.id.menu_language_russian -> { switchLanguage("ru"); true }
                        R.id.menu_language_chinese -> { switchLanguage("zh"); true }
                        R.id.menu_language_japanese -> { switchLanguage("ja"); true }
                        else -> false
                    }
                }
                show()
            }
        }
    }

    private fun setupTemperatureRadioGroup() {
        binding.radioGroupTemperature.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioCelsius -> viewModel.setTemperatureUnit(true)
                R.id.radioFahrenheit -> viewModel.setTemperatureUnit(false)
            }
        }
    }

    private fun setupWindDirectionCheckBox() {
        binding.checkboxWindDirection.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowWindDirection(isChecked)
        }
    }

    private fun observeViewModel() {
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.cities.observe(this) { cities ->
            val cache = viewModel.weatherCache.value ?: mutableMapOf()
            cityChipAdapter.updateItems(cities.map { CityItem(it, cache[it.lowercase()]) })
        }

        viewModel.selectedCityIndex.observe(this) { index ->
            cityChipAdapter.setSelectedIndex(index)
            binding.recyclerCities.smoothScrollToPosition(index)
        }

        viewModel.citiesUpdated.observe(this) { updated ->
            if (updated) {
                val cities = viewModel.cities.value ?: return@observe
                val cache = viewModel.weatherCache.value ?: mutableMapOf()
                cityChipAdapter.updateItems(cities.map { CityItem(it, cache[it.lowercase()]) })
            }
        }

        viewModel.weatherUpdatedEvent.observe(this) { event ->
            event?.let { (position, weatherData) ->
                val cities = viewModel.cities.value ?: return@observe
                val cache = viewModel.weatherCache.value?.toMutableMap() ?: mutableMapOf()
                cache[weatherData.city.lowercase()] = weatherData
                val cityName = cities.getOrNull(position) ?: weatherData.city
                cache[cityName.lowercase()] = weatherData
                val items = cities.map { CityItem(it, cache[it.lowercase()]) }
                cityChipAdapter.updateItems(items)
            }
        }

        viewModel.weatherData.observe(this) { weather ->
            updateWeatherCard(weather)
        }

        viewModel.isCelsius.observe(this) { _ ->
            updateWeatherCard(viewModel.weatherData.value)
        }

        viewModel.showWindDirection.observe(this) { _ ->
            updateWeatherCard(viewModel.weatherData.value)
        }
    }

    private fun updateWeatherCard(weather: WeatherData?) {
        val root = binding.root.findViewById<View>(R.id.includeWeatherDetail)
        val tvCity = root.findViewById<TextView>(R.id.tvWeatherCityName)
        val tvDescription = root.findViewById<TextView>(R.id.tvWeatherDescription)
        val tvTemperature = root.findViewById<TextView>(R.id.tvWeatherTemperature)
        val tvFeelsLike = root.findViewById<TextView>(R.id.tvWeatherFeelsLike)
        val tvHumidity = root.findViewById<TextView>(R.id.tvWeatherHumidity)
        val tvPressure = root.findViewById<TextView>(R.id.tvWeatherPressure)
        val tvWindSpeed = root.findViewById<TextView>(R.id.tvWeatherWindSpeed)
        val tvWindDirection = root.findViewById<TextView>(R.id.tvWeatherWindDirection)
        val layoutWindDirection = root.findViewById<LinearLayout>(R.id.layoutWeatherWindDirection)
        val tvClouds = root.findViewById<TextView>(R.id.tvWeatherClouds)
        val ivWeatherIcon = root.findViewById<ImageView>(R.id.ivWeatherIcon)

        val isCelsius = viewModel.isCelsius.value ?: true
        val showWindDirection = viewModel.showWindDirection.value ?: true

        if (weather != null) {
            root.visibility = View.VISIBLE
            tvCity.text = weather.city
            tvDescription.text = weather.descriptionCapitalized
            tvDescription.visibility = View.VISIBLE
            tvTemperature.text = if (isCelsius) weather.temperatureCelsius else weather.temperatureFahrenheit
            tvFeelsLike.text = getString(R.string.feels_like_format,
                if (isCelsius) weather.feelsLikeCelsius else weather.feelsLikeFahrenheit)
            tvHumidity.text = weather.humidityPercent
            tvPressure.text = weather.pressureHpa
            tvWindSpeed.text = getString(R.string.wind_speed_format, weather.windSpeed)
            tvWindDirection.text = getWindDirectionString(weather.windDegree)
            tvClouds.text = weather.cloudsPercent
            ivWeatherIcon.setImageResource(getWeatherIconResource(weather.icon))
            layoutWindDirection.visibility = if (showWindDirection) View.VISIBLE else View.GONE
        } else {
            root.visibility = View.VISIBLE
            val cityName = viewModel.cities.value?.getOrNull(viewModel.selectedCityIndex.value ?: 0) ?: ""
            tvCity.text = cityName
            tvDescription.text = getString(R.string.loading)
            tvDescription.visibility = View.VISIBLE
            tvTemperature.text = "--°"
            tvFeelsLike.text = ""
            tvHumidity.text = "--%"
            tvPressure.text = getString(R.string.loading_pressure)
            tvWindSpeed.text = getString(R.string.loading_wind_speed)
            tvWindDirection.text = "--"
            tvClouds.text = "--%"
            ivWeatherIcon.setImageResource(R.drawable.ic_cloudy)
            layoutWindDirection.visibility = if (showWindDirection) View.VISIBLE else View.GONE
        }
    }

    private fun getWindDirectionString(degree: Int): String {
        val index = when (degree) {
            in 0..22 -> 0
            in 23..67 -> 1
            in 68..112 -> 2
            in 113..157 -> 3
            in 158..202 -> 4
            in 203..247 -> 5
            in 248..292 -> 6
            in 293..337 -> 7
            in 338..360 -> 0
            else -> -1
        }
        return if (index >= 0) resources.getStringArray(R.array.wind_directions)[index]
        else "N/A"
    }

    private fun getWeatherIconResource(iconCode: String): Int {
        return when {
            iconCode.startsWith("01d") -> R.drawable.ic_clear_day
            iconCode.startsWith("01n") -> R.drawable.ic_clear_night
            iconCode.startsWith("02d") -> R.drawable.ic_partly_cloudy_day
            iconCode.startsWith("02n") -> R.drawable.ic_partly_cloudy_night
            iconCode.startsWith("03") -> R.drawable.ic_cloudy
            iconCode.startsWith("04") -> R.drawable.ic_cloudy
            iconCode.startsWith("09") -> R.drawable.ic_rain
            iconCode.startsWith("10") -> R.drawable.ic_rain
            iconCode.startsWith("11") -> R.drawable.ic_thunderstorm
            iconCode.startsWith("13") -> R.drawable.ic_snow
            iconCode.startsWith("50") -> R.drawable.ic_mist
            else -> R.drawable.ic_cloudy
        }
    }
}
