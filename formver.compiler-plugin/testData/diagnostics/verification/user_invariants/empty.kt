// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*


fun <!VIPER_TEXT!>bothEmpty<!>(arg: Boolean): Boolean {
    preconditions {}
    postconditions<Boolean> {}
    return arg
}

fun <!VIPER_TEXT!>preEmpty<!>(arg: Boolean): Boolean {
    preconditions {}
    postconditions<Boolean> { ret -> ret == arg }
    return arg
}

fun <!VIPER_TEXT!>postEmpty<!>(arg: Boolean): Boolean {
    preconditions {
        arg == true
    }
    postconditions<Boolean> {}
    return arg
}

fun <!VIPER_TEXT!>testInsertedReturn<!>() {
    preconditions {
        return@preconditions Unit
    }
    postconditions<Unit> {
    }
    return
}
