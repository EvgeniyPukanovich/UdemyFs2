package _7_Communication

import fs2._
import cats.effect._
import fs2.concurrent.Topic

import scala.concurrent.duration.DurationInt
import scala.util.Random

object Topics extends IOApp.Simple {

  override def run: IO[Unit] = {
    Stream.eval(Topic[IO, Int]).flatMap { topic =>
      val p = Stream.iterate(1)(_ + 1).covary[IO].through(topic.publish).drain
      val c1 = topic.subscribe(10).evalMap(i => IO.println(s"Read $i from c1")).drain
      val c2 = topic.subscribe(10).evalMap(i => IO.println(s"Read $i from c2")).metered(200.millis).drain
      Stream(p, c1, c2).parJoinUnbounded
    }.interruptAfter(3.seconds).compile.drain

    case class CarPosition(carId: Long, lat: Double, lng: Double)

    def createCar(carId: Long, topic: Topic[IO, CarPosition]): Stream[IO, Nothing] = {
      Stream
        .repeatEval(IO(CarPosition(carId, Random.between(-90.00, 90.0), Random.between(-180.0, 180.0))))
        .metered(1.second)
        .through(topic.publish)
        .drain
    }

    def createGoogleMapUpdater(topic: Topic[IO, CarPosition]): Stream[IO, Nothing] = {
      topic
        .subscribe(10)
        .evalMap(pos => IO.println(f"Drawing position (${pos.lat}%.2f, ${pos.lng}%.2f) for car ${pos.carId} in map..."))
        .drain
    }

    // Exercise
    def createDriverNotifier(
                              topic: Topic[IO, CarPosition],
                              shouldNotify: CarPosition => Boolean,
                              notify: CarPosition => IO[Unit]
                            ): Stream[IO, Nothing] = {
      topic.subscribe(10).filter(shouldNotify).evalMap(notify).drain
    }

    Stream.eval(Topic[IO, CarPosition]).flatMap { topic =>
      val cars = Stream.range(1, 10).map(id => createCar(id, topic))

      val updater = createGoogleMapUpdater(topic)
      val notifier = createDriverNotifier(topic, _.carId >= 8, pos => IO.println(s"Notifying for car ${pos.carId}: (${pos.lng}, ${pos.lng})"))

      (cars ++ Stream(updater, notifier)).parJoinUnbounded
    }.interruptAfter(5.seconds).compile.drain

  }
}
