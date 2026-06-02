package com.example.trip_advisor.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.trip_advisor.R
import com.example.trip_advisor.data.TripDBHelper
import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.example.trip_advisor.data.TripImage
import com.example.trip_advisor.ui.addtrip.AddTripActivity
import java.io.File

class TripDetailFragment : Fragment() {

    companion object {
        private const val ARG_TRIP_ID = "trip_id"

        fun newInstance(tripId: Int): TripDetailFragment {
            return TripDetailFragment().apply {
                arguments = Bundle().apply { putInt(ARG_TRIP_ID, tripId) }
            }
        }
    }

    private lateinit var dbHelper: TripDBHelper
    private var tripId: Int = -1

    private lateinit var btnBack: ImageButton
    private lateinit var btnEdit: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var cardDetailImage: View
    private lateinit var ivDetailImage: ImageView
    private lateinit var tvDestination: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvDates: TextView
    private lateinit var tvDescription: TextView

    private lateinit var rvDetailThumbnails: RecyclerView
    private lateinit var thumbnailsAdapter: DetailThumbnailsAdapter
    private val imagesList = mutableListOf<TripImage>()
    private var selectedImagePosition = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trip_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripId = arguments?.getInt(ARG_TRIP_ID, -1) ?: -1
        dbHelper = TripDBHelper(requireContext())

        bindViews(view)
        setupThumbnailsRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadTripDetails()
    }

    private fun bindViews(view: View) {
        btnBack       = view.findViewById(R.id.btn_back)
        btnEdit       = view.findViewById(R.id.btn_edit)
        btnDelete     = view.findViewById(R.id.btn_delete)
        cardDetailImage = view.findViewById(R.id.card_detail_image)
        ivDetailImage = view.findViewById(R.id.iv_detail_image)
        tvDestination = view.findViewById(R.id.tv_detail_destination)
        tvRating      = view.findViewById(R.id.tv_detail_rating)
        tvTitle       = view.findViewById(R.id.tv_detail_title)
        tvDates       = view.findViewById(R.id.tv_detail_dates)
        tvDescription = view.findViewById(R.id.tv_detail_description)
        rvDetailThumbnails = view.findViewById(R.id.rv_detail_thumbnails)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnEdit.setOnClickListener {
            navigateToEdit()
        }

        btnDelete.setOnClickListener {
            confirmDelete()
        }
    }

    private fun loadTripDetails() {
        val trip = dbHelper.getTripById(tripId)
        if (trip == null) {
            Toast.makeText(requireContext(), "여행 기록을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        tvTitle.text = trip.title
        tvDestination.text = trip.destination.uppercase()
        tvDates.text = "📅 ${trip.startDate} ~ ${trip.endDate}"
        tvRating.text = "⭐ ${"%.1f".format(trip.rating)}"
        tvDescription.text = trip.description.ifBlank { "작성된 메모가 없습니다." }

        imagesList.clear()
        imagesList.addAll(dbHelper.getImagesForTrip(tripId))

        if (imagesList.isNotEmpty()) {
            val repIndex = imagesList.indexOfFirst { it.isRepresentative }
            selectedImagePosition = if (repIndex != -1) repIndex else 0
            thumbnailsAdapter.updateSelectedPosition(selectedImagePosition)

            displaySelectedImage(imagesList[selectedImagePosition].imagePath)

            if (imagesList.size > 1) {
                rvDetailThumbnails.visibility = View.VISIBLE
            } else {
                rvDetailThumbnails.visibility = View.GONE
            }
        } else {
            cardDetailImage.visibility = View.GONE
            rvDetailThumbnails.visibility = View.GONE
        }
    }

    private fun navigateToEdit() {
        val intent = Intent(requireContext(), AddTripActivity::class.java).apply {
            putExtra(AddTripActivity.EXTRA_TRIP_ID, tripId)
        }
        startActivity(intent)
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_msg)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                dbHelper.deleteTrip(tripId)
                Toast.makeText(requireContext(), R.string.msg_deleted, Toast.LENGTH_SHORT).show()
                // Return to home screen by popping the detail fragment
                parentFragmentManager.popBackStack()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun setupThumbnailsRecyclerView() {
        thumbnailsAdapter = DetailThumbnailsAdapter(
            images = imagesList,
            selectedPosition = selectedImagePosition,
            onThumbnailClick = { position ->
                selectedImagePosition = position
                thumbnailsAdapter.updateSelectedPosition(position)
                displaySelectedImage(imagesList[position].imagePath)
            }
        )
        rvDetailThumbnails.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvDetailThumbnails.adapter = thumbnailsAdapter
    }

    private fun displaySelectedImage(path: String) {
        if (File(path).exists()) {
            cardDetailImage.visibility = View.VISIBLE
            ivDetailImage.setImageURI(Uri.fromFile(File(path)))
        } else {
            cardDetailImage.visibility = View.GONE
        }
    }
}

class DetailThumbnailsAdapter(
    private val images: List<TripImage>,
    private var selectedPosition: Int,
    private val onThumbnailClick: (Int) -> Unit
) : RecyclerView.Adapter<DetailThumbnailsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.iv_thumbnail)
        val cardContainer: MaterialCardView = view.findViewById(R.id.card_thumbnail_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail_thumbnail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val image = images[position]
        holder.ivThumbnail.setImageURI(Uri.fromFile(File(image.imagePath)))

        if (position == selectedPosition) {
            holder.cardContainer.strokeWidth = dpToPx(holder.itemView.context, 2)
        } else {
            holder.cardContainer.strokeWidth = 0
        }

        holder.itemView.setOnClickListener {
            onThumbnailClick(position)
        }
    }

    override fun getItemCount(): Int = images.size

    fun updateSelectedPosition(newPosition: Int) {
        val oldPosition = selectedPosition
        selectedPosition = newPosition
        notifyItemChanged(oldPosition)
        notifyItemChanged(newPosition)
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
