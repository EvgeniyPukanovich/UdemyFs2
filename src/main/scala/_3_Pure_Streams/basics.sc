import cats.effect.IO
import fs2._

val s1: Stream[Pure, Int] = Stream(1, 2, 3)
val s2 = Stream.empty
val s3 = Stream.emit(10)
val s4 = Stream.emits(List(1, 2, 4))
val s5 = Stream.iterate(0)(_ + 1)
val s6 = Stream.unfold(0) { s =>
  if (s < 20) Some((s, s + 1)) else None
}
val s7 = Stream.range(1, 15)
val s8 = Stream.constant(42)

s1.toVector
s4.toList
s5.take(10).toList
s6.toList
s8.take(10).toList
val sIo = Stream(1).covary[IO].compile.toList

// Exercise #1
// Stream('a', 'b', 'c', ..., 'z')
// iterate take
def lettersIter: Stream[Pure, Char] = Stream.iterate('a')(char => (char + 1).toChar).takeThrough(_ != 'z')

// Exercise #2
def lettersUnfold: Stream[Pure, Char] = Stream.unfold('a') { char =>
  if (char <= 'z') Some((char, (char + 1).toChar)) else None
}

// Exercise #3
// use only unfold
def myIterate[A](initial: A)(next: A => A): Stream[Pure, A] = {
  Stream.unfold(initial)(x => Some((x, next(x))))
}

lettersIter.toList
lettersUnfold.toList
myIterate(0)(x => x + 1).take(10).toList