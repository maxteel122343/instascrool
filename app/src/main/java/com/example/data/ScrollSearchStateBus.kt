package com.example.data

import com.example.model.ImageQuality
import com.example.model.OverlayUiVersion
import com.example.model.PhotoItem
import com.example.model.SearchEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ScrollSearchStateBus {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _currentUsername = MutableStateFlow<String?>("cristiano")
    val currentUsername: StateFlow<String?> = _currentUsername.asStateFlow()

    private val _selectedSearchEngine = MutableStateFlow(SearchEngine.GOOGLE)
    val selectedSearchEngine: StateFlow<SearchEngine> = _selectedSearchEngine.asStateFlow()

    private val _isSafeSearchDisabled = MutableStateFlow(true)
    val isSafeSearchDisabled: StateFlow<Boolean> = _isSafeSearchDisabled.asStateFlow()

    private val _imageQuality = MutableStateFlow(ImageQuality.HIGH)
    val imageQuality: StateFlow<ImageQuality> = _imageQuality.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isOverlayActive = MutableStateFlow(true)
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    private val _isBubbleCollapsed = MutableStateFlow(false)
    val isBubbleCollapsed: StateFlow<Boolean> = _isBubbleCollapsed.asStateFlow()

    private val _activeVersion = MutableStateFlow(OverlayUiVersion.F7)
    val activeVersion: StateFlow<OverlayUiVersion> = _activeVersion.asStateFlow()

    private val _tags = MutableStateFlow(
        listOf("tudo", "model", "cosplay", "photoshoot", "outfit", "wallpaper", "lifestyle")
    )
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _selectedTag = MutableStateFlow("tudo")
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    // Holds the exact text shown in the search bar: e.g. "@cristiano" or "@cristiano model"
    private val _searchBarText = MutableStateFlow("@cristiano")
    val searchBarText: StateFlow<String> = _searchBarText.asStateFlow()

    private val _heightRatio = MutableStateFlow(0.50f)
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

    private val _currentCapturedBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    val currentCapturedBitmap: StateFlow<android.graphics.Bitmap?> = _currentCapturedBitmap.asStateFlow()

    private val _fullscreenImages = MutableStateFlow<List<String>>(emptyList())
    val fullscreenImages: StateFlow<List<String>> = _fullscreenImages.asStateFlow()

    private val _fullscreenInitialIndex = MutableStateFlow(0)
    val fullscreenInitialIndex: StateFlow<Int> = _fullscreenInitialIndex.asStateFlow()

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

    /**
     * Called when InstagramAccessibilityService detects a new creator.
     * Updates current creator and sets search bar to "@<creator> <tag>"
     */
    fun updateDetectedUsername(username: String) {
        val clean = username.trim().removePrefix("@")
        if (clean.isBlank()) return
        if (_currentUsername.value != clean) {
            _currentUsername.value = clean
            _detectionCount.value += 1
            val tagSuffix = if (_selectedTag.value.isNotEmpty() && _selectedTag.value != "tudo") " ${_selectedTag.value}" else ""
            _searchBarText.value = "@$clean$tagSuffix"
            notifyEffectiveQueryChanged()
        }
    }

    fun setSearchEngine(engine: SearchEngine) {
        if (_selectedSearchEngine.value != engine) {
            _selectedSearchEngine.value = engine
            notifyEffectiveQueryChanged()
        }
    }

    fun toggleSafeSearch() {
        _isSafeSearchDisabled.value = !_isSafeSearchDisabled.value
        notifyEffectiveQueryChanged()
    }

    fun setSafeSearchDisabled(disabled: Boolean) {
        if (_isSafeSearchDisabled.value != disabled) {
            _isSafeSearchDisabled.value = disabled
            notifyEffectiveQueryChanged()
        }
    }

    fun setVersion(version: OverlayUiVersion) {
        _activeVersion.value = version
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setImageQuality(quality: ImageQuality) {
        _imageQuality.value = quality
        notifyEffectiveQueryChanged()
    }

    fun setDarkTheme(dark: Boolean) {
        _isDarkTheme.value = dark
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

    /**
     * When user taps a tag, append or update it on the current search bar text
     */
    fun selectTag(tag: String) {
        _selectedTag.value = tag
        val current = _searchBarText.value.trim()
        val userMatch = Regex("^@[a-zA-Z0-9._]+").find(current)
        if (userMatch != null) {
            val userPrefix = userMatch.value
            val tagSuffix = if (tag != "tudo") " $tag" else ""
            _searchBarText.value = "$userPrefix$tagSuffix"
        } else if (current.isNotEmpty()) {
            val tagSuffix = if (tag != "tudo") " $tag" else ""
            _searchBarText.value = "$current$tagSuffix"
        } else {
            val user = _currentUsername.value ?: "instagram"
            val tagSuffix = if (tag != "tudo") " $tag" else ""
            _searchBarText.value = "@$user$tagSuffix"
        }
        notifyEffectiveQueryChanged()
    }

    fun addCustomTag(newTag: String) {
        val clean = newTag.trim().lowercase().removePrefix("#")
        if (clean.isNotEmpty() && !_tags.value.contains(clean)) {
            _tags.value = _tags.value + clean
            selectTag(clean)
        }
    }

    fun removeTag(tag: String) {
        if (tag != "tudo" && _tags.value.contains(tag)) {
            _tags.value = _tags.value - tag
            if (_selectedTag.value == tag) {
                selectTag("tudo")
            }
        }
    }

    /**
     * User explicitly typed in the search bar.
     * If user keeps @name, it includes it.
     * If user removes @name, only what was typed is used.
     */
    fun setSearchBarText(text: String) {
        _searchBarText.value = text
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

    fun openFullscreenPhoto(
        selectedUrl: String,
        allUrls: List<String> = emptyList(),
        explicitIndex: Int = -1
    ) {
        _currentCapturedBitmap.value = null
        val list = if (allUrls.isNotEmpty()) allUrls else listOf(selectedUrl)
        val index = if (explicitIndex in list.indices) {
            explicitIndex
        } else {
            val idx = list.indexOf(selectedUrl)
            if (idx >= 0) idx else 0
        }
        _fullscreenImages.value = list
        _fullscreenInitialIndex.value = index
        _isPhotoDetailOpen.value = true
    }

    fun openFullscreenWithBitmap(
        bitmap: android.graphics.Bitmap?,
        selectedUrl: String,
        allUrls: List<String> = emptyList(),
        explicitIndex: Int = -1
    ) {
        _currentCapturedBitmap.value = if (bitmap != null && bitmap.width > 10 && bitmap.height > 10) bitmap else null
        val list = if (allUrls.isNotEmpty()) {
            allUrls
        } else if (selectedUrl.isNotBlank()) {
            listOf(selectedUrl)
        } else {
            emptyList()
        }
        val index = if (explicitIndex in list.indices) {
            explicitIndex
        } else if (selectedUrl.isNotBlank()) {
            val idx = list.indexOf(selectedUrl)
            if (idx >= 0) idx else 0
        } else {
            0
        }
        _fullscreenImages.value = list
        _fullscreenInitialIndex.value = index
        _isPhotoDetailOpen.value = true
    }

    fun closeFullscreenPhoto() {
        _currentCapturedBitmap.value = null
        _isPhotoDetailOpen.value = false
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

    /**
     * Parses the current search bar text into effective Google search query.
     * If it contains @name, converts '@name' to 'name' and keeps the rest of the query.
     * If user erased '@name', only the typed text is queried.
     */
    fun getEffectiveSearchQuery(): String {
        val raw = _searchBarText.value.trim()
        if (raw.isEmpty()) {
            val user = _currentUsername.value ?: "instagram"
            return "$user instagram"
        }
        // Remove '@' symbol so search engine searches creator's name + words
        val clean = raw.replace(Regex("@([a-zA-Z0-9._]+)"), "$1").trim()
        return clean.ifEmpty { "instagram" }
    }

    fun notifyEffectiveQueryChanged() {
        val query = getEffectiveSearchQuery()
        onQueryChangedListener?.invoke(query)
    }
}
