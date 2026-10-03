package de.haberland.meitueftler.game;

public final class Ramp {
    public double x, y, length, angle;
    public final boolean fixed;
    public Ramp(double x, double y, double length, double angle, boolean fixed) {
        this.x=x; this.y=y; this.length=length; this.angle=angle; this.fixed=fixed;
    }
    public Ramp copy() { return new Ramp(x,y,length,angle,fixed); }
    public double dx() { return Math.cos(Math.toRadians(angle))*length/2; }
    public double dy() { return Math.sin(Math.toRadians(angle))*length/2; }
    public void clamp() {
        double marginX=Math.abs(dx())+10, marginY=Math.abs(dy())+10;
        x=Math.max(marginX,Math.min(1000-marginX,x));
        y=Math.max(marginY,Math.min(600-marginY,y));
    }
    public double distance(double px,double py) {
        double ax=x-dx(),ay=y-dy(),bx=x+dx(),by=y+dy();
        double t=Math.max(0,Math.min(1,((px-ax)*(bx-ax)+(py-ay)*(by-ay))/(length*length)));
        return Math.hypot(px-ax-t*(bx-ax),py-ay-t*(by-ay));
    }
}
