package org.jetbrains.kotlin.formver.uniqueness.plugin

/**
 * [PathTrie] tag to distinguish between intermediate path components and terminal path components.
 */
enum class Access {
    Intermediate, Terminal
}

/**
 * Joins two [Access] tags.
 */
fun Access.join(other: Access): Access =
    maxOf(this, other)
