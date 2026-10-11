package de.haberland.meitueftler;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import de.haberland.meitueftler.game.mechanics.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Own navigation and save namespace: vehicle and marble progress are untouched. */
final class MechanicWorkshop {
    final MainActivity owner;
    final List<MechanicPart> build=new ArrayList<>();
    MechanicLevel level=MechanicCatalog.SANDBOX;
    MechanicPhysics engine;
    int selected=-1;
    boolean hintVisible;
    private boolean hintUsed,handled,loaded;
    private String inventionId;
    private final SharedPreferences prefs;
    private final Deque<String> history=new ArrayDeque<>();
    private final List<Button> editing=new ArrayList<>();
    private MechanicView board;
    private TextView selection,status;
    private Button run,hint,power,direction,guide;
    MechanicWorkshop(MainActivity owner,SharedPreferences prefs){this.owner=owner;this.prefs=prefs;}
    void restore(int id){level=MechanicCatalog.get(id);decode(prefs.getString(level.buildKey(),"[]"));loaded=true;}
    void saveSession(Bundle out){out.putString("mechanicInvention",inventionId);out.putBoolean("mechanicHintUsed",hintUsed);}
    void restoreSession(Bundle state){inventionId=state.getString("mechanicInvention");hintUsed=state.getBoolean("mechanicHintUsed");}
    void showMenu(LinearLayout root) {
        LinearLayout header=owner.row();header.addView(owner.button("‹ Kategorien",false,owner::goBack));header.addView(owner.text("  Maschinen & Mechanik",24,true));root.addView(header);
        root.addView(owner.text("Heben, ziehen, bewegen: Bring die Kiste ins Ziel.",15,false));
        root.addView(new MechanicView(owner,this,true),new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout choices=owner.row();owner.addEqual(choices,owner.button(MechanicCatalog.LEVELS.size()+" Mechanik-Aufgaben",true,()->owner.navigate("mechanic_levels")));
        owner.addEqual(choices,owner.button("Freie Mechanikwerkstatt",false,()->open(-1)));root.addView(choices);
        root.addView(owner.button("Meine Mechanikmaschinen",false,()->owner.navigate("mechanic_inventions")),new LinearLayout.LayoutParams(-1,owner.dp(52)));
    }
    void showLevels(LinearLayout root) {
        LinearLayout header=owner.row();header.addView(owner.button("‹ Mechanik",false,owner::goBack));header.addView(owner.text("  Mechanik-Aufgaben",24,true));root.addView(header);
        root.addView(owner.text("Alle Aufgaben sind offen. Hebel und Seilzüge können vieles bewegen.",15,false));
        ScrollView scroll=new ScrollView(owner);LinearLayout list=owner.column();
        for(MechanicLevel l:MechanicCatalog.LEVELS) {
            int stars=prefs.getInt("mechanics_stars_"+l.id,0);
            Button b=owner.button((l.id+1)+" · "+l.name+"\n"+(stars==0?"Eine neue Idee wartet":MainActivity.symbols('★',stars)+MainActivity.symbols('☆',3-stars)),false,()->open(l.id));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,owner.dp(80));lp.setMargins(0,owner.dp(4),0,owner.dp(4));list.addView(b,lp);
        }
        scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    }
    void open(int id) {
        save();level=MechanicCatalog.get(id);decode(prefs.getString(level.buildKey(),"[]"));loaded=true;
        engine=null;selected=-1;history.clear();hintVisible=false;hintUsed=false;handled=false;inventionId=null;owner.navigate("mechanic_game");
    }
    void showEditor(LinearLayout root) {
        editing.clear();LinearLayout header=owner.row();header.addView(owner.button("‹ Mechanik",false,owner::goBack));
        TextView title=owner.text("  "+(level.sandbox()?inventionName():(level.id+1)+" · "+level.name),21,true);
        title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);header.addView(title,new LinearLayout.LayoutParams(0,owner.dp(52),1));
        if(level.sandbox())header.addView(owner.button("Speichern",false,this::saveDialog));root.addView(header);
        boolean landscape=owner.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout body=new LinearLayout(owner);body.setOrientation(landscape?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        board=new MechanicView(owner,this,false);body.addView(board,landscape?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll=new ScrollView(owner);LinearLayout tools=owner.column();tools.setPadding(owner.dp(8),owner.dp(4),owner.dp(4),0);
        tools.addView(owner.text(level.task,15,false));selection=owner.text("",13,true);selection.setTextColor(MainActivity.TEAL);tools.addView(selection);
        pair(tools,edit("＋ Balken",()->add(MechanicPart.Kind.BEAM)),edit("＋ Gewicht",()->add(MechanicPart.Kind.WEIGHT)));
        pair(tools,edit("＋ Seil",()->add(MechanicPart.Kind.ROPE)),edit("＋ Seilwinde",()->add(MechanicPart.Kind.WINCH)));
        pair(tools,edit("＋ Umlenkrolle",()->add(MechanicPart.Kind.PULLEY)),edit("Bauteil-Hilfe",this::help));
        pair(tools,edit("↶ Drehen",()->resize(-10,0)),edit("Drehen ↷",()->resize(10,0)));
        pair(tools,edit("− Kleiner",()->resize(0,-1)),edit("＋ Größer",()->resize(0,1)));
        power=edit("Winde: mittel",this::cyclePower);direction=edit("Seil: einziehen",this::reverse);pair(tools,power,direction);
        guide=edit("Umlenkung: direkt",this::cycleGuide);tools.addView(guide,new LinearLayout.LayoutParams(-1,owner.dp(48)));
        pair(tools,edit("↶ Zurück",this::undo),edit("Entfernen",this::remove));
        tools.addView(edit("Bauteil wechseln",()->{if(!build.isEmpty()){selected=(selected+1)%build.size();update();board.invalidate();}}),new LinearLayout.LayoutParams(-1,owner.dp(48)));
        tools.addView(edit("Neu bauen",this::clear),new LinearLayout.LayoutParams(-1,owner.dp(48)));
        hint=owner.button("Hinweis",false,()->{hintVisible=!hintVisible;if(hintVisible)hintUsed=true;update();board.invalidate();});tools.addView(hint,new LinearLayout.LayoutParams(-1,owner.dp(48)));
        status=owner.text("",13,false);status.setPadding(0,owner.dp(6),0,owner.dp(6));tools.addView(status);scroll.addView(tools);
        run=owner.button("▶ Ausprobieren",true,this::toggle);
        if(landscape){LinearLayout side=owner.column();side.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));side.addView(run,new LinearLayout.LayoutParams(-1,owner.dp(56)));body.addView(side,new LinearLayout.LayoutParams(owner.dp(252),-1));}
        else body.addView(scroll,new LinearLayout.LayoutParams(-1,owner.dp(246)));
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));if(!landscape)root.addView(run,new LinearLayout.LayoutParams(-1,owner.dp(56)));update();
    }
    private Button edit(String text,Runnable action){Button b=owner.button(text,false,action);editing.add(b);return b;}
    private void pair(LinearLayout parent,Button a,Button b){LinearLayout row=owner.row();owner.addEqual(row,a);owner.addEqual(row,b);parent.addView(row);}
    void remember(){history.addLast(encode());while(history.size()>40)history.removeFirst();}
    void changed(){save();update();if(board!=null)board.invalidate();}
    private void add(MechanicPart.Kind kind) {
        if(build.size()>=level.maxParts()){toast("Alle "+level.maxParts()+" Bauteile sind im Einsatz.");return;}
        remember();MechanicPart p=new MechanicPart(kind,500,260,kind==MechanicPart.Kind.BEAM?240:kind==MechanicPart.Kind.WEIGHT?8:22,0);
        if(kind==MechanicPart.Kind.ROPE)p=MechanicPart.rope(440,260,620,460,-1);
        p.clamp();build.add(p);selected=build.size()-1;changed();
    }
    private MechanicPart selectedPart(){return selected>=0&&selected<build.size()?build.get(selected):null;}
    private void resize(int angle,int amount) {
        MechanicPart p=selectedPart();if(p==null){toast("Tippe zuerst ein Bauteil an.");return;}
        if(angle!=0&&p.kind!=MechanicPart.Kind.BEAM){toast("Nur Balken werden gedreht. Seilenden verschiebst du direkt.");return;}
        if(amount!=0&&p.mounted()){toast("Winden und Rollen sitzen auf Befestigungspunkten.");return;}
        remember();p.angle+=angle;p.size+=amount*(p.kind==MechanicPart.Kind.BEAM?20:p.kind==MechanicPart.Kind.ROPE?20:2);p.clamp();changed();
    }
    private void cyclePower(){MechanicPart p=selectedPart();if(p==null||p.kind!=MechanicPart.Kind.WINCH){toast("Wähle zuerst eine Seilwinde.");return;}remember();p.power=p.power%3+1;changed();}
    private void reverse(){MechanicPart p=selectedPart();if(p==null||p.kind!=MechanicPart.Kind.WINCH){toast("Wähle zuerst eine Seilwinde.");return;}remember();p.direction=-p.direction;changed();}
    private void cycleGuide() {
        MechanicPart p=selectedPart();if(p==null||p.kind!=MechanicPart.Kind.ROPE){toast("Wähle zuerst ein Seil.");return;}
        List<Integer> guides=new ArrayList<>();guides.add(-1);
        for(int i=0;i<level.mountCount();i++)for(MechanicPart q:build)if(q.kind==MechanicPart.Kind.PULLEY&&level.nearMount(q.x,q.y)==i&&!guides.contains(i))guides.add(i);
        if(guides.size()==1){toast("Setze zuerst eine Umlenkrolle auf einen Befestigungspunkt.");return;}
        remember();p.guide=guides.get((guides.indexOf(p.guide)+1)%guides.size());changed();
    }
    private void remove(){if(selectedPart()==null)return;remember();build.remove(selected);selected=-1;changed();}
    private void undo(){if(history.isEmpty())return;decode(history.removeLast());selected=-1;changed();}
    private void clear(){new AlertDialog.Builder(owner).setTitle("Neu bauen?").setMessage("Die Kiste bleibt. Mit einer neuen Idee anfangen?").setNegativeButton("Behalten",null).setPositiveButton("Neu bauen",(d,w)->{remember();build.clear();selected=-1;changed();}).show();}
    private void help() {
        new AlertDialog.Builder(owner).setTitle("Hebel und Seilzüge bauen").setMessage("Balken und Gewichte verbinden sich bei Berührung mit der Kiste zu einer festen Baugruppe.\n\nBefestigung: Lege einen Balken durch einen Schraubpunkt. Ein Punkt ist ein Drehgelenk, zwei Punkte halten den Balken fest.\n\nGewicht: Mehr Gewicht oder mehr Abstand zum Drehpunkt erzeugt mehr Drehwirkung.\n\nSeil: Verschiebe seine beiden farbigen Enden einzeln zu Kiste, Balken, Gewicht oder Winde. Ein Seil kann ziehen, nicht drücken. Kleiner/Größer verändert die Seilzugabe.\n\nSeilwinde: Setze sie auf einen Schraubpunkt. Stärke und Einziehen/Abrollen wirken auf angeschlossene Seile. Die Zugkraft ist begrenzt.\n\nUmlenkrolle: Setze sie auf einen Schraubpunkt. Wähle danach beim Seil über Umlenkung die Rolle. Sie ändert die Zugrichtung, nicht die Kraft.\n\nDie Kiste und Schraubpunkte gehören zur Aufgabe. Maschinen bleiben hier an der Werkbank; bewegliche Gelenke und Räder findest du in Fahrzeuge.").setPositiveButton("Weiter tüfteln",null).show();
    }
    private void toggle(){if(engine!=null){engine=null;handled=false;update();board.resumeDrawing();return;}engine=new MechanicPhysics(level,build);hintVisible=false;handled=false;update();board.resumeDrawing();owner.playTone(ToneGenerator.TONE_PROP_BEEP,80);}
    void finished() {
        if(engine==null||engine.state==MechanicPhysics.State.RUNNING||handled||owner.isFinishing())return;handled=true;update();
        if(engine.state!=MechanicPhysics.State.WON)return;owner.playTone(ToneGenerator.TONE_PROP_ACK,220);
        int stars=level.sandbox()?0:1+(build.size()<=level.bonusParts()?1:0)+(hintUsed?0:1);
        if(!level.sandbox())prefs.edit().putInt("mechanics_stars_"+level.id,Math.max(stars,prefs.getInt("mechanics_stars_"+level.id,0))).apply();
        AlertDialog.Builder dialog=new AlertDialog.Builder(owner).setTitle(level.sandbox()?"Deine Maschine funktioniert!":"Geschafft!  "+MainActivity.symbols('★',stars))
            .setMessage("Deine Maschine hat die Kiste ins Ziel gebracht!"+(level.sandbox()?"":"\n\nExtra-Sterne: höchstens "+level.bonusParts()+" Bauteile und ohne Hinweis."))
            .setPositiveButton("Weiter tüfteln",(d,w)->toggle());
        if(!level.sandbox()&&level.id<MechanicCatalog.LEVELS.size()-1)dialog.setNegativeButton("Nächste Aufgabe",(d,w)->open(level.id+1));
        dialog.setOnCancelListener(d->{if(engine!=null)toggle();});dialog.show();
    }
    void update() {
        if(selection==null)return;boolean editable=engine==null;for(Button b:editing){b.setEnabled(editable);b.setAlpha(editable?1:0.45f);}hint.setEnabled(editable);hint.setText(hintVisible?"Hinweis aus":"Hinweis");
        MechanicPart p=selectedPart();String detail=p==null?"":p.label()+" · "+(p.kind==MechanicPart.Kind.BEAM?(int)p.angle+"° · Länge "+(int)p.size:p.kind==MechanicPart.Kind.WEIGHT?"Masse "+(int)p.size:p.kind==MechanicPart.Kind.ROPE?"Zugabe "+(int)p.size+" · beide Enden verschieben":"am Befestigungspunkt platzieren")+"\n";
        selection.setText(owner.getString(R.string.mechanics_selection,detail,build.size(),level.maxParts(),(int)level.cargoMass));
        power.setText(p!=null&&p.kind==MechanicPart.Kind.WINCH?(p.power==1?"Winde: sanft":p.power==3?"Winde: kräftig":"Winde: mittel"):"Windeneinstellung");
        direction.setText(p!=null&&p.kind==MechanicPart.Kind.WINCH&&p.direction<0?"Seil: abrollen":"Seil: einziehen");
        guide.setText(p!=null&&p.kind==MechanicPart.Kind.ROPE&&p.guide>=0?"Umlenkung: Rolle "+(p.guide+1):"Umlenkung: direkt");
        run.setText(editable?"▶ Ausprobieren":"↶ Weiterbauen");
        status.setText(hintVisible?level.hint:engine!=null?engine.state==MechanicPhysics.State.RUNNING?"Beobachte Hebelarm, Last und Seilspannung.":engine.message:"Schraubpunkte sind fest. Balken drehen dort; Gewichte und Seile bewegen die Kiste. Bauteil-Hilfe erklärt den Aufbau.");
    }
    void resume(){if(board!=null)board.resumeDrawing();}
    void leave(){save();engine=null;selected=-1;}
    void save(){if(loaded)prefs.edit().putString(level.buildKey(),encode()).apply();}
    private String encode() {
        JSONArray result=new JSONArray();for(MechanicPart p:build)try{JSONObject o=new JSONObject();o.put("kind",p.kind.name());o.put("x",p.x);o.put("y",p.y);o.put("bx",p.bx);o.put("by",p.by);o.put("size",p.size);o.put("angle",p.angle);o.put("power",p.power);o.put("direction",p.direction);o.put("guide",p.guide);result.put(o);}catch(JSONException ignored){ }return result.toString();
    }
    private void decode(String json) {
        build.clear();try{JSONArray items=new JSONArray(json);for(int i=0;i<items.length()&&build.size()<level.maxParts();i++){JSONObject o=items.optJSONObject(i);if(o==null)continue;
            try{MechanicPart p=new MechanicPart(MechanicPart.Kind.valueOf(o.optString("kind")),o.optDouble("x",500),o.optDouble("y",260),o.optDouble("size",8),o.optDouble("angle",0));
                p.bx=o.optDouble("bx",620);p.by=o.optDouble("by",460);p.power=o.optInt("power",2);p.direction=o.optInt("direction",1);p.guide=Math.max(-1,o.optInt("guide",-1));p.clamp();build.add(p);
            }catch(IllegalArgumentException ignored){ }
        }}catch(JSONException ignored){ }
    }
    private JSONArray inventions(){try{return new JSONArray(prefs.getString("mechanics_inventions","[]"));}catch(JSONException ignored){return new JSONArray();}}
    private String inventionName(){JSONArray all=inventions();for(int i=0;i<all.length();i++){JSONObject o=all.optJSONObject(i);if(o!=null&&o.optString("id").equals(inventionId))return o.optString("name");}return level.name;}
    void showInventions(LinearLayout root) {
        LinearLayout header=owner.row();header.addView(owner.button("‹ Mechanik",false,owner::goBack));header.addView(owner.text("  Meine Mechanikmaschinen",22,true));root.addView(header);
        ScrollView scroll=new ScrollView(owner);LinearLayout list=owner.column();JSONArray all=inventions();
        if(all.length()==0)list.addView(owner.text("Speichere deine erste Hebemaschine im freien Bauplatz.",17,false));
        for(int i=0;i<all.length();i++){JSONObject o=all.optJSONObject(i);if(o==null)continue;LinearLayout row=owner.row();
            owner.addEqual(row,owner.button(o.optString("name"),false,()->{open(-1);inventionId=o.optString("id");decode(o.optString("build","[]"));save();owner.navigate("mechanic_game");}));
            row.addView(owner.button("Löschen",false,()->new AlertDialog.Builder(owner).setTitle("Maschine löschen?").setMessage(o.optString("name")).setNegativeButton("Behalten",null).setPositiveButton("Löschen",(d,w)->{JSONArray saved=inventions();for(int j=saved.length()-1;j>=0;j--){JSONObject item=saved.optJSONObject(j);if(item!=null&&item.optString("id").equals(o.optString("id")))saved.remove(j);}prefs.edit().putString("mechanics_inventions",saved.toString()).apply();owner.navigate("mechanic_inventions");}).show()));list.addView(row);
        }
        scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));root.addView(owner.button("Freie Mechanikwerkstatt",true,()->open(-1)));
    }
    private void saveDialog() {
        if(engine!=null){toast("Tippe zuerst auf Weiterbauen.");return;}
        EditText name=new EditText(owner);name.setSingleLine(true);name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});name.setText(inventionId==null?"Meine Hebemaschine":inventionName());name.setSelectAllOnFocus(true);name.setContentDescription("Name der Mechanikmaschine");
        AlertDialog.Builder dialog=new AlertDialog.Builder(owner).setTitle("Mechanikmaschine speichern").setView(name).setNegativeButton("Abbrechen",null).setPositiveButton(inventionId==null?"Speichern":"Aktualisieren",(d,w)->store(name.getText().toString(),false));
        if(inventionId!=null)dialog.setNeutralButton("Als Kopie",(d,w)->store(name.getText().toString(),true));dialog.show();
    }
    private void store(String name,boolean copy) {
        name=name.trim();if(name.isEmpty()){toast("Gib deiner Maschine einen Namen.");return;}
        if(copy&&name.equals(inventionName()))name=name.substring(0,Math.min(32,name.length()))+" (Kopie)";
        JSONArray saved=inventions();String id=copy||inventionId==null?java.util.UUID.randomUUID().toString():inventionId;boolean replaced=false;
        try{JSONObject o=new JSONObject();o.put("id",id);o.put("name",name);o.put("build",encode());for(int i=0;i<saved.length();i++){JSONObject item=saved.optJSONObject(i);if(item!=null&&item.optString("id").equals(id)){saved.put(i,o);replaced=true;break;}}
            if(!replaced){if(saved.length()>=20){toast("20 Mechanikmaschinen gespeichert. Aktualisiere oder lösche eine Maschine.");return;}saved.put(o);}prefs.edit().putString("mechanics_inventions",saved.toString()).apply();inventionId=id;toast("Mechanikmaschine gespeichert");owner.navigate("mechanic_game");
        }catch(JSONException ignored){toast("Speichern hat nicht geklappt.");}
    }
    private void toast(String text){Toast.makeText(owner,text,Toast.LENGTH_SHORT).show();}
}
