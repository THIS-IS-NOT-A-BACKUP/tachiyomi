package eu.kanade.presentation.more.settings.screen.data

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.backup.BackupFileValidator
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.data.backup.restore.RestoreOptions
import eu.kanade.tachiyomi.util.system.DeviceUtil
import kotlinx.coroutines.flow.update
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.components.SectionCard
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

class RestoreBackupScreen(
    private val uri: Uri,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { RestoreBackupScreenModel(context, uri) }
        val state by model.state.collectAsState()

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.pref_restore_backup),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                ) {
                    if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                        item {
                            WarningBanner(MR.strings.restore_miui_warning)
                        }
                    }

                    if (state.canRestore) {
                        item {
                            SectionCard {
                                RestoreOptions.options.forEach { option ->
                                    LabeledCheckbox(
                                        label = stringResource(option.label),
                                        checked = option.getter(state.options),
                                        onCheckedChange = {
                                            model.toggle(option.setter, it)
                                        },
                                        modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                                    )
                                }
                            }
                        }
                    }

                    if (state.error != null) {
                        errorMessageItem(state.error)
                    }
                }

                HorizontalDivider()

                Button(
                    enabled = state.canRestore && state.options.anyEnabled(),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    onClick = {
                        model.startRestore()
                        navigator.pop()
                    },
                ) {
                    Text(
                        text = stringResource(MR.strings.action_restore),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }

    private fun LazyListScope.errorMessageItem(
        error: Any?,
    ) {
        item {
            SectionCard {
                Column(
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    when (error) {
                        is MissingRestoreComponents -> {
                            val msg = buildString {
                                append(stringResource(MR.strings.backup_restore_content_full))
                                if (error.sources.isNotEmpty()) {
                                    append("\n\n")
                                    append(stringResource(MR.strings.backup_restore_missing_sources))
                                    error.sources.joinTo(
                                        this,
                                        separator = "\n- ",
                                        prefix = "\n- ",
                                    )
                                }
                                if (error.trackers.isNotEmpty()) {
                                    append("\n\n")
                                    append(stringResource(MR.strings.backup_restore_missing_trackers))
                                    error.trackers.joinTo(
                                        this,
                                        separator = "\n- ",
                                        prefix = "\n- ",
                                    )
                                }
                            }
                            SelectionContainer {
                                Text(text = msg)
                            }
                        }

                        is InvalidRestore -> {
                            Text(text = stringResource(MR.strings.invalid_backup_file))

                            SelectionContainer {
                                Text(text = listOfNotNull(error.uri, error.message).joinToString("\n\n"))
                            }
                        }

                        else -> {
                            SelectionContainer {
                                Text(text = error.toString())
                            }
                        }
                    }
                }
            }
        }
    }
}

private class RestoreBackupScreenModel(
    private val context: Context,
    private val uri: Uri,
) : StateScreenModel<RestoreBackupScreenModel.State>(State()) {

    init {
        validate(uri)
    }

    fun toggle(setter: (RestoreOptions, Boolean) -> RestoreOptions, enabled: Boolean) {
        mutableState.update {
            it.copy(
                options = setter(it.options, enabled),
            )
        }
    }

    fun startRestore() {
        BackupRestoreJob.start(
            context = context,
            uri = uri,
            options = state.value.options,
        )
    }

    private fun validate(uri: Uri) {
        val results = try {
            BackupFileValidator(context).validate(uri)
        } catch (e: Exception) {
            setError(
                error = InvalidRestore(uri, e.message.toString()),
                canRestore = false,
            )
            return
        }

        if (results.missingSources.isNotEmpty() || results.missingTrackers.isNotEmpty()) {
            setError(
                error = MissingRestoreComponents(uri, results.missingSources, results.missingTrackers),
                canRestore = true,
            )
            return
        }

        setError(error = null, canRestore = true)
    }

    private fun setError(error: Any?, canRestore: Boolean) {
        mutableState.update {
            it.copy(
                error = error,
                canRestore = canRestore,
            )
        }
    }

    @Immutable
    data class State(
        val error: Any? = null,
        val canRestore: Boolean = false,
        val options: RestoreOptions = RestoreOptions(),
    )
}

private data class MissingRestoreComponents(
    val uri: Uri,
    val sources: List<String>,
    val trackers: List<String>,
)

private data class InvalidRestore(
    val uri: Uri? = null,
    val message: String,
)
