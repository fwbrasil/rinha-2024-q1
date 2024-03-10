package rinha.db

import kyo.*
import rinha.*
import scala.concurrent.duration.Duration

trait DB:

    def transaction(
        account: Int,
        amount: Int,
        desc: String
    ): Result < IOs

    def statement(
        account: Int
    ): Statement < IOs

    def clear: Unit < IOs

end DB

object DB:

    case class Config(
        workingDir: String,
        flushInterval: Duration
    )

    val init: Live < (Envs[Config] & IOs) = defer {
        val index = await(Index.init)
        val log   = await(Log.init)
        Live(index, log)
    }

    class Live(index: Index, log: Log) extends DB:

        def transaction(account: Int, amount: Int, desc: String): Result < IOs =
            index.transaction(account, amount, desc).map {
                case Denied => Denied
                case result: Processed =>
                    log.transaction(result.balance, account, amount, desc).andThen(result)
            }

        def statement(account: Int): Statement < IOs =
            index.statement(account)

        def clear: Unit < IOs = defer {
            await(index.clear)
            await(log.clear)
        }
    end Live
end DB
