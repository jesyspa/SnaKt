/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

import org.jetbrains.kotlin.formver.plugin.runners.AbstractPhasedDiagnosticTest
import org.jetbrains.kotlin.formver.plugin.runners.AbstractBugReproductionTest
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "formver.compiler-plugin/testData", testsRoot = "formver.compiler-plugin/test-gen") {
            testClass<AbstractPhasedDiagnosticTest> {
                model("diagnostics")
            }
        }
        testGroup(
            testDataRoot = "formver.compiler-plugin/bugReproductions",
            testsRoot = "formver.compiler-plugin/bug-reproduction-test-gen",
        ) {
            testClass<AbstractBugReproductionTest> {
                model("booleanExistentialWitness")
            }
        }
    }
}
