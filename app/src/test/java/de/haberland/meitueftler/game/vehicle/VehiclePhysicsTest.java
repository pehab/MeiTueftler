package de.haberland.meitueftler.game.vehicle;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static de.haberland.meitueftler.game.vehicle.VehiclePart.Kind.*;
import static org.junit.Assert.*;

public final class VehiclePhysicsTest {
    private static List<VehiclePart> copy(List<VehiclePart> parts) { List<VehiclePart> result=new ArrayList<>();for(VehiclePart p:parts)result.add(p.copy());return result; }
    private static void finish(VehiclePhysics engine) { for(int i=0;i<240*121&&engine.state==VehiclePhysics.State.RUNNING;i++)engine.step(); }
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
    @Test public void stationarySandboxRunsBeyondOldLimitAndStopsAtTwoMinutes() {
        VehiclePhysics e=new VehiclePhysics(VehicleCatalog.SANDBOX,VehicleCatalog.SANDBOX.emptyBuild());
        for(int i=0;i<240*40;i++)e.step();
        assertEquals(VehiclePhysics.State.RUNNING,e.state);
        finish(e);
        assertEquals(VehiclePhysics.State.RETRY,e.state);
        assertEquals(120,e.time,VehiclePhysics.STEP*1.01);
    }

    @Test public void hingeSeparatesBodiesButKeepsMotorPowerAcrossTheConnection() {
        VehiclePhysics e=new VehiclePhysics(VehicleCatalog.LEVELS.get(6),VehicleCatalog.trailer());
        assertEquals(2,e.bodies.size());assertEquals(1,e.joints.size());assertEquals(0,e.looseParts());
        assertNotSame(e.bodyForPart(1),e.bodyForPart(4));assertTrue(e.bodyForPart(4).hasDrive);
        double maxAngle=0,maxError=0;
        for(int i=0;i<240*20&&e.state==VehiclePhysics.State.RUNNING;i++) {
            e.step();maxAngle=Math.max(maxAngle,Math.abs(e.bodyForPart(1).angle-e.bodyForPart(4).angle));
            maxError=Math.max(maxError,e.joints.get(0).anchorError());
        }
        assertEquals(VehiclePhysics.State.WON,e.state);assertTrue(maxAngle>0.08);assertTrue("Pin drift must stay below one workshop unit",maxError<1);
    }
    @Test public void removingHingeRestoresTheOriginalRigidWeld() {
        List<VehiclePart> parts=copy(VehicleCatalog.trailer());parts.remove(parts.size()-1);
        VehiclePhysics e=new VehiclePhysics(VehicleCatalog.LEVELS.get(0),parts);
        assertEquals(1,e.bodies.size());assertEquals(0,e.joints.size());
    }
    private static VehicleLevel emptyTerrain() {
        return new VehicleLevel(-2,"Test","","",900,475,java.util.Collections.emptyList(),java.util.Collections.emptyList());
    }
    private static List<VehiclePart> rotatingArm(int direction) {
        List<VehiclePart> result=new ArrayList<>();result.add(new VehiclePart(MOTOR,170,180,24,0));
        result.add(new VehiclePart(FRAME,170,200,160,0));result.add(new VehiclePart(FRAME,280,200,60,0));
        VehiclePart drive=new VehiclePart(DRIVE,250,200,14,0);drive.direction=direction;result.add(drive);return result;
    }
    @Test public void poweredJointRotatesBothWaysAndDoesNotGenerateHorizontalThrustInTheAir() {
        for(int direction:new int[]{-1,1}) {
            VehiclePhysics e=new VehiclePhysics(emptyTerrain(),rotatingArm(direction));assertEquals(1,e.joints.size());
            double cx=0,mass=0;for(VehiclePhysics.Body b:e.bodies){cx+=b.x*b.mass;mass+=b.mass;}cx/=mass;
            for(int i=0;i<120;i++)e.step();
            VehiclePhysics.Joint j=e.joints.get(0);assertTrue(direction*(j.b.angle-j.a.angle)>0.2);
            double after=0,momentum=0;for(VehiclePhysics.Body b:e.bodies){after+=b.x*b.mass;momentum+=b.vx*b.mass;}
            assertEquals(cx,after/mass,0.03);assertEquals(0,momentum,1e-6);assertTrue(j.anchorError()<0.1);
        }
    }
    @Test public void disconnectedDriveIsPassiveAndCannotInventMotorPower() {
        List<VehiclePart> build=rotatingArm(1);build.add(0,new VehiclePart(MOTOR,80,400,24,0));build.remove(1);
        // The two frames still form a hinge but neither touches the real motor.
        VehiclePhysics e=new VehiclePhysics(emptyTerrain(),build);assertEquals(1,e.joints.size());
        for(int i=0;i<120;i++)e.step();VehiclePhysics.Joint j=e.joints.get(0);
        assertFalse(e.powered[j.marker]);assertEquals(j.a.angle,j.b.angle,0.001);
    }
    @Test public void ambiguousAndDanglingJointsGiveActionableFeedback() {
        List<VehiclePart> build=copy(VehicleCatalog.LEVELS.get(0).emptyBuild());build.add(new VehiclePart(HINGE,700,200,14,0));
        VehiclePhysics e=new VehiclePhysics(VehicleCatalog.LEVELS.get(0),build);
        assertEquals(VehiclePhysics.State.RETRY,e.state);assertTrue(e.message.contains("genau zwei"));
        build=java.util.Arrays.asList(new VehiclePart(MOTOR,170,200,24,0),new VehiclePart(FRAME,170,200,160,0),new VehiclePart(FRAME,170,200,160,90),new VehiclePart(DRIVE,170,200,14,0));
        e=new VehiclePhysics(emptyTerrain(),build);assertEquals(VehiclePhysics.State.RETRY,e.state);assertEquals(1,e.assembly.invalidJoints);
    }
    @Test public void movingJointPreventsEarlyStallEvenWhileMotorBarelyMoves() {
        VehiclePhysics e=new VehiclePhysics(VehicleCatalog.LEVELS.get(9),VehicleCatalog.walker());
        for(int i=0;i<240*5;i++)e.step();assertEquals(VehiclePhysics.State.RUNNING,e.state);
        assertTrue(e.motorX()>300);assertEquals(0,e.parts.stream().filter(p->p.kind==WHEEL).count());
        finish(e);assertEquals(VehiclePhysics.State.WON,e.state);
    }
    @Test public void articulatedSimulationIsRepeatableAndLeavesConstructionUntouched() {
        List<VehiclePart> build=VehicleCatalog.walker();VehiclePhysics a=new VehiclePhysics(VehicleCatalog.LEVELS.get(10),build),b=new VehiclePhysics(VehicleCatalog.LEVELS.get(10),build);
        finish(a);finish(b);assertEquals(a.motorX(),b.motorX(),0);assertEquals(a.motorY(),b.motorY(),0);
        assertEquals(420,build.get(2).y,0);assertEquals(90,build.get(2).angle,0);
        for(VehiclePhysics.Joint joint:a.joints)assertTrue(joint.anchorError()<0.3);
    }
    @Test public void jointSettingsAreClampedAndCopiedWithoutChangingExistingSaveSemantics() {
        VehiclePart p=new VehiclePart(DRIVE,200,200,500,720);p.power=99;p.direction=-8;p.clamp(VehicleCatalog.SANDBOX);
        assertEquals(14,p.size,0);assertEquals(3,p.power);assertEquals(-1,p.direction);
        VehiclePart copy=p.copy();assertEquals(p.kind,copy.kind);assertEquals(p.power,copy.power);assertEquals(p.direction,copy.direction);
        assertEquals("vehicle_build_0_v1",VehicleCatalog.LEVELS.get(0).buildKey());
    }

