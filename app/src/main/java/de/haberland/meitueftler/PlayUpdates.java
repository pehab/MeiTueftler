package de.haberland.meitueftler;

import android.app.AlertDialog;
import android.os.Build;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

/** Parent-initiated flexible Play updates; GitHub/debug builds never contact Play. */
final class PlayUpdates {
    private final ComponentActivity activity;
    private final Runnable save;
    private final ActivityResultLauncher<IntentSenderRequest> launcher;
    private AppUpdateManager manager;
    private boolean prompting;
    private final InstallStateUpdatedListener listener=state->{if(state.installStatus()==InstallStatus.DOWNLOADED)offerRestart();};
    PlayUpdates(ComponentActivity activity,Runnable save) {
        this.activity=activity;this.save=save;
        launcher=activity.registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),result->{
            if(result.getResultCode()==android.app.Activity.RESULT_OK)Toast.makeText(activity,"Update wird im Hintergrund geladen.",Toast.LENGTH_LONG).show();
        });
    }
    private boolean installedFromPlay() {
        if(BuildConfig.DEBUG)return false;
        try {
            String installer=Build.VERSION.SDK_INT>=30?activity.getPackageManager().getInstallSourceInfo(activity.getPackageName()).getInstallingPackageName():activity.getPackageManager().getInstallerPackageName(activity.getPackageName());
            return "com.android.vending".equals(installer);
        }catch(android.content.pm.PackageManager.NameNotFoundException ignored){return false;}
    }
    void check() {
        if(!installedFromPlay()){Toast.makeText(activity,"Updates gibt es für die über Google Play installierte App.",Toast.LENGTH_LONG).show();return;}
        if(manager==null){manager=AppUpdateManagerFactory.create(activity);manager.registerListener(listener); }
        manager.getAppUpdateInfo().addOnSuccessListener(activity,info->{
            if(activity.isFinishing()||activity.isDestroyed())return;
            if(info.installStatus()==InstallStatus.DOWNLOADED){offerRestart();return;}
            if(info.updateAvailability()==UpdateAvailability.UPDATE_AVAILABLE&&info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                save.run();
                if(!manager.startUpdateFlowForResult(info,launcher,AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()))showFailure();
            }else Toast.makeText(activity,"Keine neue Version verfügbar.",Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(activity,e->showFailure());
    }
    void close() { if(manager!=null)manager.unregisterListener(listener); }
    void resume() {
        if(manager!=null)manager.getAppUpdateInfo().addOnSuccessListener(activity,info->{if(info.installStatus()==InstallStatus.DOWNLOADED)offerRestart();});
    }
    private void offerRestart() {
        if(prompting||activity.isFinishing()||activity.isDestroyed())return;
        prompting=true;
        new AlertDialog.Builder(activity).setTitle("Update bereit")
            .setMessage("Die neue Version ist geladen. Jetzt speichern und neu starten?")
            .setNegativeButton("Später",null).setPositiveButton("Neu starten",(d,w)->{save.run();manager.completeUpdate().addOnFailureListener(activity,e->showFailure());})
            .setOnDismissListener(d->prompting=false).show();
    }
    private void showFailure(){Toast.makeText(activity,"Update konnte nicht geprüft werden. Bitte später erneut versuchen.",Toast.LENGTH_LONG).show();}
}
