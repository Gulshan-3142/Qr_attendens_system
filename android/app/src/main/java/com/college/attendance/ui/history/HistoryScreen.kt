package com.college.attendance.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.attendance.data.api.models.AttendanceRecordDto
import com.college.attendance.ui.theme.*
import com.college.attendance.util.DateUtils

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Attendance History",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                    Text(
                        text = "${state.filteredRecords.size} records found",
                        fontSize = 13.sp,
                        color = Slate400
                    )
                }

                IconButton(
                    onClick = { viewModel.loadHistory() },
                    modifier = Modifier.background(Slate900, CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Indigo400)
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by subject or classroom...", fontSize = 13.sp, color = Slate400) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Indigo400) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Indigo500,
                    unfocusedBorderColor = Slate800,
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate100
                ),
                singleLine = true
            )
        }

        // Status Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill(
                    label = "All Statuses",
                    isSelected = state.selectedStatus == null
                ) { viewModel.setStatusFilter(null) }

                FilterPill(
                    label = "Present",
                    isSelected = state.selectedStatus == "PRESENT",
                    color = Emerald400
                ) { viewModel.setStatusFilter("PRESENT") }

                FilterPill(
                    label = "Absent",
                    isSelected = state.selectedStatus == "ABSENT",
                    color = Rose400
                ) { viewModel.setStatusFilter("ABSENT") }

                FilterPill(
                    label = "Late",
                    isSelected = state.selectedStatus == "LATE",
                    color = Amber400
                ) { viewModel.setStatusFilter("LATE") }
            }
        }

        // Subject Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill(
                    label = "All Subjects",
                    isSelected = state.selectedSubjectId == null
                ) { viewModel.setSubjectFilter(null) }

                state.subjects.forEach { sub ->
                    FilterPill(
                        label = sub.subjectCode,
                        isSelected = state.selectedSubjectId == sub.subjectId
                    ) { viewModel.setSubjectFilter(sub.subjectId) }
                }
            }
        }

        // History Items
        if (state.filteredRecords.isEmpty() && !state.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Inbox, null, tint = Slate600, modifier = Modifier.size(48.dp))
                        Text("No matching attendance records found.", fontSize = 13.sp, color = Slate400)
                    }
                }
            }
        }

        items(state.filteredRecords) { record ->
            HistoryRecordItem(record = record)
        }
    }
}

@Composable
private fun HistoryRecordItem(record: AttendanceRecordDto) {
    val isPresent = record.status.equals("PRESENT", ignoreCase = true)
    val statusColor = if (isPresent) Emerald400 else Rose400
    val statusBg = if (isPresent) Emerald500.copy(alpha = 0.15f) else Rose500.copy(alpha = 0.15f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate900, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = record.subjectName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
                Text(
                    text = "${record.subjectCode} • ${DateUtils.formatDisplayDate(record.date)} • ${DateUtils.formatTime(record.time)}",
                    fontSize = 12.sp,
                    color = Slate400
                )
                if (record.distanceMeters != null) {
                    Text(
                        text = "📍 Verified at ${String.format("%.1f", record.distanceMeters)}m from room",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(statusBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = record.status.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    color: Color = Indigo400,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) color.copy(alpha = 0.2f) else Slate900,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else Slate400
        )
    }
}
