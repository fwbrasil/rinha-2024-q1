package rinha

import java.time.Instant
import kyo.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import zio.json.JsonEncoder

object Endpoints:

    val init: Unit < (Envs[Handler] & Routes) =
        defer {

            val handler = await(Envs[Handler].get)

            await {
                Routes.add(
                    _.post
                        .in("clientes" / path[Int]("id") / "transacoes")
                        .errorOut(statusCode)
                        .in(jsonBody[TransacaoRequest])
                        .out(jsonBody[TransacaoResponse])
                )(handler.transaction)
            }

            await {
                Routes.add(
                    _.get
                        .in("clientes" / path[Int]("id") / "extrato")
                        .errorOut(statusCode)
                        .out(jsonBody[ExtratoResponse])
                )(handler.statement)
            }
        }

end Endpoints
