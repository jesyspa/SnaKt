// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `assign mixed function if-expression to local function`(
    f: (Any) -> Unit,
    g: (@Borrowed Any) -> Unit
) {
    val h: (@Borrowed Any) -> Unit = <!LOCALITY_CONTRACT_MISMATCH!>if (true) f else g<!>
}

fun `assign mixed function when-expression to local function`(
    f: (Any, @Borrowed Any) -> Unit,
    g: (@Borrowed Any, @Borrowed Any) -> Unit
) {
    val h: (@Borrowed Any, @Borrowed Any) -> Unit = <!LOCALITY_CONTRACT_MISMATCH!>when {
        true -> f
        else -> g
    }<!>
}

fun `assign mixed function try-expression to local function`(
    f: (Any) -> Unit,
    g: (@Borrowed Any) -> Unit
) {
    val h: (@Borrowed Any) -> Unit = <!LOCALITY_CONTRACT_MISMATCH!>try {
        f
    } catch (_: Throwable) {
        g
    }<!>
}

fun `assign mixed function nested control-flow to local function`(
    f: (Any) -> Unit,
    g: (@Borrowed Any) -> Unit
) {
    val h: (@Borrowed Any) -> Unit = <!LOCALITY_CONTRACT_MISMATCH!>if (true) {
        when {
            true -> f
            else -> g
        }
    } else {
        g
    }<!>
}
