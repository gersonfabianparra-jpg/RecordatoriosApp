package com.example.recordatorios.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local: fuente de verdad de la interfaz.
 * Se crea una única instancia por proceso (patrón singleton con doble verificación).
 */
@Database(entities = [RecordatorioEntity::class], version = 1, exportSchema = true)
abstract class RecordatoriosDatabase : RoomDatabase() {

    abstract fun recordatorioDao(): RecordatorioDao

    companion object {
        const val NOMBRE = "recordatorios.db"

        @Volatile
        private var instancia: RecordatoriosDatabase? = null

        fun obtener(context: Context): RecordatoriosDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecordatoriosDatabase::class.java,
                    NOMBRE
                ).build().also { instancia = it }
            }
    }
}
