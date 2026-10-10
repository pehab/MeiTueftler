package de.haberland.meitueftler;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.media.ToneGenerator;
import android.text.InputFilter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import de.haberland.meitueftler.game.vehicle.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Vehicle navigation and storage have their own keys; marble saves remain compatible. */
final class VehicleWorkshop {
    final MainActivity owner;
    final List<VehiclePart> build=new ArrayList<>();
    VehicleLevel level=VehicleCatalog.SANDBOX;
    VehiclePhysics engine;
    int selected=-1;
    boolean hintVisible;
    private boolean hintUsed,handled;
    private String inventionId;
    private final SharedPreferences prefs;
    private final Deque<String> history=new ArrayDeque<>();
    private final List<Button> editing=new ArrayList<>();
    private VehicleView board;
    private boolean[] connectedParts=new boolean[0],validJoints=new boolean[0];
    private Button run,power,direction,hint;
    private TextView selection,status;
    VehicleWorkshop(MainActivity owner,SharedPreferences prefs) { this.owner=owner;this.prefs=prefs; }
    void restore(int id) { level=VehicleCatalog.get(id);decode(prefs.getString(level.buildKey(),"[]")); }
    void saveSession(Bundle out) { out.putString("vehicleInvention",inventionId);out.putBoolean("vehicleHintUsed",hintUsed); }
    void restoreSession(Bundle state) { inventionId=state.getString("vehicleInvention");hintUsed=state.getBoolean("vehicleHintUsed"); }
    void showMenu(LinearLayout root) {
        LinearLayout header=owner.row();header.addView(owner.button("‹ Kategorien",false,()->owner.navigate("menu")));header.addView(owner.text("  Fahrzeuge",28,true));root.addView(header);
        root.addView(owner.text("Baue eine Maschine, die von selbst ins Ziel fährt.",16,false));
        root.addView(new VehicleView(owner,this,true),new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout line=owner.row();owner.addEqual(line,owner.button(VehicleCatalog.LEVELS.size()+" Fahrzeug-Aufgaben",true,()->owner.navigate("vehicle_levels")));
        owner.addEqual(line,owner.button("Freie Maschinenwerkstatt",false,()->open(-1)));root.addView(line);
        root.addView(owner.button("Meine Fahrzeuge",false,()->owner.navigate("vehicle_inventions")),new LinearLayout.LayoutParams(-1,owner.dp(52)));
        root.addView(owner.text("Rahmen, Räder, Gelenke und drehende Beine: Deine Idee fährt los.",14,false));
    }
    void showLevels(LinearLayout root) {
        LinearLayout header=owner.row();header.addView(owner.button("‹ Fahrzeuge",false,()->owner.navigate("vehicles")));header.addView(owner.text("  Fahrzeug-Aufgaben",25,true));root.addView(header);
        root.addView(owner.text("Alle Aufgaben sind offen. Es gibt viele mögliche Maschinen.",15,false));
        ScrollView scroll=new ScrollView(owner);LinearLayout list=owner.column();
        for(VehicleLevel l:VehicleCatalog.LEVELS) {
            int stars=prefs.getInt("vehicle_stars_"+l.id,0);
            Button b=owner.button((l.id+1)+" · "+l.name+"\n"+(stars==0?"Eine neue Maschine wartet":MainActivity.symbols('★',stars)+MainActivity.symbols('☆',3-stars)),false,()->open(l.id));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,owner.dp(80));lp.setMargins(0,owner.dp(4),0,owner.dp(4));list.addView(b,lp);
        }
        scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    }
    void open(int id) { save();level=VehicleCatalog.get(id);decode(prefs.getString(level.buildKey(),"[]"));engine=null;selected=-1;history.clear();hintUsed=false;hintVisible=false;handled=false;inventionId=null;owner.navigate("vehicle_game"); }
    void showEditor(LinearLayout root) {
        editing.clear();LinearLayout header=owner.row();header.addView(owner.button("‹ Fahrzeuge",false,owner::goBack));
        TextView title=owner.text("  "+(level.sandbox()?inventionName():(level.id+1)+" · "+level.name),21,true);title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        header.addView(title,new LinearLayout.LayoutParams(0,owner.dp(52),1));if(level.sandbox())header.addView(owner.button("Speichern",false,this::saveDialog));root.addView(header);
        boolean landscape=owner.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout body=new LinearLayout(owner);body.setOrientation(landscape?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        board=new VehicleView(owner,this,false);body.addView(board,landscape?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll=new ScrollView(owner);LinearLayout tools=owner.column();tools.setPadding(owner.dp(8),owner.dp(4),owner.dp(4),0);
        tools.addView(owner.text(level.task,15,false));selection=owner.text("",13,true);selection.setTextColor(MainActivity.TEAL);tools.addView(selection);
        tools(tools,edit("＋ Rahmen",()->add(VehiclePart.Kind.FRAME)),edit("＋ Rad",()->add(VehiclePart.Kind.WHEEL)));
        tools(tools,edit("＋ Gelenk",()->add(VehiclePart.Kind.HINGE)),edit("＋ Drehantrieb",()->add(VehiclePart.Kind.DRIVE)));
        tools(tools,edit("＋ Gewicht",()->add(VehiclePart.Kind.WEIGHT)),edit("＋ Greiffuß",()->add(VehiclePart.Kind.FOOT)));
        tools(tools,edit("↶ Zurück",this::undo),edit("Bauteil-Hilfe",this::partHelp));
        tools(tools,edit("↶ Drehen",()->change(-10,0)),edit("Drehen ↷",()->change(10,0)));
        tools(tools,edit("− Kleiner",()->change(0,-1)),edit("＋ Größer",()->change(0,1)));
        power=edit("Motor: mittel",this::cyclePower);direction=edit("Fahrt: rechts",this::reverse);tools(tools,power,direction);
        tools(tools,edit("Entfernen",this::remove),edit("Neu bauen",this::clear));
        hint=owner.button("Hinweis",false,()->{hintVisible=!hintVisible;if(hintVisible)hintUsed=true;update();board.invalidate();});tools.addView(hint,new LinearLayout.LayoutParams(-1,owner.dp(48)));
        status=owner.text("",13,false);status.setPadding(0,owner.dp(6),0,owner.dp(6));tools.addView(status);scroll.addView(tools);
        run=owner.button("▶ Ausprobieren",true,this::toggle);LinearLayout.LayoutParams runLp=new LinearLayout.LayoutParams(-1,owner.dp(56));
        if(landscape) { LinearLayout side=owner.column();side.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));side.addView(run,runLp);body.addView(side,new LinearLayout.LayoutParams(owner.dp(252),-1)); }
        else body.addView(scroll,new LinearLayout.LayoutParams(-1,owner.dp(246)));
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));if(!landscape)root.addView(run,runLp);update();
    }
    private Button edit(String text,Runnable action) { Button b=owner.button(text,false,action);editing.add(b);return b; }
    private void tools(LinearLayout parent,Button a,Button b) { LinearLayout line=owner.row();owner.addEqual(line,a);owner.addEqual(line,b);parent.addView(line); }
    void remember() { history.addLast(encode());while(history.size()>40)history.removeFirst(); }
    void changed() { save();update();if(board!=null)board.invalidate(); }
    private VehiclePart motor() { for(VehiclePart p:build)if(p.kind==VehiclePart.Kind.MOTOR)return p;throw new IllegalStateException("Missing vehicle motor"); }
    private void add(VehiclePart.Kind kind) {
        if(build.size()>=level.maxParts()) { toast("Alle "+level.maxParts()+" Bauteile sind im Einsatz.");return; }
        remember();VehiclePart m=motor(),p=new VehiclePart(kind,m.x,m.y-70,kind==VehiclePart.Kind.FRAME?180:kind==VehiclePart.Kind.WHEEL?32:18,0);
        if(p.joint()&&selected>=0&&selected<build.size()) { VehiclePart base=build.get(selected);p.x=base.x+Math.cos(Math.toRadians(base.angle))*base.halfLength();p.y=base.y+Math.sin(Math.toRadians(base.angle))*base.halfLength(); }p.clamp(level);build.add(p);selected=build.size()-1;changed();
    }
    private void partHelp() {
        new AlertDialog.Builder(owner).setTitle("Bewegliche Maschinen bauen").setMessage("Rahmen und Räder verbinden sich bei Berührung fest.\n\nGelenk: Setze den Ring genau dort hin, wo sich zwei Baugruppen berühren. Sie bleiben zusammen und können sich gegeneinander drehen. Andere Berührungen können sie weiterhin starr verbinden.\n\nDrehantrieb: Funktioniert wie ein Gelenk und dreht die äußere Baugruppe. Tippe ihn an, um Tempo und Drehsinn einzustellen. Kraft kommt über die Verbindung vom Motor; mehrere Antriebe teilen sich die Kraft.\n\nGreiffuß: Ein Gummifuß mit viel Halt für drehende Beine. Er hat keinen eigenen Antrieb.\n\nTürkis = mit dem Motor verbunden. Orange Gelenke brauchen genau zwei getrennte Baugruppen. Gestrichelt = lose.").setPositiveButton("Weiter tüfteln",null).show();
    }
    private VehiclePart driveTarget() { return selected>=0&&selected<build.size()&&build.get(selected).kind==VehiclePart.Kind.DRIVE?build.get(selected):motor(); }
    private void change(int angle,int size) {
        if(selected<0) { toast("Tippe zuerst ein Bauteil an.");return; }
        VehiclePart p=build.get(selected);if(p.kind==VehiclePart.Kind.MOTOR||p.kind==VehiclePart.Kind.WEIGHT||p.kind==VehiclePart.Kind.FOOT||p.joint()) { toast("Diesen Baustein verschiebst du mit dem Finger.");return; }
        remember();p.angle+=angle;p.size+=size*(p.kind==VehiclePart.Kind.WHEEL?4:20);p.clamp(level);changed();
    }
    private void cyclePower() { remember();VehiclePart m=driveTarget();m.power=m.power%3+1;changed(); }
    private void reverse() { remember();driveTarget().direction*=-1;changed(); }
    private void remove() { if(selected<0)return;if(build.get(selected).kind==VehiclePart.Kind.MOTOR) { toast("Der Motor gehört zur Aufgabe. Du kannst ihn im Startbereich verschieben.");return; }remember();build.remove(selected);selected=-1;changed(); }
    private void undo() { if(history.isEmpty())return;decode(history.removeLast());selected=-1;changed(); }
    private void clear() {
        new AlertDialog.Builder(owner).setTitle("Neu bauen?").setMessage("Mit dem Motor und einer neuen Idee anfangen?").setNegativeButton("Behalten",null)
            .setPositiveButton("Neu bauen",(d,w)->{remember();build.clear();for(VehiclePart p:level.emptyBuild())build.add(p.copy());selected=-1;changed();}).show();
    }
    private void toggle() {
        if(engine!=null) { engine=null;handled=false;update();board.resumeDrawing();return; }
        engine=new VehiclePhysics(level,build);hintVisible=false;handled=false;update();board.resumeDrawing();owner.playTone(ToneGenerator.TONE_PROP_BEEP,80);
    }
    void finished() {
        if(engine==null||engine.state==VehiclePhysics.State.RUNNING||handled||owner.isFinishing())return;handled=true;update();
        if(engine.state==VehiclePhysics.State.WON) {
            owner.playTone(ToneGenerator.TONE_PROP_ACK,220);
            int stars=level.sandbox()?0:1+(build.size()<=level.bonusParts()?1:0)+(hintUsed?0:1);
            if(!level.sandbox())prefs.edit().putInt("vehicle_stars_"+level.id,Math.max(stars,prefs.getInt("vehicle_stars_"+level.id,0))).apply();
            AlertDialog.Builder dialog=new AlertDialog.Builder(owner).setTitle(level.sandbox()?"Deine Maschine funktioniert!":"Geschafft!  "+MainActivity.symbols('★',stars))
                .setMessage("Deine Konstruktion hat den Motor ins Ziel gebracht!"+(level.sandbox()?"":"\n\nExtra-Sterne: höchstens "+level.bonusParts()+" Teile einschließlich Motor und ohne Hinweis."))
                .setPositiveButton("Weiter tüfteln",(d,w)->toggle());
            if(!level.sandbox()&&level.id<VehicleCatalog.LEVELS.size()-1)dialog.setNegativeButton("Nächste Aufgabe",(d,w)->open(level.id+1));
            dialog.setOnCancelListener(d->{if(engine!=null)toggle();});dialog.show();
        }
    }
    void update() {
        if(selection==null||build.isEmpty())return;boolean editable=engine==null;
        for(Button b:editing){b.setEnabled(editable);b.setAlpha(editable?1:0.45f);}hint.setEnabled(editable);hint.setText(hintVisible?"Hinweis aus":"Hinweis");
        VehiclePart m=motor(),target=driveTarget();
        boolean rotating=target.kind==VehiclePart.Kind.DRIVE;
        power.setText(rotating?(target.power==1?R.string.vehicle_drive_slow:target.power==3?R.string.vehicle_drive_fast:R.string.vehicle_drive_medium):(m.power==1?R.string.vehicle_motor_soft:m.power==3?R.string.vehicle_motor_strong:R.string.vehicle_motor_medium));
        direction.setText(rotating?(target.direction==1?R.string.vehicle_clockwise:R.string.vehicle_counterclockwise):(m.direction==1?R.string.vehicle_right:R.string.vehicle_left));
        run.setText(editable?"▶ Ausprobieren":"↶ Weiterbauen");
        VehicleAssembly assembly=new VehicleAssembly(build);validJoints=assembly.validJoint;int[] groups=assembly.connectedRoots;int motorRoot=groups[build.indexOf(m)];int connected=0;connectedParts=new boolean[groups.length];for(int i=0;i<groups.length;i++){connectedParts[i]=groups[i]==motorRoot;if(connectedParts[i])connected++;}
        String label=connected+" / "+build.size()+" Teile verbunden · max. "+level.maxParts();
        if(selected>=0&&selected<build.size()) { VehiclePart p=build.get(selected);label=p.label()+" · "+(int)p.angle+"°"+(p.kind==VehiclePart.Kind.FRAME?" · Länge "+(int)p.size:p.kind==VehiclePart.Kind.WHEEL?" · Radius "+(int)p.size:"")+"\n"+label; }
        if(selected>=0&&selected<build.size()&&build.get(selected).joint())label+=(validJoints[selected]?"\nGelenk verbindet zwei Baugruppen":"\nGelenk braucht genau zwei getrennte Baugruppen");
        selection.setText(label);
        status.setText(hintVisible?level.hint:engine!=null?engine.state==VehiclePhysics.State.RUNNING?"Deine Maschine fährt. Beobachte Räder und Schwerpunkt.":engine.message:
            assembly.invalidJoints>0?"Orange Gelenke sind noch nicht richtig verbunden. Setze sie an die Berührung zweier Baugruppen. Bauteil-Hilfe erklärt den Aufbau.":"Berührende Teile verbinden sich fest. Gelenke machen die Verbindung beweglich. Türkis = am Motor; gestrichelt = lose. Das Kreuz zeigt den Schwerpunkt.");
    }
    boolean validJoint(int i) { return engine!=null?engine.assembly.validJoint[i]:i<validJoints.length&&validJoints[i]; }
    boolean connected(int i) { return engine!=null?engine.powered[i]:i<connectedParts.length&&connectedParts[i]; }
    void resume() { if(board!=null)board.resumeDrawing(); }
    void leave() { save();engine=null;selected=-1; }
    void save() { if(!build.isEmpty())prefs.edit().putString(level.buildKey(),encode()).apply(); }
    private String encode() {
        JSONArray result=new JSONArray();for(VehiclePart p:build)try { JSONObject item=new JSONObject();item.put("kind",p.kind.name());item.put("x",p.x);item.put("y",p.y);item.put("size",p.size);item.put("angle",p.angle);item.put("power",p.power);item.put("direction",p.direction);result.put(item); }catch(JSONException ignored){ }
        return result.toString();
    }
    private void decode(String json) {
        build.clear();boolean hasMotor=false;
        try { JSONArray array=new JSONArray(json);for(int i=0;i<array.length()&&build.size()<level.maxParts();i++) {
            JSONObject item=array.optJSONObject(i);if(item==null)continue;
            try {
                VehiclePart.Kind kind=VehiclePart.Kind.valueOf(item.optString("kind"));if(kind==VehiclePart.Kind.MOTOR&&hasMotor)continue;
                double x=item.optDouble("x",170),y=item.optDouble("y",445),size=item.optDouble("size",32),angle=item.optDouble("angle",0);
                if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(size)||!Double.isFinite(angle))continue;
                VehiclePart p=new VehiclePart(kind,x,y,size,angle);p.power=item.optInt("power",2);p.direction=item.optInt("direction",1);p.clamp(level);build.add(p);if(kind==VehiclePart.Kind.MOTOR)hasMotor=true;
            }catch(IllegalArgumentException ignored){ }
        }}catch(JSONException ignored){ }
        if(!hasMotor) { if(build.size()==level.maxParts())build.remove(build.size()-1);build.add(0,level.emptyBuild().get(0).copy()); }
    }
    private JSONArray inventions() { try{return new JSONArray(prefs.getString("vehicle_inventions","[]"));}catch(JSONException ignored){return new JSONArray();} }
    private String inventionName() { JSONArray saved=inventions();for(int i=0;i<saved.length();i++){JSONObject p=saved.optJSONObject(i);if(p!=null&&p.optString("id").equals(inventionId))return p.optString("name");}return level.name; }
    void showInventions(LinearLayout root) {
        LinearLayout header=owner.row();header.addView(owner.button("‹ Fahrzeuge",false,()->owner.navigate("vehicles")));header.addView(owner.text("  Meine Fahrzeuge",24,true));root.addView(header);
        ScrollView scroll=new ScrollView(owner);LinearLayout list=owner.column();JSONArray saved=inventions();
        if(saved.length()==0)list.addView(owner.text("Speichere deine erste Maschine im freien Bauplatz.",17,false));
        for(int i=0;i<saved.length();i++) {
            JSONObject item=saved.optJSONObject(i);if(item==null)continue;LinearLayout row=owner.row();
            owner.addEqual(row,owner.button(item.optString("name"),false,()->{open(-1);inventionId=item.optString("id");decode(item.optString("build","[]"));save();owner.navigate("vehicle_game");}));
            row.addView(owner.button("Löschen",false,()->new AlertDialog.Builder(owner).setTitle("Fahrzeug löschen?").setMessage(item.optString("name")).setNegativeButton("Behalten",null).setPositiveButton("Löschen",(d,w)->{
                JSONArray all=inventions();for(int j=all.length()-1;j>=0;j--){JSONObject p=all.optJSONObject(j);if(p!=null&&p.optString("id").equals(item.optString("id")))all.remove(j);}prefs.edit().putString("vehicle_inventions",all.toString()).apply();owner.navigate("vehicle_inventions");}).show()));list.addView(row);
        }
        scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));root.addView(owner.button("Freie Maschinenwerkstatt",true,()->open(-1)));
    }
    private void saveDialog() {
        if(engine!=null) { toast("Tippe zuerst auf Weiterbauen.");return; }
        EditText name=new EditText(owner);name.setSingleLine(true);name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});name.setText(inventionId==null?"Meine Maschine":inventionName());name.setSelectAllOnFocus(true);name.setContentDescription("Name des Fahrzeugs");
        AlertDialog.Builder dialog=new AlertDialog.Builder(owner).setTitle("Fahrzeug speichern").setView(name).setNegativeButton("Abbrechen",null)
            .setPositiveButton(inventionId==null?"Speichern":"Aktualisieren",(d,w)->store(name.getText().toString(),false));
        if(inventionId!=null)dialog.setNeutralButton("Als Kopie",(d,w)->store(name.getText().toString(),true));dialog.show();
    }
    private void store(String name,boolean copy) {
        name=name.trim();if(name.isEmpty()){toast("Gib deiner Maschine einen Namen.");return;}
        if(copy&&name.equals(inventionName()))name=name.substring(0,Math.min(32,name.length()))+" (Kopie)";
        JSONArray saved=inventions();String id=copy||inventionId==null?java.util.UUID.randomUUID().toString():inventionId;boolean replaced=false;
        try {
            JSONObject item=new JSONObject();item.put("id",id);item.put("name",name);item.put("build",encode());
            for(int i=0;i<saved.length();i++){JSONObject p=saved.optJSONObject(i);if(p!=null&&p.optString("id").equals(id)){saved.put(i,item);replaced=true;break;}}
            if(!replaced){if(saved.length()>=20){toast("20 Fahrzeuge gespeichert. Aktualisiere oder lösche eine Maschine.");return;}saved.put(item);}
            prefs.edit().putString("vehicle_inventions",saved.toString()).apply();inventionId=id;toast("Fahrzeug gespeichert");owner.navigate("vehicle_game");
        }catch(JSONException ignored){toast("Speichern hat nicht geklappt.");}
    }
    private void toast(String message) { Toast.makeText(owner,message,Toast.LENGTH_SHORT).show(); }
}
