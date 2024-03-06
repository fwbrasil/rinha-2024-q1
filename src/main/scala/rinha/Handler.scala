package rinha

import kyo.*
import sttp.model.StatusCode

trait Handler:

    def transaction(
        account: Int,
        request: Transaction
    ): Processed < (Aborts[StatusCode] & Fibers)

    def statement(
        account: Int
    ): Statement < (Aborts[StatusCode] & IOs)

end Handler

object Handler:

    def init: Handler < (Envs[Ledger] & Envs[Store]) =
        zip(Envs[Ledger].get, Envs[Store].get).map(Live(_, _))

    private class Live(ledger: Ledger, store: Store) extends Handler:

        val notFound            = Aborts(StatusCode.NotFound)
        val unprocessableEntity = Aborts(StatusCode.UnprocessableEntity)

        def transaction(account: Int, request: Transaction) =
            defer {
                import request.*
                // validations
                await {
                    if account < 0 || account > 5 then notFound
                    else if description.isEmpty || description.exists(d =>
                            d.size > 10 || d.isEmpty()
                        )
                    then unprocessableEntity
                    else if kind != "c" && kind != "d" then unprocessableEntity
                    else ()
                }

                val desc  = description.get
                val value = if kind == "c" then amount else -amount

                // perform transaction
                val result =
                    await(ledger.transaction(account, value, desc))

                result match
                    case Denied =>
                        // return failure
                        await(unprocessableEntity)
                    case res @ Processed(balance, limit) =>
                        // async store transaction
                        // await(store.transaction(limit, balance, account, value, desc))
                        res
                end match
            }

        def statement(account: Int) =
            defer {
                // validations
                await {
                    if account < 0 || account > 5 then notFound
                    else ()
                }

                // get statement
                await(ledger.statement(account))
            }
    end Live

end Handler
