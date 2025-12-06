package com.intokapp.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.intokapp.app.ui.screens.auth.LoginScreen
import com.intokapp.app.ui.screens.auth.SetupScreen
import com.intokapp.app.ui.screens.chat.ChatScreen
import com.intokapp.app.ui.screens.conversations.ConversationsScreen
import com.intokapp.app.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Setup : Screen("setup")
    object Conversations : Screen("conversations")
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    object NewChat : Screen("new_chat")
    object Settings : Screen("settings")
}

@Composable
fun IntokNavigation() {
    val navController = rememberNavController()
    
    // TODO: Check auth state to determine start destination
    val startDestination = Screen.Login.route
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { isNewUser ->
                    if (isNewUser) {
                        navController.navigate(Screen.Setup.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Conversations.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable(Screen.Setup.route) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Screen.Conversations.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Conversations.route) {
            ConversationsScreen(
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                },
                onNewChatClick = {
                    navController.navigate(Screen.NewChat.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            ChatScreen(
                conversationId = conversationId,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

