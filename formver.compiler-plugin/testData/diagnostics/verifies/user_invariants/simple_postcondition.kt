import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>testGreater<!>(init: Int) : Int {
    postconditions<Int> { result ->
        result > init
    }

    return init + 5
}

@AlwaysVerify
fun <!VIPER_TEXT!>testString<!>() : Char {
    postconditions<Char> { res ->
        res == 'a'
    }
    return "bab"[1]
}

@AlwaysVerify
fun <!VIPER_TEXT!>testWithPrecondition<!>(int: Int): Int {
    preconditions {
        int > 10
    }
    postconditions<Int> {
        it > 0
    }
    return int - 10
}

@AlwaysVerify
fun <!VIPER_TEXT!>returnGreater13<!>(): Int {
    postconditions<Int> {
        it > 13
    }
    return 16
}

@AlwaysVerify
fun <!VIPER_TEXT!>testPostconditionIsUsedByPrecondition<!>() = testWithPrecondition(returnGreater13())