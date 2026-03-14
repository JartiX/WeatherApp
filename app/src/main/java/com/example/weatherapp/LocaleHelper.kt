package com.example.weatherapp

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LANGUAGE = "language"

    fun applyLocale(context: Context) {
        val language = getSavedLanguage(context) ?: return
        val locale = getLocaleForLanguage(language)
        Locale.setDefault(locale)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))
    }

    fun setLanguage(context: Context, languageCode: String) {
        saveLanguage(context, languageCode)
        val locale = getLocaleForLanguage(languageCode)
        Locale.setDefault(locale)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))
    }

    private fun getLocaleForLanguage(languageCode: String): Locale {
        return when (languageCode) {
            "zh" -> Locale("zh", "CN")
            "ja" -> Locale.JAPANESE
            "ru" -> Locale("ru")
            else -> Locale(languageCode)
        }
    }

    fun saveLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    fun getSavedLanguage(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
    }

    fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        @Suppress("DEPRECATION")
        config.locale = locale
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
