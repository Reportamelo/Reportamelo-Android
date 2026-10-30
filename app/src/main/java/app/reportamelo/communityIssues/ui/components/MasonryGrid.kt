package app.reportamelo.communityIssues.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun <T> MasonryGrid(
    modifier: Modifier = Modifier,
    columns: Int,
    spacing: Dp,
    data: List<T>,
    content: @Composable (T) -> Unit
) {
    val columnData = List(columns) { mutableListOf<T>() }
    data.forEachIndexed { index, item ->
        columnData[index % columns].add(item)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.Top
    ) {
        columnData.forEach { columnItems ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                columnItems.forEach { item ->
                    content(item)
                }
            }
        }
    }
}
