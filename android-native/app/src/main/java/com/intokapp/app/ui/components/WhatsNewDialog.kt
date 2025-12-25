package com.intokapp.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intokapp.app.R
import com.intokapp.app.data.repository.ChangelogEntry
import com.intokapp.app.data.repository.localizedString
import com.intokapp.app.ui.theme.*

@Composable
fun WhatsNewAutoDialog(
    entries: List<ChangelogEntry>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Purple500,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                localizedString(R.string.whats_new_title, "settings.settings_whats_new"),
                fontWeight = FontWeight.Bold,
                color = White
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(entries) { entry ->
                    Column {
                        Text(
                            text = "v${entry.version} - ${entry.title}",
                            fontWeight = FontWeight.SemiBold,
                            color = Purple500,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        entry.changes.forEach { change ->
                            Text(
                                text = change,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Surface300,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(localizedString(R.string.got_it, "common.done"), color = Purple500, fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = Surface800,
        titleContentColor = White,
        textContentColor = Surface300
    )
}
