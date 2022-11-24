package eu.kanade.tachiyomi.ui.browse.extension

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.ExtensionScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.util.LocalRouter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsController

@Composable
fun extensionsTab(
    extensionsScreenModel: ExtensionsScreenModel,
): TabContent {
    val router = LocalRouter.currentOrThrow
    val state by extensionsScreenModel.state.collectAsState()

    return TabContent(
        titleRes = R.string.label_extensions,
        badgeNumber = state.updates.takeIf { it > 0 },
        searchEnabled = true,
        actions = listOf(
            AppBar.Action(
                title = stringResource(R.string.action_filter),
                icon = Icons.Outlined.Translate,
                onClick = { router.pushController(ExtensionFilterController()) },
            ),
        ),
        content = { contentPadding, _ ->
            ExtensionScreen(
                state = state,
                contentPadding = contentPadding,
                onLongClickItem = { extension ->
                    when (extension) {
                        is Extension.Available -> extensionsScreenModel.installExtension(extension)
                        else -> extensionsScreenModel.uninstallExtension(extension.pkgName)
                    }
                },
                onClickItemCancel = extensionsScreenModel::cancelInstallUpdateExtension,
                onClickUpdateAll = extensionsScreenModel::updateAllExtensions,
                onInstallExtension = extensionsScreenModel::installExtension,
                onOpenExtension = { router.pushController(ExtensionDetailsController(it.pkgName)) },
                onTrustExtension = { extensionsScreenModel.trustSignature(it.signatureHash) },
                onUninstallExtension = { extensionsScreenModel.uninstallExtension(it.pkgName) },
                onUpdateExtension = extensionsScreenModel::updateExtension,
                onRefresh = extensionsScreenModel::findAvailableExtensions,
            )
        },
    )
}
