package com.overdrive.app.ui.adapter

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.overdrive.app.ui.model.RecordingFile
import com.overdrive.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Adapter for displaying recording files with video thumbnails.
 * Supports multi-select mode for batch operations.
 */
class RecordingAdapter(
    private val onPlay: (RecordingFile) -> Unit,
    private val onDelete: (RecordingFile) -> Unit,
    private val onSelectionChanged: ((Int) -> Unit)? = null
) : ListAdapter<RecordingFile, RecordingAdapter.RecordingViewHolder>(RecordingDiffCallback()) {
    
    // Cache for thumbnails
    private val thumbnailCache = mutableMapOf<String, Bitmap?>()
    
    // Multi-select state
    var selectMode = false
        private set
    private val selectedItems = mutableSetOf<String>() // paths
    
    fun enterSelectMode() {
        selectMode = true
        selectedItems.clear()
        notifyDataSetChanged()
    }
    
    fun exitSelectMode() {
        selectMode = false
        selectedItems.clear()
        notifyDataSetChanged()
    }
    
    fun selectAll() {
        for (i in 0 until itemCount) {
            selectedItems.add(getItem(i).path)
        }
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedItems.size)
    }
    
    fun deselectAll() {
        selectedItems.clear()
        notifyDataSetChanged()
        onSelectionChanged?.invoke(0)
    }
    
    fun getSelectedRecordings(): List<RecordingFile> {
        return currentList.filter { it.path in selectedItems }
    }
    
    val selectedCount: Int get() = selectedItems.size
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        private val tvTypeBadge: TextView = itemView.findViewById(R.id.tvTypeBadge)
        private val tvAiBadge1: TextView = itemView.findViewById(R.id.tvAiBadge1)
        private val tvAiBadge2: TextView = itemView.findViewById(R.id.tvAiBadge2)
        private val tvAiBadge3: TextView = itemView.findViewById(R.id.tvAiBadge3)
        private val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvRecordingTime: TextView = itemView.findViewById(R.id.tvRecordingTime)
        private val tvSize: TextView = itemView.findViewById(R.id.tvSize)
        private val btnPlay: ImageButton = itemView.findViewById(R.id.btnPlay)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)

        private val aiBadgeViews = listOf(tvAiBadge1, tvAiBadge2, tvAiBadge3)
        
        fun bind(recording: RecordingFile) {
            // Type badge
            tvTypeBadge.text = when (recording.type) {
                RecordingFile.RecordingType.SENTRY    -> "SENTRY"
                RecordingFile.RecordingType.PROXIMITY -> "PROXIMITY"
                RecordingFile.RecordingType.NORMAL    -> "NORMAL"
            }

            // AI detection badges (person / car / bike with confidence %)
            val detections = recording.aiDetections
            aiBadgeViews.forEachIndexed { i, tv ->
                val det = detections.getOrNull(i)
                if (det != null) {
                    val (icon, bgColor, fgColor) = when (det.type) {
                        "person" -> Triple("🚶", Color.parseColor("#26EF4444"), Color.parseColor("#EF4444"))
                        "car"    -> Triple("🚗", Color.parseColor("#263B82F6"), Color.parseColor("#3B82F6"))
                        else     -> Triple("🚲", Color.parseColor("#26F97316"), Color.parseColor("#F97316"))
                    }
                    tv.text = if (det.confPct > 0) "$icon ${det.confPct}%" else icon
                    tv.setTextColor(fgColor)
                    val bg = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 8f * itemView.context.resources.displayMetrics.density
                        setColor(bgColor)
                    }
                    tv.background = bg
                    tv.visibility = View.VISIBLE
                } else {
                    tv.visibility = View.GONE
                }
            }

            // Filename
            tvFileName.text = recording.name

            // Meta row
            tvDate.text = recording.formattedDate
            tvRecordingTime.text = recording.formattedTime
            tvSize.text = recording.formattedSize
            
            // Load thumbnail
            loadThumbnail(recording)
            
            // Multi-select mode
            if (selectMode) {
                cbSelect.visibility = View.VISIBLE
                cbSelect.setOnCheckedChangeListener(null)
                cbSelect.isChecked = recording.path in selectedItems
                btnPlay.visibility = View.GONE
                btnDelete.visibility = View.GONE
                
                cbSelect.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedItems.add(recording.path)
                    else selectedItems.remove(recording.path)
                    onSelectionChanged?.invoke(selectedItems.size)
                }
                itemView.setOnClickListener { cbSelect.isChecked = !cbSelect.isChecked }
                itemView.setOnLongClickListener(null)
            } else {
                cbSelect.setOnCheckedChangeListener(null)
                cbSelect.visibility = View.GONE
                btnPlay.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE
                
                btnPlay.setOnClickListener { onPlay(recording) }
                btnDelete.setOnClickListener { onDelete(recording) }
                itemView.setOnClickListener { onPlay(recording) }
                itemView.setOnLongClickListener {
                    enterSelectMode()
                    selectedItems.add(recording.path)
                    notifyDataSetChanged()
                    onSelectionChanged?.invoke(selectedItems.size)
                    true
                }
            }
        }
        
        private fun loadThumbnail(recording: RecordingFile) {
            val path = recording.path
            if (thumbnailCache.containsKey(path)) {
                val cached = thumbnailCache[path]
                if (cached != null) ivThumbnail.setImageBitmap(cached)
                else ivThumbnail.setImageResource(R.color.surface_variant)
                return
            }
            ivThumbnail.setImageResource(R.color.surface_variant)
            CoroutineScope(Dispatchers.IO).launch {
                val thumbnail = extractThumbnail(path)
                thumbnailCache[path] = thumbnail
                withContext(Dispatchers.Main) {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION &&
                        getItem(bindingAdapterPosition).path == path && thumbnail != null) {
                        ivThumbnail.setImageBitmap(thumbnail)
                    }
                }
            }
        }
        
        private fun extractThumbnail(path: String): Bitmap? {
            val thumbFile = java.io.File(path.replace(".mp4", ".thumb.jpg"))
            if (thumbFile.exists() && thumbFile.length() > 0) {
                try {
                    return android.graphics.BitmapFactory.decodeFile(thumbFile.absolutePath)
                } catch (e: Exception) { /* fall through */ }
            }
            return try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(path)
                val frame = retriever.getFrameAtTime(1_000_000)
                retriever.release()
                frame
            } catch (e: Exception) { null }
        }
    }
    
    fun clearCache() {
        thumbnailCache.clear()
    }
    
    private class RecordingDiffCallback : DiffUtil.ItemCallback<RecordingFile>() {
        override fun areItemsTheSame(oldItem: RecordingFile, newItem: RecordingFile) =
            oldItem.path == newItem.path
        override fun areContentsTheSame(oldItem: RecordingFile, newItem: RecordingFile) =
            oldItem == newItem
    }
}
