package org.jetbrains.kotlin.formver.common

import org.jetbrains.kotlin.KtSourceElement

class SnaktInternalException(val source: KtSourceElement?, override val message: String) : Exception()