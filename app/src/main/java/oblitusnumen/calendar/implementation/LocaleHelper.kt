package oblitusnumen.calendar.implementation

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import oblitusnumen.calendar.implementation.data.DbManager
import java.util.Locale

object LocaleHelper {
    const val LANGUAGE_TAG_PREF = "app_language_tag"

    fun getTag(context: Context): String =
        DbManager.getSharedPrefs(context).getString(LANGUAGE_TAG_PREF, "") ?: ""

    fun setTag(context: Context, tag: String) {
        DbManager.getSharedPrefs(context).edit { putString(LANGUAGE_TAG_PREF, tag) }
    }

    fun wrap(context: Context): Context {
        val tag = getTag(context)
        if (tag.isEmpty()) return context
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(android.os.LocaleList(locale))
        return context.createConfigurationContext(config)
    }
}
