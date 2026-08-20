/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

annotation class NeverConvert
annotation class NeverVerify
annotation class AlwaysVerify
annotation class DumpExpEmbeddings

@Target(AnnotationTarget.TYPE)
annotation class Unique

@Target(AnnotationTarget.TYPE)
annotation class Borrowed

@Target(AnnotationTarget.FUNCTION)
annotation class Pure

/**
 * Disables automatic permission management for the annotated element.
 *
 * On a class, the automatic folding, unfolding, and havoc of that class's uniqueness predicate
 * are turned off, leaving its permissions to be managed explicitly.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class Manual
