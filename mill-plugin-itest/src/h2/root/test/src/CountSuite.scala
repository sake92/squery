import ba.sake.squery.{*, given}
import public.daos.*

class CountSuite extends munit.FunSuite {

  test("Counts should return correct number") {
    val actorsCount = Globals.ctx.run {
      ActorCrudDao.countAll()
    }
    assertEquals(actorsCount, 200)

    val adressCount = Globals.ctx.run {
      AddressCrudDao.countAll()
    }
    assertEquals(adressCount, 603)
  }
}
