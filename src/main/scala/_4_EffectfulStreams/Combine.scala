package _4_EffectfulStreams

import fs2._
import cats.effect._

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Combine extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s = Stream.repeatEval(IO.println("Emitting...") *> IO(42))
    s.take(18).compile.toList.flatMap(IO.println)
    val s2 =
      for {
        x <- Stream.eval(IO.println("Producing 42") *> IO(42))
        y <- Stream.eval(IO.println("Producing 43") *> IO(x + 1))
      } yield y
    s2.compile.toList.flatMap(IO.println)

    val s3 = Stream(1, 2, 3).evalMap(i => IO.println(s" Element: $i").as(i))
    s3.compile.toList.flatMap(IO.println)

    val s4: Stream[IO, Int] = Stream(1, 2, 3).evalTap(IO.println)
    s4.compile.toList.flatMap(IO.println)

    val filterByFlippingCoin =
      Stream
        .range(start = 1, stopExclusive = 1000)
        .evalFilter(_ => IO(math.random() < 0.5))
    filterByFlippingCoin.compile.toList.flatMap(IO.println)

    val s5 = Stream.exec(IO.println("Start")) ++ Stream(1, 2, 3) ++ Stream(4, 5, 6) ++ Stream.exec(IO.println("Finish"))
    s5.compile.toList.flatMap(IO.println)
    val delayed = Stream.sleep_[IO](1.second) ++ Stream.eval(IO.println("I am awake!"))
    delayed.compile.drain

    // Exercise
    def evalEvery[A](d: FiniteDuration)(fa: IO[A]): Stream[IO, A] = Stream.repeatEval(fa).flatMap(_ => Stream.sleep_[IO](d))
    evalEvery(2.second)(IO(println("ass"))).compile.drain
  }
}