package rinha.api

import kyo.*
import rinha.db.DB
import scala.concurrent.duration.*
import sttp.tapir.server.netty.*

object Server extends App:

    def flag(name: String, default: String) =
        Option(System.getenv(name))
            .getOrElse(System.getProperty(name, default))

    val port = flag("PORT", "9999").toInt

    val dbConfig =
        DB.Config(
            flag("DB_PATH", "/app/data/"),
            flag("flushInternalMs", "1000").toInt.millis
        )

    val options =
        NettyKyoServerOptions
            .default(enableLogging = false)
            .forkExecution(false)

    val cfg =
        NettyConfig.default
            .withSocketKeepAlive
            .copy(lingerTimeout = None)

    val server =
        NettyKyoServer(options, cfg)
            .host("0.0.0.0")
            .port(port)

    val db      = Envs[DB.Config].run(dbConfig)(DB.init)
    val handler = Envs[DB].run(db)(Handler.init)
    val init    = Envs[Handler].run[Unit, Routes](handler)(Endpoints.init)

    val io = defer {
        await(Consoles.println(s"Server starting on port $port..."))
        val binding = await(Routes.run(server)(init))
        await(Consoles.println(s"Server started: ${binding.localSocket}"))
    }
    IOs.run(Fibers.run(io))

end Server
