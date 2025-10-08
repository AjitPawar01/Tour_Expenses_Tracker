package com.example.tourexpenses;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TripAdapter adapter;
    private DatabaseHelper db;
    private ExtendedFloatingActionButton fabAddTrip;
    private TextView tvActiveTrips, tvCompletedTrips, tvTripCount;
    private LinearLayout emptyState;
    private com.google.android.material.button.MaterialButton btnFilterAll, btnFilterOngoing, btnFilterCompleted;
    private Button btnCreateFirstTrip;

    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerTrips);
        fabAddTrip = findViewById(R.id.fabAddTrip);
        tvActiveTrips = findViewById(R.id.tvActiveTrips);
        tvCompletedTrips = findViewById(R.id.tvCompletedTrips);
        tvTripCount = findViewById(R.id.tvTripCount);
        emptyState = findViewById(R.id.emptyState);
        btnCreateFirstTrip = findViewById(R.id.btnCreateFirstTrip);

        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterOngoing = findViewById(R.id.btnFilterOngoing);
        btnFilterCompleted = findViewById(R.id.btnFilterCompleted);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set click listeners
        fabAddTrip.setOnClickListener(v -> showAddTripDialog());
        btnCreateFirstTrip.setOnClickListener(v -> showAddTripDialog());

        // Filter buttons
        btnFilterAll.setOnClickListener(v -> setFilter("ALL"));
        btnFilterOngoing.setOnClickListener(v -> setFilter("ONGOING"));
        btnFilterCompleted.setOnClickListener(v -> setFilter("COMPLETED"));

        loadTrips();
        updateStats();
    }

    private void setFilter(String filter) {
        currentFilter = filter;

        // Reset all buttons to inactive style
        setInactiveStyle(btnFilterAll);
        setInactiveStyle(btnFilterOngoing);
        setInactiveStyle(btnFilterCompleted);

        // Highlight selected button
        if (filter.equals("ALL")) {
            setActiveStyle(btnFilterAll, "#6366F1");
        } else if (filter.equals("ONGOING")) {
            setActiveStyle(btnFilterOngoing, "#6366F1");
        } else if (filter.equals("COMPLETED")) {
            setActiveStyle(btnFilterCompleted, "#10B981");
        }

        loadTrips();
    }

    private void setInactiveStyle(com.google.android.material.button.MaterialButton button) {
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#F3F4F6")));
        button.setTextColor(android.graphics.Color.parseColor("#6B7280"));
        button.setStrokeColor(android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#E5E7EB")));
        button.setStrokeWidth(2);
        button.setElevation(0);
    }

    private void setActiveStyle(com.google.android.material.button.MaterialButton button, String colorHex) {
        int color = android.graphics.Color.parseColor(colorHex);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        button.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
        button.setStrokeWidth(0);
        button.setElevation(8);
    }

    private void loadTrips() {
        List<DatabaseHelper.Trip> allTrips = db.getAllTrips();
        List<DatabaseHelper.Trip> filteredTrips = new ArrayList<>();

        // Filter trips based on current filter
        for (DatabaseHelper.Trip trip : allTrips) {
            if (currentFilter.equals("ALL")) {
                filteredTrips.add(trip);
            } else if (currentFilter.equals(trip.status)) {
                filteredTrips.add(trip);
            }
        }

        adapter = new TripAdapter(filteredTrips);
        recyclerView.setAdapter(adapter);

        // Show/hide empty state
        if (filteredTrips.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        // Update trip count
        tvTripCount.setText(filteredTrips.size() + (filteredTrips.size() == 1 ? " trip" : " trips"));
    }

    private void updateStats() {
        List<DatabaseHelper.Trip> allTrips = db.getAllTrips();
        int activeCount = 0;
        int completedCount = 0;

        for (DatabaseHelper.Trip trip : allTrips) {
            if ("ONGOING".equals(trip.status)) {
                activeCount++;
            } else if ("COMPLETED".equals(trip.status)) {
                completedCount++;
            }
        }

        tvActiveTrips.setText(String.valueOf(activeCount));
        tvCompletedTrips.setText(String.valueOf(completedCount));
    }

    private void showAddTripDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_trip, null);

        EditText etTripName = view.findViewById(R.id.etTripName);
        EditText etStartDate = view.findViewById(R.id.etStartDate);
        EditText etEndDate = view.findViewById(R.id.etEndDate);
        EditText etParticipants = view.findViewById(R.id.etParticipants);
        Button btnSave = view.findViewById(R.id.btnSaveTrip);
        Button btnCancel = view.findViewById(R.id.btnCancelTrip);

        final Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        etStartDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(MainActivity.this,
                    (view1, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        etStartDate.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        etEndDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(MainActivity.this,
                    (view1, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        etEndDate.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String name = etTripName.getText().toString().trim();
            String startDate = etStartDate.getText().toString().trim();
            String endDate = etEndDate.getText().toString().trim();
            String participantsText = etParticipants.getText().toString().trim();

            if (!startDate.isEmpty() && !endDate.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    Date start = sdf.parse(startDate);
                    Date end = sdf.parse(endDate);

                    if (end.before(start)) {
                        Toast.makeText(this, "End date cannot be before start date", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter trip name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (participantsText.isEmpty()) {
                Toast.makeText(this, "Please enter at least one participant", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] participantArray = participantsText.split(",");
            List<String> participants = new ArrayList<>();
            for (String participant : participantArray) {
                String trimmed = participant.trim();
                if (!trimmed.isEmpty()) {
                    participants.add(trimmed);
                }
            }

            if (participants.isEmpty()) {
                Toast.makeText(this, "Please enter valid participant names", Toast.LENGTH_SHORT).show();
                return;
            }

            long id = db.addTrip(name, startDate, endDate, participants);
            if (id > 0) {
                Toast.makeText(this, "Trip created with " + participants.size() + " members", Toast.LENGTH_SHORT).show();
                loadTrips();
                updateStats();
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTrips();
        updateStats();
    }

    // RecyclerView Adapter
    class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

        private List<DatabaseHelper.Trip> trips;

        public TripAdapter(List<DatabaseHelper.Trip> trips) {
            this.trips = trips;
        }

        @Override
        public TripViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_trip, parent, false);
            return new TripViewHolder(view);
        }

        @Override
        public void onBindViewHolder(TripViewHolder holder, int position) {
            DatabaseHelper.Trip trip = trips.get(position);
            holder.tvTripName.setText(trip.name);

            String dateRange = "";
            if (trip.startDate != null && !trip.startDate.isEmpty() &&
                    trip.endDate != null && !trip.endDate.isEmpty()) {
                dateRange = trip.startDate + " to " + trip.endDate;
            } else if (trip.startDate != null && !trip.startDate.isEmpty()) {
                dateRange = "From " + trip.startDate;
            } else {
                dateRange = "No dates set";
            }
            holder.tvTripDates.setText(dateRange);

            holder.tvTotalExpense.setText("₹" + String.format("%.2f", trip.totalExpense));
            holder.tvStatus.setText(trip.status);

            if ("COMPLETED".equals(trip.status)) {
                holder.tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                holder.tvStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                holder.btnMarkComplete.setText("Mark Ongoing");
                holder.btnMarkComplete.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_revert));
            } else {
                holder.tvStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                holder.tvStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
                holder.btnMarkComplete.setText("Complete");
                holder.btnMarkComplete.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_upload));
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, TripDetailActivity.class);
                intent.putExtra("TRIP_ID", trip.id);
                intent.putExtra("TRIP_NAME", trip.name);
                intent.putExtra("TRIP_STATUS", trip.status);
                startActivity(intent);
            });

            holder.btnMarkComplete.setOnClickListener(v -> {
                String newStatus = "ONGOING".equals(trip.status) ? "COMPLETED" : "ONGOING";
                db.updateTripStatus(trip.id, newStatus);
                loadTrips();
                updateStats();
                Toast.makeText(MainActivity.this, "Trip status updated", Toast.LENGTH_SHORT).show();
            });

            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Trip")
                        .setMessage("Are you sure? All expenses and participants will be deleted.")
                        .setPositiveButton("Delete", (d, w) -> {
                            db.deleteTrip(trip.id);
                            loadTrips();
                            updateStats();
                            Toast.makeText(MainActivity.this, "Trip deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return trips.size();
        }

        class TripViewHolder extends RecyclerView.ViewHolder {
            TextView tvTripName, tvTripDates, tvTotalExpense, tvStatus;
            com.google.android.material.button.MaterialButton btnMarkComplete, btnDelete;

            public TripViewHolder(View itemView) {
                super(itemView);
                tvTripName = itemView.findViewById(R.id.tvTripName);
                tvTripDates = itemView.findViewById(R.id.tvTripDates);
                tvTotalExpense = itemView.findViewById(R.id.tvTotalExpense);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                btnMarkComplete = itemView.findViewById(R.id.btnMarkComplete);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}