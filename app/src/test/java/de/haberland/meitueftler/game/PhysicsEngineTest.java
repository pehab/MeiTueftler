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
    @Test public void allChallengesHaveReachableReferenceSolutions() {
        assertEquals(24,LevelCatalog.LEVELS.size());
        for(Level l:LevelCatalog.LEVELS) {
            assertTrue(l.solution.size()<=l.bonusRamps);
            for(Ramp r:l.solution) { assertEquals(0,r.angle%5,0.001);assertEquals(0,(r.length-160)%40,0.001); }
            PhysicsEngine engine=new PhysicsEngine(l,l.solution);finish(engine);
            assertEquals(l.name,PhysicsEngine.State.WON,engine.state);
        }
    }
    @Test public void allDoorPuzzlesNeedTheirSwitch() {
        for(int id:new int[]{17,18,19,20,21,22,23}) {
            Level l=LevelCatalog.get(id);PhysicsEngine e=new PhysicsEngine(l,l.solution);finish(e);
            assertEquals(l.name,PhysicsEngine.State.WON,e.state);assertTrue(e.switchesTriggered>0);
            java.util.ArrayList<Ramp> fixed=new java.util.ArrayList<>(),build=new java.util.ArrayList<>();
            for(Ramp r:l.fixed)if(r.kind!=Ramp.Kind.SWITCH)fixed.add(r);
            for(Ramp r:l.solution)if(r.kind!=Ramp.Kind.SWITCH)build.add(r);
            Level closed=new Level(99,"closed","","","",l.spawnX,l.spawnY,l.goalX,l.goalY,6,2,fixed,build);
            PhysicsEngine blocked=new PhysicsEngine(closed,build);finish(blocked);assertEquals(l.name,PhysicsEngine.State.RETRY,blocked.state);
        }
    }
    @Test public void changedLayoutsStartFreshBuildsWithoutChangingStarsOrSandboxKeys() {
        int changed=0;
        for(Level l:LevelCatalog.LEVELS) {
            assertEquals(LevelCatalog.LEVELS.indexOf(l),l.id);
            if(l.layoutRevision==2) { changed++;assertEquals("build_"+l.id+"_v2",l.buildKey());assertFalse(l.fixed.isEmpty()); }
            else assertEquals("build_"+l.id,l.buildKey());
            for(Ramp r:l.solution) {
                Ramp clamped=r.copy();clamped.clamp();
                assertEquals(l.name,r.x,clamped.x,0.001);assertEquals(l.name,r.y,clamped.y,0.001);
                assertEquals(l.name,r.length,clamped.length,0.001);
            }
        }
        assertEquals(18,changed);assertEquals("build_-1",LevelCatalog.SANDBOX.buildKey());
    }
    @Test public void everyHintCanBeBuiltUsingActualDragPlacement() {
        for(Level l:LevelCatalog.LEVELS) {
            java.util.ArrayList<Ramp> build=new java.util.ArrayList<>();
            for(Ramp r:l.solution) { Ramp placed=r.copy();placed.moveTo(r.x,r.y);build.add(placed); }
            PhysicsEngine e=new PhysicsEngine(l,build);finish(e);
            assertEquals(l.name,PhysicsEngine.State.WON,e.state);
        }
        Ramp piece=new Ramp(500,300,320,-60,false,Ramp.Kind.FAN);
        piece.moveTo(120.3,300.2);assertEquals(120,piece.x,0);assertEquals(300,piece.y,0);
        piece.moveTo(124.2,303.1);assertEquals(124,piece.x,0);assertEquals(303,piece.y,0);
    }
    @Test public void revisedHintsAllowSmallPlacementErrors() {
        for(Level l:LevelCatalog.LEVELS)if(l.layoutRevision==2) {
            int attempts=0,wins=0;
            for(int i=0;i<l.solution.size();i++)for(int axis=0;axis<2;axis++)for(int delta:new int[]{-4,4}) {
                java.util.ArrayList<Ramp> build=new java.util.ArrayList<>();for(Ramp r:l.solution)build.add(r.copy());
                Ramp r=build.get(i);if(axis==0)r.x+=delta;else r.y+=delta;r.clamp();
                PhysicsEngine e=new PhysicsEngine(l,build);finish(e);attempts++;if(e.state==PhysicsEngine.State.WON)wins++;
            }
            assertTrue(l.name+" tolerates only "+wins+"/"+attempts+" placements",wins>=attempts*0.75);
        }
    }
    @Test public void multiStageRoutesRejectSampledCommonSinglePieceShortcuts() {
        for(int id:new int[]{3,5,6,8,9,12,13,14,15,17,18,19,21,22,23}) {
            Level l=LevelCatalog.get(id);
            for(Ramp.Kind kind:new Ramp.Kind[]{Ramp.Kind.PLANK,Ramp.Kind.TRAMPOLINE,Ramp.Kind.FAN})
                for(int x=100;x<=900;x+=100)for(int y=100;y<=550;y+=75)
                for(int angle=-75;angle<=75;angle+=15)for(int length:new int[]{160,320,560}) {
                    Ramp r=new Ramp(x,y,length,angle,false,kind);r.power=3;r.clamp();
                    PhysicsEngine e=new PhysicsEngine(l,Collections.singletonList(r));finish(e);
                    assertNotEquals(l.name+" with "+kind+" at "+x+","+y+" / "+angle+" degrees",PhysicsEngine.State.WON,e.state);
                }
        }
    }
    @Test public void underpassRouteActuallyTravelsBelowTheBarrierThenClimbsToHighGoal() {
        Level l=LevelCatalog.get(12);PhysicsEngine e=new PhysicsEngine(l,l.solution);boolean crossed=false;
        while(e.state==PhysicsEngine.State.RUNNING) {
            double before=e.x;e.step();
            if(before<=500&&e.x>500) { assertTrue("must pass below the blocker",e.y>400);crossed=true; }
        }
        assertTrue(crossed);assertEquals(PhysicsEngine.State.WON,e.state);
        assertTrue(l.spawnY<100);assertTrue(l.goalY<200);
    }
    @Test public void matchingDoorsLatchAndResetWithoutChangingBuild() {
        Ramp sensor=new Ramp(500,300,160,0,false,Ramp.Kind.SWITCH);sensor.channel=2;
        Ramp door=new Ramp(500,370,160,0,false,Ramp.Kind.DOOR);door.channel=2;
        Ramp other=door.copy();other.channel=1;other.x=800;
        PhysicsEngine e=new PhysicsEngine(testLevel(500,260),Arrays.asList(door,other,sensor));e.vy=1200;
        for(int i=0;i<100;i++)e.step();
        assertTrue(e.isOpen(door));assertFalse(e.isOpen(other));assertEquals(1,e.switchesTriggered);assertTrue(e.y>400);
        assertEquals(2,sensor.copy().channel);
        PhysicsEngine reset=new PhysicsEngine(testLevel(500,260),Arrays.asList(door,sensor));assertFalse(reset.isOpen(door));
        sensor.channel=1;assertEquals(2,e.ramps.get(2).channel);
    }
    @Test public void closedDoorsBlockFastMarbles() {
        Ramp door=new Ramp(500,300,240,0,false,Ramp.Kind.DOOR);
        PhysicsEngine e=new PhysicsEngine(testLevel(500,260),Arrays.asList(door));e.vy=1200;
        for(int i=0;i<240;i++)e.step();assertTrue(e.y<=300-PhysicsEngine.RADIUS-12+0.001);
        assertEquals(0,e.switchesTriggered);
    }
    @Test public void fanChallengesActuallyUseWindBeforeWinning() {
        for(int id=16;id<20;id++) {
            Level l=LevelCatalog.get(id);PhysicsEngine e=new PhysicsEngine(l,l.solution);finish(e);
            assertEquals(l.name,PhysicsEngine.State.WON,e.state);assertTrue(l.name,e.windSteps>0);
        }
    }
    @Test public void windOnlyActsInTheVisibleForwardCone() {
        Ramp fan=new Ramp(500,300,240,0,false,Ramp.Kind.FAN);
        assertTrue(fan.windWeight(600,300)>0);
        assertEquals(0,fan.windWeight(450,300),0);
        assertEquals(0,fan.windWeight(600,450),0);
        assertEquals(0,fan.windWeight(800,300),0);
        fan.angle=-90;assertTrue(fan.windWeight(500,200)>0);
        assertEquals(0,fan.windWeight(500,400),0);
    }
    @Test public void strongerWindGivesMorePushAndCopiesKeepStrength() {
        Ramp weak=new Ramp(500,300,320,0,false,Ramp.Kind.FAN);weak.power=1;
        Ramp strong=weak.copy();strong.power=3;
        PhysicsEngine a=new PhysicsEngine(testLevel(600,300),Arrays.asList(weak));
        PhysicsEngine b=new PhysicsEngine(testLevel(600,300),Arrays.asList(strong));
        for(int i=0;i<30;i++){a.step();b.step();}
        assertTrue(b.vx>a.vx);assertEquals(3,b.ramps.get(0).power);
        strong.power=1;assertEquals(3,b.ramps.get(0).power);
    }
    @Test public void fanBodyBlocksFastMarblesButAirCanBeCrossed() {
        Ramp fan=new Ramp(500,300,320,0,false,Ramp.Kind.FAN);
        PhysicsEngine e=new PhysicsEngine(testLevel(500,245),Arrays.asList(fan));e.vy=1200;
        for(int i=0;i<10;i++)e.step();assertTrue(e.y<=300-PhysicsEngine.RADIUS-32+0.001);
        assertTrue(fan.distance(500,300)==0);assertTrue(fan.distance(600,300)>30);
        assertEquals(-175,Ramp.normalizeAngle(185),0);
    }
    @Test public void springChallengesActuallyBounceBeforeWinning() {
        for(int id:new int[]{2,3,4,7,8,10,12,13,15,17,18,22}) {
            Level l=LevelCatalog.get(id);PhysicsEngine e=new PhysicsEngine(l,l.solution);finish(e);
            assertEquals(l.name,PhysicsEngine.State.WON,e.state);
            assertTrue(l.name,e.trampolineBounces>0);
        }
    }
    @Test public void trampolineLaunchesMarbleUpwardAndSnapshotKeepsItsKind() {
        Ramp spring=new Ramp(500,300,240,0,false,Ramp.Kind.TRAMPOLINE);
        PhysicsEngine e=new PhysicsEngine(testLevel(500,275),Arrays.asList(spring));e.vy=400;
        for(int i=0;i<10;i++)e.step();
        assertEquals(Ramp.Kind.TRAMPOLINE,e.ramps.get(0).kind);
        assertEquals(1,e.trampolineBounces);assertTrue(e.vy< -350);
    }
    @Test public void blockCollidesAtItsThickSurfaceAndStaysWithinBounds() {
        Ramp block=new Ramp(500,300,240,0,false,Ramp.Kind.BLOCK);
        PhysicsEngine e=new PhysicsEngine(testLevel(500,250),Arrays.asList(block));e.vy=1200;
        for(int i=0;i<240;i++)e.step();
        assertTrue(e.y<=300-PhysicsEngine.RADIUS-block.halfThickness()+0.001);
        block.x=-100;block.y=700;block.clamp();
        assertTrue(block.x-Math.abs(block.dx())-block.halfThickness()>=5);
        assertTrue(block.y+Math.abs(block.dy())+block.halfThickness()<=595);
    }
    @Test public void everyElementFitsInsideWorkshopAtEverySupportedAngle() {
        for(Ramp.Kind kind:Ramp.Kind.values())for(int angle=-85;angle<=85;angle+=5) {
            Ramp r=new Ramp(-100,900,560,angle,false,kind);r.clamp();
            double extent=kind==Ramp.Kind.TRAMPOLINE?37:r.halfThickness();
            assertTrue(r.x-Math.abs(r.dx())-extent>=4.999);
            assertTrue(r.x+Math.abs(r.dx())+extent<=995.001);
            assertTrue(r.y-Math.abs(r.dy())-extent>=4.999);
            assertTrue(r.y+Math.abs(r.dy())+extent<=595.001);
            assertEquals(0,(r.length-160)%40,0.001);
        }
    }
    @Test public void blockCanBeGrabbedAnywhereOnItsSurface() {
        Ramp block=new Ramp(500,300,240,0,false,Ramp.Kind.BLOCK);
        assertEquals(0,block.distance(500,322),0);
        assertEquals(6,block.distance(500,330),0);
        assertEquals(0,block.distance(620,300),0);
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
