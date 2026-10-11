package de.haberland.meitueftler;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import de.haberland.meitueftler.game.Level;
import de.haberland.meitueftler.game.vehicle.*;
import de.haberland.meitueftler.game.mechanics.*;
import java.util.function.Supplier;

/** Separate scene editing prevents overlapping scene markers from stealing part gestures. */
final class SandboxEditor {
    private final MainActivity owner;
    private final View view;
    private final Supplier<Object> scene;
    private final Runnable remember,changed;
    private final Supplier<Boolean> running;
    private final VehicleWorkshop vehicle;
    private final MechanicWorkshop mechanic;
    private boolean active,recorded;
    private int selected=-1;
    private double grabX,grabY,startX,startY;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    SandboxEditor(MainActivity owner,View view,Supplier<Object> scene,Runnable remember,Runnable changed,Supplier<Boolean> running,VehicleWorkshop vehicle,MechanicWorkshop mechanic){this.owner=owner;this.view=view;this.scene=scene;this.remember=remember;this.changed=changed;this.running=running;this.vehicle=vehicle;this.mechanic=mechanic;}
    void addTools(LinearLayout tools){
        Button toggle=owner.button("Level bearbeiten",false,()->{});
        toggle.setOnClickListener(v->{if(running.get())return;active=!active;selected=-1;toggle.setText(active?"Fertig · Bauteile bauen":"Level bearbeiten");view.invalidate();});
        tools.addView(toggle,new LinearLayout.LayoutParams(-1,owner.dp(48)));
        tools.addView(owner.text(vehicle==null?mechanic==null?"Level bearbeiten: Start und Ziel ziehen.":"Level bearbeiten: Start, Ziel und Schraubpunkte ziehen.":"Level bearbeiten: Motor, Ziel und Streckenpunkte ziehen.",13,false));
        if(vehicle!=null){LinearLayout row=owner.row();owner.addEqual(row,owner.button("＋ Strecke",false,()->{if(!active||running.get()||vehicle.level.surfaces.size()>=40)return;remember.run();vehicle.level.surfaces.add(new VehicleLevel.Surface(400,450,600,450));selected=2+(vehicle.level.surfaces.size()-1)*2;changed.run();}));owner.addEqual(row,owner.button("Strecke löschen",false,()->{if(!active||running.get()||selected<2||selected>=count())return;remember.run();vehicle.level.surfaces.remove((selected-2)/2);selected=-1;changed.run();}));tools.addView(row);}
    }
    private int count(){Object s=scene.get();if(s instanceof Level)return 2;if(s instanceof VehicleLevel)return 2+((VehicleLevel)s).surfaces.size()*2;return 2+((MechanicLevel)s).mountCount();}
    private double[] point(int i){Object s=scene.get();if(s instanceof Level){Level l=(Level)s;return i==0?new double[]{l.spawnX,l.spawnY}:new double[]{l.goalX,l.goalY};}if(s instanceof VehicleLevel){VehicleLevel l=(VehicleLevel)s;if(i==0){for(VehiclePart p:vehicle.build)if(p.kind==VehiclePart.Kind.MOTOR)return new double[]{p.x,p.y};return new double[]{170,445};}if(i==1)return new double[]{l.goalX,l.goalY};VehicleLevel.Surface p=l.surfaces.get((i-2)/2);return i%2==0?new double[]{p.ax,p.ay}:new double[]{p.bx,p.by};}MechanicLevel l=(MechanicLevel)s;return i==0?new double[]{l.cargoX,l.cargoY}:i==1?new double[]{l.goalX,l.goalY}:new double[]{l.mountX(i-2),l.mountY(i-2)};}
    void draw(Canvas c){if(!active||running.get())return;for(int i=0;i<count();i++){double[] p=point(i);paint.setColor(i==selected?MainActivity.ORANGE:MainActivity.TEAL);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4);c.drawCircle((float)p[0],(float)p[1],25,paint);paint.setStyle(Paint.Style.FILL);paint.setTextSize(18);String label=i==0?"Start":i==1?"Ziel":vehicle!=null?"Strecke "+((i-2)/2+1):"Fixpunkt "+(i-1);c.drawText(label,(float)Math.min(850,p[0]+28),(float)Math.max(28,p[1]-12),paint);}}
    boolean touch(MotionEvent event,double x,double y,double reach){if(!active||running.get())return false;
        int action=event.getActionMasked();if(action==MotionEvent.ACTION_DOWN){selected=-1;recorded=false;double distance=Math.max(30,reach);for(int i=0;i<count();i++){double[] p=point(i);double d=Math.hypot(x-p[0],y-p[1]);if(d<distance){selected=i;distance=d;startX=p[0];startY=p[1];}}grabX=x;grabY=y;view.getParent().requestDisallowInterceptTouchEvent(true);}
        else if((action==MotionEvent.ACTION_MOVE||action==MotionEvent.ACTION_UP)&&selected>=0){if(!recorded&&Math.hypot(x-grabX,y-grabY)>3){remember.run();recorded=true;}if(recorded)move(Math.max(30,Math.min(selected==1?940:970,startX+x-grabX)),Math.max(30,Math.min(540,startY+y-grabY)));}
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){view.getParent().requestDisallowInterceptTouchEvent(false);changed.run();if(action==MotionEvent.ACTION_UP)view.performClick();}view.invalidate();return true;
    }
    private void move(double x,double y){Object s=scene.get();if(s instanceof Level){Level l=(Level)s;if(selected==0){l.spawnX=x;l.spawnY=y;}else{l.goalX=x;l.goalY=y;}}
        else if(s instanceof VehicleLevel){VehicleLevel l=(VehicleLevel)s;if(selected==0){for(VehiclePart p:vehicle.build)if(p.kind==VehiclePart.Kind.MOTOR){p.x=x;p.y=y;p.clamp(l);}}else if(selected==1){l.goalX=x;l.goalY=y;}else{int i=(selected-2)/2;VehicleLevel.Surface p=l.surfaces.get(i);l.surfaces.set(i,selected%2==0?new VehicleLevel.Surface(x,y,p.bx,p.by,p.twoSided):new VehicleLevel.Surface(p.ax,p.ay,x,y,p.twoSided));}}
        else{MechanicLevel l=(MechanicLevel)s;if(selected==0){l.cargoX=x;l.cargoY=y;}else if(selected==1){l.goalX=x;l.goalY=y;}else{int i=selected-2;double oldX=l.mountX(i),oldY=l.mountY(i);for(MechanicPart p:mechanic.build)if(p.mounted()&&Math.hypot(p.x-oldX,p.y-oldY)<24){p.x=x;p.y=y;}l.moveMount(i,x,y);}}
    }
}
