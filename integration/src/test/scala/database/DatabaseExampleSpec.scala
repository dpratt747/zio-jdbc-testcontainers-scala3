package database

import database.schemas.UserTable
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
      },
      test("can successfully insert into a Postgres db instance that has been migrated with flyway") {
        TestContainerResource.postgresResource.flatMap { postgresContainer =>
          (for {
            flyway <- FlywayResource.flywayResource(postgresContainer.getJdbcUrl, postgresContainer.getUsername, postgresContainer.getPassword)
            validationResult: ValidateResult <- ZIO.attempt(flyway.validateWithResult())
            uName = "LimbMissing"
            fName = "David"
            lName = "Pratt"
            insertSqlFrag = sql"insert into user_table (user_name, first_name, last_name)".values((uName, fName, lName))
            //            selectSqlFrag = sql"select * from user_table".query[(Int, String, String, String)]
            selectSqlFrag = sql"select * from user_table".query[UserTable]
            underTest <- transaction(
              insertSqlFrag.insert *> selectSqlFrag.selectAll
            )
          } yield assertTrue(
            validationResult.validationSuccessful,
            underTest match {
              case Chunk(userTableRow) =>
                userTableRow.userName == uName && userTableRow.firstName == fName && userTableRow.lastName == lName
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
      }
    ) @@ TestAspect.timeout(zio.Duration.fromSeconds(35))
}
