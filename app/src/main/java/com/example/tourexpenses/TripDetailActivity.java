package com.example.tourexpenses;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TripDetailActivity extends AppCompatActivity {

    private TextView tvTripTotal, tvMyShare, tvTripStatusBanner, tvExpenseCount;
    private RecyclerView recyclerExpenses;
    private FloatingActionButton fabAddExpense;
    private Button btnViewSettlement;
    private LinearLayout emptyState;

    private DatabaseHelper db;
    private int tripId;
    private String tripName;
    private String tripStatus;
    private ExpenseAdapter adapter;

    // Category emoji mapping
    private final Map<String, String> categoryEmojis = new HashMap<String, String>() {{
        put("Food", "🍔");
        put("Transport", "🚗");
        put("Hotel", "🏨");
        put("Entertainment", "🎬");
        put("Shopping", "🛍️");
        put("Fuel", "⛽");
        put("Other", "📦");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        db = new DatabaseHelper(this);

        tripId = getIntent().getIntExtra("TRIP_ID", -1);
        tripName = getIntent().getStringExtra("TRIP_NAME");
        tripStatus = getIntent().getStringExtra("TRIP_STATUS");

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        tvTripTotal = findViewById(R.id.tvTripTotal);
        tvMyShare = findViewById(R.id.tvMyShare);
        tvTripStatusBanner = findViewById(R.id.tvTripStatusBanner);
        tvExpenseCount = findViewById(R.id.tvExpenseCount);
        recyclerExpenses = findViewById(R.id.recyclerExpenses);
        fabAddExpense = findViewById(R.id.fabAddExpense);
        btnViewSettlement = findViewById(R.id.btnViewSettlement);
        emptyState = findViewById(R.id.emptyState);

        recyclerExpenses.setLayoutManager(new LinearLayoutManager(this));

        // Show banner if trip is completed
        if ("COMPLETED".equals(tripStatus)) {
            tvTripStatusBanner.setVisibility(View.VISIBLE);
            fabAddExpense.hide();
        }

        fabAddExpense.setOnClickListener(v -> {
            if ("COMPLETED".equals(tripStatus)) {
                Toast.makeText(this, "Cannot add expenses to completed trip", Toast.LENGTH_SHORT).show();
            } else {
                showAddExpenseDialog();
            }
        });

        btnViewSettlement.setOnClickListener(v -> showSettlementDialog());

        loadExpenses();
    }

    private void loadExpenses() {
        List<DatabaseHelper.Expense> expenses = db.getTripExpenses(tripId);
        adapter = new ExpenseAdapter(expenses);
        recyclerExpenses.setAdapter(adapter);

        // Show/hide empty state
        if (expenses.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerExpenses.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerExpenses.setVisibility(View.VISIBLE);
        }

        // Update expense count
        tvExpenseCount.setText(expenses.size() + " items");

        double total = db.getTripTotalExpense(tripId);
        tvTripTotal.setText("₹" + String.format("%.2f", total));

        List<String> participants = db.getTripParticipants(tripId);
        if (!participants.isEmpty()) {
            double sharedTotal = 0;
            for (DatabaseHelper.Expense expense : expenses) {
                if (!"SELF_PAID".equals(expense.paidBy)) {
                    sharedTotal += expense.amount;
                }
            }
            double perPersonShare = sharedTotal / participants.size();
            tvMyShare.setText("₹" + String.format("%.2f", perPersonShare) + " per person");
        } else {
            tvMyShare.setText("No shared expenses yet");
        }
    }

    private void showAddExpenseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null);

        EditText etAmount = view.findViewById(R.id.etAmount);
        EditText etDescription = view.findViewById(R.id.etDescription);
        Spinner spinnerPaidBy = view.findViewById(R.id.spinnerPaidBy);
        Spinner spinnerCategory = view.findViewById(R.id.spinnerCategory);
        Button btnSelfPaid = view.findViewById(R.id.btnSelfPaid);
        Button btnSave = view.findViewById(R.id.btnSaveExpense);
        Button btnCancel = view.findViewById(R.id.btnCancelExpense);

        // Category buttons
        LinearLayout categoryFood = view.findViewById(R.id.categoryFood);
        LinearLayout categoryTransport = view.findViewById(R.id.categoryTransport);
        LinearLayout categoryHotel = view.findViewById(R.id.categoryHotel);
        LinearLayout categoryFun = view.findViewById(R.id.categoryFun);
        LinearLayout categoryShopping = view.findViewById(R.id.categoryShopping);
        LinearLayout categoryFuel = view.findViewById(R.id.categoryFuel);
        LinearLayout categoryOther = view.findViewById(R.id.categoryOther);

        final String[] selectedCategory = {"Food"};

        // Set up category selection
        View.OnClickListener categoryClickListener = v -> {
            // Reset all categories
            categoryFood.setSelected(false);
            categoryTransport.setSelected(false);
            categoryHotel.setSelected(false);
            categoryFun.setSelected(false);
            categoryShopping.setSelected(false);
            categoryFuel.setSelected(false);
            categoryOther.setSelected(false);

            // Select clicked category
            v.setSelected(true);

            // Update selected category
            if (v == categoryFood) selectedCategory[0] = "Food";
            else if (v == categoryTransport) selectedCategory[0] = "Transport";
            else if (v == categoryHotel) selectedCategory[0] = "Hotel";
            else if (v == categoryFun) selectedCategory[0] = "Entertainment";
            else if (v == categoryShopping) selectedCategory[0] = "Shopping";
            else if (v == categoryFuel) selectedCategory[0] = "Fuel";
            else if (v == categoryOther) selectedCategory[0] = "Other";
        };

        categoryFood.setOnClickListener(categoryClickListener);
        categoryTransport.setOnClickListener(categoryClickListener);
        categoryHotel.setOnClickListener(categoryClickListener);
        categoryFun.setOnClickListener(categoryClickListener);
        categoryShopping.setOnClickListener(categoryClickListener);
        categoryFuel.setOnClickListener(categoryClickListener);
        categoryOther.setOnClickListener(categoryClickListener);

        // Set default selection
        categoryFood.setSelected(true);

        // Setup spinner for backward compatibility
        String[] categories = {"Food", "Transport", "Hotel", "Entertainment", "Shopping", "Fuel", "Other"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        List<String> participants = db.getTripParticipants(tripId);
        if (participants.isEmpty()) {
            Toast.makeText(this, "No participants found for this trip", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayAdapter<String> paidByAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, participants);
        paidByAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaidBy.setAdapter(paidByAdapter);

        final boolean[] isSelfPaid = {false};

        btnSelfPaid.setOnClickListener(v -> {
            isSelfPaid[0] = !isSelfPaid[0];
            if (isSelfPaid[0]) {
                spinnerPaidBy.setEnabled(false);
                btnSelfPaid.setText("EVERYONE PAID ✓");
                btnSelfPaid.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_green_light));
            } else {
                spinnerPaidBy.setEnabled(true);
                btnSelfPaid.setText("EVERYONE PAID FOR THEMSELVES");
                btnSelfPaid.setBackgroundTintList(null);
            }
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String category = selectedCategory[0];

            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            String paidBy;
            if (isSelfPaid[0]) {
                paidBy = "SELF_PAID";
            } else {
                paidBy = spinnerPaidBy.getSelectedItem().toString();
            }

            long id = db.addExpense(tripId, paidBy, amount, category, description);
            if (id > 0) {
                Toast.makeText(this, "Expense added successfully", Toast.LENGTH_SHORT).show();
                loadExpenses();
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showSettlementDialog() {
        Map<String, Double> balances = db.getParticipantBalances(tripId);

        if (balances.isEmpty()) {
            Toast.makeText(this, "No shared expenses to settle", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settlement, null);

        LinearLayout contentLayout = view.findViewById(R.id.settlementContent);
        Button btnClose = view.findViewById(R.id.btnCloseSettlement);

        // 1. INDIVIDUAL SPENDING SECTION
        addSectionHeader(contentLayout, "👤", "Individual Spending");
        Map<String, Double> totalSpending = db.getParticipantTotalSpending(tripId);
        for (Map.Entry<String, Double> entry : totalSpending.entrySet()) {
            addSpendingCard(contentLayout, entry.getKey(), entry.getValue());
        }
        addSpacer(contentLayout, 20);

        // 2. DAY-WISE EXPENSES SECTION
        addSectionHeader(contentLayout, "📅", "Day-wise Expenses");
        Map<String, Double> dayWiseExpenses = db.getDayWiseExpenses(tripId);
        if (dayWiseExpenses.isEmpty()) {
            addEmptyMessage(contentLayout, "No expenses recorded");
        } else {
            for (Map.Entry<String, Double> entry : dayWiseExpenses.entrySet()) {
                addDayCard(contentLayout, entry.getKey(), entry.getValue());
            }
        }
        addSpacer(contentLayout, 20);

        // 3. SETTLEMENT SUMMARY SECTION
        addSectionHeader(contentLayout, "📊", "Settlement Summary");
        List<String> receivers = new ArrayList<>();
        List<String> payers = new ArrayList<>();

        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            String name = entry.getKey();
            double balance = entry.getValue();

            if (balance > 0.01) {
                receivers.add(name + ":" + balance);
                addBalanceCard(contentLayout, name, balance, "RECEIVE");
            } else if (balance < -0.01) {
                payers.add(name + ":" + Math.abs(balance));
                addBalanceCard(contentLayout, name, Math.abs(balance), "PAY");
            } else {
                addBalanceCard(contentLayout, name, 0, "SETTLED");
            }
        }
        addSpacer(contentLayout, 20);

        // 4. SIMPLIFIED TRANSACTIONS SECTION
        addSectionHeader(contentLayout, "💸", "Who Pays Whom");

        int i = 0, j = 0;
        boolean hasTransactions = false;

        while (i < payers.size() && j < receivers.size()) {
            String[] payerData = payers.get(i).split(":");
            String[] receiverData = receivers.get(j).split(":");

            String payer = payerData[0];
            double payAmount = Double.parseDouble(payerData[1]);

            String receiver = receiverData[0];
            double receiveAmount = Double.parseDouble(receiverData[1]);

            double transferAmount = Math.min(payAmount, receiveAmount);

            addTransactionCard(contentLayout, payer, receiver, transferAmount);
            hasTransactions = true;

            payAmount -= transferAmount;
            receiveAmount -= transferAmount;

            if (payAmount < 0.01) i++;
            if (receiveAmount < 0.01) j++;

            if (payAmount > 0.01) {
                payers.set(i, payer + ":" + payAmount);
            }
            if (receiveAmount > 0.01) {
                receivers.set(j, receiver + ":" + receiveAmount);
            }
        }

        if (!hasTransactions) {
            addAllSettledCard(contentLayout);
        }

        builder.setView(view);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

// Helper Methods - Add these to your TripDetailActivity class

    private void addSectionHeader(LinearLayout parent, String emoji, String title) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        headerParams.setMargins(0, 0, 0, dpToPx(12));
        header.setLayoutParams(headerParams);

        TextView emojiView = new TextView(this);
        emojiView.setText(emoji);
        emojiView.setTextSize(20);
        header.addView(emojiView);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(android.graphics.Color.parseColor("#1F2937"));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(dpToPx(8), 0, 0, 0);
        titleView.setLayoutParams(titleParams);
        header.addView(titleView);

        parent.addView(header);
    }

    private void addSpendingCard(LinearLayout parent, String name, double amount) {
        androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(8));
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(12));
        card.setCardElevation(dpToPx(2));
        card.setCardBackgroundColor(android.graphics.Color.WHITE);

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.HORIZONTAL);
        cardContent.setGravity(android.view.Gravity.CENTER_VERTICAL);
        cardContent.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Icon
        LinearLayout iconBg = new LinearLayout(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
        iconBg.setLayoutParams(iconParams);
        iconBg.setGravity(android.view.Gravity.CENTER);
        iconBg.setBackgroundResource(R.drawable.category_circle);
        TextView icon = new TextView(this);
        icon.setText("💵");
        icon.setTextSize(20);
        iconBg.addView(icon);
        cardContent.addView(iconBg);

        // Name
        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextSize(15);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        nameView.setTextColor(android.graphics.Color.parseColor("#1F2937"));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        nameParams.setMargins(dpToPx(12), 0, 0, 0);
        nameView.setLayoutParams(nameParams);
        cardContent.addView(nameView);

        // Amount
        TextView amountView = new TextView(this);
        amountView.setText("₹" + String.format("%.2f", amount));
        amountView.setTextSize(16);
        amountView.setTypeface(null, android.graphics.Typeface.BOLD);
        amountView.setTextColor(android.graphics.Color.parseColor("#6366F1"));
        cardContent.addView(amountView);

        card.addView(cardContent);
        parent.addView(card);
    }

    private void addDayCard(LinearLayout parent, String date, double amount) {
        androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(8));
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(12));
        card.setCardElevation(dpToPx(2));
        card.setCardBackgroundColor(android.graphics.Color.WHITE);

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.HORIZONTAL);
        cardContent.setGravity(android.view.Gravity.CENTER_VERTICAL);
        cardContent.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Icon
        LinearLayout iconBg = new LinearLayout(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
        iconBg.setLayoutParams(iconParams);
        iconBg.setGravity(android.view.Gravity.CENTER);
        iconBg.setBackgroundResource(R.drawable.category_circle);
        TextView icon = new TextView(this);
        icon.setText("📆");
        icon.setTextSize(20);
        iconBg.addView(icon);
        cardContent.addView(iconBg);

        // Date
        TextView dateView = new TextView(this);
        dateView.setText(date);
        dateView.setTextSize(15);
        dateView.setTypeface(null, android.graphics.Typeface.BOLD);
        dateView.setTextColor(android.graphics.Color.parseColor("#1F2937"));
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        dateParams.setMargins(dpToPx(12), 0, 0, 0);
        dateView.setLayoutParams(dateParams);
        cardContent.addView(dateView);

        // Amount
        TextView amountView = new TextView(this);
        amountView.setText("₹" + String.format("%.2f", amount));
        amountView.setTextSize(16);
        amountView.setTypeface(null, android.graphics.Typeface.BOLD);
        amountView.setTextColor(android.graphics.Color.parseColor("#6366F1"));
        cardContent.addView(amountView);

        card.addView(cardContent);
        parent.addView(card);
    }

    private void addBalanceCard(LinearLayout parent, String name, double amount, String type) {
        androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(8));
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(12));
        card.setCardElevation(dpToPx(2));

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Set background color based on type
        if (type.equals("RECEIVE")) {
            cardContent.setBackgroundColor(android.graphics.Color.parseColor("#ECFDF5"));
        } else if (type.equals("PAY")) {
            cardContent.setBackgroundColor(android.graphics.Color.parseColor("#FEF2F2"));
        } else {
            cardContent.setBackgroundColor(android.graphics.Color.parseColor("#F3F4F6"));
        }

        // Top row: Name and Amount
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextSize(16);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        nameView.setTextColor(android.graphics.Color.parseColor("#1F2937"));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        nameView.setLayoutParams(nameParams);
        topRow.addView(nameView);

        TextView amountView = new TextView(this);
        amountView.setText("₹" + String.format("%.2f", amount));
        amountView.setTextSize(20);
        amountView.setTypeface(null, android.graphics.Typeface.BOLD);

        if (type.equals("RECEIVE")) {
            amountView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (type.equals("PAY")) {
            amountView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            amountView.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
        topRow.addView(amountView);

        cardContent.addView(topRow);

        // Type label
        TextView typeView = new TextView(this);
        if (type.equals("RECEIVE")) {
            typeView.setText("Should Receive");
        } else if (type.equals("PAY")) {
            typeView.setText("Should Pay");
        } else {
            typeView.setText("Settled");
        }
        typeView.setTextSize(12);
        typeView.setTextColor(android.graphics.Color.parseColor("#6B7280"));
        LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        typeParams.setMargins(0, dpToPx(4), 0, 0);
        typeView.setLayoutParams(typeParams);
        cardContent.addView(typeView);

        card.addView(cardContent);
        parent.addView(card);
    }

    private void addTransactionCard(LinearLayout parent, String payer, String receiver, double amount) {
        androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(12));
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(16));
        card.setCardElevation(dpToPx(3));

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.HORIZONTAL);
        cardContent.setGravity(android.view.Gravity.CENTER_VERTICAL);
        cardContent.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        cardContent.setBackgroundResource(R.drawable.card_gradient);

        // Payer
        LinearLayout payerLayout = new LinearLayout(this);
        payerLayout.setOrientation(LinearLayout.VERTICAL);
        payerLayout.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams payerParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        payerLayout.setLayoutParams(payerParams);

        LinearLayout payerIcon = new LinearLayout(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48));
        payerIcon.setLayoutParams(iconParams);
        payerIcon.setGravity(android.view.Gravity.CENTER);
        payerIcon.setBackgroundResource(R.drawable.icon_circle_gradient);
        TextView payerEmoji = new TextView(this);
        payerEmoji.setText("👤");
        payerEmoji.setTextSize(24);
        payerIcon.addView(payerEmoji);
        payerLayout.addView(payerIcon);

        TextView payerName = new TextView(this);
        payerName.setText(payer);
        payerName.setTextSize(13);
        payerName.setTypeface(null, android.graphics.Typeface.BOLD);
        payerName.setTextColor(android.graphics.Color.WHITE);
        payerName.setMaxLines(1);
        LinearLayout.LayoutParams payerNameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        payerNameParams.setMargins(0, dpToPx(8), 0, 0);
        payerName.setLayoutParams(payerNameParams);
        payerLayout.addView(payerName);

        cardContent.addView(payerLayout);

        // Arrow and Amount
        LinearLayout arrowLayout = new LinearLayout(this);
        arrowLayout.setOrientation(LinearLayout.VERTICAL);
        arrowLayout.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        arrowParams.setMargins(dpToPx(8), 0, dpToPx(8), 0);
        arrowLayout.setLayoutParams(arrowParams);

        TextView arrow = new TextView(this);
        arrow.setText("→");
        arrow.setTextSize(32);
        arrow.setTextColor(android.graphics.Color.WHITE);
        arrow.setTypeface(null, android.graphics.Typeface.BOLD);
        arrowLayout.addView(arrow);

        TextView amountView = new TextView(this);
        amountView.setText("₹" + String.format("%.2f", amount));
        amountView.setTextSize(16);
        amountView.setTypeface(null, android.graphics.Typeface.BOLD);
        amountView.setTextColor(android.graphics.Color.WHITE);
        amountView.setBackgroundColor(android.graphics.Color.parseColor("#40FFFFFF"));
        amountView.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4));
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        amountParams.setMargins(0, dpToPx(4), 0, 0);
        amountView.setLayoutParams(amountParams);
        arrowLayout.addView(amountView);

        cardContent.addView(arrowLayout);

        // Receiver
        LinearLayout receiverLayout = new LinearLayout(this);
        receiverLayout.setOrientation(LinearLayout.VERTICAL);
        receiverLayout.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams receiverParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        receiverLayout.setLayoutParams(receiverParams);

        LinearLayout receiverIcon = new LinearLayout(this);
        receiverIcon.setLayoutParams(iconParams);
        receiverIcon.setGravity(android.view.Gravity.CENTER);
        receiverIcon.setBackgroundResource(R.drawable.icon_circle_gradient);
        TextView receiverEmoji = new TextView(this);
        receiverEmoji.setText("👤");
        receiverEmoji.setTextSize(24);
        receiverIcon.addView(receiverEmoji);
        receiverLayout.addView(receiverIcon);

        TextView receiverName = new TextView(this);
        receiverName.setText(receiver);
        receiverName.setTextSize(13);
        receiverName.setTypeface(null, android.graphics.Typeface.BOLD);
        receiverName.setTextColor(android.graphics.Color.WHITE);
        receiverName.setMaxLines(1);
        LinearLayout.LayoutParams receiverNameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        receiverNameParams.setMargins(0, dpToPx(8), 0, 0);
        receiverName.setLayoutParams(receiverNameParams);
        receiverLayout.addView(receiverName);

        cardContent.addView(receiverLayout);

        card.addView(cardContent);
        parent.addView(card);
    }

    private void addAllSettledCard(LinearLayout parent) {
        LinearLayout settledLayout = new LinearLayout(this);
        settledLayout.setOrientation(LinearLayout.VERTICAL);
        settledLayout.setGravity(android.view.Gravity.CENTER);
        settledLayout.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24));
        settledLayout.setBackgroundResource(R.drawable.input_background);

        LinearLayout iconBg = new LinearLayout(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(80), dpToPx(80));
        iconBg.setLayoutParams(iconParams);
        iconBg.setGravity(android.view.Gravity.CENTER);
        iconBg.setBackgroundResource(R.drawable.icon_circle_gradient);
        TextView icon = new TextView(this);
        icon.setText("✅");
        icon.setTextSize(40);
        iconBg.addView(icon);
        settledLayout.addView(iconBg);

        TextView title = new TextView(this);
        title.setText("All Settled!");
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(android.graphics.Color.parseColor("#10B981"));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, dpToPx(16), 0, 0);
        title.setLayoutParams(titleParams);
        settledLayout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("No payments needed");
        subtitle.setTextSize(13);
        subtitle.setTextColor(android.graphics.Color.parseColor("#9CA3AF"));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.setMargins(0, dpToPx(4), 0, 0);
        subtitle.setLayoutParams(subtitleParams);
        settledLayout.addView(subtitle);

        parent.addView(settledLayout);
    }

    private void addEmptyMessage(LinearLayout parent, String message) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextSize(14);
        textView.setTextColor(getResources().getColor(android.R.color.darker_gray));
        textView.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        textView.setGravity(android.view.Gravity.CENTER);
        parent.addView(textView);
    }

    private void addSpacer(LinearLayout parent, int heightDp) {
        View spacer = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(heightDp)
        );
        spacer.setLayoutParams(params);
        parent.addView(spacer);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        DatabaseHelper.Trip trip = db.getTripById(tripId);
        if (trip != null) {
            tripStatus = trip.status;

            if ("COMPLETED".equals(tripStatus)) {
                tvTripStatusBanner.setVisibility(View.VISIBLE);
                fabAddExpense.hide();
            } else {
                tvTripStatusBanner.setVisibility(View.GONE);
                fabAddExpense.show();
            }
        }
        loadExpenses();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

        private List<DatabaseHelper.Expense> expenses;

        public ExpenseAdapter(List<DatabaseHelper.Expense> expenses) {
            this.expenses = expenses;
        }

        @Override
        public ExpenseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_expense, parent, false);
            return new ExpenseViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ExpenseViewHolder holder, int position) {
            DatabaseHelper.Expense expense = expenses.get(position);

            holder.tvAmount.setText("₹" + String.format("%.2f", expense.amount));
            holder.tvCategory.setText(expense.category);
            holder.tvDescription.setText(expense.description);

            // Set category emoji
            String emoji = categoryEmojis.getOrDefault(expense.category, "📦");
            holder.tvCategoryIcon.setText(emoji);

            if ("SELF_PAID".equals(expense.paidBy)) {
                holder.tvPaidBy.setText("Everyone (Self Paid)");
                holder.tvPaidBy.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            } else {
                holder.tvPaidBy.setText("Paid by " + expense.paidBy);
                holder.tvPaidBy.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }

            String formattedDate = expense.date;
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date date = inputFormat.parse(expense.date);
                if (date != null) {
                    formattedDate = outputFormat.format(date);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            holder.tvDate.setText(formattedDate);

            holder.itemView.setOnLongClickListener(v -> {
                if ("COMPLETED".equals(tripStatus)) {
                    Toast.makeText(TripDetailActivity.this, "Cannot delete expenses from completed trip", Toast.LENGTH_SHORT).show();
                    return true;
                }

                new AlertDialog.Builder(TripDetailActivity.this)
                        .setTitle("Delete Expense")
                        .setMessage("Delete this expense?")
                        .setPositiveButton("Delete", (d, w) -> {
                            db.deleteExpense(expense.id);
                            loadExpenses();
                            Toast.makeText(TripDetailActivity.this, "Expense deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return expenses.size();
        }

        class ExpenseViewHolder extends RecyclerView.ViewHolder {
            TextView tvAmount, tvCategory, tvDescription, tvPaidBy, tvDate, tvCategoryIcon;

            public ExpenseViewHolder(View itemView) {
                super(itemView);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvPaidBy = itemView.findViewById(R.id.tvPaidBy);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvCategoryIcon = itemView.findViewById(R.id.tvCategoryIcon);
            }
        }
    }
}