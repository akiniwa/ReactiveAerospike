import AssemblyKeys._ 
import sbtassembly.Plugin._

name := """reactive-aerospike"""

version := "0.1.1-SNAPSHOT"

organization := "eu.unicredit"

scalaVersion := "2.11.4"

crossScalaVersions := Seq("2.9.2", "2.11.4")

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-language:reflectiveCalls"
)

libraryDependencies ++= Seq(
	  "com.aerospike" % "aerospike-client" % "3.0.32",
	  "com.twitter" %% "util-collection" % "6.23.0"  
)

resolvers ++= Seq(
)

publishMavenStyle := true

pomIncludeRepository := { x => false }

//To be removed
assemblySettings