package com.example.trip_advisor.ui.home

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trip_advisor.R
import com.example.trip_advisor.data.Trip
import java.io.File

class TripAdapter(
    private var trips: MutableList<Trip>,
    private val onItemClick: (Trip) -> Unit,
    private val onEditClick: (Trip) -> Unit,
    private val onDeleteClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDestination: TextView = itemView.findViewById(R.id.tv_destination)
        val tvTitle: TextView       = itemView.findViewById(R.id.tv_title)
        val tvDates: TextView       = itemView.findViewById(R.id.tv_dates)
        val tvRating: TextView      = itemView.findViewById(R.id.tv_rating)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        val viewAccent: View        = itemView.findViewById(R.id.view_accent)
        val cardTripImage: View     = itemView.findViewById(R.id.card_trip_image)
        val ivTripImage: ImageView  = itemView.findViewById(R.id.iv_trip_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_card, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = trips[position]

        holder.tvTitle.text       = trip.title
        holder.tvDestination.text = trip.destination.uppercase()
        holder.tvDates.text       = "📅 ${trip.startDate} ~ ${trip.endDate}"
        holder.tvRating.text      = "⭐ ${"%.1f".format(trip.rating)}"
        holder.tvDescription.text = trip.description.ifBlank { "메모 없음" }

        // Bind image if it exists
        if (!trip.imagePath.isNullOrBlank() && File(trip.imagePath).exists()) {
            holder.cardTripImage.visibility = View.VISIBLE
            holder.ivTripImage.setImageURI(Uri.fromFile(File(trip.imagePath)))
        } else {
            holder.cardTripImage.visibility = View.GONE
        }

        // Accent color cycling based on position
        val accentColors = listOf(
            0xFF0077B6.toInt(), // ocean blue
            0xFFFF6B35.toInt(), // sunset orange
            0xFF00B4D8.toInt(), // sky blue
            0xFF48CAE4.toInt(), // aqua
            0xFF023E8A.toInt()  // deep navy
        )
        holder.viewAccent.setBackgroundColor(accentColors[position % accentColors.size])

        holder.itemView.setOnClickListener { onItemClick(trip) }

        // Long click context menu for Edit/Delete
        holder.itemView.setOnCreateContextMenuListener { menu, v, menuInfo ->
            menu.setHeaderTitle("선택한 여행 기록")
            menu.add("수정").setOnMenuItemClickListener {
                onEditClick(trip)
                true
            }
            menu.add("삭제").setOnMenuItemClickListener {
                onDeleteClick(trip)
                true
            }
        }
    }

    override fun getItemCount(): Int = trips.size

    fun updateTrips(newTrips: List<Trip>) {
        trips.clear()
        trips.addAll(newTrips)
        notifyDataSetChanged()
    }
}
