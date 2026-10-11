package de.haberland.meitueftler.game.mechanics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MechanicLevel {
    public final int id;
    public final String name,task,hint;
    public final double cargoX,cargoY,cargoMass,goalX,goalY;
    public final boolean rail;
    private final double[] mounts;
    private final List<MechanicPart> solution;
    public MechanicLevel(int id,String name,String task,String hint,double cargoX,double cargoY,double cargoMass,
            double goalX,double goalY,boolean rail,double[] mounts,List<MechanicPart> solution) {
        this.id=id;this.name=name;this.task=task;this.hint=hint;this.cargoX=cargoX;this.cargoY=cargoY;
        this.cargoMass=cargoMass;this.goalX=goalX;this.goalY=goalY;this.rail=rail;this.mounts=mounts.clone();
        this.solution=copy(solution);
    }
    private static List<MechanicPart> copy(List<MechanicPart> parts) { List<MechanicPart> out=new ArrayList<>();for(MechanicPart p:parts)out.add(p.copy());return out; }
    public List<MechanicPart> solution() { return Collections.unmodifiableList(copy(solution)); }
    public boolean sandbox() { return id<0; }
    public int maxParts() { return sandbox()?60:24; }
    public int bonusParts() { return Math.max(4,solution.size()); }
    public String buildKey() { return "mechanics_build_"+id+"_v1"; }
    public int mountCount() { return mounts.length/2; }
    public double mountX(int i) { return mounts[2*i]; }
    public double mountY(int i) { return mounts[2*i+1]; }
    public int nearMount(double x,double y) {
        int best=-1;double distance=24;
        for(int i=0;i<mountCount();i++){double d=Math.hypot(x-mountX(i),y-mountY(i));if(d<distance){best=i;distance=d;}}
        return best;
    }
}
