package com.example.trip_advisor.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TripDBHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "trip_advisor.db"
        const val DATABASE_VERSION = 3

        // Table
        const val TABLE_TRIPS = "trips"

        // Columns
        const val COL_ID          = "id"
        const val COL_TITLE       = "title"
        const val COL_DESTINATION = "destination"
        const val COL_START_DATE  = "start_date"
        const val COL_END_DATE    = "end_date"
        const val COL_DESCRIPTION = "description"
        const val COL_RATING      = "rating"
        const val COL_IMAGE_PATH  = "image_path"
        const val COL_CREATED_AT  = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_TRIPS (
                $COL_ID          INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE       TEXT    NOT NULL,
                $COL_DESTINATION TEXT    NOT NULL,
                $COL_START_DATE  TEXT    NOT NULL,
                $COL_END_DATE    TEXT    NOT NULL,
                $COL_DESCRIPTION TEXT,
                $COL_RATING      REAL    DEFAULT 0,
                $COL_IMAGE_PATH  TEXT,
                $COL_CREATED_AT  INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTable)

        val createImagesTable = """
            CREATE TABLE trip_images (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                trip_id INTEGER NOT NULL,
                image_path TEXT NOT NULL,
                is_representative INTEGER DEFAULT 0,
                FOREIGN KEY(trip_id) REFERENCES $TABLE_TRIPS($COL_ID) ON DELETE CASCADE
            )
        """.trimIndent()
        db.execSQL(createImagesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            // Create trip_images table if not exists
            val createImagesTable = """
                CREATE TABLE IF NOT EXISTS trip_images (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    trip_id INTEGER NOT NULL,
                    image_path TEXT NOT NULL,
                    is_representative INTEGER DEFAULT 0,
                    FOREIGN KEY(trip_id) REFERENCES $TABLE_TRIPS($COL_ID) ON DELETE CASCADE
                )
            """.trimIndent()
            db.execSQL(createImagesTable)

            // Migrate existing single image paths
            try {
                val cursor = db.query(TABLE_TRIPS, arrayOf(COL_ID, COL_IMAGE_PATH), "$COL_IMAGE_PATH IS NOT NULL AND $COL_IMAGE_PATH != ''", null, null, null, null)
                cursor.use {
                    while (it.moveToNext()) {
                        val tripId = it.getInt(it.getColumnIndexOrThrow(COL_ID))
                        val imagePath = it.getString(it.getColumnIndexOrThrow(COL_IMAGE_PATH))
                        val values = ContentValues().apply {
                            put("trip_id", tripId)
                            put("image_path", imagePath)
                            put("is_representative", 1)
                        }
                        db.insert("trip_images", null, values)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            db.execSQL("DROP TABLE IF EXISTS trip_images")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_TRIPS")
            onCreate(db)
        }
    }

    // ─── CREATE ────────────────────────────────────────────────────────────────

    fun insertTrip(trip: Trip): Long {
        val db = writableDatabase
        val values = buildContentValues(trip)
        return db.insert(TABLE_TRIPS, null, values)
    }

    // ─── READ ──────────────────────────────────────────────────────────────────

    fun getAllTrips(): List<Trip> {
        val trips = mutableListOf<Trip>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRIPS, null, null, null, null, null,
            "$COL_CREATED_AT DESC"
        )
        cursor.use {
            while (it.moveToNext()) trips.add(cursorToTrip(it))
        }
        return trips
    }

    fun getTripById(id: Int): Trip? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRIPS, null,
            "$COL_ID = ?", arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) cursorToTrip(it) else null
        }
    }

    // ─── UPDATE ────────────────────────────────────────────────────────────────

    fun updateTrip(trip: Trip): Int {
        val db = writableDatabase
        val values = buildContentValues(trip)
        return db.update(TABLE_TRIPS, values, "$COL_ID = ?", arrayOf(trip.id.toString()))
    }

    // ─── DELETE ────────────────────────────────────────────────────────────────

    fun deleteTrip(id: Int): Int {
        val db = writableDatabase
        db.delete("trip_images", "trip_id = ?", arrayOf(id.toString()))
        return db.delete(TABLE_TRIPS, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun deleteAllTrips(): Int {
        val db = writableDatabase
        db.delete("trip_images", null, null)
        return db.delete(TABLE_TRIPS, null, null)
    }

    // ─── TRIP IMAGES OPERATIONS ────────────────────────────────────────────────

    fun getImagesForTrip(tripId: Int): List<TripImage> {
        val images = mutableListOf<TripImage>()
        val db = readableDatabase
        val cursor = db.query(
            "trip_images", null,
            "trip_id = ?", arrayOf(tripId.toString()),
            null, null, "id ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val path = it.getString(it.getColumnIndexOrThrow("image_path"))
                val isRep = it.getInt(it.getColumnIndexOrThrow("is_representative")) == 1
                images.add(TripImage(path, isRep))
            }
        }
        return images
    }

    fun saveImagesForTrip(tripId: Int, images: List<TripImage>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // 1. Delete existing images
            db.delete("trip_images", "trip_id = ?", arrayOf(tripId.toString()))

            // 2. Insert new images
            var repImagePath: String? = null
            for (image in images) {
                val values = ContentValues().apply {
                    put("trip_id", tripId)
                    put("image_path", image.imagePath)
                    put("is_representative", if (image.isRepresentative) 1 else 0)
                }
                db.insert("trip_images", null, values)
                if (image.isRepresentative) {
                    repImagePath = image.imagePath
                }
            }

            // If no image was representative but list is not empty, set first one as representative
            if (repImagePath == null && images.isNotEmpty()) {
                repImagePath = images[0].imagePath
                val updateValues = ContentValues().apply {
                    put("is_representative", 1)
                }
                db.update("trip_images", updateValues, "trip_id = ? AND image_path = ?", arrayOf(tripId.toString(), repImagePath))
                
                // Update local memory object too if needed (it will be saved to trips table)
                images[0].isRepresentative = true
            }

            // 3. Update representative image path in trips table
            val tripValues = ContentValues().apply {
                put(COL_IMAGE_PATH, repImagePath)
            }
            db.update(TABLE_TRIPS, tripValues, "$COL_ID = ?", arrayOf(tripId.toString()))

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────────

    private fun buildContentValues(trip: Trip): ContentValues {
        return ContentValues().apply {
            put(COL_TITLE,       trip.title)
            put(COL_DESTINATION, trip.destination)
            put(COL_START_DATE,  trip.startDate)
            put(COL_END_DATE,    trip.endDate)
            put(COL_DESCRIPTION, trip.description)
            put(COL_RATING,      trip.rating)
            put(COL_IMAGE_PATH,  trip.imagePath)
            put(COL_CREATED_AT,  trip.createdAt)
        }
    }

    private fun cursorToTrip(cursor: Cursor): Trip {
        return Trip(
            id          = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
            title       = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
            destination = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESTINATION)),
            startDate   = cursor.getString(cursor.getColumnIndexOrThrow(COL_START_DATE)),
            endDate     = cursor.getString(cursor.getColumnIndexOrThrow(COL_END_DATE)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)) ?: "",
            rating      = cursor.getFloat(cursor.getColumnIndexOrThrow(COL_RATING)),
            imagePath   = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_PATH)),
            createdAt   = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT))
        )
    }
}
