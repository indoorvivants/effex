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

object httpServer extends JfxIOApp:
  def stage(args: List[String]) =
    val host = Hostname.fromString("localhost").get
    val port = Port.fromString("9911").get

    Resource
      .eval(SignallingRef.of[IO, Int](0))
      .flatMap(state => server(state, host, port).map(state -> _))
      .map { case (state, running) =>
        new JFXApp3.PrimaryStage:
          title.value = "Hello"
          width = 500
          height = 350
          scene = new Scene:
            root = new VBox:
              padding = Insets(15, 15, 15, 15)
              children = Seq(
                new Label:
                  style = "-fx-font-size: 15px"
                  text = 
                    s"""
                      | The server is running on ${running.baseUri}
                      | To increment the counter: 
                      |   POST ${running.baseUri.withPath("/increment")}
                      | To decrement the counter: 
                      |   POST ${running.baseUri.withPath("/decrement")}
                      | To get the current counter: 
                      |   GET ${running.baseUri.withPath("/get")}
                      """.stripMargin
                      ,
                new Label:
                  text.observe(state.map("Current counter:" + _.toString))
                  style.observe(state.map { num =>

                    if num % 2 == 0 then List("-fx-text-fill: red")
                    else List("-fx-text-fill: darkgreen")
                  }.map("-fx-font-size: 15px;" ::_).map(_.mkString("\n"))
              )
            )
      }
  end stage

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
