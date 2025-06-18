/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

import org.jetbrains.kotlin.formver.plugin.runners.AbstractFirLightTreeFormVerPluginDiagnosticsTest
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "formver.compiler-plugin/testData", testsRoot = "formver.compiler-plugin/test-gen") {
            testClass<AbstractFirLightTreeFormVerPluginDiagnosticsTest> {
                model("diagnostics")
            }
        }
    }
}
