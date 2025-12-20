package com.example.weatherapp

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.weatherapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: WeatherViewModel by viewModels()

    private lateinit var cities: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cities = resources.getStringArray(R.array.cities)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setupCitySpinner()
        setupTemperatureRadioGroup()
        setupWindDirectionCheckBox()
        observeViewModel()

        binding.btnRefresh.setOnClickListener {
            val selectedCity = binding.spinnerCity.selectedItem.toString()
            viewModel.loadWeather(selectedCity)
        }
    }

    private fun setupCitySpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            cities
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCity.adapter = adapter

        binding.spinnerCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.loadWeather(cities[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
    }
}