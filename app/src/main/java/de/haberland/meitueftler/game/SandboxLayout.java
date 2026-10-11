package de.haberland.meitueftler.game;

import org.json.*;
import de.haberland.meitueftler.game.vehicle.*;
import de.haberland.meitueftler.game.mechanics.*;
import java.util.ArrayList;

/** Optional layout travels with the construction; legacy arrays remain readable. */
public final class SandboxLayout {
    private SandboxLayout() { }
    public static JSONArray parts(String json)throws JSONException {
        Object value=new JSONTokener(json).nextValue();
        if(value instanceof JSONObject)return ((JSONObject)value).getJSONArray("parts");
        if(value instanceof JSONArray)return (JSONArray)value;
        throw new JSONException("Invalid construction");
    }
    public static JSONObject layout(String json)throws JSONException {
        Object value=new JSONTokener(json).nextValue();
        return value instanceof JSONObject?((JSONObject)value).optJSONObject("layout"):null;
    }
    public static String pack(JSONArray parts,Object level) {
        try {JSONObject o=new JSONObject();o.put("parts",parts);JSONObject l=new JSONObject();
            if(level instanceof Level){Level v=(Level)level;if(!v.sandbox())return parts.toString();l.put("sx",v.spawnX);l.put("sy",v.spawnY);l.put("gx",v.goalX);l.put("gy",v.goalY);}
            else if(level instanceof VehicleLevel){VehicleLevel v=(VehicleLevel)level;if(!v.sandbox())return parts.toString();l.put("gx",v.goalX);l.put("gy",v.goalY);JSONArray surfaces=new JSONArray();for(VehicleLevel.Surface s:v.surfaces)surfaces.put(new JSONArray(new double[]{s.ax,s.ay,s.bx,s.by,s.twoSided?1:0}));l.put("surfaces",surfaces);}
            else {MechanicLevel v=(MechanicLevel)level;if(!v.sandbox())return parts.toString();l.put("sx",v.cargoX);l.put("sy",v.cargoY);l.put("gx",v.goalX);l.put("gy",v.goalY);JSONArray mounts=new JSONArray();for(int i=0;i<v.mountCount();i++)mounts.put(new JSONArray(new double[]{v.mountX(i),v.mountY(i)}));l.put("mounts",mounts);}
            o.put("layout",l);return o.toString();
        }catch(JSONException e){throw new IllegalStateException(e);}
    }
    private static double coord(JSONObject l,String k,double fallback,double max){double v=l==null?fallback:l.optDouble(k,fallback);return Double.isFinite(v)?Math.max(30,Math.min(max,v)):fallback;}
    public static Level marble(String json)throws JSONException {Level v=LevelCatalog.SANDBOX;JSONObject l=layout(json);return new Level(v.id,v.name,v.task,v.hint,v.discovery,coord(l,"sx",v.spawnX,970),coord(l,"sy",v.spawnY,540),coord(l,"gx",v.goalX,940),coord(l,"gy",v.goalY,540),v.maxRamps,v.bonusRamps,v.fixed,v.solution,v.layoutRevision);}
    public static VehicleLevel vehicle(String json)throws JSONException {VehicleLevel v=VehicleCatalog.SANDBOX;JSONObject l=layout(json);java.util.List<VehicleLevel.Surface> surfaces=new ArrayList<>(v.surfaces);JSONArray a=l==null?null:l.optJSONArray("surfaces");if(a!=null&&a.length()<=40){surfaces.clear();for(int i=0;i<a.length();i++){JSONArray s=a.getJSONArray(i);double ax=s.getDouble(0),ay=s.getDouble(1),bx=s.getDouble(2),by=s.getDouble(3);if(valid(ax,ay)&&valid(bx,by))surfaces.add(new VehicleLevel.Surface(ax,ay,bx,by,s.optInt(4,0)==1));}}return new VehicleLevel(v.id,v.name,v.task,v.hint,coord(l,"gx",v.goalX,940),coord(l,"gy",v.goalY,540),surfaces,v.solution);}
    public static MechanicLevel mechanic(String json)throws JSONException {MechanicLevel v=MechanicCatalog.SANDBOX;JSONObject l=layout(json);double[] mounts=new double[v.mountCount()*2];for(int i=0;i<v.mountCount();i++){mounts[2*i]=v.mountX(i);mounts[2*i+1]=v.mountY(i);}JSONArray a=l==null?null:l.optJSONArray("mounts");if(a!=null&&a.length()==v.mountCount())for(int i=0;i<a.length();i++){JSONArray m=a.getJSONArray(i);if(valid(m.getDouble(0),m.getDouble(1))){mounts[2*i]=m.getDouble(0);mounts[2*i+1]=m.getDouble(1);}}return new MechanicLevel(v.id,v.name,v.task,v.hint,coord(l,"sx",v.cargoX,970),coord(l,"sy",v.cargoY,540),v.cargoMass,coord(l,"gx",v.goalX,940),coord(l,"gy",v.goalY,540),v.rail,mounts,v.solution());}
    private static boolean valid(double x,double y){return Double.isFinite(x)&&Double.isFinite(y)&&x>=0&&x<=1000&&y>=0&&y<=600;}
}
