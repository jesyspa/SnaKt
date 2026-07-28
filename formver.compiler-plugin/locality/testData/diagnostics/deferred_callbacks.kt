// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

class Request

fun @Borrowed Request.respond(status: Int) {}

// `action` is borrowed: it is run synchronously and never retained, so it may
// safely capture borrowed state from its surroundings.
fun runScoped(action: @Borrowed () -> Unit) {
    action()
}

// `later` is an ordinary (escaping) callback that may be stored and run after the call returns.
var later: () -> Unit = {}

fun `process the request in a scoped callback`(request: @Borrowed Request) {
    runScoped {
        request.respond(200)
    }
}

fun `defer handling and leak the request`(request: @Borrowed Request) {
    later = <!LOCALITY_MISMATCH!>{
        request.respond(500)
    }<!>
}
