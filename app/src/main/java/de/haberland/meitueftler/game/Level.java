package de.haberland.meitueftler.game;
import java.util.List;
public final class Level {
    public final int id, maxRamps, bonusRamps;
    public final String name, task, hint, discovery;
    public final double spawnX, spawnY, goalX, goalY;
    public final List<Ramp> fixed, solution;
    public Level(int id,String name,String task,String hint,String discovery,double sx,double sy,double gx,double gy,int max,int bonus,List<Ramp> fixed,List<Ramp> solution) {
        this.id=id;this.name=name;this.task=task;this.hint=hint;this.discovery=discovery;
        spawnX=sx;spawnY=sy;goalX=gx;goalY=gy;maxRamps=max;bonusRamps=bonus;this.fixed=fixed;this.solution=solution;
    }
    public boolean sandbox() { return id<0; }
}
