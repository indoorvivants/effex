// VERSIONS

val Versions = new {
  val Scala    = "3.1.0-RC3"
  val AllScala = Seq(Scala)

  val scalaFX    = "16.0.0-R24"
  val javaFX     = "16"
  val catsEffect = "3.2.9"
  val fs2        = "3.1.6"

  val http4s = "0.23.5"
  val weaver = "0.7.7"
}

// MODULES

lazy val root = project.aggregate(core, demo)

lazy val core =
  project
    .in(file("modules/core"))
    .settings(
      libraryDependencies += "org.scalafx"         %% "scalafx"     % Versions.scalaFX,
      libraryDependencies += "org.typelevel"       %% "cats-effect" % Versions.catsEffect,
      libraryDependencies += "co.fs2"              %% "fs2-core"    % Versions.fs2,
      libraryDependencies += "com.disneystreaming" %% "weaver-cats" % Versions.weaver,
      testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
      libraryDependencies ++= javaFXDependencies(
        Seq("base", "controls"),
        provided = true
      ),
      name                                         := "core"
    )
    .settings(simpleLayout ++ commons)

lazy val demo =
  project
    .in(file("modules/demo"))
    .dependsOn(core)
    .settings(
      libraryDependencies ++= javaFXDependencies(
        Seq("base", "controls", "graphics", "web")
      ),
      run / fork     := true,
      libraryDependencies ++= Seq(
        "org.http4s" %% "http4s-ember-server" % Versions.http4s,
        "org.http4s" %% "http4s-dsl"          % Versions.http4s
      ),
      publish / skip := true
    )
    .settings(simpleLayout ++ commons)

lazy val docs =
  project
    .in(file("modules/docs"))
    /* .dependsOn(core) */
    .enablePlugins(SubatomicPlugin)
    .settings(publish / skip := true)
    /* .settings(commons) */

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
  scalaVersion       := Versions.Scala,
  crossScalaVersions := Versions.AllScala,
  organization       := "com.indoorvivants.effex"
)
