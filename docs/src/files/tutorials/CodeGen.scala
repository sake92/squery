package files.tutorials

import utils.*
import Bundle.*, Tags.*

object CodeGen extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("Code Generation")

  override def blogSettings =
    super.blogSettings.withSections(introSection, standaloneGenerator, millSection)

  val introSection = Section(
    "Code generator",
    s"""
      Squery has a code generator that can generate code for various databases:  
        Postgres, MySQL, MariaDB, Oracle and H2.  
      
      It generates models for table rows and handy DAO code with various utility methods:
        - countAll, countWhere
        - findAll, findWhere, findWhereOpt, findAllWhere, findById, findByIdOpt, findByIds
        - insert, updateById
        - deleteWhere, deleteById, deleteIds
      
      Squery codegene is a bit special since it is using [Regenesca library](https://github.com/sake92/regenesca).  
      When you add a new column for example, it will refactor the `*Row` and `*Dao` code in place!  
      This means you can add your own methods/vals to the *generated code*, without fear that the codegen will remove it.  
      Of course, it is best to use `scalafmt` after codegen so that the diff is minimal.
      """.md
  )

  val standaloneGenerator = Section(
    "Standalone generator",
    s"""
      You can use Ammonite to test the generator:
      ```scala
      import $$ivy.`ba.sake:squery-generator_2.13:${Consts.ArtifactVersion}`
      import $$ivy.`ba.sake::squery:${Consts.ArtifactVersion}`
      import $$ivy.`org.postgresql:postgresql:42.7.4`
      import $$ivy.`com.zaxxer:HikariCP:5.1.0`
      import ba.sake.squery.generator.*
      import com.zaxxer.hikari.HikariDataSource

      // if using Postgres JSONB
      // import $$ivy.`ba.sake::squery-postgres-jawn:${Consts.ArtifactVersion}`
      // import ba.sake.squery.postgres.jawn.{*, given}

      val dataSource = HikariDataSource()
      dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/mydb")
      dataSource.setUsername("username")
      dataSource.setPassword("password")
      
      val generator = SqueryGenerator(dataSource)
      val generatedCode = generator.generateString(Seq("myschema"))
      repl.load(generatedCode)

      // now you can use the generated code
      val ctx = SqueryContext(dataSource)
      ctx.run {
        MyTableDao.findAll()
      }
      """.md
  )

  val millSection = Section(
    "Mill plugin",
    s"""
    Squery provides a Mill plugin:
    ```scala
    import $$ivy.`ba.sake::mill-squery-generator_mill0.11:${Consts.ArtifactVersion}`
    import ba.sake.squery.generator._
    import ba.sake.squery.generator.mill.SqueryGeneratorModule

    object app extends ScalaModule  with SqueryGeneratorModule {
      // use T.input(T.ctx.env("MY_ENV_VAR")) to set sensitive variables like password etc
      def squeryJdbcUrl = "jdbc:..."
      def squeryUsername = ".."
      def squeryPassword = ".."
      def squerySchemas = Seq("myschema" -> "com.mypackage.db")

      // override to tweak codegen settings
      def squeryGeneratorConfig: T[SqueryGeneratorConfig] = ...
    ```

    Then you can call generator like this:
    ```scala
    ./mill root.squeryGenerate
    ```
    and it will generate source code inside `com.mypackage.db`, based on `myschema` schema.
    """.md
  )

}
