// NEVER_VALIDATE

// Test function literals
fun <!VIPER_TEXT!>testFunctionLiteral<!>() {
    val f = { x: Int -> x + 1 } // Anonymous function
}
// Test Double literals
fun <!VIPER_TEXT!>testDoubleLiteral<!>() {
    val x = 1.0  // Double литерал
}

// Test class-related errors
class TestClass
fun <!VIPER_TEXT!>testClassUsages<!>() {
    TestClass      // ResolvedQualifier
    TestClass::class  // Class literal
}

// Test when expression
fun <!VIPER_TEXT!>testWhen<!>(x: String) {
    when (x) {
        1 -> println("one")
        else -> println("other")
    }
}

