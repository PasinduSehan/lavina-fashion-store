package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Utility for Firebase initialization and providing configured SDK instances.
 * Sets up FirebaseApp, FirebaseAuth, and FirebaseFirestore with robust multi-lifecycle support.
 */
object FirebaseInitializer {

    private const val TAG = "FirebaseInitializer"

    @Volatile
    private var isAppInitialized: Boolean = false

    @Volatile
    private var firebaseApp: FirebaseApp? = null

    @Volatile
    private var authInstance: FirebaseAuth? = null

    @Volatile
    private var firestoreInstance: FirebaseFirestore? = null

    /**
     * Initializes FirebaseApp with the required configuration and sets up
     * Firebase Auth and Firestore for use across application components.
     */
    @Synchronized
    fun initialize(context: Context): FirebaseApp {
        val appContext = context.applicationContext

        // If already initialized and instances are ready, return immediately
        firebaseApp?.let { existingApp ->
            if (authInstance != null && firestoreInstance != null) {
                return existingApp
            }
        }

        val app = getOrCreateFirebaseApp(appContext)
        firebaseApp = app

        // Initialize and configure FirebaseAuth
        initAuth(app)

        // Initialize and configure FirebaseFirestore
        initFirestore(app)

        isAppInitialized = true
        return app
    }

    private fun getOrCreateFirebaseApp(appContext: Context): FirebaseApp {
        // 1. Check if DEFAULT FirebaseApp is already initialized
        val existingApps = try {
            FirebaseApp.getApps(appContext)
        } catch (_: Exception) {
            emptyList<FirebaseApp>()
        }

        val defaultApp = existingApps.firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
        if (defaultApp != null) {
            Log.i(TAG, "Reusing existing DEFAULT FirebaseApp: ${defaultApp.name}")
            return defaultApp
        }

        // 2. If any other named app exists (e.g. from previous activity instantiation), reuse it
        val existingNamedApp = existingApps.firstOrNull()
        if (existingNamedApp != null) {
            Log.i(TAG, "Reusing existing FirebaseApp instance: ${existingNamedApp.name}")
            return existingNamedApp
        }

        // 3. Try standard auto-init from google-services configuration if available
        try {
            val app = FirebaseApp.initializeApp(appContext)
            if (app != null) {
                Log.i(TAG, "FirebaseApp initialized via default google-services configuration")
                return app
            }
        } catch (e: Exception) {
            Log.d(TAG, "Standard initialization via google-services note: ${e.message}")
        }

        // 4. Initialize DEFAULT FirebaseApp with provisioned options
        val apiKey = try {
            val resId = appContext.resources.getIdentifier("google_api_key", "string", appContext.packageName)
            if (resId != 0) appContext.getString(resId) else "AIzaSyApDuJQqMdykixz6iuMxg5Vpdh0imQjuU0"
        } catch (_: Exception) {
            "AIzaSyApDuJQqMdykixz6iuMxg5Vpdh0imQjuU0"
        }

        val projectId = try {
            val resId = appContext.resources.getIdentifier("project_id", "string", appContext.packageName)
            if (resId != 0) appContext.getString(resId) else "gen-lang-client-0471500265"
        } catch (_: Exception) {
            "gen-lang-client-0471500265"
        }

        val appId = try {
            val resId = appContext.resources.getIdentifier("google_app_id", "string", appContext.packageName)
            if (resId != 0) appContext.getString(resId) else "1:341482481757:android:784aa21f7f6521025345b4"
        } catch (_: Exception) {
            "1:341482481757:android:784aa21f7f6521025345b4"
        }

        val options = FirebaseOptions.Builder()
            .setProjectId(projectId)
            .setApplicationId(appId)
            .setApiKey(apiKey)
            .setStorageBucket("$projectId.firebasestorage.app")
            .build()

        return try {
            // Initialize without a custom name so it becomes the DEFAULT FirebaseApp
            FirebaseApp.initializeApp(appContext, options).also {
                Log.i(TAG, "FirebaseApp initialized as DEFAULT with project configuration: ${it.options.projectId}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice during initializeApp with options: ${e.message}")
            try {
                FirebaseApp.getApps(appContext).firstOrNull()
                    ?: FirebaseApp.getInstance()
            } catch (fallbackEx: Exception) {
                Log.w(TAG, "Fallback retrieval notice: ${fallbackEx.message}")
                try {
                    FirebaseApp.initializeApp(appContext, options, "LavinaFallbackApp")
                } catch (_: Exception) {
                    FirebaseApp.getApps(appContext).firstOrNull()
                        ?: throw IllegalStateException("Could not initialize or retrieve FirebaseApp", fallbackEx)
                }
            }
        }
    }

    private fun initAuth(app: FirebaseApp) {
        authInstance = try {
            val auth = FirebaseAuth.getInstance(app)
            try {
                auth.useAppLanguage()
            } catch (e: Exception) {
                Log.d(TAG, "Auth language note: ${e.message}")
            }
            Log.i(TAG, "FirebaseAuth initialized successfully. Current user: ${auth.currentUser?.uid ?: "Anonymous/None"}")
            auth
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth configuration notice: ${e.message}")
            try {
                FirebaseAuth.getInstance(app)
            } catch (fallbackEx: Exception) {
                Log.w(TAG, "Default FirebaseAuth fallback note: ${fallbackEx.message}")
                null
            }
        }
    }

    private fun initFirestore(app: FirebaseApp) {
        val appContext = app.applicationContext
        val dbId = try {
            val resId = appContext.resources.getIdentifier("firestore_database_id", "string", appContext.packageName)
            if (resId != 0) appContext.getString(resId) else null
        } catch (_: Exception) {
            null
        }

        firestoreInstance = try {
            val firestore = if (!dbId.isNullOrBlank()) {
                FirebaseFirestore.getInstance(app, dbId)
            } else {
                FirebaseFirestore.getInstance(app)
            }
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                firestore.firestoreSettings = settings
                Log.i(TAG, "FirebaseFirestore initialized with database: ${dbId ?: "(default)"}")
            } catch (settingsEx: Exception) {
                // If firestore was already started in this process, settings cannot be mutated; safely ignored
                Log.d(TAG, "FirebaseFirestore settings persistence already configured: ${settingsEx.message}")
            }
            firestore
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore configuration notice: ${e.message}")
            try {
                if (!dbId.isNullOrBlank()) FirebaseFirestore.getInstance(app, dbId) else FirebaseFirestore.getInstance(app)
            } catch (fallbackEx: Exception) {
                Log.w(TAG, "Default FirebaseFirestore fallback note: ${fallbackEx.message}")
                null
            }
        }
    }

