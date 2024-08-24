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
      Squery has a standalone code generator that can generate code for various databases:  
        Postgres, MySQL, MariaDB, Oracle and H2.  
      
      It generates models for table rows and handy DAO code with various utility methods:
        - countAll, countWhere
        - findAll, findWhere, findWhereOpt, findAllWhere, findById, findByIdOpt, findByIds
        - insert, updateById
        - deleteWhere, deleteById, deleteIds
      """.md
  )

  val standaloneGenerator = Section(
    "Standalone generator",
    s"""
      You can use Ammonite to test the generator:
      ```scala
      import $$ivy.`ba.sake::squery-generator:${Consts.ArtifactVersion}`

      val dataSource = new org.h2.jdbcx.JdbcDataSource()
      dataSource.setURL("jdbc:postgres...")
      
      val generator = new SqueryGenerator(dataSource)
      val generatedCode = generator.generateString("myschema")
      repl.load(generatedCode)

      // now you can use the generated code
      MyTableDao.findAll()
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
    """.md
  )

}
