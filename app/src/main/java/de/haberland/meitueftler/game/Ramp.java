package de.haberland.meitueftler.game;

public final class Ramp {
    public enum Kind { PLANK, TRAMPOLINE, BLOCK, FAN, SWITCH, DOOR }
    public final Kind kind;
    public int power=2, channel=1;
    public boolean linked() { return kind==Kind.SWITCH||kind==Kind.DOOR; }
    public String channelLabel() { return new String[]{"Rot 1","Blau 2","Gelb 3","Grün 4"}[Math.max(1,Math.min(4,channel))-1]; }
    public String label() { return kind==Kind.SWITCH?"Schalter":kind==Kind.DOOR?"Tür":kind==Kind.FAN?"Ventilator":kind==Kind.TRAMPOLINE?"Trampolin":kind==Kind.BLOCK?"Block":"Brett"; }
    public double halfThickness() { return kind==Kind.SWITCH?24:kind==Kind.DOOR?12:kind==Kind.FAN?32:kind==Kind.BLOCK?24:5; }
    public double x, y, length, angle;
    public final boolean fixed;
    public Ramp(double x, double y, double length, double angle, boolean fixed) {
        this(x,y,length,angle,fixed,Kind.PLANK);
    }
    public Ramp(double x,double y,double length,double angle,boolean fixed,Kind kind) {
        this.kind=kind;
        this.x=x; this.y=y; this.length=length; this.angle=angle; this.fixed=fixed;
    }
    public Ramp copy() { Ramp r=new Ramp(x,y,length,angle,fixed,kind);r.power=power;r.channel=channel;return r; }
    public double dx() { return (kind==Kind.FAN||kind==Kind.SWITCH)?0:Math.cos(Math.toRadians(angle))*length/2; }
    public double dy() { return (kind==Kind.FAN||kind==Kind.SWITCH)?0:Math.sin(Math.toRadians(angle))*length/2; }
    public static double normalizeAngle(double angle) { return ((angle+180)%360+360)%360-180; }
    public String powerLabel() { return power==1?"sanft":power==3?"kräftig":"mittel"; }
    /** Visible widening cone; weight is zero behind the fan or outside its air stream. */
    public double windWeight(double px,double py) {
        if(kind!=Kind.FAN)return 0;
        double a=Math.toRadians(angle),rx=px-x,ry=py-y;
        double along=rx*Math.cos(a)+ry*Math.sin(a),side=Math.abs(-rx*Math.sin(a)+ry*Math.cos(a));
        if(along<32||along>length+32||side>35+along*0.22)return 0;
        return 1-0.55*(along-32)/length;
    }
    public void clamp() {
        if(kind==Kind.FAN||kind==Kind.SWITCH) { x=Math.max(42,Math.min(958,x));y=Math.max(42,Math.min(558,y));return; }
        // Include the visible spring base and thick rounded ends, even at steep angles.
        double padding=(kind==Kind.TRAMPOLINE?37:halfThickness())+5;
        double radians=Math.toRadians(angle);
        double available=Math.min((1000-2*padding)/Math.max(0.001,Math.abs(Math.cos(radians))),
                (600-2*padding)/Math.max(0.001,Math.abs(Math.sin(radians))));
        length=Math.min(length,40*Math.floor(available/40));
        double marginX=Math.abs(dx())+padding, marginY=Math.abs(dy())+padding;
        x=Math.max(marginX,Math.min(1000-marginX,x));
        y=Math.max(marginY,Math.min(600-marginY,y));
    }
    public double distance(double px,double py) {
        if(kind==Kind.FAN||kind==Kind.SWITCH)return Math.max(0,Math.hypot(px-x,py-y)-halfThickness());
        double ax=x-dx(),ay=y-dy(),bx=x+dx(),by=y+dy();
        double t=Math.max(0,Math.min(1,((px-ax)*(bx-ax)+(py-ay)*(by-ay))/(length*length)));
        return Math.max(0,Math.hypot(px-ax-t*(bx-ax),py-ay-t*(by-ay))-halfThickness());
    }
}
