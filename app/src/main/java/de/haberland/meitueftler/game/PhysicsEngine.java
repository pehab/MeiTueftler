package de.haberland.meitueftler.game;
import java.util.ArrayList;
import java.util.List;

/** Fixed 240 Hz physics in a 1000 x 600 workshop. Rendering does not control simulation speed. */
public final class PhysicsEngine {
    public static final double STEP=1.0/240, RADIUS=14, CONTACT=19;
    public enum State { RUNNING, WON, RETRY }
    public double x,y,vx,vy,time;
    public int trampolineBounces,windSteps;
    public double lastBounceTime=-10;
    public Ramp lastBouncedRamp;
    public State state=State.RUNNING;
    public final List<Ramp> ramps=new ArrayList<>();
    private final Level level;
    private double stillTime;
    private final boolean[] channels=new boolean[4];
    public int switchesTriggered;
    public boolean isOpen(Ramp r) { return r.linked()&&channels[Math.max(1,Math.min(4,r.channel))-1]; }
    public PhysicsEngine(Level level,List<Ramp> build) {
        this.level=level; x=level.spawnX;y=level.spawnY;
        for(Ramp ramp:level.fixed) ramps.add(ramp.copy());
        for(Ramp ramp:build) ramps.add(ramp.copy());
    }
    public void step() {
        if(state!=State.RUNNING) return;
        time+=STEP;vy+=620*STEP;
        for(Ramp r:ramps)if(r.kind==Ramp.Kind.FAN) {
            double weight=r.windWeight(x,y);
            if(weight>0) {
                double acceleration=(r.power==1?700:r.power==3?1600:1100)*weight;
                double a=Math.toRadians(r.angle);
                vx+=Math.cos(a)*acceleration*STEP;vy+=Math.sin(a)*acceleration*STEP;windSteps++;
            }
        }
        vx*=0.9999;vy*=0.9999;
        double speed=Math.hypot(vx,vy);
        if(speed>1200) { vx*=1200/speed;vy*=1200/speed; }
        x+=vx*STEP;y+=vy*STEP;
        // All sensors run before doors collide, independent of construction order.
        for(Ramp r:ramps)if(r.kind==Ramp.Kind.SWITCH&&!isOpen(r)&&r.distance(x,y)<=RADIUS) {
            channels[Math.max(1,Math.min(4,r.channel))-1]=true;switchesTriggered++;
        }
        for(int pass=0;pass<3;pass++) for(Ramp ramp:ramps) collide(ramp);
        if(Math.abs(x-level.goalX)<32 && y>=level.goalY-30 && y<=level.goalY+26 && vy>=-30) {
            state=State.WON;x=level.goalX;y=level.goalY-5;vx=0;vy=0;return;
        }
        stillTime = Math.hypot(vx,vy)<5 ? stillTime+STEP : 0;
        if(y>650 || x< -50 || x>1050 || time>16 || stillTime>2) state=State.RETRY;
    }
    private void collide(Ramp r) {
        if(r.kind==Ramp.Kind.SWITCH||(r.kind==Ramp.Kind.DOOR&&isOpen(r)))return;
        if(r.kind==Ramp.Kind.FAN) {
            double nx=x-r.x,ny=y-r.y,distance=Math.hypot(nx,ny),contact=RADIUS+32;
            if(distance>=contact)return;
            if(distance<0.000001){nx=0;ny=-1;}else{nx/=distance;ny/=distance;}
            x+=nx*(contact-distance);y+=ny*(contact-distance);
            double normal=vx*nx+vy*ny;
            if(normal<0){vx-=1.12*normal*nx;vy-=1.12*normal*ny;}
            return;
        }
        double ax=r.x-r.dx(),ay=r.y-r.dy(),bx=r.x+r.dx(),by=r.y+r.dy();
        double t=Math.max(0,Math.min(1,((x-ax)*(bx-ax)+(y-ay)*(by-ay))/(r.length*r.length)));
        double nx=x-ax-t*(bx-ax),ny=y-ay-t*(by-ay),distance=Math.hypot(nx,ny);
        double contact=RADIUS+r.halfThickness();
        if(distance>=contact) return;
        if(distance<0.000001) { nx=Math.sin(Math.toRadians(r.angle));ny=-Math.cos(Math.toRadians(r.angle)); }
        else { nx/=distance;ny/=distance; }
        x+=nx*(contact-distance);y+=ny*(contact-distance);
        double normal=vx*nx+vy*ny;
        if(normal<0) {
            if(r.kind==Ramp.Kind.TRAMPOLINE && normal< -30) {
                // A spring adds energy; a resting marble must not be kicked on every solver pass.
                double rebound=Math.min(850,Math.max(380,-normal*1.05));
                vx+=(rebound-normal)*nx;vy+=(rebound-normal)*ny;
                trampolineBounces++;lastBounceTime=time;lastBouncedRamp=r;
            } else { vx-=1.12*normal*nx;vy-=1.12*normal*ny; }
        }
        // Rolling loss, rather than killing the tangential motion on contact.
        double tangent=-vx*ny+vy*nx;
        vx+=0.0012*tangent*ny;vy-=0.0012*tangent*nx;
    }
}
