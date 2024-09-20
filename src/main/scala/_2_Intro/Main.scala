package _2_Intro

import cats.effect.{IO, IOApp}
import fs2.io.file.{Files, Path}
import fs2.text

import java.nio.charset.{Charset, StandardCharsets}

object Main extends IOApp.Simple {
  case class Salary(number: Int, position: String, rate: Double)

  def convert(line: String) = {
    val splitted = line.split(";")
    for {
      number <- splitted.headOption.flatMap(_.toIntOption)
      position <- splitted.lift(1).flatMap(Option(_))
      salary <- splitted.lift(2).flatMap(_.toDoubleOption)
    } yield Salary(number, position, salary)
  }

  def read(path: String, filter: Salary => Boolean, limit: Int) = {
    val target = Path(path)
    Files[IO].readAll(target)
      .through(text.decodeWithCharset(Charset.forName("windows-1251")))
      .through(text.lines)
      .parEvalMapUnbounded(str => IO(convert(str)))
      .unNone
      .filter(filter)
      .take(limit)
      .compile
      .toList
  }

  override def run: IO[Unit] = read("salary_.csv", _.rate > 0.5, 10)
    .flatMap(lst => IO.println(lst))
}
