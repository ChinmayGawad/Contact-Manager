package com.example.contactmanager

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "contacts",
    indices = [Index(value = ["phoneNo"], unique = true)]
)
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneNo: String,
    val email: String,
    val imgId: Int = 0,
    val imageUri : String? = null
)
