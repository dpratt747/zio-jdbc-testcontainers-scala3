ThisBuild / scalaVersion := "3.4.2"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val zioVersion = "2.1.3"
lazy val sttpVersion = "3.9.7"
lazy val testContainersForZioVersion = "0.10.0"
lazy val zioJdbcVersion = "0.1.2"

lazy val root = (project in file("."))
  .settings(
    name := "zio-jdbc-testcontainers-scala3",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-jdbc" % zioJdbcVersion
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
  "io.github.scottweaver" %% "zio-2-0-testcontainers-postgresql" % testContainersForZioVersion % Test,
  "io.github.scottweaver" %% "zio-2-0-db-migration-aspect" % testContainersForZioVersion % Test
)

Test / fork := true