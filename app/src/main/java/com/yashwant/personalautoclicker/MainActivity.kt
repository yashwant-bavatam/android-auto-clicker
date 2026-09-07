package com.yashwant.personalautoclicker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yashwant.personalautoclicker.ui.theme.PersonalAutoClickerTheme

object AutoClickerConfig {
    var tapX: Float = 500f
    var tapY: Float = 1000f
    var intervalMs: Long = 1000L
    var continuousMode: Boolean = true
    var repeatCount: Int = 1
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PersonalAutoClickerTheme {
                AutoClickerScreen(
                    openAccessibilitySettings = {
                        startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        )
                    },
                    openOverlayPermission = {
                        if (!Settings.canDrawOverlays(this)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            startActivity(intent)
                        }
                    },
                    startFloatingControls = {
                        startFloatingControlsIfAllowed()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        startFloatingControlsIfAllowed()
    }

    private fun startFloatingControlsIfAllowed() {
        if (Settings.canDrawOverlays(this)) {
            startService(
                Intent(
                    this,
                    FloatingControlService::class.java
                )
            )
        }
    }
}

@Composable
fun AutoClickerScreen(
    openAccessibilitySettings: () -> Unit,
    openOverlayPermission: () -> Unit,
    startFloatingControls: () -> Unit
) {

    var intervalText by remember {
        mutableStateOf(AutoClickerConfig.intervalMs.toString())
    }

    var continuousMode by remember {
        mutableStateOf(AutoClickerConfig.continuousMode)
    }

    var repeatCountText by remember {
        mutableStateOf(
            AutoClickerConfig.repeatCount
                .coerceAtLeast(1)
                .toString()
        )
    }

    val accessibilityOn = AutoClickerService.instance != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 18.dp,
                vertical = 22.dp
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "Personal Auto Clicker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Create tap and swipe automations, run them over other apps, and save reusable profiles.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        StatusCard(
            accessibilityOn = accessibilityOn,
            openAccessibilitySettings = openAccessibilitySettings
        )

        AutomationSettingsCard(
            intervalText = intervalText,
            onIntervalChange = {
                intervalText = it
            },
            continuousMode = continuousMode,
            onContinuousModeChange = {
                continuousMode = it
            },
            repeatCountText = repeatCountText,
            onRepeatCountChange = {
                repeatCountText = it
            }
        )

        FloatingControlsCard(
            openOverlayPermission = openOverlayPermission,
            showFloatingControls = {

                val interval =
                    intervalText
                        .toLongOrNull()
                        ?.coerceAtLeast(100L)
                        ?: 1000L

                val repeats =
                    repeatCountText
                        .toIntOrNull()
                        ?.coerceAtLeast(1)
                        ?: 1

                AutoClickerConfig.intervalMs = interval
                AutoClickerConfig.continuousMode = continuousMode
                AutoClickerConfig.repeatCount = repeats

                startFloatingControls()
            }
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )
    }
}

@Composable
private fun StatusCard(
    accessibilityOn: Boolean,
    openAccessibilitySettings: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        text =
                            if (accessibilityOn)
                                "Accessibility enabled"
                            else
                                "Accessibility disabled",
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color =
                        if (accessibilityOn)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                ) {

                    Text(
                        text =
                            if (accessibilityOn)
                                "ON"
                            else
                                "OFF",
                        modifier = Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 7.dp
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = openAccessibilitySettings
            ) {
                Text("Accessibility Settings")
            }
        }
    }
}

@Composable
private fun AutomationSettingsCard(
    intervalText: String,
    onIntervalChange: (String) -> Unit,
    continuousMode: Boolean,
    onContinuousModeChange: (Boolean) -> Unit,
    repeatCountText: String,
    onRepeatCountChange: (String) -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {

        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Text(
                text = "Automation Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = intervalText,
                onValueChange = onIntervalChange,
                label = {
                    Text("Default interval (ms)")
                },
                supportingText = {
                    Text("1000 ms = 1 second")
                },
                singleLine = true
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Continuous mode",
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Keep repeating until you press Stop",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = continuousMode,
                    onCheckedChange = onContinuousModeChange
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = repeatCountText,
                onValueChange = onRepeatCountChange,
                enabled = !continuousMode,
                label = {
                    Text("Repeat count")
                },
                singleLine = true
            )
        }
    }
}

@Composable
private fun FloatingControlsCard(
    openOverlayPermission: () -> Unit,
    showFloatingControls: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Floating Automation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Create multiple taps, swipes, delays, sequences and saved profiles over other apps.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = openOverlayPermission
            ) {
                Text("Allow Floating Controls")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = showFloatingControls
            ) {
                Text("Show Floating Controls")
            }
        }
    }
}