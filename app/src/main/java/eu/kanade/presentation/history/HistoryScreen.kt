package eu.kanade.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.buildSpannedString
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.MangaCover
import eu.kanade.presentation.components.MangaCoverAspect
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.presentation.util.horizontalPadding
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.recent.history.HistoryPresenter
import eu.kanade.tachiyomi.ui.recent.history.UiModel
import eu.kanade.tachiyomi.util.lang.toRelativeString
import eu.kanade.tachiyomi.util.lang.toTimestampString
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

val chapterFormatter = DecimalFormat(
    "#.###",
    DecimalFormatSymbols()
        .apply { decimalSeparator = '.' },
)

@Composable
fun HistoryScreen(
    composeView: ComposeView,
    presenter: HistoryPresenter,
    onClickItem: (MangaChapterHistory) -> Unit,
    onClickResume: (MangaChapterHistory) -> Unit,
    onClickDelete: (MangaChapterHistory, Boolean) -> Unit,
) {
    val nestedSrollInterop = rememberNestedScrollInteropConnection(composeView)
    TachiyomiTheme {
        val state by presenter.state.collectAsState()
        val history = state.list?.collectAsLazyPagingItems()
        when {
            history == null -> {
                CircularProgressIndicator()
            }
            history.itemCount == 0 -> {
                EmptyScreen(
                    textResource = R.string.information_no_recent_manga
                )
            }
            else -> {
                HistoryContent(
                    nestedScroll = nestedSrollInterop,
                    history = history,
                    onClickItem = onClickItem,
                    onClickResume = onClickResume,
                    onClickDelete = onClickDelete,
                )
            }
        }
    }
}

@Composable
fun HistoryContent(
    history: LazyPagingItems<UiModel>,
    onClickItem: (MangaChapterHistory) -> Unit,
    onClickResume: (MangaChapterHistory) -> Unit,
    onClickDelete: (MangaChapterHistory, Boolean) -> Unit,
    preferences: PreferencesHelper = Injekt.get(),
    nestedScroll: NestedScrollConnection
) {
    val relativeTime: Int = remember { preferences.relativeTime().get() }
    val dateFormat: DateFormat = remember { preferences.dateFormat() }

    val (removeState, setRemoveState) = remember { mutableStateOf<MangaChapterHistory?>(null) }

    val scrollState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier
            .nestedScroll(nestedScroll),
        state = scrollState,
    ) {
        items(history) { item ->
            when (item) {
                is UiModel.Header -> {
                    HistoryHeader(
                        modifier = Modifier
                            .animateItemPlacement(),
                        date = item.date,
                        relativeTime = relativeTime,
                        dateFormat = dateFormat
                    )
                }
                is UiModel.History -> {
                    val value = item.item
                    HistoryItem(
                        modifier = Modifier.animateItemPlacement(),
                        history = value,
                        onClickItem = { onClickItem(value) },
                        onClickResume = { onClickResume(value) },
                        onClickDelete = { setRemoveState(value) },
                    )
                }
                null -> {}
            }
        }
        item {
            Spacer(
                modifier = Modifier
                    .navigationBarsPadding()
            )
        }
    }

    if (removeState != null) {
        RemoveHistoryDialog(
            onPositive = { all ->
                onClickDelete(removeState, all)
                setRemoveState(null)
            },
            onNegative = { setRemoveState(null) }
        )
    }
}

@Composable
fun HistoryHeader(
    modifier: Modifier = Modifier,
    date: Date,
    relativeTime: Int,
    dateFormat: DateFormat,
) {
    Text(
        modifier = modifier
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        text = date.toRelativeString(
            LocalContext.current,
            relativeTime,
            dateFormat
        ),
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
    )
}

@Composable
fun HistoryItem(
    modifier: Modifier = Modifier,
    history: MangaChapterHistory,
    onClickItem: () -> Unit,
    onClickResume: () -> Unit,
    onClickDelete: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClickItem)
            .height(96.dp)
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MangaCover(
            modifier = Modifier.fillMaxHeight(),
            manga = history.manga,
            aspect = MangaCoverAspect.COVER
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = horizontalPadding, end = 8.dp),
        ) {
            val textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = history.manga.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = textStyle.copy(fontWeight = FontWeight.SemiBold)
            )
            Row {
                Text(
                    text = buildSpannedString {
                        if (history.chapter.chapter_number > -1) {
                            append(
                                stringResource(
                                    R.string.history_prefix,
                                    chapterFormatter.format(history.chapter.chapter_number)
                                )
                            )
                        }
                        append(Date(history.history.last_read).toTimestampString())
                    }.toString(),
                    modifier = Modifier.padding(top = 2.dp),
                    style = textStyle
                )
            }
        }
        IconButton(onClick = onClickDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(id = R.string.action_delete),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onClickResume) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(id = R.string.action_resume),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun RemoveHistoryDialog(
    onPositive: (Boolean) -> Unit,
    onNegative: () -> Unit
) {
    val (removeEverything, removeEverythingState) = remember { mutableStateOf(false) }

    AlertDialog(
        title = {
            Text(text = stringResource(id = R.string.action_remove))
        },
        text = {
            Column {
                Text(text = stringResource(id = R.string.dialog_with_checkbox_remove_description))
                Row(
                    modifier = Modifier.toggleable(value = removeEverything, onValueChange = removeEverythingState),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = removeEverything,
                        onCheckedChange = removeEverythingState,
                    )
                    Text(
                        text = stringResource(id = R.string.dialog_with_checkbox_reset)
                    )
                }
            }
        },
        onDismissRequest = onNegative,
        confirmButton = {
            TextButton(onClick = { onPositive(removeEverything) }) {
                Text(text = stringResource(id = R.string.action_remove))
            }
        },
        dismissButton = {
            TextButton(onClick = onNegative) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        },
    )
}
