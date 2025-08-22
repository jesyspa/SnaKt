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


