package de.haberland.meitueftler.game.vehicle;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static de.haberland.meitueftler.game.vehicle.VehiclePart.Kind.*;
import static org.junit.Assert.*;

public final class VehiclePhysicsTest {
    private static List<VehiclePart> copy(List<VehiclePart> parts) { List<VehiclePart> result=new ArrayList<>();for(VehiclePart p:parts)result.add(p.copy());return result; }
    private static void finish(VehiclePhysics engine) { for(int i=0;i<240*36&&engine.state==VehiclePhysics.State.RUNNING;i++)engine.step(); }
    @Test public void allAuthoredTasksAndFreeBuildAreSolvable() {
        List<VehicleLevel> levels=new ArrayList<>(VehicleCatalog.LEVELS);levels.add(VehicleCatalog.SANDBOX);
        for(VehicleLevel l:levels) {
            VehiclePhysics e=new VehiclePhysics(l,l.solution);assertEquals(l.name,0,e.looseParts());finish(e);assertEquals(l.name,VehiclePhysics.State.WON,e.state);
        }
    }
    @Test public void referenceBuildsTolerateSmallFingerPlacementErrors() {
        for(VehicleLevel l:VehicleCatalog.LEVELS)for(int variant=0;variant<4;variant++) {
            List<VehiclePart> build=copy(l.solution);
            for(int i=0;i<build.size();i++){VehiclePart p=build.get(i);p.x+=(i%2==0?1:-1)*(variant%2==0?2:-2);p.y+=(variant<2?2:-2);p.clamp(l);}
            VehiclePhysics e=new VehiclePhysics(l,build);finish(e);assertEquals(l.name+" variation "+variant,VehiclePhysics.State.WON,e.state);
        }
    }
    @Test public void connectionsFollowTouchingAndCrossingCapsulesWithoutGrid() {
        VehiclePart motor=new VehiclePart(MOTOR,150.3,200.7,24,0);
        VehiclePart bar=new VehiclePart(FRAME,250.3,200.7,180,0);
        VehiclePart cross=new VehiclePart(FRAME,290.3,200.7,180,90);
        VehiclePart wheel=new VehiclePart(WHEEL,290.3,319.7,32,0);
        int[] groups=VehiclePhysics.connections(Arrays.asList(motor,bar,cross,wheel));
        assertEquals(groups[0],groups[1]);assertEquals(groups[0],groups[2]);assertEquals(groups[0],groups[3]);
        wheel.y+=20;groups=VehiclePhysics.connections(Arrays.asList(motor,bar,cross,wheel));assertNotEquals(groups[0],groups[3]);
    }
    @Test public void motorIsConfinedToTheStartFieldIncludingItsVisibleBounds() {
        VehicleLevel l=VehicleCatalog.LEVELS.get(0);VehiclePart motor=new VehiclePart(MOTOR,900,700,24,0);motor.clamp(l);
        assertEquals(l.buildRight-24,motor.x,0);assertEquals(l.buildBottom-24,motor.y,0);
        VehiclePart cheating=new VehiclePart(MOTOR,l.goalX,l.goalY,24,0);VehiclePhysics e=new VehiclePhysics(l,Arrays.asList(cheating));
        assertEquals(VehiclePhysics.State.RETRY,e.state);
    }
    @Test public void disconnectedWheelsFallAndCannotReceiveMotorPower() {
        VehicleLevel l=VehicleCatalog.LEVELS.get(0);List<VehiclePart> build=copy(l.emptyBuild());build.add(new VehiclePart(WHEEL,600,200,32,0));
        VehiclePhysics e=new VehiclePhysics(l,build);assertEquals(2,e.bodies.size());assertFalse(e.powered[1]);
        for(int i=0;i<240;i++)e.step();assertTrue(e.parts.get(1).y>350);assertEquals(600,e.parts.get(1).x,0.1);
        finish(e);assertEquals(VehiclePhysics.State.RETRY,e.state);
    }
    @Test public void weightMovesTheCenterOfMassAndAddsInertia() {
        VehicleLevel l=VehicleCatalog.LEVELS.get(0);List<VehiclePart> light=copy(l.solution);VehiclePhysics a=new VehiclePhysics(l,light);
        light.add(new VehiclePart(WEIGHT,235,445,18,0));VehiclePhysics b=new VehiclePhysics(l,light);
        assertTrue(b.motorBody().mass>a.motorBody().mass);assertTrue(b.motorBody().x>a.motorBody().x+20);assertTrue(b.motorBody().inertia>a.motorBody().inertia);
    }
    @Test public void rotatingBodyFallsAndTipsUnderAnOffCenterWeight() {
        VehicleLevel l=VehicleCatalog.LEVELS.get(0);
        List<VehiclePart> build=Arrays.asList(new VehiclePart(MOTOR,170,440,24,0),new VehiclePart(FRAME,170,455,180,0),
            new VehiclePart(WHEEL,100,484,32,0),new VehiclePart(WEIGHT,240,440,18,0));
        VehiclePhysics e=new VehiclePhysics(l,build);for(int i=0;i<240;i++)e.step();
        assertTrue("Unsupported weight should tip the frame",Math.abs(e.motorBody().angle)>0.15);
    }
    @Test public void longPartTouchingGoalDoesNotWinWithoutMotor() {
        VehicleLevel l=VehicleCatalog.LEVELS.get(0);List<VehiclePart> build=copy(l.emptyBuild());build.add(new VehiclePart(FRAME,900,475,180,0));
        VehiclePhysics e=new VehiclePhysics(l,build);finish(e);assertEquals(VehiclePhysics.State.RETRY,e.state);assertTrue(e.motorX()<400);
    }
    @Test public void defaultMotorDoesNotPullAnOverweightMachineUpTheClimbingRamp() {
        VehicleLevel l=VehicleCatalog.LEVELS.get(5);List<VehiclePart> build=copy(l.solution);build.add(new VehiclePart(WEIGHT,170,405,18,0));
        VehiclePhysics e=new VehiclePhysics(l,build);finish(e);assertEquals(VehiclePhysics.State.RETRY,e.state);
    }
    @Test public void reversingMotorChangesTravelDirection() {
        VehicleLevel l=VehicleCatalog.LEVELS.get(0);List<VehiclePart> build=copy(l.solution);build.get(0).direction=-1;VehiclePhysics e=new VehiclePhysics(l,build);
        for(int i=0;i<240;i++)e.step();assertTrue(e.motorX()<150);
    }
    @Test public void simulationIsRepeatableAndNeverModifiesSavedConstruction() {
        VehicleLevel l=VehicleCatalog.LEVELS.get(2);List<VehiclePart> build=copy(l.solution);double before=build.get(0).x;
        VehiclePhysics a=new VehiclePhysics(l,build),b=new VehiclePhysics(l,build);finish(a);finish(b);
        assertEquals(a.motorX(),b.motorX(),0);assertEquals(a.motorBody().angle,b.motorBody().angle,0);assertEquals(before,build.get(0).x,0);
    }
}
