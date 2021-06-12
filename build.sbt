// VERSIONS

val Versions = new {
  val Scala     = "3.0.0"
  val ScalaNext = "3.0.1-RC1"
  val AllScala  = Seq(Scala, ScalaNext)

  val scalaFX    = "16.0.0-R24"
  val javaFX     = "16"
  val catsEffect = "3.1.1"
  val fs2        = "3.0.4"

  val http4s = "0.23.0-RC1"
}

// MODULES

lazy val root = project.aggregate(core, demo)

lazy val core =
  project
    .in(file("core"))
    .settings(
      libraryDependencies += "org.scalafx"   %% "scalafx"     % Versions.scalaFX,
      libraryDependencies += "org.typelevel" %% "cats-effect" % Versions.catsEffect,
      libraryDependencies += "co.fs2"        %% "fs2-core"    % Versions.fs2,
      libraryDependencies ++= javaFXDependencies(
        Seq("base", "controls"),
        provided = false
      )
    )
    .settings(simpleLayout ++ commons)

lazy val demo =
  project
    .in(file("demo"))
    .dependsOn(core)
    .settings(
      libraryDependencies ++= javaFXDependencies(
        Seq("base", "controls", "graphics", "web")
      ),
      run / fork := true,
      libraryDependencies ++= Seq(
        "org.http4s" %% "http4s-ember-server" % Versions.http4s,
        "org.http4s" %% "http4s-dsl"          % Versions.http4s
      ),
      publish / skip := true
    )
    .settings(simpleLayout ++ commons)

// HELPERS
def osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => "mac"
  case n if n.startsWith("Windows") => "win"
  case _                            => throw new Exception("Unknown platform!")
}

def javaFXDependencies(modules: Seq[String], provided: Boolean = false) =
  modules.map { m =>
    if (provided)
      "org.openjfx" % s"javafx-$m" % Versions.javaFX % "provided" classifier (osName)
    else
      "org.openjfx" % s"javafx-$m" % Versions.javaFX classifier osName
  }

// SETTINGS

lazy val simpleLayout = Seq[Setting[_]](
  Compile / unmanagedSourceDirectories +=
    baseDirectory.value / "src" / "main",
  Test / unmanagedSourceDirectories +=
    baseDirectory.value / "src" / "test"
)

lazy val commons = Seq(
  scalacOptions ++= Seq("-source:future", "-language:adhocExtensions"),
  scalaVersion := Versions.Scala,
  crossScalaVersions := Versions.AllScala
)
