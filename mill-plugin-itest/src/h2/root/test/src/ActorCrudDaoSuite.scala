package public

import java.time.*
import ba.sake.squery.{*, given}
import public.models.*
import public.daos.*

class ActorCrudDaoSuite extends munit.FunSuite {

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

  test("ActorCrudDao should work as expected") {
    Globals.ctx.run {
      /* count */
      val totalCount = ActorCrudDao.countAll()
      assertEquals(totalCount, 200)

      val matthewsCount = ActorCrudDao.countWhere(sql"${ActorRow.firstName} = 'MATTHEW'")
      assertEquals(matthewsCount, 3)

      /* find */
      val matthews = ActorCrudDao.findAllWhere(sql"${ActorRow.firstName} = 'MATTHEW'")
      assertEquals(matthews, matthewRows)

      val matthewJohansson =
        ActorCrudDao.findWhere(sql"${ActorRow.firstName} = 'MATTHEW' AND ${ActorRow.lastName} = 'JOHANSSON'")
      assertEquals(matthewJohansson, matthewRows(0))

      val matthewJohanssonById = ActorCrudDao.findById(8)
      assertEquals(matthewJohanssonById, matthewRows(0))

      val matthewsByIds = ActorCrudDao.findByIds(Set(8, 103))
      assertEquals(matthewsByIds, matthewRows.take(2))

      /* insert */
      val newMatthew = ActorRow(
        ACTOR_ID = 203,
        FIRST_NAME = "MATTHEW",
        LAST_NAME = "MATTHEWEWICH",
        LAST_UPDATE = LocalDateTime.parse("2006-02-15T04:34:33")
      )
      ActorCrudDao.insert(newMatthew)
      val newMatthewInserted = ActorCrudDao.findById(newMatthew.ACTOR_ID)
      assertEquals(newMatthewInserted, newMatthew)

      /* upadte */
      val updateMatthew = newMatthew.copy(LAST_NAME = "MATTHEWROLOMEU")
      ActorCrudDao.updateById(updateMatthew)
      val newMatthewUpdated = ActorCrudDao.findById(newMatthew.ACTOR_ID)
      assertEquals(newMatthewUpdated, updateMatthew)

      /* delete */
      ActorCrudDao.deleteById(newMatthew.ACTOR_ID)
      val newMatthewOpt = ActorCrudDao.findByIdOpt(newMatthew.ACTOR_ID)
      assertEquals(newMatthewOpt, None)
    }
  }
}
