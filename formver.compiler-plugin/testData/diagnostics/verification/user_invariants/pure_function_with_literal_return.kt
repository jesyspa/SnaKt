import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>annotatedIntLitReturn<!>(): Int {
    postconditions<Int> { result ->
        result == 42
        forAll<Int> {
            triggers (it == result)
            (it == result) implies (it == 42)
        }
    }
    return 42
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>annotatedBoolLitReturn<!>(): Boolean {
    postconditions<Boolean> { result ->
        result
    }
    return true
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>annotatedCharLitReturn<!>(): Char {
    postconditions<Char> { result ->
        result == 'A'
    }
    return 'A'
}

@AlwaysVerify
@Pure
fun <!VIPER_TEXT!>annotatedStringLitReturn<!>(): String {
    postconditions<String> { result ->
        result == "Hello SnaKt"
    }
    return "Hello SnaKt"
}