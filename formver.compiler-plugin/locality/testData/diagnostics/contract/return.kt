// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun localFunction(): (@Borrowed Any) -> Unit =
    { _: @Borrowed Any -> Unit }

fun globalFunction(): (Any) -> Unit =
    { _: Any -> Unit }

fun `return global function explicitly`(): (@Borrowed Any) -> Unit {
    return <!LOCALITY_CONTRACT_MISMATCH!>globalFunction()<!>
}

fun `return global function from expression body`(): (@Borrowed Any) -> Unit =
    <!LOCALITY_CONTRACT_MISMATCH!>globalFunction()<!>

fun `return local function as global function`(): (Any) -> Unit =
    localFunction()

fun `return mixed function if-expression`(
    f: (Any) -> Unit,
    g: (@Borrowed Any) -> Unit
): (@Borrowed Any) -> Unit =
    <!LOCALITY_CONTRACT_MISMATCH!>if (true) f else g<!>

fun localFunctionProducer(): () -> ((@Borrowed Any) -> Unit) =
    { localFunction() }

fun globalFunctionProducer(): () -> ((Any) -> Unit) =
    { globalFunction() }

fun `return global function producer as local function producer`(): () -> ((@Borrowed Any) -> Unit) =
    <!LOCALITY_CONTRACT_MISMATCH!>globalFunctionProducer()<!>

fun `return local function producer as global function producer`(): () -> ((Any) -> Unit) =
    localFunctionProducer()
