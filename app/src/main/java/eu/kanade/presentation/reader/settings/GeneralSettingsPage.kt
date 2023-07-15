package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.components.material.SegmentedButtons

private val themes = listOf(
    R.string.black_background to 1,
    R.string.gray_background to 2,
    R.string.white_background to 0,
    R.string.automatic_background to 3,
)

@Composable
internal fun ColumnScope.GeneralPage(screenModel: ReaderSettingsScreenModel) {
    HeadingItem(R.string.pref_reader_theme)
    val readerTheme by screenModel.preferences.readerTheme().collectAsState()
    SegmentedButtons(
        modifier = Modifier.padding(
            start = SettingsItemsPaddings.Horizontal,
            top = 0.dp,
            end = SettingsItemsPaddings.Horizontal,
            bottom = SettingsItemsPaddings.Vertical,
        ),
        entries = themes.map { stringResource(it.first) },
        selectedIndex = themes.indexOfFirst { readerTheme == it.second },
        onClick = { screenModel.preferences.readerTheme().set(themes[it].second) },
    )

    val showPageNumber by screenModel.preferences.showPageNumber().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.pref_show_page_number),
        checked = showPageNumber,
        onClick = {
            screenModel.togglePreference(ReaderPreferences::showPageNumber)
        },
    )

    val fullscreen by screenModel.preferences.fullscreen().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.pref_fullscreen),
        checked = fullscreen,
        onClick = {
            screenModel.togglePreference(ReaderPreferences::fullscreen)
        },
    )

    // TODO: hide if there's no cutout
    val cutoutShort by screenModel.preferences.cutoutShort().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.pref_cutout_short),
        checked = cutoutShort,
        onClick = {
            screenModel.togglePreference(ReaderPreferences::cutoutShort)
        },
    )

    val keepScreenOn by screenModel.preferences.keepScreenOn().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.pref_keep_screen_on),
        checked = keepScreenOn,
        onClick = {
            screenModel.togglePreference(ReaderPreferences::keepScreenOn)
        },
    )

    val readWithLongTap by screenModel.preferences.readWithLongTap().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.pref_read_with_long_tap),
        checked = readWithLongTap,
        onClick = {
            screenModel.togglePreference(ReaderPreferences::readWithLongTap)
        },
    )

    val alwaysShowChapterTransition by screenModel.preferences.alwaysShowChapterTransition().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.pref_always_show_chapter_transition),
        checked = alwaysShowChapterTransition,
        onClick = {
            screenModel.togglePreference(ReaderPreferences::alwaysShowChapterTransition)
        },
    )

    val pageTransitions by screenModel.preferences.pageTransitions().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.pref_page_transitions),
        checked = pageTransitions,
        onClick = {
            screenModel.togglePreference(ReaderPreferences::pageTransitions)
        },
    )
}
