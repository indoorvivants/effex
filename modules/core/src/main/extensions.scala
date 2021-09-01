package effex

import cats.effect.std.Dispatcher
import fs2.concurrent.Signal
import cats.effect.kernel.Ref
import cats.effect.IO
import cats.effect.std.Supervisor
import scalafx.beans.property.Property
import scalafx.application.Platform
import javafx.beans.property.{Property as JProperty}
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import scalafx.beans.property.ReadOnlyProperty

extension [T](d: JProperty[T])
  def $populate[A](rf: Ref[IO, A], f: T => A)(using disp: Dispatcher[IO]) =
    d.addListener(changeListener[T](raw => rf.set(f(raw))))

  def $update[A](rf: Ref[IO, A], f: A => T => A)(using disp: Dispatcher[IO]) =
    d.addListener(changeListener[T](raw => rf.update(a => f(a).apply(raw))))
  
  def $observe(
      rf: Signal[IO, T]
  )(using disp: Dispatcher[IO], superv: Supervisor[IO]): Unit =
    observer(rf.discrete, d.setValue(_))

  def $observe(
      rf: fs2.Stream[IO, T]
  )(using disp: Dispatcher[IO], superv: Supervisor[IO]): Unit =
    observer(rf, d.setValue(_))
end extension

extension [T, T1](d: ReadOnlyProperty[T, T1])
  def $populate[A](rf: Ref[IO, A], f: T1 => A)(using disp: Dispatcher[IO]) =
    d.addListener(changeListener[T1](raw => rf.set(f(raw))))

  def $update[A](rf: Ref[IO, A], f: A => T1 => A)(using disp: Dispatcher[IO]) =
    d.addListener(changeListener[T1](raw => rf.update(a => f(a).apply(raw))))
end extension

extension [T, T1](d: Property[T, T1])
  def $observe(
      rf: Signal[IO, T]
  )(using disp: Dispatcher[IO], superv: Supervisor[IO]): Unit =
    observer(rf.discrete, d.update(_))

  def $observe(
      rf: fs2.Stream[IO, T]
  )(using disp: Dispatcher[IO], superv: Supervisor[IO]): Unit =
    observer(rf, d.update(_))

  def $populate[A](rf: Ref[IO, A], f: T1 => A)(using
      disp: Dispatcher[IO]
  ): Unit =
    d.addListener(changeListener[T1](raw => rf.set(f(raw))))

  def $update[A](rf: Ref[IO, A], f: A => T1 => A)(using
      disp: Dispatcher[IO]
  ): Unit =
    d.addListener(changeListener[T1](raw => rf.update(a => f(a).apply(raw))))
end extension

extension [T](i: IO[T])
  def dispatchAsync(using d: Dispatcher[IO]): Unit =
    d.unsafeRunAndForget(i)

  def dispatchBackground(using d: Dispatcher[IO], s: Supervisor[IO]): Unit =
    s.supervise(i).dispatchAsync

private[effex] def changeListener[Raw](set: Raw => IO[Unit])(using
    disp: Dispatcher[IO]
) =
  new ChangeListener[Raw]:
    override def changed(obs: ObservableValue[? <: Raw], oldV: Raw, newV: Raw) =
      disp.unsafeRunAndForget(set(newV))

private[effex] def observer[T](stream: fs2.Stream[IO, T], act: T => Unit)(using disp: Dispatcher[IO], superv: Supervisor[IO]) =
    disp.unsafeRunAndForget {
      superv.supervise {
        stream
          .evalMap { st =>
            IO(Platform.runLater(() => act(st)))
          }
          .compile
          .drain
      }
    }

