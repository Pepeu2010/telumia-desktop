package com.nuvio.app.features.profiles

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileAvatarUrlTest {
    @Test
    fun acceptsTheSupportedWebpAvatarUrlAndUsesItOverAnOldStudioImage() {
        val customUrl = "https://cdn.xperience-app.com/avatars/nuvio/baymax-profile-avatar.webp"

        assertEquals(customUrl, normalizedAvatarUrl(customUrl))
        assertEquals(
            customUrl,
            resolveProfileAvatarImageUrl(
                customAvatarUrl = customUrl,
                studioAvatarUrl = "file:///old-profile-avatar.png",
                catalogAvatarUrl = "https://example.com/default-avatar.png",
            ),
        )
    }

    @Test
    fun invalidCustomUrlFallsBackToTheExistingStudioOrCatalogAvatar() {
        assertEquals(
            "file:///profile-studio.png",
            resolveProfileAvatarImageUrl("not a URL", "file:///profile-studio.png", "https://example.com/default.png"),
        )
        assertEquals(
            "https://example.com/default.png",
            resolveProfileAvatarImageUrl("not a URL", null, "https://example.com/default.png"),
        )
        assertNull(resolveProfileAvatarImageUrl("not a URL", null, null))
    }
}
