package rinha.db

import java.io.FileWriter
import kyo.*
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

trait Log:

    def transaction(
        balance: Int,
        account: Int,
        amount: Int,
        desc: String
    ): Unit < IOs

    def clear: Unit < IOs

end Log

object Log:

    case class Entry(balance: Int, account: Int, amount: Int, desc: String)

    val init: Log < (Envs[DB.Config] & IOs) =
        defer {
            val cfg  = await(Envs[DB.Config].get)
            val q    = await(Queues.initUnbounded[Entry](Access.Mpsc))
            val flag = await(Atomics.initBoolean(false))
            val log  = Live(cfg.workingDir + "/log.dat", q, flag)
            await(Fibers.init(log.flushLoop(cfg.flushInterval)))
            log
        }

    class Live(filePath: String, q: Queues.Unbounded[Entry], clearFlag: AtomicBoolean) extends Log:

        private var writer = new FileWriter(filePath, true)

        def transaction(
            balance: Int,
            account: Int,
            amount: Int,
            desc: String
        ): Unit < IOs =
            q.add(Entry(balance, account, amount, desc))

        def clear =
            clearFlag.set(true)

        private[Log] def flushLoop(interval: Duration): Unit < Fibers =
            defer {
                await(Fibers.sleep(interval))
                if await(clearFlag.cas(true, false)) then
                    await(clearFile)
                val entries = await(q.drain)
                await(append(entries))
                await(flushLoop(interval))
            }

        private def append(entries: Seq[Entry]) =
            IOs {
                if entries.nonEmpty then
                    println("flushing " + entries.size)
                    val str =
                        entries.map { e =>
                            s"${e.balance}|${e.account}|${e.amount}|${e.desc}"
                        }.mkString("\n")
                    writer.append(str + "\n")
                    writer.flush()
            }

        private def clearFile: Unit < IOs =
            IOs {
                writer = new FileWriter(filePath, false)
                writer.write("")
                writer.flush()
                writer = new FileWriter(filePath, true)
            }

    end Live

end Log
