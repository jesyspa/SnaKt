// NEVER_VALIDATE

fun <!VIPER_TEXT!>addition<!>(x: Int) {
    val y = x + x
}
fun <!VIPER_TEXT!>subtraction<!>(x: Int) {
    val y = x - x
}
fun <!VIPER_TEXT!>multiplication<!>(x: Int) {
    val y = x * x
}
fun <!VIPER_TEXT!>division<!>(x: Int) {
    // will not verify because `x` is not guaranteed to be non-zero
    val y = x / x
}
