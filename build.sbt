name := """reactive-aerospike"""

version := "0.1.6"

organization := "eu.unicredit"

scalaVersion := "2.11.5"

crossScalaVersions := Seq("2.9.2", "2.11.5")

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-language:reflectiveCalls"
)

libraryDependencies ++= Seq(
	  "com.aerospike" % "aerospike-client" % "3.0.34",
	  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
	  "com.twitter" %% "util-collection" % "6.23.0" % "test"
)

resolvers ++= Seq(
	"Local Aerospike Build" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)

publishMavenStyle := true

parallelExecution in Test := false

pomIncludeRepository := { x => false }

scalariformSettings