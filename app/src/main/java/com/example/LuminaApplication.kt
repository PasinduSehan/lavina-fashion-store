package com.example

import android.app.Application
import android.util.Log
import com.example.data.firebase.FirebaseInitializer
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LuminaApplication : Application() {

    companion object {
        private const val TAG = "LuminaApplication"

        val auth: FirebaseAuth?
            get() = try { FirebaseInitializer.getAuth() } catch (e: Exception) { null }

        val firestore: FirebaseFirestore?
            get() = try { FirebaseInitializer.getFirestore() } catch (e: Exception) { null }
    }

    override fun onCreate() {
        super.onCreate()
        initFirebase()
    }

    private fun initFirebase() {
        try {
            FirebaseInitializer.initialize(this)
            Log.i(TAG, "Firebase initialized via FirebaseInitializer utility. Ready for Auth and Firestore operations.")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization notice: ${e.message}")
        }
    }
}
