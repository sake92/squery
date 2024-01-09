package files.tutorials

import utils.*
import Bundle.*, Tags.*

object Index extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("Tutorials")
    .withLabel("Tutorials")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "Quickstart",
    s"""
      Hello world!
    """.md,
    List(
      Section(
        "Mill",
        s"""
        ```scala
        def ivyDeps = super.ivyDeps() ++ Agg(
          ivy"${Consts.ArtifactOrg}::${Consts.ArtifactName}:${Consts.ArtifactVersion}"
          // <add your favorite JDBC driver>
          // <preferably add a connection pool like HikariCP too>
        )
        ```
        """.md
      ),
      Section(
        "Sbt",
        s"""
        ```scala
        libraryDependencies ++= Seq(
          "${Consts.ArtifactOrg}" %% "${Consts.ArtifactName}" % "${Consts.ArtifactVersion}"
          // <add your favorite JDBC driver>
          // <preferably add a connection pool like HikariCP too>
        )
        ```
        """.md
      ),
      Section(
        "Scala CLI",
        s"""
        ```scala
        //> using dep ${Consts.ArtifactOrg}::${Consts.ArtifactName}:${Consts.ArtifactVersion}
        // <add your favorite JDBC driver>
        // <preferably add a connection pool like HikariCP too>
        ```
        """.md
      ),
      Section(
        "Scastie",
        s"""
        You can also use this [Scastie example](${Consts.ScastieExampleUrl}) to try ${Consts.ProjectName} online.
        """.md
      ),
      Section(
        "Examples",
        div(
          s"""
          You can find examples:
          - in the [examples](${Consts.GhSourcesUrl}/examples) folder
          - in the [sharaf-petclinic demo](https://github.com/sake92/sharaf-petclinic/tree/main/app/src/ba/sake/sharaf/petclinic/db/daos)
          - in the [squery tests](${Consts.GhSourcesUrl}/squery/test/src/ba/sake/squery/SquerySuite.scala)
          """.md
        )
      )
    )
  )
}
