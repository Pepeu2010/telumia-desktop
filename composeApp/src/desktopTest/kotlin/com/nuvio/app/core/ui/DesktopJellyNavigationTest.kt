package com.nuvio.app.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.awt.SwingWindow
import com.nuvio.app.runDesktopMotionApplication
import kotlinx.coroutines.currentCoroutineContext
import com.nuvio.app.AppScreenTab
import com.nuvio.app.DesktopHoverSidebar
import com.nuvio.app.DesktopSidebarExpandedWidth
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_nav_search
import nuvio.composeapp.generated.resources.compose_nav_library
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import org.junit.Rule
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertSame

@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class DesktopJellyNavigationTest {
    private val runtimeDurationScale = UiAnimationDurationScale()
    @get:Rule
    val compose = createComposeRule(effectContext = runtimeDurationScale)
    private val selected = mutableIntStateOf(0)
    private val clicks = mutableListOf<Int>()
    private var profileOpened = false

    @Test
    fun rawComposeAnimationsStopAndInfiniteTransitionsResumeWithoutRemounting() {
        compose.mainClock.autoAdvance = false
        runtimeDurationScale.mode = NavigationMotion.OFF
        val finite = Animatable(0f)
        var loop: State<Float>? = null
        var observedContext: MotionDurationScale? = null
        compose.setContent {
            loop = rememberInfiniteTransition().animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1_000, easing = LinearEasing)),
            )
            LaunchedEffect(Unit) {
                observedContext = currentCoroutineContext()[MotionDurationScale]
                finite.animateTo(1f, tween(1_000))
            }
            Box(Modifier.size(32.dp))
        }
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle {
            assertSame(runtimeDurationScale, observedContext)
            assertEquals(1f, finite.value)
            assertEquals(1f, loop!!.value)
            runtimeDurationScale.mode = NavigationMotion.FULL
        }
        compose.mainClock.advanceTimeBy(320)
        compose.runOnIdle {
            assertTrue(loop!!.value in 0.01f..0.99f, "Infinite transition must resume after Off -> Full")
            runtimeDurationScale.mode = NavigationMotion.OFF
        }
        compose.mainClock.advanceTimeBy(1_600)
        compose.runOnIdle {
            assertEquals(1f, loop!!.value)
            runtimeDurationScale.mode = NavigationMotion.FULL
            runtimeDurationScale.systemScale = 0f
        }
        compose.mainClock.advanceTimeBy(320)
        compose.runOnIdle {
            assertEquals(1f, loop!!.value)
            runtimeDurationScale.systemScale = 2f
        }
        compose.mainClock.advanceTimeBy(320)
        compose.runOnIdle {
            assertTrue(loop!!.value in 0.01f..0.99f, "System motion must also resume without remounting")
        }
    }

    @Test
    fun disablingRuntimeMotionFinishesAnAlreadyRunningRawTween() {
        compose.mainClock.autoAdvance = false
        val finite = Animatable(0f)
        compose.setContent {
            LaunchedEffect(Unit) { finite.animateTo(1f, tween(2_000, easing = LinearEasing)) }
            Box(Modifier.size(32.dp))
        }
        compose.mainClock.advanceTimeBy(320)
        compose.runOnIdle {
            assertTrue(finite.value in 0.01f..0.99f)
            runtimeDurationScale.mode = NavigationMotion.OFF
        }
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle { assertEquals(1f, finite.value) }
    }

    @org.junit.Test(timeout = 20_000)
    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    fun productionApplicationRunnerPassesMotionContextToItsSwingWindowAndCloses() {
        val scale = UiAnimationDurationScale(NavigationMotion.OFF)
        var windowEffectRan = false
        runDesktopMotionApplication(scale) {
            SwingWindow(onCloseRequest = ::exitApplication, title = "Telumia runtime QA", init = {}) {
                LaunchedEffect(Unit) {
                    assertSame(scale, currentCoroutineContext()[MotionDurationScale])
                    windowEffectRan = true
                    exitApplication()
                }
            }
        }
        assertTrue(windowEffectRan)
    }

    @Test
    fun disabledSidebarKeepsMouseTargetAndKeyboardDestinations() {
        var searchLabel = ""
        var libraryLabel = ""
        compose.setContent {
            searchLabel = stringResource(Res.string.compose_nav_search)
            libraryLabel = stringResource(Res.string.compose_nav_library)
            NuvioTheme(navigationMotion = NavigationMotion.OFF, animationIntensity = AnimationIntensity.CINEMATIC) {
                Box(Modifier.size(720.dp, 560.dp).background(Color(0xFF151619))) {
                    DesktopHoverSidebar(
                        selectedTab = AppScreenTab.entries[selected.intValue],
                        onTabSelected = { tab ->
                            clicks += tab.ordinal
                            selected.intValue = tab.ordinal
                        },
                        onProfileSelected = {},
                        onAddProfileRequested = {},
                        sidebarExpanded = true,
                        sidebarWidth = DesktopSidebarExpandedWidth,
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Telumia").assertIsDisplayed()
        val search = compose.onNodeWithContentDescription(searchLabel)
        val beforeHover = search.fetchSemanticsNode().boundsInRoot
        search.performMouseInput { enter(center) }
        compose.waitForIdle()
        assertEquals(beforeHover, search.fetchSemanticsNode().boundsInRoot)
        search.performMouseInput { click() }
        compose.waitForIdle()
        search.assertIsSelected()

        val library = compose.onNodeWithContentDescription(libraryLabel)
        library.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        library.performKeyInput { pressKey(Key.Enter) }
        compose.waitForIdle()
        library.assertIsSelected()
        compose.runOnIdle {
            assertEquals(listOf(AppScreenTab.Search.ordinal, AppScreenTab.Library.ordinal), clicks)
        }
        savePreview("sidebar-off")
    }

    @Test
    fun disabledAnimationsKeepMouseTargetsStableAndNavigateExactlyOnce() {
        setContent(motion = NavigationMotion.OFF)
        compose.onNodeWithContentDescription("Search").performMouseInput { enter(center); click() }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Search").assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(1), clicks) }
        savePreview("off")
    }

    @Test
    fun mouseSelectionNavigatesOnceAndKeepsDesktopLabels() {
        setContent()
        compose.onNodeWithContentDescription("Library").performMouseInput {
            enter(center)
            click()
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Library").assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(2), clicks) }
        savePreview("expanded")
    }

    @Test
    fun hoveringHomeKeepsItsTargetGeometryStable() {
        setContent()
        compose.waitForIdle()
        val home = compose.onNodeWithContentDescription("Home")
        val compactWidth = home.fetchSemanticsNode().boundsInRoot.width
        savePreview("compact")
        home.performMouseInput { enter(center) }
        compose.waitForIdle()
        val hoveredWidth = home.fetchSemanticsNode().boundsInRoot.width
        assertEquals(compactWidth, hoveredWidth, 2f)
        home.performMouseInput { exit() }
        compose.waitForIdle()
        assertEquals(compactWidth, home.fetchSemanticsNode().boundsInRoot.width, 2f)
    }

    @Test
    fun wideHomeShowsFloatingLabelsAndKeepsProfileAtTheLeft() {
        setContent(withProfile = true, wideItems = true, viewportWidth = 1920.dp, viewportHeight = 120.dp)
        compose.waitForIdle()
        val home = compose.onNodeWithContentDescription("Home")
        val search = compose.onNodeWithContentDescription("Search")
        val library = compose.onNodeWithContentDescription("Library")
        val settings = compose.onNodeWithContentDescription("Settings")
        val profile = compose.onNodeWithContentDescription("Profile")
        home.assertIsDisplayed()
        search.assertIsDisplayed()
        library.assertIsDisplayed()
        settings.assertIsDisplayed()
        profile.assertIsDisplayed()
        assertTrue(profile.fetchSemanticsNode().boundsInRoot.left < home.fetchSemanticsNode().boundsInRoot.left)
        home.performMouseInput { enter(center) }
        compose.waitForIdle()
        assertTrue(home.fetchSemanticsNode().boundsInRoot.width > 70f, "Wide Home navigation should expose its text label")
        savePreview("wide-home")
    }

    @Test
    fun fullHdHomeNavigationCentersTheCapsuleAndKeepsTheProfileAtTheEdge() =
        runDesktopComposeUiTest(width = 1920, height = 120) {
            val labels = listOf("Home", "TV", "Search", "Library", "Settings")
            val icons = listOf(
                Icons.Default.Home,
                Icons.Default.VideoLibrary,
                Icons.Default.Search,
                Icons.Default.VideoLibrary,
                Icons.Default.Settings,
            )
            setContent {
                NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                    Box(Modifier.fillMaxSize().background(Color(0xFF080808))) {
                        DesktopNavigationBar(
                            items = labels.mapIndexed { index, label ->
                                FloatingNavigationItem(
                                    label = label,
                                    selected = index == 0,
                                    onClick = {},
                                    icon = icons[index],
                                )
                            },
                            isHeroEnabled = true,
                            profileSwitcherContent = {
                                Box(
                                    Modifier.size(38.dp)
                                        .background(Color(0xFF252525), CircleShape)
                                        .semantics {
                                            contentDescription = "Profile"
                                            role = Role.Button
                                        },
                                )
                            },
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }
                }
            }
            waitForIdle()
            val homeNode = onNodeWithContentDescription("Home")
            homeNode.performMouseInput { enter(center) }
            waitForIdle()
            val home = homeNode.fetchSemanticsNode().boundsInRoot
            val settings = onNodeWithContentDescription("Settings").fetchSemanticsNode().boundsInRoot
            val profile = onNodeWithContentDescription("Profile").fetchSemanticsNode().boundsInRoot
            assertTrue(home.width > 70f, "Full HD Home navigation should show its label")
            assertTrue(settings.width > 70f, "Full HD Settings navigation should show its label")
            assertTrue(profile.left < home.left, "Profile avatar should remain left of the centered capsule")
            assertTrue(profile.left in 80f..120f, "Profile avatar should align with the left inset in the cinematic reference")
            val capsuleCenter = (home.left + settings.right) / 2f
            assertTrue(kotlin.math.abs(capsuleCenter - 960f) < 8f, "Navigation capsule should be centered on the Full HD viewport")
            val directory = File("build/reports/jelly-navigation").apply { mkdirs() }
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(directory, "wide-home-1920.png"))
        }

    @Test
    fun profileButtonOpensOnClickWithoutNavigatingToSettings() {
        setContent(withProfile = true, motion = NavigationMotion.OFF)
        compose.onNodeWithContentDescription("Profile").performMouseInput { click() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(profileOpened)
            assertTrue(clicks.isEmpty())
            assertEquals(0, selected.intValue)
        }
    }

    @Test
    fun profileButtonOpensFromKeyboardFocusWithoutNavigatingToSettings() {
        setContent(withProfile = true, motion = NavigationMotion.OFF)
        val profile = compose.onNodeWithContentDescription("Profile")
        profile.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        profile.assertIsFocused()
        profile.performKeyInput { pressKey(Key.Enter) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(profileOpened)
            assertTrue(clicks.isEmpty())
            assertEquals(0, selected.intValue)
        }
    }

    @Test
    fun keyboardFocusDrawsAVisibleNavigationItemIndicator() {
        setContent(motion = NavigationMotion.OFF)
        val before = compose.onRoot().captureToImage().toAwtImage()
        val search = compose.onNodeWithContentDescription("Search")
        search.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        search.assertIsFocused()
        compose.waitForIdle()
        val after = compose.onRoot().captureToImage().toAwtImage()
        assertTrue(
            countDifferentPixels(before, after) > 0,
            "Keyboard focus should add a visible indicator to the focused navigation item",
        )
    }

    @Test
    fun settingsRouteRemainsClickableBesideTheProfileButton() {
        setContent(withProfile = true, motion = NavigationMotion.OFF)
        val settings = compose.onNodeWithContentDescription("Settings")
        settings.performMouseInput { click() }
        compose.waitForIdle()
        settings.assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(3), clicks) }
    }

    private fun setContent(
        withProfile: Boolean = false,
        motion: NavigationMotion = NavigationMotion.FULL,
        wideItems: Boolean = false,
        viewportWidth: Dp = 640.dp,
        viewportHeight: Dp = 220.dp,
    ) {
        selected.intValue = 0
        clicks.clear()
        profileOpened = false
        val labels = if (wideItems) {
            listOf("Home", "TV", "Search", "Library", "Settings")
        } else {
            listOf("Home", "Search", "Library", "Settings")
        }
        val icons = if (wideItems) {
            listOf(Icons.Default.Home, Icons.Default.VideoLibrary, Icons.Default.Search, Icons.Default.VideoLibrary, Icons.Default.Settings)
        } else {
            listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.VideoLibrary, Icons.Default.Settings)
        }
        compose.setContent {
            NuvioTheme(navigationMotion = motion) {
                Box(Modifier.size(viewportWidth, viewportHeight).background(Color(0xFF151619))) {
                    DesktopNavigationBar(
                        items = labels.mapIndexed { index, label ->
                            FloatingNavigationItem(
                                label = label,
                                selected = selected.intValue == index,
                                onClick = {
                                    clicks += index
                                    selected.intValue = index
                                },
                                icon = icons[index],
                            )
                        },
                        isHeroEnabled = true,
                        settingsSelected = selected.intValue == labels.lastIndex,
                        profileSwitcherContent = if (withProfile) {
                            {
                                Box(
                                    Modifier.size(26.dp).background(Color.Gray)
                                        .semantics {
                                            contentDescription = "Profile"
                                            role = Role.Button
                                        }
                                        .clickable { profileOpened = true }
                                )
                            }
                        } else null,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
        }
    }

    private fun savePreview(name: String) {
        val directory = File("build/reports/jelly-navigation").apply { mkdirs() }
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", File(directory, "$name.png"))
    }

    private fun countDifferentPixels(before: BufferedImage, after: BufferedImage): Int {
        var count = 0
        for (y in 0 until minOf(before.height, after.height)) {
            for (x in 0 until minOf(before.width, after.width)) {
                if (before.getRGB(x, y) != after.getRGB(x, y)) count++
            }
        }
        return count
    }
}
