package _5_TransformingStreams

import fs2._
import cats.effect._
import fs2.Stream.ToPull

object Pulls extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s = Stream(1, 2) ++ Stream(3) ++ Stream(4, 5)
    val outputPull: Pull[Pure, Int, Unit] = Pull.output1(1)
    IO.println(outputPull.stream.toList)
    val outputChunk = Pull.output(Chunk(1, 2, 3))
    IO.println(outputChunk.stream.toList)
    val donePull: Pull[Pure, Nothing, Unit] = Pull.done
    val purePull: Pull[Pure, Nothing, Int] = Pull.pure(5)
    val combined =
      for {
        _ <- Pull.output1(1)
        _ <- Pull.output(Chunk(2, 3, 4))
      } yield ()
    IO.println(combined.stream.toList)

    IO.println(combined.stream.chunks.toList)
    val toPull: ToPull[Pure, Int] = s.pull
    val echoPull: Pull[Pure, Int, Unit] = s.pull.echo
    val takePull: Pull[Pure, Int, Option[Stream[Pure, Int]]] = s.pull.take(3)
    val dropPull: Pull[Pure, Int, Option[Stream[Pure, Int]]] = s.pull.drop(3)

    // Exercise -> implement using pulls
    def skipLimit[A](skip: Int, limit: Int)(s: Stream[IO, A]): Stream[IO, A] = {
      val a = s.pull.drop(skip)
      val b = a.flatMap {
        case Some(value) => value.pull.take(limit)
        case None => Pull.done
      }
      b.flatMap(_ => Pull.done).stream
    }

    skipLimit(10, 10)(Stream.range(1, 188)).compile.toList.flatMap(IO.println)
    skipLimit(1, 15)(Stream.range(1, 5)).compile.toList.flatMap(IO.println)

    val unconsedRange: Pull[Pure, Nothing, Option[(Chunk[Int], Stream[Pure, Int])]] = s.pull.uncons

    def firstChunk[A]: Pipe[Pure, A, A] = s => {
      s.pull.uncons.flatMap {
        case Some((chunk, restOfStream)) => Pull.output(chunk)
        case None => Pull.done
      }.stream
    }

    IO.println(s.through(firstChunk).toList)

    def drop[A](n: Int): Pipe[Pure, A, A] = s => {

      def go(s: Stream[Pure, A], n: Int): Pull[Pure, A, Unit] = {
        s.pull.uncons.flatMap {
          case Some((chunk, restOfStream)) =>
            if (chunk.size < n) go(restOfStream, n - chunk.size)
            else Pull.output(chunk.drop(n)) >> restOfStream.pull.echo
          case None => Pull.done
        }
      }

      go(s, n).stream
    }

    IO.println(s.through(drop(2)).toList)

    //Exercise

    def filter[A](p: A => Boolean): Pipe[Pure, A, A] = s => {

      def go(s: Stream[Pure, A]): Pull[Pure, A, Unit] = {
        s.pull.uncons.flatMap {
          case Some((chunk, restOfStream)) => Pull.output(chunk.filter(p)) >> go(restOfStream)
          case None => Pull.done
        }
      }

      go(s).stream
    }

    IO.println(s.through(filter(_ % 2 == 0)).toList)

    def runningSum: Pipe[Pure, Int, Int] = s => {
      s.scanChunksOpt(0) { sumAcc =>
        Some { chunk =>
          val newSum = chunk.foldLeft(0)(_ + _) + sumAcc
          (newSum, Chunk.singleton(newSum))
        }
      }
    }

    IO.println(s.through(runningSum).toList)

    //Exercise
    def runningMax: Pipe[Pure, Int, Int] = s => {
      s.scanChunksOpt(Int.MinValue){currMax =>
        Some { chunk =>
          val maxInChunk = chunk.foldLeft(Int.MinValue)(_ max _)
          val newMax = currMax max maxInChunk
          (newMax, Chunk.singleton(newMax))
        }
      }
    }

    IO.println(s.through(runningMax).toList)
  }
}