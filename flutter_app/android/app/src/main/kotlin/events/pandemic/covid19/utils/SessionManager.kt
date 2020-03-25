package events.pandemic.covid19.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private var editor: SharedPreferences.Editor
    private var pref: SharedPreferences

    init {
        val PRIVATE_MODE = 0
        this.pref = context.getSharedPreferences("events.pandmic.covid19", PRIVATE_MODE)
        this.editor = pref.edit()
    }

    fun setStringData(key: String, value: String) {
        editor.putString(key, value)
        editor.commit()
    }

    fun getStringData(key: String): String? {
        return pref.getString(key, "")
    }

    fun setLongData(key: String, value: Long) {
        editor.putLong(key, value)
        editor.commit()
    }

    fun getLongData(key: String): Long? {
        return pref.getLong(key, 0)
    }
}