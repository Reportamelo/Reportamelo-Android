package app.reportamelo.communityIssues.ui.screens.subviews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Date

// CustomChartSubView models
data class MonthlyData(
    val month: Int,
    val count: Int
) {
    val date: Date
        get() {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, 2026)
            calendar.set(Calendar.MONTH, month - 1)
            return calendar.time
        }
}

data class StatsSummary(
    val totalEntries: Int,
    val monthlyBreakdown: List<MonthlyData>
)

@Composable
fun CustomChartSubView() {
    val statsSummary = StatsSummary(
        totalEntries = 77,
        monthlyBreakdown = listOf(
            MonthlyData(month = 1, count = 1),
            MonthlyData(month = 2, count = 2),
            MonthlyData(month = 3, count = 1),
            MonthlyData(month = 4, count = 4),
            MonthlyData(month = 5, count = 5),
            MonthlyData(month = 6, count = 6),
            MonthlyData(month = 7, count = 7),
            MonthlyData(month = 8, count = 8),
            MonthlyData(month = 9, count = 9),
            MonthlyData(month = 10, count = 10),
            MonthlyData(month = 11, count = 11),
            MonthlyData(month = 12, count = 12)
        )
    )
    
    StatsCardView(stats = statsSummary)
}

@Composable
fun StatsCardView(stats: StatsSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color(0xFF121212), shape = RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${stats.totalEntries}",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Entries",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "This Year",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }
        
        // Placeholder for Chart as there's no built-in Compose Chart equivalent to Swift Charts out of the box
        // Ideally this would use a library like Vico or similar.
        Box(
            modifier = Modifier
                .height(120.dp)
                .weight(2f)
                .background(Color.DarkGray.copy(alpha=0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text("Chart Placeholder", color = Color.Gray)
        }
    }
}
