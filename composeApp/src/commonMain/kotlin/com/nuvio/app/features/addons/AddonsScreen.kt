package com.nuvio.app.features.addons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.isDesktop
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.core.ui.NuvioIconActionButton
import com.nuvio.app.core.ui.NuvioInfoBadge
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioStatusModal
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddonsScreen(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
) {
    NuvioScreen(modifier = modifier) {
        stickyHeader {
            NuvioScreenHeader(
                title = title ?: stringResource(Res.string.addon_title),
                onBack = onBack,
            ) {
            }
        }
        item {
            AddonsSettingsPageContent()
        }
    }
}

@Composable
internal fun AddonsSettingsPageContent(
    modifier: Modifier = Modifier,
) {
    var isInitializing by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        try {
            AddonRepository.initialize()
        } finally {
            isInitializing = false
        }
    }

    val uiState by AddonRepository.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    var addonUrl by rememberSaveable { mutableStateOf("") }
    var formMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var installModalState by remember { mutableStateOf<AddonInstallModalState?>(null) }
    var addonPendingDeletionUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val enterAddonUrlMessage = stringResource(Res.string.addons_error_enter_url)
    val usePersonalMediaCopy = AppFeaturePolicy.personalMediaAddonCopyEnabled

    val overview = remember(uiState.addons) { uiState.addons.toOverview() }
    val installIsBusy = installModalState is AddonInstallModalState.Checking

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nuvio.spacing.listGap),
    ) {
        SectionHeader(stringResource(Res.string.addons_section_overview))
        OverviewCard(overview = overview, isLoading = isInitializing)

        SectionHeader(stringResource(Res.string.addons_section_add_addon))
        AddAddonCard(
            addonUrl = addonUrl,
            formMessage = formMessage,
            usePersonalMediaCopy = usePersonalMediaCopy,
            installIsBusy = installIsBusy,
            onAddonUrlChange = {
                addonUrl = it
                formMessage = null
            },
            onAddClick = {
                val requestedUrl = addonUrl.trim()
                if (requestedUrl.isBlank()) {
                    formMessage = enterAddonUrlMessage
                    return@AddAddonCard
                }

                formMessage = null
                installModalState = AddonInstallModalState.Checking
                coroutineScope.launch {
                    val result = AddonRepository.addAddon(requestedUrl)
                    installModalState = when (result) {
                        is AddAddonResult.Success -> {
                            addonUrl = ""
                            AddonInstallModalState.Success(result.manifest.name)
                        }

                        is AddAddonResult.Error -> {
                            AddonInstallModalState.Error(result.message)
                        }
                    }
                }
            },
        )

        SectionHeader(stringResource(Res.string.addons_section_installed))
        when {
            isInitializing -> LoadingAddonsCard()
            uiState.addons.isEmpty() -> EmptyStateCard(usePersonalMediaCopy = usePersonalMediaCopy)
            else -> {
                val lastIndex = uiState.addons.lastIndex
                uiState.addons.forEachIndexed { index, addon ->
                    val manifest = addon.manifest
                    val behaviorHints = manifest?.behaviorHints
                    val showConfigureAction = behaviorHints?.configurable == true || behaviorHints?.configurationRequired == true
                    val configureUrl = addon.manifestUrl.toConfigureUrl()
                    InstalledAddonCard(
                        addon = addon,
                        onMoveUpClick = if (index > 0) {
                            { AddonRepository.moveAddon(index, index - 1) }
                        } else {
                            null
                        },
                        onMoveDownClick = if (index < lastIndex) {
                            { AddonRepository.moveAddon(index, index + 1) }
                        } else {
                            null
                        },
                        onRefreshClick = {
                            AddonRepository.refreshAddon(
                                manifestUrl = addon.manifestUrl,
                                forceRefresh = true,
                            )
                        },
                        onEnabledChange = { enabled ->
                            AddonRepository.setAddonEnabled(addon.manifestUrl, enabled)
                        },
                        onConfigureClick = if (showConfigureAction && !configureUrl.isNullOrBlank()) {
                            {
                                runCatching {
                                    uriHandler.openUri(configureUrl)
                                }
                            }
                        } else {
                            null
                        },
                        onDeleteClick = { addonPendingDeletionUrl = addon.manifestUrl },
                    )
                }
            }
        }
    }

    val pendingDeletionUrl = addonPendingDeletionUrl
    if (pendingDeletionUrl != null) {
        NuvioStatusModal(
            title = stringResource(Res.string.addons_delete_confirm_title),
            message = stringResource(Res.string.action_delete_confirm_message),
            isVisible = true,
            destructive = true,
            confirmText = stringResource(Res.string.action_yes),
            dismissText = stringResource(Res.string.action_no),
            onConfirm = {
                AddonRepository.removeAddon(pendingDeletionUrl)
                addonPendingDeletionUrl = null
            },
            onDismiss = { addonPendingDeletionUrl = null },
        )
    }

    val modalState = installModalState
    if (modalState != null) {
        val modalTitle = when (modalState) {
            AddonInstallModalState.Checking -> stringResource(Res.string.addons_modal_checking_title)
            is AddonInstallModalState.Success -> stringResource(Res.string.addons_modal_success_title)
            is AddonInstallModalState.Error -> stringResource(Res.string.addons_modal_failure_title)
        }
        val modalMessage = when (modalState) {
            AddonInstallModalState.Checking -> stringResource(Res.string.addons_modal_checking_message)
            is AddonInstallModalState.Success -> stringResource(
                Res.string.addons_modal_success_message,
                modalState.addonName,
            )
            is AddonInstallModalState.Error -> modalState.reason
        }
        val modalConfirmText = when (modalState) {
            AddonInstallModalState.Checking -> stringResource(Res.string.addon_installing)
            is AddonInstallModalState.Success -> stringResource(Res.string.action_done)
            is AddonInstallModalState.Error -> stringResource(Res.string.action_close)
        }
        NuvioStatusModal(
            title = modalTitle,
            message = modalMessage,
            isVisible = true,
            isBusy = modalState.isBusy,
            confirmText = modalConfirmText,
            onConfirm = {
                if (!modalState.isBusy) {
                    installModalState = null
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    NuvioSectionLabel(text = text)
}

@Composable
private fun OverviewCard(
    overview: AddonOverview,
    isLoading: Boolean,
) {
    val tokens = MaterialTheme.nuvio
    NuvioSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OverviewStat(
                value = if (isLoading) "—" else overview.totalAddons.toString(),
                label = stringResource(Res.string.addons_overview_addons),
                modifier = Modifier.weight(1f),
                valueColor = tokens.colors.textPrimary,
            )
            VerticalSeparator()
            OverviewStat(
                value = if (isLoading) "—" else overview.activeAddons.toString(),
                label = stringResource(Res.string.addons_overview_active),
                modifier = Modifier.weight(1f),
                valueColor = tokens.colors.accent,
            )
            VerticalSeparator()
            OverviewStat(
                value = if (isLoading) "—" else overview.totalCatalogs.toString(),
                label = stringResource(Res.string.addons_overview_catalogs),
                modifier = Modifier.weight(1f),
                valueColor = tokens.colors.textPrimary,
            )
        }
    }
}

@Composable
private fun OverviewStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.nuvio.colors.textMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VerticalSeparator() {
    Box(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .width(1.dp)
            .height(56.dp)
            .background(MaterialTheme.nuvio.colors.borderSubtle),
    )
}

@Composable
private fun AddAddonCard(
    addonUrl: String,
    formMessage: String?,
    usePersonalMediaCopy: Boolean,
    installIsBusy: Boolean,
    onAddonUrlChange: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    NuvioSurfaceCard {
        NuvioInputField(
            value = addonUrl,
            onValueChange = onAddonUrlChange,
            placeholder = stringResource(
                if (usePersonalMediaCopy) {
                    Res.string.addons_appstore_input_placeholder
                } else {
                    Res.string.addons_input_placeholder
                },
            ),
        )
        Spacer(modifier = Modifier.height(NuvioTokens.Space.s18))
        NuvioPrimaryButton(
            text = stringResource(
                if (usePersonalMediaCopy) {
                    Res.string.addons_appstore_install_button
                } else {
                    Res.string.addons_install_button
                },
            ),
            enabled = !installIsBusy,
            onClick = onAddClick,
        )
        if (usePersonalMediaCopy) {
            Spacer(modifier = Modifier.height(NuvioTokens.Space.s14))
            Text(
                text = stringResource(Res.string.addons_appstore_add_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        formMessage?.let { message ->
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.nuvio.shapes.compactCard)
                    .background(MaterialTheme.nuvio.colors.danger.copy(alpha = 0.12f))
                    .padding(horizontal = NuvioTokens.Space.s12, vertical = NuvioTokens.Space.s10)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s10),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.nuvio.colors.danger,
                    modifier = Modifier.size(NuvioTokens.Icon.md),
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.nuvio.colors.danger,
                )
            }
        }
    }
}

