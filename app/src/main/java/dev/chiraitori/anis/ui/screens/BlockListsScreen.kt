package dev.chiraitori.anis.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chiraitori.anis.data.model.RuleCategory
import dev.chiraitori.anis.ui.MainViewModel
import dev.chiraitori.anis.ui.components.BlockListCard
import dev.chiraitori.anis.ui.theme.shapes.ShapeCache

@Composable
fun BlockListsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val sources by viewModel.blockLists.collectAsState()
    val isUpdating by viewModel.isUpdatingLists.collectAsState()
    val updateProgress by viewModel.updateProgress.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<RuleCategory?>(null) }

    val allEnabled = sources.isNotEmpty() && sources.all { it.isEnabled }
    val enabledCount = sources.count { it.isEnabled }

    val filteredSources = if (selectedCategoryFilter != null) {
        sources.filter { it.category == selectedCategoryFilter }
    } else {
        sources
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Custom List", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = ShapeCache.smooth20,
                modifier = Modifier.padding(bottom = 96.dp)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Adblock Lists",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Choose curated lists or enable all for maximum ad & malware blocking",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Master "USE ALL" Hero Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapeCache.smooth28,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (allEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = ShapeCache.star8,
                                    color = if (allEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = if (allEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Use All Adblock Lists",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (allEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$enabledCount of ${sources.size} lists active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (allEnabled) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Switch(
                                checked = allEnabled,
                                onCheckedChange = { enableAll ->
                                    if (enableAll) viewModel.enableAllBlockLists() else viewModel.disableAllBlockLists()
                                },
                                thumbContent = {
                                    if (allEnabled) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize))
                                    else Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize))
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Update All Button
                        FilledTonalButton(
                            onClick = { viewModel.updateAllBlockLists() },
                            enabled = !isUpdating,
                            shape = ShapeCache.smooth14,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (updateProgress.isNotEmpty()) updateProgress else "Updating...", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            } else {
                                Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Update All Lists", fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }

                        if (isUpdating) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Category Filter Pills
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("All (${sources.size})") },
                            shape = ShapeCache.smoothPill
                        )
                    }

                    RuleCategory.values().forEach { category ->
                        val count = sources.count { it.category == category }
                        if (count > 0) {
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == category,
                                    onClick = {
                                        selectedCategoryFilter = if (selectedCategoryFilter == category) null else category
                                    },
                                    label = { Text("${category.displayName} ($count)") },
                                    shape = ShapeCache.smoothPill
                                )
                            }
                        }
                    }
                }
            }

            // List Items
            items(filteredSources, key = { it.id }) { source ->
                BlockListCard(
                    source = source,
                    onToggle = { isEnabled -> viewModel.toggleBlockList(source.id, isEnabled) },
                    onUpdate = { viewModel.updateBlockList(source.id) },
                    onDelete = if (source.isCustom) {
                        { viewModel.removeCustomBlockList(source.id) }
                    } else null
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Add Custom List Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf(RuleCategory.CUSTOM) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom Filter List", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("List Name") },
                        placeholder = { Text("My AdBlock Rules") },
                        singleLine = true,
                        shape = ShapeCache.smooth12,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Filter List URL") },
                        placeholder = { Text("https://example.com/hosts.txt") },
                        singleLine = true,
                        shape = ShapeCache.smooth12,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        singleLine = true,
                        shape = ShapeCache.smooth12,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Quick Presets:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            SuggestionChip(
                                onClick = {
                                    name = "Hägezi Multi PRO"
                                    url = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/hosts/pro.txt"
                                    description = "Comprehensive ad & tracker blocker with zero false positives"
                                },
                                label = { Text("Hägezi PRO", fontSize = 11.sp) },
                                shape = ShapeCache.smoothPill
                            )
                        }
                        item {
                            SuggestionChip(
                                onClick = {
                                    name = "OISD Big"
                                    url = "https://big.oisd.nl"
                                    description = "Massive comprehensive anti-tracker & ad blocklist"
                                },
                                label = { Text("OISD Big", fontSize = 11.sp) },
                                shape = ShapeCache.smoothPill
                            )
                        }
                        item {
                            SuggestionChip(
                                onClick = {
                                    name = "1Hosts (Lite)"
                                    url = "https://raw.githubusercontent.com/badmojr/1Hosts/master/Lite/hosts.txt"
                                    description = "Lightweight, highly effective mobile advertisement blocker"
                                },
                                label = { Text("1Hosts", fontSize = 11.sp) },
                                shape = ShapeCache.smoothPill
                            )
                        }
                        item {
                            SuggestionChip(
                                onClick = {
                                    name = "StevenBlack Unified"
                                    url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
                                    description = "Consolidated hosts file blocking ads + malware"
                                },
                                label = { Text("StevenBlack", fontSize = 11.sp) },
                                shape = ShapeCache.smoothPill
                            )
                        }
                        item {
                            SuggestionChip(
                                onClick = {
                                    name = "ABPVN Filter"
                                    url = "https://raw.githubusercontent.com/abpvn/abpvn/master/filter/abpvn.txt"
                                    description = "Vietnamese ad, popup & banner blocker"
                                },
                                label = { Text("ABPVN", fontSize = 11.sp) },
                                shape = ShapeCache.smoothPill
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && url.isNotBlank()) {
                            viewModel.addCustomBlockList(
                                name = name.trim(),
                                url = url.trim(),
                                category = selectedCategory
                            )
                            showAddDialog = false
                        }
                    },
                    shape = ShapeCache.smooth12
                ) {
                    Text("Add & Sync")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = ShapeCache.smooth26
        )
    }
}
