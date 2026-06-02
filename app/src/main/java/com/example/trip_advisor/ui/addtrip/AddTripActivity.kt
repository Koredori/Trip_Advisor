package com.example.trip_advisor.ui.addtrip

import android.Manifest
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import android.content.Context
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trip_advisor.data.TripImage
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

    private lateinit var btnAddPhotos: MaterialButton
    private lateinit var rvSelectedImages: RecyclerView
    private lateinit var imagesAdapter: SelectedImagesAdapter
    private val selectedImagesList = mutableListOf<TripImage>()

    private var tripId: Int = NO_ID
    private var isEditMode = false

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            for (uri in uris) {
                val localPath = saveImageToInternalStorage(uri)
                if (localPath != null) {
                    val isFirst = selectedImagesList.isEmpty()
                    selectedImagesList.add(TripImage(imagePath = localPath, isRepresentative = isFirst))
                }
            }
            updateImagesUI()
        }
    }

    // 카메라로 찍은 사진의 임시 저장 URI
    private var cameraImageUri: Uri? = null

    // 카메라 촬영 결과 처리
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val uri = cameraImageUri ?: return@registerForActivityResult
            val localPath = saveImageToInternalStorage(uri)
            if (localPath != null) {
                val isFirst = selectedImagesList.isEmpty()
                selectedImagesList.add(TripImage(imagePath = localPath, isRepresentative = isFirst))
                updateImagesUI()
            } else {
                Toast.makeText(this, "사진 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 카메라 권한 요청 런처
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_add_trip)

        tripId = intent.getIntExtra(EXTRA_TRIP_ID, NO_ID)
        isEditMode = tripId != NO_ID

        dbHelper = TripDBHelper(this)

        bindViews()
        setupImagesRecyclerView()
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
        btnAddPhotos     = findViewById(R.id.btn_add_photos)
        rvSelectedImages = findViewById(R.id.rv_selected_images)
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
        cardSelectImage.setOnClickListener { showImageSourceDialog() }
        btnAddPhotos.setOnClickListener { showImageSourceDialog() }
    }

    /** 사진 추가 방법 선택 다이얼로그 (갤러리 / 카메라) */
    private fun showImageSourceDialog() {
        val options = arrayOf("📷  카메라로 촬영", "🖼️  갤러리에서 선택")
        AlertDialog.Builder(this)
            .setTitle("사진 추가")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> pickImagesLauncher.launch("image/*")
                }
            }
            .show()
    }

    /** 카메라 권한 확인 후 촬영 실행 */
    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> launchCamera()
            else -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /** 카메라 앱 실행 - 임시 파일 URI 생성 후 TakePicture 계약으로 실행 */
    private fun launchCamera() {
        val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: filesDir
        val tempFile = File(picturesDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            tempFile
        )
        cameraImageUri = uri
        takePictureLauncher.launch(uri)
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

        selectedImagesList.clear()
        selectedImagesList.addAll(dbHelper.getImagesForTrip(tripId))
        updateImagesUI()
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

        val repImage = selectedImagesList.find { it.isRepresentative } ?: selectedImagesList.firstOrNull()
        if (repImage != null) {
            repImage.isRepresentative = true
        }

        val trip = Trip(
            id          = if (isEditMode) tripId else 0,
            title       = etTitle.text.toString().trim(),
            destination = etDestination.text.toString().trim(),
            startDate   = etStartDate.text.toString(),
            endDate     = etEndDate.text.toString(),
            description = etDescription.text.toString().trim(),
            rating      = ratingBar.rating,
            imagePath   = repImage?.imagePath,
            createdAt   = System.currentTimeMillis()
        )

        val savedTripId = if (isEditMode) {
            dbHelper.updateTrip(trip)
            tripId.toLong()
        } else {
            dbHelper.insertTrip(trip)
        }

        if (savedTripId > 0) {
            dbHelper.saveImagesForTrip(savedTripId.toInt(), selectedImagesList)
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

    private fun setupImagesRecyclerView() {
        imagesAdapter = SelectedImagesAdapter(
            images = selectedImagesList,
            onImageClick = { position ->
                for (i in selectedImagesList.indices) {
                    selectedImagesList[i].isRepresentative = (i == position)
                }
                imagesAdapter.notifyDataSetChanged()
            },
            onDeleteClick = { position ->
                val wasRepresentative = selectedImagesList[position].isRepresentative
                selectedImagesList.removeAt(position)
                if (wasRepresentative && selectedImagesList.isNotEmpty()) {
                    selectedImagesList[0].isRepresentative = true
                }
                updateImagesUI()
            }
        )
        rvSelectedImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvSelectedImages.adapter = imagesAdapter
    }

    private fun updateImagesUI() {
        if (selectedImagesList.isEmpty()) {
            cardSelectImage.visibility = View.VISIBLE
            rvSelectedImages.visibility = View.GONE
            btnAddPhotos.visibility = View.GONE
        } else {
            cardSelectImage.visibility = View.GONE
            rvSelectedImages.visibility = View.VISIBLE
            btnAddPhotos.visibility = View.VISIBLE
        }
        imagesAdapter.notifyDataSetChanged()
    }
}

class SelectedImagesAdapter(
    private val images: List<TripImage>,
    private val onImageClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<SelectedImagesAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.iv_image)
        val tvBadge: TextView = view.findViewById(R.id.tv_representative_badge)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
        val cardContainer: MaterialCardView = view.findViewById(R.id.card_image_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_selected_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val image = images[position]
        holder.ivImage.setImageURI(Uri.fromFile(File(image.imagePath)))
        
        if (image.isRepresentative) {
            holder.tvBadge.visibility = View.VISIBLE
            holder.cardContainer.strokeWidth = dpToPx(holder.itemView.context, 2)
        } else {
            holder.tvBadge.visibility = View.GONE
            holder.cardContainer.strokeWidth = 0
        }

        holder.cardContainer.setOnClickListener { onImageClick(position) }
        holder.btnDelete.setOnClickListener { onDeleteClick(position) }
    }

    override fun getItemCount(): Int = images.size

    private fun dpToPx(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
