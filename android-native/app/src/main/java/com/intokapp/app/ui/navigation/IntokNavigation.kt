package com.intokapp.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.intokapp.app.data.repository.AuthState
import com.intokapp.app.ui.screens.auth.LoginScreen
import com.intokapp.app.ui.screens.auth.SetupScreen
import com.intokapp.app.ui.screens.chat.AddParticipantsScreen
import com.intokapp.app.ui.screens.chat.ChatScreen
import com.intokapp.app.ui.screens.chat.RemoveParticipantsScreen
import com.intokapp.app.ui.screens.conversations.ConversationsScreen
import com.intokapp.app.ui.screens.newchat.NewChatScreen
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
    object AddParticipants : Screen("add_participants/{conversationId}") {
        fun createRoute(conversationId: String) = "add_participants/$conversationId"
    }
    object RemoveParticipants : Screen("remove_participants/{conversationId}") {
        fun createRoute(conversationId: String) = "remove_participants/$conversationId"
    }
}

@Composable
fun IntokNavigation(
    viewModel: NavigationViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val authState by viewModel.authState.collectAsState()
    
    // Track previous auth state to only navigate on state transitions
    var previousAuthStateType by remember { mutableStateOf<String?>(null) }
    
    // Initialize auth
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }
    
    // Handle auth state changes - only navigate on state TYPE transitions, not user data updates
    LaunchedEffect(authState) {
        val currentStateType = when (authState) {
            is AuthState.Loading -> "Loading"
            is AuthState.Unauthenticated -> "Unauthenticated"
            is AuthState.Authenticated -> {
                val state = authState as AuthState.Authenticated
                if (state.needsSetup) "NeedsSetup" else "Authenticated"
            }
            is AuthState.Error -> "Error"
        }
        
        // Only navigate if the state TYPE has changed (not just user data)
        if (currentStateType != previousAuthStateType) {
            previousAuthStateType = currentStateType
            
            when (authState) {
                is AuthState.Unauthenticated -> {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                is AuthState.Authenticated -> {
                    val state = authState as AuthState.Authenticated
                    if (state.needsSetup) {
                        navController.navigate(Screen.Setup.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Conversations.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
                else -> {}
            }
        }
    }
    
    // Determine start destination based on initial state
    val startDestination = when (authState) {
        is AuthState.Authenticated -> {
            if ((authState as AuthState.Authenticated).needsSetup) Screen.Setup.route
            else Screen.Conversations.route
        }
        is AuthState.Unauthenticated -> Screen.Login.route
        else -> Screen.Login.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { isNewUser ->
                    // Navigation handled by auth state change
                }
            )
        }
        
        composable(Screen.Setup.route) {
            SetupScreen(
                onSetupComplete = {
                    // Navigation handled by auth state change
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
                onBackClick = { navController.popBackStack() },
                onAddParticipants = { convId ->
                    navController.navigate(Screen.AddParticipants.createRoute(convId))
                },
                onRemoveParticipants = { convId ->
                    navController.navigate(Screen.RemoveParticipants.createRoute(convId))
                }
            )
        }
        
        composable(
            route = Screen.AddParticipants.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            AddParticipantsScreen(
                conversationId = conversationId,
                onBackClick = { navController.popBackStack() },
                onParticipantsAdded = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.RemoveParticipants.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            RemoveParticipantsScreen(
                conversationId = conversationId,
                onBackClick = { navController.popBackStack() },
                onParticipantRemoved = { navController.popBackStack() }
            )
        }
        
        composable(Screen.NewChat.route) {
            NewChatScreen(
                onBackClick = { navController.popBackStack() },
                onChatCreated = { conversationId ->
                    navController.popBackStack()
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = {
                    // Navigation handled by auth state change
                }
            )
        }
    }
}
