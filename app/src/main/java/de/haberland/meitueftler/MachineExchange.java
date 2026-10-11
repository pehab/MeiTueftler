package de.haberland.meitueftler;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.core.content.IntentCompat;
import de.haberland.meitueftler.exchange.MachineFile;
import de.haberland.meitueftler.exchange.MachineFile.Category;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Android document picker and share sheet around a validated, portable construction. */
final class MachineExchange {
    private final MainActivity owner;
    private final SharedPreferences prefs;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<Intent> importer,exporter;
    private String pendingExport;
    private MachineFile pendingImport;
    private boolean busy,closed;
    MachineExchange(MainActivity owner,SharedPreferences prefs) {
        this.owner=owner;this.prefs=prefs;
        importer=owner.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),result->{
            Intent data=result.getData();if(result.getResultCode()==MainActivity.RESULT_OK&&data!=null&&data.getData()!=null)read(data.getData());
        });
        exporter=owner.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),result->{
            String json=pendingExport;pendingExport=null;Intent data=result.getData();
            if(result.getResultCode()==MainActivity.RESULT_OK&&data!=null&&data.getData()!=null&&json!=null)write(data.getData(),json);
        });
    }
    void saveSession(Bundle state){state.putString("machineExport",pendingExport);if(pendingImport!=null)state.putString("machineImport",portable(pendingImport));}
    void restoreSession(Bundle state){pendingExport=state.getString("machineExport");String json=state.getString("machineImport");if(json!=null)try{pendingImport=MachineFile.decode(json);}catch(MachineFile.FormatException ignored){ }}
    void restorePreview(){if(pendingImport!=null)preview(pendingImport);}
    void addImport(LinearLayout root) {
        root.addView(owner.button("Maschine importieren",false,this::chooseImport),new LinearLayout.LayoutParams(-1,owner.dp(52)));
    }
    void chooseImport() {
        if(busy){toast("Eine Datei wird gerade verarbeitet.");return;}
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        try{importer.launch(intent);}catch(ActivityNotFoundException e){error("Auf diesem Gerät ist keine Dateiauswahl verfügbar.");}
    }
    void export(Category category,JSONObject saved) {
        if(busy){toast("Eine Datei wird gerade verarbeitet.");return;}
        try {
            String name=saved.optString("name");String json=MachineFile.encode(category,name,saved.optString("build","[]"));
            new AlertDialog.Builder(owner).setTitle("Maschine exportieren").setItems(new String[]{"Teilen","Als Datei speichern"},(dialog,which)->{
                if(which==0)share(name,json);else{pendingExport=json;Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(MachineFile.MIME).putExtra(Intent.EXTRA_TITLE,MachineFile.filename(name));
                    try{exporter.launch(intent);}catch(ActivityNotFoundException e){pendingExport=null;error("Auf diesem Gerät ist keine Dateiauswahl verfügbar.");}}
            }).show();
        }catch(MachineFile.FormatException e){error(e.getMessage());}
    }
    private void share(String name,String json) {
        busy=true;io.execute(()->{
            try {
                File root=new File(owner.getCacheDir(),"machine-exports");
                if(!root.exists()&&!root.mkdirs())throw new IOException("Cannot create export directory");
                // Keep grants usable for a day; remove only older completed exports.
                File[] old=root.listFiles();if(old!=null)for(File file:old)if(file.isFile()&&file.lastModified()<System.currentTimeMillis()-24*60*60*1000L)file.delete();
                File file=new File(root,UUID.randomUUID()+"-"+MachineFile.filename(name));
                try(OutputStream out=new FileOutputStream(file)){out.write(json.getBytes(StandardCharsets.UTF_8));}
                Uri uri=FileProvider.getUriForFile(owner,BuildConfig.APPLICATION_ID+".machines",file,MachineFile.filename(name));
                owner.runOnUiThread(()->{if(closed)return;busy=false;
                    Intent send=new Intent(Intent.ACTION_SEND).setType(MachineFile.MIME).putExtra(Intent.EXTRA_STREAM,uri).putExtra(Intent.EXTRA_SUBJECT,name)
                        .setClipData(ClipData.newUri(owner.getContentResolver(),name,uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try{owner.startActivity(Intent.createChooser(send,"Maschine teilen"));}catch(ActivityNotFoundException e){error("Keine App zum Teilen gefunden. Speichere die Maschine als Datei.");}
                });
            }catch(IOException|IllegalArgumentException|SecurityException e){failed("Die Maschine konnte nicht geteilt werden.");}
        });
    }
    private void write(Uri uri,String json) {
        if(!"content".equals(uri.getScheme())){error("Die Datei kann an diesem Ort nicht gespeichert werden.");return;}
        busy=true;io.execute(()->{
            try(OutputStream out=owner.getContentResolver().openOutputStream(uri,"wt")) {
                if(out==null)throw new IOException("No document stream");out.write(json.getBytes(StandardCharsets.UTF_8));
                owner.runOnUiThread(()->{if(!closed){busy=false;toast("Maschinendatei gespeichert");}});
            }catch(IOException|SecurityException|IllegalArgumentException e){failed("Die Datei konnte nicht gespeichert werden. Bitte einen anderen Speicherort wählen.");}
        });
    }
    private void read(Uri uri) {
        if(!"content".equals(uri.getScheme())){error("Bitte die Maschinendatei über die Dateiauswahl öffnen.");return;}
        if(busy){toast("Eine Datei wird gerade verarbeitet.");return;}
        busy=true;toast("Maschinendatei wird gelesen …");io.execute(()->{
            try(InputStream input=owner.getContentResolver().openInputStream(uri)) {
                if(input==null)throw new IOException("No document stream");MachineFile machine=MachineFile.read(input);
                owner.runOnUiThread(()->{if(!closed){busy=false;pendingImport=machine;preview(machine);}});
            }catch(MachineFile.FormatException e){failed(e.getMessage());}
            catch(IOException|SecurityException|IllegalArgumentException e){failed("Die Datei konnte nicht gelesen werden. Bitte herunterladen und über Maschine importieren erneut öffnen.");}
        });
    }
    void incoming(Intent intent) {
        if(intent==null)return;Uri uri=null;
        try {
        if(Intent.ACTION_VIEW.equals(intent.getAction()))uri=intent.getData();
        else if(Intent.ACTION_SEND.equals(intent.getAction())){
            uri=IntentCompat.getParcelableExtra(intent,Intent.EXTRA_STREAM,Uri.class);
            if(uri==null&&intent.getClipData()!=null&&intent.getClipData().getItemCount()>0)uri=intent.getClipData().getItemAt(0).getUri();
        }else return;
        if(uri==null){error("Keine Maschinendatei gefunden.");return;}
        read(uri);
        }catch(android.os.BadParcelableException|ClassCastException e){error("Keine gültige Maschinendatei gefunden.");}
    }
    private JSONArray gallery(Category category){try{return new JSONArray(prefs.getString(category.prefKey,"[]"));}catch(JSONException e){return new JSONArray();}}
    private void preview(MachineFile machine) {
        JSONArray gallery=gallery(machine.category);
        if(gallery.length()>=MachineFile.MAX_SAVED){pendingImport=null;error("In dieser Galerie sind bereits 20 Maschinen. Lösche zuerst einen Eintrag und importiere die Datei dann erneut.");return;}
        EditText name=new EditText(owner);name.setSingleLine(true);name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});name.setText(MachineFile.uniqueName(machine.name,gallery));name.setContentDescription("Name der importierten Maschine");
        AlertDialog dialog=new AlertDialog.Builder(owner).setTitle("Maschine importieren")
            .setMessage(machine.category.label+" · "+machine.partCount+" Bauteile\n\nUnter welchem Namen soll die neue Maschine gespeichert werden?")
            .setView(name).setNegativeButton("Abbrechen",(d,w)->pendingImport=null).setPositiveButton("Importieren",null).create();
        dialog.setOnCancelListener(d->pendingImport=null);dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String chosen;try{chosen=MachineFile.validName(name.getText().toString());}catch(MachineFile.FormatException e){name.setError(e.getMessage());return;}
            JSONArray current=gallery(machine.category);if(current.length()>=MachineFile.MAX_SAVED){error("Diese Galerie ist voll. Lösche zuerst eine Maschine.");return;}
            try {
                JSONObject item=new JSONObject();item.put("id",UUID.randomUUID().toString());item.put("name",MachineFile.uniqueName(chosen,current));item.put("build",machine.build);current.put(item);
                prefs.edit().putString(machine.category.prefKey,current.toString()).apply();pendingImport=null;dialog.dismiss();owner.navigate(machine.category.screen);toast("Maschine importiert");
            }catch(JSONException e){error("Die Maschine konnte nicht gespeichert werden.");}
        }));dialog.show();
    }
    private static String portable(MachineFile machine){try{return MachineFile.encode(machine.category,machine.name,machine.build);}catch(MachineFile.FormatException e){return null;}}
    private void failed(String message){owner.runOnUiThread(()->{if(!closed){busy=false;error(message);}});}
    private void error(String message){if(!closed&&!owner.isFinishing())new AlertDialog.Builder(owner).setTitle("Maschinendatei").setMessage(message).setPositiveButton("OK",null).show();}
    private void toast(String message){Toast.makeText(owner,message,Toast.LENGTH_LONG).show();}
    void close(){closed=true;io.shutdownNow();}
}
