package com.example.data

import com.example.model.OverlayUiVersion
import com.example.model.PhotoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object ScrollSearchStateBus {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _currentUsername = MutableStateFlow<String?>("cristiano")
    val currentUsername: StateFlow<String?> = _currentUsername.asStateFlow()

    private val _isOverlayActive = MutableStateFlow(true)
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    private val _isBubbleCollapsed = MutableStateFlow(false)
    val isBubbleCollapsed: StateFlow<Boolean> = _isBubbleCollapsed.asStateFlow()

    private val _activeVersion = MutableStateFlow(OverlayUiVersion.V6)
    val activeVersion: StateFlow<OverlayUiVersion> = _activeVersion.asStateFlow()

    private val _tags = MutableStateFlow(
        listOf("tudo", "model", "cosplay", "photoshoot", "outfit", "wallpaper", "lifestyle")
    )
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _selectedTag = MutableStateFlow("tudo")
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    private val _customSearchQuery = MutableStateFlow("")
    val customSearchQuery: StateFlow<String> = _customSearchQuery.asStateFlow()

    private val _heightRatio = MutableStateFlow(0.48f)
    val heightRatio: StateFlow<Float> = _heightRatio.asStateFlow()

    private val _isSearchFocused = MutableStateFlow(false)
    val isSearchFocused: StateFlow<Boolean> = _isSearchFocused.asStateFlow()

    private val _photos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val photos: StateFlow<List<PhotoItem>> = _photos.asStateFlow()

    private val _isLoadingPhotos = MutableStateFlow(false)
    val isLoadingPhotos: StateFlow<Boolean> = _isLoadingPhotos.asStateFlow()

    private val _detectionCount = MutableStateFlow(0)
    val detectionCount: StateFlow<Int> = _detectionCount.asStateFlow()

    private val _isInstagramForeground = MutableStateFlow(false)
    val isInstagramForeground: StateFlow<Boolean> = _isInstagramForeground.asStateFlow()

    private val _isPhotoDetailOpen = MutableStateFlow(false)
    val isPhotoDetailOpen: StateFlow<Boolean> = _isPhotoDetailOpen.asStateFlow()

    private val _isV1WebViewExpanded = MutableStateFlow(false)
    val isV1WebViewExpanded: StateFlow<Boolean> = _isV1WebViewExpanded.asStateFlow()

    private val _bubbleX = MutableStateFlow(0)
    val bubbleX: StateFlow<Int> = _bubbleX.asStateFlow()

    private val _bubbleY = MutableStateFlow(400)
    val bubbleY: StateFlow<Int> = _bubbleY.asStateFlow()

    // Listener callbacks for overlay updates
    private var onQueryChangedListener: ((query: String) -> Unit)? = null
    private var onRequestFocusToggle: ((focused: Boolean) -> Unit)? = null

    fun setOnQueryChangedListener(listener: ((query: String) -> Unit)?) {
        onQueryChangedListener = listener
    }

    fun setOnRequestFocusToggle(listener: ((focused: Boolean) -> Unit)?) {
        onRequestFocusToggle = listener
    }

    fun updateDetectedUsername(username: String) {
        val clean = username.trim().removePrefix("@")
        if (clean.isBlank()) return
        if (_currentUsername.value != clean) {
            _currentUsername.value = clean
            _detectionCount.value += 1
            _customSearchQuery.value = "" // Reset manual query when new creator is detected
            notifyEffectiveQueryChanged()
        }
    }

    fun setVersion(version: OverlayUiVersion) {
        _activeVersion.value = version
    }

    fun setOverlayActive(active: Boolean) {
        _isOverlayActive.value = active
    }

    fun setBubbleCollapsed(collapsed: Boolean) {
        _isBubbleCollapsed.value = collapsed
        if (collapsed) {
            setSearchFocused(false)
        }
    }

    fun selectTag(tag: String) {
        _selectedTag.value = tag
        notifyEffectiveQueryChanged()
    }

    fun addCustomTag(newTag: String) {
        val clean = newTag.trim().lowercase()
        if (clean.isNotEmpty() && !_tags.value.contains(clean)) {
            _tags.value = _tags.value + clean
            _selectedTag.value = clean
            notifyEffectiveQueryChanged()
        }
    }

    fun removeTag(tag: String) {
        if (tag != "tudo" && _tags.value.contains(tag)) {
            _tags.value = _tags.value - tag
            if (_selectedTag.value == tag) {
                _selectedTag.value = "tudo"
                notifyEffectiveQueryChanged()
            }
        }
    }

    fun setCustomSearchQuery(query: String) {
        _customSearchQuery.value = query
        notifyEffectiveQueryChanged()
    }

    fun setSearchFocused(focused: Boolean) {
        _isSearchFocused.value = focused
        onRequestFocusToggle?.invoke(focused)
    }

    fun setHeightRatio(ratio: Float) {
        _heightRatio.value = ratio.coerceIn(0.20f, 0.85f)
    }

    fun setInstagramForeground(inForeground: Boolean) {
        _isInstagramForeground.value = inForeground
    }

    fun setPhotoDetailOpen(open: Boolean) {
        _isPhotoDetailOpen.value = open
    }

    fun setV1WebViewExpanded(expanded: Boolean) {
        _isV1WebViewExpanded.value = expanded
    }

    fun updateBubblePosition(x: Int, y: Int) {
        _bubbleX.value = x
        _bubbleY.value = y
    }

    fun updatePhotos(items: List<PhotoItem>, loading: Boolean = false) {
        _photos.value = items
        _isLoadingPhotos.value = loading
    }

    fun setLoadingPhotos(loading: Boolean) {
        _isLoadingPhotos.value = loading
    }

    fun getEffectiveSearchQuery(): String {
        val manual = _customSearchQuery.value.trim()
        if (manual.isNotEmpty()) return manual

        val user = _currentUsername.value ?: "instagram"
        val tag = _selectedTag.value
        return if (tag.isEmpty() || tag == "tudo") {
            "$user instagram"
        } else {
            "$user $tag"
        }
    }

    fun notifyEffectiveQueryChanged() {
        val query = getEffectiveSearchQuery()
        onQueryChangedListener?.invoke(query)
    }
}
