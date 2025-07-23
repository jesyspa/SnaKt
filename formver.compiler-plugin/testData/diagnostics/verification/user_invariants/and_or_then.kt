import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>resultOrArg<!>(arg: Boolean): Boolean {
    postconditions<Boolean> { res ->
        res || arg
    }
    return !arg
}

@AlwaysVerify
fun <!VIPER_TEXT!>testImplies<!>(arg: Boolean): Boolean {
    postconditions<Boolean> { res ->
        arg implies !res
        res implies !arg
        !arg implies res
        !res implies arg
    }
    return !arg
}

@AlwaysVerify
fun <!VIPER_TEXT!>testAnd<!>(arg1: Boolean, arg2: Boolean): Boolean {
    postconditions<Boolean> { res ->
        res implies (arg1 && arg2)
        !res implies (!arg1 || !arg2)
        (arg1 && arg2) implies res
        !arg1 implies !res
        !arg2 implies !res
    }
    return arg1 && arg2
}