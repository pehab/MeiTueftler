package de.haberland.meitueftler;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;
import de.haberland.meitueftler.game.mechanics.*;
import java.util.List;

/** Fixed screw points, movable parts and two individually draggable rope ends. */
final class MechanicView extends View {
    private final MainActivity owner;
    private final MechanicWorkshop workshop;
    private final boolean preview;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final DashPathEffect dashed=new DashPathEffect(new float[]{8,6},0);
    private final List<MechanicPart> previewParts=MechanicCatalog.get(0).solution();
    private float scale,ox,oy;
    private int handle;
    private double startX,startY,grabX,grabY,originalBX,originalBY,accumulator;
    private boolean dragging,remembered;
    private long lastFrame;
    private MechanicPhysics lastEngine;
    public MechanicView(Context context) { this(context,context instanceof MainActivity?(MainActivity)context:null,null,true); }
    MechanicView(MainActivity owner,MechanicWorkshop workshop,boolean preview) { this(owner,owner,workshop,preview); }
    private MechanicView(Context context,MainActivity owner,MechanicWorkshop workshop,boolean preview) {
        super(context);this.owner=owner;this.workshop=workshop;this.preview=preview;
        setContentDescription(preview?"Ein Holzhebel hebt eine Kiste mit einem Gegengewicht.":"Mechanik-Baufläche. Balken und Gewichte frei verschieben. Beide Seilenden einzeln ziehen. Die Kiste und Schraubpunkte sind fest vorgegeben.");
        setClickable(!preview);setFocusable(!preview);
    }
    void resumeDrawing(){lastFrame=0;accumulator=0;invalidate();}
    private void fill(int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);paint.setPathEffect(null);paint.setStrokeCap(Paint.Cap.BUTT);}
    private void stroke(int color,float width){fill(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);}
    private void label(Canvas c,String text,float x,float y,int size,int color){fill(color);paint.setTextSize(size);c.drawText(text,x,y,paint);}
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);scale=Math.min((getWidth()-12f)/1000,(getHeight()-12f)/600);if(scale<=0)return;
        ox=(getWidth()-1000*scale)/2;oy=(getHeight()-600*scale)/2;if(!preview)advance();
        MechanicLevel l=preview?MechanicCatalog.get(0):workshop.level;
        c.save();c.translate(ox,oy);c.scale(scale,scale);
        fill(0xFF976244);c.drawRoundRect(0,0,1000,600,22,22,paint);fill(0xFFEED6AB);c.drawRoundRect(10,10,990,590,16,16,paint);c.clipRect(10,10,990,590);
        for(int y=65;y<590;y+=70){fill(0x22956743);c.drawRect(10,y,990,y+2,paint);}
        fill(0xFF738D7A);c.drawRect(10,550,990,590,paint);stroke(0xFF365D53,5);c.drawLine(10,550,990,550,paint);
        if(l.rail){stroke(0xFF708B9D,6);c.drawLine((float)l.cargoX-30,90,(float)l.cargoX-30,550,paint);c.drawLine((float)l.cargoX+30,90,(float)l.cargoX+30,550,paint);}
        fill(0x33147D78);c.drawRoundRect((float)l.goalX-48,(float)l.goalY-48,(float)l.goalX+48,(float)l.goalY+48,10,10,paint);
        stroke(MainActivity.TEAL,3);paint.setPathEffect(dashed);c.drawRoundRect((float)l.goalX-48,(float)l.goalY-48,(float)l.goalX+48,(float)l.goalY+48,10,10,paint);
        label(c,"ZIEL",(float)l.goalX-24,(float)l.goalY-60,20,MainActivity.TEAL);
        List<MechanicPart> parts=preview?previewParts:workshop.engine==null?workshop.build:workshop.engine.parts;
        for(int pass=0;pass<3;pass++)for(int i=0;i<parts.size();i++) {
            MechanicPart p=parts.get(i);int layer=p.kind==MechanicPart.Kind.ROPE?1:p.kind==MechanicPart.Kind.BEAM?0:2;
            if(layer==pass)drawPart(c,p,l,!preview&&i==workshop.selected,false);
        }
        double x=preview?l.cargoX:workshop.engine==null?l.cargoX:workshop.engine.cargoX();
        double y=preview?l.cargoY:workshop.engine==null?l.cargoY:workshop.engine.cargoY();
        double angle=preview||workshop.engine==null?0:workshop.engine.cargoAngle();
        c.save();c.translate((float)x,(float)y);c.rotate((float)angle);fill(0xFFE4B54E);c.drawRoundRect(-22,-22,22,22,5,5,paint);
        stroke(0xFF805F31,3);c.drawRoundRect(-22,-22,22,22,5,5,paint);c.drawLine(-16,-16,16,16,paint);c.drawLine(16,-16,-16,16,paint);c.restore();
        label(c,"KISTE · "+(int)l.cargoMass,(float)x-34,(float)y+42,15,MainActivity.INK);
        for(int i=0;i<l.mountCount();i++) {
            float mx=(float)l.mountX(i),my=(float)l.mountY(i);fill(0xFF708B9D);c.drawCircle(mx,my,11,paint);stroke(0xFFF4EDDF,2);c.drawLine(mx-5,my-5,mx+5,my+5,paint);c.drawLine(mx-5,my+5,mx+5,my-5,paint);
            label(c,""+(i+1),mx+15,my-12,16,MainActivity.INK);
        }
        if(!preview&&workshop.hintVisible)for(MechanicPart p:l.solution())drawPart(c,p,l,false,true);
        if(preview){label(c,"HEBEN · ZIEHEN · BEWEGEN",70,120,30,MainActivity.INK);label(c,"Was bewirken Hebelarm und Gewicht?",70,164,21,MainActivity.TEAL);}
        c.restore();if(!preview&&workshop.engine!=null&&workshop.engine.state==MechanicPhysics.State.RUNNING&&!owner.paused)postInvalidateOnAnimation();
    }
    private void drawPart(Canvas c,MechanicPart p,MechanicLevel l,boolean selected,boolean ghost) {
        int outline=selected?MainActivity.ORANGE:ghost?0x99147D78:MainActivity.TEAL;
        if(p.kind==MechanicPart.Kind.ROPE) {
            stroke(ghost?outline:0xFF805F31,selected?6:4);if(ghost)paint.setPathEffect(dashed);
            path.reset();path.moveTo((float)p.x,(float)p.y);
            if(p.guide>=0&&p.guide<l.mountCount())path.lineTo((float)l.mountX(p.guide),(float)l.mountY(p.guide));
            path.lineTo((float)p.bx,(float)p.by);c.drawPath(path,paint);
            fill(0xFFE4B54E);c.drawCircle((float)p.x,(float)p.y,8,paint);fill(MainActivity.TEAL);c.drawCircle((float)p.bx,(float)p.by,8,paint);
            if(selected){label(c,"1",(float)p.x+12,(float)p.y-12,20,MainActivity.INK);label(c,"2",(float)p.bx+12,(float)p.by-12,20,MainActivity.INK);}return;
        }
        c.save();c.translate((float)p.x,(float)p.y);c.rotate((float)p.angle);
        float half=p.kind==MechanicPart.Kind.BEAM?(float)p.size/2:0,r=p.kind==MechanicPart.Kind.BEAM?9:22;
        if(ghost){stroke(outline,3);paint.setPathEffect(dashed);c.drawRoundRect(-half-r,-r,half+r,r,r,r,paint);c.restore();return;}
        if(p.kind==MechanicPart.Kind.BEAM) {
            fill(0xFFAE7C43);c.drawRoundRect(-half-9,-9,half+9,9,9,9,paint);fill(0xFFF0C37D);c.drawRoundRect(-half,-7,half,2,4,4,paint);
            for(float x=-half+10;x<half;x+=30){fill(0xFF7D5A37);c.drawCircle(x,0,3,paint);}
        } else if(p.kind==MechanicPart.Kind.WEIGHT) {
            fill(0xFF708B9D);c.drawRoundRect(-18,-18,18,18,6,6,paint);label(c,""+(int)p.size,-8,7,19,Color.WHITE);
        } else {
            boolean valid=l.nearMount(p.x,p.y)>=0;outline=valid?outline:MainActivity.ORANGE;
            fill(p.kind==MechanicPart.Kind.WINCH?MainActivity.TEAL:0xFFE4B54E);c.drawCircle(0,0,22,paint);stroke(MainActivity.INK,3);c.drawCircle(0,0,15,paint);
            c.save();if(!preview&&workshop.engine!=null)c.rotate((float)(workshop.engine.time*110*p.direction));
            for(int i=0;i<6;i++){c.rotate(60);c.drawLine(0,5,0,14,paint);}c.restore();
            if(p.kind==MechanicPart.Kind.WINCH)label(c,"M",-8,7,18,Color.WHITE);
        }
        stroke(outline,selected?4:2);c.drawRoundRect(-half-r-4,-r-4,half+r+4,r+4,r,r,paint);c.restore();
    }
    private void advance() {
        MechanicPhysics e=workshop.engine;if(e!=lastEngine){lastEngine=e;lastFrame=0;accumulator=0;}
        if(e==null||owner.paused)return;long now=System.nanoTime();if(lastFrame!=0)accumulator+=Math.min(0.05,(now-lastFrame)/1e9);lastFrame=now;
        while(accumulator>=MechanicPhysics.STEP&&e.state==MechanicPhysics.State.RUNNING){e.step();accumulator-=MechanicPhysics.STEP;}
        if(e.state!=MechanicPhysics.State.RUNNING)post(workshop::finished);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(preview||workshop.engine!=null||scale<=0)return false;
        double x=(event.getX()-ox)/scale,y=(event.getY()-oy)/scale;
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if(x<0||x>1000||y<0||y>600)return false;dragging=false;remembered=false;workshop.selected=-1;handle=0;
                for(int pass=0;pass<2&&workshop.selected<0;pass++)for(int i=workshop.build.size()-1;i>=0;i--) {
                    MechanicPart p=workshop.build.get(i);double reach=pass==0?4:Math.max(22,owner.dp(18)/scale);
                    double da=Math.hypot(x-p.x,y-p.y),db=Math.hypot(x-p.bx,y-p.by);
                    if(p.kind==MechanicPart.Kind.ROPE&&Math.min(da,db)<(pass==0?10:reach)){workshop.selected=i;handle=da<=db?1:2;break;}
                    if(hitDistance(p,x,y)<reach){workshop.selected=i;break;}
                }
                if(workshop.selected>=0){MechanicPart p=workshop.build.get(workshop.selected);startX=p.x;startY=p.y;originalBX=p.bx;originalBY=p.by;grabX=x;grabY=y;dragging=true;getParent().requestDisallowInterceptTouchEvent(true);}
                workshop.update();invalidate();return true;
            case MotionEvent.ACTION_MOVE:move(x,y);return true;
            case MotionEvent.ACTION_UP:
                move(x,y);snap();dragging=false;getParent().requestDisallowInterceptTouchEvent(false);workshop.changed();performClick();return true;
            case MotionEvent.ACTION_CANCEL:dragging=false;getParent().requestDisallowInterceptTouchEvent(false);workshop.changed();return true;
            default:return true;
        }
    }
    private double hitDistance(MechanicPart p,double x,double y) {
        if(p.kind!=MechanicPart.Kind.ROPE||p.guide<0||p.guide>=workshop.level.mountCount())return p.distance(x,y);
        double gx=workshop.level.mountX(p.guide),gy=workshop.level.mountY(p.guide);
        return Math.min(segmentDistance(p.x,p.y,gx,gy,x,y),segmentDistance(gx,gy,p.bx,p.by,x,y));
    }
    private static double segmentDistance(double ax,double ay,double bx,double by,double x,double y) {
        double dx=bx-ax,dy=by-ay,length=dx*dx+dy*dy;
        double t=length==0?0:Math.max(0,Math.min(1,((x-ax)*dx+(y-ay)*dy)/length));
        return Math.hypot(x-ax-dx*t,y-ay-dy*t);
    }
    private void snap() {
        if(!dragging||!remembered)return;MechanicPart p=workshop.build.get(workshop.selected);
        if(p.mounted()){int mount=workshop.level.nearMount(p.x,p.y);if(mount>=0){p.x=workshop.level.mountX(mount);p.y=workshop.level.mountY(mount);}}
    }
    private void move(double x,double y) {
        if(!dragging||workshop.selected<0)return;double dx=x-grabX,dy=y-grabY;
        if(!remembered&&Math.hypot(dx,dy)>3){workshop.remember();remembered=true;}
        if(remembered){MechanicPart p=workshop.build.get(workshop.selected);if(handle!=2){p.x=startX+dx;p.y=startY+dy;}if(p.kind==MechanicPart.Kind.ROPE&&handle!=1){p.bx=originalBX+dx;p.by=originalBY+dy;}p.clamp();workshop.update();invalidate();}
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
