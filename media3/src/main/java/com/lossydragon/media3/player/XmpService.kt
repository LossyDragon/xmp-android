package com.lossydragon.media3.player

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.lossydragon.media3.MainActivity
import com.lossydragon.media3.R
import com.lossydragon.media3.core.AutoMediaId
import com.lossydragon.media3.data.XmpPreferences
import kotlinx.coroutines.runBlocking
import org.helllabs.libxmp.Xmp
import org.helllabs.libxmp.model.ModInfo
import org.koin.android.ext.android.inject
import timber.log.Timber

@OptIn(UnstableApi::class)
class XmpService : MediaLibraryService() {

    private companion object {
        private const val NOTIFICATION_ID = 669
        private const val CHANNEL_ID = "669"
        private const val ROOT_ID = "xmp_root"
    }

    private val player: XmpPlayer by inject()
    private val prefs: XmpPreferences by inject()
    private lateinit var mediaLibrarySession: MediaLibrarySession

    private val artworkUri by lazy {
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
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId(AutoMediaId.ROOT)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Xmp Player")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

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

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            Timber.d("Auto onSetMediaItems count=${mediaItems.size} startIndex=$startIndex")
            mediaItems.forEach {
                Timber.d("Auto item mediaId=${it.mediaId} uri=${it.localConfiguration?.uri}")
            }
            return super.onSetMediaItems(
                mediaSession,
                controller,
                mediaItems,
                startIndex,
                startPositionMs
            )
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            Timber.d("Auto onAddMediaItems count=${mediaItems.size}")

            val resolved = mediaItems.map { item ->
                val uri = when {
                    AutoMediaId.isFile(item.mediaId) ->
                        AutoMediaId.uriFromFile(item.mediaId).toUri()

                    item.localConfiguration?.uri != null ->
                        item.localConfiguration!!.uri

                    else -> null
                }
                Timber.d("Auto resolving mediaId=${item.mediaId} uri=$uri")
                if (uri != null) {
                    item.buildUpon()
                        .setUri(uri)
                        .build()
                } else {
                    item
                }
            }

            return Futures.immediateFuture(resolved)
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // Return empty — no resumption support yet
            return Futures.immediateFailedFuture(UnsupportedOperationException())
        }
    }

    override fun onCreate() {
        super.onCreate()

        val sessionId = getString(R.string.app_name) + "_session"
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, libraryCallback)
            .setId(sessionId)
            .setSessionActivity(sessionActivity)
            .build()

        DefaultMediaNotificationProvider.Builder(this)
            .setNotificationId(NOTIFICATION_ID)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .build()
            .also(::setMediaNotificationProvider)

        // Handle the case where foreground service can't start (API 31+)
        object : Listener {
            override fun onForegroundServiceStartNotAllowedException() {
                Timber.e("Foreground service start not allowed")
            }
        }.also(::setListener)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaLibrarySession

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

    // region Android Auto
    private fun getRootChildren(): ImmutableList<MediaItem> = ImmutableList.of(
        buildBrowsableItem(
            id = AutoMediaId.FILE_BROWSER,
            title = "File Browser",
            type = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
        ),
        buildBrowsableItem(
            id = AutoMediaId.PLAYLISTS,
            title = "Playlists",
            type = MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS,
        ),
    )

    // TODO
    private fun getPlaylists(): ImmutableList<MediaItem> = ImmutableList.of(
        buildBrowsableItem(
            id = "playlists_coming_soon",
            title = "Coming Soon",
            type = MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS,
        )
    )

    private fun getFileBrowserRoot(): ImmutableList<MediaItem> {
        val rootUriStr = runBlocking { prefs.getLastDirectoryUri() } ?: return ImmutableList.of()
        val treeUri = rootUriStr.toUri()

        // Ensure we have permission
        try {
            contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            Timber.e(e, "No permission for $treeUri")
            return ImmutableList.of()
        }

        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)
        return buildChildItems(treeUri, childrenUri)
    }

    private fun getDirectoryChildren(parentId: String): ImmutableList<MediaItem> {
        Timber.d("Auto getDirectoryChildren parentId=$parentId")
        val treeUri = AutoMediaId.treeUriFromDir(parentId)
        val docId = AutoMediaId.docIdFromDir(parentId)
        Timber.d("Auto treeUri=$treeUri docId=$docId")
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        Timber.d("Auto childrenUri=$childrenUri")
        return buildChildItems(treeUri, childrenUri)
    }

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
                    mime == DocumentsContract.Document.MIME_TYPE_DIR -> {
                        items.add(
                            buildBrowsableItem(
                                id = AutoMediaId.dir(treeUri, docId), // ← encode both
                                title = name,
                                type = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                            )
                        )
                    }

                    Xmp.testFromFd(this, childUri, ModInfo()) -> {
                        items.add(
                            buildPlayableItem(
                                id = AutoMediaId.file(childUri.toString()),
                                title = name,
                                uri = childUri,
                            )
                        )
                    }
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
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(uri)
                    .build()
            )
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
