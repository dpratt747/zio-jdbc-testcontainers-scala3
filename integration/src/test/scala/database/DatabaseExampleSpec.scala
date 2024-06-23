package database

import database.schemas.UserTable
import domain.PortDetails
import org.flywaydb.core.api.output.ValidateResult
import util.*
import zio.*
import zio.jdbc.*
import zio.test.*

object DatabaseExampleSpec extends ZIOSpecDefault {

  override def spec =
    suite("DatabaseInterpreter")(
      test("can successfully connect to a Postgres db instance") {
        TestContainerResource.postgresResource.flatMap { postgresContainer =>
          (for {
            underTest <- transaction {
              sql"SELECT datname FROM pg_database".query[String].selectOne
            }
          } yield assertTrue(underTest.contains("postgres"))).provide(
            ZConnectionPoolWrapper.connectionPool(
              postgresContainer.getHost,
              postgresContainer.getMappedPort(PortDetails.PostgresPort.port),
              postgresContainer.getDatabaseName,
              postgresContainer.getUsername,
              postgresContainer.getPassword
            ),
            ZLayer.succeed(ZConnectionPoolConfig.default)
          )
        }
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
            ZConnectionPoolWrapper.connectionPool(
              postgresContainer.getHost,
              postgresContainer.getMappedPort(PortDetails.PostgresPort.port),
              postgresContainer.getDatabaseName,
              postgresContainer.getUsername,
              postgresContainer.getPassword
            ),
            ZLayer.succeed(ZConnectionPoolConfig.default),
            Scope.default
          )
        }
      }
    ) @@ TestAspect.timeout(zio.Duration.fromSeconds(35))
}
