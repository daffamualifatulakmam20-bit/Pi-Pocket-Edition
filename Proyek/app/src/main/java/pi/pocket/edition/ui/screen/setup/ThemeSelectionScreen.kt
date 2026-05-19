package pi.pocket.edition.ui.screen.setup

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pi.pocket.edition.data.PrefsManager

@Composable
fun ThemeSelectionScreen(
    prefsManager: PrefsManager,
    onContinue: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedDark by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (selectedDark) Color.Black else Color(0xFFFAFAFA))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App icon placeholder
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = Color(0xFFE84057).copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "🐱",
                    fontSize = 40.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Pi-Pocket Edition",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (selectedDark) Color.White else Color(0xFF1A1A1A)
        )

        Text(
            text = "Pilih tema tampilan",
            fontSize = 14.sp,
            color = if (selectedDark) Color(0xFFB0B0B0) else Color(0xFF666666),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Dark theme option
        ThemeOptionCard(
            title = "Dark (AMOLED)",
            description = "Hitam murni, hemat baterai",
            icon = Icons.Default.DarkMode,
            isSelected = selectedDark,
            previewColors = listOf(Color.Black, Color(0xFF121212), Color(0xFF1A1A1A)),
            textColor = Color.White,
            onClick = { selectedDark = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Light theme option
        ThemeOptionCard(
            title = "Light",
            description = "Bersih dan terang",
            icon = Icons.Default.LightMode,
            isSelected = !selectedDark,
            previewColors = listOf(Color(0xFFFAFAFA), Color.White, Color(0xFFF5F5F5)),
            textColor = Color(0xFF1A1A1A),
            onClick = { selectedDark = false }
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                scope.launch {
                    prefsManager.setDarkTheme(selectedDark)
                    prefsManager.setThemeSelected(true)
                    onContinue()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE84057)
            )
        ) {
            Text(
                text = "Lanjutkan →",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ThemeOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    previewColors: List<Color>,
    textColor: Color,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFE84057) else Color(0xFF333333),
        label = "border"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        color = previewColors[0],
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFFE84057)
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFFE84057) else textColor.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = textColor
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // Color preview dots
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                previewColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, textColor.copy(alpha = 0.2f), CircleShape)
                    )
                }
            }
        }
    }
}
