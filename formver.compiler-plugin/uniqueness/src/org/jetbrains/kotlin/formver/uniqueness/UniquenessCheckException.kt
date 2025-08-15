package org.jetbrains.kotlin.formver.uniqueness

import org.jetbrains.kotlin.KtSourceElement

class UniquenessCheckException(val source: KtSourceElement?, override val message: String) : Exception()