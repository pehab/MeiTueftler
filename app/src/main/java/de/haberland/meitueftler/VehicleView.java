package de.haberland.meitueftler;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;
import de.haberland.meitueftler.game.vehicle.*;
import java.util.List;

/** A scalable free-placement workbench; physics always uses a 1000 × 600 world. */
final class VehicleView extends View {
    private final MainActivity owner;
    private final VehicleWorkshop workshop;
    private final boolean preview;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path groundPath=new Path();
    private final DashPathEffect startDash=new DashPathEffect(new float[]{10,8},0);
    private final DashPathEffect hintDash=new DashPathEffect(new float[]{8,6},0);
    private final DashPathEffect looseDash=new DashPathEffect(new float[]{6,5},0);
    private final List<VehiclePart> previewParts=VehicleCatalog.car(40,180,440);
    private float scale,ox,oy;
    private double grabX,grabY,startX,startY,accumulator;
    private boolean dragging,remembered;
    private long lastFrame;
    private VehiclePhysics lastEngine;
    public VehicleView(Context context) { this(context,context instanceof MainActivity?(MainActivity)context:null,null,true); }
    VehicleView(MainActivity owner,VehicleWorkshop workshop,boolean preview) { this(owner,owner,workshop,preview); }
    private VehicleView(Context context,MainActivity owner,VehicleWorkshop workshop,boolean preview) {
        super(context);this.owner=owner;this.workshop=workshop;this.preview=preview;
        setContentDescription(preview?"Ein selbstgebautes Fahrzeug mit Motor, Holzrahmen und Rädern.":"Fahrzeug-Baufläche. Teile frei verschieben. Der Motor bleibt im markierten Startbereich.");
        setClickable(!preview);setFocusable(!preview);
    }
    void resumeDrawing() { lastFrame=0;accumulator=0;invalidate(); }
    private void fill(int color) { paint.setColor(color);paint.setStyle(Paint.Style.FILL);paint.setPathEffect(null);paint.setShader(null); }
    private void label(Canvas c,String text,float x,float y,int size,int color) { fill(color);paint.setTextSize(size);c.drawText(text,x,y,paint); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);scale=Math.min((getWidth()-12f)/1000,(getHeight()-12f)/600);if(scale<=0)return;
        ox=(getWidth()-1000*scale)/2;oy=(getHeight()-600*scale)/2;
        if(!preview)advance();VehicleLevel level=preview?VehicleCatalog.LEVELS.get(0):workshop.level;
        c.save();c.translate(ox,oy);c.scale(scale,scale);
        fill(0xFF976244);c.drawRoundRect(0,0,1000,600,22,22,paint);fill(0xFFEED6AB);c.drawRoundRect(10,10,990,590,16,16,paint);c.clipRect(10,10,990,590);
        for(int y=60;y<590;y+=70) { fill(0x22956743);c.drawRect(10,y,990,y+2,paint); }
        for(int x=40;x<980;x+=40)for(int y=40;y<580;y+=40){fill(0x334F7767);c.drawCircle(x,y,1.5f,paint);}
        if(!preview&&workshop.engine==null) {
            fill(0x18147D78);c.drawRoundRect((float)level.buildLeft,(float)level.buildTop,(float)level.buildRight,(float)level.buildBottom,10,10,paint);
            paint.setColor(MainActivity.TEAL);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setPathEffect(startDash);
            c.drawRoundRect((float)level.buildLeft,(float)level.buildTop,(float)level.buildRight,(float)level.buildBottom,10,10,paint);
            label(c,"MOTOR-STARTBEREICH",(float)level.buildLeft+8,(float)level.buildTop-10,17,MainActivity.TEAL);
        }
        for(VehicleLevel.Surface surface:level.surfaces) {
            if(!surface.twoSided) {
                groundPath.reset();groundPath.moveTo((float)surface.ax,(float)surface.ay);groundPath.lineTo((float)surface.bx,(float)surface.by);groundPath.lineTo((float)surface.bx,590);groundPath.lineTo((float)surface.ax,590);groundPath.close();fill(0xFF738D7A);c.drawPath(groundPath,paint);
            }
            fill(surface.twoSided?0xFF708B9D:0xFF365D53);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(surface.twoSided?16:5);paint.setStrokeCap(Paint.Cap.ROUND);
            c.drawLine((float)surface.ax,(float)surface.ay,(float)surface.bx,(float)surface.by,paint);paint.setStrokeCap(Paint.Cap.BUTT);
        }
        flag(c,level);
        List<VehiclePart> parts=preview?previewParts:workshop.engine==null?workshop.build:workshop.engine.parts;
        double mass=0,cx=0,cy=0;
        // Frames first, bearings on top; selection is redrawn last for clear editing.
        for(int pass=0;pass<2;pass++)for(int i=0;i<parts.size();i++) {
            VehiclePart p=parts.get(i);if((p.kind==VehiclePart.Kind.FRAME)!=(pass==0))continue;
            boolean powered=preview||workshop.connected(i);
            drawPart(c,p,!preview&&i==workshop.selected,powered,false);
            if(powered){mass+=p.mass();cx+=p.x*p.mass();cy+=p.y*p.mass();}
        }
        if(!preview&&workshop.hintVisible)for(VehiclePart p:level.solution)drawPart(c,p,false,true,true);
        if(mass>0&&!preview) {
            float x=(float)(cx/mass),y=(float)(cy/mass);fill(Color.WHITE);c.drawCircle(x,y,9,paint);fill(0xFFBA7426);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);
            c.drawCircle(x,y,8,paint);c.drawLine(x-13,y,x+13,y,paint);c.drawLine(x,y-13,x,y+13,paint);
            label(c,"Schwerpunkt",x+16,y-10,15,MainActivity.INK);
        }
        if(preview) { label(c,"DEINE IDEE BEKOMMT RÄDER",80,150,29,MainActivity.INK);label(c,"Bauen · Ausprobieren · Weiter tüfteln",80,195,20,MainActivity.TEAL); }
        c.restore();
        if(!preview&&workshop.engine!=null&&workshop.engine.state==VehiclePhysics.State.RUNNING&&!owner.paused)postInvalidateOnAnimation();
    }
    private void flag(Canvas c,VehicleLevel l) {
        float x=(float)l.goalX,y=(float)l.goalY;fill(0x22147D78);c.drawRoundRect(x-35,y-85,x+35,y+85,6,6,paint);
        fill(MainActivity.INK);c.drawRect(x+35,y-90,x+40,y+70,paint);
        for(int row=0;row<3;row++)for(int col=0;col<4;col++){fill((row+col)%2==0?MainActivity.TEAL:Color.WHITE);c.drawRect(x+40+col*12,y-90+row*12,x+52+col*12,y-78+row*12,paint);}
        label(c,"ZIEL",x-20,y-100,20,MainActivity.TEAL);
    }
    private void drawPart(Canvas c,VehiclePart p,boolean selected,boolean powered,boolean ghost) {
        c.save();c.translate((float)p.x,(float)p.y);c.rotate((float)p.angle);
        float r=(float)p.radius(),half=(float)p.halfLength();
        if(ghost) { paint.setColor(0x99147D78);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);paint.setPathEffect(hintDash);c.drawRoundRect(-half-r,-r,half+r,r,r,r,paint);c.restore();return; }
        if(p.kind==VehiclePart.Kind.FRAME) {
            fill(0xFFAE7C43);c.drawRoundRect(-half-r,-r,half+r,r,r,r,paint);fill(0xFFF0C37D);c.drawRoundRect(-half,-r+2,half,2,4,4,paint);
            for(float x=-half+10;x<half;x+=30){fill(0xFF7D5A37);c.drawCircle(x,0,3,paint);}
        } else if(p.kind==VehiclePart.Kind.WHEEL) {
            fill(0xFF263C3E);c.drawCircle(0,0,r,paint);fill(0xFF9CB7AF);c.drawCircle(0,0,r-8,paint);
            c.save();if(!preview&&workshop.engine!=null&&powered)c.rotate((float)(workshop.engine.time*110/p.radius()*180/Math.PI*workshop.engine.motorDirection()));
            for(int spoke=0;spoke<8;spoke++){c.rotate(45);fill(0xFF416B68);c.drawRect(-2,-r+10,2,-6,paint);}c.restore();
            fill(0xFFEBC783);c.drawCircle(0,0,7,paint);
        } else if(p.kind==VehiclePart.Kind.MOTOR) {
            fill(MainActivity.TEAL);c.drawRoundRect(-24,-24,24,24,9,9,paint);fill(0xFFBED9CC);c.drawRoundRect(-20,-20,20,-12,4,4,paint);
            label(c,"M",-12,13,28,Color.WHITE);fill(0xFFEAC783);c.drawCircle(-18,17,3,paint);c.drawCircle(18,17,3,paint);
        } else {
            fill(0xFF708B9D);c.drawCircle(0,0,r,paint);label(c,"8",-6,7,19,Color.WHITE);
        }
        paint.setColor(selected?0xFFDA6339:powered?MainActivity.TEAL:0xFFA66343);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(selected?4:2);
        if(!powered)paint.setPathEffect(looseDash);
        c.drawRoundRect(-half-r-4,-r-4,half+r+4,r+4,r+4,r+4,paint);c.restore();
    }
    private void advance() {
        VehiclePhysics engine=workshop.engine;if(engine!=lastEngine){lastEngine=engine;lastFrame=0;accumulator=0;}
        if(engine==null||owner.paused)return;long now=System.nanoTime();if(lastFrame!=0)accumulator+=Math.min(0.05,(now-lastFrame)/1e9);lastFrame=now;
        while(accumulator>=VehiclePhysics.STEP&&engine.state==VehiclePhysics.State.RUNNING){engine.step();accumulator-=VehiclePhysics.STEP;}
        if(engine.state!=VehiclePhysics.State.RUNNING)post(workshop::finished);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(preview||workshop.engine!=null||scale<=0)return false;
        double x=(event.getX()-ox)/scale,y=(event.getY()-oy)/scale;
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if(x<0||x>1000||y<0||y>600)return false;dragging=false;remembered=false;workshop.selected=-1;
                // Exact hit before generous touch targets, so bearings remain selectable on a frame.
                for(int pass=0;pass<2&&workshop.selected<0;pass++)for(int i=workshop.build.size()-1;i>=0;i--)if(workshop.build.get(i).distance(x,y)<(pass==0?4:Math.max(22,owner.dp(18)/scale))){workshop.selected=i;break;}
                if(workshop.selected>=0){VehiclePart p=workshop.build.get(workshop.selected);dragging=true;grabX=x-p.x;grabY=y-p.y;startX=p.x;startY=p.y;getParent().requestDisallowInterceptTouchEvent(true);}
                workshop.update();invalidate();return true;
            case MotionEvent.ACTION_MOVE:move(x,y);return true;
            case MotionEvent.ACTION_UP:move(x,y);dragging=false;getParent().requestDisallowInterceptTouchEvent(false);workshop.changed();performClick();return true;
            case MotionEvent.ACTION_CANCEL:dragging=false;getParent().requestDisallowInterceptTouchEvent(false);workshop.changed();return true;
            default:return true;
        }
    }
    private void move(double x,double y) {
        if(!dragging||workshop.selected<0)return;
        if(!remembered&&Math.hypot(x-grabX-startX,y-grabY-startY)>3){workshop.remember();remembered=true;}
        if(remembered){VehiclePart p=workshop.build.get(workshop.selected);p.x=x-grabX;p.y=y-grabY;p.clamp(workshop.level);workshop.update();invalidate();}
    }
    @Override public boolean performClick() { super.performClick();return true; }
}