private sealed interface AddonInstallModalState {
    val isBusy: Boolean

    data object Checking : AddonInstallModalState {
        override val isBusy: Boolean = true
    }

    data class Success(
        val addonName: String,
    ) : AddonInstallModalState {
        override val isBusy: Boolean = false
    }

    data class Error(
        val reason: String,
    ) : AddonInstallModalState {
        override val isBusy: Boolean = false
    }
}

@Composable
private fun LoadingAddonsCard() {
    AddonStateCard(
        icon = Icons.Rounded.Refresh,
        title = stringResource(Res.string.addons_loading_manifest_details),
        message = null,
        tint = MaterialTheme.nuvio.colors.accent,
    )
}

@Composable
private fun EmptyStateCard(
    usePersonalMediaCopy: Boolean,
) {
    AddonStateCard(
        icon = Icons.Rounded.Extension,
        title = stringResource(
            if (usePersonalMediaCopy) {
                Res.string.addons_appstore_empty_title
            } else {
                Res.string.addons_empty_title
            },
        ),
        message = stringResource(
            if (usePersonalMediaCopy) {
                Res.string.addons_appstore_empty_subtitle
            } else {
                Res.string.addons_empty_subtitle
            },
        ),
        tint = MaterialTheme.nuvio.colors.accent,
    )
}

