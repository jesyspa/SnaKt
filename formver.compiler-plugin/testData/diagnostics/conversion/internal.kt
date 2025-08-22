// NEVER_VALIDATE

// Test function literals
fun testFunctionLiteral() {
    val f = <!VIPER_VERIFICATION_ERROR!>{ x: Int -> x + 1 }<!> // Anonymous function
}
// Test Double literals
fun testDoubleLiteral() {
    val x = <!VIPER_VERIFICATION_ERROR!>1.0<!>  // Double литерал
}

/*
// Test class-related errors
class TestClass
fun testClassUsages() {
    TestClass      // ResolvedQualifier
    TestClass::class  // Class literal
}*/

// Test when expression
fun <!VIPER_TEXT!>testWhen<!>(x: String) {
    when (x) {
        <!INCOMPATIBLE_TYPES!>1<!> -> println("one")
        else -> println("other")
    }
}

