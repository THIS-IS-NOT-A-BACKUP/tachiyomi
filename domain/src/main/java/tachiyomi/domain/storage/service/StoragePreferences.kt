package tachiyomi.domain.storage.service

import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.storage.FolderProvider

class StoragePreferences(
    private val folderProvider: FolderProvider,
    private val preferenceStore: PreferenceStore,
) {

    fun baseStorageDirectory() = preferenceStore.getString("storage_dir", folderProvider.path())
}
