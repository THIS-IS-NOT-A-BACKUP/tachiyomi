package eu.kanade.domain.base

import android.content.Context
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import tachiyomi.core.preference.Preference
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.i18n.MR

class BasePreferences(
    val context: Context,
    private val preferenceStore: PreferenceStore,
) {

    fun downloadedOnly() = preferenceStore.getBoolean(
        Preference.appStateKey("pref_downloaded_only"),
        false,
    )

    fun incognitoMode() = preferenceStore.getBoolean(Preference.appStateKey("incognito_mode"), false)

    fun extensionInstaller() = ExtensionInstallerPreference(context, preferenceStore)

    fun acraEnabled() = preferenceStore.getBoolean("acra.enable", isPreviewBuildType || isReleaseBuildType)

    enum class ExtensionInstaller(val titleRes: StringResource) {
        LEGACY(MR.strings.ext_installer_legacy),
        PACKAGEINSTALLER(MR.strings.ext_installer_packageinstaller),
        SHIZUKU(MR.strings.ext_installer_shizuku),
        PRIVATE(MR.strings.ext_installer_private),
    }
}
