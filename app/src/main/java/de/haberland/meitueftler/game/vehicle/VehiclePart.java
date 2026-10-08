package de.haberland.meitueftler.game.vehicle;

/** Free placement; all dimensions are workshop units, independent of screen density. */
public final class VehiclePart {
    public enum Kind { MOTOR, FRAME, WHEEL, WEIGHT, HINGE, DRIVE, FOOT }
    public final Kind kind;
    public double x,y,size,angle;
    public int power=2, direction=1;
    public VehiclePart(Kind kind,double x,double y,double size,double angle) {
        this.kind=kind;this.x=x;this.y=y;this.size=size;this.angle=angle;
    }
    public VehiclePart copy() { VehiclePart p=new VehiclePart(kind,x,y,size,angle);p.power=power;p.direction=direction;return p; }
    public boolean joint() { return kind==Kind.HINGE||kind==Kind.DRIVE; }
    public String label() { return kind==Kind.MOTOR?"Motor":kind==Kind.FRAME?"Rahmen":kind==Kind.WHEEL?"Rad":kind==Kind.HINGE?"Gelenk":kind==Kind.DRIVE?"Drehantrieb":kind==Kind.FOOT?"Greiffuß":"Gewicht"; }
    public double radius() { return joint()?14:kind==Kind.FRAME?9:kind==Kind.MOTOR?24:kind==Kind.WEIGHT||kind==Kind.FOOT?18:size; }
    public double halfLength() { return kind==Kind.FRAME?size/2:0; }
    public double mass() { return joint()?0.35:kind==Kind.FRAME?size/90:kind==Kind.MOTOR?4:kind==Kind.WEIGHT?8:kind==Kind.FOOT?1:size/32; }
    public double distance(double px,double py) {
        double a=Math.toRadians(angle),dx=px-x,dy=py-y;
        double along=dx*Math.cos(a)+dy*Math.sin(a),side=-dx*Math.sin(a)+dy*Math.cos(a);
        return Math.max(0,Math.hypot(Math.max(0,Math.abs(along)-halfLength()),side)-radius());
    }
    public void clamp(VehicleLevel level) {
        size=kind==Kind.FRAME?Math.max(60,Math.min(320,size)):kind==Kind.WHEEL?Math.max(20,Math.min(64,size)):radius();
        angle=((angle+180)%360+360)%360-180;
        double a=Math.toRadians(angle),mx=Math.abs(Math.cos(a))*halfLength()+radius(),my=Math.abs(Math.sin(a))*halfLength()+radius();
        x=Math.max(12+mx,Math.min(988-mx,x));y=Math.max(12+my,Math.min(578-my,y));
        power=Math.max(1,Math.min(3,power));direction=direction<0?-1:1;
        if(kind==Kind.MOTOR) {
            x=Math.max(level.buildLeft+radius(),Math.min(level.buildRight-radius(),x));
            y=Math.max(level.buildTop+radius(),Math.min(level.buildBottom-radius(),y));
        }
    }
}
