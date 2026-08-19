package dev.chiraitori.anis.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chiraitori.anis.data.model.DnsQueryLog
import dev.chiraitori.anis.data.model.QueryStatus
import dev.chiraitori.anis.ui.theme.CoralRed
import dev.chiraitori.anis.ui.theme.EmeraldPrimary
import dev.chiraitori.anis.ui.theme.IndigoPrimary
import dev.chiraitori.anis.ui.theme.shapes.ShapeCache
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogItemRow(
    log: DnsQueryLog,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = ShapeCache.smooth16,
        colors = CardDefaults.cardColors(
            containerColor = when (log.status) {
                QueryStatus.BLOCKED_AD -> CoralRed.copy(alpha = 0.08f)
                QueryStatus.BLOCKED_FIREWALL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                QueryStatus.WHITELISTED -> EmeraldPrimary.copy(alpha = 0.08f)
                QueryStatus.CUSTOM_REWRITE -> IndigoPrimary.copy(alpha = 0.08f)
                QueryStatus.SAFESEARCH_REDIRECT -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                QueryStatus.ALLOWED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status Icon Box using M3 Expressive Star6 Shape
                Surface(
                    shape = ShapeCache.star6,
                    color = when (log.status) {
                        QueryStatus.BLOCKED_AD, QueryStatus.BLOCKED_FIREWALL -> CoralRed.copy(alpha = 0.15f)
                        QueryStatus.WHITELISTED -> EmeraldPrimary.copy(alpha = 0.15f)
                        QueryStatus.CUSTOM_REWRITE -> IndigoPrimary.copy(alpha = 0.15f)
                        QueryStatus.SAFESEARCH_REDIRECT -> MaterialTheme.colorScheme.secondaryContainer
                        QueryStatus.ALLOWED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (log.status) {
                                QueryStatus.BLOCKED_AD -> Icons.Filled.Block
                                QueryStatus.BLOCKED_FIREWALL -> Icons.Filled.LocalFireDepartment
                                QueryStatus.WHITELISTED -> Icons.Filled.Shield
                                QueryStatus.CUSTOM_REWRITE -> Icons.Filled.Route
                                QueryStatus.SAFESEARCH_REDIRECT -> Icons.Filled.FamilyRestroom
                                QueryStatus.ALLOWED -> Icons.Filled.CheckCircle
                            },
                            contentDescription = null,
                            tint = when (log.status) {
                                QueryStatus.BLOCKED_AD, QueryStatus.BLOCKED_FIREWALL -> CoralRed
                                QueryStatus.WHITELISTED -> EmeraldPrimary
                                QueryStatus.CUSTOM_REWRITE -> IndigoPrimary
                                QueryStatus.SAFESEARCH_REDIRECT -> MaterialTheme.colorScheme.onSecondaryContainer
                                QueryStatus.ALLOWED -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.domain,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = ShapeCache.smooth8,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = log.queryType,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        if (log.blockReason != null) {
                            Text(
                                text = log.blockReason,
                                fontSize = 11.sp,
                                color = when (log.status) {
                                    QueryStatus.BLOCKED_AD, QueryStatus.BLOCKED_FIREWALL -> CoralRed
                                    QueryStatus.CUSTOM_REWRITE -> IndigoPrimary
                                    QueryStatus.SAFESEARCH_REDIRECT -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = formatTime(log.timestamp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(status = log.status)
                if (log.upstreamLatencyMs > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${log.upstreamLatencyMs} ms",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: QueryStatus) {
    val (text, bg, fg) = when (status) {
        QueryStatus.BLOCKED_AD -> Triple("BLOCKED", CoralRed.copy(alpha = 0.15f), CoralRed)
        QueryStatus.BLOCKED_FIREWALL -> Triple("DROPPED", MaterialTheme.colorScheme.error.copy(alpha = 0.15f), MaterialTheme.colorScheme.error)
        QueryStatus.WHITELISTED -> Triple("ALLOWED", EmeraldPrimary.copy(alpha = 0.15f), EmeraldPrimary)
        QueryStatus.CUSTOM_REWRITE -> Triple("REWRITE", IndigoPrimary.copy(alpha = 0.15f), IndigoPrimary)
        QueryStatus.SAFESEARCH_REDIRECT -> Triple("SAFE", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        QueryStatus.ALLOWED -> Triple("ALLOWED", EmeraldPrimary.copy(alpha = 0.12f), EmeraldPrimary)
    }

    Surface(
        shape = ShapeCache.smoothPill,
        color = bg
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
