ThisBuild / scalaVersion := "3.4.2"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val zioVersion = "2.1.3"
lazy val sttpVersion = "3.9.7"
lazy val testContainerScalaVersion = "0.41.4"
lazy val zioJdbcVersion = "0.1.2"
lazy val postgresDriverVersion = "42.7.3"
lazy val flywayVersion = "10.15.0"

lazy val root = (project in file("."))
  .settings(
    name := "zio-jdbc-testcontainers-scala3",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-jdbc" % zioJdbcVersion,
      "org.postgresql" % "postgresql" % postgresDriverVersion
    ) ++ testDependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val integration = (project in file("integration"))
  .dependsOn(root)
  .settings(
    publish / skip := true,
    libraryDependencies ++= testDependencies
  )

lazy val testDependencies = Seq(
  "dev.zio" %% "zio-test" % zioVersion % Test,
  "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
  "dev.zio" %% "zio-test-magnolia" % zioVersion % Test,
  "org.flywaydb" % "flyway-database-postgresql" % flywayVersion % Test,
  "com.dimafeng" %% "testcontainers-scala-postgresql" % testContainerScalaVersion % Test
)

Test / fork := true