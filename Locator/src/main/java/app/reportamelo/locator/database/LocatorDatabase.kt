package app.reportamelo.locator.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.reportamelo.locator.data.DistrictEntity
import app.reportamelo.locator.data.CantonEntity
import java.text.Normalizer

@Database(entities = [DistrictEntity::class, CantonEntity::class], version = 1, exportSchema = false)
abstract class LocatorDatabase : RoomDatabase() {
    abstract fun locatorDao(): LocatorDao

    companion object {
        @Volatile
        private var INSTANCE: LocatorDatabase? = null

        fun getDatabase(context: Context): LocatorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocatorDatabase::class.java,
                    "districts.db"
                )
                .createFromAsset("database/districts.db")
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object LocatorLocalDataSource {

    private val allDistricts: List<DistrictEntity> by lazy {
        loadDistrictsFromResource()
    }

    private fun loadDistrictsFromResource(): List<DistrictEntity> {
        val stream = LocatorLocalDataSource::class.java.classLoader
            ?.getResourceAsStream("SV.districts.tsv")
            ?: LocatorLocalDataSource::class.java.getResourceAsStream("/SV.districts.tsv")
            ?: return emptyList()

        return stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.mapNotNull { line ->
                if (line.isBlank()) return@mapNotNull null
                val cols = line.split('\t')
                if (cols.size < 18) return@mapNotNull null
                DistrictEntity(
                    cityId = cols[0],
                    countryCode = cols[1],
                    zipCode = cols[2].ifEmpty { null },
                    firstLevel = cols[3].ifEmpty { null },
                    secondLevel = cols[4].ifEmpty { null },
                    thirdLevel = cols[5].ifEmpty { null },
                    groupingId = cols[6].ifEmpty { null },
                    groupingName = cols[7].ifEmpty { null },
                    groupingNameCode = cols[8].ifEmpty { null },
                    legalGroupName = cols[9].ifEmpty { null },
                    isCapitalCity = cols[10] == "1" || cols[10].equals("true", ignoreCase = true),
                    isDepartmentalCapital = cols[11] == "1" || cols[11].equals("true", ignoreCase = true),
                    lat = cols[12].toDoubleOrNull() ?: 0.0,
                    lng = cols[13].toDoubleOrNull() ?: 0.0,
                    cityNameSortKey = cols[14].ifEmpty { null },
                    groupingNameSortKey = cols[15].ifEmpty { null },
                    stateNameSortKey = cols[16].ifEmpty { null },
                    geoCode = cols[17].ifEmpty { null }
                )
            }.toList()
        }
    }

    private fun String.normalizedForSearch(): String {
        val decomposed = Normalizer.normalize(this, Normalizer.Form.NFD)
        return decomposed.replace("\\p{Mn}+".toRegex(), "").lowercase()
    }

    private fun String?.localizedStandardContains(query: String): Boolean {
        if (this == null) return false
        if (query.isEmpty()) return true
        return this.normalizedForSearch().contains(query.normalizedForSearch())
    }

    fun findDepartmentalCapitals(countryCode: String): List<DistrictEntity> {
        return allDistricts.filter {
            it.countryCode.equals(countryCode, ignoreCase = true) && it.isDepartmentalCapital
        }
    }

    fun findDistrictsByThirdLevel(
        thirdLevel: String,
        countryCode: String,
        limit: Int = 30
    ): List<DistrictEntity> {
        return allDistricts.asSequence()
            .filter {
                it.countryCode.equals(countryCode, ignoreCase = true) &&
                    (thirdLevel.isEmpty() || it.thirdLevel.localizedStandardContains(thirdLevel))
            }
            .take(limit)
            .toList()
    }

    fun findDistrictsByGroupingName(
        groupingName: String,
        countryCode: String,
        limit: Int = 30
    ): List<DistrictEntity> {
        return allDistricts.asSequence()
            .filter {
                it.countryCode.equals(countryCode, ignoreCase = true) &&
                    (groupingName.isEmpty() || it.groupingName.localizedStandardContains(groupingName))
            }
            .take(limit)
            .toList()
    }

    fun findDistrictsByState(
        stateName: String,
        countryCode: String,
        limit: Int = 30
    ): List<DistrictEntity> {
        return allDistricts.asSequence()
            .filter {
                it.countryCode.equals(countryCode, ignoreCase = true) &&
                    (stateName.isEmpty() || it.secondLevel.localizedStandardContains(stateName))
            }
            .take(limit)
            .toList()
    }

    fun findCityById(cityId: String, countryCode: String): DistrictEntity? {
        return allDistricts.firstOrNull {
            it.cityId == cityId && it.countryCode.equals(countryCode, ignoreCase = true)
        }
    }

    fun findCityByName(name: String): DistrictEntity? {
        return allDistricts.firstOrNull { it.thirdLevel == name }
    }
}
