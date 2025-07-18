package eu.kanade.tachiyomi.data.preference

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import com.fredporciuncula.flow.preferences.Preference
import com.google.android.material.color.DynamicColors
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.updater.AppDownloadInstallJob
import eu.kanade.tachiyomi.extension.model.InstalledExtensionsOrder
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.ui.library.filter.FilterBottomSheet
import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.settings.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.recents.RecentMangaAdapter
import eu.kanade.tachiyomi.ui.recents.RecentsPresenter
import eu.kanade.tachiyomi.util.system.Themes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.preference.PreferenceValues as Values

fun <T> Preference<T>.asImmediateFlow(block: (value: T) -> Unit): Flow<T> {
    block(get())
    return asFlow()
        .onEach { block(it) }
}

fun <T> Preference<T>.asImmediateFlowIn(
    scope: CoroutineScope,
    block: (value: T) -> Unit,
): Job {
    block(get())
    return asFlow()
        .onEach { block(it) }
        .launchIn(scope)
}

fun Preference<Boolean>.toggle() = set(!get())

operator fun <T> Preference<Set<T>>.plusAssign(item: T) {
    set(get() + item)
}

operator fun <T> Preference<Set<T>>.minusAssign(item: T) {
    set(get() - item)
}

operator fun <T> Preference<Set<T>>.plusAssign(item: Collection<T>) {
    set(get() + item)
}

operator fun <T> Preference<Set<T>>.minusAssign(item: Collection<T>) {
    set(get() - item)
}

