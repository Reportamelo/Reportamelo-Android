package app.reportamelo.locator.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [District::class, Canton::class], version = 1, exportSchema = false)
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
                .createFromAsset("databases/districts.db")
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
