val scala3Version = "3.3.1"
val kyoVersion    = "0.8.10"

ThisBuild / assemblyMergeStrategy := (_ => MergeStrategy.first)

lazy val root = project
    .in(file("."))
    .settings(
        name                    := "rinha-2024-q1",
        version                 := "0.1.0-SNAPSHOT",
        scalaVersion            := scala3Version,
        Compile / doc / sources := Seq.empty,
        libraryDependencies ++= Seq(
            "com.softwaremill.sttp.tapir" %% "tapir-json-zio"  % "1.9.11",
            "ch.qos.logback"               % "logback-classic" % "1.5.0",
            "io.getkyo"                   %% "kyo-tapir"       % kyoVersion,
            "io.getkyo"                   %% "kyo-tapir"       % kyoVersion,
            "io.getkyo"                   %% "kyo-direct"      % kyoVersion,
            "io.getkyo"                   %% "kyo-os-lib"      % kyoVersion,
            "org.scalatest"               %% "scalatest"       % "3.2.16" % Test
        ),
        scalacOptions ++= Seq(
            "-encoding",
            "utf8",
            "-feature",
            "-unchecked",
            "-language:implicitConversions",
            "-Wvalue-discard",
            "-Wunused:all"
        )
    )
