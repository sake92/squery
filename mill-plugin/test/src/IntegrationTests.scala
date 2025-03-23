package ba.sake.squery

import scala.concurrent.duration.Duration
import mill.api.PathRef
import mill.testkit.IntegrationTester

class IntegrationTests extends munit.FunSuite {

  override val munitTimeout = Duration(5, "m")

  test("integration") {
    val millExecutable = os.Path(sys.env("MILL_EXECUTABLE_PATH"))
    val resourceFolder = os.Path(sys.env("MILL_TEST_RESOURCE_DIR"))

    scala.util.Using.resource(
      new IntegrationTester(
        clientServerMode = false,
        workspaceSourcePath = resourceFolder / "h2",
        millExecutable = millExecutable
      )
    ) { tester =>

      assert(tester.eval("flywayMigrate").isSuccess)
      assert(tester.eval("squeryGenerate").isSuccess)
      val squeryTargetDir = tester.out("squeryTargetDir").value[PathRef]

      val generatedModels = os.walk(squeryTargetDir.path / "public" / "models").filter(os.isFile)
      assertEquals(generatedModels.size, 17) // 16 + flyway table..
      assert(generatedModels.map(_.last).contains("ActorRow.scala"), "ActorRow was not generated")

      val generatedDaos = os.walk(squeryTargetDir.path / "public" / "daos").filter(os.isFile)
      assertEquals(generatedDaos.size, 17)
      assert(generatedDaos.map(_.last).contains("ActorDao.scala"), "ActorDao was not generated")

      assert(tester.eval("test").isSuccess)
    }
  }
}
