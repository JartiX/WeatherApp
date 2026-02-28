package com.example.weatherapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class CityItem(
    val cityName: String,
    val weatherData: WeatherData? = null
)

class CityAdapter(
    private var items: List<CityItem> = emptyList(),
    private var isCelsius: Boolean = true,
    private var showWindDirection: Boolean = true,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<CityAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCityName: TextView = itemView.findViewById(R.id.tvCityName)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val ivWeatherIcon: ImageView = itemView.findViewById(R.id.ivWeatherIcon)
        val tvTemperature: TextView = itemView.findViewById(R.id.tvTemperature)
        val tvFeelsLike: TextView = itemView.findViewById(R.id.tvFeelsLike)
        val tvHumidity: TextView = itemView.findViewById(R.id.tvHumidity)
        val tvPressure: TextView = itemView.findViewById(R.id.tvPressure)
        val tvWindSpeed: TextView = itemView.findViewById(R.id.tvWindSpeed)
        val tvWindDirection: TextView = itemView.findViewById(R.id.tvWindDirection)
        val layoutWindDirection: LinearLayout = itemView.findViewById(R.id.layoutWindDirection)
        val tvClouds: TextView = itemView.findViewById(R.id.tvClouds)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteCity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_city, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val weather = item.weatherData

        holder.tvCityName.text = weather?.city ?: item.cityName

        if (weather != null) {
            holder.tvDescription.text = weather.descriptionCapitalized
            holder.tvDescription.visibility = View.VISIBLE

            holder.tvTemperature.text = if (isCelsius) weather.temperatureCelsius else weather.temperatureFahrenheit
            holder.tvFeelsLike.text = if (isCelsius)
                holder.itemView.context.getString(R.string.feels_like_format, weather.feelsLikeCelsius)
            else
                holder.itemView.context.getString(R.string.feels_like_format, weather.feelsLikeFahrenheit)

            holder.tvHumidity.text = weather.humidityPercent
            holder.tvPressure.text = weather.pressureHpa
            holder.tvWindSpeed.text = weather.windSpeedMs
            holder.tvWindDirection.text = weather.windDirection
            holder.tvClouds.text = weather.cloudsPercent

            val iconRes = getWeatherIconResource(weather.icon)
            holder.ivWeatherIcon.setImageResource(iconRes)

            holder.layoutWindDirection.visibility = if (showWindDirection) View.VISIBLE else View.GONE
        } else {
            holder.tvDescription.text = "Загрузка..."
            holder.tvTemperature.text = "--°"
            holder.tvFeelsLike.text = ""
            holder.tvHumidity.text = "--%"
            holder.tvPressure.text = "-- hPa"
            holder.tvWindSpeed.text = "-- м/с"
            holder.tvWindDirection.text = "--"
            holder.tvClouds.text = "--%"
            holder.ivWeatherIcon.setImageResource(R.drawable.ic_cloudy)
            holder.layoutWindDirection.visibility = if (showWindDirection) View.VISIBLE else View.GONE
        }

        holder.btnDelete.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onDeleteClick(pos)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<CityItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateSettings(celsius: Boolean, windDirection: Boolean) {
        isCelsius = celsius
        showWindDirection = windDirection
        notifyDataSetChanged()
    }

    fun updateWeatherForCity(position: Int, weatherData: WeatherData) {
        if (position in items.indices) {
            val mutableItems = items.toMutableList()
            mutableItems[position] = mutableItems[position].copy(weatherData = weatherData)
            items = mutableItems
            notifyItemChanged(position)
        }
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