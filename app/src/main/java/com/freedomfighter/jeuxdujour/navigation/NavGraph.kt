package com.freedomfighter.jeuxdujour.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.freedomfighter.jeuxdujour.ui.connexions.ConnexionsScreen
import com.freedomfighter.jeuxdujour.ui.hexagone.HexagoneScreen
import com.freedomfighter.jeuxdujour.ui.home.HomeScreen
import com.freedomfighter.jeuxdujour.ui.lemot.LeMotScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToLeMot = { navController.navigate(Screen.LeMot.route) },
                onNavigateToHexagone = { navController.navigate(Screen.Hexagone.route) },
                onNavigateToConnexions = { navController.navigate(Screen.Connexions.route) }
            )
        }
        composable(Screen.LeMot.route) {
            LeMotScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Hexagone.route) {
            HexagoneScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Connexions.route) {
            ConnexionsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
