package com.overdrive.app.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.overdrive.app.ui.adapter.CalendarAdapter
import com.overdrive.app.ui.adapter.RecordingAdapter
import com.overdrive.app.ui.model.RecordingFile
import com.overdrive.app.ui.util.RecordingScanner
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.overdrive.app.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Fragment for browsing recorded videos with calendar view.
 * SOTA: Uses background thread for scanning to prevent UI lag.
 */
class RecordingLibraryFragment : Fragment() {
    
    companion object {
        private const val TAG = "RecordingLibrary"
    }
    
    private lateinit var tvCurrentMonth: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var recyclerCalendar: RecyclerView
    private lateinit var tvSelectedDate: TextView
    private lateinit var recyclerRecordings: RecyclerView
    private lateinit var tvEmptyState: TextView
    private var emptyStateContainer: LinearLayout? = null
    private var chipFilterAll: Chip? = null
    private var chipFilterNormal: Chip? = null
    private var chipFilterSentry: Chip? = null
    private var chipFilterProximity: Chip? = null
    
    // Multi-select toolbar
    private var selectToolbar: LinearLayout? = null
    private var tvSelectedCount: TextView? = null
    private var btnSelectAll: View? = null
    private var btnDeleteSelected: View? = null
    private var btnCancelSelect: View? = null
    
    // Pagination
    private var paginationControls: LinearLayout? = null
    private var btnPrevPage: MaterialButton? = null
    private var btnNextPage: MaterialButton? = null
    private var tvPageInfo: TextView? = null
    private var currentPage = 1
    private var totalPages = 1
    private var allFilteredRecordings: List<RecordingFile> = emptyList()
    private val PAGE_SIZE = 12
    
    // Storage stats
    private var tvStorageUsed: TextView? = null
    private var viewStorageFill: View? = null
    private var tvNormalCount: TextView? = null
    private var tvSentryCount: TextView? = null
    private var tvProximityCount: TextView? = null
    
    private val calendarAdapter = CalendarAdapter { day -> onDaySelected(day) }
    private lateinit var recordingAdapter: RecordingAdapter
    
    private val calendar = Calendar.getInstance()
    private var selectedDay = calendar.get(Calendar.DAY_OF_MONTH)
    private var currentFilter = RecordingFilter.ALL
    
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    // SOTA: Background executor for scanning operations
    private var scanExecutor = Executors.newSingleThreadExecutor()
    
    enum class RecordingFilter {
        ALL, NORMAL, SENTRY, PROXIMITY
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure executor is available (may have been shutdown on previous destroy)
        if (scanExecutor.isShutdown) {
            scanExecutor = Executors.newSingleThreadExecutor()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recording_library, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Ensure executor is available (may have been shutdown in onDestroyView)
        if (scanExecutor.isShutdown || scanExecutor.isTerminated) {
            scanExecutor = Executors.newSingleThreadExecutor()
        }
        
        initViews(view)
        setupCalendar()
        setupRecordingsList()
        setupClickListeners()
        
        // SOTA: Check for All Files Access permission and trigger MediaScan
        checkPermissionsAndScan()
        
        updateCalendar()
        loadRecordingsForSelectedDate()
        loadStorageStats()
    }
    
    /**
     * SOTA: Setup directories and load recordings.
     * Since App owns the directories, we use direct file access.
     */
    private fun checkPermissionsAndScan() {
        // SOTA: UI App creates directories so it OWNS them (can listFiles)
        setupStorageDirectories()
        
        // Direct file access - no MediaStore needed
        RecordingScanner.invalidateCache()
        updateCalendar()
        loadRecordingsForSelectedDate()
    }
    
