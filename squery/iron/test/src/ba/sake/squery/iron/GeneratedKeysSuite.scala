package ba.sake.squery.iron

import java.io.File
import java.net.{URL, URLClassLoader}
import java.nio.file.{Files, Path}
import java.sql.{Connection, DriverManager}
import scala.jdk.CollectionConverters.*
import scala.util.Using
import ba.sake.squery.generator.{SchemaConfig, SqueryGenerator, SqueryGeneratorConfig, TableFilter, TypeMappingRule}

/** Compiles the actual generated files, then executes their DAOs against JDBC. */
class GeneratedKeysSuite extends munit.FunSuite:
  override val munitTimeout = scala.concurrent.duration.Duration(2, "minutes")

  test("SQLite generated PKs, nullable FKs, shared PKs and composite FKs compile and execute") {
    Using.resource(DriverManager.getConnection("jdbc:sqlite::memory:")) { conn =>
      execute(
        conn,
        Seq(
          "PRAGMA foreign_keys = ON",
          "CREATE TABLE users(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)",
          "CREATE TABLE orders(id INTEGER PRIMARY KEY, user_id INTEGER NOT NULL REFERENCES users(id), reviewer_id INTEGER REFERENCES users(id))",
          "CREATE TABLE profiles(id INTEGER PRIMARY KEY REFERENCES users(id), bio TEXT NOT NULL)",
          "CREATE TABLE memberships(tenant_id INTEGER NOT NULL, id INTEGER NOT NULL, label TEXT NOT NULL, PRIMARY KEY(tenant_id, id))",
          "CREATE TABLE events(tenant_id INTEGER NOT NULL, member_id INTEGER NOT NULL, FOREIGN KEY(tenant_id, member_id) REFERENCES memberships(tenant_id, id))",
          "CREATE TABLE nodes(id INTEGER PRIMARY KEY, parent_id INTEGER REFERENCES nodes(id))"
        )
      )
      withDirectory { directory =>
        val schema = SchemaConfig("main", directory, "generated")
        // Exercise migration from existing primitive keys and subsequent regeneration.
        new SqueryGenerator(conn).generateFiles(Seq(schema))
        val userFile = directory.resolve("generated/models/UsersRow.scala")
        Files.writeString(
          userFile,
          Files
            .readString(userFile)
            .replace(
              "def pk:",
              "def description: String = name\n  def pk:"
            )
        )
        val generator = new SqueryGenerator(conn, SqueryGeneratorConfig.Default.copy(ironNewtypes = true))
        generator.generateFiles(Seq(schema))
        generator.generateFiles(Seq(schema))
        val probe = """
          package generated
          import ba.sake.squery.*
          import generated.models.*
          import generated.daos.*
          import scala.compiletime.testing.typeChecks
          object Probe {
            def run(conn: java.sql.Connection): Unit = {
              given SqueryConnection = SqueryConnection(conn, false)
              val alice = UsersDao.insert(UsersRow(UsersRow.PK(0L), "Alice"))
              assert(UsersDao.findById(alice.id) == alice)
              assert(alice.description == "Alice")
              assert(UsersDao.findByIds(Set(alice.id)) == Seq(alice))
              val order = OrdersDao.insert(OrdersRow(OrdersRow.PK(10L), alice.id, Some(alice.id)))
              assert(OrdersDao.findById(order.pk).reviewer_id.contains(alice.pk))
              val second = OrdersDao.insert(OrdersRow(OrdersRow.PK(11L), alice.id, None))
              assert(OrdersDao.findById(second.pk).reviewer_id.isEmpty)
              val profile = ProfilesDao.insert(ProfilesRow(ProfilesRow.PK(1L), "bio"))
              val sameId: UsersRow.PK = profile.pk
              assert(sameId == alice.pk)
              val membership = MembershipsDao.insert(MembershipsRow(
                MembershipsRow.TenantIdKey(3L), MembershipsRow.IdKey(4L), "member"))
              assert(MembershipsDao.findById(membership.pk) == membership)
              val event = EventsDao.insert(EventsRow(membership.tenant_id, membership.id))
              assert(event.member_id == membership.id)
              val node = NodesDao.insert(NodesRow(NodesRow.PK(1L), None))
              assert(NodesDao.insert(NodesRow(NodesRow.PK(2L), Some(node.id))).parent_id.contains(node.id))
              assert(OrdersDao.updateById(order.copy(reviewer_id = None)) == 1)
              assert(OrdersDao.findById(order.pk).reviewer_id.isEmpty)
              assert(OrdersDao.deleteByIds(Set(order.pk, second.pk)) == 2)
              assert(ProfilesDao.deleteById(profile.pk) == 1)
              assert(UsersDao.deleteById(alice.pk) == 1)
              assert(typeChecks("import generated.models.*; val id: UsersRow.PK = UsersRow.PK(1L)"))
              assert(!typeChecks("import generated.models.*; val id: UsersRow.PK = OrdersRow.PK(1L)"))
              assert(!typeChecks("import generated.models.*; UsersRow(1L, 1.toString)"))
              assert(!typeChecks("import generated.models.*; OrdersRow(OrdersRow.PK(1L), OrdersRow.PK(2L), None)"))
              assert(!typeChecks("import generated.models.*; MembershipsRow.PK(MembershipsRow.IdKey(1L), MembershipsRow.TenantIdKey(2L))"))
              assert(!typeChecks("import generated.models.*; import generated.daos.*; UsersDao.findById(OrdersRow.PK(1L))"))
            }
          }
        """
        compileAndRun(directory, probe, "generated.Probe", conn)
      }
    }
  }

  test("H2 generated files resolve cross-schema keys, UUIDs and mutually referencing rows") {
    Using.resource(DriverManager.getConnection("jdbc:h2:mem:")) { conn =>
      execute(
        conn,
        Seq(
          "CREATE SCHEMA account",
          "CREATE SCHEMA shop",
          "CREATE TABLE account.users(id UUID PRIMARY KEY, name VARCHAR NOT NULL, favorite_order UUID)",
          "CREATE TABLE shop.orders(id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES account.users(id))",
          "ALTER TABLE account.users ADD FOREIGN KEY(favorite_order) REFERENCES shop.orders(id)"
        )
      )
      withDirectory { directory =>
        val config = SqueryGeneratorConfig.Default.copy(
          ironNewtypes = true,
          typeMappingRules = Seq(TypeMappingRule(".*", "UUID", scala.meta.Type.Name("UUID"), Seq("java.util.UUID")))
        )
        val generator = new SqueryGenerator(conn, config)
        generator.generateFiles(
          Seq(
            SchemaConfig("ACCOUNT", directory, "generated.account"),
            SchemaConfig("SHOP", directory, "generated.shop")
          )
        )
        compileAndRun(
          directory,
          """
          package generated
          import ba.sake.squery.*
          import generated.account.models.*
          import generated.account.daos.*
          import generated.shop.models.*
          import generated.shop.daos.*
          object Probe {
            def run(conn: java.sql.Connection): Unit = {
              given SqueryConnection = SqueryConnection(conn, false)
              val user = UsersRow(UsersRow.PK(java.util.UUID.randomUUID()), "Alice", None)
              val order = OrdersRow(OrdersRow.PK(java.util.UUID.randomUUID()), user.ID)
              UsersDao.insert(user)
              OrdersDao.insert(order)
              assert(UsersDao.findById(user.pk) == user)
              assert(OrdersDao.findById(order.pk).USER_ID == user.pk)
              assert(UsersDao.updateById(user.copy(FAVORITE_ORDER = Some(order.pk))) == 1)
              assert(UsersDao.findById(user.pk).FAVORITE_ORDER.contains(order.pk))
            }
          }
        """,
          "generated.Probe",
          conn
        )
      }
    }
  }

  test("excluded FK targets fail before generation writes any files") {
    Using.resource(DriverManager.getConnection("jdbc:sqlite::memory:")) { conn =>
      execute(
        conn,
        Seq(
          "CREATE TABLE users(id INTEGER PRIMARY KEY)",
          "CREATE TABLE orders(id INTEGER PRIMARY KEY, user_id INTEGER REFERENCES users(id))"
        )
      )
      withDirectory { directory =>
        val config = SqueryGeneratorConfig.Default.copy(
          ironNewtypes = true,
          tableFilter = TableFilter(excludePatterns = Seq("main.users"))
        )
        val error = intercept[IllegalArgumentException] {
          new SqueryGenerator(conn, config).generateFiles(Seq(SchemaConfig("main", directory, "generated")))
        }
        assert(error.getMessage.contains("main.users"))
        Using.resource(Files.list(directory))(entries => assertEquals(entries.count(), 0L))
      }
    }
  }

  private def compileAndRun(directory: Path, probe: String, className: String, conn: Connection): Unit =
    Files.writeString(directory.resolve("Probe.scala"), probe)
    val output = Files.createDirectory(directory.resolve("classes"))
    val sources = Using.resource(Files.walk(directory)) { files =>
      files.iterator().asScala.filter(_.toString.endsWith(".scala")).map(_.toString).toSeq
    }
    def classpath(loader: ClassLoader): Seq[String] =
      if loader == null then Seq.empty
      else
        val own = loader match
          case urls: URLClassLoader => urls.getURLs.toSeq.map(url => Path.of(url.toURI).toString)
          case _                    => Seq.empty
        own ++ classpath(loader.getParent)
    // Deder's test runner has its own Scala 2 library. Isolate the Scala 3.9
    // compiler and generated code so that its parent cannot shadow the new stdlib.
    val cp =
      (System.getProperty("java.class.path").split(File.pathSeparator).toSeq ++ classpath(getClass.getClassLoader))
        .filterNot(path =>
          path.contains("scala-library-2.") || path.contains("scala3-library_3") || path.endsWith("test-runner.jar")
        )
        .distinct
    val urls = (output.toString +: cp).map(path => Path.of(path).toUri.toURL).toArray
    Using.resource(new URLClassLoader(urls, ClassLoader.getPlatformClassLoader)) { loader =>
      val driver = loader.loadClass("dotty.tools.dotc.Driver").getConstructor().newInstance()
      val arguments = (Seq(
        "-classpath",
        cp.mkString(File.pathSeparator),
        "-d",
        output.toString,
        "-language:implicitConversions"
      ) ++ sources).toArray
      val reporter = driver.getClass
        .getMethod("process", classOf[Array[String]])
        .invoke(driver, arguments.asInstanceOf[Object])
      assert(
        !reporter.getClass.getMethod("hasErrors").invoke(reporter).asInstanceOf[Boolean],
        "Generated source failed to compile; see compiler diagnostics above."
      )
      val module = loader.loadClass(className + "$").getField("MODULE$").get(null)
      module.getClass.getMethod("run", classOf[Connection]).invoke(module, conn)
    }

  private def execute(conn: Connection, statements: Seq[String]): Unit =
    Using.resource(conn.createStatement())(stmt => statements.foreach(stmt.execute))

  private def withDirectory(body: Path => Unit): Unit =
    val directory = Files.createTempDirectory("squery-generated-iron-")
    try body(directory)
    finally
      Using.resource(Files.walk(directory)) { paths =>
        paths.iterator().asScala.toSeq.sortBy(_.getNameCount).reverse.foreach(Files.delete)
      }
