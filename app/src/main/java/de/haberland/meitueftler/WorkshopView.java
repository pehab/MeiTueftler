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
        fill(Color.rgb(227,235,222));c.drawRoundRect(0,0,1000,600,24,24,paint);
        fill(Color.rgb(201,215,199));for(int x=20;x<1000;x+=40)for(int y=20;y<600;y+=40)c.drawCircle(x,y,2,paint);
        Level level=preview?LevelCatalog.LEVELS.get(0):owner.current;
        fill(0xFFCCDAC8);c.drawRoundRect(15,560,985,585,12,12,paint);
        paint.setTextSize(16);fill(0xFF627B69);c.drawText("Gute Ideen brauchen Versuche.",32,578,paint);
        drawSource(c,level);
        drawBasket(c,level,false);
        for(Ramp r:level.fixed)drawRamp(c,r,false,false);
        List<Ramp> ramps=preview?level.solution:owner.build;
        for(int i=0;i<ramps.size();i++)drawRamp(c,ramps.get(i),!preview&&i==owner.selected,false);
        if(!preview&&owner.hintVisible)for(Ramp r:level.solution)drawRamp(c,r,false,true);
        if(!preview) {
            int n=0;fill(0x77D96A42);for(float[] point:trail)if(n++%3==0)c.drawCircle(point[0],point[1],3,paint);
        }
        double ballX=preview?420:owner.engine==null?level.spawnX:owner.engine.x;
        double ballY=preview?254:owner.engine==null?level.spawnY:owner.engine.y;
        drawBall(c,(float)ballX,(float)ballY);
        drawBasket(c,level,true);
        if(!preview&&owner.engine!=null&&owner.engine.state==PhysicsEngine.State.WON) {
            for(int i=0;i<16;i++) { fill(i%2==0?MainActivity.TEAL:MainActivity.ORANGE);float x=(float)level.goalX+(i%5-2)*22,y=(float)level.goalY-75-(i/5)*18;c.drawRoundRect(x,y,x+7,y+12,2,2,paint); }
        }
        c.restore();
        if(!preview&&owner.engine!=null&&owner.engine.state==PhysicsEngine.State.RUNNING&&!owner.paused)postInvalidateOnAnimation();
    }
    private void advance() {
        PhysicsEngine engine=owner.engine;
        if(engine!=lastEngine){lastEngine=engine;lastFrame=0;accumulator=0;trail.clear();trailSteps=0;}
        if(engine==null||owner.paused)return;
        long now=System.nanoTime();
        if(lastFrame!=0)accumulator+=Math.min(0.05,(now-lastFrame)/1_000_000_000.0);
        lastFrame=now;
        while(accumulator>=PhysicsEngine.STEP&&engine.state==PhysicsEngine.State.RUNNING) {
            engine.step();accumulator-=PhysicsEngine.STEP;
            if(trailSteps++%8==0){trail.addLast(new float[]{(float)engine.x,(float)engine.y});if(trail.size()>150)trail.removeFirst();}
        }
        if(engine.state!=PhysicsEngine.State.RUNNING)post(owner::simulationFinished);
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
        if(!front) {
            fill(0x331A4140);c.drawOval(x-53,y+23,x+53,y+40,paint);
            fill(0xFFF6D5A3);c.drawRoundRect(x-45,y-24,x+45,y+29,9,9,paint);
            fill(MainActivity.ORANGE);c.drawRoundRect(x-48,y-30,x-36,y+30,5,5,paint);c.drawRoundRect(x+36,y-30,x+48,y+30,5,5,paint);
        }else {
            fill(0xFFE7A24D);c.drawRoundRect(x-43,y+8,x+43,y+30,6,6,paint);
            fill(MainActivity.ORANGE);c.drawRoundRect(x-48,y+24,x+48,y+35,5,5,paint);
            fill(0xFFFBEBD1);for(int k=-2;k<=2;k++)c.drawRoundRect(x+k*16-3,y+10,x+k*16+3,y+23,2,2,paint);
        }
    }
    private void drawRamp(Canvas c,Ramp r,boolean selected,boolean ghost) {
        c.save();c.translate((float)r.x,(float)r.y);c.rotate((float)r.angle);
        float half=(float)r.length/2;
        float thickness=(float)r.halfThickness();
        if(ghost) {
            fill(0x55147D78);c.drawRoundRect(-half,-thickness-3,half,thickness+3,8,8,paint);
            paint.setColor(MainActivity.TEAL);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);paint.setPathEffect(new DashPathEffect(new float[]{10,8},0));c.drawRoundRect(-half,-thickness-4,half,thickness+4,9,9,paint);
        }else {
            if(r.kind==Ramp.Kind.BLOCK) {
                fill(0x22192725);c.drawRoundRect(-half-24,-18,half+24,32,24,24,paint);
                fill(0xFF708B9D);c.drawRoundRect(-half-24,-24,half+24,24,24,24,paint);
                fill(0xFFAFC9D6);c.drawRoundRect(-half+12,-21,half-12,-12,5,5,paint);
                fill(0xFF405C70);for(float bolt=-half+24;bolt<half;bolt+=40)c.drawCircle(bolt,7,4,paint);
            } else if(r.kind==Ramp.Kind.TRAMPOLINE) {
                fill(0x22192725);c.drawRoundRect(-half,8,half,37,9,9,paint);
                fill(0xFFCDB9E7);c.drawRoundRect(-half+10,8,half-10,28,6,6,paint);
                paint.setColor(0xFF8060A8);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);
                Path spring=new Path();spring.moveTo(-half+14,18);
                for(float a=-half+22;a<half-12;a+=16){spring.lineTo(a,9);spring.lineTo(a+8,25);}
                c.drawPath(spring,paint);
                fill(0xFF8060A8);c.drawRoundRect(-half,-5,half,5,5,5,paint);
                fill(0xFFDCC7F4);c.drawRoundRect(-half+4,-5,half-4,-1,3,3,paint);
                fill(0xFF5A3D83);c.drawCircle(-half+12,0,5,paint);c.drawCircle(half-12,0,5,paint);
            } else {
            fill(0x22192725);c.drawRoundRect(-half,-3,half,15,9,9,paint);
            fill(r.fixed?0xFF728B8B:0xFFE7B365);c.drawRoundRect(-half,-6,half,6,6,6,paint);
            fill(r.fixed?0xFFADC1BE:0xFFF8D292);c.drawRoundRect(-half+4,-5,half-4,-1,3,3,paint);
            fill(r.fixed?0xFF405E60:0xFFAE7C43);c.drawCircle(-half+15,0,3,paint);c.drawCircle(half-15,0,3,paint);
            }
            if(selected) {
                paint.setColor(MainActivity.TEAL);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4);c.drawRoundRect(-half-thickness-5,-thickness-7,half+thickness+5,thickness+7,12,12,paint);
                fill(MainActivity.TEAL);c.drawCircle(-half,0,7,paint);c.drawCircle(half,0,7,paint);
            }
        }
        c.restore();fill(MainActivity.INK);
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
                if(dragging&&owner.selected>=0&&owner.selected<owner.build.size()) {
                    Ramp r=owner.build.get(owner.selected);
                    if(!remembered&&Math.hypot(x-grabX-startX,y-grabY-startY)>5) { owner.rememberBuild();remembered=true; }
                    if(remembered){r.x=Math.round((x-grabX)/10)*10;r.y=Math.round((y-grabY)/10)*10;r.clamp();invalidate();}
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging=false;getParent().requestDisallowInterceptTouchEvent(false);owner.constructionChanged();if(event.getActionMasked()==MotionEvent.ACTION_UP)performClick();return true;
            default:return true;
        }
    }
    @Override public boolean performClick() { super.performClick();return true; }
}
