package rinha

import Ledger.*
import java.lang.foreign.Arena
import java.lang.reflect.Field
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
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

end Ledger

object Ledger:

    def init(filePath: String) =
        IOs(Live(filePath))

    class Live(filePath: String) extends Ledger:
        val entrySize       = 1024
        val transactionSize = 32
        val fileSize        = entrySize * limits.size

        val address = FileChannel
            .open(
                Paths.get(filePath),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE
            )
            .map(FileChannel.MapMode.READ_WRITE, 0, fileSize, Arena.global())
            .address();

        def transaction(account: Int, amount: Int, desc: String) =
            IOs {
                val descChars = new Array[Char](10)
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
                        Result.Denied
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
                            Character.BYTES * 10
                        )
                        Result.Processed(newBalance, limit)
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
                while !unsafe.compareAndSwapInt(null, offset, 0, 1) do {} // busy wait
                try
                    balance = unsafe.getInt(offset + 4)
                    val tail = unsafe.getInt(offset + 8)
                    val head = Math.max(0, tail - 10)
                    for i <- head.until(tail) do
                        val toffset   = (offset + 12) + (i & 15) * transactionSize
                        val timestamp = unsafe.getLong(toffset)
                        val amount    = unsafe.getInt(toffset + 8)
                        val desc      = new Array[Char](10)
                        unsafe.copyMemory(
                            null,
                            toffset + 12,
                            desc,
                            Unsafe.ARRAY_CHAR_BASE_OFFSET,
                            Character.BYTES * 10
                        )
                        transactions(i - head) = Transaction(timestamp, amount, desc)
                    end for
                finally
                    unsafe.putOrderedInt(null, offset, 0)
                end try
                Statement(account, balance, limit, transactions)
            }
    end Live

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