    /**
     * Returns the initialized FirebaseAuth instance.
     */
    fun getAuth(): FirebaseAuth {
        authInstance?.let { return it }
        return synchronized(this) {
            authInstance ?: run {
                val app = firebaseApp ?: runCatching { FirebaseApp.getInstance() }.getOrNull()
                val auth = if (app != null) {
                    runCatching { FirebaseAuth.getInstance(app) }.getOrNull()
                } else null ?: runCatching { FirebaseAuth.getInstance() }.getOrNull()

                auth?.also { authInstance = it }
                    ?: throw IllegalStateException("FirebaseAuth has not been initialized. Call FirebaseInitializer.initialize(context) first.")
            }
        }
    }

    /**
     * Returns the initialized FirebaseFirestore instance.
     */
    fun getFirestore(): FirebaseFirestore {
        firestoreInstance?.let { return it }
        return synchronized(this) {
            firestoreInstance ?: run {
                val app = firebaseApp ?: runCatching { FirebaseApp.getInstance() }.getOrNull()
                val firestore = if (app != null) {
                    runCatching { FirebaseFirestore.getInstance(app) }.getOrNull()
                } else null ?: runCatching { FirebaseFirestore.getInstance() }.getOrNull()

                firestore?.also { firestoreInstance = it }
                    ?: throw IllegalStateException("FirebaseFirestore has not been initialized. Call FirebaseInitializer.initialize(context) first.")
            }
        }
    }

    /**
     * Checks if Firebase has been initialized.
     */
    fun isInitialized(): Boolean = isAppInitialized && authInstance != null && firestoreInstance != null

    /**
     * Convenience property to access current Firebase user.
     */
    val currentUser: FirebaseUser?
        get() = authInstance?.currentUser

    /**
     * Convenience property to access current user's UID.
     */
    val currentUserId: String?
        get() = authInstance?.currentUser?.uid

    /**
     * Helper to sign out the current user across Firebase Auth components.
     */
    fun signOut() {
        authInstance?.signOut()
        Log.i(TAG, "User signed out from Firebase Auth")
    }

    /**
     * Extension helper to await Google Play Tasks as Coroutines safely.
     */
    suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) continuation.resumeWithException(exception)
        }
        addOnCanceledListener {
            if (continuation.isActive) continuation.cancel()
        }
    }
}
