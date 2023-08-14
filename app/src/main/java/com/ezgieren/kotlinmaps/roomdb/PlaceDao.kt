package com.ezgieren.kotlinmaps.roomdb

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.ezgieren.kotlinmaps.model.Place

@Dao
interface PlaceDao {
    @Insert
    fun insert(place: Place)

    @Delete
    fun delete(place: Place)

    @Query("SELECT * FROM Place")
    fun getAll() : List<Place>

}