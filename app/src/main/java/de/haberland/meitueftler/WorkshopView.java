package de.haberland.meitueftler;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import de.haberland.meitueftler.game.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

final class WorkshopView extends View {
    private final MainActivity owner;
    private final boolean preview;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private float scale=1,offsetX,offsetY;
    private long lastFrame;
    private double accumulator,grabX,grabY,startX,startY;
    private boolean dragging,remembered;
    private PhysicsEngine lastEngine;
    private final Deque<float[]> trail=new ArrayDeque<>();
    private int trailSteps;
    private long winWallTime;
    WorkshopView(Context context,MainActivity owner,boolean preview) {
        super(context);this.owner=owner;this.preview=preview;
        setContentDescription(preview?"Eine Murmel rollt auf einer Holzrampe zum Korb.":"Baufläche. Bauteile antippen und verschieben. Drehen und Größe über die Tasten daneben ändern.");
        setClickable(!preview);setFocusable(!preview);
    }
    void resumeDrawing() { lastFrame=0;accumulator=0;invalidate(); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        scale=Math.min((getWidth()-12f)/1000f,(getHeight()-12f)/600f);
        if(scale<=0)return;
        offsetX=(getWidth()-1000*scale)/2;offsetY=(getHeight()-600*scale)/2;
        if(!preview) advance();
        c.save();c.translate(offsetX,offsetY);c.scale(scale,scale);
        drawWorkbench(c);
        c.clipRect(10,10,990,590);
        Level level=preview?LevelCatalog.LEVELS.get(0):owner.current;
        fill(0xFFEED6A6);c.drawRoundRect(22,562,410,587,5,5,paint);
        paint.setTextSize(16);fill(0xFF72543B);c.drawText("Gute Ideen brauchen Versuche.",32,578,paint);
        drawSource(c,level);
        if(!preview)drawConnections(c);
        drawBasket(c,level,false);
        for(Ramp r:level.fixed)drawRamp(c,r,false,false);
        List<Ramp> ramps=preview?level.solution:owner.build;
        for(int i=0;i<ramps.size();i++)drawRamp(c,ramps.get(i),!preview&&i==owner.selected,false);
        if(preview){drawRamp(c,new Ramp(680,385,160,-15,false,Ramp.Kind.TRAMPOLINE),false,false);drawRamp(c,new Ramp(125,420,240,-60,false,Ramp.Kind.FAN),false,false);}
        if(!preview&&owner.hintVisible)for(Ramp r:level.solution)drawRamp(c,r,false,true);
        if(!preview) {
            int n=0;fill(0x77D96A42);for(float[] point:trail)if(n++%3==0)c.drawCircle(point[0],point[1],3,paint);
        }
        double ballX=preview?120+400*((Math.sin(animationSeconds()*0.7)+1)/2):owner.engine==null?level.spawnX:owner.engine.x;
        double ballY=preview?230+(ballX-300)*Math.tan(Math.toRadians(20))-19:owner.engine==null?level.spawnY:owner.engine.y;
        drawBall(c,(float)ballX,(float)ballY);
        drawBasket(c,level,true);
        if(!preview&&owner.engine!=null&&owner.engine.state==PhysicsEngine.State.WON) {
            for(int i=0;i<16;i++) { fill(i%2==0?MainActivity.TEAL:MainActivity.ORANGE);float x=(float)level.goalX+(i%5-2)*22,y=(float)level.goalY-75-(i/5)*18;c.drawRoundRect(x,y,x+7,y+12,2,2,paint); }
        }
        c.restore();
        if(!owner.paused && (preview || (!preview&&owner.engine==null&&hasFans())))postInvalidateOnAnimation();
        if(!preview&&owner.engine!=null&&!owner.paused&&(owner.engine.state==PhysicsEngine.State.RUNNING || (winWallTime!=0&&System.nanoTime()-winWallTime<1_200_000_000L)))postInvalidateOnAnimation();
    }
    private boolean hasFans() { for(Ramp r:owner.build)if(r.kind==Ramp.Kind.FAN)return true;return false; }
    private float animationSeconds() { return preview?(System.nanoTime()%60_000_000_000L)/1_000_000_000f:owner.engine==null?(System.nanoTime()%60_000_000_000L)/1_000_000_000f:(float)owner.engine.time; }
    private void drawWorkbench(Canvas c) {
        fill(0xFF976244);c.drawRoundRect(0,0,1000,600,24,24,paint);
        fill(Color.WHITE);paint.setShader(new LinearGradient(0,12,0,588,new int[]{0xFFF0DDB9,0xFFE7CEA2,0xFFDFC08E},null,Shader.TileMode.CLAMP));
        c.drawRoundRect(10,10,990,590,18,18,paint);paint.setShader(null);
        c.save();Path clip=new Path();clip.addRoundRect(10,10,990,590,18,18,Path.Direction.CW);c.clipPath(clip);
        for(int row=0;row<8;row++) {
            float y=18+row*76;
            paint.setColor(0x22956743);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);
            for(int line=0;line<4;line++) {
                Path grain=new Path();grain.moveTo(-10,y+line*13);
                for(int x=0;x<1000;x+=100)grain.quadTo(x+50,y+line*13+(float)Math.sin(x*0.016+row)*7,x+100,y+line*13);
                c.drawPath(grain,paint);
            }
            paint.setColor(0x338E6440);paint.setStrokeWidth(2);c.drawLine(10,y+70,990,y+70,paint);
        }
        fill(0x224D6B60);for(int x=40;x<980;x+=40)for(int y=40;y<550;y+=40)c.drawCircle(x,y,1.5f,paint);
        paint.setColor(0x228A633F);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);
        c.drawOval(738,102,780,114,paint);c.drawOval(748,105,770,111,paint);
        c.drawOval(162,435,188,447,paint);
        c.restore();
        for(int x:new int[]{20,980})for(int y:new int[]{20,580}) {
            fill(0xFFB1A48A);c.drawCircle(x,y,6,paint);paint.setColor(0xFF71695B);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);c.drawLine(x-3,y+2,x+3,y-2,paint);
        }
        // Small objects stay on the bottom rim, away from the working area.
        c.save();c.translate(885,574);c.rotate(-8);
        fill(0xFFF8ECC9);c.drawRoundRect(-55,-13,55,15,3,3,paint);
        fill(MainActivity.TEAL);paint.setTextSize(14);c.drawText("Ideen gesucht!",-45,5,paint);
        c.restore();
        c.save();c.translate(682,575);c.rotate(8);
        fill(0xFFCC684C);c.drawRoundRect(-38,-4,38,4,3,3,paint);fill(0xFFECDCB5);
        Path tip=new Path();tip.moveTo(38,-4);tip.lineTo(49,0);tip.lineTo(38,4);tip.close();c.drawPath(tip,paint);
        fill(0xFF3B4843);c.drawCircle(47,0,1.5f,paint);c.restore();
        fill(MainActivity.INK);
    }
    private void advance() {
        PhysicsEngine engine=owner.engine;
        if(engine!=lastEngine){winWallTime=0;lastEngine=engine;lastFrame=0;accumulator=0;trail.clear();trailSteps=0;}
        if(engine==null||owner.paused)return;
        long now=System.nanoTime();
        if(lastFrame!=0)accumulator+=Math.min(0.05,(now-lastFrame)/1_000_000_000.0);
        lastFrame=now;
        while(accumulator>=PhysicsEngine.STEP&&engine.state==PhysicsEngine.State.RUNNING) {
            engine.step();accumulator-=PhysicsEngine.STEP;
            if(trailSteps++%8==0){trail.addLast(new float[]{(float)engine.x,(float)engine.y});if(trail.size()>150)trail.removeFirst();}
        }
        if(engine.state!=PhysicsEngine.State.RUNNING){if(engine.state==PhysicsEngine.State.WON&&winWallTime==0)winWallTime=System.nanoTime();post(owner::simulationFinished);}
    }
    private void fill(int color) { paint.setColor(color);paint.setStyle(Paint.Style.FILL);paint.setShader(null);paint.setPathEffect(null); }
    private void drawSource(Canvas c,Level l) {
        float x=(float)l.spawnX,y=(float)l.spawnY;
        fill(0x221F4544);c.drawRoundRect(x-34,8,x+38,y-12,9,9,paint);
        fill(MainActivity.TEAL);c.drawRoundRect(x-38,4,x+34,y-16,9,9,paint);
        fill(0xFFD4EBDD);c.drawCircle(x-20,20,5,paint);c.drawCircle(x+17,20,5,paint);
        fill(Color.WHITE);paint.setTextSize(15);paint.setTypeface(Typeface.DEFAULT_BOLD);c.drawText("START",x-25,y-25,paint);paint.setTypeface(Typeface.DEFAULT);
    }
    private void drawBasket(Canvas c,Level l,boolean front) {
        float x=(float)l.goalX,y=(float)l.goalY;
        c.save();
        if(!preview&&owner.engine!=null&&owner.engine.state==PhysicsEngine.State.WON) {
            float elapsed=(System.nanoTime()-winWallTime)/1_000_000_000f;
            if(elapsed<1.2f)c.rotate((float)Math.sin(elapsed*22)*5*(1-elapsed/1.2f),x,y+30);
        }
        if(!front) {
            fill(0x331A4140);c.drawOval(x-53,y+23,x+53,y+40,paint);
            fill(0xFFF6D5A3);c.drawRoundRect(x-45,y-24,x+45,y+29,9,9,paint);
            fill(MainActivity.ORANGE);c.drawRoundRect(x-48,y-30,x-36,y+30,5,5,paint);c.drawRoundRect(x+36,y-30,x+48,y+30,5,5,paint);
        }else {
            fill(0xFFE7A24D);c.drawRoundRect(x-43,y+8,x+43,y+30,6,6,paint);
            fill(MainActivity.ORANGE);c.drawRoundRect(x-48,y+24,x+48,y+35,5,5,paint);
            fill(0xFFFBEBD1);for(int k=-2;k<=2;k++)c.drawRoundRect(x+k*16-3,y+10,x+k*16+3,y+23,2,2,paint);
        }
        c.restore();
    }
    private void drawRamp(Canvas c,Ramp r,boolean selected,boolean ghost) {
        c.save();c.translate((float)r.x,(float)r.y);c.rotate((float)r.angle);
        if(r.linked()){drawLinked(c,r,selected,ghost);c.restore();fill(MainActivity.INK);return;}
        if(r.kind==Ramp.Kind.FAN){drawFan(c,r,selected,ghost);c.restore();fill(MainActivity.INK);return;}
        float half=(float)r.length/2;
        float thickness=(float)r.halfThickness();
        if(ghost) {
            float end=half+(r.kind==Ramp.Kind.BLOCK?thickness:0);
            fill(0x55147D78);c.drawRoundRect(-end,-thickness-3,end,thickness+3,8,8,paint);
            paint.setColor(MainActivity.TEAL);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);paint.setPathEffect(new DashPathEffect(new float[]{10,8},0));c.drawRoundRect(-end,-thickness-4,end,thickness+4,9,9,paint);
            fill(MainActivity.TEAL);paint.setTextSize(16);c.drawText(r.label(),-half+12,-thickness-16,paint);
        }else {
            if(r.kind==Ramp.Kind.BLOCK) {
                fill(0x22192725);c.drawRoundRect(-half-24,-18,half+24,32,24,24,paint);
                fill(0xFF708B9D);c.drawRoundRect(-half-24,-24,half+24,24,24,24,paint);
                fill(0xFFAFC9D6);c.drawRoundRect(-half+12,-21,half-12,-12,5,5,paint);
                fill(0xFF405C70);for(float bolt=-half+24;bolt<half;bolt+=40)c.drawCircle(bolt,7,4,paint);
            } else if(r.kind==Ramp.Kind.TRAMPOLINE) {
                float squash=0;
                if(!preview&&owner.engine!=null&&owner.engine.lastBouncedRamp!=null) {
                    Ramp bounced=owner.engine.lastBouncedRamp;
                    double age=owner.engine.time-owner.engine.lastBounceTime;
                    if(age>=0&&age<0.25&&bounced.x==r.x&&bounced.y==r.y)squash=(float)Math.sin(age/0.25*Math.PI)*8;
                }
                c.save();c.translate(0,squash);c.scale(1,1-squash/42);
                fill(0x22192725);c.drawRoundRect(-half,8,half,37,9,9,paint);
                fill(0xFFCDB9E7);c.drawRoundRect(-half+10,8,half-10,28,6,6,paint);
                paint.setColor(0xFF8060A8);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);
                Path spring=new Path();spring.moveTo(-half+14,18);
                for(float a=-half+22;a<half-12;a+=16){spring.lineTo(a,9);spring.lineTo(a+8,25);}
                c.drawPath(spring,paint);
                fill(0xFF8060A8);c.drawRoundRect(-half,-5,half,5,5,5,paint);
                fill(0xFFDCC7F4);c.drawRoundRect(-half+4,-5,half-4,-1,3,3,paint);
                fill(0xFF5A3D83);c.drawCircle(-half+12,0,5,paint);c.drawCircle(half-12,0,5,paint);c.restore();
            } else {
            fill(0x22192725);c.drawRoundRect(-half,-3,half,15,9,9,paint);
            fill(r.fixed?0xFF728B8B:0xFFE7B365);c.drawRoundRect(-half,-6,half,6,6,6,paint);
            fill(r.fixed?0xFFADC1BE:0xFFF8D292);c.drawRoundRect(-half+4,-5,half-4,-1,3,3,paint);
            if(!r.fixed) {
                paint.setColor(0x559E6A3D);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(0.8f);
                c.drawOval(-half+38,-3,-half+62,3,paint);c.drawLine(-half+75,2,half-24,2,paint);
            }
            fill(r.fixed?0xFF405E60:0xFFAE7C43);c.drawCircle(-half+15,0,3,paint);c.drawCircle(half-15,0,3,paint);
            }
            if(selected) {
                paint.setColor(MainActivity.TEAL);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4);c.drawRoundRect(-half-thickness-5,-thickness-7,half+thickness+5,thickness+7,12,12,paint);
                fill(MainActivity.TEAL);c.drawCircle(-half,0,7,paint);c.drawCircle(half,0,7,paint);
            }
        }
        c.restore();fill(MainActivity.INK);
    }
    static int channelColor(int channel) { return new int[]{0xFFC65648,0xFF497DB2,0xFFB79121,0xFF56835C}[Math.max(1,Math.min(4,channel))-1]; }
    private void drawConnections(Canvas c) {
        if(owner.selected<0||owner.selected>=owner.build.size())return;
        Ramp chosen=owner.build.get(owner.selected);if(!chosen.linked())return;
        java.util.ArrayList<Ramp> all=new java.util.ArrayList<>(owner.current.fixed);all.addAll(owner.build);
        paint.setColor(channelColor(chosen.channel));paint.setAlpha(110);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);
        paint.setPathEffect(new DashPathEffect(new float[]{7,9},0));
        for(Ramp r:all)if(r.linked()&&r.channel==chosen.channel&&r.kind!=chosen.kind)c.drawLine((float)chosen.x,(float)chosen.y,(float)r.x,(float)r.y,paint);
        fill(MainActivity.INK);
    }
    private void drawLinked(Canvas c,Ramp r,boolean selected,boolean ghost) {
        boolean open=!ghost&&!preview&&owner.engine!=null&&owner.engine.isOpen(r);
        int color=channelColor(r.channel);float half=(float)r.length/2;
        if(r.kind==Ramp.Kind.SWITCH) {
            fill(0xFFDBC9A4);c.drawCircle(0,0,28,paint);
            fill(color);c.drawCircle(0,0,open?19:23,paint);
            fill(Color.WHITE);paint.setTextSize(20);paint.setTypeface(Typeface.DEFAULT_BOLD);c.drawText(open?"✓":String.valueOf(r.channel),-7,7,paint);paint.setTypeface(Typeface.DEFAULT);
            if(selected){paint.setColor(MainActivity.TEAL);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);c.drawCircle(0,0,34,paint);}
        } else {
            fill(open?(color&0x00FFFFFF)|0x33000000:color);c.drawRoundRect(-half-12,-12,half+12,12,8,8,paint);
            if(!open){paint.setColor(0x77FFFFFF);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);for(float x=-half+8;x<half;x+=24)c.drawLine(x,-9,x+10,9,paint);}
            fill(color);c.drawCircle(-half,0,16,paint);c.drawCircle(half,0,16,paint);
            fill(Color.WHITE);paint.setTextSize(17);c.drawText(String.valueOf(r.channel),-half-5,6,paint);c.drawText(String.valueOf(r.channel),half-5,6,paint);
            if(selected||ghost){paint.setColor(MainActivity.TEAL);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);paint.setPathEffect(new DashPathEffect(new float[]{8,6},0));c.drawRoundRect(-half-17,-17,half+17,17,9,9,paint);}
        }
        if(ghost){fill(color);paint.setTextSize(16);c.drawText(r.label()+" · "+r.channelLabel(),-32,-43,paint);}
    }
    private void drawFan(Canvas c,Ramp r,boolean selected,boolean ghost) {
        float range=(float)r.length,far=35+(range+32)*0.22f;
        fill(ghost?0x154586A0:0x184586A0);
        Path air=new Path();air.moveTo(32,-42);air.lineTo(range+32,-far);air.lineTo(range+32,far);air.lineTo(32,42);air.close();c.drawPath(air,paint);
        paint.setColor(ghost?0xAA3B819B:0x663B819B);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);
        if(ghost)paint.setPathEffect(new DashPathEffect(new float[]{8,6},0));
        c.drawLine(32,-42,range+32,-far,paint);c.drawLine(32,42,range+32,far,paint);paint.setPathEffect(null);
        float phase=owner.paused?0:(animationSeconds()*(40+r.power*25))%70;
        for(int lane=-1;lane<=1;lane++)for(float d=50+phase;d<range+25;d+=70) {
            float y=lane*(25+d*0.17f);
            paint.setColor(0x886098A9);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);
            c.drawLine(d-13,y,d+5,y,paint);c.drawLine(d,y-4,d+5,y,paint);c.drawLine(d,y+4,d+5,y,paint);
        }
        if(ghost){fill(MainActivity.TEAL);paint.setTextSize(16);c.drawText("Ventilator · "+r.powerLabel(),-32,-49,paint);}
        fill(0x332E3B3B);c.drawCircle(3,5,35,paint);
        fill(ghost?0x889EBCB5:0xFF527D83);c.drawCircle(0,0,34,paint);
        fill(0xFFE1D6B2);c.drawCircle(0,0,29,paint);
        c.save();c.rotate(owner.paused?0:animationSeconds()*(90+r.power*70));
        for(int blade=0;blade<3;blade++){c.rotate(120);fill(0xFFDB8056);c.drawOval(0,-9,24,8,paint);}c.restore();
        paint.setColor(0xBB365C61);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);
        c.drawCircle(0,0,23,paint);c.drawCircle(0,0,14,paint);c.drawLine(-28,0,28,0,paint);c.drawLine(0,-28,0,28,paint);
        fill(0xFF365C61);c.drawCircle(0,0,5,paint);
        fill(0xFFD9B477);c.drawRoundRect(-18,29,18,37,4,4,paint);
        if(selected){paint.setColor(MainActivity.TEAL);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4);c.drawCircle(0,0,40,paint);}
    }
    private void drawBall(Canvas c,float x,float y) {
        fill(0x331C4441);c.drawOval(x-13,y+10,x+16,y+18,paint);
        fill(Color.WHITE);paint.setShader(new RadialGradient(x-5,y-6,22,new int[]{0xFFFFBE82,0xFFEE794F,0xFFC44C33},null,Shader.TileMode.CLAMP));
        c.drawCircle(x,y,(float)PhysicsEngine.RADIUS,paint);paint.setShader(null);
        fill(0xFFFDEACA);c.drawCircle(x-5,y-5,3,paint);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(preview||owner.engine!=null||scale<=0)return false;
        double x=(event.getX()-offsetX)/scale,y=(event.getY()-offsetY)/scale;
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if(x<0||x>1000||y<0||y>600)return false;
                dragging=false;remembered=false;owner.selected=-1;
                for(int i=owner.build.size()-1;i>=0;i--)if(owner.build.get(i).distance(x,y)<Math.max(35,owner.dp(24)/scale)) { owner.selected=i;dragging=true;break; }
                if(dragging){Ramp r=owner.build.get(owner.selected);grabX=x-r.x;grabY=y-r.y;startX=r.x;startY=r.y;getParent().requestDisallowInterceptTouchEvent(true);}
                owner.updateControls();invalidate();return true;
            case MotionEvent.ACTION_MOVE:
                moveDraggedPiece(x,y);return true;
            case MotionEvent.ACTION_UP:
                // Android can coalesce MOVE events. The release point is the final drag position.
                moveDraggedPiece(x,y);
                dragging=false;getParent().requestDisallowInterceptTouchEvent(false);owner.constructionChanged();performClick();return true;
            case MotionEvent.ACTION_CANCEL:
                dragging=false;getParent().requestDisallowInterceptTouchEvent(false);owner.constructionChanged();if(event.getActionMasked()==MotionEvent.ACTION_UP)performClick();return true;
            default:return true;
        }
    }
    private void moveDraggedPiece(double x,double y) {
        if(!dragging||owner.selected<0||owner.selected>=owner.build.size())return;
        if(!remembered&&Math.hypot(x-grabX-startX,y-grabY-startY)>5) { owner.rememberBuild();remembered=true; }
        if(remembered) { owner.build.get(owner.selected).moveTo(x-grabX,y-grabY);invalidate(); }
    }
    @Override public boolean performClick() { super.performClick();return true; }
}
