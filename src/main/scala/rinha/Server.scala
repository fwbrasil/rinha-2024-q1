package rinha

import kyo.*
import sttp.tapir.server.netty.*

object Server extends App:
    val mongoUrl =
        Option(System.getenv("MONGO_URL"))
            .getOrElse("mongodb://db")

    val ledgerPath =
        Option(System.getenv("LEDGER_PATH"))
            .getOrElse("ledger.dat")

    val options =
        NettyKyoServerOptions
            .default(enableLogging = false)
            .forkExecution(false)

    val cfg =
        NettyConfig.default
            .withSocketKeepAlive
            .socketBacklog(1)
            // .withAddLoggingHandler
            .copy(socketTimeout = None)

    val server =
        NettyKyoServer(options, cfg)
            .host("0.0.0.0")
            .port(8080)

    val a: NettyKyoServerBinding < (Envs[Ledger] & Envs[Store] & Fibers) =
        Envs[Handler].run(Handler.init) {
            Routes.run(server)(Endpoints.init)
        }

    val b = Envs[Store].run[NettyKyoServerBinding, Fibers & Envs[Ledger]](Store.init(mongoUrl))(a)
    val c = Envs[Ledger].run(Ledger.init(ledgerPath))(b)

    IOs.run(Fibers.run(Resources.run(c)))

end Server
