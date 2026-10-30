package app.reportamelo.communityIssues.helpers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

interface XStyleTabItem {
    val title: String
    val symbol: ImageVector
    val description: String
}

@Composable
fun <T : XStyleTabItem> XStyleTabBar(
    tabs: List<T>,
    progress: Float,
    onTap: (T) -> Unit
) {
    val cappedProgress = max(0f, min(progress, (tabs.size - 1).toFloat()))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        tabs.forEachIndexed { index, tab ->
            val tabProgress = min(abs(cappedProgress - index.toFloat()), 1f)
            val isSelected = tabProgress < 0.5f

            Row(
                modifier = Modifier
                    .height(30.dp)
                    .clickable { onTap(tab) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = tab.symbol,
                    contentDescription = tab.title,
                    tint = if (isSelected) Color.Black else Color.Gray
                )
                
                if (isSelected) {
                    Text(
                        text = tab.description,
                        modifier = Modifier.padding(start = 8.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
