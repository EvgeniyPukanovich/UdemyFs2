import fs2._

val s1 = Stream(1, 2, 3)
val s2 = Stream(4, 5, 6)
val nats = Stream.iterate(start = 1)(_ + 1)
(s1 ++ s2).toList
(s1 ++ nats).take(18).toList
(nats ++ s1).take(18).toList

val doubled = s1.map(_ * 2)
doubled.toList
val evens = nats.map(_ * 2)
evens.take(18).toList

s1.flatMap(i => Stream(i, i + 1)).toList
nats.flatMap(i => Stream(i, i + 1)).take(18).toList
s2.flatMap(i => nats.drop(i)).take(18).toList

val odds = nats.filter(_ % 2 == 1)
odds.take(5).toList
val x = for {
  n <- nats
  n1 <- s1
  n2 <- s2
} yield n + n1 + n2
x.take(10).toList

s1.zip(s2).toList
nats.zip(evens).take(18).toList
nats.zip(s1).toList
s1.zip(nats).toList
Stream.constant(2).zipWith(nats)(_ * _).take(18).toList

val length = s1.fold(0) { case (res, _) => res + 1 }.toList
val sum = s1.fold(0) { case (res, i) => res + i }.toList

// Exercise #1
// Create the stream of all odd numbers using nats and map
nats.map(n => n * 2 - 1).take(10).toList

// Exercise #2
// Implement a repeat method which takes a Stream and repeats
// all its elements indefinitely
def repeat[A](s: Stream[Pure, A]): Stream[Pure, A] =
  Stream.constant(0).flatMap(_ => s)

def repeat1[A](s: Stream[Pure, A]): Stream[Pure, A] =
  s ++ repeat1(s)

repeat1(Stream(1, 2, 3)).take(20).toList

// Exercise #3
// unNone (Stream(Some (1), None, Some(2)))) == Stream (1, 2)
// Hint: Stream.fromOption
def unNone[A](s: Stream[Pure, Option[A]]): Stream[Pure, A] =
  s.collect { case Some(value) => value }

def unNone1[A](s: Stream[Pure, Option[A]]): Stream[Pure, A] =
  s.flatMap(opt => Stream.fromOption(opt))

unNone(Stream(Some(1), None, Some(2))).toList
unNone1(Stream(Some(1), None, Some(2))).toList