// FULL_JDK
// NEVER_VALIDATE

class C

fun <!VIPER_TEXT!>identity<!>(x: C, y: C): Boolean {
    return x === y
}

fun <!VIPER_TEXT!>nonIdentity<!>(x: C, y: C): Boolean {
    return x !== y
}

fun <!VIPER_TEXT!>identityWithNull<!>(x: C?): Boolean {
    return x === null
}

fun <!VIPER_TEXT!>nonIdentityWithNull<!>(x: C?): Boolean {
    return x !== null
}
