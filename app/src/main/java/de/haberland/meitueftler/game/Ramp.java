package de.haberland.meitueftler.game;

public final class Ramp {
    public enum Kind { PLANK, TRAMPOLINE, BLOCK }
    public final Kind kind;
    public String label() { return kind==Kind.TRAMPOLINE?"Trampolin":kind==Kind.BLOCK?"Block":"Brett"; }
    public double halfThickness() { return kind==Kind.BLOCK?24:5; }
    public double x, y, length, angle;
    public final boolean fixed;
    public Ramp(double x, double y, double length, double angle, boolean fixed) {
        this(x,y,length,angle,fixed,Kind.PLANK);
    }
    public Ramp(double x,double y,double length,double angle,boolean fixed,Kind kind) {
        this.kind=kind;
        this.x=x; this.y=y; this.length=length; this.angle=angle; this.fixed=fixed;
    }
    public Ramp copy() { return new Ramp(x,y,length,angle,fixed,kind); }
    public double dx() { return Math.cos(Math.toRadians(angle))*length/2; }
    public double dy() { return Math.sin(Math.toRadians(angle))*length/2; }
    public void clamp() {
        double marginX=Math.abs(dx())+halfThickness()+5, marginY=Math.abs(dy())+halfThickness()+5;
        x=Math.max(marginX,Math.min(1000-marginX,x));
        y=Math.max(marginY,Math.min(600-marginY,y));
    }
    public double distance(double px,double py) {
        double ax=x-dx(),ay=y-dy(),bx=x+dx(),by=y+dy();
        double t=Math.max(0,Math.min(1,((px-ax)*(bx-ax)+(py-ay)*(by-ay))/(length*length)));
        return Math.max(0,Math.hypot(px-ax-t*(bx-ax),py-ay-t*(by-ay))-halfThickness());
    }
}
