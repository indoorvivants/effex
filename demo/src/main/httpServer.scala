package demo.effex

import effex.{*, given}

import scalafx.application.JFXApp3
import cats.effect.*
import scalafx.scene.Scene
import scalafx.scene.layout.VBox
import scalafx.scene.control.Label
import cats.effect.std.Dispatcher
import fs2.concurrent.SignallingRef
import scalafx.scene.control.Button
import scalafx.geometry.Insets
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.Hostname
import com.comcast.ip4s.Port
import fs2.concurrent.Signal
import cats.data.Chain
import cats.effect.std.Supervisor
import scalafx.scene.chart.LineChart
import scalafx.scene.chart.CategoryAxis
import java.time.OffsetDateTime
import scalafx.scene.chart.NumberAxis
import scalafx.collections.ObservableBuffer
import javafx.collections.ObservableList
import scalafx.scene.chart.XYChart
import java.time.DateTimeException
import java.time.format.DateTimeFormatter
import org.http4s.server.Server

object Tracker:
  def folding[T](max: Int, signal: Signal[IO, T])(using
      s: Supervisor[IO]
  ): IO[SignallingRef[IO, Vector[(OffsetDateTime, T)]]] =
    SignallingRef.of[IO, Vector[(OffsetDateTime, T)]](Vector.empty).flatMap {
      chainRef =>
        val constantUpdate = signal.discrete
          .evalMap(v => IO(OffsetDateTime.now).map(_ -> v))
          .evalMap { newValue =>
            chainRef.update { chain =>
              if chain.length == max then newValue +: chain.init
              else newValue +: chain
            }
          }

        s.supervise(constantUpdate.compile.drain).as(chainRef)
    }
end Tracker

object httpServer extends JfxIOApp:
  def stage(args: List[String]) =
    val host = Hostname.fromString("localhost").get
    val port = Port.fromString("9911").get

    Resource
      .eval(SignallingRef.of[IO, Int](0))
      .flatMap(state => server(state, host, port).map(state -> _))
      .evalMap { case (state, running) =>
        Tracker.folding(20, state).map { tracker =>
          new JFXApp3.PrimaryStage:
            width = 800
            height = 800
            scene = new Scene:
              root = new VBox:
                padding = Insets(15, 15, 15, 15)
                children = Seq(
                  infoLabel(running),
                  counterLabel(state),
                  trackingChart(tracker)
                )
        }
      }
  end stage

  def trackingChart(
      tracker: Signal[IO, Vector[(OffsetDateTime, Int)]]
  )(using Dispatcher[IO], Supervisor[IO]) =
    new LineChart(CategoryAxis("Time"), NumberAxis("Counter Value")):
      title = "History of changes"
      data.$observe(tracker.map(toSeries).map(_.delegate))

  def infoLabel(running: Server) =
    new Label:
      style = "-fx-font-size: 15px"
      text = s"""
          | The server is running on ${running.baseUri}
          | To increment the counter: 
          |   POST ${running.baseUri.withPath("/increment")}
          | To decrement the counter: 
          |   POST ${running.baseUri.withPath("/decrement")}
          | To get the current counter: 
          |   GET ${running.baseUri.withPath("/get")}
          """.stripMargin

  def counterLabel(
      state: Signal[IO, Int]
  )(using Dispatcher[IO], Supervisor[IO]) =
    new Label:
      text.$observe(state.map("Current counter:" + _.toString))

      style.$observe(
        state
          .map { num =>
            if num % 2 == 0 then List("-fx-text-fill: red")
            else List("-fx-text-fill: darkgreen")
          }
          .map("-fx-font-size: 15px;" :: _)
          .map(_.mkString("\n"))
      )

  def toSeries(buf: Vector[(OffsetDateTime, Int)]) =
    val dataPairs = buf.reverse.map { case (timestamp, value) =>
      XYChart.Data[String, Number](
        timestamp.withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        value
      )
    }

    val observableBuffer = ObservableBuffer.from(dataPairs)
    val series           = XYChart.Series.apply("Series 1", observableBuffer)

    ObservableBuffer(series)

  def server(state: Ref[IO, Int], host: Hostname, port: Port) =
    import org.http4s.dsl.io.*
    import org.http4s.implicits.*
    val app = HttpRoutes.of[IO] {
      case POST -> Root / "increment" =>
        state.update(_ + 1) *> Ok()
      case POST -> Root / "decrement" =>
        state.update(_ - 1) *> Ok()
      case GET -> Root / "get" =>
        state.get.flatMap(res => Ok(res.toString))
    }

    EmberServerBuilder
      .default[IO]
      .withHost(host)
      .withPort(port)
      .withHttpApp(app.orNotFound)
      .build

end httpServer
