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

object state extends JfxIOApp.Simple:
  def stage =
    SignallingRef.of[IO, Int](0).map { state =>
      new JFXApp3.PrimaryStage:
        title.$observe(state.map(_.toString).map(_ + " and counting!"))
        width = 300
        height = 450
        scene = new Scene:
          root = new VBox:
            padding = Insets(15, 15, 15, 15)
            children = Seq(
              new Label(""):
                text.$observe(state.map(_.toString))
              ,
              new Button("Increase"):
                this.setOnAction { _ =>
                  state.update(_ + 1).dispatchAsync
                }
              ,
              new Button("Decrease"):
                this.setOnAction { _ =>
                  state.update(_ - 1).dispatchAsync
                }
            )
    }
end state
