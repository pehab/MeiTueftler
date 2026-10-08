package de.haberland.meitueftler.game.vehicle;

import java.util.ArrayList;
import java.util.List;

/** Deterministic rigid assemblies with wheel bearings, gravity, torque and friction contacts. */
public final class VehiclePhysics {
    public static final double STEP=1.0/240, GRAVITY=460, CONNECTION_GAP=1.5, MAX_TIME=120;
    public enum State { RUNNING, WON, RETRY }
    public final List<VehiclePart> parts=new ArrayList<>();
    public final List<Body> bodies=new ArrayList<>();
    public final List<Joint> joints=new ArrayList<>();
    public final VehicleAssembly assembly;
    public final boolean[] powered;
    public State state=State.RUNNING;
    public double time;
    public String message="";
    private final VehicleLevel level;
    private final List<VehiclePart> local=new ArrayList<>();
    private final Body[] owners;
    private final int motor;
    private double stalled;
    private int poweredWheelCount, poweredDriveCount;
    private boolean[][] linkedBodies;
    public static final class Body {
        public double x,y,angle,vx,vy,omega,mass,inertia;
        public boolean hasMotor,hasDrive;
        public int index;
    }
    public VehiclePhysics(VehicleLevel level,List<VehiclePart> build) {
        this.level=level;
        int motorIndex=-1;
        for(VehiclePart p:build) { parts.add(p.copy());if(p.kind==VehiclePart.Kind.MOTOR)motorIndex=parts.size()-1; }
        motor=motorIndex;owners=new Body[parts.size()];powered=new boolean[parts.size()];
        assembly=new VehicleAssembly(parts);int[] roots=assembly.rigidRoots;
        for(int i=0;i<parts.size();i++) {
            Body b=null;
            for(int j=0;j<i;j++)if(roots[j]==roots[i]) { b=owners[j];break; }
            if(b==null) { b=new Body();b.index=bodies.size();bodies.add(b); }
            VehiclePart p=parts.get(i);owners[i]=b;b.mass+=p.mass();b.x+=p.x*p.mass();b.y+=p.y*p.mass();
            b.hasMotor|=p.kind==VehiclePart.Kind.MOTOR;
        }
        for(Body b:bodies) { b.x/=b.mass;b.y/=b.mass; }
        for(int i=0;i<parts.size();i++) {
            VehiclePart p=parts.get(i),q=p.copy();Body b=owners[i];q.x-=b.x;q.y-=b.y;local.add(q);powered[i]=motor>=0&&assembly.connectedRoots[i]==assembly.connectedRoots[motor];
            b.hasDrive|=powered[i];if(powered[i]&&p.kind==VehiclePart.Kind.WHEEL)poweredWheelCount++;
            double shape=p.kind==VehiclePart.Kind.FRAME?p.size*p.size/12:p.radius()*p.radius()/2;
            b.inertia+=p.mass()*(shape+q.x*q.x+q.y*q.y);
        }
        linkedBodies=new boolean[bodies.size()][bodies.size()];
        for(VehicleAssembly.Link link:assembly.links) {
            Body a=owners[link.a],b=owners[link.b];
            // The side closest to the motor is the base; clockwise always turns the outer side.
            int da=distanceToMotor(link.a),db=distanceToMotor(link.b);
            if(db<da) { Body swap=a;a=b;b=swap; }
            joints.add(new Joint(link.marker,a,b));
            int ai=a.index,bi=b.index;linkedBodies[ai][bi]=linkedBodies[bi][ai]=true;
            if(parts.get(link.marker).kind==VehiclePart.Kind.DRIVE&&powered[link.marker])poweredDriveCount++;
        }
        if(motor<0) { state=State.RETRY;message="Ein Motor fehlt."; }
        else if(parts.get(motor).x<level.buildLeft+24||parts.get(motor).x>level.buildRight-24||parts.get(motor).y<level.buildTop+24||parts.get(motor).y>level.buildBottom-24) {
            state=State.RETRY;message="Der Motor muss im Startbereich bleiben.";
        }
        else if(assembly.invalidJoints>0) { state=State.RETRY;message="Setze jedes Gelenk an die Berührung von genau zwei getrennten Baugruppen. Orange Gelenke sind noch nicht richtig verbunden."; }
    }
    private int distanceToMotor(int index) {
        if(motor<0)return parts.size();
        int[] distance=new int[parts.size()];java.util.Arrays.fill(distance,parts.size());distance[assembly.rigidRoots[motor]]=0;
        for(int pass=0;pass<assembly.links.size();pass++)for(VehicleAssembly.Link link:assembly.links) {
            distance[link.a]=Math.min(distance[link.a],distance[link.b]+1);
            distance[link.b]=Math.min(distance[link.b],distance[link.a]+1);
        }
        return distance[assembly.rigidRoots[index]];
    }
    public static int[] connections(List<VehiclePart> parts) { return new VehicleAssembly(parts).connectedRoots; }
    public static boolean touching(VehiclePart a,VehiclePart b) { return VehicleAssembly.touching(a,b); }
    public int motorDirection() { return motor<0?1:parts.get(motor).direction; }
    public double motorX() { return motor<0?0:parts.get(motor).x; }
    public double motorY() { return motor<0?0:parts.get(motor).y; }
    public Body motorBody() { return motor<0?null:owners[motor]; }
    public Body bodyForPart(int index) { return owners[index]; }
    public int looseParts() { int n=0;for(boolean p:powered)if(!p)n++;return n; }
    private void transform() {
        for(int i=0;i<parts.size();i++) {
            Body b=owners[i];VehiclePart p=parts.get(i),q=local.get(i);double c=Math.cos(b.angle),s=Math.sin(b.angle);
            p.x=b.x+c*q.x-s*q.y;p.y=b.y+s*q.x+c*q.y;p.angle=q.angle+Math.toDegrees(b.angle);
        }
        for(Joint joint:joints)parts.get(joint.marker).angle=local.get(joint.marker).angle+Math.toDegrees(joint.b.angle);
    }
    public void step() {
        if(state!=State.RUNNING)return;
        time+=STEP;
        for(Body b:bodies) {
            b.vy+=GRAVITY*STEP;b.vx*=0.99998;b.vy*=0.99998;b.omega*=0.9998;
            double speed=Math.hypot(b.vx,b.vy);if(speed>700){b.vx*=700/speed;b.vy*=700/speed;}
            b.omega=Math.max(-8,Math.min(8,b.omega));b.x+=b.vx*STEP;b.y+=b.vy*STEP;b.angle+=b.omega*STEP;
        }
        transform();List<Contact> contacts=new ArrayList<>();
        for(int i=0;i<parts.size();i++) {
            VehiclePart p=parts.get(i);if(p.joint())continue;double a=Math.toRadians(p.angle),dx=Math.cos(a)*p.halfLength(),dy=Math.sin(a)*p.halfLength();
            int samples=p.kind==VehiclePart.Kind.FRAME?(int)Math.ceil(p.size/18)+1:1;
            for(int sample=0;sample<samples;sample++) {
                double t=samples==1?0:2.0*sample/(samples-1)-1,px=p.x+dx*t,py=p.y+dy*t;
                for(VehicleLevel.Surface surface:level.surfaces)surfaceContact(contacts,i,px,py,p.radius(),surface);
                for(int j=i+1;j<parts.size();j++)if(!parts.get(j).joint()&&owners[i]!=owners[j]&&!linkedBodies[owners[i].index][owners[j].index])partContact(contacts,i,j,px,py,p.radius());
            }
        }
        for(Joint joint:joints)joint.driveImpulse=0;
        for(int pass=0;pass<8;pass++) {
            for(Contact c:contacts)c.solve();
            for(Joint joint:joints)joint.solve();
        }
        // Position correction is separate from velocity impulses and never generates propulsion.
        for(Contact c:contacts)c.correct();
        for(int pass=0;pass<4;pass++)for(Joint joint:joints)joint.correct();
        transform();
        if(Math.abs(motorX()-level.goalX)<35&&Math.abs(motorY()-level.goalY)<85) { state=State.WON;message="Deine Maschine hat den Motor ins Ziel gebracht!";return; }
        boolean moving=false;
        for(Body b:bodies)if(b.hasDrive&&(Math.hypot(b.vx,b.vy)>=3||Math.abs(b.omega)>=0.05)){moving=true;break;}
        stalled=moving?0:stalled+STEP;
        if(motorY()>680||motorX()< -100||motorX()>1100||time>=MAX_TIME||(!level.sandbox()&&stalled>4)) {
            state=State.RETRY;message=looseParts()>0?"Lose Teile bekommen keinen Antrieb. Verbinde sie mit dem Motor und probiere es erneut.":"Noch nicht im Ziel. Ändere Räder, Schwerpunkt oder Motorstärke und probiere es erneut.";
        }
    }
    public final class Joint {
        public final int marker;
        public final Body a,b;
        private final double ax,ay,bx,by;
        private double driveImpulse;
        Joint(int marker,Body a,Body b) {
            this.marker=marker;this.a=a;this.b=b;VehiclePart p=parts.get(marker);
            ax=p.x-a.x;ay=p.y-a.y;bx=p.x-b.x;by=p.y-b.y;
        }
        private double rx(Body body,double x,double y) { return Math.cos(body.angle)*x-Math.sin(body.angle)*y; }
        private double ry(Body body,double x,double y) { return Math.sin(body.angle)*x+Math.cos(body.angle)*y; }
        public double anchorError() { return Math.hypot(b.x+rx(b,bx,by)-a.x-rx(a,ax,ay),b.y+ry(b,bx,by)-a.y-ry(a,ax,ay)); }
        private void apply(Body body,double x,double y,double rx,double ry) {
            body.vx+=x/body.mass;body.vy+=y/body.mass;body.omega+=(rx*y-ry*x)/body.inertia;
        }
        void solve() {
            double arx=rx(a,ax,ay),ary=ry(a,ax,ay),brx=rx(b,bx,by),bry=ry(b,bx,by);
            double ex=b.x+brx-a.x-arx,ey=b.y+bry-a.y-ary;
            double vx=b.vx-b.omega*bry-a.vx+a.omega*ary,vy=b.vy+b.omega*brx-a.vy-a.omega*arx;
            vx+=Math.max(-60,Math.min(60,0.12*ex/STEP));vy+=Math.max(-60,Math.min(60,0.12*ey/STEP));
            double kx=1/a.mass+1/b.mass+ary*ary/a.inertia+bry*bry/b.inertia;
            double ky=1/a.mass+1/b.mass+arx*arx/a.inertia+brx*brx/b.inertia;
            double kxy=-arx*ary/a.inertia-brx*bry/b.inertia,det=kx*ky-kxy*kxy;
            double ix=(-ky*vx+kxy*vy)/det,iy=(kxy*vx-kx*vy)/det;
            apply(a,-ix,-iy,arx,ary);apply(b,ix,iy,brx,bry);
            VehiclePart p=parts.get(marker);boolean active=p.kind==VehiclePart.Kind.DRIVE&&powered[marker];
            double target=active?p.direction*(p.power==1?0.9:p.power==3?2.7:1.8):0;
            double limit=active?parts.get(motor).power*400000.0/Math.max(1,poweredDriveCount)*STEP:80*STEP;
            double next=Math.max(-limit,Math.min(limit,driveImpulse+(target-(b.omega-a.omega))/(1/a.inertia+1/b.inertia)));
            double change=next-driveImpulse;driveImpulse=next;a.omega-=change/a.inertia;b.omega+=change/b.inertia;
        }
        void correct() {
            double arx=rx(a,ax,ay),ary=ry(a,ax,ay),brx=rx(b,bx,by),bry=ry(b,bx,by);
            double ex=b.x+brx-a.x-arx,ey=b.y+bry-a.y-ary;
            double length=Math.hypot(ex,ey),factor=length>0?Math.min(0.25,2/length):0;ex*=factor;ey*=factor;
            double kx=1/a.mass+1/b.mass+ary*ary/a.inertia+bry*bry/b.inertia;
            double ky=1/a.mass+1/b.mass+arx*arx/a.inertia+brx*brx/b.inertia;
            double kxy=-arx*ary/a.inertia-brx*bry/b.inertia,det=kx*ky-kxy*kxy;
            double ix=(-ky*ex+kxy*ey)/det,iy=(kxy*ex-kx*ey)/det;
            a.x-=ix/a.mass;a.y-=iy/a.mass;a.angle-=(arx*iy-ary*ix)/a.inertia;
            b.x+=ix/b.mass;b.y+=iy/b.mass;b.angle+=(brx*iy-bry*ix)/b.inertia;
        }
    }
    private void surfaceContact(List<Contact> contacts,int i,double px,double py,double radius,VehicleLevel.Surface s) {
        double dx=s.bx-s.ax,dy=s.by-s.ay,len2=dx*dx+dy*dy;
        double t=Math.max(0,Math.min(1,((px-s.ax)*dx+(py-s.ay)*dy)/len2)),qx=s.ax+dx*t,qy=s.ay+dy*t;
        double nx=px-qx,ny=py-qy,d=Math.hypot(nx,ny);
        if(d>radius+0.7)return;
        if(d<1e-8) { double len=Math.sqrt(len2);nx=dy/len;ny=-dx/len; }
        else { nx/=d;ny/=d; }
        // Ground is solid underneath; an initial slight overlap is corrected upward.
        if(!s.twoSided&&nx*dy-ny*dx<0) { double len=Math.sqrt(len2);nx=dy/len;ny=-dx/len;d=-d; }
        contacts.add(new Contact(i,-1,px-nx*radius,py-ny*radius,nx,ny,Math.max(0,radius-d)));
    }
    private void partContact(List<Contact> contacts,int i,int j,double px,double py,double radius) {
        VehiclePart q=parts.get(j);double angle=Math.toRadians(q.angle),c=Math.cos(angle),s=Math.sin(angle);
        double along=Math.max(-q.halfLength(),Math.min(q.halfLength(),(px-q.x)*c+(py-q.y)*s));
        double qx=q.x+c*along,qy=q.y+s*along,nx=px-qx,ny=py-qy,d=Math.hypot(nx,ny),reach=radius+q.radius();
        if(d>reach||d<1e-8)return;
        nx/=d;ny/=d;contacts.add(new Contact(i,j,px-nx*radius,py-ny*radius,nx,ny,reach-d));
    }
    private final class Contact {
        final Body a,b;
        final double px,py,nx,ny,depth,drive,friction;
        double normalImpulse,tangentImpulse;
        Contact(int i,int j,double px,double py,double nx,double ny,double depth) {
            a=owners[i];b=j<0?null:owners[j];this.px=px;this.py=py;this.nx=nx;this.ny=ny;this.depth=depth;
            boolean tire=parts.get(i).kind==VehiclePart.Kind.WHEEL;
            // Only terrain contacts can power a wheel; a loose wheel is a passive bearing.
            VehiclePart m=motor<0?null:parts.get(motor);
            drive=tire&&a.hasDrive&&b==null?m.direction*(m.power==1?75:m.power==3?145:110):0;
            friction=parts.get(i).kind==VehiclePart.Kind.FOOT?3.2:tire?2.4:0.55;
        }
        double velocity(Body body,double tx,double ty) { return body==null?0:(body.vx-body.omega*(py-body.y))*tx+(body.vy+body.omega*(px-body.x))*ty; }
        double inverse(Body body,double tx,double ty) { if(body==null)return 0;double cross=(px-body.x)*ty-(py-body.y)*tx;return 1/body.mass+cross*cross/body.inertia; }
        void impulse(Body body,double tx,double ty,double impulse) {
            if(body==null)return;body.vx+=tx*impulse/body.mass;body.vy+=ty*impulse/body.mass;
            body.omega+=((px-body.x)*ty-(py-body.y)*tx)*impulse/body.inertia;
        }
        void solve() {
            double vn=velocity(a,nx,ny)-velocity(b,nx,ny),kn=inverse(a,nx,ny)+inverse(b,nx,ny);
            double next=Math.max(0,normalImpulse-vn/kn),change=next-normalImpulse;normalImpulse=next;
            impulse(a,nx,ny,change);impulse(b,nx,ny,-change);
            double tx=-ny,ty=nx,vt=velocity(a,tx,ty)-velocity(b,tx,ty),kt=inverse(a,tx,ty)+inverse(b,tx,ty);
            double limit=normalImpulse*friction;
            if(drive!=0)limit=Math.min(limit,(parts.get(motor).power*1400.0/Math.max(1,poweredWheelCount))*STEP);
            next=Math.max(-limit,Math.min(limit,tangentImpulse+(drive-vt)/kt));change=next-tangentImpulse;tangentImpulse=next;
            impulse(a,tx,ty,change);impulse(b,tx,ty,-change);
        }
        void correct() {
            double k=inverse(a,nx,ny)+inverse(b,nx,ny),correction=Math.min(3,Math.max(0,depth-0.15)*0.25)/k;
            a.x+=nx*correction/a.mass;a.y+=ny*correction/a.mass;
            a.angle+=((px-a.x)*ny-(py-a.y)*nx)*correction/a.inertia;
            if(b!=null) { b.x-=nx*correction/b.mass;b.y-=ny*correction/b.mass;b.angle-=((px-b.x)*ny-(py-b.y)*nx)*correction/b.inertia; }
        }
    }
}
