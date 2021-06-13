package effex

import cats.effect.*
import scalafx.application.JFXApp
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import cats.effect.std.Supervisor
import cats.effect.std.Dispatcher
import cats.syntax.all.given

abstract class JfxIOApp extends IOApp:
  type Inject[T] = (Dispatcher[IO], Supervisor[IO]) ?=> Resource[IO, T]

  def stage(args: List[String]): (
      Dispatcher[IO],
      Supervisor[IO]
  ) ?=> Resource[IO, (Dispatcher[IO], Supervisor[IO]) ?=> PrimaryStage]

  def run(args: List[String]): IO[ExitCode] =
    (Dispatcher[IO], Supervisor[IO]).parTupled
      .flatMap { case (disp, spv) =>
        stage(args)(using disp, spv).map { stgF =>
          (
            disp,
            spv,
            (d: Dispatcher[IO], s: Supervisor[IO]) ?=> stgF(using d, s)
          )
        }
      }
      .use { case (disp, spv, stg) =>
        val jfxApp = IO(
          new JFXApp3:
            override def start(): Unit =
              this.stage = stg(using disp, spv)
        )

        jfxApp
          .flatMap { app =>
            IO.interruptible(false)(app.main(args.toArray))
          }
          .as(ExitCode.Success)
      }
end JfxIOApp
