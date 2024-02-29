package rinha

import java.time.Instant
import zio.json.JsonCodec
import zio.json.jsonField

case class Transaction(
    @jsonField("valor")
    amount: Int,
    @jsonField("tipo")
    kind: String,
    @jsonField("descricao")
    description: Option[String],
    @jsonField("realizada_em")
    timestamp: Option[Instant]
) derives JsonCodec

sealed trait Result

case object Denied
    extends Result

case class Processed(
    @jsonField("limite")
    limit: Int,
    @jsonField("saldo")
    balance: Int
) extends Result derives JsonCodec

case class Statement(
    @jsonField("saldo")
    balance: Balance,
    @jsonField("ultimas_transacoes")
    lastTransactions: Seq[Transaction]
) derives JsonCodec

case class Balance(
    total: Int,
    @jsonField("data_extrato")
    date: Instant,
    @jsonField("limite")
    limit: Int
) derives JsonCodec
