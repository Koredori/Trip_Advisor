package com.example.trip_advisor.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trip_advisor.R
import com.example.trip_advisor.data.Trip
import com.example.trip_advisor.data.TripDBHelper
import com.example.trip_advisor.ui.addtrip.AddTripActivity
import com.example.trip_advisor.ui.detail.TripDetailFragment

class HomeFragment : Fragment() {

    private lateinit var dbHelper: TripDBHelper
    private lateinit var adapter: TripAdapter
    private lateinit var rvTrips: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvTripCount: TextView

    private var currentSortOrder = "default" // "default", "date", "rating"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        dbHelper     = TripDBHelper(requireContext())
        rvTrips      = view.findViewById(R.id.rv_trips)
        layoutEmpty  = view.findViewById(R.id.layout_empty)
        tvTripCount  = view.findViewById(R.id.tv_trip_count)

        adapter = TripAdapter(
            trips = mutableListOf(),
            onItemClick = { trip -> navigateToDetail(trip) },
            onEditClick = { trip -> navigateToEdit(trip.id) },
            onDeleteClick = { trip -> confirmDelete(trip) }
        )

        rvTrips.layoutManager = LinearLayoutManager(requireContext())
        rvTrips.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadTrips()
    }

    private fun loadTrips() {
        val trips = dbHelper.getAllTrips()
        val sortedTrips = when (currentSortOrder) {
            "date" -> trips.sortedByDescending { it.startDate }
            "rating" -> trips.sortedByDescending { it.rating }
            else -> trips
        }
        adapter.updateTrips(sortedTrips)

        tvTripCount.text = getString(R.string.home_trip_count, sortedTrips.size)

        if (sortedTrips.isEmpty()) {
            rvTrips.visibility     = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvTrips.visibility     = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_date -> {
                currentSortOrder = "date"
                loadTrips()
                true
            }
            R.id.action_sort_rating -> {
                currentSortOrder = "rating"
                loadTrips()
                true
            }
            R.id.action_delete_all -> {
                confirmDeleteAll()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmDeleteAll() {
        AlertDialog.Builder(requireContext())
            .setTitle("전체 삭제")
            .setMessage("모든 여행 기록을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                dbHelper.deleteAllTrips()
                loadTrips()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun confirmDelete(trip: Trip) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_msg)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                dbHelper.deleteTrip(trip.id)
                loadTrips()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun navigateToDetail(trip: Trip) {
        val fragment = TripDetailFragment.newInstance(trip.id)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToEdit(tripId: Int) {
        val intent = Intent(requireContext(), AddTripActivity::class.java).apply {
            putExtra(AddTripActivity.EXTRA_TRIP_ID, tripId)
        }
        startActivity(intent)
    }
}
