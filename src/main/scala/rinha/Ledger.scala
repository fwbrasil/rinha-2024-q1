package rinha

import Ledger.*
import java.util.concurrent.atomic.*
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import sun.misc.Unsafe
import java.lang.reflect.Field
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.lang.foreign.Arena
import scala.util.Random
import scala.util.control.NoStackTrace

trait Ledger:

  def isDirty(): Boolean

  def dump(): Array[Statement]

  def transaction(
      account: Int,
      amount: Int,
      desc: Array[Char]
  ): Result

  def statement(account: Int): Statement

end Ledger

object Ledger:

  private val unsafe: Unsafe =
    val f: Field = classOf[Unsafe].getDeclaredField("theUnsafe")
    f.setAccessible(true)
    f.get(null).asInstanceOf[Unsafe]

  private val limits =
    List(
      Integer.MAX_VALUE, // warmup account
      100000,
      80000,
      1000000,
      10000000,
      500000
    ).toArray

  enum Result {
    case Processed(balance: Int, limit: Int)
    case Denied
  }

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

  def offheap(file: String = "/app/data/ledger.dat"): Ledger =
    new Ledger:
      val entrySize = 1024
      val transactionSize = 32
      val fileSize = entrySize * limits.size
      val dirty = new AtomicBoolean

      var address = FileChannel
        .open(
          Paths.get(file),
          StandardOpenOption.READ,
          StandardOpenOption.WRITE,
          StandardOpenOption.CREATE
        )
        .map(FileChannel.MapMode.READ_WRITE, 0, fileSize, Arena.global())
        .address();

      def isDirty(): Boolean =
        dirty.getAndSet(false)

      def dump() =
        val arr = new Array[Statement](limits.size)
        for (i <- 0 until limits.size) {
          arr(i) = statement(i + 1)
        }
        arr

      def transaction(account: Int, amount: Int, desc: Array[Char]) =
        val limit = limits(account)
        val offset = address + (account * entrySize)
        val timestamp = System.currentTimeMillis()
        var newBalance = 0
        while (!unsafe.compareAndSwapInt(null, offset, 0, 1)) {} // busy wait
        try {
          val balance = unsafe.getInt(offset + 4)
          newBalance = balance + amount
          if (newBalance < -limit)
            return Result.Denied
          unsafe.putInt(offset + 4, newBalance)
          val tail = unsafe.getInt(offset + 8)
          unsafe.putInt(offset + 8, tail + 1)
          val toffset = (offset + 12) + (tail & 15) * transactionSize
          unsafe.putLong(toffset, timestamp)
          unsafe.putInt(toffset + 8, amount)
          unsafe.copyMemory(
            desc,
            Unsafe.ARRAY_CHAR_BASE_OFFSET,
            null,
            toffset + 12,
            Character.BYTES * 10
          )
          dirty.lazySet(true)
        } finally {
          unsafe.putOrderedInt(null, offset, 0)
        }
        Result.Processed(newBalance, limit)

      def statement(account: Int) =
        val limit = limits(account)
        val offset = address + (account * entrySize)
        var balance = 0
        val transactions: Array[Transaction] = new Array[Transaction](10)
        while (!unsafe.compareAndSwapInt(null, offset, 0, 1)) {} // busy wait
        try {
          balance = unsafe.getInt(offset + 4)
          val tail = unsafe.getInt(offset + 8)
          val head = Math.max(0, tail - 10)
          for (i <- head.until(tail)) {
            val toffset = (offset + 12) + (i & 15) * transactionSize
            val timestamp = unsafe.getLong(toffset)
            val amount = unsafe.getInt(toffset + 8)
            val desc = new Array[Char](10)
            unsafe.copyMemory(
              null,
              toffset + 12,
              desc,
              Unsafe.ARRAY_CHAR_BASE_OFFSET,
              Character.BYTES * 10
            )
            transactions(i - head) = Transaction(timestamp, amount, desc)
          }
        } finally {
          unsafe.putOrderedInt(null, offset, 0)
        }
        Statement(account, balance, limit, transactions)
    end new
end Ledger
