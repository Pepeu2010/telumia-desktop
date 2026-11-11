package com.nuvio.app.features.livetv

import androidx.compose.runtime.Composable

@Composable
internal expect fun PlatformLiveTvGuide(onPlay: (LiveTvChannel) -> Unit)
