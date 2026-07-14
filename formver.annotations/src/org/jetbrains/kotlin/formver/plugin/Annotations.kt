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

@Target(AnnotationTarget.PROPERTY)
annotation class Manual