    /**
     * SOTA: UI App creates directories so it OWNS them.
     * This ensures listFiles() works even for files created by daemon (UID 2000).
     * Directory ownership = listFiles() access, not file ownership.
     * 
     * Now uses RecordingScanner to get configured storage paths (internal or SD card).
     */
    private fun setupStorageDirectories() {
        try {
            // Get configured directories from RecordingScanner
            val recordingsDir = RecordingScanner.getRecordingsDir(requireContext())
            val surveillanceDir = RecordingScanner.getSentryEventsDir(requireContext())
            val proximityDir = RecordingScanner.getProximityEventsDir(requireContext())
            
            Log.d(TAG, "Configured directories:")
            Log.d(TAG, "  Recordings: ${recordingsDir.absolutePath}")
            Log.d(TAG, "  Surveillance: ${surveillanceDir.absolutePath}")
            Log.d(TAG, "  Proximity: ${proximityDir.absolutePath}")
            
            // Ensure base directory exists
            val baseDir = recordingsDir.parentFile
            if (baseDir != null && !baseDir.exists()) {
                val created = baseDir.mkdirs()
                Log.d(TAG, "Created base directory: ${baseDir.absolutePath} (success=$created)")
            }
            
            // Ensure subdirectories exist
            listOf(recordingsDir, surveillanceDir, proximityDir).forEach { dir ->
                if (!dir.exists()) {
                    val created = dir.mkdirs()
                    Log.d(TAG, "Created subdirectory: ${dir.absolutePath} (success=$created)")
                }
            }
            
            // Verify we can list files now
            val files = recordingsDir.listFiles()
            Log.d(TAG, "After setup - recordings dir listFiles: ${files?.size ?: "null"}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup storage directories: ${e.message}")
        }
    }
    
