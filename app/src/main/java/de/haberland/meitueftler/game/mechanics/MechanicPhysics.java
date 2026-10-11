package de.haberland.meitueftler.game.mechanics;

import java.util.ArrayList;
import java.util.List;
import de.haberland.meitueftler.game.vehicle.VehicleAssembly;
import de.haberland.meitueftler.game.vehicle.VehiclePart;
import de.haberland.meitueftler.game.vehicle.VehiclePhysics;

/** Gravity-driven levers and tension-only ropes. Winches have a finite pulling force. */
public final class MechanicPhysics {
    public static final double STEP=VehiclePhysics.STEP,GRAVITY=VehiclePhysics.GRAVITY,MAX_TIME=120;
    public enum State { RUNNING, WON, RETRY }
    public State state=State.RUNNING;
    public String message="";
    public double time;
    public final List<MechanicPart> parts=new ArrayList<>();
    public final List<Body> bodies=new ArrayList<>();
    public final List<Cable> cables=new ArrayList<>();
    private final List<Node> nodes=new ArrayList<>();
    private final MechanicLevel level;
    private Node cargo;
    public final class Body {
        public double x,y,angle,vx,vy,omega,mass,inertia;
        public boolean pinned,fixed,slider;
        private double cmx,cmy;
        private final List<Node> members=new ArrayList<>();
        double inverse(double px,double py,double nx,double ny) {
            if(fixed)return 0;
            double arm=(px-x)*ny-(py-y)*nx;
            return (pinned?0:slider?ny*ny/mass:1/mass)+(slider?0:arm*arm/inertia);
        }
        double velocity(double px,double py,double nx,double ny) {
            return (vx-omega*(py-y))*nx+(vy+omega*(px-x))*ny;
        }
        void impulse(double px,double py,double nx,double ny,double amount) {
            if(fixed)return;
            if(!pinned){if(!slider)vx+=nx*amount/mass;vy+=ny*amount/mass;}
            if(!slider)omega+=((px-x)*ny-(py-y)*nx)*amount/inertia;
        }
        void correct(double px,double py,double nx,double ny,double amount) {
            if(fixed)return;
            if(!pinned){if(!slider)x+=nx*amount/mass;y+=ny*amount/mass;}
            if(!slider)angle+=((px-x)*ny-(py-y)*nx)*amount/inertia;
        }
    }
    private static final class Node {
        final int part;
        final VehiclePart shape;
        final double mass;
        double lx,ly,initialAngle;
        Body body;
        Node(int part,VehiclePart shape,double mass){this.part=part;this.shape=shape;this.mass=mass;initialAngle=shape.angle;}
    }
    public MechanicPhysics(MechanicLevel level,List<MechanicPart> build) {
        this.level=level;
        if(build.size()>level.maxParts()){fail("Zu viele Bauteile.");return;}
        for(MechanicPart original:build) {
            MechanicPart p=original.copy();p.clamp();parts.add(p);
            if(p.mounted()) {
                int mount=level.nearMount(p.x,p.y);
                if(mount<0)fail("Setze Winden und Umlenkrollen auf einen Befestigungspunkt.");
                else {p.x=level.mountX(mount);p.y=level.mountY(mount);}
            }
        }
        List<VehiclePart> shapes=new ArrayList<>();
        for(int i=0;i<parts.size();i++)if(parts.get(i).solid()) {
            MechanicPart p=parts.get(i);VehiclePart shape=p.shape();shapes.add(shape);
            nodes.add(new Node(i,shape,p.kind==MechanicPart.Kind.BEAM?p.size/90:p.size));
        }
        VehiclePart crate=new VehiclePart(VehiclePart.Kind.WEIGHT,level.cargoX,level.cargoY,18,0);
        cargo=new Node(-1,crate,level.cargoMass);nodes.add(cargo);shapes.add(crate);
        int[] roots=new VehicleAssembly(shapes).rigidRoots;
        for(int i=0;i<nodes.size();i++) {
            Node n=nodes.get(i);Body b=null;
            for(int j=0;j<i;j++)if(roots[j]==roots[i]){b=nodes.get(j).body;break;}
            if(b==null){b=new Body();bodies.add(b);}n.body=b;b.members.add(n);b.mass+=n.mass;b.x+=n.shape.x*n.mass;b.y+=n.shape.y*n.mass;
        }
        for(Body b:bodies) {
            b.x/=b.mass;b.y/=b.mass;double comx=b.x,comy=b.y;int pin=-1;
            for(int i=0;i<level.mountCount();i++)for(Node n:b.members)if(n.part>=0&&parts.get(n.part).kind==MechanicPart.Kind.BEAM&&n.shape.distance(level.mountX(i),level.mountY(i))<5) {
                if(pin>=0&&pin!=i)b.fixed=true;
                if(pin<0)pin=i;
            }
            if(pin>=0){b.pinned=true;b.x=level.mountX(pin);b.y=level.mountY(pin);}
            b.slider=level.rail&&b==cargo.body&&!b.pinned;
            if(level.rail&&b==cargo.body&&b.pinned)fail("Die Kiste sitzt auf einer senkrechten Führung. Verbinde sie mit einem Seil statt mit einem befestigten Balken.");
            b.cmx=comx-b.x;b.cmy=comy-b.y;
            for(Node n:b.members) {
                n.lx=n.shape.x-b.x;n.ly=n.shape.y-b.y;
                double shape=n.shape.kind==VehiclePart.Kind.FRAME?n.shape.size*n.shape.size/12:162;
                b.inertia+=n.mass*(shape+n.lx*n.lx+n.ly*n.ly);
            }
        }
        for(int i=0;i<parts.size();i++)if(parts.get(i).kind==MechanicPart.Kind.ROPE) {
            MechanicPart p=parts.get(i);End a=end(p.x,p.y),b=end(p.bx,p.by);int guide=p.guide;
            if(a==null||b==null){fail("Beide Seilenden müssen Kiste, Balken, Gewicht, Winde oder Befestigung berühren.");continue;}
            if(a.body==b.body&&a.body!=null){fail("Ein Seil braucht zwei unterschiedliche Befestigungen.");continue;}
            if(guide>=0&&(guide>=level.mountCount()||!hasPulley(guide))){fail("Die gewählte Umlenkrolle fehlt. Wähle beim Seil eine vorhandene Rolle.");continue;}
            if(Math.hypot(a.x()-b.x(),a.y()-b.y())<12){fail("Die Seilenden liegen zu nah zusammen.");continue;}
            cables.add(new Cable(i,a,b,guide));
        }
        transform();
    }
    private void fail(String text){state=State.RETRY;message=text;}
    public boolean hasPulley(int mount) {
        for(MechanicPart p:parts)if(p.kind==MechanicPart.Kind.PULLEY&&level.nearMount(p.x,p.y)==mount)return true;return false;
    }
    public double cargoX(){return cargo==null?level.cargoX:cargo.shape.x;}
    public double cargoY(){return cargo==null?level.cargoY:cargo.shape.y;}
    public double cargoAngle(){return cargo==null?0:cargo.shape.angle;}
    public boolean pinned(int part){for(Node n:nodes)if(n.part==part)return n.body.pinned;return parts.get(part).mounted()&&level.nearMount(parts.get(part).x,parts.get(part).y)>=0;}
    private End end(double x,double y) {
        for(int i=0;i<parts.size();i++) {
            MechanicPart p=parts.get(i);
            if(p.kind==MechanicPart.Kind.WINCH&&Math.hypot(x-p.x,y-p.y)<34)return new End(null,p.x,p.y,i);
        }
        Node best=null;double distance=20;
        for(Node n:nodes){double d=n.shape.distance(x,y);if(d<distance){distance=d;best=n;}}
        if(best!=null) {
            // Attach on the touched capsule; the marker remains visually where it was placed.
            return new End(best.body,x-best.body.x,y-best.body.y,-1);
        }
        int mount=level.nearMount(x,y);return mount<0?null:new End(null,level.mountX(mount),level.mountY(mount),-1);
    }
    public final class End {
        final Body body;
        final double lx,ly;
        final int winch;
        End(Body body,double x,double y,int winch){this.body=body;lx=x;ly=y;this.winch=winch;}
        public double x(){return body==null?lx:body.x+Math.cos(body.angle)*lx-Math.sin(body.angle)*ly;}
        public double y(){return body==null?ly:body.y+Math.sin(body.angle)*lx+Math.cos(body.angle)*ly;}
    }
    public final class Cable {
        public final int part,guide;
        public final End a,b;
        public double restLength;
        private double tensionImpulse;
        private final int winch;
        Cable(int part,End a,End b,int guide){this.part=part;this.a=a;this.b=b;this.guide=guide;winch=a.winch>=0?a.winch:b.winch;restLength=length()+parts.get(part).size;}
        public double length(){return guide<0?Math.hypot(a.x()-b.x(),a.y()-b.y()):Math.hypot(a.x()-level.mountX(guide),a.y()-level.mountY(guide))+Math.hypot(b.x()-level.mountX(guide),b.y()-level.mountY(guide));}
        void prepare() {
            tensionImpulse=0;
            if(winch>=0){MechanicPart p=parts.get(winch);restLength=Math.max(40,Math.min(2000,restLength-p.direction*(p.power==1?25:p.power==3?70:45)*STEP));}
        }
        void solve() {
            double ax=a.x(),ay=a.y(),bx=b.x(),by=b.y();
            double gx=guide<0?bx:level.mountX(guide),gy=guide<0?by:level.mountY(guide);
            double anx=ax-gx,any=ay-gy,ad=Math.hypot(anx,any);if(ad<1e-8)return;anx/=ad;any/=ad;
            double bnx=guide<0?-anx:bx-gx,bny=guide<0?-any:by-gy;
            if(guide>=0){double bd=Math.hypot(bnx,bny);if(bd<1e-8)return;bnx/=bd;bny/=bd;}
            double inv=(a.body==null?0:a.body.inverse(ax,ay,anx,any))+(b.body==null?0:b.body.inverse(bx,by,bnx,bny));if(inv<1e-10)return;
            double error=length()-restLength;
            double speed=(a.body==null?0:a.body.velocity(ax,ay,anx,any))+(b.body==null?0:b.body.velocity(bx,by,bnx,bny));
            double force=winch<0?50000:parts.get(winch).power==1?1800:parts.get(winch).power==3?7000:4500;
            double bias=Math.max(-80,Math.min(80,error*0.15/STEP));
            double next=Math.max(0,Math.min(force*STEP,tensionImpulse+(speed+bias)/inv));
            double change=next-tensionImpulse;tensionImpulse=next;
            if(a.body!=null)a.body.impulse(ax,ay,anx,any,-change);
            if(b.body!=null)b.body.impulse(bx,by,bnx,bny,-change);
        }
    }
    private void transform() {
        for(Node n:nodes) {
            Body b=n.body;double c=Math.cos(b.angle),s=Math.sin(b.angle);
            n.shape.x=b.x+c*n.lx-s*n.ly;n.shape.y=b.y+s*n.lx+c*n.ly;n.shape.angle=n.initialAngle+Math.toDegrees(b.angle);
            if(n.part>=0){MechanicPart p=parts.get(n.part);p.x=n.shape.x;p.y=n.shape.y;p.angle=n.shape.angle;}
        }
        for(Cable c:cables){MechanicPart p=parts.get(c.part);p.x=c.a.x();p.y=c.a.y();p.bx=c.b.x();p.by=c.b.y();}
    }
    private void floor(Node n,double px,double py,double radius) {
        if(py+radius<549.5)return;
        Body b=n.body;double inv=b.inverse(px,py,0,-1);if(inv<1e-10)return;
        double speed=b.velocity(px,py,0,-1);if(speed<0)b.impulse(px,py,0,-1,-speed/inv);
        double amount=Math.min(3,Math.max(0,py+radius-550)*0.35)/inv;b.correct(px,py,0,-1,amount);
        double friction=b.inverse(px,py,1,0);if(friction>1e-10)b.impulse(px,py,1,0,-b.velocity(px,py,1,0)*0.08/friction);
    }
    private void contact(Node a,Node b,double px,double py,double radius) {
        if(a.body==b.body)return;
        double angle=Math.toRadians(b.shape.angle),dx=Math.cos(angle),dy=Math.sin(angle);
        double along=Math.max(-b.shape.halfLength(),Math.min(b.shape.halfLength(),(px-b.shape.x)*dx+(py-b.shape.y)*dy));
        double nx=px-b.shape.x-dx*along,ny=py-b.shape.y-dy*along,d=Math.hypot(nx,ny),reach=radius+b.shape.radius();
        if(d>=reach||d<1e-8)return;nx/=d;ny/=d;
        double cx=px-nx*radius,cy=py-ny*radius;
        double inv=a.body.inverse(cx,cy,nx,ny)+b.body.inverse(cx,cy,nx,ny);if(inv<1e-10)return;
        double speed=a.body.velocity(cx,cy,nx,ny)-b.body.velocity(cx,cy,nx,ny);
        if(speed<0){a.body.impulse(cx,cy,nx,ny,-speed/inv);b.body.impulse(cx,cy,nx,ny,speed/inv);}
        double correction=Math.min(2,(reach-d)*0.2)/inv;
        a.body.correct(cx,cy,nx,ny,correction);b.body.correct(cx,cy,nx,ny,-correction);
    }
    public void step() {
        if(state!=State.RUNNING)return;time+=STEP;
        for(Body b:bodies)if(!b.fixed) {
            if(b.pinned){double horizontal=Math.cos(b.angle)*b.cmx-Math.sin(b.angle)*b.cmy;b.omega+=b.mass*GRAVITY*horizontal/b.inertia*STEP;}
            else {b.vy=Math.max(-700,Math.min(700,b.vy+GRAVITY*STEP));b.vx=Math.max(-700,Math.min(700,b.vx*0.9999));b.vy*=0.9999;b.x+=b.vx*STEP;b.y+=b.vy*STEP;}
            b.omega*=0.9995;b.omega=Math.max(-6,Math.min(6,b.omega));b.angle+=b.omega*STEP;
        }
        transform();for(Cable c:cables)c.prepare();
        for(int pass=0;pass<8;pass++)for(Cable c:cables)c.solve();
        for(Node n:nodes) {
            double angle=Math.toRadians(n.shape.angle),dx=Math.cos(angle)*n.shape.halfLength(),dy=Math.sin(angle)*n.shape.halfLength();
            int samples=n.shape.kind==VehiclePart.Kind.FRAME?(int)Math.ceil(n.shape.size/18)+1:1;
            for(int i=0;i<samples;i++) {
                double t=samples==1?0:2.0*i/(samples-1)-1,px=n.shape.x+dx*t,py=n.shape.y+dy*t;
                floor(n,px,py,n.shape.radius());
                for(Node other:nodes)if(other!=n)contact(n,other,px,py,n.shape.radius());
            }
        }
        transform();
        if(Math.abs(cargoX()-level.goalX)<26&&Math.abs(cargoY()-level.goalY)<26){state=State.WON;message="Deine Maschine hat die Kiste ins Ziel gebracht!";return;}
        if(time>=MAX_TIME||cargoX()< -80||cargoX()>1080||cargoY()>680)fail("Noch nicht im Ziel. Ändere Hebelarm, Gewicht, Seilverbindung oder Windeneinstellung und probiere es erneut.");
    }
}
