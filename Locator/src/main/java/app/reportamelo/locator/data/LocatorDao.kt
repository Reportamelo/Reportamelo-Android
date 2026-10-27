package app.reportamelo.locator.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface LocatorDao {
    @Transaction
    @Query("SELECT * FROM cities")
    fun getAllDistrictsWithCantons(): List<DistrictWithCantons>

    @Query("SELECT * FROM cities WHERE cityId = :cityId")
    fun getDistrictById(cityId: String): District?

    @Transaction
    @Query("SELECT * FROM cities WHERE cityId = :cityId")
    fun getDistrictWithCantonsById(cityId: String): DistrictWithCantons?

    @Query("SELECT * FROM cantons WHERE code = :code")
    fun getCantonByCode(code: String): Canton?
}
