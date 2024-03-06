val scala3Version = "3.3.1"
val kyoVersion    = "0.8.8+21-47e92c9f+20240306-1046-SNAPSHOT"

ThisBuild / assemblyMergeStrategy := (_ => MergeStrategy.first)

lazy val root = project
    .in(file("."))
    .enablePlugins(GraalVMNativeImagePlugin)
    .settings(
        name         := "rinha-2024-q1",
        version      := "0.1.0-SNAPSHOT",
        scalaVersion := scala3Version,
        Compile / doc / sources := Seq.empty,
        libraryDependencies ++= Seq(
            "com.softwaremill.sttp.tapir" %% "tapir-json-zio"       % "1.9.11",
            "ch.qos.logback"               % "logback-classic"      % "1.5.0",
            "org.mongodb"                  % "mongodb-driver-async" % "3.12.14",
            "io.getkyo"                   %% "kyo-tapir"            % kyoVersion,
            "io.getkyo"                   %% "kyo-tapir"            % kyoVersion,
            "io.getkyo"                   %% "kyo-direct"           % kyoVersion,
            "io.getkyo"                   %% "kyo-os-lib"           % kyoVersion
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
