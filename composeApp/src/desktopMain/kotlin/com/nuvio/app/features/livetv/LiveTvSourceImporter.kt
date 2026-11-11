package com.nuvio.app.features.livetv

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class LiveTvSourceImporter(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun load(source: LiveTvSource): ImportedLiveTvSource {
        val playlist = fetch(source.playlistUrl, M3uPlaylistParser.MAX_PLAYLIST_CHARS)
        val channels = withContext(Dispatchers.Default) {
            M3uPlaylistParser.parse(playlist, source.id).channels
        }
        val guideConfigured = !source.epgUrl.isNullOrBlank()
        val programs = try {
            source.epgUrl?.let { url ->
                val document = fetch(url, XmlTvParser.MAX_DOCUMENT_CHARS)
                withContext(Dispatchers.Default) { XmlTvParser.parse(document, source.id).programs }
            }.orEmpty()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return ImportedLiveTvSource(
                channels = channels,
                programs = emptyList(),
                guideFailed = true,
                guideConfigured = true,
            )
        }
        return ImportedLiveTvSource(
            channels = channels,
            programs = programs,
            guideConfigured = guideConfigured,
            guideUpdatedAtEpochMillis = currentTimeMillis().takeIf { guideConfigured },
        )
    }

    private suspend fun fetch(url: String, limit: Int): String = suspendCancellableCoroutine { continuation ->
        val request = try {
            Request.Builder().url(url).build()
        } catch (_: IllegalArgumentException) {
            continuation.resumeWithException(IOException("URL de fonte inválida"))
            return@suspendCancellableCoroutine
        }
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(IOException("Não foi possível carregar a fonte"))
            }

            override fun onResponse(call: Call, response: Response) {
                val result = runCatching {
                    response.use {
                        if (!it.isSuccessful) throw IOException("A fonte respondeu com HTTP ${it.code}")
                        val body = it.body ?: throw IOException("A fonte retornou uma resposta vazia")
                        if (body.contentLength() > limit) throw IOException("A fonte excede o tamanho permitido")
                        val bytes = body.byteStream().readNBytes(limit + 1)
                        if (bytes.size > limit) throw IOException("A fonte excede o tamanho permitido")
                        bytes.toString(Charsets.UTF_8)
                    }
                }
                continuation.resumeWith(result.fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { Result.failure(IOException("Não foi possível ler a fonte configurada")) },
                ))
            }
        })
    }
}

internal data class ImportedLiveTvSource(
    val channels: List<LiveTvChannel>,
    val programs: List<LiveTvProgram>,
    val guideFailed: Boolean = false,
    val guideConfigured: Boolean = false,
    val guideUpdatedAtEpochMillis: Long? = null,
)
