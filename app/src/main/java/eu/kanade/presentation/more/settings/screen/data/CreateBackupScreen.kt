package eu.kanade.presentation.more.settings.screen.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.backup.create.BackupCreateFlags
import eu.kanade.tachiyomi.data.backup.create.BackupCreateJob
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.update
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

class CreateBackupScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { CreateBackupScreenModel() }
        val state by model.state.collectAsState()

        val chooseBackupDir = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/*"),
        ) {
            if (it != null) {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                model.createBackup(context, it)
                navigator.pop()
            }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.pref_create_backup),
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
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = MaterialTheme.padding.medium),
                ) {
                    item {
                        LabeledCheckbox(
                            label = stringResource(MR.strings.manga),
                            checked = true,
                            onCheckedChange = {},
                            enabled = false,
                        )
                    }
                    BackupChoices.forEach { (k, v) ->
                        item {
                            LabeledCheckbox(
                                label = stringResource(v),
                                checked = state.flags.contains(k),
                                onCheckedChange = {
                                    model.toggleFlag(k)
                                },
                            )
                        }
                    }
                }

                HorizontalDivider()

                Button(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    onClick = {
                        if (!BackupCreateJob.isManualJobRunning(context)) {
                            if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                                context.toast(MR.strings.restore_miui_warning, Toast.LENGTH_LONG)
                            }
                            try {
                                chooseBackupDir.launch(Backup.getFilename())
                            } catch (e: ActivityNotFoundException) {
                                context.toast(MR.strings.file_picker_error)
                            }
                        } else {
                            context.toast(MR.strings.backup_in_progress)
                        }
                    },
                ) {
                    Text(
                        text = stringResource(MR.strings.action_create),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

private class CreateBackupScreenModel : StateScreenModel<CreateBackupScreenModel.State>(State()) {

    fun toggleFlag(flag: Int) {
        mutableState.update {
            if (it.flags.contains(flag)) {
                it.copy(flags = it.flags - flag)
            } else {
                it.copy(flags = it.flags + flag)
            }
        }
    }

    fun createBackup(context: Context, uri: Uri) {
        val flags = state.value.flags.fold(initial = 0, operation = { a, b -> a or b })
        BackupCreateJob.startNow(context, uri, flags)
    }

    @Immutable
    data class State(
        val flags: PersistentSet<Int> = BackupChoices.keys.toPersistentSet(),
    )
}

private val BackupChoices = mapOf(
    BackupCreateFlags.BACKUP_CATEGORY to MR.strings.categories,
    BackupCreateFlags.BACKUP_CHAPTER to MR.strings.chapters,
    BackupCreateFlags.BACKUP_TRACK to MR.strings.track,
    BackupCreateFlags.BACKUP_HISTORY to MR.strings.history,
    BackupCreateFlags.BACKUP_APP_PREFS to MR.strings.app_settings,
    BackupCreateFlags.BACKUP_SOURCE_PREFS to MR.strings.source_settings,
)
