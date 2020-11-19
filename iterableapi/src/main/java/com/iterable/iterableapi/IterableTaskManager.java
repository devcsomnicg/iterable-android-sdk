package com.iterable.iterableapi;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;

public class IterableTaskManager {

    private static IterableTaskManager sharedInstance;

    private static final String TAG = "IterableTaskManager";

    static final String ITERABLE_TASK_TABLE_NAME = "OfflineTask";
    private static final String REPLACING_STRING = "*#*#*#*";
    private static final String QUERY_GET_TASK_BY_ID = "select * from OfflineTask where task_id = '"+REPLACING_STRING+"'";

    static final String OFFLINE_TASK_COLUMN_DATA = " (" + IterableTask.TASK_ID + " TEXT PRIMARY KEY," +
            IterableTask.NAME + " TEXT," +
            IterableTask.VERSION + " INTEGER," +
            IterableTask.CREATED_AT + " TEXT," +
            IterableTask.MODIFIED_AT + " TEXT," +
            IterableTask.LAST_ATTEMPTED_AT + " TEXT," +
            IterableTask.SCHEDULED_AT + " TEXT," +
            IterableTask.REQUESTED_AT + " TEXT," +
            IterableTask.PROCESSING + " BOOLEAN," +
            IterableTask.FAILED + " BOOLEAN," +
            IterableTask.BLOCKING + " BOOLEAN," +
            IterableTask.DATA + " TEXT," +
            IterableTask.ERROR + " TEXT," +
            IterableTask.TYPE + " TEXT," +
            IterableTask.ATTEMPTS + " INTEGER" + ")";
    private static final String KEY_ROWID = "rowid";

    private SQLiteDatabase database;
    private IterableTaskDatabaseManager databaseManager;
    private ContentValues contentValues = new ContentValues();
    private boolean hasDatabaseError = false;

    IterableTaskManager() {
        try {
            Context context = IterableApi.getInstance().getMainActivityContext();
            if (context == null) {
                return;
            }

            if (databaseManager == null) {
                databaseManager = new IterableTaskDatabaseManager(IterableApi.getInstance().getMainActivityContext());
            }
            database = databaseManager.getWritableDatabase();
        } catch (Throwable t) {
            IterableLogger.e(TAG, "Failed to create database");
        }
    }

    public static IterableTaskManager sharedInstance() {
        if(sharedInstance == null) {
            sharedInstance = new IterableTaskManager();
        }
        return sharedInstance;
    }

    /*
    * Create and insert a new task in the database
    *
    * */
    String createTask(String name) {

        if(database == null) {
            IterableLogger.e(TAG, "Database not initialized");
            return null;
        }
        IterableTask iterableTask = new IterableTask(name, IterableTaskType.API);
        contentValues.put(IterableTask.TASK_ID, iterableTask.id);
        contentValues.put(IterableTask.NAME, iterableTask.name);
        contentValues.put(IterableTask.VERSION, iterableTask.version);
        contentValues.put(IterableTask.CREATED_AT, iterableTask.createdAt.toString());
        if(iterableTask.modifiedAt != null) {
            contentValues.put(IterableTask.MODIFIED_AT, iterableTask.modifiedAt.toString());
        }
        if(iterableTask.lastAttemptedAt != null) {
            contentValues.put(IterableTask.LAST_ATTEMPTED_AT, iterableTask.lastAttemptedAt.toString());
        }
        if(iterableTask.scheduledAt != null) {
            contentValues.put(IterableTask.SCHEDULED_AT, iterableTask.scheduledAt.toString());
        }
        if(iterableTask.requestedAt != null) {
            contentValues.put(IterableTask.REQUESTED_AT, iterableTask.requestedAt.toString());
        }
        if(iterableTask.processing != null) {
            contentValues.put(IterableTask.PROCESSING, iterableTask.processing);
        }
        if(iterableTask.failed != null) {
            contentValues.put(IterableTask.FAILED, iterableTask.failed);
        }
        if(iterableTask.blocking != null) {
            contentValues.put(IterableTask.BLOCKING, iterableTask.blocking);
        }
        if(iterableTask.data != null) {
            contentValues.put(IterableTask.DATA, iterableTask.data.toString());
        }
        if(iterableTask.taskFailureData != null) {
            contentValues.put(IterableTask.ERROR, iterableTask.taskFailureData.toString());
        }

        contentValues.put(IterableTask.TYPE, iterableTask.taskType.toString());
        contentValues.put(IterableTask.ATTEMPTS, iterableTask.attempts);

        database.insert(ITERABLE_TASK_TABLE_NAME,null,contentValues);
        contentValues.clear();

        return iterableTask.id;
    }

