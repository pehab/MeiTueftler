package de.haberland.meitueftler.game;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public final class PhysicsEngineTest {
    private static Level testLevel(double x,double y) {
        return new Level(99,"Test","","","",x,y,950,590,12,2,Collections.emptyList(),Collections.emptyList());
    }
    private static void finish(PhysicsEngine engine) { for(int i=0;i<4800&&engine.state==PhysicsEngine.State.RUNNING;i++)engine.step(); }
    @Test public void allTenChallengesHaveReachableReferenceSolutions() {
        assertEquals(10,LevelCatalog.LEVELS.size());
        for(Level l:LevelCatalog.LEVELS) {
            assertTrue(l.solution.size()<=l.bonusRamps);
            for(Ramp r:l.solution) { assertEquals(0,r.angle%5,0.001);assertEquals(0,(r.length-160)%40,0.001); }
            PhysicsEngine engine=new PhysicsEngine(l,l.solution);finish(engine);
            assertEquals(l.name,PhysicsEngine.State.WON,engine.state);
        }
    }
    @Test public void emptyBuildFallsAndCanBeRetried() {
        PhysicsEngine engine=new PhysicsEngine(LevelCatalog.LEVELS.get(0),Collections.emptyList());finish(engine);
        assertEquals(PhysicsEngine.State.RETRY,engine.state);
        assertEquals(110,engine.x,0.001);
        PhysicsEngine retry=new PhysicsEngine(LevelCatalog.LEVELS.get(0),LevelCatalog.LEVELS.get(0).solution);finish(retry);
        assertEquals(PhysicsEngine.State.WON,retry.state);
    }
    @Test public void fastMarbleDoesNotTunnelThroughAPlank() {
        PhysicsEngine engine=new PhysicsEngine(testLevel(500,275),Arrays.asList(new Ramp(500,300,480,0,false)));
        engine.vy=1200;
        for(int i=0;i<240;i++)engine.step();
        assertTrue(engine.y<=300-PhysicsEngine.CONTACT+0.001);
        assertTrue(Double.isFinite(engine.x)&&Double.isFinite(engine.y));
    }
    @Test public void stationaryBallStopsSimulationInsteadOfLoopingForever() {
        PhysicsEngine engine=new PhysicsEngine(testLevel(500,60),Arrays.asList(new Ramp(500,300,480,0,false)));finish(engine);
        assertEquals(PhysicsEngine.State.RETRY,engine.state);
        assertTrue(engine.y<300);
    }
    @Test public void simulationUsesSnapshotAndDoesNotChangeConstruction() {
        Ramp ramp=LevelCatalog.LEVELS.get(0).solution.get(0).copy();
        PhysicsEngine engine=new PhysicsEngine(LevelCatalog.LEVELS.get(0),Arrays.asList(ramp));
        ramp.x=800;
        finish(engine);assertEquals(PhysicsEngine.State.WON,engine.state);assertEquals(800,ramp.x,0);
    }
    @Test public void placedRampStaysInsideTheWorkshopIncludingItsEnds() {
        Ramp ramp=new Ramp(-100,900,560,80,false);ramp.clamp();
        assertTrue(ramp.x-Math.abs(ramp.dx())>=9.999);
        assertTrue(ramp.y+Math.abs(ramp.dy())<=590.001);
    }
    @Test public void identicalStepsGiveIdenticalResultAndWinningStopsMotion() {
        Level l=LevelCatalog.LEVELS.get(8);PhysicsEngine a=new PhysicsEngine(l,l.solution),b=new PhysicsEngine(l,l.solution);
        finish(a);finish(b);assertEquals(a.x,b.x,0);assertEquals(a.time,b.time,0);
        double time=a.time;a.step();assertEquals(time,a.time,0);assertEquals(0,a.vx,0);assertEquals(0,a.vy,0);
    }
}
