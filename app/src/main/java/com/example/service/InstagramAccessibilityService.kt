package com.example.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.ScrollSearchStateBus
import java.util.ArrayDeque
import java.util.Locale

class InstagramAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "InstagramDetector"
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val THROTTLE_MS = 40L // Low throttle as requested (~40ms)

        // Known Instagram resource IDs for creator usernames across different versions
        private val TARGET_VIEW_IDS = listOf(
            "clips_author_name",
            "reel_viewer_author_name",
            "row_feed_photo_profile_name",
            "author_name",
            "feed_header_user_name",
            "reel_header_title",
            "profile_user_name",
            "row_feed_profile_picture_container"
        )

        // Reserved non-username words common in Reels and Feed
        private val BLACKLIST_TERMS = hashSetOf(
            "instagram", "reels", "reel", "feed", "audio", "original", "som", "original audio",
            "follow", "following", "seguir", "seguindo", "curtidas", "liked", "curtir",
            "share", "compartilhar", "comments", "comentários", "comentarios",
            "sponsored", "patrocinado", "shop", "comprar", "ver", "mais", "music", "musica",
            "enviar", "send", "salvar", "save", "pesquisar", "search", "direct", "stories",
            "story", "mensagens", "notification", "notificações", "home", "início", "inicio"
        )
    }

    private var lastEventTime = 0L
    private var lastDetectedUser: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: return
        if (pkgName != INSTAGRAM_PACKAGE) {
            ScrollSearchStateBus.setInstagramForeground(false)
            return
        }

        ScrollSearchStateBus.setInstagramForeground(true)

        val now = SystemClock.uptimeMillis()
        if (now - lastEventTime < THROTTLE_MS) {
            return
        }
        lastEventTime = now

        val rootNode = rootInActiveWindow ?: return
        try {
            val username = extractUsernameFast(rootNode)
            if (!username.isNullOrBlank() && username != lastDetectedUser) {
                lastDetectedUser = username
                Log.d(TAG, "Detected new Instagram creator: @$username")
                ScrollSearchStateBus.updateDetectedUsername(username)
            }
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Fast, lightweight BFS scanner limited to 60 nodes to ensure <2ms execution
     * and zero UI lag during scroll.
     */
    private fun extractUsernameFast(root: AccessibilityNodeInfo): String? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        var visited = 0
        val maxNodes = 60

        while (!queue.isEmpty() && visited < maxNodes) {
            val current = queue.poll() ?: break
            visited++

            // Strategy 1: Check known Instagram View ID
            val viewId = current.viewIdResourceName
            if (viewId != null) {
                for (targetId in TARGET_VIEW_IDS) {
                    if (viewId.contains(targetId, ignoreCase = true)) {
                        val text = current.text?.toString()?.trim()
                        val valid = sanitizeUsername(text)
                        if (valid != null) {
                            return valid
                        }
                    }
                }
            }

            // Strategy 2: Check Content Description pattern (e.g. "Foto de perfil de zendaya")
            val contentDesc = current.contentDescription?.toString()
            if (!contentDesc.isNullOrBlank()) {
                val extracted = extractFromContentDescription(contentDesc)
                if (extracted != null) {
                    return extracted
                }
            }

            // Strategy 3: Check plain text if it strictly looks like an Instagram username
            val text = current.text?.toString()
            if (!text.isNullOrBlank()) {
                val candidate = sanitizeUsername(text)
                if (candidate != null) {
                    return candidate
                }
            }

            // Enqueue child nodes
            val childCount = current.childCount
            for (i in 0 until childCount) {
                current.getChild(i)?.let { child ->
                    queue.add(child)
                }
            }
        }

        return null
    }

    private fun extractFromContentDescription(desc: String): String? {
        val lower = desc.lowercase(Locale.ROOT)
        val prefixes = listOf(
            "foto de perfil de ",
            "profile picture of ",
            "foto do perfil de ",
            "ver o perfil de ",
            "view profile of "
        )
        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                val candidate = desc.substring(prefix.length).trim().removePrefix("@")
                val clean = candidate.split(" ").firstOrNull() ?: candidate
                return sanitizeUsername(clean)
            }
        }
        return null
    }

    private fun sanitizeUsername(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim().removePrefix("@")

        // Instagram usernames: 2-30 chars, alphanumeric with dots and underscores
        if (trimmed.length in 2..30 && trimmed.matches(Regex("^[a-zA-Z0-9._]+$"))) {
            val lower = trimmed.lowercase(Locale.ROOT)
            if (!BLACKLIST_TERMS.contains(lower) && !lower.all { it.isDigit() }) {
                return trimmed
            }
        }
        return null
    }

    override fun onInterrupt() {
        Log.w(TAG, "InstagramAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        ScrollSearchStateBus.setInstagramForeground(false)
    }
}
