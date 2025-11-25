import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>annotatedIntLitReturn<!>(arg: Int): Int {
    preconditions {
        true
        arg >= 42
        arg <= 42
    }
    postconditions<Int> { result ->
        result == 42
        forAll<Int> {
            triggers (it == result)
            (it == result) implies (it == arg)
        }
    }
    return 42
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>annotatedBoolLitReturn<!>(arg: Int): Boolean {
    preconditions {
        arg <= 0
        arg >= 0
    }
    postconditions<Boolean> { result ->
        result
        arg == 0
    }
    return true
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>annotatedCharLitReturn<!>(arg: String): Char {
    preconditions {
        arg == "Hello SnaKt"
    }
    postconditions<Char> { result ->
        result == 'A'
    }
    return 'A'
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>annotatedStringLitReturn<!>(arg: Boolean): String {
    preconditions {
        arg
    }
    postconditions<String> { result ->
        result == "Hello SnaKt"
    }
    return "Hello SnaKt"
}