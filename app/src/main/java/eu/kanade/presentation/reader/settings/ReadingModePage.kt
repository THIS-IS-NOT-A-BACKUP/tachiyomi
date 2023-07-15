package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import eu.kanade.domain.manga.model.orientationType
import eu.kanade.domain.manga.model.readingModeType
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.setting.OrientationType
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.RadioItem
import tachiyomi.presentation.core.components.SliderItem
import java.text.NumberFormat

@Composable
internal fun ColumnScope.ReadingModePage(screenModel: ReaderSettingsScreenModel) {
    HeadingItem("This is still a WIP, the UI will be improved soon")

    HeadingItem(R.string.pref_category_for_this_series)

    val manga by screenModel.mangaFlow.collectAsState()
    val readingMode = remember(manga) { ReadingModeType.fromPreference(manga?.readingModeType?.toInt()) }
    val orientation = remember(manga) { OrientationType.fromPreference(manga?.orientationType?.toInt()) }

    HeadingItem(R.string.pref_category_reading_mode)
    ReadingModeType.values().map {
        RadioItem(
            label = stringResource(it.stringRes),
            selected = readingMode == it,
            onClick = { screenModel.onChangeReadingMode(it) },
        )
    }

    HeadingItem(R.string.rotation_type)
    OrientationType.values().map {
        RadioItem(
            label = stringResource(it.stringRes),
            selected = orientation == it,
            onClick = { screenModel.onChangeOrientation(it) },
        )
    }

    val viewer by screenModel.viewerFlow.collectAsState()
    if (viewer is WebtoonViewer) {
        WebtoonViewerSettings(screenModel)
    } else {
        PagerViewerSettings(screenModel)
    }
}

@Composable
private fun ColumnScope.PagerViewerSettings(screenModel: ReaderSettingsScreenModel) {
    HeadingItem(R.string.pager_viewer)

    val navigationModePager by screenModel.preferences.navigationModePager().collectAsState()
    HeadingItem(R.string.pref_viewer_nav)
    ReaderPreferences.TapZones.mapIndexed { index, titleResId ->
        RadioItem(
            label = stringResource(titleResId),
            selected = navigationModePager == index,
            onClick = { screenModel.preferences.navigationModePager().set(index) },
        )
    }

    if (navigationModePager != 5) {
        val pagerNavInverted by screenModel.preferences.pagerNavInverted().collectAsState()
        HeadingItem(R.string.pref_read_with_tapping_inverted)
        ReaderPreferences.TappingInvertMode.values().map {
            RadioItem(
                label = stringResource(it.titleResId),
                selected = pagerNavInverted == it,
                onClick = { screenModel.preferences.pagerNavInverted().set(it) },
            )
        }
    }

    val imageScaleType by screenModel.preferences.imageScaleType().collectAsState()
    HeadingItem(R.string.pref_image_scale_type)
    ReaderPreferences.ImageScaleType.mapIndexed { index, it ->
        RadioItem(
            label = stringResource(it),
            selected = imageScaleType == index + 1,
            onClick = { screenModel.preferences.imageScaleType().set(index + 1) },
        )
    }

    val zoomStart by screenModel.preferences.zoomStart().collectAsState()
    HeadingItem(R.string.pref_zoom_start)
    ReaderPreferences.ZoomStart.mapIndexed { index, it ->
        RadioItem(
            label = stringResource(it),
            selected = zoomStart == index + 1,
            onClick = { screenModel.preferences.zoomStart().set(index + 1) },
        )
    }

    val cropBorders by screenModel.preferences.cropBorders().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.pref_crop_borders),
        checked = cropBorders,
        onClick = {
            screenModel.togglePreference(ReaderPreferences::cropBorders)
        },
    )

    val landscapeZoom by screenModel.preferences.landscapeZoom().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.pref_landscape_zoom),
        checked = landscapeZoom,
        onClick = {
            screenModel.togglePreference(ReaderPreferences::landscapeZoom)
        },
    )

    val navigateToPan by screenModel.preferences.navigateToPan().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.pref_navigate_pan),
        checked = navigateToPan,
        onClick = {
            screenModel.togglePreference(ReaderPreferences::navigateToPan)
        },
    )

    val dualPageSplitPaged by screenModel.preferences.dualPageSplitPaged().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.pref_dual_page_split),
        checked = dualPageSplitPaged,
        onClick = {
            screenModel.togglePreference(ReaderPreferences::dualPageSplitPaged)
        },
    )

    if (dualPageSplitPaged) {
        val dualPageInvertPaged by screenModel.preferences.dualPageInvertPaged().collectAsState()
        CheckboxItem(
            label = stringResource(R.string.pref_dual_page_invert),
            checked = dualPageInvertPaged,
            onClick = {
                screenModel.togglePreference(ReaderPreferences::dualPageInvertPaged)
            },
        )
    }

    val dualPageRotateToFit by screenModel.preferences.dualPageRotateToFit().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.pref_page_rotate),
        checked = dualPageRotateToFit,
        onClick = {
            screenModel.togglePreference(ReaderPreferences::dualPageRotateToFit)
        },
    )

    if (dualPageRotateToFit) {
        val dualPageRotateToFitInvert by screenModel.preferences.dualPageRotateToFitInvert().collectAsState()
        CheckboxItem(
            label = stringResource(R.string.pref_page_rotate_invert),
            checked = dualPageRotateToFitInvert,
            onClick = {
                screenModel.togglePreference(ReaderPreferences::dualPageRotateToFitInvert)
            },
        )
    }
}

