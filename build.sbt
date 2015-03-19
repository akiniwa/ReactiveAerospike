import SonatypeKeys._

name := """reactive-aerospike"""

version := "0.2.0-SNAPSHOT"

organization := "eu.unicredit"

scalaVersion := "2.11.6"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-language:reflectiveCalls",
  "-language:existentials",
  "-language:higherKinds"
)

libraryDependencies ++= Seq(
  "com.aerospike" % "aerospike-client" % "3.0.35",
  "org.scala-lang" % "scala-reflect" % "2.11.6",
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
  "com.twitter" %% "util-collection" % "6.23.0" % "test"
)

publishMavenStyle := true

parallelExecution in Test := false

pomIncludeRepository := { x => false }

scalariformSettings

sonatypeSettings

credentials += Credentials(Path.userHome / ".ivy2" / "sonatype.credentials")
