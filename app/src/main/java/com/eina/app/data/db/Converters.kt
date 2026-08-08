package com.eina.app.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = encodeStringListJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = decodeStringListJson(value)

    @TypeConverter
    fun fromWeightType(value: WeightType): String = value.name

    @TypeConverter
    fun toWeightType(value: String): WeightType = WeightType.valueOf(value)

    @TypeConverter
    fun fromSetType(value: SetType): String = value.name

    @TypeConverter
    fun toSetType(value: String): SetType = SetType.valueOf(value)

    @TypeConverter
    fun fromPlaylistType(value: PlaylistType?): String? = value?.name

    @TypeConverter
    fun toPlaylistType(value: String?): PlaylistType? = value?.let { PlaylistType.valueOf(it) }
}
