package de.haberland.meitueftler.game.mechanics;

import de.haberland.meitueftler.game.vehicle.VehiclePart;

/** Coordinates use the same 1000 by 600 workbench as the other workshops. */
public final class MechanicPart {
    public enum Kind { BEAM, WEIGHT, ROPE, WINCH, PULLEY }
    public final Kind kind;
    public double x,y,bx,by,size,angle;
    public int power=2,direction=1,guide=-1;
    public MechanicPart(Kind kind,double x,double y,double size,double angle) {
        this.kind=kind;this.x=x;this.y=y;this.size=size;this.angle=angle;bx=x+120;by=y+100;
    }
    public static MechanicPart rope(double ax,double ay,double bx,double by,int guide) {
        MechanicPart p=new MechanicPart(Kind.ROPE,ax,ay,0,0);p.bx=bx;p.by=by;p.guide=guide;return p;
    }
    public MechanicPart copy() {
        MechanicPart p=new MechanicPart(kind,x,y,size,angle);p.bx=bx;p.by=by;p.power=power;p.direction=direction;p.guide=guide;return p;
    }
    public boolean solid() { return kind==Kind.BEAM||kind==Kind.WEIGHT; }
    public boolean mounted() { return kind==Kind.WINCH||kind==Kind.PULLEY; }
    public String label() { return kind==Kind.BEAM?"Balken":kind==Kind.WEIGHT?"Gewicht":kind==Kind.ROPE?"Seil":kind==Kind.WINCH?"Seilwinde":"Umlenkrolle"; }
    public VehiclePart shape() { return new VehiclePart(kind==Kind.BEAM?VehiclePart.Kind.FRAME:VehiclePart.Kind.WEIGHT,x,y,kind==Kind.BEAM?size:18,angle); }
    public double distance(double px,double py) {
        if(kind==Kind.ROPE) {
            double dx=bx-x,dy=by-y,len=dx*dx+dy*dy;
            double t=len==0?0:Math.max(0,Math.min(1,((px-x)*dx+(py-y)*dy)/len));
            return Math.hypot(px-x-t*dx,py-y-t*dy);
        }
        return solid()?shape().distance(px,py):Math.max(0,Math.hypot(px-x,py-y)-22);
    }
    public void clamp() {
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(bx)||!Double.isFinite(by)||!Double.isFinite(size)||!Double.isFinite(angle))throw new IllegalArgumentException("Invalid coordinates");
        size=kind==Kind.BEAM?Math.max(60,Math.min(400,size)):kind==Kind.WEIGHT?Math.max(2,Math.min(12,size)):kind==Kind.ROPE?Math.max(0,Math.min(240,size)):22;
        angle=((angle+180)%360+360)%360-180;
        double r=solid()?shape().radius():22,half=kind==Kind.BEAM?size/2:0;
        double mx=r+Math.abs(Math.cos(Math.toRadians(angle)))*half,my=r+Math.abs(Math.sin(Math.toRadians(angle)))*half;
        x=Math.max(12+mx,Math.min(988-mx,x));y=Math.max(12+my,Math.min(578-my,y));
        bx=Math.max(22,Math.min(978,bx));by=Math.max(22,Math.min(578,by));
        power=Math.max(1,Math.min(3,power));direction=direction<0?-1:1;
    }
}
