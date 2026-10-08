package de.haberland.meitueftler.game.vehicle;

import java.util.List;

public final class VehicleLevel {
    public static final int MAX_PARTS=24;
    public final int id;
    public final String name,task,hint;
    public final double goalX,goalY;
    public final double buildLeft=35,buildRight=320,buildTop=90,buildBottom=495;
    public final List<Surface> surfaces;
    public final List<VehiclePart> solution;
    public VehicleLevel(int id,String name,String task,String hint,double goalX,double goalY,List<Surface> surfaces,List<VehiclePart> solution) {
        this.id=id;this.name=name;this.task=task;this.hint=hint;this.goalX=goalX;this.goalY=goalY;this.surfaces=java.util.Collections.unmodifiableList(new java.util.ArrayList<>(surfaces));this.solution=java.util.Collections.unmodifiableList(new java.util.ArrayList<>(solution));
    }
    public boolean sandbox() { return id<0; }
    public String buildKey() { return "vehicle_build_"+id+"_v1"; }
    public List<VehiclePart> emptyBuild() { return java.util.Arrays.asList(new VehiclePart(VehiclePart.Kind.MOTOR,170,445,24,0)); }
    public static final class Surface {
        public final double ax,ay,bx,by;
        public final boolean twoSided;
        public Surface(double ax,double ay,double bx,double by) { this(ax,ay,bx,by,false); }
        public Surface(double ax,double ay,double bx,double by,boolean twoSided) { this.ax=ax;this.ay=ay;this.bx=bx;this.by=by;this.twoSided=twoSided; }
    }
}
