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

object readOnly extends JfxIOApp:
  case class WindowSize(width: Int, height: Int)
  def stage(args: List[String]) =
    val default = WindowSize(300, 450)
    Resource.eval(SignallingRef.of[IO, WindowSize](default)).map { state =>
      new JFXApp3.PrimaryStage:
        title.value = "Hello"
        width = default.width
        height = default.height

        width.$update(state, w => i => w.copy(width = i.intValue))
        height.$update(state, w => i => w.copy(height = i.intValue))

        scene = new Scene:
          root = new VBox:
            padding = Insets(15, 15, 15, 15)
            children = Seq(
              new Label:
                text.$observe(state.map(_.toString))
            )
    }
  end stage
end readOnly
