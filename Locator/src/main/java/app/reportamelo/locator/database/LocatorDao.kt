package app.reportamelo.locator.database

import androidx.room.Dao
import androidx.room.Query
import app.reportamelo.locator.data.DistrictEntity
import app.reportamelo.locator.data.CantonEntity

@Dao
interface LocatorDao {
    @Query("""
        SELECT * FROM cities 
        WHERE countryCode = :countryCode AND 
              (:thirdLevel = '' OR thirdLevel LIKE '%' || :thirdLevel || '%')
        LIMIT :limit
    """)
    @JvmSuppressWildcards suspend fun findDistrictsByThirdLevel(thirdLevel: String, countryCode: String, limit: Int = 30): List<DistrictEntity>

    @Query("""
        SELECT * FROM cities 
        WHERE countryCode = :countryCode AND 
              (:groupingName = '' OR groupingName LIKE '%' || :groupingName || '%')
        LIMIT :limit
    """)
    @JvmSuppressWildcards suspend fun findDistrictsByGroupingName(groupingName: String, countryCode: String, limit: Int = 30): List<DistrictEntity>

    @Query("SELECT * FROM cities WHERE countryCode = :countryCode AND isDepartmentalCapital = 1")
    @JvmSuppressWildcards suspend fun findDepartmentalCapitals(countryCode: String): List<DistrictEntity>

    @Query("""
        SELECT * FROM cities 
        WHERE countryCode = :countryCode AND 
              (:stateName = '' OR secondLevel LIKE '%' || :stateName || '%')
        LIMIT :limit
    """)
    @JvmSuppressWildcards suspend fun findDistrictsByState(stateName: String, countryCode: String, limit: Int = 30): List<DistrictEntity>

    @Query("SELECT * FROM cities WHERE cityId = :cityId AND countryCode = :countryCode LIMIT 1")
    @JvmSuppressWildcards suspend fun findCityById(cityId: String, countryCode: String): DistrictEntity?

    @Query("SELECT * FROM cities WHERE thirdLevel = :name LIMIT 1")
    @JvmSuppressWildcards suspend fun findCityByName(name: String): DistrictEntity?

    @Query("SELECT * FROM cantons WHERE cityId = :cityId")
    @JvmSuppressWildcards suspend fun getCantonsOf(cityId: String): List<CantonEntity>
}
