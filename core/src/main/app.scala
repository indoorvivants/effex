package effex

import cats.effect.*
import scalafx.application.JFXApp
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import cats.effect.std.Supervisor
import cats.effect.std.Dispatcher
import cats.syntax.all.given

abstract class JfxIOApp extends IOApp:
  type Inject = (Dispatcher[IO], Supervisor[IO]) ?=> PrimaryStage

  def stage(args: List[String]): Resource[IO, Inject]

  def run(args: List[String]): IO[ExitCode] =
    (Dispatcher[IO], Supervisor[IO], stage(args)).parTupled.use {
      case (disp, spv, stg) =>
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