class PreferencesHelper(
    val context: Context,
) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val flowPrefs = FlowSharedPreferences(prefs)

    private val defaultDownloadsDir =
        Uri.fromFile(
            File(
                Environment.getExternalStorageDirectory().absolutePath + File.separator +
                    context.getString(R.string.app_name),
                "downloads",
            ),
        )

    private val defaultBackupDir =
        Uri.fromFile(
            File(
                Environment.getExternalStorageDirectory().absolutePath + File.separator +
                    context.getString(R.string.app_name),
                "backup",
            ),
        )

    fun getInt(
        key: String,
        default: Int,
    ) = flowPrefs.getInt(key, default)

    fun getStringPref(
        key: String,
        default: String = "",
    ) = flowPrefs.getString(key, default)

    fun getStringSet(
        key: String,
        default: Set<String>,
    ) = flowPrefs.getStringSet(key, default)

    fun startingTab() = flowPrefs.getInt(Keys.startingTab, 0)

    fun backReturnsToStart() = flowPrefs.getBoolean(Keys.backToStart, true)

    fun hasShownNotifPermission() = flowPrefs.getBoolean("has_shown_notification_permission", false)

    fun hasDeniedA11FilePermission() = flowPrefs.getBoolean(Keys.deniedA11FilePermission, false)

    fun clear() = prefs.edit().clear().apply()

    fun nightMode() = flowPrefs.getInt(Keys.nightMode, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    fun themeDarkAmoled() = flowPrefs.getBoolean(Keys.themeDarkAmoled, false)

    private val supportsDynamic = DynamicColors.isDynamicColorAvailable()

    fun lightTheme() = flowPrefs.getEnum(Keys.lightTheme, if (supportsDynamic) Themes.MONET else Themes.DEFAULT)

    fun darkTheme() = flowPrefs.getEnum(Keys.darkTheme, if (supportsDynamic) Themes.MONET else Themes.DEFAULT)

    fun pageTransitions() = flowPrefs.getBoolean(Keys.enableTransitions, true)

    fun pagerCutoutBehavior() = flowPrefs.getInt(Keys.pagerCutoutBehavior, 0)

    fun landscapeCutoutBehavior() = flowPrefs.getInt("landscape_cutout_behavior", 0)

    fun doubleTapAnimSpeed() = flowPrefs.getInt(Keys.doubleTapAnimationSpeed, 500)

    fun showPageNumber() = flowPrefs.getBoolean(Keys.showPageNumber, true)

    fun trueColor() = flowPrefs.getBoolean(Keys.trueColor, false)

    fun fullscreen() = flowPrefs.getBoolean(Keys.fullscreen, true)

    fun keepScreenOn() = flowPrefs.getBoolean(Keys.keepScreenOn, true)

    fun customBrightness() = flowPrefs.getBoolean(Keys.customBrightness, false)

    fun customBrightnessValue() = flowPrefs.getInt(Keys.customBrightnessValue, 0)

    fun colorFilter() = flowPrefs.getBoolean(Keys.colorFilter, false)

    fun colorFilterValue() = flowPrefs.getInt(Keys.colorFilterValue, 0)

    fun colorFilterMode() = flowPrefs.getInt(Keys.colorFilterMode, 0)

    fun defaultReadingMode() = prefs.getInt(Keys.defaultReadingMode, ReadingModeType.RIGHT_TO_LEFT.flagValue)

    fun defaultOrientationType() = flowPrefs.getInt(Keys.defaultOrientationType, OrientationType.FREE.flagValue)

    fun imageScaleType() = flowPrefs.getInt(Keys.imageScaleType, 1)

    fun zoomStart() = flowPrefs.getInt(Keys.zoomStart, 1)

    fun readerTheme() = flowPrefs.getInt(Keys.readerTheme, 2)

    fun cropBorders() = flowPrefs.getBoolean(Keys.cropBorders, false)

    fun cropBordersWebtoon() = flowPrefs.getBoolean(Keys.cropBordersWebtoon, false)

    fun navigateToPan() = flowPrefs.getBoolean("navigate_pan", true)

    fun landscapeZoom() = flowPrefs.getBoolean("landscape_zoom", false)

    fun grayscale() = flowPrefs.getBoolean("pref_grayscale", false)

    fun invertedColors() = flowPrefs.getBoolean("pref_inverted_colors", false)

    fun webtoonSidePadding() = flowPrefs.getInt(Keys.webtoonSidePadding, 0)

    fun webtoonEnableZoomOut() = flowPrefs.getBoolean(Keys.webtoonEnableZoomOut, false)

    fun readWithLongTap() = flowPrefs.getBoolean(Keys.readWithLongTap, true)

    fun readWithVolumeKeys() = flowPrefs.getBoolean(Keys.readWithVolumeKeys, false)

    fun readWithVolumeKeysInverted() = flowPrefs.getBoolean(Keys.readWithVolumeKeysInverted, false)

    fun navigationModePager() = flowPrefs.getInt(Keys.navigationModePager, 0)

    fun navigationModeWebtoon() = flowPrefs.getInt(Keys.navigationModeWebtoon, 0)

    fun pagerNavInverted() = flowPrefs.getEnum(Keys.pagerNavInverted, ViewerNavigation.TappingInvertMode.NONE)

    fun webtoonNavInverted() = flowPrefs.getEnum(Keys.webtoonNavInverted, ViewerNavigation.TappingInvertMode.NONE)

    fun pageLayout() = flowPrefs.getInt(Keys.pageLayout, PageLayout.AUTOMATIC.value)

    fun automaticSplitsPage() = flowPrefs.getBoolean(Keys.automaticSplitsPage, false)

    fun invertDoublePages() = flowPrefs.getBoolean(Keys.invertDoublePages, false)

    fun webtoonPageLayout() = flowPrefs.getInt(Keys.webtoonPageLayout, PageLayout.SINGLE_PAGE.value)

    fun webtoonReaderHideThreshold() = flowPrefs.getEnum("reader_hide_threshold", Values.ReaderHideThreshold.LOW)

    fun webtoonInvertDoublePages() = flowPrefs.getBoolean(Keys.webtoonInvertDoublePages, false)

    fun readerBottomButtons() =
        flowPrefs.getStringSet(
            Keys.readerBottomButtons,
            ReaderBottomButton.BUTTONS_DEFAULTS,
        )

    fun showNavigationOverlayNewUser() = flowPrefs.getBoolean(Keys.showNavigationOverlayNewUser, true)

    fun showNavigationOverlayNewUserWebtoon() = flowPrefs.getBoolean(Keys.showNavigationOverlayNewUserWebtoon, true)

    fun preloadSize() = flowPrefs.getInt(Keys.preloadSize, 6)

    fun autoUpdateTrack() = prefs.getBoolean(Keys.autoUpdateTrack, true)

    fun trackMarkedAsRead() = prefs.getBoolean(Keys.trackMarkedAsRead, false)

    fun trackingsToAddOnline() = flowPrefs.getStringSet(Keys.trackingsToAddOnline, emptySet())

    fun lastUsedCatalogueSource() = flowPrefs.getLong(Keys.lastUsedCatalogueSource, -1)

    fun lastUsedCategory() = flowPrefs.getInt(Keys.lastUsedCategory, 0)

    fun lastUsedSources() = flowPrefs.getStringSet("last_used_sources", emptySet())

    fun lastVersionCode() = flowPrefs.getInt("last_version_code", 0)

    fun browseAsList() = flowPrefs.getBoolean(Keys.catalogueAsList, false)

    fun enabledLanguages() =
        flowPrefs.getStringSet(
            Keys.enabledLanguages,
            setOfNotNull("all", "en", Locale.getDefault().language.takeIf { !it.startsWith("en") }),
        )

    fun sourceSorting() = flowPrefs.getInt(Keys.sourcesSort, 0)

    fun anilistScoreType() = flowPrefs.getString("anilist_score_type", "POINT_10")

    fun backupsDirectory() = flowPrefs.getString(Keys.backupDirectory, defaultBackupDir.toString())

    fun dateFormat(format: String = flowPrefs.getString(Keys.dateFormat, "").get()): DateFormat =
        when (format) {
            "" -> DateFormat.getDateInstance(DateFormat.SHORT)
            else -> SimpleDateFormat(format, Locale.getDefault())
        }

    fun appLanguage() = flowPrefs.getString("app_language", "")

    fun downloadsDirectory() = flowPrefs.getString(Keys.downloadsDirectory, defaultDownloadsDir.toString())

    fun downloadOnlyOverWifi() = prefs.getBoolean(Keys.downloadOnlyOverWifi, true)

    fun folderPerManga() = flowPrefs.getBoolean("create_folder_per_manga", false)

    fun librarySearchSuggestion() = flowPrefs.getString(Keys.librarySearchSuggestion, "")

    fun showLibrarySearchSuggestions() = flowPrefs.getBoolean(Keys.showLibrarySearchSuggestions, false)

    fun lastLibrarySuggestion() = flowPrefs.getLong("last_library_suggestion", 0L)

    fun numberOfBackups() = flowPrefs.getInt(Keys.numberOfBackups, 2)

    fun backupInterval() = flowPrefs.getInt(Keys.backupInterval, 0)

    fun removeAfterReadSlots() = flowPrefs.getInt(Keys.removeAfterReadSlots, -1)

    fun removeAfterMarkedAsRead() = prefs.getBoolean(Keys.removeAfterMarkedAsRead, false)

    fun libraryUpdateInterval() = flowPrefs.getInt(Keys.libraryUpdateInterval, 24)

    fun libraryUpdateLastTimestamp() = flowPrefs.getLong("library_update_last_timestamp", 0L)

    fun libraryUpdateDeviceRestriction() = flowPrefs.getStringSet("library_update_restriction", setOf(DEVICE_ONLY_ON_WIFI))

    fun libraryUpdateMangaRestriction() =
        flowPrefs.getStringSet("library_update_manga_restriction", setOf(MANGA_HAS_UNREAD, MANGA_NON_COMPLETED, MANGA_NON_READ))

    fun libraryUpdateCategories() = flowPrefs.getStringSet("library_update_categories", emptySet())

    fun libraryUpdateCategoriesExclude() = flowPrefs.getStringSet("library_update_categories_exclude", emptySet())

    fun libraryLayout() = flowPrefs.getInt(Keys.libraryLayout, LibraryItem.LAYOUT_COMFORTABLE_GRID)

    fun gridSize() = flowPrefs.getFloat(Keys.gridSize, 1f)

    fun uniformGrid() = flowPrefs.getBoolean(Keys.uniformGrid, true)

    fun outlineOnCovers() = flowPrefs.getBoolean(Keys.outlineOnCovers, true)

    fun downloadBadge() = flowPrefs.getBoolean(Keys.downloadBadge, false)

    fun languageBadge() = flowPrefs.getBoolean(Keys.languageBadge, false)

    fun filterDownloaded() = flowPrefs.getInt(Keys.filterDownloaded, 0)

    fun filterUnread() = flowPrefs.getInt(Keys.filterUnread, 0)

    fun filterCompleted() = flowPrefs.getInt(Keys.filterCompleted, 0)

    fun filterBookmarked() = flowPrefs.getInt("pref_filter_bookmarked_key", 0)

    fun filterTracked() = flowPrefs.getInt(Keys.filterTracked, 0)

    fun filterMangaType() = flowPrefs.getInt(Keys.filterMangaType, 0)

    fun showEmptyCategoriesWhileFiltering() = flowPrefs.getBoolean(Keys.showEmptyCategoriesFiltering, false)

    fun librarySortingMode() = flowPrefs.getInt("library_sorting_mode", 0)

    fun librarySortingAscending() = flowPrefs.getBoolean("library_sorting_ascending", true)

    fun automaticExtUpdates() = flowPrefs.getBoolean(Keys.automaticExtUpdates, true)

    fun extensionRepos() = flowPrefs.getStringSet("extension_repos", emptySet())

    fun installedExtensionsOrder() = flowPrefs.getInt(Keys.installedExtensionsOrder, InstalledExtensionsOrder.Name.value)

    fun migrationSourceOrder() = flowPrefs.getInt("migration_source_order", Values.MigrationSourceOrder.Alphabetically.value)

    fun collapsedCategories() = flowPrefs.getStringSet("collapsed_categories", mutableSetOf())

    fun collapsedDynamicCategories() = flowPrefs.getStringSet("collapsed_dynamic_categories", mutableSetOf())

    fun collapsedDynamicAtBottom() = flowPrefs.getBoolean("collapsed_dynamic_at_bottom", false)

    fun hiddenSources() = flowPrefs.getStringSet("hidden_catalogues", mutableSetOf())

    fun pinnedCatalogues() = flowPrefs.getStringSet("pinned_catalogues", mutableSetOf())

    fun saveChaptersAsCBZ() = flowPrefs.getBoolean("save_chapter_as_cbz", true)

    fun splitTallImages() = flowPrefs.getBoolean("split_tall_images", false)

    fun downloadNewChapters() = flowPrefs.getBoolean(Keys.downloadNew, false)

    fun downloadNewChaptersInCategories() = flowPrefs.getStringSet("download_new_categories", emptySet())

    fun excludeCategoriesInDownloadNew() = flowPrefs.getStringSet("download_new_categories_exclude", emptySet())

    fun autoDownloadWhileReading() = flowPrefs.getInt("auto_download_while_reading", 0)

    fun defaultCategory() = prefs.getInt(Keys.defaultCategory, -2)

    fun skipRead() = prefs.getBoolean(Keys.skipRead, false)

    fun skipFiltered() = prefs.getBoolean(Keys.skipFiltered, true)

    fun skipDupe() = flowPrefs.getBoolean("skip_dupe", false)

    fun useBiometrics() = flowPrefs.getBoolean(Keys.useBiometrics, false)

    fun lockAfter() = flowPrefs.getInt(Keys.lockAfter, 0)

    fun lastUnlock() = flowPrefs.getLong(Keys.lastUnlock, 0)

    fun secureScreen() = flowPrefs.getEnum("secure_screen_v2", Values.SecureScreenMode.INCOGNITO)

    fun hideNotificationContent() = prefs.getBoolean(Keys.hideNotificationContent, false)

    fun removeArticles() = flowPrefs.getBoolean(Keys.removeArticles, false)

    fun migrateFlags() = flowPrefs.getInt("migrate_flags", Int.MAX_VALUE)

    fun trustedExtensions() = flowPrefs.getStringSet("trusted_extensions", emptySet())

    // using string instead of set so it is ordered
    fun migrationSources() = flowPrefs.getString("migrate_sources", "")

    fun useSourceWithMost() = flowPrefs.getBoolean("use_source_with_most", false)

    fun skipPreMigration() = flowPrefs.getBoolean(Keys.skipPreMigration, false)

    fun defaultMangaOrder() = flowPrefs.getString("default_manga_order", "")

    fun refreshCoversToo() = flowPrefs.getBoolean(Keys.refreshCoversToo, true)

    fun extensionUpdatesCount() = flowPrefs.getInt("ext_updates_count", 0)

    fun recentsViewType() = flowPrefs.getInt("recents_view_type", 0)

    fun showRecentsDownloads() = flowPrefs.getEnum(Keys.showDLsInRecents, RecentMangaAdapter.ShowRecentsDLs.All)

    fun showRecentsRemHistory() = flowPrefs.getBoolean(Keys.showRemHistoryInRecents, true)

    fun showReadInAllRecents() = flowPrefs.getBoolean(Keys.showReadInAllRecents, false)

    fun showUpdatedTime() = flowPrefs.getBoolean(Keys.showUpdatedTime, false)

    fun sortFetchedTime() = flowPrefs.getBoolean("sort_fetched_time", false)

    fun collapseGroupedUpdates() = flowPrefs.getBoolean("group_chapters_updates", false)

    fun groupChaptersHistory() = flowPrefs.getEnum("group_chapters_history_type", RecentsPresenter.GroupType.ByWeek)

    fun collapseGroupedHistory() = flowPrefs.getBoolean("collapse_group_history", true)

    fun showTitleFirstInRecents() = flowPrefs.getBoolean(Keys.showTitleFirstInRecents, false)

    fun lastExtCheck() = flowPrefs.getLong("last_ext_check", 0)

    fun lastAppCheck() = flowPrefs.getLong("last_app_check", 0)

    fun checkForBetas() = flowPrefs.getBoolean("check_for_betas", BuildConfig.BETA)

    fun unreadBadgeType() = flowPrefs.getInt("unread_badge_type", 2)

    fun categoryNumberOfItems() = flowPrefs.getBoolean(Keys.categoryNumberOfItems, false)

    fun hideStartReadingButton() = flowPrefs.getBoolean("hide_reading_button", false)

    fun alwaysShowChapterTransition() = flowPrefs.getBoolean(Keys.alwaysShowChapterTransition, true)

    fun deleteRemovedChapters() = flowPrefs.getInt(Keys.deleteRemovedChapters, 0)

    fun removeBookmarkedChapters() = flowPrefs.getBoolean("pref_remove_bookmarked", false)

    fun removeExcludeCategories() = flowPrefs.getStringSet("remove_exclude_categories", emptySet())

    fun showAllCategories() = flowPrefs.getBoolean("show_all_categories", true)

    fun showAllCategoriesWhenSearchingSingleCategory() = flowPrefs.getBoolean("show_all_categories_when_searching_single_category", false)

    fun hopperGravity() = flowPrefs.getInt("hopper_gravity", 1)

    fun filterOrder() = flowPrefs.getString("filter_order", FilterBottomSheet.Filters.DEFAULT_ORDER)

    fun hopperLongPressAction() = flowPrefs.getInt(Keys.hopperLongPress, 0)

    fun hideHopper() = flowPrefs.getBoolean("hide_hopper", false)

    fun autohideHopper() = flowPrefs.getBoolean(Keys.autoHideHopper, true)

    fun groupLibraryBy() = flowPrefs.getInt("group_library_by", 0)

    fun showCategoryInTitle() = flowPrefs.getBoolean("category_in_title", false)

    fun onlySearchPinned() = flowPrefs.getBoolean(Keys.onlySearchPinned, false)

    fun hideInLibraryItems() = flowPrefs.getBoolean("browse_hide_in_library_items", false)

    fun showDuplicatedInLibraryItems() = flowPrefs.getBoolean("browse_show_duplicated_in_library_items", false)

    // Tutorial preferences
    fun shownFilterTutorial() = flowPrefs.getBoolean("shown_filter_tutorial", false)

    fun shownChapterSwipeTutorial() = flowPrefs.getBoolean("shown_swipe_tutorial", false)

    fun shownDownloadQueueTutorial() = flowPrefs.getBoolean("shown_download_queue", false)

    fun shownLongPressCategoryTutorial() = flowPrefs.getBoolean("shown_long_press_category", false)

    fun shownHopperSwipeTutorial() = flowPrefs.getBoolean("shown_hopper_swipe", false)

    fun shownDownloadSwipeTutorial() = flowPrefs.getBoolean("shown_download_tutorial", false)

    fun hideBottomNavOnScroll() = flowPrefs.getBoolean(Keys.hideBottomNavOnScroll, true)

    fun sideNavIconAlignment() = flowPrefs.getInt(Keys.sideNavIconAlignment, 1)

    fun showNsfwSources() = flowPrefs.getBoolean(Keys.showNsfwSource, true)

    fun themeMangaDetails() = prefs.getBoolean(Keys.themeMangaDetails, true)

    fun useLargeToolbar() = flowPrefs.getBoolean("use_large_toolbar", true)

    fun dohProvider() = prefs.getInt(Keys.dohProvider, -1)

    fun defaultUserAgent() = flowPrefs.getString("default_user_agent", NetworkHelper.DEFAULT_USER_AGENT)

    fun showSeriesInShortcuts() = prefs.getBoolean(Keys.showSeriesInShortcuts, true)

    fun showSourcesInShortcuts() = prefs.getBoolean(Keys.showSourcesInShortcuts, true)

    fun openChapterInShortcuts() = prefs.getBoolean(Keys.openChapterInShortcuts, true)

    fun incognitoMode() = flowPrefs.getBoolean(Keys.incognitoMode, false)

    fun hasPromptedBeforeUpdateAll() = flowPrefs.getBoolean("has_prompted_update_all", false)

    fun sideNavMode() = flowPrefs.getInt(Keys.sideNavMode, 0)

    fun appShouldAutoUpdate() = prefs.getInt(Keys.shouldAutoUpdate, AppDownloadInstallJob.ONLY_ON_UNMETERED)

    fun autoUpdateExtensions() = prefs.getInt(Keys.autoUpdateExtensions, AppDownloadInstallJob.ONLY_ON_UNMETERED)

    fun extensionInstaller() = flowPrefs.getInt("extension_installer", ExtensionInstaller.PACKAGE_INSTALLER)

    fun filterChapterByRead() = flowPrefs.getInt(Keys.defaultChapterFilterByRead, Manga.SHOW_ALL)

    fun filterChapterByDownloaded() = flowPrefs.getInt(Keys.defaultChapterFilterByDownloaded, Manga.SHOW_ALL)

    fun filterChapterByBookmarked() = flowPrefs.getInt(Keys.defaultChapterFilterByBookmarked, Manga.SHOW_ALL)

    fun sortChapterOrder() = flowPrefs.getInt(Keys.defaultChapterSortBySourceOrNumber, Manga.CHAPTER_SORTING_SOURCE)

    fun hideChapterTitlesByDefault() = flowPrefs.getBoolean(Keys.hideChapterTitles, false)

    fun chaptersDescAsDefault() = flowPrefs.getBoolean(Keys.chaptersDescAsDefault, true)

    fun sortChapterByAscendingOrDescending() = prefs.getInt(Keys.defaultChapterSortByAscendingOrDescending, Manga.CHAPTER_SORT_DESC)

    fun coverRatios() = flowPrefs.getStringSet(Keys.coverRatios, emptySet())

    fun coverColors() = flowPrefs.getStringSet(Keys.coverColors, emptySet())

    fun useStaggeredGrid() = flowPrefs.getBoolean("use_staggered_grid", false)
}
