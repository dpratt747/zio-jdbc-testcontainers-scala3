package database

import domain.PortDetails
import util.TestContainerResource
import zio.*
import zio.jdbc.*
import zio.test.*

object DatabaseExampleSpec extends ZIOSpecDefault {

  def connectionPool(host: String, port: Int, database: String, props: Map[String, String]): ZLayer[ZConnectionPoolConfig, Throwable, ZConnectionPool] =
    ZConnectionPool.postgres(host, port, database, props)

  private def properties(user: String, password: String) = Map(
    "user" -> user,
    "password" -> password
  )

  override def spec =
    suite("DatabaseInterpreter")(
      test("can successfully connect to a Postgres db instance") {
        TestContainerResource.resource.flatMap { postgresContainer =>
          (for {
            underTest <- transaction {
              sql"SELECT datname FROM pg_database".query[String].selectOne
            }
          } yield assertTrue(underTest.contains("postgres"))).provide(
            connectionPool(
              postgresContainer.getHost,
              postgresContainer.getMappedPort(PortDetails.PostgresPort.port),
              postgresContainer.getDatabaseName,
              properties(postgresContainer.getUsername, postgresContainer.getPassword)
            ),
            ZLayer.succeed(ZConnectionPoolConfig.default)
          )
        }.provide(
          Scope.default
        )
      } @@ TestAspect.timeout(zio.Duration.fromSeconds(35))
    )
}
