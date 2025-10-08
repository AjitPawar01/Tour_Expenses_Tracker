package com.example.tourexpenses;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "TourExpenses.db";
    private static final int DATABASE_VERSION = 5;

    // Trips Table
    private static final String TABLE_TRIPS = "trips";
    private static final String TRIP_ID = "trip_id";
    private static final String TRIP_NAME = "trip_name";
    private static final String TRIP_START_DATE = "start_date";
    private static final String TRIP_END_DATE = "end_date";
    private static final String TRIP_STATUS = "status";

    // Participants Table
    private static final String TABLE_PARTICIPANTS = "participants";
    private static final String PARTICIPANT_ID = "participant_id";
    private static final String PARTICIPANT_TRIP_ID = "trip_id";
    private static final String PARTICIPANT_NAME = "name";

    // Expenses Table
    private static final String TABLE_EXPENSES = "expenses";
    private static final String EXPENSE_ID = "expense_id";
    private static final String EXPENSE_TRIP_ID = "trip_id";
    private static final String EXPENSE_PAID_BY = "paid_by";
    private static final String EXPENSE_AMOUNT = "amount";
    private static final String EXPENSE_CATEGORY = "category";
    private static final String EXPENSE_DESCRIPTION = "description";
    private static final String EXPENSE_DATE = "expense_date";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTripsTable = "CREATE TABLE " + TABLE_TRIPS + " (" +
                TRIP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TRIP_NAME + " TEXT NOT NULL, " +
                TRIP_START_DATE + " TEXT, " +
                TRIP_END_DATE + " TEXT, " +
                TRIP_STATUS + " TEXT DEFAULT 'ONGOING')";
        db.execSQL(createTripsTable);

        String createParticipantsTable = "CREATE TABLE " + TABLE_PARTICIPANTS + " (" +
                PARTICIPANT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                PARTICIPANT_TRIP_ID + " INTEGER, " +
                PARTICIPANT_NAME + " TEXT NOT NULL, " +
                "FOREIGN KEY(" + PARTICIPANT_TRIP_ID + ") REFERENCES " + TABLE_TRIPS + "(" + TRIP_ID + "))";
        db.execSQL(createParticipantsTable);

        String createExpensesTable = "CREATE TABLE " + TABLE_EXPENSES + " (" +
                EXPENSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                EXPENSE_TRIP_ID + " INTEGER, " +
                EXPENSE_PAID_BY + " TEXT NOT NULL, " +
                EXPENSE_AMOUNT + " REAL NOT NULL, " +
                EXPENSE_CATEGORY + " TEXT, " +
                EXPENSE_DESCRIPTION + " TEXT, " +
                EXPENSE_DATE + " TEXT, " +
                "FOREIGN KEY(" + EXPENSE_TRIP_ID + ") REFERENCES " + TABLE_TRIPS + "(" + TRIP_ID + "))";
        db.execSQL(createExpensesTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PARTICIPANTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
        onCreate(db);
    }

    // TRIP OPERATIONS
    public long addTrip(String name, String startDate, String endDate, List<String> participants) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues tripValues = new ContentValues();
        tripValues.put(TRIP_NAME, name);
        tripValues.put(TRIP_START_DATE, startDate);
        tripValues.put(TRIP_END_DATE, endDate);
        tripValues.put(TRIP_STATUS, "ONGOING");
        long tripId = db.insert(TABLE_TRIPS, null, tripValues);

        if (tripId > 0 && participants != null && !participants.isEmpty()) {
            for (String participant : participants) {
                ContentValues participantValues = new ContentValues();
                participantValues.put(PARTICIPANT_TRIP_ID, tripId);
                participantValues.put(PARTICIPANT_NAME, participant.trim());
                db.insert(TABLE_PARTICIPANTS, null, participantValues);
            }
        }

        db.close();
        return tripId;
    }

    public List<Trip> getAllTrips() {
        List<Trip> trips = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TRIPS + " ORDER BY " + TRIP_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                Trip trip = new Trip();
                trip.id = cursor.getInt(0);
                trip.name = cursor.getString(1);
                trip.startDate = cursor.getString(2);
                trip.endDate = cursor.getString(3);
                trip.status = cursor.getString(4);
                trip.totalExpense = getTripTotalExpense(trip.id);

                // If no end date set, use last expense date
                if (trip.endDate == null || trip.endDate.isEmpty()) {
                    String lastExpenseDate = getLastExpenseDate(trip.id);
                    if (lastExpenseDate != null && !lastExpenseDate.isEmpty()) {
                        trip.endDate = lastExpenseDate;
                    }
                }

                trips.add(trip);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return trips;
    }

    public Trip getTripById(int tripId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TRIPS + " WHERE " + TRIP_ID + " = ?",
                new String[]{String.valueOf(tripId)});

        Trip trip = null;
        if (cursor.moveToFirst()) {
            trip = new Trip();
            trip.id = cursor.getInt(0);
            trip.name = cursor.getString(1);
            trip.startDate = cursor.getString(2);
            trip.endDate = cursor.getString(3);
            trip.status = cursor.getString(4);
            trip.totalExpense = getTripTotalExpense(trip.id);

            // If no end date set, use last expense date
            if (trip.endDate == null || trip.endDate.isEmpty()) {
                String lastExpenseDate = getLastExpenseDate(trip.id);
                if (lastExpenseDate != null && !lastExpenseDate.isEmpty()) {
                    trip.endDate = lastExpenseDate;
                }
            }
        }
        cursor.close();
        db.close();
        return trip;
    }

    public String getLastExpenseDate(int tripId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + EXPENSE_DATE + " FROM " + TABLE_EXPENSES +
                        " WHERE " + EXPENSE_TRIP_ID + " = ? ORDER BY " + EXPENSE_DATE + " DESC LIMIT 1",
                new String[]{String.valueOf(tripId)});

        String lastDate = null;
        if (cursor.moveToFirst()) {
            String dateTime = cursor.getString(0);
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = inputFormat.parse(dateTime);
                if (date != null) {
                    lastDate = outputFormat.format(date);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        cursor.close();
        db.close();
        return lastDate;
    }

    public void updateTripStatus(int tripId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TRIP_STATUS, status);
        db.update(TABLE_TRIPS, values, TRIP_ID + " = ?", new String[]{String.valueOf(tripId)});
        db.close();
    }

    public void deleteTrip(int tripId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EXPENSES, EXPENSE_TRIP_ID + " = ?", new String[]{String.valueOf(tripId)});
        db.delete(TABLE_PARTICIPANTS, PARTICIPANT_TRIP_ID + " = ?", new String[]{String.valueOf(tripId)});
        db.delete(TABLE_TRIPS, TRIP_ID + " = ?", new String[]{String.valueOf(tripId)});
        db.close();
    }

    // PARTICIPANT OPERATIONS
    public List<String> getTripParticipants(int tripId) {
        List<String> participants = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + PARTICIPANT_NAME + " FROM " + TABLE_PARTICIPANTS +
                        " WHERE " + PARTICIPANT_TRIP_ID + " = ? ORDER BY " + PARTICIPANT_NAME,
                new String[]{String.valueOf(tripId)});

        if (cursor.moveToFirst()) {
            do {
                participants.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return participants;
    }

    // EXPENSE OPERATIONS
    public long addExpense(int tripId, String paidBy, double amount, String category, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(EXPENSE_TRIP_ID, tripId);
        values.put(EXPENSE_PAID_BY, paidBy);
        values.put(EXPENSE_AMOUNT, amount);
        values.put(EXPENSE_CATEGORY, category);
        values.put(EXPENSE_DESCRIPTION, description);
        values.put(EXPENSE_DATE, getCurrentDateTime());
        long id = db.insert(TABLE_EXPENSES, null, values);
        db.close();
        return id;
    }

    public List<Expense> getTripExpenses(int tripId) {
        List<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EXPENSES +
                        " WHERE " + EXPENSE_TRIP_ID + " = ? ORDER BY " + EXPENSE_ID + " DESC",
                new String[]{String.valueOf(tripId)});

        if (cursor.moveToFirst()) {
            do {
                Expense expense = new Expense();
                expense.id = cursor.getInt(0);
                expense.tripId = cursor.getInt(1);
                expense.paidBy = cursor.getString(2);
                expense.amount = cursor.getDouble(3);
                expense.category = cursor.getString(4);
                expense.description = cursor.getString(5);
                expense.date = cursor.getString(6);
                expenses.add(expense);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return expenses;
    }

    public void deleteExpense(int expenseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EXPENSES, EXPENSE_ID + " = ?", new String[]{String.valueOf(expenseId)});
        db.close();
    }

    // CALCULATIONS
    public double getTripTotalExpense(int tripId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + EXPENSE_AMOUNT + ") FROM " + TABLE_EXPENSES +
                " WHERE " + EXPENSE_TRIP_ID + " = ?", new String[]{String.valueOf(tripId)});
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    public Map<String, Double> getParticipantBalances(int tripId) {
        Map<String, Double> balances = new HashMap<>();
        List<String> participants = getTripParticipants(tripId);

        if (participants.isEmpty()) {
            return balances;
        }

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor sharedCursor = db.rawQuery("SELECT SUM(" + EXPENSE_AMOUNT + ") FROM " + TABLE_EXPENSES +
                        " WHERE " + EXPENSE_TRIP_ID + " = ? AND " + EXPENSE_PAID_BY + " != 'SELF_PAID'",
                new String[]{String.valueOf(tripId)});

        double totalShared = 0;
        if (sharedCursor.moveToFirst()) {
            totalShared = sharedCursor.getDouble(0);
        }
        sharedCursor.close();

        double perPersonShare = totalShared / participants.size();

        for (String participant : participants) {
            Cursor paidCursor = db.rawQuery("SELECT SUM(" + EXPENSE_AMOUNT + ") FROM " + TABLE_EXPENSES +
                            " WHERE " + EXPENSE_TRIP_ID + " = ? AND " + EXPENSE_PAID_BY + " = ?",
                    new String[]{String.valueOf(tripId), participant});

            double paid = 0;
            if (paidCursor.moveToFirst()) {
                paid = paidCursor.getDouble(0);
            }
            paidCursor.close();

            double balance = paid - perPersonShare;
            balances.put(participant, balance);
        }

        db.close();
        return balances;
    }

    public Map<String, Double> getParticipantTotalSpending(int tripId) {
        Map<String, Double> spending = new HashMap<>();
        List<String> participants = getTripParticipants(tripId);

        SQLiteDatabase db = this.getReadableDatabase();

        for (String participant : participants) {
            Cursor cursor = db.rawQuery("SELECT SUM(" + EXPENSE_AMOUNT + ") FROM " + TABLE_EXPENSES +
                            " WHERE " + EXPENSE_TRIP_ID + " = ? AND " + EXPENSE_PAID_BY + " = ?",
                    new String[]{String.valueOf(tripId), participant});

            double total = 0;
            if (cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
            cursor.close();
            spending.put(participant, total);
        }

        db.close();
        return spending;
    }

    public Map<String, Double> getDayWiseExpenses(int tripId) {
        Map<String, Double> dayWiseExpenses = new TreeMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT " + EXPENSE_DATE + ", " + EXPENSE_AMOUNT + " FROM " + TABLE_EXPENSES +
                " WHERE " + EXPENSE_TRIP_ID + " = ? ORDER BY " + EXPENSE_DATE, new String[]{String.valueOf(tripId)});

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        if (cursor.moveToFirst()) {
            do {
                String dateTime = cursor.getString(0);
                double amount = cursor.getDouble(1);

                try {
                    Date date = inputFormat.parse(dateTime);
                    String dayKey = outputFormat.format(date);

                    if (dayWiseExpenses.containsKey(dayKey)) {
                        dayWiseExpenses.put(dayKey, dayWiseExpenses.get(dayKey) + amount);
                    } else {
                        dayWiseExpenses.put(dayKey, amount);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return dayWiseExpenses;
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // DATA MODELS
    public static class Trip {
        public int id;
        public String name;
        public String startDate;
        public String endDate;
        public String status;
        public double totalExpense;
    }

    public static class Expense {
        public int id;
        public int tripId;
        public String paidBy;
        public double amount;
        public String category;
        public String description;
        public String date;
    }
}