package de.haberland.meitueftler;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.text.InputFilter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import de.haberland.meitueftler.game.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class MainActivity extends ComponentActivity {
    static final int INK=Color.rgb(35,63,64), TEAL=Color.rgb(20,125,120), CREAM=Color.rgb(247,242,232), ORANGE=Color.rgb(218,99,57);
    Level current;
    final List<Ramp> build=new ArrayList<>();
    int selected=-1;
    PhysicsEngine engine;
    boolean hintVisible, hintUsed, paused;
    private boolean sound=true, finishedHandled;
    private String screen="menu";
    private String inventionId;
    private Button powerButton, channelButton;
    private SharedPreferences prefs;
    private final Deque<String> history=new ArrayDeque<>();
    private WorkshopView board;
    private Button runButton, hintButton, soundButton;
    private TextView selectionText, statusText;
    private final List<Button> editButtons=new ArrayList<>();
    private ToneGenerator tones;
    private PlayUpdates updates;
    private LinearLayout root;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("workshop",MODE_PRIVATE);
        updates=new PlayUpdates(this,this::saveBuild);
        sound=prefs.getBoolean("sound",true);
        Diagnostics.start(this,prefs.getBoolean("diagnostics",false));
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        try { tones=new ToneGenerator(AudioManager.STREAM_MUSIC,35); } catch(RuntimeException ignored) { tones=null; }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { goBack(); }
        });
        if(state!=null && state.containsKey("level")) {
            current=LevelCatalog.get(state.getInt("level"));
            decodeBuild(state.getString("build","[]"));
            hintUsed=state.getBoolean("hintUsed");
            inventionId=state.getString("inventionId");
            screen="game";
        }
        rebuildScreen();
    }
    @Override public void onConfigurationChanged(Configuration config) { super.onConfigurationChanged(config);rebuildScreen(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if(current!=null && screen.equals("game")) { out.putInt("level",current.id);out.putString("build",encodeBuild());out.putBoolean("hintUsed",hintUsed);out.putString("inventionId",inventionId); }
    }
    @Override protected void onPause() { paused=true;saveBuild();super.onPause(); }
    @Override protected void onResume() { super.onResume();paused=false;if(board!=null)board.resumeDrawing();if(updates!=null)updates.resume(); }
    @Override protected void onDestroy() { if(tones!=null) tones.release();if(updates!=null)updates.close();super.onDestroy(); }
    private void goBack() { if(screen.equals("menu")) finish();else { saveBuild();engine=null;current=null;screen="menu";rebuildScreen(); } }

    int dp(float value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v; }
    private LinearLayout row() { LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v; }
    private TextView text(String value,int size,boolean bold) {
        TextView t=new TextView(this);t.setText(value);t.setTextColor(INK);t.setTextSize(size);
        if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        t.setGravity(Gravity.CENTER_VERTICAL);return t;
    }
    private Button button(String title,boolean primary,Runnable action) {
        Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(15);b.setMinHeight(dp(48));b.setMinimumWidth(0);b.setMinWidth(0);
        b.setTextColor(primary?Color.WHITE:INK);b.setPadding(dp(8),dp(4),dp(8),dp(4));
        GradientDrawable bg=new GradientDrawable();bg.setColor(primary?TEAL:Color.WHITE);bg.setCornerRadius(dp(14));bg.setStroke(dp(1),primary?TEAL:Color.rgb(214,222,211));
        android.graphics.drawable.RippleDrawable ripple=new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x22000000),bg,null);
        b.setBackground(ripple);b.setOnClickListener(v->action.run());return b;
    }
    private void rebuildScreen() {
        editButtons.clear();board=null;
        root=column();root.setBackgroundColor(CREAM);root.setPadding(dp(12),dp(8),dp(12),dp(8));
        setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                v.setPadding(bars.left+dp(12),bars.top+dp(8),bars.right+dp(12),bars.bottom+dp(8));
            }
            return insets;
        });
        root.requestApplyInsets();
        if(screen.equals("game")) showWorkshop(); else if(screen.equals("levels")) showLevels();else if(screen.equals("inventions"))showInventions();else showMenu();
    }
    private void showMenu() {
        TextView eyebrow=text("BAUEN · TESTEN · STAUNEN",12,true);eyebrow.setTextColor(TEAL);root.addView(eyebrow);
        root.addView(text("MeiTüftler",36,true));
        root.addView(text("Deine verrückte Erfinderwerkstatt",17,false));
        WorkshopView hero=new WorkshopView(this,this,true);
        root.addView(hero,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout choices=row();
        Button tasks=button(LevelCatalog.LEVELS.size()+" Murmel-Aufgaben",true,()->{screen="levels";rebuildScreen();});
        Button free=button("Freier Bauplatz",false,()->openLevel(-1));
        addEqual(choices,tasks);addEqual(choices,free);root.addView(choices);
        Button gallery=button("Meine Erfindungen",false,()->{screen="inventions";rebuildScreen();});
        root.addView(gallery,new LinearLayout.LayoutParams(-1,dp(48)));
        Button parents=button("Für Eltern · Info",false,this::parentGate);
        root.addView(parents,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView footer=text("Ohne Zeitdruck. Jede Idee darf ausprobiert werden.   ·   "+totalStars()+" / "+(LevelCatalog.LEVELS.size()*3)+" Sterne",13,false);
        footer.setPadding(0,dp(10),0,0);root.addView(footer);
    }
    private static String symbols(char symbol,int count) { StringBuilder value=new StringBuilder();for(int i=0;i<count;i++)value.append(symbol);return value.toString(); }
    private int totalStars() { int total=0;for(Level l:LevelCatalog.LEVELS)total+=prefs.getInt("stars_"+l.id,0);return total; }
    private void showLevels() {
        LinearLayout header=row();header.addView(button("‹ Zurück",false,this::goBack));
        TextView title=text("  Deine Murmel-Aufgaben",24,true);header.addView(title,new LinearLayout.LayoutParams(0,dp(56),1));root.addView(header);
        TextView intro=text("Alle Aufgaben sind offen. Wähle, worauf du Lust hast.",15,false);intro.setPadding(0,dp(4),0,dp(8));root.addView(intro);
        ScrollView scroll=new ScrollView(this);LinearLayout grid=column();
        for(int i=0;i<LevelCatalog.LEVELS.size();i+=2) {
            LinearLayout line=row();
            for(int j=i;j<Math.min(i+2,LevelCatalog.LEVELS.size());j++) {
                Level l=LevelCatalog.LEVELS.get(j);int stars=prefs.getInt("stars_"+l.id,0);
                Button b=button((l.id+1)+" · "+l.name+"\n"+(stars==0?"Eine neue Idee wartet":symbols('★',stars)+symbols('☆',3-stars)),false,()->openLevel(l.id));
                b.setTextSize(16);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(76),1);lp.setMargins(dp(3),dp(4),dp(3),dp(4));line.addView(b,lp);
            }
            grid.addView(line);
        }
        scroll.addView(grid);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    }
    private void openLevel(int id) {
        saveBuild();inventionId=null;current=LevelCatalog.get(id);engine=null;selected=-1;history.clear();hintUsed=false;hintVisible=false;finishedHandled=false;
        decodeBuild(prefs.getString("build_"+id,"[]"));screen="game";rebuildScreen();
    }
    private void showWorkshop() {
        LinearLayout header=row();header.addView(button("‹ Menü",false,this::goBack));
        TextView title=text("  "+(current.sandbox()?"":(current.id+1)+" · ")+(current.sandbox()?inventionName():current.name),21,true);
        title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);header.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));
        if(current.sandbox())header.addView(button("Speichern",false,this::saveInventionDialog));
        root.addView(header);
        boolean landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout body=new LinearLayout(this);body.setOrientation(landscape?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        board=new WorkshopView(this,this,false);
        body.addView(board,landscape?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll=new ScrollView(this);LinearLayout tools=column();tools.setPadding(dp(10),dp(4),dp(4),dp(4));
        TextView task=text(current.task,15,false);task.setPadding(0,0,0,dp(8));tools.addView(task);
        selectionText=text("Tippe ein Bauteil an.",13,true);selectionText.setTextColor(TEAL);tools.addView(selectionText);
        toolRow(tools,editButton("＋ Brett",()->addElement(Ramp.Kind.PLANK)),editButton("↶ Zurück",this::undo));
        toolRow(tools,editButton("＋ Trampolin",()->addElement(Ramp.Kind.TRAMPOLINE)),editButton("＋ Block",()->addElement(Ramp.Kind.BLOCK)));
        powerButton=editButton("Stärke: mittel",this::cyclePower);
        toolRow(tools,editButton("＋ Ventilator",()->addElement(Ramp.Kind.FAN)),powerButton);
        toolRow(tools,editButton("＋ Schalter",()->addElement(Ramp.Kind.SWITCH)),editButton("＋ Tür",()->addElement(Ramp.Kind.DOOR)));
        channelButton=editButton("Verbindung wählen",this::cycleChannel);tools.addView(channelButton,new LinearLayout.LayoutParams(-1,dp(48)));
        toolRow(tools,editButton("↶ Drehen",()->changeRamp(-5,0)),editButton("Drehen ↷",()->changeRamp(5,0)));
        toolRow(tools,editButton("− Kürzer",()->changeRamp(0,-40)),editButton("＋ Länger",()->changeRamp(0,40)));
        toolRow(tools,editButton("Entfernen",this::removeRamp),editButton("Neu bauen",this::clearBuild));
        hintButton=button("Hinweis",false,()->{hintVisible=!hintVisible;if(hintVisible)hintUsed=true;updateControls();board.invalidate();});
        soundButton=button(sound?"Ton: an":"Ton: aus",false,()->{sound=!sound;prefs.edit().putBoolean("sound",sound).apply();updateControls();});
        toolRow(tools,hintButton,soundButton);
        runButton=button("▶ Ausprobieren",true,this::toggleSimulation);LinearLayout.LayoutParams runLp=new LinearLayout.LayoutParams(-1,dp(56));runLp.setMargins(0,dp(6),0,dp(6));
        statusText=text("Bauteil antippen und verschieben. Luftkegel zeigt die Blasrichtung.",13,false);tools.addView(statusText);
        scroll.addView(tools);
        if(landscape) { LinearLayout side=column();side.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));side.addView(runButton,runLp);body.addView(side,new LinearLayout.LayoutParams(dp(244),-1)); }
        else body.addView(scroll,new LinearLayout.LayoutParams(-1,dp(246)));
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        if(!landscape)root.addView(runButton,runLp);
        updateControls();
    }
    private void addEqual(LinearLayout line,View v) { LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(56),1);lp.setMargins(dp(3),dp(3),dp(3),dp(3));line.addView(v,lp); }
    private void toolRow(LinearLayout parent,Button a,Button b) { LinearLayout line=row();addEqual(line,a);addEqual(line,b);parent.addView(line); }
    private Button editButton(String label,Runnable action) { Button b=button(label,false,action);editButtons.add(b);return b; }
    void rememberBuild() { history.addLast(encodeBuild());while(history.size()>30)history.removeFirst(); }
    void constructionChanged() { saveBuild();updateControls();if(board!=null)board.invalidate(); }
    private void addElement(Ramp.Kind kind) {
        if(build.size()>=current.maxRamps) { Toast.makeText(this,"Alle Bauteile sind im Einsatz. Verschiebe oder entferne eines.",Toast.LENGTH_SHORT).show();return; }
        rememberBuild();build.add(new Ramp(500,300,kind==Ramp.Kind.PLANK?480:kind==Ramp.Kind.FAN?320:160,kind==Ramp.Kind.PLANK?20:kind==Ramp.Kind.FAN?-60:0,false,kind));selected=build.size()-1;constructionChanged();
    }
    private void changeRamp(int angle,int length) {
        if(selected<0||selected>=build.size()) { Toast.makeText(this,"Tippe zuerst ein Bauteil an.",Toast.LENGTH_SHORT).show();return; }
        rememberBuild();Ramp r=build.get(selected);r.angle=r.kind==Ramp.Kind.FAN?Ramp.normalizeAngle(r.angle+angle):Math.max(-85,Math.min(85,r.angle+angle));r.length=Math.max(160,Math.min(560,r.length+length));r.clamp();constructionChanged();
    }
    private void cycleChannel() {
        if(selected<0||selected>=build.size()||!build.get(selected).linked())return;
        rememberBuild();Ramp r=build.get(selected);r.channel=r.channel%4+1;constructionChanged();
    }
    private void cyclePower() {
        if(selected<0||selected>=build.size()||build.get(selected).kind!=Ramp.Kind.FAN) {
            Toast.makeText(this,"Tippe zuerst einen Ventilator an.",Toast.LENGTH_SHORT).show();return;
        }
        rememberBuild();Ramp r=build.get(selected);r.power=r.power%3+1;constructionChanged();
    }
    private void removeRamp() { if(selected<0||selected>=build.size())return;rememberBuild();build.remove(selected);selected=-1;constructionChanged(); }
    private void undo() { if(history.isEmpty())return;decodeBuild(history.removeLast());selected=-1;constructionChanged(); }
    private void clearBuild() {
        if(build.isEmpty())return;
        new AlertDialog.Builder(this).setTitle("Neu bauen?").setMessage("Die Bauteile entfernen und mit einer neuen Idee anfangen?")
            .setNegativeButton("Behalten",null).setPositiveButton("Neu bauen",(d,w)->{rememberBuild();build.clear();selected=-1;constructionChanged();}).show();
    }
    private void toggleSimulation() {
        if(engine!=null) { engine=null;finishedHandled=false;updateControls();board.resumeDrawing();return; }
        engine=new PhysicsEngine(current,build);finishedHandled=false;hintVisible=false;updateControls();board.resumeDrawing();playTone(ToneGenerator.TONE_PROP_BEEP,80);
    }
    void simulationFinished() {
        if(finishedHandled || engine==null || engine.state==PhysicsEngine.State.RUNNING || isFinishing())return;finishedHandled=true;updateControls();
        if(engine.state==PhysicsEngine.State.WON) {
            playTone(ToneGenerator.TONE_PROP_ACK,220);
            int stars=1+(build.size()<=current.bonusRamps?1:0)+(!hintUsed?1:0);
            if(!current.sandbox())prefs.edit().putInt("stars_"+current.id,Math.max(stars,prefs.getInt("stars_"+current.id,0))).apply();
            AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle(current.sandbox()?"Deine Maschine funktioniert!":"Geschafft!  "+symbols('★',stars))
                .setMessage(current.discovery+(current.sandbox()?"":"\n\nExtra-Sterne: höchstens "+current.bonusRamps+" "+(current.bonusRamps==1?"Bauteil":"Bauteile")+" und ohne Hinweis lösen."))
                .setPositiveButton("Weiter tüfteln",(d,w)->toggleSimulation());
            if(!current.sandbox()&&current.id<LevelCatalog.LEVELS.size()-1)dialog.setNegativeButton("Nächste Aufgabe",(d,w)->openLevel(current.id+1));
            dialog.setOnCancelListener(d->{if(engine!=null)toggleSimulation();});dialog.show();
        }
    }
    private void playTone(int tone,int duration) { if(sound&&tones!=null)try{tones.startTone(tone,duration);}catch(RuntimeException ignored){} }
    void updateControls() {
        if(runButton==null||current==null)return;
        boolean editing=engine==null;
        for(Button b:editButtons){b.setEnabled(editing);b.setAlpha(editing?1f:0.45f);}
        hintButton.setEnabled(editing);hintButton.setText(hintVisible?"Hinweis aus":"Hinweis");soundButton.setText(sound?"Ton: an":"Ton: aus");
        runButton.setText(editing?"▶ Ausprobieren":"↶ Weiterbauen");
        String label="Bauteile: "+build.size()+" / "+current.maxRamps;
        if(selected>=0&&selected<build.size()) { Ramp r=build.get(selected);label=r.label()+" "+(selected+1)+" · "+(int)r.angle+"° · "+(int)r.length; }
        boolean fanSelected=selected>=0&&selected<build.size()&&build.get(selected).kind==Ramp.Kind.FAN;
        powerButton.setText(fanSelected?"Stärke: "+build.get(selected).powerLabel():"Stärke wählen");
        powerButton.setEnabled(editing&&fanSelected);powerButton.setAlpha(editing&&fanSelected?1f:0.45f);
        boolean linked=selected>=0&&selected<build.size()&&build.get(selected).linked();
        channelButton.setText(linked?"Verbindung: "+build.get(selected).channelLabel():"Verbindung wählen");
        channelButton.setEnabled(editing&&linked);channelButton.setAlpha(editing&&linked?1f:0.45f);
        selectionText.setText(label);
        if(hintVisible)statusText.setText(current.hint);
        else if(engine==null)statusText.setText("Bauteil antippen und verschieben. Luftkegel zeigt die Blasrichtung.");
        else if(engine.state==PhysicsEngine.State.RETRY)statusText.setText("Noch nicht im Korb? Verändere ein Bauteil und probiere es wieder.");
        else if(engine.state==PhysicsEngine.State.WON)statusText.setText("Deine Idee hat funktioniert!");
        else statusText.setText("Beobachte die Murmel. Was passiert am nächsten Bauteil?");
    }
    private String encodeBuild() {
        JSONArray array=new JSONArray();
        for(Ramp r:build)try { JSONObject o=new JSONObject();o.put("x",r.x);o.put("y",r.y);o.put("length",r.length);o.put("angle",r.angle);o.put("kind",r.kind.name());o.put("power",r.power);o.put("channel",r.channel);array.put(o); }catch(JSONException ignored){}
        return array.toString();
    }
    private void decodeBuild(String json) {
        build.clear();
        try { JSONArray array=new JSONArray(json);for(int i=0;i<Math.min(array.length(),current.maxRamps);i++) {
            JSONObject o=array.getJSONObject(i);double x=o.getDouble("x"),y=o.getDouble("y"),length=o.getDouble("length"),angle=o.getDouble("angle");
            if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(length)||!Double.isFinite(angle))continue;
            Ramp.Kind kind=Ramp.Kind.PLANK;
            try { kind=Ramp.Kind.valueOf(o.optString("kind","PLANK")); }catch(IllegalArgumentException ignored){}
            Ramp r=new Ramp(x,y,Math.max(160,Math.min(560,length)),kind==Ramp.Kind.FAN?Ramp.normalizeAngle(angle):Math.max(-85,Math.min(85,angle)),false,kind);
            r.channel=Math.max(1,Math.min(4,o.optInt("channel",1)));r.power=Math.max(1,Math.min(3,o.optInt("power",2)));r.clamp();build.add(r);
        }}catch(JSONException ignored){build.clear();}
    }
    private JSONArray inventions() {
        try { return new JSONArray(prefs.getString("inventions","[]")); }catch(JSONException ignored){return new JSONArray();}
    }
    private String inventionName() {
        JSONArray saved=inventions();
        for(int i=0;i<saved.length();i++){JSONObject item=saved.optJSONObject(i);if(item!=null&&item.optString("id").equals(inventionId))return item.optString("name",current.name);}
        return current.name;
    }
    private void showInventions() {
        LinearLayout header=row();header.addView(button("‹ Zurück",false,this::goBack));
        header.addView(text("  Meine Erfindungen",24,true));root.addView(header);
        TextView intro=text("Deine Maschinen zum Weiterbauen. Speichere sie im freien Bauplatz.",16,false);
        intro.setPadding(0,dp(12),0,dp(12));root.addView(intro);
        ScrollView scroll=new ScrollView(this);LinearLayout list=column();JSONArray saved=inventions();
        if(saved.length()==0)list.addView(text("Hier ist Platz für deine erste Erfindung!",18,true));
        for(int i=0;i<saved.length();i++) {
            JSONObject item=saved.optJSONObject(i);if(item==null)continue;
            LinearLayout line=row();Button load=button(item.optString("name","Meine Maschine"),false,()->loadInvention(item));
            line.addView(load,new LinearLayout.LayoutParams(0,dp(64),1));
            Button remove=button("Löschen",false,()->deleteInvention(item.optString("id"),item.optString("name")));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(96),dp(52));lp.setMargins(dp(8),0,0,0);line.addView(remove,lp);
            LinearLayout.LayoutParams lineLp=new LinearLayout.LayoutParams(-1,dp(76));list.addView(line,lineLp);
        }
        scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(button("Zum freien Bauplatz",true,()->openLevel(-1)),new LinearLayout.LayoutParams(-1,dp(56)));
    }
    private void loadInvention(JSONObject item) {
        saveBuild();current=LevelCatalog.SANDBOX;engine=null;selected=-1;history.clear();hintUsed=false;hintVisible=false;finishedHandled=false;
        inventionId=item.optString("id");decodeBuild(item.optString("build","[]"));saveBuild();screen="game";rebuildScreen();
    }
    private void saveInventionDialog() {
        if(engine!=null){Toast.makeText(this,"Tippe zuerst auf Weiterbauen.",Toast.LENGTH_SHORT).show();return;}
        JSONArray saved=inventions();String name="Meine Maschine "+(saved.length()+1);boolean replacing=false;
        for(int i=0;i<saved.length();i++){JSONObject item=saved.optJSONObject(i);if(item!=null&&item.optString("id").equals(inventionId)){name=item.optString("name");replacing=true;break;}}
        EditText input=new EditText(this);input.setSingleLine(true);input.setText(name);input.setSelectAllOnFocus(true);
        input.setContentDescription("Name der Erfindung");input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        input.setPadding(dp(16),dp(12),dp(16),dp(12));
        AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle("Erfindung speichern").setView(input)
            .setNegativeButton("Abbrechen",null).setPositiveButton(replacing?"Aktualisieren":"Speichern",(d,w)->storeInvention(input.getText().toString(),false));
        if(replacing)dialog.setNeutralButton("Als Kopie",(d,w)->storeInvention(input.getText().toString(),true));
        dialog.show();
    }
    private void storeInvention(String name,boolean copy) {
        name=name.trim();if(name.isEmpty()){Toast.makeText(this,"Gib deiner Erfindung einen Namen.",Toast.LENGTH_SHORT).show();return;}
        if(copy&&name.equals(inventionName()))name=name.substring(0,Math.min(32,name.length()))+" (Kopie)";
        JSONArray saved=inventions();String id=copy||inventionId==null?java.util.UUID.randomUUID().toString():inventionId;
        boolean replaced=false;
        try {
            JSONObject item=new JSONObject();item.put("id",id);item.put("name",name);item.put("build",encodeBuild());
            for(int i=0;i<saved.length();i++){JSONObject old=saved.optJSONObject(i);if(old!=null&&old.optString("id").equals(id)){saved.put(i,item);replaced=true;break;}}
            if(!replaced){if(saved.length()>=20){Toast.makeText(this,"20 Erfindungen gespeichert. Öffne eine zum Aktualisieren oder lösche eine in der Galerie.",Toast.LENGTH_LONG).show();return;}saved.put(item);}
            prefs.edit().putString("inventions",saved.toString()).apply();inventionId=id;
            Toast.makeText(this,"Erfindung gespeichert",Toast.LENGTH_SHORT).show();rebuildScreen();
        }catch(JSONException ignored){Toast.makeText(this,"Speichern hat nicht geklappt.",Toast.LENGTH_SHORT).show();}
    }
    private void deleteInvention(String id,String name) {
        new AlertDialog.Builder(this).setTitle("Erfindung löschen?").setMessage(name+" aus der Galerie entfernen?")
            .setNegativeButton("Behalten",null).setPositiveButton("Löschen",(d,w)->{
                JSONArray saved=inventions();for(int i=saved.length()-1;i>=0;i--){JSONObject item=saved.optJSONObject(i);if(item!=null&&item.optString("id").equals(id))saved.remove(i);}
                prefs.edit().putString("inventions",saved.toString()).apply();rebuildScreen();
            }).show();
    }
    private void parentGate() {
        int a=3+new java.util.Random().nextInt(7),b=3+new java.util.Random().nextInt(7);
        EditText answer=new EditText(this);answer.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        answer.setContentDescription("Antwort für Eltern");
        new AlertDialog.Builder(this).setTitle("Für Eltern").setMessage("Bitte eine erwachsene Person fragen. Wie viel ist "+a+" × "+b+"?")
            .setView(answer).setNegativeButton("Abbrechen",null).setPositiveButton("Weiter",(d,w)->{
                if(answer.getText().toString().trim().equals(String.valueOf(a*b)))parentInfo();
                else Toast.makeText(this,"Bitte eine erwachsene Person fragen.",Toast.LENGTH_SHORT).show();
            }).show();
    }
    private void parentInfo() {
        boolean enabled=prefs.getBoolean("diagnostics",false)&&Diagnostics.configured();
        String description="MeiTüftler "+BuildConfig.VERSION_NAME+" · Peter\n\nSpielstände und Erfindungen bleiben auf diesem Gerät. Keine Werbung oder Konten.\n\nOptionale Absturzdiagnose: Nach Zustimmung sendet Firebase Crashlytics technische Absturzberichte an Google, darunter Stacktraces, App-/Android-Version, Gerätemodell und Installationskennungen. Erfindungsnamen und Bauwerke werden nicht als Diagnosedaten angehängt. Die Zustimmung ist freiwillig und kann hier widerrufen werden. Bereits gesendete Berichte werden dadurch nicht zurückgerufen.\n\nDiagnose: "+(Diagnostics.configured()?(enabled?"an":"aus"):"in diesem Build nicht eingerichtet");
        AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle("Info und Datenschutz").setMessage(description).setNegativeButton("Schließen",null);
        if(Diagnostics.configured())dialog.setPositiveButton(enabled?"Diagnose ausschalten":"Diagnose erlauben",(d,w)->{
            boolean value=!enabled;
            if(Diagnostics.start(this,value)){prefs.edit().putBoolean("diagnostics",value).apply();Toast.makeText(this,value?"Diagnose eingeschaltet":"Diagnose ausgeschaltet",Toast.LENGTH_SHORT).show();}
            else Toast.makeText(this,"Diagnose konnte nicht aktiviert werden.",Toast.LENGTH_SHORT).show();
        });
        if(!BuildConfig.DEBUG)dialog.setNeutralButton("Nach Updates suchen",(d,w)->updates.check());
        if(BuildConfig.DEBUG&&enabled)dialog.setNeutralButton("Test-Absturz",(d,w)->new AlertDialog.Builder(this)
            .setTitle("Diagnose testen?").setMessage("Die App wird absichtlich beendet. Danach erneut öffnen und den Bericht in Firebase prüfen.")
            .setNegativeButton("Abbrechen",null).setPositiveButton("Test auslösen",(x,y)->{throw new IllegalStateException("MeiTueftler manual Crashlytics test");}).show());
        dialog.show();
    }
    private void saveBuild() { if(current!=null&&prefs!=null)prefs.edit().putString("build_"+current.id,encodeBuild()).apply(); }
}
