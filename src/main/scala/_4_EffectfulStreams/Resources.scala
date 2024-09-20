package _4_EffectfulStreams

import java.io.{BufferedReader, FileReader}
import fs2._
import cats.effect._

object Resources extends IOApp.Simple {
  override def run: IO[Unit] = {
    val acquireReader = IO.blocking(new BufferedReader(new FileReader("salary_.csv")))
    val releaseReader = (reader: BufferedReader) => IO.println("Releasing") *> IO.blocking(reader.close())

    def readLines(reader: BufferedReader): Stream[IO, String] = {
      Stream
        .repeatEval(IO.blocking(reader.readLine()))
        .takeWhile(_ != null)
    }

    val readerResource: Resource[IO, BufferedReader] = Resource.make(acquireReader)(releaseReader)

    Stream
      //.bracket(acquireReader)(releaseReader)
      //.resource(readerResource)
      .fromAutoCloseable(acquireReader)
      .flatMap(readLines)
      .take(10)
      .printlns
      .compile
      .drain
  }
}