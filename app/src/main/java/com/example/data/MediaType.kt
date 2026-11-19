package com.example.data

enum class MediaType {
    IMAGE,
    GIF,
    VIDEO;

    companion object {
        fun fromStored(value: String): MediaType =
            entries.firstOrNull { it.name == value } ?: IMAGE
    }
}
