package com.example.weatherapp

import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CityChipAdapter(
    private var items: List<CityItem> = emptyList(),
    private var selectedIndex: Int = 0,
    private val onCityClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<CityChipAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chipContainer: LinearLayout = itemView.findViewById(R.id.chipContainer)
        val tvCityName: TextView = itemView.findViewById(R.id.tvCityName)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteCity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_city_chip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val displayName = item.weatherData?.city ?: item.cityName
        holder.tvCityName.text = displayName

        val isSelected = position == selectedIndex
        holder.chipContainer.setBackgroundResource(
            if (isSelected) R.drawable.bg_city_item_selected
            else R.drawable.bg_city_item
        )
        holder.tvCityName.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (isSelected) android.R.color.white else R.color.text_primary
            )
        )

        holder.itemView.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onCityClick(pos)
            }
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

    fun setSelectedIndex(index: Int) {
        val oldIndex = selectedIndex
        selectedIndex = index
        if (oldIndex in items.indices) notifyItemChanged(oldIndex)
        if (index in items.indices) notifyItemChanged(index)
    }
}
