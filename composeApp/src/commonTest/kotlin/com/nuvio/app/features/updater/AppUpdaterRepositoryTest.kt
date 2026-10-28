package com.nuvio.app.features.updater

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppUpdaterRepositoryTest {
    @Test
    fun stableChannelUsesGithubLatestReleaseEndpoint() {
        assertEquals("releases/latest", AppUpdaterRepository.releasePath(UpdateChannel.STABLE))
    }

    @Test
    fun stableAcceptsReleasesFromAnyBranch() {
        listOf("main", "master", "cmp-rewrite", "1234567").forEach { branch ->
            val response = """{
                "tag_name": "v1.1.0", "target_commitish": "$branch",
                "assets": [{"name": "app.apk", "browser_download_url": "https://example.com/app.apk"}]
            }"""
            assertEquals("v1.1.0", select(response, UpdateChannel.STABLE)?.tag)
        }
    }

    @Test
    fun stableRejectsLegacyBetaNamesEvenIfGithubMarksThemStable() {
        val response = """{
            "tag_name": "1.1.0", "name": "Beta 1.1.0 Hotfix",
            "assets": [{"name": "app.apk", "browser_download_url": "https://example.com/app.apk"}]
        }"""
        assertNull(select(response, UpdateChannel.STABLE))
    }

    @Test
    fun stableRejectsPrereleaseEvenIfItIsReturnedAsTheLatestPayload() {
        val response = """{
            "tag_name": "1.1.0-alpha.2", "prerelease": true, "assets": [
                {"name": "Telumia.msi", "browser_download_url": "https://example.com/alpha.msi"}]
        }"""

        val update = AppUpdaterRepository.selectUpdate(
            responseBody = response,
            channel = UpdateChannel.STABLE,
            supportedAbis = emptyList(),
            selector = AppUpdateAssetSelector(fileExtensions = listOf(".msi")),
        )

        assertNull(update)
    }

    @Test
    fun stableRejectsAlphaTagEvenWhenGithubDoesNotMarkItPrerelease() {
        val response = """{
            "tag_name": "1.1.0-alpha.2", "assets": [
                {"name": "app.apk", "browser_download_url": "https://example.com/alpha.apk"}]
        }"""

        assertNull(select(response, UpdateChannel.STABLE))
    }

    @Test
    fun apkSelectionPreservesDeviceAbiPreference() {
        val response = """{
            "tag_name": "1.1.0", "assets": [
                {"name": "app-universal.apk", "browser_download_url": "https://example.com/universal.apk"},
                {"name": "app-arm64-v8a.apk", "browser_download_url": "https://example.com/arm64.apk"},
                {"name": "app-x86_64.apk", "browser_download_url": "https://example.com/x86.apk"}
            ]
        }"""
        assertEquals("app-arm64-v8a.apk", select(response, UpdateChannel.STABLE)?.assetName)
        assertEquals(
            "app-universal.apk",
            AppUpdaterRepository.selectUpdate(response, UpdateChannel.STABLE, listOf("armeabi-v7a"))?.assetName,
        )
    }

    private fun select(response: String, channel: UpdateChannel): AppUpdate? =
        AppUpdaterRepository.selectUpdate(response, channel, listOf("arm64-v8a"))
}
