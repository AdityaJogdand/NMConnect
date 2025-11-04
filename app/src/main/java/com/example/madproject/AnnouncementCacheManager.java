package com.example.madproject;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementCacheManager {
    private static final String PREF_NAME = "announcement_cache";
    private static final String KEY_ANNOUNCEMENTS = "cached_announcements";
    private static final String KEY_LAST_UPDATED = "last_updated";
    private static final String KEY_CACHE_VERSION = "cache_version_";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Gson gson;

    public AnnouncementCacheManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
        gson = new Gson();
    }

    // Save announcements to cache
    public void saveAnnouncements(List<Announcement> announcements, String branch, String semester) {
        String json = gson.toJson(announcements);
        String cacheKey = KEY_ANNOUNCEMENTS + "_" + branch + "_" + semester;

        editor.putString(cacheKey, json);
        editor.putLong(KEY_LAST_UPDATED + "_" + branch + "_" + semester, System.currentTimeMillis());
        editor.apply();
    }

    // Get cached announcements
    public List<Announcement> getCachedAnnouncements(String branch, String semester) {
        String cacheKey = KEY_ANNOUNCEMENTS + "_" + branch + "_" + semester;
        String json = prefs.getString(cacheKey, null);

        if (json != null) {
            Type type = new TypeToken<List<Announcement>>(){}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }

    // Check if cache exists
    public boolean hasCachedData(String branch, String semester) {
        String cacheKey = KEY_ANNOUNCEMENTS + "_" + branch + "_" + semester;
        return prefs.contains(cacheKey);
    }

    // Get last update timestamp
    public long getLastUpdateTime(String branch, String semester) {
        return prefs.getLong(KEY_LAST_UPDATED + "_" + branch + "_" + semester, 0);
    }

    // Check if cache is stale (older than specified minutes)
    public boolean isCacheStale(String branch, String semester, long staleMinutes) {
        long lastUpdate = getLastUpdateTime(branch, semester);
        long currentTime = System.currentTimeMillis();
        long staleTime = staleMinutes * 60 * 1000; // Convert to milliseconds

        return (currentTime - lastUpdate) > staleTime;
    }

    // Clear cache for specific branch/semester
    public void clearCache(String branch, String semester) {
        String cacheKey = KEY_ANNOUNCEMENTS + "_" + branch + "_" + semester;
        editor.remove(cacheKey);
        editor.remove(KEY_LAST_UPDATED + "_" + branch + "_" + semester);
        editor.apply();
    }

    // Clear all announcement cache
    public void clearAllCache() {
        editor.clear();
        editor.apply();
    }
}