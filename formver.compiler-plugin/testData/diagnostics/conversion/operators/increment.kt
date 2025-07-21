// NEVER_VALIDATE

fun <!VIPER_TEXT!>test_simple<!>() {
    var x = 10
    x += 5
    --x
    x -= 3
    ++x
    x *= 2
    x /= 4
}

fun <!VIPER_TEXT!>test_postincvrement<!>() {
    var x = 10
    val first = x++
    val second = x--
    val unary = x
}