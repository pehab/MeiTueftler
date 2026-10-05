package de.haberland.meitueftler;

import android.content.Context;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/** No Firebase initialization until an adult explicitly enables optional diagnostics. */
final class Diagnostics {
    private Diagnostics() {}
    static boolean configured() { return BuildConfig.FIREBASE_CONFIGURED; }
    static boolean start(Context context,boolean enabled) {
        if(!configured())return !enabled;
        try {
            if(!enabled) {
                if(!FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);
                    FirebaseCrashlytics.getInstance().deleteUnsentReports();
                }
                return true;
            }
            FirebaseApp app=FirebaseApp.initializeApp(context.getApplicationContext());
            if(app==null)return false;
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
            return true;
        } catch(RuntimeException failure) {
            android.util.Log.w("MeiTueftler","Optional diagnostics could not be initialized");
            return false;
        }
    }
}
