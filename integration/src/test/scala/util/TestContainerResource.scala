//package util
//
//import com.dimafeng.testcontainers.*
//import org.testcontainers.containers
////import domain.PortDetails
//import zio.*
//
//object TestContainerResource {
//
//  private val initAndStartKafka: ZIO[Any, Throwable, com.dimafeng.testcontainers.KafkaContainer] = for {
//    _ <- ZIO.logInfo(s"Starting Kafka test-container instance.")
//    container = com.dimafeng.testcontainers.KafkaContainer()
//    _ = container.start()
////    _ <- ZIO.attempt(container.start())
////    _ <- ZIO.sleep(zio.Duration.fromSeconds(20))
//    bs = container.bootstrapServers
//    _ = println(s"=============== ${bs} ==bs===>>>")
//  } yield container
////  private val initAndStartPostgres: ZIO[Any, Throwable, org.testcontainers.containers.PostgrSQLContainer[?]] = for {
////    container: org.testcontainers.containers.PostgreSQLContainer[?] <- ZIO.attempt(PostgreSQLContainer().container)
////    _ <- ZIO.attempt(container.start())
////    _ <- ZIO.logInfo(s"Starting PostgreSQL test-container instance. Url: [${container.getJdbcUrl}]" +
////      s" Port: [${container.getMappedPort(PortDetails.PostgresPort.port)}]")
////  } yield container
//
//  val kafkaResource =
//    ZIO.acquireRelease(initAndStartKafka) { (kafkaContainer: com.dimafeng.testcontainers.KafkaContainer) =>
//        ZIO.attempt(kafkaContainer.stop()).tapError(e =>
//          ZIO.logError(s"Something went wrong ${e.getMessage}")
//        ).orDie
//      }
//      .zipLeft(ZIO.logInfo("Stopping Kafka container"))
//      .tapError(t =>
//        ZIO.logErrorCause(s"Failed to stop Kafka container", Cause.die(t)))
//
//}
