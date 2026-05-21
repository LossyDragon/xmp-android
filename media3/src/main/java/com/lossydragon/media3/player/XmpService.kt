package com.lossydragon.media3.player

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.lossydragon.media3.MainActivity
import com.lossydragon.media3.R
import com.lossydragon.media3.core.AutoMediaId
import com.lossydragon.media3.db.XmpPreferences
import kotlinx.coroutines.runBlocking
import org.helllabs.libxmp.Xmp
import org.helllabs.libxmp.model.ModInfo
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * [MediaLibraryService] that bridges [XmpPlayer] to the Media3 session API.
 * Provides a browsable media tree for Android Auto (File Browser + Playlists stub).
 */
@OptIn(UnstableApi::class)
class XmpService : MediaLibraryService() {

    private companion object {
        const val NOTIFICATION_ID = 669
        const val CHANNEL_ID = "669"
    }

    private val player: XmpPlayer by inject()
    private val prefs: XmpPreferences by inject()

    private lateinit var mediaLibrarySession: MediaLibrarySession

    /** App icon URI used as artwork in the notification and Auto browse tree. */
    private val artworkUri: Uri by lazy {
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(packageName)
            .appendPath(resources.getResourceTypeName(R.drawable.icon512))
            .appendPath(resources.getResourceEntryName(R.drawable.icon512))
            .build()
    }

    private val libraryCallback = object : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(
                LibraryResult.ofItem(
                    buildBrowsableItem(
                        id = AutoMediaId.ROOT,
                        title = "Xmp Player",
                        type = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                    ),
                    params,
                )
            )

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val items = when {
                parentId == AutoMediaId.ROOT -> getRootChildren()
                parentId == AutoMediaId.FILE_BROWSER -> getFileBrowserRoot()
                parentId == AutoMediaId.PLAYLISTS -> getPlaylists()
                AutoMediaId.isDir(parentId) -> getDirectoryChildren(parentId)
                else -> ImmutableList.of()
            }
            return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            val resolved = mediaItems.map { item ->
                val uri = when {
                    AutoMediaId.isFile(
                        item.mediaId
                    ) -> AutoMediaId.uriFromFile(item.mediaId).toUri()

                    item.localConfiguration?.uri != null -> item.localConfiguration!!.uri

                    else -> null
                }
                if (uri != null) item.buildUpon().setUri(uri).build() else item
            }
            return Futures.immediateFuture(resolved)
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
            Futures.immediateFailedFuture(UnsupportedOperationException())
    }

    override fun onCreate() {
        super.onCreate()

        mediaLibrarySession = MediaLibrarySession
            .Builder(this, player, libraryCallback)
            .setId(getString(R.string.app_name) + "_session")
            .setSessionActivity(buildSessionActivity())
            .build()

        DefaultMediaNotificationProvider.Builder(this)
            .setNotificationId(NOTIFICATION_ID)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .build()
            .also(::setMediaNotificationProvider)

        setListener(object : Listener {
            override fun onForegroundServiceStartNotAllowedException() {
                Timber.e("Foreground service start not allowed")
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaLibrarySession

    override fun onDestroy() {
        player.abandonAudioFocus()
        mediaLibrarySession.release()
        player.releaseEngine()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!isPlaybackOngoing) {
            player.releaseEngine()
            stopSelf()
        }
    }

    private fun buildSessionActivity(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** Root level — File Browser and Playlists. */
    private fun getRootChildren(): ImmutableList<MediaItem> = ImmutableList.of(
        buildBrowsableItem(
            AutoMediaId.FILE_BROWSER,
            "File Browser",
            MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
        ),
        buildBrowsableItem(
            AutoMediaId.PLAYLISTS,
            "Playlists",
            MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS
        ),
    )

    /** Stubbed until playlist persistence is implemented. */
    private fun getPlaylists(): ImmutableList<MediaItem> = ImmutableList.of(
        buildBrowsableItem(
            "playlists_coming_soon",
            "Coming Soon",
            MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS
        )
    )

    /** Returns children of the last-opened root directory. */
    private fun getFileBrowserRoot(): ImmutableList<MediaItem> {
        val treeUri = runBlocking { prefs.getLastDirectoryUri() }?.toUri()
            ?: return ImmutableList.of()
        return try {
            contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            buildChildItems(
                treeUri,
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri),
                )
            )
        } catch (e: SecurityException) {
            Timber.e(e, "No permission for $treeUri")
            ImmutableList.of()
        }
    }

    /** Returns children of the directory encoded in [parentId]. */
    private fun getDirectoryChildren(parentId: String): ImmutableList<MediaItem> =
        buildChildItems(
            AutoMediaId.treeUriFromDir(parentId),
            DocumentsContract.buildChildDocumentsUriUsingTree(
                AutoMediaId.treeUriFromDir(parentId),
                AutoMediaId.docIdFromDir(parentId),
            )
        )

    /**
     * Queries [childrenUri] and builds browsable dirs and playable modules.
     * Files are validated via [Xmp.testFromFd] before being included.
     */
    private fun buildChildItems(treeUri: Uri, childrenUri: Uri): ImmutableList<MediaItem> {
        val items = mutableListOf<MediaItem>()
        contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null,
            null,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val docId = cursor.getString(0)
                val name = cursor.getString(1)
                val mime = cursor.getString(2)
                val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

                when {
                    mime == DocumentsContract.Document.MIME_TYPE_DIR ->
                        items.add(
                            buildBrowsableItem(
                                AutoMediaId.dir(treeUri, docId),
                                name,
                                MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
                            )
                        )

                    Xmp.testFromFd(this, childUri, ModInfo()) ->
                        items.add(
                            buildPlayableItem(AutoMediaId.file(childUri.toString()), name, childUri)
                        )
                }
            }
        }
        return ImmutableList.copyOf(items)
    }

    private fun buildBrowsableItem(id: String, title: String, type: Int): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(type)
                    .setArtworkUri(artworkUri)
                    .build()
            )
            .build()

    private fun buildPlayableItem(id: String, title: String, uri: Uri): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setArtworkUri(artworkUri)
                    .build()
            )
            .build()
}
