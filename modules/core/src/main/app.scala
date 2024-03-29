package effex

import cats.effect.*
import scalafx.application.JFXApp
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import cats.effect.std.Supervisor
import cats.effect.std.Dispatcher
import cats.syntax.all.given

case class EffexEnv(disp: Dispatcher[IO], superv: Supervisor[IO])
object EffexEnv:
  def create: Resource[IO, EffexEnv] =
    (Dispatcher[IO], Supervisor[IO]).parTupled
      .map { case (d, s) => EffexEnv(d, s) }

abstract class JfxIOApp extends IOApp:
  type Inject[T] = EffexEnv ?=> Resource[IO, T]

  def stage(
      args: List[String]
  ): EffexEnv ?=> Resource[IO, EffexEnv ?=> PrimaryStage]

  def run(args: List[String]): IO[ExitCode] =
    EffexEnv.create
      .flatMap { env =>
        stage(args)(using env).map { stgF =>
          (
            env,
            env ?=> stgF(using env)
          )
        }
      }
      .use { case (env, stg) =>
        val jfxApp = IO(
          new JFXApp3:
            override def start(): Unit =
              this.stage = stg(using env)
        )

        jfxApp
          .flatMap { app =>
            IO.interruptible(false)(app.main(args.toArray))
          }
          .as(ExitCode.Success)
      }
end JfxIOApp

object JfxIOApp:
  trait Simple extends IOApp.Simple:
    def stage: EffexEnv ?=> IO[EffexEnv ?=> PrimaryStage]
    override def run: IO[Unit] =
      EffexEnv.create.use { env =>
        stage(using env).flatMap { stg =>
          val jfxApp = IO(
            new JFXApp3:
              override def start(): Unit =
                this.stage = stg(using env)
          )

          jfxApp
            .flatMap { app =>
              IO.interruptible(false)(app.main(Array.empty[String]))
            }
        }
      }
  end Simple
end JfxIOApp
