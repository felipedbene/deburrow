package dev.debene.gopher.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import dev.debene.gopher.protocol.GopherItem
import dev.debene.gopher.protocol.GopherType

@Composable
fun MenuList(
    items: List<GopherItem>,
    onItemClick: (GopherItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(items) { item ->
            when {
                item.type == GopherType.INFO -> InfoRow(item)
                item.type == GopherType.ERROR -> ErrorRow(item)
                else -> MenuRow(item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun MenuRow(item: GopherItem, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeBadge(item.type)
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.display.ifBlank { item.selector },
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TypeBadge(type: GopherType) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.size(32.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = iconFor(type),
                contentDescription = type.label,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun InfoRow(item: GopherItem) {
    Text(
        text = item.display,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .alpha(0.85f),
    )
}

@Composable
private fun ErrorRow(item: GopherItem) {
    Text(
        text = item.display,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
    HorizontalDivider()
}

private fun iconFor(type: GopherType): ImageVector = when (type.kind) {
    GopherType.Kind.MENU -> Icons.Filled.Folder
    GopherType.Kind.TEXT -> Icons.Filled.Description
    GopherType.Kind.IMAGE -> Icons.Filled.Image
    GopherType.Kind.SEARCH -> Icons.Filled.Search
    GopherType.Kind.HTML -> Icons.Filled.Language
    GopherType.Kind.BINARY -> Icons.Filled.Download
    GopherType.Kind.TELNET -> Icons.Filled.Terminal
    GopherType.Kind.INFO, GopherType.Kind.ERROR -> Icons.Filled.Warning
}
