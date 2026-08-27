// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed


fun requireLocalFunction(f: (@Borrowed Any) -> Unit) {}

fun requireGlobalFunction(f: (Any) -> Unit) {}

fun requireTwoLocalFunctions(f: (@Borrowed Any) -> Unit, g: (@Borrowed Any) -> Unit) {}

fun requireLocalLocalFunction(f: (@Borrowed Any, @Borrowed Any) -> Unit) {}

fun requireLocalReceiverFunction(f: (@Borrowed Any).() -> Unit) {}

fun ((@Borrowed Any) -> Unit).requireLocalFunctionReceiver() {}

fun produceLocalFunction(): (@Borrowed Any) -> Unit =
    { _: @Borrowed Any -> Unit }

fun produceGlobalFunction(): (Any) -> Unit =
    { _: Any -> Unit }

fun produceGlobalLocalFunction(): (Any, @Borrowed Any) -> Unit =
    { _: Any, _: @Borrowed Any -> Unit }

fun produceGlobalReceiverFunction(): (Any).() -> Unit =
    { Unit }

fun `pass global function as local function argument`() {
    requireLocalFunction(<!LOCALITY_CONTRACT_MISMATCH!>produceGlobalFunction()<!>)
}

fun `pass global function as named local function argument`() {
    requireLocalFunction(f = <!LOCALITY_CONTRACT_MISMATCH!>produceGlobalFunction()<!>)
}

fun `pass global function as second local function argument`() {
    requireTwoLocalFunctions(produceLocalFunction(), <!LOCALITY_CONTRACT_MISMATCH!>produceGlobalFunction()<!>)
}

fun `pass global-local function as local-local function argument`() {
    requireLocalLocalFunction(<!LOCALITY_CONTRACT_MISMATCH!>produceGlobalLocalFunction()<!>)
}

fun `pass local function as global function argument`() {
    requireGlobalFunction(produceLocalFunction())
}

fun `pass global receiver function as local receiver function`() {
    requireLocalReceiverFunction(<!LOCALITY_CONTRACT_MISMATCH!>produceGlobalReceiverFunction()<!>)
}

fun `pass global function as local function receiver`() {
    <!LOCALITY_CONTRACT_MISMATCH!>produceGlobalFunction()<!>.requireLocalFunctionReceiver()
}
