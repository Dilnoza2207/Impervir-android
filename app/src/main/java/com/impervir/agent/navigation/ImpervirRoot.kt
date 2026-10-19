package com.impervir.agent.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.impervir.agent.data.ImpervirRepository
import com.impervir.agent.feature.analytics.AnalyticsScreen
import com.impervir.agent.feature.calendar.CalendarScreen
import com.impervir.agent.feature.editor.PostEditorScreen
import com.impervir.agent.feature.home.HomeScreen
import com.impervir.agent.feature.inbox.InboxScreen
import com.impervir.agent.feature.onboarding.OnboardingScreen
import com.impervir.agent.feature.postdetail.PostDetailScreen
import com.impervir.agent.feature.posts.PostsScreen
import com.impervir.agent.feature.report.ReportScreen
import com.impervir.agent.feature.settings.SettingsScreen
import com.impervir.agent.feature.talk.TalkScreen
import com.impervir.agent.feature.voice.VoiceScreen
import com.impervir.agent.feature.writing.WritingScreen
import com.impervir.agent.feature.yourvoice.YourVoiceScreen
import kotlin.reflect.KClass

private data class Tab(val route: Destination, val type: KClass<*>, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab(Destination.Home, Destination.Home::class, "Home", Icons.Outlined.Home, Icons.Rounded.Home),
    Tab(Destination.Talk, Destination.Talk::class, "Talk", Icons.Outlined.Mic, Icons.Rounded.Mic),
    Tab(Destination.Posts, Destination.Posts::class, "Posts", Icons.Outlined.TaskAlt, Icons.Rounded.TaskAlt),
    Tab(Destination.Inbox, Destination.Inbox::class, "Inbox", Icons.Outlined.ChatBubbleOutline, Icons.Rounded.ChatBubble),
    Tab(Destination.Analytics, Destination.Analytics::class, "Analytics", Icons.Outlined.BarChart, Icons.Rounded.BarChart),
)

private class NavControllerNavigator(private val nav: NavHostController) : Navigator {
    override fun open(destination: Destination) = nav.navigate(destination) { launchSingleTop = true }
    override fun back() { nav.popBackStack() }
    override fun switchTab(destination: Destination) = nav.navigate(destination) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun ImpervirRoot(repository: ImpervirRepository, startOnboarding: Boolean = false) {
    val nav = rememberNavController()
    val navigator = remember(nav) { NavControllerNavigator(nav) }
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination
    val showBar = tabs.any { t -> current?.hierarchy?.any { it.hasRoute(t.type) } == true }
    val comments by repository.comments.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    tabs.forEach { tab ->
                        val selected = current?.hierarchy?.any { it.hasRoute(tab.type) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                navigator.switchTab(tab.route)
                            },
                            icon = {
                                BadgedBox(badge = {
                                    if (tab.route == Destination.Inbox && comments.isNotEmpty()) Badge { Text("${comments.size}") }
                                }) { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) }
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = nav,
                startDestination = if (startOnboarding) Destination.Onboarding else Destination.Home,
                modifier = Modifier.fillMaxSize(),
                // Material shared-axis X for push/pop; predictive back drives the pop animation.
                enterTransition = { slideInHorizontally(tween(320)) { it / 6 } + fadeIn(tween(320)) },
                exitTransition = { slideOutHorizontally(tween(320)) { -it / 10 } + fadeOut(tween(200)) },
                popEnterTransition = { slideInHorizontally(tween(320)) { -it / 10 } + fadeIn(tween(320)) },
                popExitTransition = { slideOutHorizontally(tween(320)) { it / 6 } + fadeOut(tween(200)) },
            ) {
                composable<Destination.Onboarding> { OnboardingScreen(repository, navigator) }
                composable<Destination.Home> { HomeScreen(repository, navigator, padding) }
                composable<Destination.Talk> { TalkScreen(repository, navigator, padding) }
                composable<Destination.Voice> { VoiceScreen(repository, navigator) }
                composable<Destination.Writing> { WritingScreen(repository, navigator) }
                composable<Destination.Posts> { PostsScreen(repository, navigator, padding) }
                composable<Destination.PostEditor> { PostEditorScreen(repository, navigator, it.toRoute<Destination.PostEditor>().postId) }
                composable<Destination.Calendar> { CalendarScreen(repository, navigator) }
                composable<Destination.Inbox> { InboxScreen(repository, navigator, padding) }
                composable<Destination.Analytics> { AnalyticsScreen(repository, navigator, padding) }
                composable<Destination.PostDetail> { PostDetailScreen(repository, navigator, it.toRoute<Destination.PostDetail>().postId) }
                composable<Destination.Report> { ReportScreen(repository, navigator) }
                composable<Destination.YourVoice> { YourVoiceScreen(repository, navigator) }
                composable<Destination.Settings> { SettingsScreen(repository, navigator) }
            }
        }
    }
}
