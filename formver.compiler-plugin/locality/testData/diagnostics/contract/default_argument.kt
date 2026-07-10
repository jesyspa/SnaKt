// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun localFunction(): (@Borrowed Any) -> Unit =
    { _: @Borrowed Any -> Unit }

fun globalFunction(): (Any) -> Unit =
    { _: Any -> Unit }

fun `assign global function as global default argument`(
    f: (Any) -> Unit = globalFunction()
) {}

fun `assign local function as global default argument`(
    f: (Any) -> Unit = localFunction()
) {}

fun `assign global function as local default argument`(
    f: (@Borrowed Any) -> Unit = <!LOCALITY_CONTRACT_MISMATCH!>globalFunction()<!>
) {}

fun `assign local function as local default argument`(
    f: (@Borrowed Any) -> Unit = localFunction()
) {}

class `assign global function as local default argument in constructor`(
    f: (@Borrowed Any) -> Unit = <!LOCALITY_CONTRACT_MISMATCH!>globalFunction()<!>
)