@Composable
private fun AddonStateCard(
    icon: ImageVector,
    title: String,
    message: String?,
    tint: Color,
) {
    val tokens = MaterialTheme.nuvio
    NuvioSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(NuvioTokens.Space.s56)
                    .clip(tokens.shapes.compactCard)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(NuvioTokens.Icon.lg),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s6),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.colors.textPrimary,
                )
                message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun InstalledAddonCard(
    addon: ManagedAddon,
    onMoveUpClick: (() -> Unit)?,
    onMoveDownClick: (() -> Unit)?,
    onRefreshClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onConfigureClick: (() -> Unit)?,
    onDeleteClick: () -> Unit,
) {
    val manifest = addon.manifest
    val tokens = MaterialTheme.nuvio
    val cardInteraction = remember { MutableInteractionSource() }
    val hovered by cardInteraction.collectIsHoveredAsState()
    val hoverBorder = isDesktop && hovered

    NuvioSurfaceCard(
        modifier = Modifier
            .focusGroup()
            .hoverable(cardInteraction)
            .border(
                width = NuvioTokens.Border.thin,
                color = if (hoverBorder) tokens.colors.accent.copy(alpha = 0.72f) else tokens.colors.borderSubtle,
                shape = tokens.shapes.card,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AddonIconBadge(
                imageUrl = manifest?.logoUrl,
                icon = Icons.Rounded.Extension,
                tint = if (manifest != null) tokens.colors.accent else tokens.colors.textMuted,
            )
            Spacer(modifier = Modifier.width(tokens.spacing.controlGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = addon.displayTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                manifest?.version?.let { version ->
                    Spacer(modifier = Modifier.height(NuvioTokens.Space.s6))
                    Text(
                        text = stringResource(Res.string.addons_version_format, version),
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.colors.textMuted,
                    )
                }
            }
            Spacer(modifier = Modifier.width(NuvioTokens.Space.s8))
            Switch(
                checked = addon.enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.semantics {
                    contentDescription = addon.displayTitle
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = tokens.colors.onAccent,
                    checkedTrackColor = tokens.colors.accent,
                    uncheckedThumbColor = tokens.colors.textMuted,
                    uncheckedTrackColor = tokens.colors.borderDefault,
                ),
            )
        }

        Spacer(modifier = Modifier.height(tokens.spacing.controlGap))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s4),
        ) {
            onMoveUpClick?.let { onMoveUp ->
                NuvioIconActionButton(
                    icon = Icons.Rounded.ArrowUpward,
                    contentDescription = stringResource(Res.string.addons_move_up),
                    tint = tokens.colors.textMuted,
                    onClick = onMoveUp,
                )
            }
            onMoveDownClick?.let { onMoveDown ->
                NuvioIconActionButton(
                    icon = Icons.Rounded.ArrowDownward,
                    contentDescription = stringResource(Res.string.addons_move_down),
                    tint = tokens.colors.textMuted,
                    onClick = onMoveDown,
                )
            }
            NuvioIconActionButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = stringResource(Res.string.addons_refresh),
                tint = tokens.colors.accent,
                onClick = onRefreshClick,
            )
            onConfigureClick?.let { onConfigure ->
                NuvioIconActionButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = stringResource(Res.string.addons_configure),
                    tint = tokens.colors.accentStrong,
                    onClick = onConfigure,
                )
            }
            NuvioIconActionButton(
                icon = Icons.Rounded.Delete,
                contentDescription = stringResource(Res.string.addons_delete),
                tint = tokens.colors.danger,
                onClick = onDeleteClick,
            )
        }

        Spacer(modifier = Modifier.height(NuvioTokens.Space.s8))
        HorizontalDivider(color = tokens.colors.borderSubtle)
        Spacer(modifier = Modifier.height(tokens.spacing.controlGap))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
        ) {
            AddonStatusBadge(
                text = when {
                    !addon.enabled -> stringResource(Res.string.addons_badge_disabled)
                    addon.isRefreshing -> stringResource(Res.string.addons_badge_refreshing)
                    manifest != null -> stringResource(Res.string.addons_badge_active)
                    else -> stringResource(Res.string.addons_badge_unavailable)
                },
                color = when {
                    !addon.enabled -> tokens.colors.textMuted
                    addon.isRefreshing -> tokens.colors.info
                    manifest != null -> tokens.colors.success
                    else -> tokens.colors.danger
                },
            )
            manifest?.let {
                NuvioInfoBadge(text = stringResource(Res.string.addons_badge_resources, it.resources.size))
                NuvioInfoBadge(text = stringResource(Res.string.addons_badge_catalogs, it.catalogs.size))
                if (it.behaviorHints.configurable || it.behaviorHints.configurationRequired) {
                    NuvioInfoBadge(text = stringResource(Res.string.addons_badge_configurable))
                }
            }
        }

        when {
            manifest == null && addon.isRefreshing -> {
                Spacer(modifier = Modifier.height(NuvioTokens.Space.s14))
                Text(
                    text = stringResource(Res.string.addons_loading_manifest_details),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                )
            }

            manifest == null && !addon.errorMessage.isNullOrBlank() -> {
                Spacer(modifier = Modifier.height(NuvioTokens.Space.s14))
                AddonErrorMessage(addon.errorMessage.orEmpty())
            }

            manifest != null -> {
                Spacer(modifier = Modifier.height(NuvioTokens.Space.s14))
                manifest.description.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textSecondary,
                    )

                    Spacer(modifier = Modifier.height(NuvioTokens.Space.s10))
                }

                Text(
                    text = manifestSummary(manifest),
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textMuted,
                )

                addon.errorMessage?.takeIf(String::isNotBlank)?.let { staleError ->
                    Spacer(modifier = Modifier.height(NuvioTokens.Space.s14))
                    AddonErrorMessage(staleError)
                }
            }
        }
    }
}

