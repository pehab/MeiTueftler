package de.haberland.meitueftler.game;
import java.util.ArrayList;
import java.util.List;

/** Fixed 240 Hz physics in a 1000 x 600 workshop. Rendering does not control simulation speed. */
public final class PhysicsEngine {
    public static final double STEP=1.0/240, RADIUS=14, CONTACT=19;
    public enum State { RUNNING, WON, RETRY }
    public double x,y,vx,vy,time;
    public State state=State.RUNNING;
    public final List<Ramp> ramps=new ArrayList<>();
    private final Level level;
    private double stillTime;
    public PhysicsEngine(Level level,List<Ramp> build) {
        this.level=level; x=level.spawnX;y=level.spawnY;
        for(Ramp ramp:level.fixed) ramps.add(ramp.copy());
        for(Ramp ramp:build) ramps.add(ramp.copy());
    }
    public void step() {
        if(state!=State.RUNNING) return;
        time+=STEP;vy+=620*STEP;
        vx*=0.9999;vy*=0.9999;
        double speed=Math.hypot(vx,vy);
        if(speed>1200) { vx*=1200/speed;vy*=1200/speed; }
        x+=vx*STEP;y+=vy*STEP;
        for(int pass=0;pass<3;pass++) for(Ramp ramp:ramps) collide(ramp);
        if(Math.abs(x-level.goalX)<32 && y>=level.goalY-30 && y<=level.goalY+26 && vy>=-30) {
            state=State.WON;x=level.goalX;y=level.goalY-5;vx=0;vy=0;return;
        }
        stillTime = Math.hypot(vx,vy)<5 ? stillTime+STEP : 0;
        if(y>650 || x< -50 || x>1050 || time>16 || stillTime>2) state=State.RETRY;
    }
    private void collide(Ramp r) {
        double ax=r.x-r.dx(),ay=r.y-r.dy(),bx=r.x+r.dx(),by=r.y+r.dy();
        double t=Math.max(0,Math.min(1,((x-ax)*(bx-ax)+(y-ay)*(by-ay))/(r.length*r.length)));
        double nx=x-ax-t*(bx-ax),ny=y-ay-t*(by-ay),distance=Math.hypot(nx,ny);
        if(distance>=CONTACT) return;
        if(distance<0.000001) { nx=Math.sin(Math.toRadians(r.angle));ny=-Math.cos(Math.toRadians(r.angle)); }
        else { nx/=distance;ny/=distance; }
        x+=nx*(CONTACT-distance);y+=ny*(CONTACT-distance);
        double normal=vx*nx+vy*ny;
        if(normal<0) { vx-=1.12*normal*nx;vy-=1.12*normal*ny; }
        // Rolling loss, rather than killing the tangential motion on contact.
        double tangent=-vx*ny+vy*nx;
        vx+=0.0012*tangent*ny;vy-=0.0012*tangent*nx;
    }
}
