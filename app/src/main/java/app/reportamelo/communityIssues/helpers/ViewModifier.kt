package app.reportamelo.communityIssues.helpers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.cellStyle(): Modifier = this.then(
    Modifier
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .background(
            color = Color.White, // Represents Color.theme.cardBackground
            shape = RoundedCornerShape(16.dp) // Represents .themeRadius * 2
        )
        .border(
            width = 1.dp,
            color = Color.LightGray, // Represents Color.theme.border
            shape = RoundedCornerShape(16.dp)
        )
        .clip(RoundedCornerShape(16.dp))
        .padding(16.dp)
)

fun Modifier.simpleCellStyle(): Modifier = this.then(
    Modifier
        .padding(horizontal = 0.dp, vertical = 16.dp)
)
