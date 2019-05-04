lazy val akkaHttpVersion = "10.1.8"
lazy val akkaVersion = "2.5.19"
lazy val akkaPersistenceCassandraVersion = "0.96"
lazy val slickVersion = "3.3.0"
lazy val mariadbDriverVersion = "2.4.1"

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:existentials",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Ywarn-dead-code",
  "-Ywarn-nullary-unit",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused",
  "-Ywarn-unused-import",
)

lazy val root = (project in file(".")).settings(
  libraryDependencies ++= Seq(
    "com.github.romix.akka" %% "akka-kryo-serialization" % "0.5.1",
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-cassandra" % akkaPersistenceCassandraVersion,
    "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.slick" %% "slick" % slickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
    "org.mariadb.jdbc" % "mariadb-java-client" % mariadbDriverVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  ),
)

lazy val `slick-codegen` = (project in file("slick-codegen")).settings(
  libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick-codegen" % slickVersion,
    "org.mariadb.jdbc" % "mariadb-java-client" % mariadbDriverVersion,
  ),
)
