package rinha

import kyo.*
import kyo.server.*

object Server extends App:

    val options =
        NettyKyoServerOptions
            .default(enableLogging = false)
            .forkExecution(false)

    val server = NettyKyoServer(options)
        .host("0.0.0.0")
        .port(8080)

    val a: NettyKyoServerBinding < (Envs[Ledger] & Envs[Store] & Fibers) =
        Envs[Handler].run(Handler.init) {
            Routes.run(server)(Endpoints.init)
        }

    val b = Envs[Store].run[NettyKyoServerBinding, Fibers & Envs[Ledger]](Store.init)(a)
    val c = Envs[Ledger].run(Ledger.init("/app/data/ledger.dat"))(b)

    IOs.run(Fibers.run(c))

end Server
