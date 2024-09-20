package _4_EffectfulStreams

import fs2._
import cats.effect._

object Create extends IOApp.Simple {
  override def run: IO[Unit] = {
    val s: Stream[IO, Unit] = Stream.eval(IO.println("my first effectful stream!"))

    val s2: Stream[IO, Nothing] = Stream.exec(IO.println("my second effectful stream!"))

    val fromPure: Stream[IO, Int] = Stream(1, 2, 3).covary[IO]

    val natsEval = Stream.iterateEval(start = 1)(a => IO.println(s"Producing ${a + 1}") *> IO(a + 1))

    val alphabet = Stream.unfoldEval('a') { c =>
      if (c == 'z' + 1) IO.println("Finishing...") *> IO(None)
      else IO.println(s" Producing $c") *> IO(Some(c, (c + 1).toChar))
    }

    // Exercise
    val data = List.range(1, 22)
    val pageSize = 20

    def fetchPage(pageNumber: Int): IO[List[Int]] = {
      val start = pageNumber * pageSize
      val end = start + pageSize
      IO.println(s"Fetching page $pageNumber").as(data.slice(start, end))
    }

    // Suggestion: use unfoldEval flatten
    def fetchAll(): Stream[IO, Int] = {
      val numberOfPages = math.ceil(data.size.toDouble / pageSize).toInt
      Stream.range(0, numberOfPages).covary[IO].flatMap { pageNumber =>
        Stream.eval(fetchPage(pageNumber)).flatMap(Stream.emits)
      }
    }

    //    s.compile.toList.flatMap(IO.println) >>
    //      s2.compile.drain >>
    //      fromPure.compile.toList.flatMap(IO.println) >>
    //      natsEval.take(10).compile.toList.flatMap(IO.println) >>
    //      alphabet.compile.toList.flatMap(IO.println)
    fetchAll().compile.toList.flatMap(IO.println)
  }
}