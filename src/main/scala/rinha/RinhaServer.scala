package rinha

import ch.qos.logback.core.status.Status
import kyo.*
import kyo.direct.*
import kyo.server.NettyKyoServer
import kyo.server.NettyKyoServerOptions
import rinha.Ledger.Result
import rinha.RinhaServer.ExtratoResponse.Saldo
import rinha.RinhaServer.ExtratoResponse.Transacao
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.zio._
import sttp.tapir.static.StaticErrorOutput.BadRequest
import zio.json.JsonCodec
import zio.json.JsonEncoder

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object RinhaServer extends KyoApp {

  val notFound = Aborts(StatusCode.NotFound)
  val unprocessableEntity = Aborts(StatusCode.UnprocessableEntity)
  val ledger = Ledger.offheap()

  run {
    Routes.run(NettyKyoServer().host("0.0.0.0").port(8080)) {
      Routes
        .add(
          _.post
            .in("clientes" / path[Int]("id") / "transacoes")
            .errorOut(statusCode)
            .in(jsonBody[TransacaoRequest])
            .out(jsonBody[TransacaoResponse])
        ) { (account, request) =>
          import request._
          defer {
            // validations
            await {
              if account < 0 || account > 5 then notFound
              else if descricao.isEmpty || descricao.exists(d =>
                  d.size > 10 || d.isEmpty()
                )
              then unprocessableEntity
              else if tipo != "c" && tipo != "d" then unprocessableEntity
              else ()
            }

            // perform transaction
            val result = await {
              IOs {
                val amount = if (tipo == "c") valor else -valor
                val desc = new Array[Char](10)
                val d = descricao.get
                for (i <- 0 until d.size) {
                  desc(i) = d.charAt(i)
                }
                ledger.transaction(account, amount, desc)
              }
            }

            // return result
            result match {
              case Result.Denied =>
                await(unprocessableEntity)
              case Result.Processed(balance, limit) =>
                TransacaoResponse(limit, balance)
            }
          }
        }
        .andThen {
          Routes.add(
            _.get
              .in("clientes" / path[Int]("id") / "extrato")
              .errorOut(statusCode)
              .out(jsonBody[ExtratoResponse])
          ) { account =>
            defer {
              // validations
              await {
                if account < 0 || account > 5 then notFound
                else ()
              }

              // get statement
              val result = await {
                IOs(ledger.statement(account))
              }

              // generate response
              val transactions =
                result.transactions.toList
                  .filter(_ != null)
                  .reverse
                  .map { t =>
                    Transacao(
                      t.amount.abs,
                      if (t.amount < 0) "d" else "c",
                      t.desc.takeWhile(_ != 0).mkString,
                      formatTimestamp(t.timestamp)
                    )
                  }
              ExtratoResponse(
                Saldo(
                  result.balance,
                  formatTimestamp(System.currentTimeMillis()),
                  result.limit
                ),
                transactions
              )
            }
          }
        }
    }
  }

  case class TransacaoRequest(
      valor: Int,
      tipo: String,
      descricao: Option[String]
  ) derives JsonCodec
  case class TransacaoResponse(limite: Int, saldo: Int) derives JsonCodec

  case class ExtratoResponse(
      saldo: ExtratoResponse.Saldo,
      ultimas_transacoes: List[ExtratoResponse.Transacao]
  ) derives JsonCodec

  object ExtratoResponse {
    case class Saldo(total: Int, data_extrato: String, limite: Int)
        derives JsonCodec
    case class Transacao(
        valor: Int,
        tipo: String,
        descricao: String,
        realizada_em: String
    ) derives JsonCodec
  }

  private val formatter =
    DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
      .withZone(ZoneId.of("UTC"))

  def formatTimestamp(timestamp: Long): String =
    formatter.format(Instant.ofEpochMilli(timestamp))
}
