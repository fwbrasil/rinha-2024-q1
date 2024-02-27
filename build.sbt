val scala3Version = "3.3.1"
val kyoVersion = "0.8.7+1-40d781d9+20240226-1601-SNAPSHOT"

ThisBuild / assemblyMergeStrategy := (_ => MergeStrategy.first)

lazy val root = project
  .in(file("."))
  .settings(
    name := "rinha-2024-q1",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++=
      Seq(
        "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % "1.9.10",
        "ch.qos.logback" % "logback-classic" % "1.5.0",
        "io.getkyo" %% "kyo-tapir" % kyoVersion,
        "io.getkyo" %% "kyo-direct" % kyoVersion
      )
  )
