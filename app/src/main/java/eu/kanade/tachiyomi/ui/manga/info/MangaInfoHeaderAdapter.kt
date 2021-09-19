package eu.kanade.tachiyomi.ui.manga.info

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.databinding.MangaInfoHeaderBinding
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.controller.getMainAppBarHeight
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.view.loadAnyAutoPause
import eu.kanade.tachiyomi.util.view.setChips
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.view.clicks
import reactivecircus.flowbinding.android.view.longClicks
import uy.kohesive.injekt.injectLazy

class MangaInfoHeaderAdapter(
    private val controller: MangaController,
    private val fromSource: Boolean,
    private val isTablet: Boolean,
) :
    RecyclerView.Adapter<MangaInfoHeaderAdapter.HeaderViewHolder>() {

    private val trackManager: TrackManager by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()
    private val sourceManager: SourceManager by injectLazy()

    private var manga: Manga = controller.presenter.manga
    private var source: Source = controller.presenter.source
    private var trackCount: Int = 0

    private lateinit var binding: MangaInfoHeaderBinding

    private var initialLoad: Boolean = true

    private val maxLines = 3

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        binding = MangaInfoHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        updateCoverPosition()
        return HeaderViewHolder(binding.root)
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind()
    }

    /**
     * Update the view with manga information.
     *
     * @param manga manga object containing information about manga.
     * @param source the source of the manga.
     */
    fun update(manga: Manga, source: Source) {
        this.manga = manga
        this.source = source

        notifyDataSetChanged()
    }

    fun setTrackingCount(trackCount: Int) {
        this.trackCount = trackCount

        notifyDataSetChanged()
    }

    private fun updateCoverPosition() {
        val appBarHeight = controller.getMainAppBarHeight()
        binding.mangaCover.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin += appBarHeight
        }
    }

    inner class HeaderViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            // For rounded corners
            binding.mangaCover.clipToOutline = true

            binding.btnFavorite.clicks()
                .onEach { controller.onFavoriteClick() }
                .launchIn(controller.viewScope)

            if (controller.presenter.manga.favorite && controller.presenter.getCategories().isNotEmpty()) {
                binding.btnFavorite.longClicks()
                    .onEach { controller.onCategoriesClick() }
                    .launchIn(controller.viewScope)
            }

            with(binding.btnTracking) {
                if (trackManager.hasLoggedServices()) {
                    isVisible = true

                    if (trackCount > 0) {
                        setIconResource(R.drawable.ic_done_24dp)
                        text = view.context.resources.getQuantityString(
                            R.plurals.num_trackers,
                            trackCount,
                            trackCount
                        )
                        isActivated = true
                    } else {
                        setIconResource(R.drawable.ic_sync_24dp)
                        text = view.context.getString(R.string.manga_tracking_tab)
                        isActivated = false
                    }

                    clicks()
                        .onEach { controller.onTrackingClick() }
                        .launchIn(controller.viewScope)
                } else {
                    isVisible = false
                }
            }

            if (controller.presenter.source is HttpSource) {
                binding.btnWebview.isVisible = true
                binding.btnWebview.clicks()
                    .onEach { controller.openMangaInWebView() }
                    .launchIn(controller.viewScope)
            }

            binding.mangaFullTitle.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        view.context.getString(R.string.title),
                        binding.mangaFullTitle.text.toString()
                    )
                }
                .launchIn(controller.viewScope)

            binding.mangaFullTitle.clicks()
                .onEach {
                    controller.performGlobalSearch(binding.mangaFullTitle.text.toString())
                }
                .launchIn(controller.viewScope)

            binding.mangaAuthor.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        binding.mangaAuthor.text.toString(),
                        binding.mangaAuthor.text.toString()
                    )
                }
                .launchIn(controller.viewScope)

            binding.mangaAuthor.clicks()
                .onEach {
                    controller.performGlobalSearch(binding.mangaAuthor.text.toString())
                }
                .launchIn(controller.viewScope)

            binding.mangaArtist.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        binding.mangaArtist.text.toString(),
                        binding.mangaArtist.text.toString()
                    )
                }
                .launchIn(controller.viewScope)

            binding.mangaArtist.clicks()
                .onEach {
                    controller.performGlobalSearch(binding.mangaArtist.text.toString())
                }
                .launchIn(controller.viewScope)

            binding.mangaSummaryText.longClicks()
                .onEach {
                    controller.activity?.copyToClipboard(
                        view.context.getString(R.string.description),
                        binding.mangaSummaryText.text.toString()
                    )
                }
                .launchIn(controller.viewScope)

            binding.mangaCover.clicks()
                .onEach {
                    controller.showFullCoverDialog()
                }
                .launchIn(controller.viewScope)

            binding.mangaCover.longClicks()
                .onEach {
                    showCoverOptionsDialog()
                }
                .launchIn(controller.viewScope)

            setMangaInfo(manga, source)
        }

        private fun showCoverOptionsDialog() {
            val options = listOfNotNull(
                R.string.action_share,
                R.string.action_save,
                // Can only edit cover for library manga
                if (manga.favorite) R.string.action_edit else null
            ).map(controller.activity!!::getString).toTypedArray()

            MaterialAlertDialogBuilder(controller.activity!!)
                .setTitle(R.string.manga_cover)
                .setItems(options) { _, item ->
                    when (item) {
                        0 -> controller.shareCover()
                        1 -> controller.saveCover()
                        2 -> controller.changeCover()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        /**
         * Update the view with manga information.
         *
         * @param manga manga object containing information about manga.
         * @param source the source of the manga.
         */
        private fun setMangaInfo(manga: Manga, source: Source?) {
            // Update full title TextView.
            binding.mangaFullTitle.text = if (manga.title.isBlank()) {
                view.context.getString(R.string.unknown)
            } else {
                manga.title
            }

            // Update author TextView.
            binding.mangaAuthor.text = if (manga.author.isNullOrBlank()) {
                view.context.getString(R.string.unknown_author)
            } else {
                manga.author
            }

            // Update artist TextView.
            val hasArtist = !manga.artist.isNullOrBlank() && manga.artist != manga.author
            binding.mangaArtist.isVisible = hasArtist
            if (hasArtist) {
                binding.mangaArtist.text = manga.artist
            }

            // If manga source is known update source TextView.
            val mangaSource = source?.toString()
            with(binding.mangaSource) {
                if (mangaSource != null) {
                    val enabledLanguages = preferences.enabledLanguages().get()
                        .filterNot { it in listOf("all", "other") }

                    val hasOneActiveLanguages = enabledLanguages.size == 1
                    val isInEnabledLanguages = source.lang in enabledLanguages
                    text = when {
                        // For edge cases where user disables a source they got manga of in their library.
                        hasOneActiveLanguages && !isInEnabledLanguages -> mangaSource
                        // Hide the language tag when only one language is used.
                        hasOneActiveLanguages && isInEnabledLanguages -> source.name
                        else -> mangaSource
                    }

                    setOnClickListener {
                        controller.performSearch(sourceManager.getOrStub(source.id).name)
                    }
                } else {
                    text = view.context.getString(R.string.unknown)
                }
            }

            // Update manga status.
            val (statusDrawable, statusString) = when (manga.status) {
                SManga.ONGOING -> R.drawable.ic_status_ongoing_24dp to R.string.ongoing
                SManga.COMPLETED -> R.drawable.ic_status_completed_24dp to R.string.completed
                SManga.LICENSED -> R.drawable.ic_status_licensed_24dp to R.string.licensed
                else -> R.drawable.ic_status_unknown_24dp to R.string.unknown
            }
            binding.mangaStatusIcon.setImageResource(statusDrawable)
            binding.mangaStatus.setText(statusString)

            // Set the favorite drawable to the correct one.
            setFavoriteButtonState(manga.favorite)

            // Set cover if changed.
            binding.backdrop.loadAnyAutoPause(manga)
            binding.mangaCover.loadAnyAutoPause(manga)

            // Manga info section
            val hasInfoContent = !manga.description.isNullOrBlank() || !manga.genre.isNullOrBlank()
            showMangaInfo(hasInfoContent)
            if (hasInfoContent) {
                // Update description TextView.
                binding.mangaSummaryText.text = updateDescription(manga.description, (fromSource || isTablet).not())

                // Update genres list
                if (!manga.genre.isNullOrBlank()) {
                    binding.mangaGenresTagsCompactChips.setChips(
                        manga.getGenres(),
                        controller::performGenreSearch
                    )
                    binding.mangaGenresTagsFullChips.setChips(
                        manga.getGenres(),
                        controller::performGenreSearch
                    )
                } else {
                    binding.mangaGenresTagsCompact.isVisible = false
                    binding.mangaGenresTagsCompactChips.isVisible = false
                    binding.mangaGenresTagsFullChips.isVisible = false
                }

                // Handle showing more or less info
                merge(
                    binding.mangaSummaryText.clicks(),
                    binding.mangaInfoToggleMore.clicks(),
                    binding.mangaInfoToggleLess.clicks(),
                    binding.mangaSummarySection.clicks(),
                )
                    .onEach { toggleMangaInfo() }
                    .launchIn(controller.viewScope)

                if (initialLoad) {
                    binding.mangaGenresTagsCompact.requestLayout()
                }

                // Expand manga info if navigated from source listing or explicitly set to
                // (e.g. on tablets)
                if (initialLoad && (fromSource || isTablet)) {
                    toggleMangaInfo()
                    initialLoad = false
                }
            }
        }

        private fun showMangaInfo(visible: Boolean) {
            binding.mangaSummarySection.isVisible = visible
        }

        private fun toggleMangaInfo() {
            val isCurrentlyExpanded = binding.mangaSummaryText.maxLines != maxLines

            binding.mangaInfoToggleMore.isVisible = isCurrentlyExpanded
            binding.mangaInfoScrim.isVisible = isCurrentlyExpanded
            binding.mangaInfoToggleMoreScrim.isVisible = isCurrentlyExpanded
            binding.mangaGenresTagsCompact.isVisible = isCurrentlyExpanded
            binding.mangaGenresTagsCompactChips.isVisible = isCurrentlyExpanded

            binding.mangaInfoToggleLess.isVisible = !isCurrentlyExpanded
            binding.mangaGenresTagsFullChips.isVisible = !isCurrentlyExpanded

            binding.mangaSummaryText.text = updateDescription(manga.description, isCurrentlyExpanded)

            binding.mangaSummaryText.maxLines = when {
                isCurrentlyExpanded -> maxLines
                else -> Int.MAX_VALUE
            }
        }

        private fun updateDescription(description: String?, isCurrentlyExpanded: Boolean): CharSequence {
            return when {
                description.isNullOrBlank() -> view.context.getString(R.string.unknown)
                isCurrentlyExpanded ->
                    description
                        .replace(Regex(" +\$", setOf(RegexOption.MULTILINE)), "")
                        .replace(Regex("[\\r\\n]{2,}", setOf(RegexOption.MULTILINE)), "\n")
                else -> description
            }
        }

        /**
         * Update favorite button with correct drawable and text.
         *
         * @param isFavorite determines if manga is favorite or not.
         */
        private fun setFavoriteButtonState(isFavorite: Boolean) {
            // Set the Favorite drawable to the correct one.
            // Border drawable if false, filled drawable if true.
            val (iconResource, stringResource) = when (isFavorite) {
                true -> R.drawable.ic_favorite_24dp to R.string.in_library
                false -> R.drawable.ic_favorite_border_24dp to R.string.add_to_library
            }
            binding.btnFavorite.apply {
                setIconResource(iconResource)
                text = context.getString(stringResource)
                isActivated = isFavorite
            }
        }
    }
}
