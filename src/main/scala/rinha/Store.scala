package rinha

import kyo.*

trait Store:

    def transaction(
        limit: Int,
        balance: Int,
        account: Int,
        amount: Int,
        desc: String
    ): Unit < Fibers

end Store

object Store:

    case class Entry(limit: Int, balance: Int, account: Int, amount: Int, desc: String)

    private def consume(ch: Channel[Entry]): Unit < Fibers =
        ch.take.unit.andThen(consume(ch))

    val init = defer {
        val ch = await(Channels.init[Entry](1000))
        await(Fibers.init(consume(ch)))
        Live(ch)
    }

    class Live(ch: Channel[Entry]) extends Store:

        def transaction(
            limit: Int,
            balance: Int,
            account: Int,
            amount: Int,
            desc: String
        ): Unit < Fibers =
            ch.put(Entry(limit, balance, account, amount, desc))

    end Live

end Store
