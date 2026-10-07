package app.reportamelo.locator.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.reportamelo.locator.data.DistrictEntity
import app.reportamelo.locator.data.CantonEntity

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
