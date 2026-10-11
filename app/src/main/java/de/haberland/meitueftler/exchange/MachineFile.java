package de.haberland.meitueftler.exchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import de.haberland.meitueftler.game.Ramp;
import de.haberland.meitueftler.game.SandboxLayout;
import de.haberland.meitueftler.game.vehicle.VehiclePart;
import de.haberland.meitueftler.game.mechanics.MechanicPart;
import de.haberland.meitueftler.game.mechanics.MechanicCatalog;

/** Portable constructions only: local IDs, progress and preferences never enter the file. */
public final class MachineFile {
    public static final int VERSION=1,MAX_BYTES=128*1024,MAX_PARTS=60,MAX_SAVED=20;
    public static final String FORMAT="MeiTueftler-Maschine",MIME="application/json",SUFFIX=".meitueftler.json";
    public enum Category {
        MARBLES("marbles","Kugelbahnen","inventions","inventions"),
        VEHICLES("vehicles","Fahrzeuge","vehicle_inventions","vehicle_inventions"),
        MECHANICS("mechanics","Maschinen & Mechanik","mechanics_inventions","mechanic_inventions");
        public final String key,label,prefKey,screen;
        Category(String key,String label,String prefKey,String screen){this.key=key;this.label=label;this.prefKey=prefKey;this.screen=screen;}
        static Category parse(String key)throws FormatException{for(Category c:values())if(c.key.equals(key))return c;throw new FormatException("Die Werkstatt dieser Datei wird nicht unterstützt.");}
    }
    public static final class FormatException extends Exception {
        private static final long serialVersionUID=1L;
        public FormatException(String message){super(message);}
    }
    public final Category category;
    public final String name,build;
    public final int partCount;
    private MachineFile(Category category,String name,JSONArray parts,JSONObject layout)throws JSONException,FormatException{this.category=category;this.name=name;build=withLayout(category,parts,layout);partCount=parts.length();}
    public static String encode(Category category,String name,String build)throws FormatException {
        try {
            JSONObject file=new JSONObject();file.put("format",FORMAT);file.put("version",VERSION);file.put("workshopVersion",1);
            file.put("category",category.key);file.put("name",validName(name));
            file.put("parts",validateParts(category,SandboxLayout.parts(build)));
            JSONObject layout=SandboxLayout.layout(build);if(layout!=null){String normalized=withLayout(category,SandboxLayout.parts(build),layout);file.put("layout",SandboxLayout.layout(normalized));file.put("workshopVersion",2);}
            return file.toString(2);
        }catch(JSONException|IllegalArgumentException e){throw new FormatException("Diese Maschine konnte nicht exportiert werden.");}
    }
    public static MachineFile decode(String json)throws FormatException {
        if(json==null||json.length()>MAX_BYTES||json.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)throw new FormatException("Die Maschinendatei ist zu groß (höchstens 128 KB).");
        checkDepth(json);
        try {
            JSONTokener tokens=new JSONTokener(json);Object value=tokens.nextValue();
            if(!(value instanceof JSONObject)||tokens.nextClean()!=0)throw new FormatException("Das ist keine gültige MeiTüftler-Maschinendatei.");
            JSONObject file=(JSONObject)value;
            if(!FORMAT.equals(file.opt("format")))throw new FormatException("Das ist keine MeiTüftler-Maschinendatei.");
            if(integer(file,"version",1,Integer.MAX_VALUE)!=VERSION||integer(file,"workshopVersion",1,Integer.MAX_VALUE)>2)throw new FormatException("Diese Dateiversion wird noch nicht unterstützt. Bitte MeiTüftler aktualisieren.");
            Object category=file.opt("category"),name=file.opt("name"),parts=file.opt("parts");
            if(!(category instanceof String)||!(name instanceof String)||!(parts instanceof JSONArray))throw new FormatException("In der Datei fehlen Name, Werkstatt oder Bauteile.");
            if(file.has("layout")&&!(file.opt("layout") instanceof JSONObject))throw new FormatException("Die Levelpunkte sind ungültig.");
            if(file.optInt("workshopVersion",1)==2&&!file.has("layout"))throw new FormatException("In der Datei fehlen die Levelpunkte.");
            Category c=Category.parse((String)category);return new MachineFile(c,validName((String)name),validateParts(c,(JSONArray)parts),file.optJSONObject("layout"));
        }catch(JSONException|IllegalArgumentException e){throw new FormatException("Die Datei enthält ungültige Bauteile oder ist beschädigt.");}
    }
    private static String withLayout(Category category,JSONArray parts,JSONObject layout)throws JSONException,FormatException {
        if(layout==null)return parts.toString();
        for(String key:new String[]{"sx","sy","gx","gy"})if(layout.has(key))number(layout,key,30,key.endsWith("x")?970:540);
        String arrayKey=category==Category.VEHICLES?"surfaces":"mounts";
        if(layout.has(arrayKey)){JSONArray points=layout.getJSONArray(arrayKey);if(category==Category.VEHICLES&&points.length()>40||category==Category.MECHANICS&&points.length()!=MechanicCatalog.SANDBOX.mountCount())throw new FormatException("Die Levelpunkte sind ungültig.");for(int i=0;i<points.length();i++){JSONArray point=points.getJSONArray(i);int size=category==Category.VEHICLES?5:2;if(point.length()!=size)throw new FormatException("Die Levelpunkte sind ungültig.");for(int j=0;j<size;j++){Object v=point.get(j);double n=v instanceof Number?((Number)v).doubleValue():Double.NaN;if(!Double.isFinite(n)||n<0||n>(j==4?1:j%2==0?1000:600))throw new FormatException("Die Levelpunkte sind ungültig.");}}}
        JSONObject wrapped=new JSONObject();wrapped.put("parts",parts);wrapped.put("layout",layout);
        String json=wrapped.toString();
        return category==Category.MARBLES?SandboxLayout.pack(parts,SandboxLayout.marble(json)):category==Category.VEHICLES?SandboxLayout.pack(parts,SandboxLayout.vehicle(json)):SandboxLayout.pack(parts,SandboxLayout.mechanic(json));
    }
    public static MachineFile read(InputStream input)throws IOException,FormatException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
        while((count=input.read(buffer))!=-1){if(bytes.size()+count>MAX_BYTES)throw new FormatException("Die Maschinendatei ist zu groß (höchstens 128 KB).");bytes.write(buffer,0,count);}
        try {return decode(StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray())).toString());}
        catch(CharacterCodingException e){throw new FormatException("Die Datei ist keine gültige UTF-8-Maschinendatei.");}
    }
    private static void checkDepth(String json)throws FormatException {
        int depth=0;boolean quoted=false,escaped=false;
        for(int i=0;i<json.length();i++){char c=json.charAt(i);if(quoted){if(escaped)escaped=false;else if(c=='\\')escaped=true;else if(c=='"')quoted=false;}else if(c=='"')quoted=true;else if(c=='{'||c=='['){if(++depth>12)throw new FormatException("Die Maschinendatei ist beschädigt.");}else if(c=='}'||c==']')depth--;}
    }
    public static String validName(String name)throws FormatException {
        if(name==null)throw new FormatException("Der Maschinenname fehlt.");name=name.trim();
        if(name.isEmpty()||name.length()>40)throw new FormatException("Der Maschinenname muss 1 bis 40 Zeichen lang sein.");
        for(int i=0;i<name.length();i++)if(Character.isISOControl(name.charAt(i)))throw new FormatException("Der Maschinenname enthält ungültige Zeichen.");return name;
    }
    private static double number(JSONObject o,String key,double min,double max)throws JSONException,FormatException {
        Object value=o.get(key);if(!(value instanceof Number))throw new FormatException("Ein Bauteil hat eine ungültige Einstellung: "+key);
        double n=((Number)value).doubleValue();if(!Double.isFinite(n)||n<min||n>max)throw new FormatException("Ein Bauteil hat eine ungültige Einstellung: "+key);return n;
    }
    private static int integer(JSONObject o,String key,int min,int max)throws JSONException,FormatException {
        double n=number(o,key,min,max);if(n!=Math.rint(n))throw new FormatException("Ein Bauteil hat eine ungültige Einstellung: "+key);return (int)n;
    }
    private static int optionalInteger(JSONObject o,String key,int fallback,int min,int max)throws JSONException,FormatException{return o.has(key)?integer(o,key,min,max):fallback;}
    private static JSONArray validateParts(Category category,JSONArray parts)throws JSONException,FormatException {
        if(parts.length()>MAX_PARTS)throw new FormatException("Eine Maschine darf höchstens 60 Bauteile enthalten.");
        JSONArray clean=new JSONArray();int motors=0;
        for(int i=0;i<parts.length();i++) {
            JSONObject p=parts.getJSONObject(i),out=new JSONObject();Object kind=p.opt("kind");
            // Original marble saves without kind remain exportable as wooden planks.
            if(kind==null&&category==Category.MARBLES)kind="PLANK";
            if(!(kind instanceof String))throw new FormatException("Ein Bauteiltyp fehlt.");
            String k=(String)kind;double size;
            out.put("kind",k);out.put("x",number(p,"x",0,1000));out.put("y",number(p,"y",0,600));out.put("angle",number(p,"angle",-180,180));
            out.put("power",optionalInteger(p,"power",2,1,3));
            if(category==Category.MARBLES) {
                Ramp.Kind.valueOf(k);out.put("length",number(p,"length",160,560));out.put("channel",optionalInteger(p,"channel",1,1,4));
            }else {
                int direction=optionalInteger(p,"direction",1,-1,1);if(direction==0)throw new FormatException("Die Drehrichtung eines Bauteils ist ungültig.");out.put("direction",direction);
                if(category==Category.VEHICLES) {
                    VehiclePart.Kind type=VehiclePart.Kind.valueOf(k);if(type==VehiclePart.Kind.MOTOR)motors++;
                    size=number(p,"size",type==VehiclePart.Kind.FRAME?60:type==VehiclePart.Kind.WHEEL?20:1,type==VehiclePart.Kind.FRAME?320:type==VehiclePart.Kind.WHEEL?64:64);
                }else {
                    MechanicPart.Kind type=MechanicPart.Kind.valueOf(k);
                    size=number(p,"size",type==MechanicPart.Kind.ROPE?0:type==MechanicPart.Kind.BEAM?60:type==MechanicPart.Kind.WEIGHT?2:22,type==MechanicPart.Kind.ROPE?240:type==MechanicPart.Kind.BEAM?400:type==MechanicPart.Kind.WEIGHT?12:22);
                    out.put("bx",number(p,"bx",0,1000));out.put("by",number(p,"by",0,600));out.put("guide",optionalInteger(p,"guide",-1,-1,MechanicCatalog.SANDBOX.mountCount()-1));
                }
                out.put("size",size);
            }
            clean.put(out);
        }
        if(category==Category.VEHICLES&&motors!=1)throw new FormatException("Ein Fahrzeug braucht genau einen Motor.");return clean;
    }
    public static String filename(String name) {
        String safe=name.replaceAll("[^\\p{L}\\p{N} _-]","_").trim();if(safe.isEmpty())safe="Meine Maschine";return safe.substring(0,Math.min(40,safe.length()))+SUFFIX;
    }
    public static String uniqueName(String name,JSONArray gallery) {
        String candidate=name;int index=1;
        while(containsName(gallery,candidate)){String suffix=" (Import "+index+++")";candidate=name.substring(0,Math.min(name.length(),40-suffix.length()))+suffix;}return candidate;
    }
    private static boolean containsName(JSONArray gallery,String name){for(int i=0;i<gallery.length();i++){JSONObject item=gallery.optJSONObject(i);if(item!=null&&name.equals(item.optString("name")))return true;}return false;}
}
