package com.intokapp.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.intokapp.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ChangelogEntry(
    val version: String,
    val title: String,
    val changes: List<String>
)

@Singleton
class WhatsNewManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("intok_prefs", Context.MODE_PRIVATE)
    private val lastSeenVersionKey = "last_seen_version"
    
    val currentVersion: String = BuildConfig.VERSION_NAME
    
    val changelog = listOf(
        ChangelogEntry(
            version = "0.1.16",
            title = "Message Management Update",
            changes = listOf(
                "🗑️ Delete individual messages",
                "🗑️ Delete entire conversations",
                "💾 Save images to gallery",
                "📥 Download documents to device"
            )
        ),
        ChangelogEntry(
            version = "0.1.15",
            title = "Stability & Fixes Update",
            changes = listOf(
                "🔐 Fixed Google Sign-In authentication",
                "📤 Fixed image and document uploads",
                "⌨️ Fixed keyboard blocking message input",
                "🔢 Version now displays correctly in Settings"
            )
        ),
        ChangelogEntry(
            version = "0.1.8",
            title = "Profile & Reactions Update",
            changes = listOf(
                "😀 Emoji reactions on messages",
                "↩️ Reply to specific messages",
                "📷 Update profile picture in Settings",
                "🎨 Improved chat UI layout"
            )
        ),
        ChangelogEntry(
            version = "0.1.5",
            title = "Engagement Update",
            changes = listOf(
                "🔔 Push notifications for new messages",
                "🔢 Unread message count badges",
                "✓✓ Read receipts on messages",
                "✨ What's New popup on app updates",
                "🎨 Fixed app icon"
            )
        ),
        ChangelogEntry(
            version = "0.1.4",
            title = "Smart Translation Update",
            changes = listOf(
                "📄 Choose whether to translate documents",
                "🖼️ Images and GIFs skip translation automatically",
                "⚡ Improved real-time messaging performance"
            )
        ),
        ChangelogEntry(
            version = "0.1.3",
            title = "Rich Messaging Update",
            changes = listOf(
                "📸 Share images from gallery",
                "📷 Camera integration",
                "🎬 GIF support via GIPHY",
                "📄 Document sharing",
                "👍 Message reactions"
            )
        ),
        ChangelogEntry(
            version = "0.1.2",
            title = "Messaging Improvements",
            changes = listOf(
                "🔄 Real-time message sync",
                "🌐 Improved translation accuracy",
                "🐛 Bug fixes and stability improvements"
            )
        ),
        ChangelogEntry(
            version = "0.1.1",
            title = "Authentication Update",
            changes = listOf(
                "📧 Email & password login",
                "🔐 Google Sign-In",
                "👤 Profile setup"
            )
        ),
        ChangelogEntry(
            version = "0.1.0",
            title = "Initial Release",
            changes = listOf(
                "💬 Real-time messaging",
                "🌍 Automatic translation",
                "👥 Direct and group chats"
            )
        )
    )
    
    fun shouldShowWhatsNew(): Boolean {
        val lastSeenVersion = prefs.getString(lastSeenVersionKey, null)
        return lastSeenVersion != currentVersion
    }
    
    fun getNewEntries(): List<ChangelogEntry> {
        val lastSeenVersion = prefs.getString(lastSeenVersionKey, null)
        
        return if (lastSeenVersion == null) {
            // First launch - show current version only
            changelog.take(1)
        } else {
            // Find all versions since last seen
            val lastIndex = changelog.indexOfFirst { it.version == lastSeenVersion }
            if (lastIndex > 0) {
                changelog.take(lastIndex)
            } else {
                changelog.take(1)
            }
        }
    }
    
    fun markAsSeen() {
        prefs.edit().putString(lastSeenVersionKey, currentVersion).apply()
    }
}