    private fun initViews(view: View) {
        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth)
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)
        recyclerCalendar = view.findViewById(R.id.recyclerCalendar)
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)
        recyclerRecordings = view.findViewById(R.id.recyclerRecordings)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        
        // Multi-select toolbar
        selectToolbar = view.findViewById(R.id.selectToolbar)
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount)
        btnSelectAll = view.findViewById(R.id.btnSelectAll)
        btnDeleteSelected = view.findViewById(R.id.btnDeleteSelected)
        btnCancelSelect = view.findViewById(R.id.btnCancelSelect)
        
        btnSelectAll?.setOnClickListener { recordingAdapter.selectAll() }
        btnDeleteSelected?.setOnClickListener { confirmBatchDelete() }
        btnCancelSelect?.setOnClickListener { exitSelectMode() }
        
        // Pagination
        paginationControls = view.findViewById(R.id.paginationControls)
        btnPrevPage = view.findViewById(R.id.btnPrevPage)
        btnNextPage = view.findViewById(R.id.btnNextPage)
        tvPageInfo = view.findViewById(R.id.tvPageInfo)
        
        btnPrevPage?.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                applyPage()
            }
        }
        btnNextPage?.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                applyPage()
            }
        }
        
        // Storage stats
        tvStorageUsed = view.findViewById(R.id.tvStorageUsed)
        viewStorageFill = view.findViewById(R.id.viewStorageFill)
        tvNormalCount = view.findViewById(R.id.tvNormalCount)
        tvSentryCount = view.findViewById(R.id.tvSentryCount)
        tvProximityCount = view.findViewById(R.id.tvProximityCount)
        
        // Filter chips - modern design
        try {
            chipFilterAll = view.findViewById(R.id.btnFilterAll)
            chipFilterNormal = view.findViewById(R.id.btnFilterNormal)
            chipFilterSentry = view.findViewById(R.id.btnFilterSentry)
            chipFilterProximity = view.findViewById(R.id.btnFilterProximity)
            setupFilterChips()
        } catch (e: Exception) {
            // Filter chips not available - use default filter
        }
    }
    
    private fun setupFilterChips() {
        chipFilterAll?.setOnClickListener {
            currentFilter = RecordingFilter.ALL
            updateFilterChips()
            currentPage = 1
            loadRecordingsForSelectedDate()
        }
        
        chipFilterNormal?.setOnClickListener {
            currentFilter = RecordingFilter.NORMAL
            updateFilterChips()
            currentPage = 1
            loadRecordingsForSelectedDate()
        }
        
        chipFilterSentry?.setOnClickListener {
            currentFilter = RecordingFilter.SENTRY
            updateFilterChips()
            currentPage = 1
            loadRecordingsForSelectedDate()
        }
        
        chipFilterProximity?.setOnClickListener {
            currentFilter = RecordingFilter.PROXIMITY
            updateFilterChips()
            currentPage = 1
            loadRecordingsForSelectedDate()
        }
        
        updateFilterChips()
    }
    
    private fun updateFilterChips() {
        // Skip if chips not available
        if (chipFilterAll == null) return
        
        chipFilterAll?.isChecked = currentFilter == RecordingFilter.ALL
        chipFilterNormal?.isChecked = currentFilter == RecordingFilter.NORMAL
        chipFilterSentry?.isChecked = currentFilter == RecordingFilter.SENTRY
        chipFilterProximity?.isChecked = currentFilter == RecordingFilter.PROXIMITY
    }
    
    private fun setupCalendar() {
        recyclerCalendar.apply {
            layoutManager = GridLayoutManager(context, 7)
            adapter = calendarAdapter
        }
    }
    
    private fun setupRecordingsList() {
        recordingAdapter = RecordingAdapter(
            onPlay = { recording -> playRecording(recording) },
            onDelete = { recording -> confirmDelete(recording) },
            onSelectionChanged = { count -> onSelectionChanged(count) }
        )
        
        recyclerRecordings.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recordingAdapter
        }
    }
    
    private fun onSelectionChanged(count: Int) {
        tvSelectedCount?.text = "$count selected"
        if (recordingAdapter.selectMode && selectToolbar?.visibility != View.VISIBLE) {
            selectToolbar?.visibility = View.VISIBLE
        }
    }
    
    private fun setupClickListeners() {
        btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            selectedDay = 1
            updateCalendar()
            loadRecordingsForSelectedDate()
        }
        
        btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            selectedDay = 1
            updateCalendar()
            loadRecordingsForSelectedDate()
        }
    }
    
    private fun updateCalendar() {
        tvCurrentMonth.text = monthFormat.format(calendar.time)
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        
        Log.d(TAG, "Updating calendar for $year-${month+1}")
        
        // Build calendar days first (fast operation)
        val days = buildCalendarDays(year, month)
        
        // SOTA: Load recording counts in background to prevent UI lag
        if (!scanExecutor.isShutdown) {
            scanExecutor.submit {
                try {
                    val recordingCounts = RecordingScanner.getRecordingCountsByDate(requireContext(), year, month)
                    Log.d(TAG, "Recording counts for month: $recordingCounts")
                    
                    activity?.runOnUiThread {
                        if (isAdded) {
                            calendarAdapter.setDays(days, recordingCounts)
                            calendarAdapter.setSelectedDay(selectedDay)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting recording counts", e)
                }
            }
        }
        
        // Set days immediately with empty counts for instant feedback
        calendarAdapter.setDays(days, emptyMap())
        calendarAdapter.setSelectedDay(selectedDay)
    }
    
    private fun buildCalendarDays(year: Int, month: Int): List<CalendarAdapter.CalendarDay> {
        val days = mutableListOf<CalendarAdapter.CalendarDay>()
        
        val tempCal = Calendar.getInstance().apply {
            set(year, month, 1)
        }
        
        val today = Calendar.getInstance()
        val isCurrentMonth = today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) == month
        val isFutureMonth = (year > today.get(Calendar.YEAR)) || 
            (year == today.get(Calendar.YEAR) && month > today.get(Calendar.MONTH))
        val todayDay = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else -1
        
        // Add empty cells for days before the first of the month
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1
        repeat(firstDayOfWeek) {
            days.add(CalendarAdapter.CalendarDay(0, false))
        }
        
        // Add days of the month
        val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..daysInMonth) {
            val isFuture = isFutureMonth || (isCurrentMonth && day > todayDay)
            days.add(CalendarAdapter.CalendarDay(
                dayOfMonth = day,
                isCurrentMonth = true,
                isToday = day == todayDay,
                isFuture = isFuture
            ))
        }
        
        return days
    }
    
    private fun onDaySelected(day: Int) {
        selectedDay = day
        calendarAdapter.setSelectedDay(day)
        currentPage = 1
        loadRecordingsForSelectedDate()
    }
    
    private fun loadRecordingsForSelectedDate() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        
        // Update header immediately
        val selectedCal = Calendar.getInstance().apply {
            set(year, month, selectedDay)
        }
        tvSelectedDate.text = SimpleDateFormat("MMM d", Locale.getDefault()).format(selectedCal.time)
        
        Log.d(TAG, "Loading recordings for $year-${month+1}-$selectedDay")
        
        if (scanExecutor.isShutdown) return
        scanExecutor.submit {
            try {
                val allRecordings = RecordingScanner.getRecordingsForDate(requireContext(), year, month, selectedDay)
                
                val recordings = when (currentFilter) {
                    RecordingFilter.ALL -> allRecordings
                    RecordingFilter.NORMAL -> allRecordings.filter { it.type == RecordingFile.RecordingType.NORMAL }
                    RecordingFilter.SENTRY -> allRecordings.filter { it.type == RecordingFile.RecordingType.SENTRY }
                    RecordingFilter.PROXIMITY -> allRecordings.filter { it.type == RecordingFile.RecordingType.PROXIMITY }
                }
                
                Log.d(TAG, "After filter (${currentFilter}): ${recordings.size} recordings")
                
                activity?.runOnUiThread {
                    if (isAdded) {
                        allFilteredRecordings = recordings
                        totalPages = if (recordings.isEmpty()) 1 else (recordings.size + PAGE_SIZE - 1) / PAGE_SIZE
                        if (currentPage > totalPages) currentPage = totalPages
                        applyPage()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading recordings", e)
            }
        }
    }
    
    private fun applyPage() {
        val start = (currentPage - 1) * PAGE_SIZE
        val end = minOf(start + PAGE_SIZE, allFilteredRecordings.size)
        val page = if (start < allFilteredRecordings.size) allFilteredRecordings.subList(start, end) else emptyList()
        
        if (allFilteredRecordings.isEmpty()) {
            recyclerRecordings.visibility = View.GONE
            emptyStateContainer?.visibility = View.VISIBLE
            tvEmptyState.visibility = View.VISIBLE
            tvEmptyState.text = when (currentFilter) {
                RecordingFilter.ALL -> "No recordings for this date"
                RecordingFilter.NORMAL -> "No normal recordings"
                RecordingFilter.SENTRY -> "No sentry events"
                RecordingFilter.PROXIMITY -> "No proximity events"
            }
        } else {
            recyclerRecordings.visibility = View.VISIBLE
            emptyStateContainer?.visibility = View.GONE
            tvEmptyState.visibility = View.GONE
            recordingAdapter.submitList(page)
            recyclerRecordings.scrollToPosition(0)
        }
        updatePagination()
    }
    
    private fun updatePagination() {
        if (totalPages <= 1) {
            paginationControls?.visibility = View.GONE
            return
        }
        paginationControls?.visibility = View.VISIBLE
        tvPageInfo?.text = "Page $currentPage of $totalPages"
        btnPrevPage?.isEnabled = currentPage > 1
        btnNextPage?.isEnabled = currentPage < totalPages
    }
    
    private fun loadStorageStats() {
        if (scanExecutor.isShutdown) return
        scanExecutor.submit {
            try {
                val ctx = requireContext()
                val all = RecordingScanner.scanRecordings(ctx)
                val totalSize = all.sumOf { it.sizeBytes }
                val normalCount = all.count { it.type == RecordingFile.RecordingType.NORMAL }
                val sentryCount = all.count { it.type == RecordingFile.RecordingType.SENTRY }
                val proximityCount = all.count { it.type == RecordingFile.RecordingType.PROXIMITY }
                
                // Get total storage space from StorageManager
                val sm = com.overdrive.app.storage.StorageManager.getInstance()
                val baseDir = sm.recordingsDir.parentFile
                val totalSpace = baseDir?.totalSpace ?: 1L
                val usedPercent = if (totalSpace > 0) ((totalSize.toDouble() / totalSpace) * 100).toInt().coerceIn(0, 100) else 0
                
                val sizeStr = when {
                    totalSize >= 1_000_000_000 -> String.format("%.1f GB", totalSize / 1_000_000_000.0)
                    totalSize >= 1_000_000 -> String.format("%.1f MB", totalSize / 1_000_000.0)
                    totalSize >= 1_000 -> String.format("%.1f KB", totalSize / 1_000.0)
                    else -> "$totalSize B"
                }
                
                activity?.runOnUiThread {
                    if (isAdded) {
                        tvStorageUsed?.text = sizeStr
                        tvNormalCount?.text = normalCount.toString()
                        tvSentryCount?.text = sentryCount.toString()
                        tvProximityCount?.text = proximityCount.toString()
                        
                        // Animate storage fill bar width
                        viewStorageFill?.let { fill ->
                            val parent = fill.parent as? View ?: return@let
                            fill.post {
                                val params = fill.layoutParams
                                params.width = (parent.width * usedPercent / 100)
                                fill.layoutParams = params
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading storage stats", e)
            }
        }
    }
    
    private fun playRecording(recording: RecordingFile) {
        try {
            val bundle = Bundle().apply {
                putString(VideoPlayerFragment.ARG_VIDEO_PATH, recording.path)
                putString(VideoPlayerFragment.ARG_VIDEO_TITLE, recording.name)
            }
            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.action_global_videoPlayer, bundle)
        } catch (e: Exception) {
            // Fallback: open with external player if navigation fails
            try {
                val uri = recording.contentUri ?: FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    recording.file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/mp4")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Play with"))
            } catch (e2: Exception) {
                Toast.makeText(context, "Cannot play video: ${e2.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun confirmDelete(recording: RecordingFile) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Recording")
            .setMessage("Delete ${recording.name}?\nThis cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteRecording(recording)
            }
            .show()
    }
    
    private fun deleteRecording(recording: RecordingFile) {
        if (RecordingScanner.deleteRecording(recording)) {
            Toast.makeText(context, "Recording deleted", Toast.LENGTH_SHORT).show()
            loadRecordingsForSelectedDate()
            updateCalendar()
            loadStorageStats()
        } else {
            Toast.makeText(context, "Failed to delete recording", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun confirmBatchDelete() {
        val selected = recordingAdapter.getSelectedRecordings()
        if (selected.isEmpty()) return
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete ${selected.size} Recording${if (selected.size > 1) "s" else ""}")
            .setMessage("This will permanently delete ${selected.size} recording${if (selected.size > 1) "s" else ""}. This cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                batchDeleteRecordings(selected)
            }
            .show()
    }
    
    private fun batchDeleteRecordings(recordings: List<RecordingFile>) {
        if (scanExecutor.isShutdown) return
        
        scanExecutor.submit {
            var deleted = 0
            var failed = 0
            
            for (recording in recordings) {
                if (RecordingScanner.deleteRecording(recording)) {
                    deleted++
                } else {
                    failed++
                }
            }
            
            activity?.runOnUiThread {
                if (isAdded) {
                    val msg = if (failed > 0) {
                        "$deleted deleted, $failed failed"
                    } else {
                        "$deleted recording${if (deleted > 1) "s" else ""} deleted"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    exitSelectMode()
                    loadRecordingsForSelectedDate()
                    updateCalendar()
                    loadStorageStats()
                }
            }
        }
    }
    
    private fun exitSelectMode() {
        recordingAdapter.exitSelectMode()
        selectToolbar?.visibility = View.GONE
    }
    
    override fun onResume() {
        super.onResume()
        // Invalidate cache on resume to pick up new recordings
        RecordingScanner.invalidateCache()
        updateCalendar()
        loadRecordingsForSelectedDate()
        loadStorageStats()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Shutdown executor to prevent memory leaks
        scanExecutor.shutdown()
    }
}
