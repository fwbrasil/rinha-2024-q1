package rinha.db

import kyo.*
import org.scalatest.compatible.Assertion
import org.scalatest.freespec.AnyFreeSpec
import scala.concurrent.duration.*

class IndexTest extends AnyFreeSpec:

    def run[T: Flat](f: (Int, Index) => Assertion < IOs): Unit =
        val idx =
            Envs[DB.Config].run(DB.Config(".", 1.second)) {
                Index.init.map(idx => idx.clear.andThen(idx))
            }
        for account <- 0 to 5 do
            s"account $account" in {
                IOs.run(idx.map(f(account, _)))
            }
        end for
    end run

    "no transaction" - run { (account, idx) =>
        defer {
            val s = await(idx.statement(account))
            assert(s.balance.total == 0)
            assert(s.lastTransactions.isEmpty)
        }
    }

    "single transaction storage and retrieval" - run { (account, idx) =>
        defer {
            await(idx.transaction(account, 100, "testSingl"))
            val s = await(idx.statement(account))
            assert(s.balance.total == 100)
            val t = s.lastTransactions.head
            assert(t.description.getOrElse("") == "testSingl")
        }
    }

    "multiple transactions handling" - run { (account, idx) =>
        defer {
            await(idx.transaction(account, 100, "txn1Test"))
            await(idx.transaction(account, -50, "txn2Test"))
            await(idx.transaction(account, 150, "txn3Test"))

            val s = await(idx.statement(account))
            assert(s.balance.total == 200)
            assert(s.lastTransactions.exists(t => t.description.getOrElse("") == "txn1Test"))
            assert(s.lastTransactions.exists(t => t.description.getOrElse("") == "txn2Test"))
            assert(s.lastTransactions.exists(t => t.description.getOrElse("") == "txn3Test"))
        }
    }

    "transaction overwriting with maximum history" - run { (account, idx) =>
        defer {
            await(Seqs.traverse(1 to 12)(i => idx.transaction(account, i * 10, s"tx${i}Hist")))

            val s = await(idx.statement(account))
            assert(!s.lastTransactions.exists(t => t.description.getOrElse("") == "tx1Hist"))
            assert(s.lastTransactions.exists(t => t.description.getOrElse("") == "tx12Hist"))
        }
    }

    "older transactions should be correctly overwritten, maintaining order" - {
        run { (account, idx) =>
            defer {
                await(Seqs.traverse(1 to 15)(i => idx.transaction(account, i * 10, s"tx$i")))
                val s = await(idx.statement(account))
                assert(s.lastTransactions.size == 10)
                assert(s.lastTransactions.last.description.getOrElse("") == "tx6")
                assert(s.lastTransactions.head.description.getOrElse("") == "tx15")
            }
        }
    }

end IndexTest