    @Test public void fullSixtyPartConstructionWithElevenJointsRemainsFiniteAndAttached() {
        List<VehiclePart> build=new ArrayList<>();build.add(new VehiclePart(MOTOR,170,300,24,0));
        for(int i=0;i<12;i++) {
            double x=200+i*60;build.add(new VehiclePart(FRAME,x,300,60,0));
            for(int weight=0;weight<3;weight++)build.add(new VehiclePart(WEIGHT,x,300,18,0));
            if(i<11)build.add(new VehiclePart(HINGE,x+30,300,14,0));
        }
        VehiclePhysics e=new VehiclePhysics(VehicleCatalog.SANDBOX,build);
        assertEquals(60,e.parts.size());assertEquals(11,e.joints.size());assertEquals(0,e.looseParts());
        for(int i=0;i<240*15;i++) {
            e.step();for(VehiclePhysics.Joint j:e.joints)assertTrue(j.anchorError()<1);
            for(VehiclePhysics.Body b:e.bodies){assertTrue(Double.isFinite(b.x));assertTrue(Double.isFinite(b.y));assertTrue(Double.isFinite(b.angle));}
        }
        assertEquals(VehiclePhysics.State.RUNNING,e.state);
    }

    @Test public void jointsRemainSelectableAboveFramesAddedLaterAndExactLegHitsWin() {
        List<VehiclePart> parts=java.util.Arrays.asList(new VehiclePart(DRIVE,250,420,14,0),new VehiclePart(FRAME,250,420,100,90),new VehiclePart(FOOT,250,470,18,0));
        assertEquals(0,VehiclePart.hitTest(parts,250,420,49));assertEquals(1,VehiclePart.hitTest(parts,250,445,49));
        assertEquals(2,VehiclePart.hitTest(parts,250,470,49));assertEquals(-1,VehiclePart.hitTest(parts,900,100,49));
    }

}
