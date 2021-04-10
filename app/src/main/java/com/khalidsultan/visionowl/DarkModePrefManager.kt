package com.khalidsultan.visionowl

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor

class DarkModePrefManager(var _context: Context) {
    var pref: SharedPreferences
    var editor: Editor

    // shared pref mode
    var PRIVATE_MODE = 0
    fun setDarkMode(isFirstTime: Boolean) {
        editor.putBoolean(IS_NIGHT_MODE, isFirstTime)
        editor.commit()
    }

    val isNightMode: Boolean
        get() = pref.getBoolean(IS_NIGHT_MODE, true)

    companion object {
        // Shared preferences file name
        private const val PREF_NAME = "education-dark-mode"
        private const val IS_NIGHT_MODE = "IsNightMode"
    }

    init {
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        editor = pref.edit()
    }
}