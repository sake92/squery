package public

import java.time.*
import ba.sake.squery.{*, given}
import public.models.*
import public.daos.*

class ActorDaoSuite extends munit.FunSuite with TestUtils {

  val matthewRows = Seq(
    ActorRow(
      ACTOR_ID = 8,
      FIRST_NAME = "MATTHEW",
      LAST_NAME = "JOHANSSON",
      LAST_UPDATE = LocalDateTime.parse("2006-02-15T04:34:33")
    ),
    ActorRow(
      ACTOR_ID = 103,
      FIRST_NAME = "MATTHEW",
      LAST_NAME = "LEIGH",
      LAST_UPDATE = LocalDateTime.parse("2006-02-15T04:34:33")
    ),
    ActorRow(
      ACTOR_ID = 181,
      FIRST_NAME = "MATTHEW",
      LAST_NAME = "CARREY",
      LAST_UPDATE = LocalDateTime.parse("2006-02-15T04:34:33")
    )
  )

  test("ActorDao should work as expected") {
    squeryCtx.run {
      /* count */
      val totalCount = ActorDao.countAll()
      assertEquals(totalCount, 200)

      val matthewsCount = ActorDao.countWhere(sql"${ActorRow.firstName} = 'MATTHEW'")
      assertEquals(matthewsCount, 3)

      /* find */
      val matthews = ActorDao.findAllWhere(sql"${ActorRow.firstName} = 'MATTHEW'")
      assertEquals(matthews, matthewRows)

      val matthewJohansson =
        ActorDao.findWhere(sql"${ActorRow.firstName} = 'MATTHEW' AND ${ActorRow.lastName} = 'JOHANSSON'")
      assertEquals(matthewJohansson, matthewRows(0))

      val matthewJohanssonById = ActorDao.findById(8)
      assertEquals(matthewJohanssonById, matthewRows(0))

      val matthewsByIds = ActorDao.findByIds(Set(8, 103))
      assertEquals(matthewsByIds, matthewRows.take(2))

      /* insert */
      val newMatthew = ActorRow(
        ACTOR_ID = 203,
        FIRST_NAME = "MATTHEW",
        LAST_NAME = "MATTHEWEWICH",
        LAST_UPDATE = LocalDateTime.parse("2006-02-15T04:34:33")
      )
      ActorDao.insert(newMatthew)
      val newMatthewInserted = ActorDao.findById(newMatthew.ACTOR_ID)
      assertEquals(newMatthewInserted, newMatthew)

      /* upadte */
      val updateMatthew = newMatthew.copy(LAST_NAME = "MATTHEWROLOMEU")
      ActorDao.updateById(updateMatthew)
      val newMatthewUpdated = ActorDao.findById(newMatthew.ACTOR_ID)
      assertEquals(newMatthewUpdated, updateMatthew)

      /* delete */
      ActorDao.deleteById(newMatthew.ACTOR_ID)
      val newMatthewOpt = ActorDao.findByIdOpt(newMatthew.ACTOR_ID)
      assertEquals(newMatthewOpt, None)
    }
  }
}
