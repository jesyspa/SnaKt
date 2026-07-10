// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

class Connection

fun @Borrowed Connection.execute(sql: Any) {}

fun log(message: @Borrowed Any) {}

fun withConnection(block: (@Borrowed Connection) -> Unit) {
    //...
}

fun useConnection(connection: @Borrowed Connection) {
    connection.execute("SELECT 1")
}

fun `run a query within the connection scope`() {
    withConnection { connection ->
        connection.execute("SELECT * FROM users")
        log(connection)
    }
}

fun `forward the connection to a borrowing helper`() {
    withConnection { connection ->
        useConnection(connection)
    }
}

var leakedConnection: Connection? = null

fun `leak the connection into global state`() {
    withConnection { connection ->
        leakedConnection = <!LOCALITY_MISMATCH!>connection<!>
    }
}

var onShutdown: () -> Unit = {}

fun `leak the connection through a deferred callback`() {
    withConnection { connection ->
        onShutdown = <!LOCALITY_MISMATCH!>{ log(connection) }<!>
    }
}
