package com.eina.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Nome inglese del dataset: e' la chiave con cui ExerciseSeeder riconosce l'esercizio
    // fra un catalogo e l'altro, oltre al fallback quando manca la traduzione.
    val name: String,
    val nameIt: String? = null,
    val nameFr: String? = null,
    // Testo originale del dataset, in inglese: e' anche il fallback quando manca la
    // traduzione nella lingua attiva.
    val description: String,
    val descriptionIt: String? = null,
    val descriptionFr: String? = null,
    // Vuota per gli esercizi di libreria: la loro spiegazione dipende solo da weightType
    // e vive in strings.xml, cosi' segue la lingua dell'app. La riempie solo chi crea un
    // esercizio custom e vuole scriversi una nota propria.
    val loggingInstructions: String,
    val weightType: WeightType,
    val muscleGroupsPrimary: List<String>,     // TypeConverter: JSON string
    val muscleGroupsSecondary: List<String>,   // TypeConverter: JSON string
    val equipment: String? = null,
    val mediaUri: String? = null,              // GIF locale o bundled
    val isCustom: Boolean = false,
    val source: String? = null
)
