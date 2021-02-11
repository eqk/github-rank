name := """github-rank"""
organization := "ru.krylovd"

version := "0.0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(guice, ws, ehcache)
libraryDependencies += "org.typelevel"          %% "cats-core"          % "2.1.1"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
