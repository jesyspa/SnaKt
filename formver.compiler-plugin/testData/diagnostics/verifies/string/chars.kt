import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify
import org.jetbrains.kotlin.formver.plugin.NeverConvert

class CharBox(val char: Char)

@AlwaysVerify
fun <!VIPER_TEXT!>testChars<!>(c: Char) {
    val box = CharBox('a')
    val charA = 'a'
    val cond1 = charA == box.char
    verify(cond1)
    val charZ = 'z'
    verify(
        charA == charZ - 25,
        charA - charZ == -25,
        charZ == charA + 25,
    )
    verify(
        charA <= charZ,
        (charA < charZ),
        (charZ > charA),
        charZ >= charZ,
    )
}