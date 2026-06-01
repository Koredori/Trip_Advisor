package com.example.trip_advisor.ui.addtrip

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.example.trip_advisor.R
import com.example.trip_advisor.data.Trip
import com.example.trip_advisor.data.TripDBHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale
import kotlin.concurrent.thread

class AddTripActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRIP_ID = "trip_id"
        private const val NO_ID = -1
    }

    private lateinit var dbHelper: TripDBHelper

    private lateinit var tvFormTitle: TextView
    private lateinit var tilTitle: TextInputLayout
    private lateinit var etTitle: TextInputEditText
    private lateinit var tilDestination: TextInputLayout
    private lateinit var etDestination: TextInputEditText
    private lateinit var tilStartDate: TextInputLayout
    private lateinit var etStartDate: TextInputEditText
    private lateinit var tilEndDate: TextInputLayout
    private lateinit var etEndDate: TextInputEditText
    private lateinit var tilDescription: TextInputLayout
    private lateinit var etDescription: TextInputEditText
    private lateinit var ratingBar: RatingBar
    private lateinit var tvRatingValue: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnDelete: MaterialButton

    private lateinit var cardSelectImage: MaterialCardView
    private lateinit var layoutNoImage: LinearLayout
    private lateinit var layoutHasImage: FrameLayout
    private lateinit var ivSelectedImage: ImageView
    private lateinit var btnRemoveImage: ImageButton

    private var selectedImagePath: String? = null
    private var tripId: Int = NO_ID
    private var isEditMode = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val localPath = saveImageToInternalStorage(uri)
            if (localPath != null) {
                selectedImagePath = localPath
                showSelectedImage(localPath)
            } else {
                Toast.makeText(this, "사진을 저장하지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_add_trip)

        tripId = intent.getIntExtra(EXTRA_TRIP_ID, NO_ID)
        isEditMode = tripId != NO_ID

        dbHelper = TripDBHelper(this)

        bindViews()
        setupListeners()

        if (isEditMode) {
            loadTripForEdit()
        }
    }

    private fun bindViews() {
        tvFormTitle     = findViewById(R.id.tv_form_title)
        tilTitle        = findViewById(R.id.til_title)
        etTitle         = findViewById(R.id.et_title)
        tilDestination  = findViewById(R.id.til_destination)
        etDestination   = findViewById(R.id.et_destination)
        tilStartDate    = findViewById(R.id.til_start_date)
        etStartDate     = findViewById(R.id.et_start_date)
        tilEndDate      = findViewById(R.id.til_end_date)
        etEndDate       = findViewById(R.id.et_end_date)
        tilDescription  = findViewById(R.id.til_description)
        etDescription   = findViewById(R.id.et_description)
        ratingBar       = findViewById(R.id.rating_bar)
        tvRatingValue   = findViewById(R.id.tv_rating_value)
        btnSave         = findViewById(R.id.btn_save)
        btnDelete       = findViewById(R.id.btn_delete)
        cardSelectImage = findViewById(R.id.card_select_image)
        layoutNoImage   = findViewById(R.id.layout_no_image)
        layoutHasImage  = findViewById(R.id.layout_has_image)
        ivSelectedImage = findViewById(R.id.iv_selected_image)
        btnRemoveImage  = findViewById(R.id.btn_remove_image)
    }

    private fun setupListeners() {
        etStartDate.setOnClickListener { showDatePicker(etStartDate) }
        etEndDate.setOnClickListener { showDatePicker(etEndDate) }
        etDestination.setOnClickListener { showMapPickerDialog() }

        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            tvRatingValue.text = "%.1f".format(rating)
        }

        btnSave.setOnClickListener { saveTrip() }
        btnDelete.setOnClickListener { confirmDelete() }
        cardSelectImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        btnRemoveImage.setOnClickListener { clearSelectedImage() }
    }

    private fun loadTripForEdit() {
        val trip = dbHelper.getTripById(tripId) ?: return

        tvFormTitle.text      = getString(R.string.edit_trip_title)
        etTitle.setText(trip.title)
        etDestination.setText(trip.destination)
        etStartDate.setText(trip.startDate)
        etEndDate.setText(trip.endDate)
        etDescription.setText(trip.description)
        ratingBar.rating      = trip.rating
        tvRatingValue.text    = "%.1f".format(trip.rating)
        btnDelete.visibility  = View.VISIBLE

        selectedImagePath     = trip.imagePath
        if (!selectedImagePath.isNullOrBlank()) {
            showSelectedImage(selectedImagePath!!)
        } else {
            clearSelectedImage()
        }
    }

    private fun showDatePicker(targetField: TextInputEditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val dateStr = "%04d.%02d.%02d".format(year, month + 1, day)
                targetField.setText(dateStr)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveTrip() {
        if (!validateForm()) return

        val trip = Trip(
            id          = if (isEditMode) tripId else 0,
            title       = etTitle.text.toString().trim(),
            destination = etDestination.text.toString().trim(),
            startDate   = etStartDate.text.toString(),
            endDate     = etEndDate.text.toString(),
            description = etDescription.text.toString().trim(),
            rating      = ratingBar.rating,
            imagePath   = selectedImagePath,
            createdAt   = System.currentTimeMillis()
        )

        if (isEditMode) {
            dbHelper.updateTrip(trip)
        } else {
            dbHelper.insertTrip(trip)
        }

        Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_msg)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                dbHelper.deleteTrip(tripId)
                Toast.makeText(this, R.string.msg_deleted, Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun validateForm(): Boolean {
        var valid = true

        if (etTitle.text.isNullOrBlank()) {
            tilTitle.error = getString(R.string.error_required)
            valid = false
        } else {
            tilTitle.error = null
        }

        if (etDestination.text.isNullOrBlank()) {
            tilDestination.error = getString(R.string.error_required)
            valid = false
        } else {
            tilDestination.error = null
        }

        if (etStartDate.text.isNullOrBlank()) {
            tilStartDate.error = getString(R.string.error_date)
            valid = false
        } else {
            tilStartDate.error = null
        }

        if (etEndDate.text.isNullOrBlank()) {
            tilEndDate.error = getString(R.string.error_date)
            valid = false
        } else {
            tilEndDate.error = null
        }

        return valid
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "trip_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showSelectedImage(path: String) {
        ivSelectedImage.setImageURI(Uri.fromFile(File(path)))
        layoutNoImage.visibility = View.GONE
        layoutHasImage.visibility = View.VISIBLE
    }

    private fun clearSelectedImage() {
        selectedImagePath = null
        ivSelectedImage.setImageDrawable(null)
        layoutNoImage.visibility = View.VISIBLE
        layoutHasImage.visibility = View.GONE
    }

    private fun showMapPickerDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_map_picker)
        
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        val searchView: SearchView = dialog.findViewById(R.id.search_view)
        val mapView: MapView = dialog.findViewById(R.id.map_view)
        val tvAddress: TextView = dialog.findViewById(R.id.tv_selected_address)
        val btnCancel: MaterialButton = dialog.findViewById(R.id.btn_dialog_cancel)
        val btnConfirm: MaterialButton = dialog.findViewById(R.id.btn_dialog_confirm)

        mapView.onCreate(null)
        mapView.onResume()

        var currentLatLng = LatLng(37.5665, 126.9780) // Default Seoul
        var currentAddress = ""
        var googleMap: GoogleMap? = null
        var marker: Marker? = null

        mapView.getMapAsync { map ->
            googleMap = map
            map.uiSettings.isZoomControlsEnabled = true
            
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            
            marker = map.addMarker(
                MarkerOptions()
                    .position(currentLatLng)
                    .draggable(true)
            )

            getAddressFromLocation(currentLatLng) { address ->
                currentAddress = address
                tvAddress.text = address
            }

            map.setOnMapClickListener { latLng ->
                currentLatLng = latLng
                marker?.position = latLng
                tvAddress.text = "주소 검색 중..."
                getAddressFromLocation(latLng) { address ->
                    currentAddress = address
                    tvAddress.text = address
                }
            }

            map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDragStart(m: Marker) {}
                override fun onMarkerDrag(m: Marker) {}
                override fun onMarkerDragEnd(m: Marker) {
                    currentLatLng = m.position
                    tvAddress.text = "주소 검색 중..."
                    getAddressFromLocation(m.position) { address ->
                        currentAddress = address
                        tvAddress.text = address
                    }
                }
            })
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    tvAddress.text = "위치 찾는 중..."
                    getLocationFromAddressName(query) { latLng, address ->
                        if (latLng != null && address != null) {
                            currentLatLng = latLng
                            currentAddress = address
                            tvAddress.text = address
                            
                            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                            marker?.position = latLng
                        } else {
                            tvAddress.text = "검색 결과를 찾을 수 없습니다."
                            Toast.makeText(this@AddTripActivity, "검색 결과를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            if (currentAddress.isNotBlank() && currentAddress != "주소를 찾을 수 없습니다." && !currentAddress.startsWith("주소 파싱 오류")) {
                etDestination.setText(currentAddress)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "유효한 여행지를 선택해 주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setOnDismissListener {
            mapView.onDestroy()
        }

        dialog.show()
    }

    private fun getAddressFromLocation(latLng: LatLng, callback: (String) -> Unit) {
        val geocoder = Geocoder(this, Locale.KOREA)
        thread {
            try {
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                val addressText = if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val fullAddress = address.getAddressLine(0) ?: ""
                    if (fullAddress.startsWith("대한민국 ")) {
                        fullAddress.substring("대한민국 ".length)
                    } else {
                        fullAddress
                    }
                } else {
                    "주소를 찾을 수 없습니다."
                }
                Handler(Looper.getMainLooper()).post {
                    callback(addressText)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    callback("주소 파싱 오류: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun getLocationFromAddressName(query: String, callback: (LatLng?, String?) -> Unit) {
        val geocoder = Geocoder(this, Locale.KOREA)
        thread {
            try {
                val addresses = geocoder.getFromLocationName(query, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val latLng = LatLng(address.latitude, address.longitude)
                    val fullAddress = address.getAddressLine(0) ?: query
                    val trimmedAddress = if (fullAddress.startsWith("대한민국 ")) {
                        fullAddress.substring("대한민국 ".length)
                    } else {
                        fullAddress
                    }
                    Handler(Looper.getMainLooper()).post {
                        callback(latLng, trimmedAddress)
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        callback(null, null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    callback(null, null)
                }
            }
        }
    }
}
