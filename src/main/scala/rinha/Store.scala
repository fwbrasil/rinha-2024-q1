package rinha

import com.mongodb.async.client.MongoClients
import com.mongodb.async.client.MongoCollection
import kyo.*
import org.bson.Document
import scala.jdk.CollectionConverters.*

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

    def init(mongoUrl: String): Store < IOs =
        defer {
            val col = await(connect(mongoUrl))
            val ch  = await(Channels.init[Entry](1000))
            await(Fibers.init(consume(ch, col)))
            Live(ch)
        }

    private class Live(ch: Channel[Entry]) extends Store:

        def transaction(
            limit: Int,
            balance: Int,
            account: Int,
            amount: Int,
            desc: String
        ): Unit < Fibers =
            ch.put(Entry(limit, balance, account, amount, desc))

    end Live

    private def connect(mongoUrl: String) =
        IOs(MongoClients.create(mongoUrl).getDatabase("ledger").getCollection("transaction"))

    private def consume(ch: Channel[Entry], col: MongoCollection[Document]): Unit < Fibers =
        ch.take.map(insert(_, col)).andThen(consume(ch, col))

    private def insert(entry: Entry, col: MongoCollection[Document]): Unit < Fibers =
        defer {
            val doc = Document(entry.productElementNames.zip(entry.productIterator).toMap.asJava)
            val promise = await(Fibers.initPromise[Unit])
            await(col.insertOne(doc, (res, ex) => complete(promise, res, ex)))
            await(promise.get)
        }

    private def complete(p: Promise[Unit], res: Void, ex: Throwable): Unit =
        IOs.run {
            if res != null then
                p.complete(()).unit
            else
                p.complete(IOs.fail(ex)).unit
        }

end Store
