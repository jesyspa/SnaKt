package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.KtSourceElement

class ExactLocationException(val source: KtSourceElement?, override val message: String) : Exception()