package _7_Communication

import cats.Order
import fs2._
import fs2.concurrent.Channel
import cats.effect._
import cats.implicits._

import scala.concurrent.duration._
import scala.util.Random

object Channels extends IOApp.Simple {

  override def run: IO[Unit] = {
    Stream.eval(Channel.bounded[IO, Int](1)).flatMap { channel =>
      val p = Stream.iterate(1)(_ + 1).covary[IO].evalMap(channel.send).drain
      val c = channel.stream.metered(200.millis).evalMap(i => IO.println(s"Read $i")).drain
      c.concurrently(p).interruptAfter(3.seconds)
    }.compile.drain

    sealed trait Measurement
    case class Temperature(value: Double) extends Measurement
    case class Humidity(value: Double) extends Measurement

    implicit val ordHum: Order[Humidity] = Order.by(_.value)
    implicit val ordTem: Order[Temperature] = Order.by(_.value)

    def createTemperatureSensor(alarm: Channel[IO, Measurement], threshold: Temperature): Stream[IO, Nothing] =
      Stream
        .repeatEval(IO(Temperature(Random.between(-40.0, 40.0))))
        .evalTap(t => IO.println(f"Current temperature: ${t.value}%.1f"))
        .evalMap(t => if (t > threshold) alarm.send(t) else IO.unit)
        .metered(300.millis)
        .drain

    // Exercise
    // Repeatedly generate random humidities between 0.0 and 100.0
    // Print every humidity as the current humidity
    // Check if the humidity goes above the threshold and send an alarm
    // Assume that we read each humidity every 100 milliseconds
    def createHumiditySensor(alarm: Channel[IO, Measurement], threshold: Humidity): Stream[IO, Nothing] =
      Stream
        .repeatEval(IO(Humidity(Random.between(0.0, 100.0))))
        .evalTap(t => IO.println(f"Current humidity: ${t.value}%.1f"))
        .evalMap(t => if (t > threshold) alarm.send(t) else IO.unit)
        .metered(100.millis)
        .drain

    // Read the values from the channel
    // Handle alarms by outputting something to console
    def createCooler(alarm: Channel[IO, Measurement]): Stream[IO, Nothing] =
      alarm.stream.evalMap {
        case Temperature(value) => IO.println(s"Temperature is too high: $value. Cooling")
        case Humidity(value) => IO.println(s"Humidity is too high: $value. Drying")
      }.drain

    val temperatureThreshold = Temperature(20)
    val humidityThreshold = Humidity(60)

    Stream.eval(Channel.unbounded[IO, Measurement]).flatMap { channel =>
        val temperatureSensor = createTemperatureSensor(channel, temperatureThreshold)
        val humiditySensor = createHumiditySensor(channel, humidityThreshold)
        val cooler = createCooler(channel)
        Stream(temperatureSensor, humiditySensor, cooler).parJoinUnbounded
      }.interruptAfter(3.seconds)
      .compile
      .drain
  }
}
