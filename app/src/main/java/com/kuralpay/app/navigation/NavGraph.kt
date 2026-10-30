package com.kuralpay.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kuralpay.app.account.AuthScreen
import com.kuralpay.app.analytics.ui.AnalyticsScreen
import com.kuralpay.app.forwarding.ForwardingScreen
import com.kuralpay.app.assistant.ui.AssistantScreen
import com.kuralpay.app.settings.ui.AboutScreen
import com.kuralpay.app.settings.ui.SettingsScreen
import com.kuralpay.app.summary.ui.SummarySettingsScreen
import com.kuralpay.app.ui.components.kpClickable
import com.kuralpay.app.ui.dashboard.DashboardScreen
import com.kuralpay.app.ui.theme.Kp

object Routes {
    const val DASHBOARD = "dashboard"
    const val ANALYTICS = "analytics"
    const val SETTINGS = "settings"
    const val ASSISTANT = "assistant"
    const val SUMMARY = "summary"
    const val ABOUT = "about"
    const val FORWARDING = "forwarding"
    const val AUTH = "auth"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val Tabs = listOf(
    Tab(Routes.DASHBOARD, "Home", Icons.Rounded.Home),
    Tab(Routes.ANALYTICS, "Analytics", Icons.Rounded.BarChart),
    Tab(Routes.ASSISTANT, "Ask", Icons.Rounded.Mic),
    Tab(Routes.SETTINGS, "Settings", Icons.Rounded.Settings)
)

@Composable
fun KuralPayNavGraph(
    navController: NavHostController
) {
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    val showBar = Tabs.any { tab -> current?.hierarchy?.any { it.route == tab.route } == true }

    fun goTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(Modifier.fillMaxSize().background(Kp.colors.ink)) {
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            enterTransition = { fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.985f) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(260)) },
            popExitTransition = { fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.985f) }
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onNavigateToAnalytics = { goTab(Routes.ANALYTICS) },
                    onNavigateToAssistant = { goTab(Routes.ASSISTANT) },
                    onNavigateToSummary = { navController.navigate(Routes.SUMMARY) },
                    onNavigateToForwarding = { navController.navigate(Routes.FORWARDING) }
                )
            }
            composable(Routes.ANALYTICS) { AnalyticsScreen() }
            composable(Routes.ASSISTANT) { AssistantScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onNavigateToSummary = { navController.navigate(Routes.SUMMARY) },
                    onNavigateToForwarding = { navController.navigate(Routes.FORWARDING) },
                    onNavigateToAbout = { navController.navigate(Routes.ABOUT) }
                )
            }
            composable(
                Routes.FORWARDING,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(320)) + fadeIn() },
                popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) + fadeOut() }
            ) {
                ForwardingScreen(
                    onBack = { navController.popBackStack() },
                    onSignIn = { navController.navigate(Routes.AUTH) }
                )
            }
            composable(
                Routes.AUTH,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(320)) + fadeIn() },
                popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) + fadeOut() }
            ) {
                AuthScreen(
                    onBack = { navController.popBackStack() },
                    onSignedIn = { navController.popBackStack() }
                )
            }
            composable(
                Routes.ABOUT,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(320)) + fadeIn() },
                popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) + fadeOut() }
            ) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Routes.SUMMARY,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(320)) + fadeIn() },
                popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) + fadeOut() }
            ) {
                SummarySettingsScreen(onBack = { navController.popBackStack() })
            }
        }

        AnimatedVisibility(
            visible = showBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            BottomBar(
                selected = Tabs.firstOrNull { tab -> current?.hierarchy?.any { it.route == tab.route } == true }?.route,
                onSelect = ::goTab
            )
        }
    }
}

@Composable
private fun BottomBar(selected: String?, onSelect: (String) -> Unit) {
    val c = Kp.colors
    val shape = RoundedCornerShape(26.dp)
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(68.dp)
            .clip(shape)
            .background(c.deck)
            .border(1.dp, c.line, shape)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Tabs.forEach { tab ->
            val on = tab.route == selected
            val tint by animateColorAsState(if (on) c.royal else c.muted, tween(220), label = "tabTint")
            val pill by animateColorAsState(if (on) c.royal.copy(alpha = 0.16f) else c.royal.copy(alpha = 0f), tween(220), label = "tabPill")
            Column(
                Modifier.weight(1f).kpClickable { onSelect(tab.route) }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    Modifier.width(54.dp).height(30.dp).clip(RoundedCornerShape(15.dp)).background(pill),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                }
                Text(tab.label, style = MaterialTheme.typography.labelSmall, color = tint)
            }
        }
    }
}
