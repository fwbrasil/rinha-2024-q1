package rinha

import java.time.Instant
import kyo.*
import sttp.model.StatusCode

trait Handler:

    def transaction(
        account: Int,
        request: TransacaoRequest
    ): TransacaoResponse < (Aborts[StatusCode] & Fibers)

    def statement(
        account: Int
    ): ExtratoResponse < (Aborts[StatusCode] & IOs)

end Handler

object Handler:

    def init = zip(Envs[Ledger].get, Envs[Store].get).map(Live(_, _))

    class Live(ledger: Ledger, store: Store) extends Handler:

        val notFound            = Aborts(StatusCode.NotFound)
        val unprocessableEntity = Aborts(StatusCode.UnprocessableEntity)

        def transaction(account: Int, request: TransacaoRequest) = defer {
            import request.*
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

            val desc   = descricao.get
            val amount = if tipo == "c" then valor else -valor

            // perform transaction
            val result =
                await(ledger.transaction(account, amount, desc))

            result match
                case Result.Denied =>
                    // return failure
                    await(unprocessableEntity)
                case Result.Processed(balance, limit) =>
                    // async store transaction and return result
                    await(store.transaction(limit, balance, account, amount, desc))
                    TransacaoResponse(limit, balance)
            end match
        }

        def statement(account: Int) = defer {
            // validations
            await {
                if account < 0 || account > 5 then notFound
                else ()
            }

            // get statement
            val result = await(ledger.statement(account))

            // generate response
            val transactions =
                result.transactions.toList
                    .filter(_ != null)
                    .reverse
                    .map { t =>
                        Transacao(
                            t.amount.abs,
                            if t.amount < 0 then "d" else "c",
                            t.desc.takeWhile(_ != 0).mkString,
                            Instant.ofEpochMilli(t.timestamp)
                        )
                    }

            ExtratoResponse(
                Saldo(
                    result.balance,
                    Instant.ofEpochMilli(System.currentTimeMillis()),
                    result.limit
                ),
                transactions
            )
        }
    end Live

end Handler
