package com.example.tourexpenses;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TripDetailActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private int tripId;
    private String tripName;
    private String tripStatus;
    private RecyclerView recyclerView;
    private DateGroupAdapter adapter;
    private TextView tvTripTotal, tvMyShare, tvExpenseCount;
    private FloatingActionButton fabAddExpense;
    private LinearLayout emptyState;
    private com.google.android.material.button.MaterialButton btnViewSettlement;
    private String selectedCategory = "Food";
    private boolean isSelfPaid = false;
    private String selectedExpenseDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        db = new DatabaseHelper(this);
        tripId = getIntent().getIntExtra("TRIP_ID", -1);
        tripName = getIntent().getStringExtra("TRIP_NAME");
        tripStatus = getIntent().getStringExtra("TRIP_STATUS");

        if (tripId == -1) {
            Toast.makeText(this, "Error loading trip", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadExpenses();
        updateTotals();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerExpenses);
        tvTripTotal = findViewById(R.id.tvTripTotal);
        tvMyShare = findViewById(R.id.tvMyShare);
        tvExpenseCount = findViewById(R.id.tvExpenseCount);
        fabAddExpense = findViewById(R.id.fabAddExpense);
        emptyState = findViewById(R.id.emptyState);
        btnViewSettlement = findViewById(R.id.btnViewSettlement);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fabAddExpense.setOnClickListener(v -> showAddExpenseDialog());
        btnViewSettlement.setOnClickListener(v -> showSettlementReport());
    }

    private void showAddExpenseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null);

        EditText etAmount = view.findViewById(R.id.etAmount);
        EditText etDescription = view.findViewById(R.id.etDescription);
        EditText etExpenseDate = view.findViewById(R.id.etExpenseDate);
        Spinner spinnerPaidBy = view.findViewById(R.id.spinnerPaidBy);
        Button btnSave = view.findViewById(R.id.btnSaveExpense);
        Button btnCancel = view.findViewById(R.id.btnCancelExpense);
        com.google.android.material.button.MaterialButton btnSelfPaid = view.findViewById(R.id.btnSelfPaid);

        // Category selection views
        LinearLayout categoryFood = view.findViewById(R.id.categoryFood);
        LinearLayout categoryTransport = view.findViewById(R.id.categoryTransport);
        LinearLayout categoryHotel = view.findViewById(R.id.categoryHotel);
        LinearLayout categoryFun = view.findViewById(R.id.categoryFun);
        LinearLayout categoryShopping = view.findViewById(R.id.categoryShopping);
        LinearLayout categoryFuel = view.findViewById(R.id.categoryFuel);
        LinearLayout categoryOther = view.findViewById(R.id.categoryOther);

        // Initialize date picker
        final Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        selectedExpenseDate = dateFormat.format(calendar.getTime()); // Default to today
        etExpenseDate.setText(selectedExpenseDate);

        etExpenseDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(TripDetailActivity.this,
                    (view1, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        selectedExpenseDate = dateFormat.format(calendar.getTime());
                        etExpenseDate.setText(selectedExpenseDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        // Setup participants spinner
        List<String> participants = db.getTripParticipants(tripId);
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this,
                R.layout.spinner_item, participants);
        adapterSpinner.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerPaidBy.setAdapter(adapterSpinner);

        // Reset category selection
        selectedCategory = "Food";
        isSelfPaid = false;

        // Category click listeners
        View.OnClickListener categoryClickListener = v -> {
            // Reset all categories
            resetCategoryStyle(categoryFood);
            resetCategoryStyle(categoryTransport);
            resetCategoryStyle(categoryHotel);
            resetCategoryStyle(categoryFun);
            resetCategoryStyle(categoryShopping);
            resetCategoryStyle(categoryFuel);
            resetCategoryStyle(categoryOther);

            // Highlight selected
            v.setBackgroundResource(R.drawable.category_selector_selected);

            if (v == categoryFood) selectedCategory = "Food";
            else if (v == categoryTransport) selectedCategory = "Transport";
            else if (v == categoryHotel) selectedCategory = "Hotel";
            else if (v == categoryFun) selectedCategory = "Fun";
            else if (v == categoryShopping) selectedCategory = "Shopping";
            else if (v == categoryFuel) selectedCategory = "Fuel";
            else if (v == categoryOther) selectedCategory = "Other";
        };

        categoryFood.setOnClickListener(categoryClickListener);
        categoryTransport.setOnClickListener(categoryClickListener);
        categoryHotel.setOnClickListener(categoryClickListener);
        categoryFun.setOnClickListener(categoryClickListener);
        categoryShopping.setOnClickListener(categoryClickListener);
        categoryFuel.setOnClickListener(categoryClickListener);
        categoryOther.setOnClickListener(categoryClickListener);

        // Set Food as default selected
        categoryFood.setBackgroundResource(R.drawable.category_selector_selected);

        // Self-paid button
        btnSelfPaid.setOnClickListener(v -> {
            isSelfPaid = !isSelfPaid;
            if (isSelfPaid) {
                btnSelfPaid.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#10B981")));
                btnSelfPaid.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                btnSelfPaid.setIconTint(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#FFFFFF")));
                spinnerPaidBy.setEnabled(false);
                spinnerPaidBy.setAlpha(0.5f);
            } else {
                btnSelfPaid.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#FFFFFF")));
                btnSelfPaid.setTextColor(android.graphics.Color.parseColor("#1F2937"));
                btnSelfPaid.setIconTint(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#1F2937")));
                spinnerPaidBy.setEnabled(true);
                spinnerPaidBy.setAlpha(1.0f);
            }
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            if (description.isEmpty()) {
                Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedExpenseDate.isEmpty()) {
                Toast.makeText(this, "Please select expense date", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                String paidBy = isSelfPaid ? "SELF_PAID" : spinnerPaidBy.getSelectedItem().toString();

                // Convert selected date to database format (yyyy-MM-dd HH:mm:ss)
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                String dbDateTime;
                try {
                    Date date = displayFormat.parse(selectedExpenseDate);
                    // Set time to current time to maintain chronological order within the day
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    cal.set(Calendar.HOUR_OF_DAY, Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
                    cal.set(Calendar.MINUTE, Calendar.getInstance().get(Calendar.MINUTE));
                    cal.set(Calendar.SECOND, Calendar.getInstance().get(Calendar.SECOND));
                    dbDateTime = dbFormat.format(cal.getTime());
                } catch (ParseException e) {
                    e.printStackTrace();
                    dbDateTime = dbFormat.format(new Date());
                }

                long id = db.addExpense(tripId, paidBy, amount, selectedCategory, description, dbDateTime);

                if (id > 0) {
                    // Show success message with date indicator
                    String todayDate = dateFormat.format(new Date());
                    String message = selectedExpenseDate.equals(todayDate)
                            ? "Expense added successfully"
                            : "Expense added to " + selectedExpenseDate;
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                    loadExpenses();
                    updateTotals();
                    dialog.dismiss();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void resetCategoryStyle(LinearLayout category) {
        category.setBackgroundResource(R.drawable.category_selector);
    }

    private void loadExpenses() {
        List<DatabaseHelper.Expense> expenses = db.getTripExpenses(tripId);

        // Group expenses by date
        Map<String, DateGroup> dateGroups = new LinkedHashMap<>();
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        for (DatabaseHelper.Expense expense : expenses) {
            try {
                Date date = inputFormat.parse(expense.date);
                String dateKey = outputFormat.format(date);

                if (!dateGroups.containsKey(dateKey)) {
                    DateGroup group = new DateGroup();
                    group.date = dateKey;
                    group.expenses = new ArrayList<>();
                    group.totalAmount = 0;
                    dateGroups.put(dateKey, group);
                }

                DateGroup group = dateGroups.get(dateKey);
                group.expenses.add(expense);
                group.totalAmount += expense.amount;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<DateGroup> groupList = new ArrayList<>(dateGroups.values());
        adapter = new DateGroupAdapter(groupList);
        recyclerView.setAdapter(adapter);

        if (expenses.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        tvExpenseCount.setText(expenses.size() + (expenses.size() == 1 ? " item" : " items"));
    }

    private void updateTotals() {
        double total = db.getTripTotalExpense(tripId);
        tvTripTotal.setText("₹" + String.format("%.2f", total));

        List<String> participants = db.getTripParticipants(tripId);
        if (!participants.isEmpty()) {
            double perPerson = total / participants.size();
            tvMyShare.setText("₹" + String.format("%.2f", perPerson) + " per person");
        } else {
            tvMyShare.setText("No participants");
        }
    }

    private void showSettlementReport() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settlement, null);

        LinearLayout settlementContent = view.findViewById(R.id.settlementContent);
        Button btnClose = view.findViewById(R.id.btnCloseSettlement);

        // Get all data
        Map<String, Double> balances = db.getParticipantBalances(tripId);
        Map<String, Double> spending = db.getParticipantTotalSpending(tripId);
        Map<String, Double> dayWiseExpenses = db.getDayWiseExpenses(tripId);
        double totalExpense = db.getTripTotalExpense(tripId);
        List<String> participants = db.getTripParticipants(tripId);

        // 1. TRIP OVERVIEW SECTION
        addSectionTitle(settlementContent, "📊 Trip Overview");

        View overviewCard = createInfoCard(
                "Total Trip Expense",
                "₹" + String.format("%.2f", totalExpense),
                "#6366F1"
        );
        settlementContent.addView(overviewCard);

        if (!participants.isEmpty()) {
            double perPerson = totalExpense / participants.size();
            View perPersonCard = createInfoCard(
                    "Per Person Share",
                    "₹" + String.format("%.2f", perPerson),
                    "#10B981"
            );
            settlementContent.addView(perPersonCard);
        }

        addDivider(settlementContent);

        // 2. DAY-WISE EXPENSES SECTION
        if (!dayWiseExpenses.isEmpty()) {
            addSectionTitle(settlementContent, "📅 Day-wise Expenses");

            for (Map.Entry<String, Double> entry : dayWiseExpenses.entrySet()) {
                View dayView = createDayWiseView(entry.getKey(), entry.getValue());
                settlementContent.addView(dayView);
            }

            addDivider(settlementContent);
        }

        // 3. INDIVIDUAL SPENDING SECTION
        addSectionTitle(settlementContent, "💳 Individual Spending");

        for (Map.Entry<String, Double> entry : spending.entrySet()) {
            String name = entry.getKey();
            double spent = entry.getValue();

            View spendingView = createSpendingView(name, spent, totalExpense);
            settlementContent.addView(spendingView);
        }

        addDivider(settlementContent);

        // 4. SETTLEMENT BALANCES SECTION
        addSectionTitle(settlementContent, "⚖️ Who Owes Whom");

        TextView balanceInfo = new TextView(this);
        balanceInfo.setText("Based on equal split among all participants");
        balanceInfo.setTextSize(11);
        balanceInfo.setTextColor(android.graphics.Color.parseColor("#6B7280"));
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.setMargins(0, 0, 0, 12);
        balanceInfo.setLayoutParams(infoParams);
        settlementContent.addView(balanceInfo);

        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            String name = entry.getKey();
            double balance = entry.getValue();
            double spent = spending.get(name) != null ? spending.get(name) : 0;

            // Create custom balance view instead of using item_settlement_entry
            LinearLayout balanceCard = new LinearLayout(this);
            balanceCard.setOrientation(LinearLayout.HORIZONTAL);
            balanceCard.setBackgroundResource(R.drawable.input_background);
            int padding = (int) (14 * getResources().getDisplayMetrics().density);
            balanceCard.setPadding(padding, padding, padding, padding);
            balanceCard.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 8);
            balanceCard.setLayoutParams(cardParams);

            // Person icon
            TextView iconView = new TextView(this);
            iconView.setText("👤");
            iconView.setTextSize(20);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    (int) (40 * getResources().getDisplayMetrics().density),
                    (int) (40 * getResources().getDisplayMetrics().density)
            );
            iconParams.setMargins(0, 0, 12, 0);
            iconView.setLayoutParams(iconParams);
            iconView.setGravity(android.view.Gravity.CENTER);
            balanceCard.addView(iconView);

            // Name and paid info
            LinearLayout infoLayout = new LinearLayout(this);
            infoLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoLayoutParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            );
            infoLayout.setLayoutParams(infoLayoutParams);

            TextView nameView = new TextView(this);
            nameView.setText(name);
            nameView.setTextSize(14);
            nameView.setTypeface(null, android.graphics.Typeface.BOLD);
            nameView.setTextColor(android.graphics.Color.parseColor("#1F2937"));
            infoLayout.addView(nameView);

            TextView paidView = new TextView(this);
            paidView.setText("Paid: ₹" + String.format("%.2f", spent));
            paidView.setTextSize(11);
            paidView.setTextColor(android.graphics.Color.parseColor("#6B7280"));
            LinearLayout.LayoutParams paidParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            paidParams.setMargins(0, 2, 0, 0);
            paidView.setLayoutParams(paidParams);
            infoLayout.addView(paidView);

            balanceCard.addView(infoLayout);

            // Balance amount
            TextView balanceView = new TextView(this);
            balanceView.setTextSize(14);
            balanceView.setTypeface(null, android.graphics.Typeface.BOLD);

            if (balance > 0) {
                balanceView.setText("Gets ₹" + String.format("%.2f", Math.abs(balance)));
                balanceView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else if (balance < 0) {
                balanceView.setText("Owes ₹" + String.format("%.2f", Math.abs(balance)));
                balanceView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                balanceView.setText("Settled ✓");
                balanceView.setTextColor(android.graphics.Color.parseColor("#6B7280"));
            }

            balanceCard.addView(balanceView);
            settlementContent.addView(balanceCard);
        }

        builder.setView(view);
        AlertDialog dialog = builder.create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void addSectionTitle(LinearLayout container, String title) {
        TextView sectionTitle = new TextView(this);
        sectionTitle.setText(title);
        sectionTitle.setTextSize(16);
        sectionTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        sectionTitle.setTextColor(android.graphics.Color.parseColor("#1F2937"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 20, 0, 12);
        sectionTitle.setLayoutParams(params);
        container.addView(sectionTitle);
    }

    private void addDivider(LinearLayout container) {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
        );
        params.setMargins(0, 20, 0, 0);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(android.graphics.Color.parseColor("#E5E7EB"));
        container.addView(divider);
    }

    private View createInfoCard(String label, String value, String colorHex) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.input_background);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        card.setPadding(padding, padding, padding, padding);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
        card.setLayoutParams(params);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTextColor(android.graphics.Color.parseColor("#6B7280"));
        card.addView(labelView);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(22);
        valueView.setTypeface(null, android.graphics.Typeface.BOLD);
        valueView.setTextColor(android.graphics.Color.parseColor(colorHex));
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        valueParams.setMargins(0, 4, 0, 0);
        valueView.setLayoutParams(valueParams);
        card.addView(valueView);

        return card;
    }

    private View createDayWiseView(String date, double amount) {
        LinearLayout dayView = new LinearLayout(this);
        dayView.setOrientation(LinearLayout.HORIZONTAL);
        dayView.setBackgroundResource(R.drawable.input_background);
        int padding = (int) (12 * getResources().getDisplayMetrics().density);
        dayView.setPadding(padding, padding, padding, padding);
        dayView.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
        dayView.setLayoutParams(params);

        // Date icon
        TextView iconView = new TextView(this);
        iconView.setText("📅");
        iconView.setTextSize(18);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        iconParams.setMargins(0, 0, 12, 0);
        iconView.setLayoutParams(iconParams);
        dayView.addView(iconView);

        // Date text
        TextView dateView = new TextView(this);
        dateView.setText(date);
        dateView.setTextSize(14);
        dateView.setTypeface(null, android.graphics.Typeface.BOLD);
        dateView.setTextColor(android.graphics.Color.parseColor("#1F2937"));
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        dateView.setLayoutParams(dateParams);
        dayView.addView(dateView);

        // Amount
        TextView amountView = new TextView(this);
        amountView.setText("₹" + String.format("%.2f", amount));
        amountView.setTextSize(15);
        amountView.setTypeface(null, android.graphics.Typeface.BOLD);
        amountView.setTextColor(android.graphics.Color.parseColor("#6366F1"));
        dayView.addView(amountView);

        return dayView;
    }

    private View createSpendingView(String name, double spent, double totalExpense) {
        LinearLayout spendingView = new LinearLayout(this);
        spendingView.setOrientation(LinearLayout.VERTICAL);
        spendingView.setBackgroundResource(R.drawable.input_background);
        int padding = (int) (14 * getResources().getDisplayMetrics().density);
        spendingView.setPadding(padding, padding, padding, padding);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
        spendingView.setLayoutParams(params);

        // Name and amount row
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextSize(14);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        nameView.setTextColor(android.graphics.Color.parseColor("#1F2937"));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        nameView.setLayoutParams(nameParams);
        headerRow.addView(nameView);

        TextView amountView = new TextView(this);
        amountView.setText("₹" + String.format("%.2f", spent));
        amountView.setTextSize(15);
        amountView.setTypeface(null, android.graphics.Typeface.BOLD);
        amountView.setTextColor(android.graphics.Color.parseColor("#10B981"));
        headerRow.addView(amountView);

        spendingView.addView(headerRow);

        // Percentage text (removed progress bar)
        double percentage = totalExpense > 0 ? (spent / totalExpense) * 100 : 0;

        TextView percentView = new TextView(this);
        percentView.setText(String.format("%.1f%% of total expenses", percentage));
        percentView.setTextSize(10);
        percentView.setTextColor(android.graphics.Color.parseColor("#6B7280"));
        LinearLayout.LayoutParams percentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        percentParams.setMargins(0, 4, 0, 0);
        percentView.setLayoutParams(percentParams);
        spendingView.addView(percentView);

        return spendingView;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadExpenses();
        updateTotals();
    }

    // Date Group Data Class
    class DateGroup {
        String date;
        List<DatabaseHelper.Expense> expenses;
        double totalAmount;
        boolean isExpanded = false;
    }

    // RecyclerView Adapter for Date Groups
    class DateGroupAdapter extends RecyclerView.Adapter<DateGroupAdapter.DateGroupViewHolder> {

        private List<DateGroup> dateGroups;

        public DateGroupAdapter(List<DateGroup> dateGroups) {
            this.dateGroups = dateGroups;
        }

        @Override
        public DateGroupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_group, parent, false);
            return new DateGroupViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DateGroupViewHolder holder, int position) {
            DateGroup group = dateGroups.get(position);

            holder.tvDate.setText(group.date);
            holder.tvExpenseCount.setText(group.expenses.size() + " expense" + (group.expenses.size() > 1 ? "s" : ""));
            holder.tvDateTotal.setText("₹" + String.format("%.2f", group.totalAmount));

            // Toggle expand/collapse
            if (group.isExpanded) {
                holder.expensesContainer.setVisibility(View.VISIBLE);
                holder.ivExpandIcon.setRotation(180);
            } else {
                holder.expensesContainer.setVisibility(View.GONE);
                holder.ivExpandIcon.setRotation(0);
            }

            // Populate expenses
            holder.expensesContainer.removeAllViews();
            for (DatabaseHelper.Expense expense : group.expenses) {
                View expenseView = createExpenseView(expense);
                holder.expensesContainer.addView(expenseView);
            }

            // Click listener for date header
            holder.dateHeader.setOnClickListener(v -> {
                group.isExpanded = !group.isExpanded;
                notifyItemChanged(position);
            });
        }

        private View createExpenseView(DatabaseHelper.Expense expense) {
            View view = LayoutInflater.from(TripDetailActivity.this)
                    .inflate(R.layout.item_expense_compact, null);

            TextView tvCategoryIcon = view.findViewById(R.id.tvCategoryIcon);
            TextView tvDescription = view.findViewById(R.id.tvDescription);
            TextView tvCategory = view.findViewById(R.id.tvCategory);
            TextView tvPaidBy = view.findViewById(R.id.tvPaidBy);
            TextView tvAmount = view.findViewById(R.id.tvAmount);

            tvCategoryIcon.setText(getCategoryIcon(expense.category));
            tvDescription.setText(expense.description);
            tvCategory.setText(expense.category);
            tvAmount.setText("₹" + String.format("%.2f", expense.amount));

            if ("SELF_PAID".equals(expense.paidBy)) {
                tvPaidBy.setText("• Self Paid");
            } else {
                tvPaidBy.setText("• Paid by " + expense.paidBy);
            }

            // Long click to delete
            view.setOnLongClickListener(v -> {
                new AlertDialog.Builder(TripDetailActivity.this)
                        .setTitle("Delete Expense")
                        .setMessage("Are you sure you want to delete this expense?")
                        .setPositiveButton("Delete", (d, w) -> {
                            db.deleteExpense(expense.id);
                            loadExpenses();
                            updateTotals();
                            Toast.makeText(TripDetailActivity.this, "Expense deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });

            return view;
        }

        @Override
        public int getItemCount() {
            return dateGroups.size();
        }

        private String getCategoryIcon(String category) {
            switch (category) {
                case "Food": return "🍔";
                case "Transport": return "🚗";
                case "Hotel": return "🏨";
                case "Fun": return "🎬";
                case "Shopping": return "🛍️";
                case "Fuel": return "⛽";
                default: return "📦";
            }
        }

        class DateGroupViewHolder extends RecyclerView.ViewHolder {
            LinearLayout dateHeader, expensesContainer;
            TextView tvDate, tvExpenseCount, tvDateTotal;
            ImageView ivExpandIcon;

            public DateGroupViewHolder(View itemView) {
                super(itemView);
                dateHeader = itemView.findViewById(R.id.dateHeader);
                expensesContainer = itemView.findViewById(R.id.expensesContainer);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvExpenseCount = itemView.findViewById(R.id.tvExpenseCount);
                tvDateTotal = itemView.findViewById(R.id.tvDateTotal);
                ivExpandIcon = itemView.findViewById(R.id.ivExpandIcon);
            }
        }
    }
}