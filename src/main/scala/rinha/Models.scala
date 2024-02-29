package rinha

import java.time.Instant
import zio.json.JsonCodec

case class TransacaoRequest(
    valor: Int,
    tipo: String,
    descricao: Option[String]
) derives JsonCodec

case class TransacaoResponse(
    limite: Int,
    saldo: Int
) derives JsonCodec

case class ExtratoResponse(
    saldo: Saldo,
    ultimas_transacoes: List[Transacao]
) derives JsonCodec

case class Saldo(
    total: Int,
    data_extrato: Instant,
    limite: Int
) derives JsonCodec

case class Transacao(
    valor: Int,
    tipo: String,
    descricao: String,
    realizada_em: Instant
) derives JsonCodec

enum Result:
    case Processed(balance: Int, limit: Int)
    case Denied

case class Statement(
    account: Int,
    balance: Int,
    limit: Int,
    transactions: Array[Transaction],
    timestamp: Long = System.currentTimeMillis()
)

case class Transaction(
    timestamp: Long,
    amount: Int,
    desc: Array[Char]
)
