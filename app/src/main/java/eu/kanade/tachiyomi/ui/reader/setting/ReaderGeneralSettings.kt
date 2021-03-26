package eu.kanade.tachiyomi.ui.reader.setting

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.CompoundButton
import android.widget.Spinner
import androidx.annotation.ArrayRes
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.tfcporciuncula.flow.Preference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.asImmediateFlow
import eu.kanade.tachiyomi.databinding.ReaderGeneralSettingsBinding
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import eu.kanade.tachiyomi.widget.IgnoreFirstSpinnerListener
import kotlinx.coroutines.flow.launchIn
import uy.kohesive.injekt.injectLazy

/**
 * Sheet to show reader and viewer preferences.
 */
class ReaderGeneralSettings @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    NestedScrollView(context, attrs) {

    private val preferences: PreferencesHelper by injectLazy()

    private val binding = ReaderGeneralSettingsBinding.inflate(LayoutInflater.from(context), this, false)

    init {
        addView(binding.root)

        initGeneralPreferences()

        when ((context as ReaderActivity).viewer) {
            is PagerViewer -> initPagerPreferences()
            is WebtoonViewer -> initWebtoonPreferences()
        }
    }

    /**
     * Init general reader preferences.
     */
    private fun initGeneralPreferences() {
        binding.viewer.onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            (context as ReaderActivity).presenter.setMangaViewer(position)

            val mangaViewer = (context as ReaderActivity).presenter.getMangaViewer()
            if (mangaViewer == ReadingModeType.WEBTOON.prefValue || mangaViewer == ReadingModeType.CONTINUOUS_VERTICAL.prefValue) {
                initWebtoonPreferences()
            } else {
                initPagerPreferences()
            }
        }
        binding.viewer.setSelection((context as ReaderActivity).presenter.manga?.viewer ?: 0, false)

        binding.rotationMode.bindToPreference(preferences.rotation(), 1)
        binding.backgroundColor.bindToIntPreference(preferences.readerTheme(), R.array.reader_themes_values)
        binding.showPageNumber.bindToPreference(preferences.showPageNumber())
        binding.fullscreen.bindToPreference(preferences.fullscreen())
        binding.keepscreen.bindToPreference(preferences.keepScreenOn())
        binding.longTap.bindToPreference(preferences.readWithLongTap())
        binding.alwaysShowChapterTransition.bindToPreference(preferences.alwaysShowChapterTransition())
        binding.pageTransitions.bindToPreference(preferences.pageTransitions())

        // If the preference is explicitly disabled, that means the setting was configured since there is a cutout
        if ((context as ReaderActivity).hasCutout || !preferences.cutoutShort().get()) {
            binding.cutoutShort.isVisible = true
            binding.cutoutShort.bindToPreference(preferences.cutoutShort())
        }
    }

    /**
     * Init the preferences for the pager reader.
     */
    private fun initPagerPreferences() {
        binding.webtoonPrefsGroup.root.isVisible = false
        binding.pagerPrefsGroup.root.isVisible = true

        binding.pagerPrefsGroup.tappingPrefsGroup.isVisible = preferences.readWithTapping().get()

        binding.pagerPrefsGroup.tappingInverted.bindToPreference(preferences.pagerNavInverted())

        binding.pagerPrefsGroup.pagerNav.bindToPreference(preferences.navigationModePager())
        binding.pagerPrefsGroup.scaleType.bindToPreference(preferences.imageScaleType(), 1)
        binding.pagerPrefsGroup.zoomStart.bindToPreference(preferences.zoomStart(), 1)
        binding.pagerPrefsGroup.cropBorders.bindToPreference(preferences.cropBorders())

        // Makes so that dual page invert gets hidden away when turning of dual page split
        binding.dualPageSplit.bindToPreference(preferences.dualPageSplitPaged())
        preferences.dualPageSplitPaged()
            .asImmediateFlow { binding.dualPageInvert.isVisible = it }
            .launchIn((context as ReaderActivity).lifecycleScope)
        binding.dualPageInvert.bindToPreference(preferences.dualPageInvertPaged())
    }

    /**
     * Init the preferences for the webtoon reader.
     */
    private fun initWebtoonPreferences() {
        binding.pagerPrefsGroup.root.isVisible = false
        binding.webtoonPrefsGroup.root.isVisible = true

        binding.webtoonPrefsGroup.tappingPrefsGroup.isVisible = preferences.readWithTapping().get()

        binding.webtoonPrefsGroup.tappingInverted.bindToPreference(preferences.webtoonNavInverted())

        binding.webtoonPrefsGroup.webtoonNav.bindToPreference(preferences.navigationModeWebtoon())
        binding.webtoonPrefsGroup.cropBordersWebtoon.bindToPreference(preferences.cropBordersWebtoon())
        binding.webtoonPrefsGroup.webtoonSidePadding.bindToIntPreference(preferences.webtoonSidePadding(), R.array.webtoon_side_padding_values)

        // Makes so that dual page invert gets hidden away when turning of dual page split
        binding.dualPageSplit.bindToPreference(preferences.dualPageSplitWebtoon())
        preferences.dualPageSplitWebtoon()
            .asImmediateFlow { binding.dualPageInvert.isVisible = it }
            .launchIn((context as ReaderActivity).lifecycleScope)
        binding.dualPageInvert.bindToPreference(preferences.dualPageInvertWebtoon())
    }

    /**
     * Binds a checkbox or switch view with a boolean preference.
     */
    private fun CompoundButton.bindToPreference(pref: Preference<Boolean>) {
        isChecked = pref.get()
        setOnCheckedChangeListener { _, isChecked -> pref.set(isChecked) }
    }

    /**
     * Binds a spinner to an int preference with an optional offset for the value.
     */
    private fun Spinner.bindToPreference(pref: Preference<Int>, offset: Int = 0) {
        onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            pref.set(position + offset)
        }
        setSelection(pref.get() - offset, false)
    }

    /**
     * Binds a spinner to an enum preference.
     */
    private inline fun <reified T : Enum<T>> Spinner.bindToPreference(pref: Preference<T>) {
        val enumConstants = T::class.java.enumConstants

        onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            enumConstants?.get(position)?.let { pref.set(it) }
        }

        enumConstants?.indexOf(pref.get())?.let { setSelection(it, false) }
    }

    /**
     * Binds a spinner to an int preference. The position of the spinner item must
     * correlate with the [intValues] resource item (in arrays.xml), which is a <string-array>
     * of int values that will be parsed here and applied to the preference.
     */
    private fun Spinner.bindToIntPreference(pref: Preference<Int>, @ArrayRes intValuesResource: Int) {
        val intValues = resources.getStringArray(intValuesResource).map { it.toIntOrNull() }
        onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
            pref.set(intValues[position]!!)
        }
        setSelection(intValues.indexOf(pref.get()), false)
    }
}