@Composable
private fun ColumnScope.WebtoonViewerSettings(screenModel: ReaderSettingsScreenModel) {
    val numberFormat = remember { NumberFormat.getPercentInstance() }

    HeadingItem(R.string.webtoon_viewer)

    val navigationModeWebtoon by screenModel.preferences.navigationModeWebtoon().collectAsState()
    HeadingItem(R.string.pref_viewer_nav)
    ReaderPreferences.TapZones.mapIndexed { index, titleResId ->
        RadioItem(
            label = stringResource(titleResId),
            selected = navigationModeWebtoon == index,
            onClick = { screenModel.preferences.navigationModeWebtoon().set(index) },
        )
    }

    if (navigationModeWebtoon != 5) {
        val webtoonNavInverted by screenModel.preferences.webtoonNavInverted().collectAsState()
        HeadingItem(R.string.pref_read_with_tapping_inverted)
        ReaderPreferences.TappingInvertMode.values().map {
            RadioItem(
                label = stringResource(it.titleResId),
                selected = webtoonNavInverted == it,
                onClick = { screenModel.preferences.webtoonNavInverted().set(it) },
            )
        }
    }

    val webtoonSidePadding by screenModel.preferences.webtoonSidePadding().collectAsState()
    SliderItem(
        label = stringResource(R.string.pref_webtoon_side_padding),
        min = ReaderPreferences.WEBTOON_PADDING_MIN,
        max = ReaderPreferences.WEBTOON_PADDING_MAX,
        value = webtoonSidePadding,
        valueText = numberFormat.format(webtoonSidePadding / 100f),
        onChange = {
            screenModel.preferences.webtoonSidePadding().set(it)
        },
    )

    val cropBordersWebtoon by screenModel.preferences.cropBordersWebtoon().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.pref_crop_borders),
        checked = cropBordersWebtoon,
        onClick = {
            screenModel.togglePreference(ReaderPreferences::cropBordersWebtoon)
        },
    )

    val dualPageSplitWebtoon by screenModel.preferences.dualPageSplitWebtoon().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.pref_dual_page_split),
        checked = dualPageSplitWebtoon,
        onClick = {
            screenModel.togglePreference(ReaderPreferences::dualPageSplitWebtoon)
        },
    )

    if (dualPageSplitWebtoon) {
        val dualPageInvertWebtoon by screenModel.preferences.dualPageInvertWebtoon()
            .collectAsState()
        CheckboxItem(
            label = stringResource(R.string.pref_dual_page_invert),
            checked = dualPageInvertWebtoon,
            onClick = {
                screenModel.togglePreference(ReaderPreferences::dualPageInvertWebtoon)
            },
        )
    }

    if (!isReleaseBuildType) {
        val longStripSplitWebtoon by screenModel.preferences.longStripSplitWebtoon()
            .collectAsState()
        CheckboxItem(
            label = stringResource(R.string.pref_long_strip_split),
            checked = longStripSplitWebtoon,
            onClick = {
                screenModel.togglePreference(ReaderPreferences::longStripSplitWebtoon)
            },
        )
    }

    val webtoonDoubleTapZoomEnabled by screenModel.preferences.webtoonDoubleTapZoomEnabled().collectAsState()
    CheckboxItem(
        label = stringResource(R.string.pref_double_tap_zoom),
        checked = webtoonDoubleTapZoomEnabled,
        onClick = {
            screenModel.togglePreference(ReaderPreferences::webtoonDoubleTapZoomEnabled)
        },
    )
}
