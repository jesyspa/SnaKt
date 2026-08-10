/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper

import java.io.File

private const val Z3_EXE_ENV_VAR = "Z3_EXE"

class ProverNotFoundException(message: String) : Exception(message)

/**
 * Resolves the Z3 executable the same way Silicon does ([Z3_EXE_ENV_VAR], then `PATH`), so the
 * path named in [checkProverIsUsable]'s diagnostic is the same one Silicon would otherwise have
 * failed on deep inside its own low-level "not a file" error.
 */
private fun resolveZ3Executable(): File {
    System.getenv(Z3_EXE_ENV_VAR)?.let { return File(it) }
    val executableName = if (System.getProperty("os.name").lowercase().startsWith("windows")) "z3.exe" else "z3"
    val searchPath = System.getenv("PATH").orEmpty().split(File.pathSeparatorChar)
    return searchPath.map { File(it, executableName) }.firstOrNull { it.isFile } ?: File(executableName)
}

/** Throws [ProverNotFoundException] with a user-facing diagnostic if no usable Z3 executable can be found. */
fun checkProverIsUsable() {
    val exe = resolveZ3Executable()
    val hint = "Set the $Z3_EXE_ENV_VAR environment variable to the path of a z3 binary, or install Z3 " +
        "(https://github.com/Z3Prover/z3/releases) and add it to PATH."
    if (!exe.isFile) {
        throw ProverNotFoundException("Could not find the Z3 prover executable at '${exe.path}'. $hint")
    }
    if (!exe.canExecute()) {
        throw ProverNotFoundException("The Z3 prover at '${exe.path}' is not executable. $hint")
    }
}
