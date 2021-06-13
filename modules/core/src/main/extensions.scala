package effex

import cats.effect.std.Dispatcher
import fs2.concurrent.Signal
import cats.effect.kernel.Ref
import cats.effect.IO
import cats.effect.std.Supervisor
import scalafx.beans.property.Property
import scalafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import scalafx.beans.property.ReadOnlyProperty

extension [T, T1](d: ReadOnlyProperty[T, T1])
  def $populate[A](rf: Ref[IO, A], f: T1 => A)(using disp: Dispatcher[IO]) =
    d.addListener {
      new ChangeListener[T1]:
        override def changed(
            obs: ObservableValue[? <: T1],
            oldV: T1,
            newV: T1
        ): Unit =
          disp.unsafeRunAndForget {
            rf.set(f(newV))
          }
    }

  def $update[A](rf: Ref[IO, A], f: A => T1 => A)(using disp: Dispatcher[IO]) =
    d.addListener {
      new ChangeListener[T1]:
        override def changed(
            obs: ObservableValue[? <: T1],
            oldV: T1,
            newV: T1
        ): Unit =
          disp.unsafeRunAndForget {
            rf.update(a => f(a).apply(newV))
          }
    }
end extension

extension [T, T1](d: Property[T, T1])
  def $observe(
      rf: Signal[IO, T]
  )(using disp: Dispatcher[IO], superv: Supervisor[IO]): Unit =
    $observe(rf.discrete)

  def $observe(
      rf: fs2.Stream[IO, T]
  )(using disp: Dispatcher[IO], superv: Supervisor[IO]): Unit =
    disp.unsafeRunAndForget {
      superv.supervise {
        rf
          .evalMap { st =>
            IO(Platform.runLater(() => d.update(st)))
          }
          .compile
          .drain
      }
    }

  def $populate[A](rf: Ref[IO, A], f: T1 => A)(using
      disp: Dispatcher[IO]
  ): Unit =
    d.addListener {
      new ChangeListener[T1]:
        override def changed(
            obs: ObservableValue[? <: T1],
            oldV: T1,
            newV: T1
        ): Unit =
          disp.unsafeRunAndForget {
            rf.set(f(newV))
          }
    }
  def $update[A](rf: Ref[IO, A], f: A => T1 => A)(using
      disp: Dispatcher[IO]
  ): Unit =
    d.addListener {
      new ChangeListener[T1]:
        override def changed(
            obs: ObservableValue[? <: T1],
            oldV: T1,
            newV: T1
        ): Unit =
          disp.unsafeRunAndForget {
            rf.update(a => f(a)(newV))
          }
    }
end extension

extension [T](i: IO[T])
  def dispatchAsync(using d: Dispatcher[IO]): Unit =
    d.unsafeRunAndForget(i)

  def dispatchBackground(using d: Dispatcher[IO], s: Supervisor[IO]): Unit =
    s.supervise(i).dispatchAsync