    IterableTask getTask(String id) {

        String name = null;
        IterableTaskType type = null;
        int version = 1;
        int attempts = 0;
        Date dateCreated = null;
        Date dateModified = null, datelastAttempted = null, datescheduled = null, dateRequested = null;
        Boolean processing = false, failed = false, blocking = false;
        Object data = null, error = null;


        if (database == null) {
            IterableLogger.e(TAG, "database not initialized");
            return null;
        }

        String query = QUERY_GET_TASK_BY_ID.replace(REPLACING_STRING, id);
        Cursor cursor = database.rawQuery(query, null);

        if (!cursor.moveToFirst()) {
            IterableLogger.d(TAG, "No record found");
            return null;
        }
        name = cursor.getString(cursor.getColumnIndex(IterableTask.NAME));
        version = cursor.getInt(cursor.getColumnIndex(IterableTask.VERSION));
        dateCreated = new Date(cursor.getString(cursor.getColumnIndex(IterableTask.CREATED_AT)));
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.MODIFIED_AT))) {
            dateModified = new Date(cursor.getString(cursor.getColumnIndex(IterableTask.MODIFIED_AT)));
        }

        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.LAST_ATTEMPTED_AT))) {
            datelastAttempted = new Date(cursor.getString(cursor.getColumnIndex(IterableTask.LAST_ATTEMPTED_AT)));
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.SCHEDULED_AT))) {
            datescheduled = new Date(cursor.getString(cursor.getColumnIndex(IterableTask.SCHEDULED_AT)));
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.REQUESTED_AT))) {
            dateRequested = new Date(cursor.getString(cursor.getColumnIndex(IterableTask.REQUESTED_AT)));
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.PROCESSING))) {
            processing = cursor.getInt(cursor.getColumnIndex(IterableTask.PROCESSING)) > 0;
        }

        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.FAILED))) {
            failed = cursor.getInt(cursor.getColumnIndex(IterableTask.FAILED)) > 0;
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.BLOCKING))) {
            blocking = cursor.getInt(cursor.getColumnIndex(IterableTask.BLOCKING)) > 0;
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.DATA))) {
            data = cursor.getBlob(cursor.getColumnIndex(IterableTask.DATA));
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.ERROR))) {
            error = cursor.getBlob(cursor.getColumnIndex(IterableTask.ERROR));
        }
        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.TYPE))) {
            type = IterableTaskType.valueOf(cursor.getString(cursor.getColumnIndex(IterableTask.TYPE)));
        }

        if (!cursor.isNull(cursor.getColumnIndex(IterableTask.ATTEMPTS))) {
            attempts = cursor.getInt(cursor.getColumnIndex(IterableTask.ATTEMPTS));
        }

        IterableTask task = new IterableTask(id, name, version, dateCreated, dateModified, datelastAttempted, datescheduled, dateRequested, processing, failed, blocking, data, error,IterableTaskType.API,attempts);
        IterableLogger.v(TAG, "Found " + cursor.getColumnCount() + "columns");
        return task;
    }

    ArrayList<String> getAllTaskIds() {
        if(!precheck()) return null;
        Cursor cursor = database.rawQuery("SELECT " + IterableTask.TASK_ID +
                        " FROM " + ITERABLE_TASK_TABLE_NAME,
                null);
        ArrayList<String> taskIds = new ArrayList<>();
        if(cursor.moveToFirst()) {
            do {
                taskIds.add(cursor.getString(0));
            }while (cursor.moveToNext());
        }
        IterableLogger.v(TAG, "Found " + cursor.getColumnCount() + " columns");

        return taskIds;
    }
    /**
     * Gets number of rows in the event table.
     *
     * @return Number of rows in the event table.
     */
    long getTaskCount() {
        long count = 0;
        if (database == null) {
            return count;
        }
        try {
            count = DatabaseUtils.queryNumEntries(database, ITERABLE_TASK_TABLE_NAME);
            hasDatabaseError = false;
        } catch (Throwable t) {
            IterableLogger.e(TAG, "Unable to get a number of rows in the table.", t);
        }
        return count;
    }

    Boolean deleteAllTasks () {
        if (!precheck()) return false;

        int numberOfRowsDeleted = database.delete(ITERABLE_TASK_TABLE_NAME, null, null);
        IterableLogger.v(TAG, "Deleted " + numberOfRowsDeleted + " offline tasks");
        return true;
    }

    Boolean deleteTask(String id) {
        if (! precheck()) return false;
        int numberOfEntriesDeleted = database.delete(ITERABLE_TASK_TABLE_NAME, IterableTask.TASK_ID + " =?", new String[]{id});
        IterableLogger.v(TAG, "Deleted entry - " + numberOfEntriesDeleted);
        return true;
    }

    Boolean updateModifiedAt(String id, Date date) {
        if (!precheck()) return false;
        contentValues.put(IterableTask.MODIFIED_AT, date.toString());
        return updateTaskWithContentValues(id);
    }

    Boolean updateLastAttemptedAt(String id, Date date) {
        if (!precheck()) return false;
        contentValues.put(IterableTask.LAST_ATTEMPTED_AT, date.toString());
        return updateTaskWithContentValues(id);
    }

    Boolean updateRequestedAt(String id, Date date) {
        if (!precheck()) return false;
        contentValues.put(IterableTask.REQUESTED_AT, date.toString());
        return updateTaskWithContentValues(id);
    }

    Boolean updateScheduledAt(String id, Date date) {
        if (precheck()) return false;
        contentValues.put(IterableTask.SCHEDULED_AT, date.toString());
        return updateTaskWithContentValues(id);
    }

    Boolean updateIsProcessing(String id, Boolean state) {
        if (!precheck()) return false;
        contentValues.put(IterableTask.PROCESSING, state);
        return updateTaskWithContentValues(id);
    }

    Boolean updateHasFailed(String id, Boolean state) {
        if (!precheck()) return false;
        contentValues.put(IterableTask.FAILED, state);
        return updateTaskWithContentValues(id);
    }

    Boolean updateAttempts(String id, int attempt) {
        if (!precheck()) return false;
        contentValues.put(IterableTask.ATTEMPTS, attempt);
        return updateTaskWithContentValues(id);
    }

    Boolean updateAttempts(String id) {
        if (!precheck()) return false;
        IterableTask task = getTask(id);
        if (task == null) {
            IterableLogger.e(TAG, "No task found for id " + id);
            return false;
        }
        contentValues.put(IterableTask.ATTEMPTS, task.attempts + 1);
        return updateTaskWithContentValues(id);
    }

    private Boolean updateTaskWithContentValues(String id) {
        int result = database.update(ITERABLE_TASK_TABLE_NAME, contentValues, IterableTask.TASK_ID + "=?", new String[]{id});
        contentValues.clear();
        if (result > 0) return true;
        else return false;
    }

    private boolean precheck() {
        if (database == null) {
            IterableLogger.e(TAG, "Database not initialized");
            return false;
        }
        if (getTaskCount() == 0) {
            IterableLogger.d(TAG, "No pending offline tasks found");
            return false;
        }
        return true;
    }

    //TODO: Decide how the data and error are going to be stored. Is it going to be stringified JSON ?
//    Boolean updateError(String id, Object errorData) {
//        if (!precheck()) return false;
//        contentValues.put(IterableTask.ERROR, errorData);
//        return updateTaskWithContentValues(id);
//    }
}


//TODO: To delete this enum if unnecessary
//enum OfflineTaskColumnIndex{
//    ID,
//   NAME,
//   VERSION,
//   CREATED,
//   MODIFIED,
//   LAST_ATTEMPTED,
//   SCHEDEULED,
//   REQUESTED,
//   PROCESSING,
//   FAILED,
//   BLOCKED,
//   DATA,
//   ERROR,
//   TYPE,
//   ATTEMPTS
//        }
