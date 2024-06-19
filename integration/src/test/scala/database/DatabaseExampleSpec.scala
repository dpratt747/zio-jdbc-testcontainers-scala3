package database

import domain.PortDetails
import org.flywaydb.core.api.output.ValidateResult
import util.{FlywayResource, TestContainerResource}
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
        TestContainerResource.postgresResource.flatMap { postgresContainer =>
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
      } @@ TestAspect.timeout(zio.Duration.fromSeconds(35)),
      test("can successfully insert into a Postgres db instance that has been migrated with flyway") {
        TestContainerResource.postgresResource.flatMap { postgresContainer =>
          (for {
            flyway <- FlywayResource.flywayResource(postgresContainer.getJdbcUrl, postgresContainer.getUsername, postgresContainer.getPassword)
            validationResult: ValidateResult <- ZIO.attempt(flyway.validateWithResult())
            insertSqlFrag = sql"insert into user_table (user_name, first_name, last_name)".values(("LimbMissing", "David", "Pratt"))
            getSqlFrag = sql"select * from user_table".query[(Int, String, String, String)]
            underTest <- transaction (
              insertSqlFrag.insert.zip(getSqlFrag.selectAll)
            )
          } yield assertTrue(
            validationResult.validationSuccessful,
            underTest match {
              case (longRowsUpdated, Chunk((_, userName, firstName, lastName))) =>
                longRowsUpdated == 1 && userName == "LimbMissing" && firstName == "David" && lastName == "Pratt"
            }
          )).provide(
            connectionPool(
              postgresContainer.getHost,
              postgresContainer.getMappedPort(PortDetails.PostgresPort.port),
              postgresContainer.getDatabaseName,
              properties(postgresContainer.getUsername, postgresContainer.getPassword)
            ),
            ZLayer.succeed(ZConnectionPoolConfig.default),
            Scope.default
          )
        }
      } @@ TestAspect.timeout(zio.Duration.fromSeconds(35))
    )
}
