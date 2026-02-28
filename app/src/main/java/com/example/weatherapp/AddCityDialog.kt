package com.example.weatherapp

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddCityDialog(
    private val context: Context,
    private val onCityAdded: (String) -> Unit
) {
    fun show() {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_add_city, null)
        val editText = dialogView.findViewById<EditText>(R.id.etCityName)

        MaterialAlertDialogBuilder(context)
            .setTitle("Добавить город")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val cityName = editText.text.toString()
                if (cityName.isNotBlank()) {
                    onCityAdded(cityName.trim())
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}