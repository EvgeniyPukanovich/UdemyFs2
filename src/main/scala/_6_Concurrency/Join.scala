package _6_Concurrency

import fs2._
import cats.effect._
import cats.effect.std.Queue

import scala.concurrent.duration._

object Join extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s1Finite = Stream(1, 2, 3).covary[IO].metered(100.millis)
    val s2Finite = Stream(4, 5, 6).covary[IO].metered(50.millis)
    val jFinite = Stream(s1Finite, s2Finite).parJoinUnbounded //== s1Finite.merge(s2Finite)
    jFinite.printlns.compile.drain

    val s3Infinite = Stream.iterate(start = 3000000)(_ + 1).covary[IO].metered(50.millis)
    val s4Infinite = Stream.iterate(start = 4000000)(_ + 1).covary[IO].metered(50.millis)
    val jAll = Stream(s1Finite, s2Finite, s3Infinite, s4Infinite).parJoinUnbounded
    jAll.printlns.interruptAfter(3.seconds).compile.drain

    val s1Failing = Stream(1, 2, 3).covary[IO].metered(100.millis) ++ Stream.raiseError[IO](new Exception("s1 failed"))
    val jFailingS1 = Stream(s1Failing, s2Finite, s3Infinite, s4Infinite).parJoinUnbounded
    jFailingS1.printlns.interruptAfter(3.seconds).compile.drain

    val jBounded = Stream(s1Finite, s2Finite, s3Infinite).parJoin(maxOpen = 2)
    jBounded.printlns.interruptAfter(3.seconds).compile.drain

    val s1Infinite = Stream.iterate(start = 1000000)(_ + 1).covary[IO].metered(50.millis)
    val jBounded2 = Stream(s1Infinite, s3Infinite, s4Infinite).parJoin(maxOpen = 2)
    jBounded2.printlns.interruptAfter(3.seconds).compile.drain

    // Exercise
    def producer(id: Int, queue: Queue[IO, Int]): Stream[IO, Nothing] = {
      Stream.repeatEval(IO.println(s"Producing for $id") >> queue.offer(id)).drain
    }

    def consumer(id: Int, queue: Queue[IO, Int]): Stream[IO, Nothing] = {
      Stream.repeatEval(queue.take).map(i => s"Consuming message $i from consumer $id").printlns
    }
    // Create a stream that emits the queue
    // Use that queue to create 5 producers and 10 consumers with sequential ids
    // Run the producers and the consumers in parallel
    // Finish after 5 seconds

    Stream.eval(Queue.unbounded[IO, Int]).flatMap{queue =>
      val producers = Stream.range(0, 5).covary[IO].map(id => producer(id, queue))
      val consumers = Stream.range(10, 20).covary[IO].map(id => consumer(id, queue))
      (producers ++ consumers).parJoinUnbounded
    }.interruptAfter(5.seconds).compile.drain
  }
}