package com.example.madproject;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MarksCacheManager {
    private static final String PREF_NAME = "marks_cache";
    private static final String KEY_MARKS_PREFIX = "cached_marks_";
    private static final String KEY_TIMESTAMP_PREFIX = "last_updated_";
    private static MarksCacheManager instance;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    private MarksCacheManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized MarksCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new MarksCacheManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Save marks list to SharedPreferences for a specific student
     * @param marksList List of InternalMark objects
     * @param sapId Student SAP ID
     */
    public void saveMarksList(List<InternalMark> marksList, String sapId) {
        if (marksList == null || sapId == null) return;

        try {
            String json = gson.toJson(marksList);
            sharedPreferences.edit()
                    .putString(KEY_MARKS_PREFIX + sapId, json)
                    .putLong(KEY_TIMESTAMP_PREFIX + sapId, System.currentTimeMillis())
                    .apply();
            android.util.Log.d("MarksCacheManager", "Saved " + marksList.size() + " marks for " + sapId);
        } catch (Exception e) {
            android.util.Log.e("MarksCacheManager", "Error saving marks: " + e.getMessage());
        }
    }

    /**
     * Get cached marks list for a specific student
     * @param sapId Student SAP ID
     * @return List of InternalMark objects or null if no cache exists
     */
    public List<InternalMark> getCachedMarksList(String sapId) {
        if (sapId == null) return null;

        try {
            String json = sharedPreferences.getString(KEY_MARKS_PREFIX + sapId, null);
            if (json == null) {
                android.util.Log.d("MarksCacheManager", "No cached marks for " + sapId);
                return null;
            }

            Type listType = new TypeToken<ArrayList<InternalMark>>(){}.getType();
            List<InternalMark> marksList = gson.fromJson(json, listType);
            android.util.Log.d("MarksCacheManager", "Loaded " + marksList.size() + " marks from cache for " + sapId);
            return marksList;
        } catch (Exception e) {
            android.util.Log.e("MarksCacheManager", "Error loading marks: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if cached data exists for a student
     * @param sapId Student SAP ID
     * @return true if cache exists
     */
    public boolean hasCachedData(String sapId) {
        if (sapId == null) return false;
        return sharedPreferences.contains(KEY_MARKS_PREFIX + sapId);
    }

    /**
     * Get the last update timestamp for a student's marks
     * @param sapId Student SAP ID
     * @return timestamp in milliseconds
     */
    public long getLastUpdateTime(String sapId) {
        if (sapId == null) return 0;
        return sharedPreferences.getLong(KEY_TIMESTAMP_PREFIX + sapId, 0);
    }

    /**
     * Check if cache is still valid (not older than specified time)
     * @param sapId Student SAP ID
     * @param validityMinutes How many minutes the cache should be valid
     * @return true if cache is valid
     */
    public boolean isCacheValid(String sapId, int validityMinutes) {
        if (sapId == null) return false;

        long lastUpdated = getLastUpdateTime(sapId);
        if (lastUpdated == 0) return false;

        long currentTime = System.currentTimeMillis();
        long diff = currentTime - lastUpdated;
        long validityMillis = validityMinutes * 60 * 1000L;

        return diff < validityMillis;
    }

    /**
     * Clear cache for a specific student
     * @param sapId Student SAP ID
     */
    public void clearCache(String sapId) {
        if (sapId == null) return;

        sharedPreferences.edit()
                .remove(KEY_MARKS_PREFIX + sapId)
                .remove(KEY_TIMESTAMP_PREFIX + sapId)
                .apply();
        android.util.Log.d("MarksCacheManager", "Cleared cache for " + sapId);
    }

    /**
     * Clear all cached marks data
     */
    public void clearAllCache() {
        sharedPreferences.edit().clear().apply();
        android.util.Log.d("MarksCacheManager", "Cleared all marks cache");
    }

    /**
     * Save marks data (backward compatibility)
     */
    public void saveMarks(java.util.Map<String, Object> marksData) {
        String json = gson.toJson(marksData);
        sharedPreferences.edit()
                .putString("cached_marks", json)
                .putLong("last_updated_time", System.currentTimeMillis())
                .apply();
    }

    /**
     * Get cached marks data (backward compatibility)
     */
    public java.util.Map<String, Object> getCachedMarks() {
        String json = sharedPreferences.getString("cached_marks", null);
        if (json == null) return null;
        return gson.fromJson(json, java.util.Map.class);
    }

    /**
     * Check if cache is recent (backward compatibility)
     */
    public boolean isCacheValid() {
        long lastUpdated = sharedPreferences.getLong("last_updated_time", 0);
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - lastUpdated;
        return diff < 10 * 60 * 1000; // 10 minutes validity
    }

    /**
     * Clear cache manually (backward compatibility)
     */
    public void clearCache() {
        clearAllCache();
    }
}