@Composable
private fun AddonStatusBadge(
    text: String,
    color: Color,
) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.13f), MaterialTheme.nuvio.shapes.chip)
            .padding(horizontal = NuvioTokens.Space.s10, vertical = NuvioTokens.Space.s6),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddonErrorMessage(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.nuvio.shapes.compactCard)
            .background(MaterialTheme.nuvio.colors.danger.copy(alpha = 0.12f))
            .padding(horizontal = NuvioTokens.Space.s12, vertical = NuvioTokens.Space.s10)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s10),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.nuvio.colors.danger,
            modifier = Modifier
                .padding(top = NuvioTokens.Space.s2)
                .size(NuvioTokens.Icon.md),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.nuvio.colors.danger,
        )
    }
}

@Composable
private fun AddonIconBadge(
    imageUrl: String?,
    icon: ImageVector,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .size(NuvioTokens.Space.s64)
            .clip(MaterialTheme.nuvio.shapes.compactCard)
            .background(MaterialTheme.nuvio.colors.surfaceCard),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(NuvioTokens.Icon.xl),
            )
        }
    }
}

@Composable
private fun manifestSummary(manifest: AddonManifest): String {
    val resources = manifest.resources.joinToString(separator = ", ") { it.name }
    val types = manifest.types.joinToString(separator = " / ") { it.replaceFirstChar(Char::uppercase) }
    return buildList {
        if (types.isNotBlank()) add(types)
        if (resources.isNotBlank()) add(resources)
        if (manifest.idPrefixes.isNotEmpty()) {
            add(stringResource(Res.string.addons_summary_id_rules, manifest.idPrefixes.size))
        }
        if (manifest.behaviorHints.p2p) add("P2P")
    }.joinToString(separator = " • ")
}

private fun String.toConfigureUrl(): String {
    val base = substringBefore("?").trimEnd('/')
    return if (base.endsWith("/manifest.json")) {
        base.removeSuffix("/manifest.json") + "/configure"
    } else {
        "$base/configure"
    }
}
