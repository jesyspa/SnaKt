/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core

fun <T> List<T>.forEachWithIsLast(f: (T, Boolean) -> Unit) = indices.forEach { f(this[it], it == lastIndex) }
fun <T, R> List<T>.mapWithIsLast(f: (T, Boolean) -> R): List<R> = indices.map { f(this[it], it == lastIndex) }

/**
 * Similar to `all`, but does not short-circuit.
 */
fun <T> Sequence<T>.exhaustiveAll(predicate: (T) -> Boolean): Boolean {
    var result = true
    for (item in this) {
        if (!predicate(item)) result = false
    }
    return result
}