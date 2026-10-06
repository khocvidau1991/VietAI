package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.MediaStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.musicDataStore by preferencesDataStore(name = "music_playlist")
private val savedPlaylistKey = stringPreferencesKey("saved_playlist")

data class LocalAudioTrack(
    val uri: Uri,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val artworkUri: Uri? = null
)

object LocalMusicRepository {
    fun scanDevice(context: Context): List<LocalAudioTrack> {
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val tracks = mutableListOf<LocalAudioTrack>()
        resolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumColumn)
                tracks += LocalAudioTrack(
                    uri = ContentUris.withAppendedId(collection, id),
                    title = cursor.getString(titleColumn).orEmpty(),
                    artist = cursor.getString(artistColumn).orEmpty(),
                    durationMs = cursor.getLong(durationColumn),
                    artworkUri = if (albumId > 0) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        )
                    } else {
                        null
                    }
                )
            }
        }
        return tracks
    }

    fun trackForUri(context: Context, uri: Uri): LocalAudioTrack {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                return LocalAudioTrack(
                    uri = uri,
                    title = if (nameColumn >= 0) cursor.getString(nameColumn).orEmpty() else "",
                    artist = "",
                    durationMs = 0L
                )
            }
        }
        return LocalAudioTrack(
            uri = uri,
            title = uri.lastPathSegment?.substringAfterLast('/') ?: "Audio",
            artist = "",
            durationMs = 0L
        )
    }

    suspend fun loadPlaylist(context: Context): List<LocalAudioTrack> {
        val saved = context.musicDataStore.data.first()[savedPlaylistKey].orEmpty()
        return try {
            val json = JSONArray(saved)
            (0 until json.length()).mapNotNull { index ->
                val item = json.optJSONObject(index) ?: return@mapNotNull null
                val uri = Uri.parse(item.optString("uri"))
                if (uri.scheme != "content") return@mapNotNull null
                LocalAudioTrack(
                    uri = uri,
                    title = item.optString("title"),
                    artist = item.optString("artist"),
                    durationMs = item.optLong("duration"),
                    artworkUri = item.optString("artwork").takeIf(String::isNotBlank)?.let(Uri::parse)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun savePlaylist(context: Context, tracks: List<LocalAudioTrack>) {
        val json = JSONArray()
        tracks.distinctBy { it.uri.toString() }.forEach { track ->
            json.put(
                JSONObject()
                    .put("uri", track.uri.toString())
                    .put("title", track.title)
                    .put("artist", track.artist)
                    .put("duration", track.durationMs)
                    .put("artwork", track.artworkUri?.toString().orEmpty())
            )
        }
        context.musicDataStore.edit { it[savedPlaylistKey] = json.toString() }
    }

    fun toMediaItem(track: LocalAudioTrack): MediaItem =
        MediaItem.Builder()
            .setMediaId(track.uri.toString())
            .setUri(track.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(track.artworkUri)
                    .build()
            )
            .build()
}
