// Adapted from Codeforces 734A, "Anton and Danik":
// https://codeforces.com/problemset/problem/734/A
import org.jetbrains.kotlin.formver.plugin.*

@Pure
fun gameBalance(games: String, end: Int): Int {
    preconditions {
        0 <= end && end <= games.length
        forAll<Int> { i ->
            (0 <= i && i < games.length) implies
                    (games[i] == 'A' || games[i] == 'D')
        }
    }
    return if (end == 0) {
        0
    } else if (games[end - 1] == 'A') {
        gameBalance(games, end - 1) + 1
    } else {
        gameBalance(games, end - 1) - 1
    }
}

@AlwaysVerify
fun winner(games: String): Int {
    preconditions {
        1 <= games.length && games.length <= 100_000
        forAll<Int> { i ->
            (0 <= i && i < games.length) implies
                    (games[i] == 'A' || games[i] == 'D')
        }
    }
    postconditions<Int> { result ->
        result == -1 || result == 0 || result == 1
        (result == 1) == (gameBalance(games, games.length) > 0)
        (result == -1) == (gameBalance(games, games.length) < 0)
        (result == 0) == (gameBalance(games, games.length) == 0)
    }

    var i = 0
    var balance = 0
    while (i < games.length) {
        loopInvariants {
            0 <= i && i <= games.length
            balance == gameBalance(games, i)
        }
        if (games[i] == 'A') {
            balance += 1
        } else {
            balance -= 1
        }
        i += 1
    }

    return if (balance > 0) 1 else if (balance < 0) -1 else 0
}
