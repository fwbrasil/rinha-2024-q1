package rinha

import Ledger.*
import java.lang.foreign.Arena.global
import java.lang.reflect.Field
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode.READ_WRITE
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Instant
import kyo.*
import sun.misc.Unsafe

trait Ledger:

    def transaction(
        account: Int,
        amount: Int,
        desc: String
    ): Result < IOs

    def statement(
        account: Int
    ): Statement < IOs

    def clear: Unit < IOs

end Ledger

object Ledger:

    def init(filePath: String): Ledger < (IOs with Resources) =
        defer {
            val file = await(open(filePath))
            await(Resources.ensure(IOs(file.close())))
            await(IOs(Live(file)))
        }

    class Live(file: FileChannel) extends Ledger:
        val descSize        = 10
        val entrySize       = 1024
        val transactionSize = 32
        val fileSize        = entrySize * limits.size

        val address =
            file.map(READ_WRITE, 0, fileSize, global()).address();

        def transaction(account: Int, amount: Int, desc: String) =
            IOs {
                val descChars = new Array[Char](descSize)
                for i <- 0 until desc.size do
                    descChars(i) = desc.charAt(i)

                val limit     = limits(account)
                val offset    = address + (account * entrySize)
                val timestamp = System.currentTimeMillis()
                while !unsafe.compareAndSwapInt(null, offset, 0, 1) do {} // busy wait
                try
                    val balance    = unsafe.getInt(offset + 4)
                    val newBalance = balance + amount
                    if newBalance < -limit then
                        Denied
                    else
                        unsafe.putInt(offset + 4, newBalance)
                        val tail = unsafe.getInt(offset + 8)
                        unsafe.putInt(offset + 8, tail + 1)
                        val toffset = (offset + 12) + (tail & 15) * transactionSize
                        unsafe.putLong(toffset, timestamp)
                        unsafe.putInt(toffset + 8, amount)
                        unsafe.copyMemory(
                            descChars,
                            Unsafe.ARRAY_CHAR_BASE_OFFSET,
                            null,
                            toffset + 12,
                            Character.BYTES * descSize
                        )
                        Processed(limit, newBalance)
                    end if
                finally
                    unsafe.putOrderedInt(null, offset, 0)
                end try
            }

        def statement(account: Int) =
            IOs {
                val limit                            = limits(account)
                val offset                           = address + (account * entrySize)
                var balance                          = 0
                val transactions: Array[Transaction] = new Array[Transaction](10)
                val desc                             = new Array[Char](descSize)
                while !unsafe.compareAndSwapInt(null, offset, 0, 1) do {} // busy wait
                try
                    balance = unsafe.getInt(offset + 4)
                    val tail = unsafe.getInt(offset + 8)
                    val head = Math.max(0, tail - 10)
                    for i <- head.until(tail) do
                        val toffset   = (offset + 12) + (i & 15) * transactionSize
                        val timestamp = unsafe.getLong(toffset)
                        val amount    = unsafe.getInt(toffset + 8)
                        unsafe.copyMemory(
                            null,
                            toffset + 12,
                            desc,
                            Unsafe.ARRAY_CHAR_BASE_OFFSET,
                            Character.BYTES * 10
                        )
                        transactions(i - head) = Transaction(
                            amount.abs,
                            if amount < 0 then "d" else "c",
                            Some(desc.takeWhile(_ != 0).mkString),
                            Some(Instant.ofEpochMilli(timestamp))
                        )
                    end for
                finally
                    unsafe.putOrderedInt(null, offset, 0)
                end try
                Statement(
                    Balance(balance, Instant.now(), limit),
                    transactions.takeWhile(_ != null).reverse
                )
            }

        def clear =
            IOs {
                unsafe.setMemory(address, fileSize, 0)
            }
    end Live

    private def open(filePath: String) =
        IOs {
            FileChannel
                .open(
                    Paths.get(filePath),
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE
                )
        }

    private val unsafe: Unsafe =
        val f: Field = classOf[Unsafe].getDeclaredField("theUnsafe")
        f.setAccessible(true)
        f.get(null).asInstanceOf[Unsafe]
    end unsafe

    private val limits =
        List(
            Integer.MAX_VALUE, // warmup account
            100000,
            80000,
            1000000,
            10000000,
            500000
        ).toArray

end Ledger
