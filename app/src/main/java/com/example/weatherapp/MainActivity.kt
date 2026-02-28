package com.example.weatherapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: WeatherViewModel by viewModels()
    private lateinit var cityAdapter: CityAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private var isUserScrolling = false

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

        // Загружаем погоду для всех городов
        viewModel.loadAllCitiesWeather()
    }

    private fun setupRecyclerView() {
        val initialCities = viewModel.cities.value ?: mutableListOf()
        val items = initialCities.map { CityItem(it) }

        cityAdapter = CityAdapter(
            items = items,
            isCelsius = viewModel.isCelsius.value ?: true,
            showWindDirection = viewModel.showWindDirection.value ?: true,
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
            adapter = cityAdapter
            setHasFixedSize(false)
        }

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.recyclerCities)

        binding.recyclerCities.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isUserScrolling = true
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE && isUserScrolling) {
                    isUserScrolling = false
                    val snapView = snapHelper.findSnapView(this@MainActivity.layoutManager)
                    if (snapView != null) {
                        val snapPosition = this@MainActivity.layoutManager.getPosition(snapView)
                        if (snapPosition != RecyclerView.NO_POSITION) {
                            val currentSelected = viewModel.selectedCityIndex.value ?: -1
                            if (snapPosition != currentSelected) {
                                viewModel.selectCity(snapPosition)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun setupButtons() {
        binding.btnAddCity.setOnClickListener {
            AddCityDialog(this) { cityName ->
                viewModel.addCity(cityName)
            }.show()
        }

        binding.btnRefresh.setOnClickListener {
            viewModel.refreshCurrentCity()
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
            val items = cities.map { cityName ->
                CityItem(
                    cityName = cityName,
                    weatherData = cache[cityName.lowercase()]
                )
            }
            cityAdapter.updateItems(items)
        }

        viewModel.selectedCityIndex.observe(this) { index ->
            if (!isUserScrolling) {
                binding.recyclerCities.smoothScrollToPosition(index)
            }
        }

        viewModel.citiesUpdated.observe(this) { updated ->
            if (updated) {
                val cities = viewModel.cities.value ?: return@observe
                val cache = viewModel.weatherCache.value ?: mutableMapOf()
                val items = cities.map { cityName ->
                    CityItem(
                        cityName = cityName,
                        weatherData = cache[cityName.lowercase()]
                    )
                }
                cityAdapter.updateItems(items)
            }
        }

        viewModel.weatherUpdatedEvent.observe(this) { event ->
            event?.let { (position, weatherData) ->
                cityAdapter.updateWeatherForCity(position, weatherData)
            }
        }

        viewModel.isCelsius.observe(this) { celsius ->
            val windDir = viewModel.showWindDirection.value ?: true
            cityAdapter.updateSettings(celsius, windDir)
        }

        viewModel.showWindDirection.observe(this) { windDir ->
            val celsius = viewModel.isCelsius.value ?: true
            cityAdapter.updateSettings(celsius, windDir)
        }
    }
}