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
    private SharedPreferences prefs;
    private final Deque<String> history=new ArrayDeque<>();
    private WorkshopView board;
    private Button runButton, hintButton, soundButton;
    private TextView selectionText, statusText;
    private final List<Button> editButtons=new ArrayList<>();
    private ToneGenerator tones;
    private LinearLayout root;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("workshop",MODE_PRIVATE);
        sound=prefs.getBoolean("sound",true);
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
            screen="game";
        }
        rebuildScreen();
    }
    @Override public void onConfigurationChanged(Configuration config) { super.onConfigurationChanged(config);rebuildScreen(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if(current!=null && screen.equals("game")) { out.putInt("level",current.id);out.putString("build",encodeBuild());out.putBoolean("hintUsed",hintUsed); }
    }
    @Override protected void onPause() { paused=true;saveBuild();super.onPause(); }
    @Override protected void onResume() { super.onResume();paused=false;if(board!=null)board.resumeDrawing(); }
    @Override protected void onDestroy() { if(tones!=null) tones.release();super.onDestroy(); }
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
        if(screen.equals("game")) showWorkshop(); else if(screen.equals("levels")) showLevels();else showMenu();
    }
    private void showMenu() {
        TextView eyebrow=text("BAUEN · TESTEN · STAUNEN",12,true);eyebrow.setTextColor(TEAL);root.addView(eyebrow);
        root.addView(text("MeiTüftler",36,true));
        root.addView(text("Deine verrückte Erfinderwerkstatt",17,false));
        WorkshopView hero=new WorkshopView(this,this,true);
        root.addView(hero,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout choices=row();
        Button tasks=button("10 Murmel-Aufgaben",true,()->{screen="levels";rebuildScreen();});
        Button free=button("Freier Bauplatz",false,()->openLevel(-1));
        addEqual(choices,tasks);addEqual(choices,free);root.addView(choices);
        TextView footer=text("Ohne Zeitdruck. Jede Idee darf ausprobiert werden.   ·   "+totalStars()+" / 30 Sterne",13,false);
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
        saveBuild();current=LevelCatalog.get(id);engine=null;selected=-1;history.clear();hintUsed=false;hintVisible=false;finishedHandled=false;
        decodeBuild(prefs.getString("build_"+id,"[]"));screen="game";rebuildScreen();
    }
    private void showWorkshop() {
        LinearLayout header=row();header.addView(button("‹ Menü",false,this::goBack));
        TextView title=text("  "+(current.sandbox()?"":(current.id+1)+" · ")+current.name,21,true);
        title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);header.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(header);
        boolean landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout body=new LinearLayout(this);body.setOrientation(landscape?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        board=new WorkshopView(this,this,false);
        body.addView(board,landscape?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll=new ScrollView(this);LinearLayout tools=column();tools.setPadding(dp(10),dp(4),dp(4),dp(4));
        TextView task=text(current.task,15,false);task.setPadding(0,0,0,dp(8));tools.addView(task);
        selectionText=text("Tippe ein Brett an.",13,true);selectionText.setTextColor(TEAL);tools.addView(selectionText);
        toolRow(tools,editButton("＋ Brett",this::addRamp),editButton("↶ Zurück",this::undo));
        toolRow(tools,editButton("↶ Drehen",()->changeRamp(-5,0)),editButton("Drehen ↷",()->changeRamp(5,0)));
        toolRow(tools,editButton("− Kürzer",()->changeRamp(0,-40)),editButton("＋ Länger",()->changeRamp(0,40)));
        toolRow(tools,editButton("Entfernen",this::removeRamp),editButton("Neu bauen",this::clearBuild));
        hintButton=button("Hinweis",false,()->{hintVisible=!hintVisible;if(hintVisible)hintUsed=true;updateControls();board.invalidate();});
        soundButton=button(sound?"Ton: an":"Ton: aus",false,()->{sound=!sound;prefs.edit().putBoolean("sound",sound).apply();updateControls();});
        toolRow(tools,hintButton,soundButton);
        runButton=button("▶ Ausprobieren",true,this::toggleSimulation);LinearLayout.LayoutParams runLp=new LinearLayout.LayoutParams(-1,dp(56));runLp.setMargins(0,dp(6),0,dp(6));if(landscape)tools.addView(runButton,runLp);
        statusText=text("Brett antippen und mit dem Finger verschieben.",13,false);tools.addView(statusText);
        scroll.addView(tools);body.addView(scroll,landscape?new LinearLayout.LayoutParams(dp(244),-1):new LinearLayout.LayoutParams(-1,dp(246)));
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        if(!landscape)root.addView(runButton,runLp);
        updateControls();
    }
    private void addEqual(LinearLayout line,View v) { LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(56),1);lp.setMargins(dp(3),dp(3),dp(3),dp(3));line.addView(v,lp); }
    private void toolRow(LinearLayout parent,Button a,Button b) { LinearLayout line=row();addEqual(line,a);addEqual(line,b);parent.addView(line); }
    private Button editButton(String label,Runnable action) { Button b=button(label,false,action);editButtons.add(b);return b; }
    void rememberBuild() { history.addLast(encodeBuild());while(history.size()>30)history.removeFirst(); }
    void constructionChanged() { saveBuild();updateControls();if(board!=null)board.invalidate(); }
    private void addRamp() {
        if(build.size()>=current.maxRamps) { Toast.makeText(this,"Alle Bretter sind im Einsatz. Verschiebe oder entferne eines.",Toast.LENGTH_SHORT).show();return; }
        rememberBuild();build.add(new Ramp(500,300,480,20,false));selected=build.size()-1;constructionChanged();
    }
    private void changeRamp(int angle,int length) {
        if(selected<0||selected>=build.size()) { Toast.makeText(this,"Tippe zuerst ein Brett an.",Toast.LENGTH_SHORT).show();return; }
        rememberBuild();Ramp r=build.get(selected);r.angle=Math.max(-85,Math.min(85,r.angle+angle));r.length=Math.max(160,Math.min(560,r.length+length));r.clamp();constructionChanged();
    }
    private void removeRamp() { if(selected<0||selected>=build.size())return;rememberBuild();build.remove(selected);selected=-1;constructionChanged(); }
    private void undo() { if(history.isEmpty())return;decodeBuild(history.removeLast());selected=-1;constructionChanged(); }
    private void clearBuild() {
        if(build.isEmpty())return;
        new AlertDialog.Builder(this).setTitle("Neu bauen?").setMessage("Die Bretter entfernen und mit einer neuen Idee anfangen?")
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
                .setMessage(current.discovery+(current.sandbox()?"":"\n\nExtra-Sterne: höchstens "+current.bonusRamps+" Bretter und ohne Hinweis lösen."))
                .setPositiveButton("Weiter tüfteln",(d,w)->toggleSimulation());
            if(!current.sandbox()&&current.id<9)dialog.setNegativeButton("Nächste Aufgabe",(d,w)->openLevel(current.id+1));
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
        String label="Bretter: "+build.size()+" / "+current.maxRamps;
        if(selected>=0&&selected<build.size()) { Ramp r=build.get(selected);label="Brett "+(selected+1)+" · "+(int)r.angle+"° · "+(int)r.length; }
        selectionText.setText(label);
        if(hintVisible)statusText.setText(current.hint);
        else if(engine==null)statusText.setText("Brett antippen und mit dem Finger verschieben.");
        else if(engine.state==PhysicsEngine.State.RETRY)statusText.setText("Noch nicht im Korb? Verändere ein Brett und probiere es wieder.");
        else if(engine.state==PhysicsEngine.State.WON)statusText.setText("Deine Idee hat funktioniert!");
        else statusText.setText("Beobachte die Murmel. Was passiert am Brettende?");
    }
    private String encodeBuild() {
        JSONArray array=new JSONArray();
        for(Ramp r:build)try { JSONObject o=new JSONObject();o.put("x",r.x);o.put("y",r.y);o.put("length",r.length);o.put("angle",r.angle);array.put(o); }catch(JSONException ignored){}
        return array.toString();
    }
    private void decodeBuild(String json) {
        build.clear();
        try { JSONArray array=new JSONArray(json);for(int i=0;i<Math.min(array.length(),current.maxRamps);i++) {
            JSONObject o=array.getJSONObject(i);double x=o.getDouble("x"),y=o.getDouble("y"),length=o.getDouble("length"),angle=o.getDouble("angle");
            if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(length)||!Double.isFinite(angle))continue;
            Ramp r=new Ramp(x,y,Math.max(160,Math.min(560,length)),Math.max(-85,Math.min(85,angle)),false);r.clamp();build.add(r);
        }}catch(JSONException ignored){build.clear();}
    }
    private void saveBuild() { if(current!=null&&prefs!=null)prefs.edit().putString("build_"+current.id,encodeBuild()).apply(); }
}
