package de.haberland.meitueftler.game.mechanics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import static de.haberland.meitueftler.game.mechanics.MechanicPart.Kind.*;

public final class MechanicPhysicsTest {
    private static void finish(MechanicPhysics e){for(int i=0;i<240*121&&e.state==MechanicPhysics.State.RUNNING;i++)e.step();}
    @Test public void everyHintSolvesItsTaskAndSandboxWithoutChangingConstruction() {
        List<MechanicLevel> levels=new ArrayList<>(MechanicCatalog.LEVELS);levels.add(MechanicCatalog.SANDBOX);
        for(MechanicLevel l:levels) {
            List<MechanicPart> parts=l.solution();double x=parts.get(0).x,y=parts.get(0).y;
            MechanicPhysics e=new MechanicPhysics(l,parts);finish(e);assertEquals(l.name,MechanicPhysics.State.WON,e.state);
            assertEquals(x,parts.get(0).x,0);assertEquals(y,parts.get(0).y,0);
        }
    }
    @Test public void expandedTasksRequireAConstructionAndUseSeveralMechanisms() {
        assertEquals(24,MechanicCatalog.LEVELS.size());
        for(MechanicLevel l:MechanicCatalog.LEVELS){MechanicPhysics e=new MechanicPhysics(l,Collections.emptyList());finish(e);assertEquals(l.name,MechanicPhysics.State.RETRY,e.state);}
        assertTrue(MechanicCatalog.get(10).solution().stream().noneMatch(p->p.kind==WINCH));
        assertTrue(MechanicCatalog.get(13).solution().stream().anyMatch(p->p.kind==PULLEY));
        assertEquals(2,MechanicCatalog.get(20).solution().stream().filter(p->p.kind==WINCH).count());
    }
    @Test public void hintsTolerateSmallPlacementErrors() {
        for(MechanicLevel l:MechanicCatalog.LEVELS)for(int v=0;v<4;v++) {
            List<MechanicPart> parts=l.solution();
            for(int i=0;i<parts.size();i++){MechanicPart p=parts.get(i);p.x+=(v%2==0?2:-2)*(i%2==0?1:-1);p.y+=v<2?2:-2;p.bx+=v%2==0?2:-2;p.by+=v<2?2:-2;}
            MechanicPhysics e=new MechanicPhysics(l,parts);finish(e);assertEquals(l.name+" variation "+v,MechanicPhysics.State.WON,e.state);
        }
    }
    @Test public void weightAndDistanceDetermineLeverDirection() {
        MechanicLevel l=MechanicCatalog.get(2);List<MechanicPart> weak=l.solution();weak.get(1).size=2;
        MechanicPhysics e=new MechanicPhysics(l,weak);for(int i=0;i<240;i++)e.step();assertTrue(e.cargoY()>l.cargoY);
        MechanicPhysics strong=new MechanicPhysics(l,l.solution());for(int i=0;i<240;i++)strong.step();assertTrue(strong.cargoY()<l.cargoY);
    }
    @Test public void weakWinchCannotLiftHeavyCargoAndPayingOutDoesNotPushItUp() {
        MechanicLevel l=MechanicCatalog.get(5);List<MechanicPart> weak=l.solution();weak.get(0).power=1;
        MechanicPhysics e=new MechanicPhysics(l,weak);finish(e);assertEquals(MechanicPhysics.State.RETRY,e.state);assertTrue(e.cargoY()>450);
        List<MechanicPart> reverse=MechanicCatalog.get(3).solution();reverse.get(0).direction=-1;
        e=new MechanicPhysics(MechanicCatalog.get(3),reverse);for(int i=0;i<240*4;i++)e.step();assertTrue(e.cargoY()>460);
    }
    @Test public void pulleyChangesDirectionAndLiftKeepsCargoOnItsRail() {
        MechanicPhysics e=new MechanicPhysics(MechanicCatalog.get(4),MechanicCatalog.get(4).solution());
        double initial=e.cables.get(0).restLength;finish(e);assertEquals(MechanicPhysics.State.WON,e.state);
        assertEquals(620,e.cargoX(),0);assertEquals(0,e.cargoAngle(),0);assertTrue(e.cables.get(0).restLength<initial-150);
        assertEquals(1,e.cables.get(0).guide);
    }
    @Test public void looseEndsMissingGuidesAndUnmountedWinchesExplainTheProblem() {
        MechanicLevel l=MechanicCatalog.get(3);
        MechanicPhysics e=new MechanicPhysics(l,Arrays.asList(MechanicPart.rope(620,100,800,300,-1)));
        assertEquals(MechanicPhysics.State.RETRY,e.state);assertTrue(e.message.contains("Seilenden"));
        List<MechanicPart> missing=l.solution();missing.get(1).guide=0;e=new MechanicPhysics(l,missing);assertTrue(e.message.contains("Umlenkrolle fehlt"));
        List<MechanicPart> unmounted=l.solution();unmounted.get(0).x=300;e=new MechanicPhysics(l,unmounted);assertEquals(MechanicPhysics.State.RETRY,e.state);assertTrue(e.message.contains("Befestigungspunkt"));
    }
    @Test public void twoFixedPinsHoldBeamStillAndEmptySandboxRunsForTwoMinutes() {
        MechanicLevel l=MechanicCatalog.SANDBOX;MechanicPart bridge=new MechanicPart(BEAM,340,400,320,0);
        MechanicPhysics e=new MechanicPhysics(l,Arrays.asList(bridge));for(int i=0;i<240*4;i++)e.step();
        assertTrue(e.pinned(0));assertEquals(340,e.parts.get(0).x,0);assertEquals(400,e.parts.get(0).y,0);assertEquals(0,e.parts.get(0).angle,0);
        e=new MechanicPhysics(l,Collections.emptyList());for(int i=0;i<240*40;i++)e.step();assertEquals(MechanicPhysics.State.RUNNING,e.state);
        finish(e);assertEquals(MechanicPhysics.State.RETRY,e.state);assertEquals(120,e.time,MechanicPhysics.STEP*1.01);
    }
    @Test public void simulationsAreRepeatableAndSolutionsAreDefensivelyCopied() {
        MechanicLevel l=MechanicCatalog.get(4);MechanicPhysics a=new MechanicPhysics(l,l.solution()),b=new MechanicPhysics(l,l.solution());finish(a);finish(b);
        assertEquals(a.cargoX(),b.cargoX(),0);assertEquals(a.cargoY(),b.cargoY(),0);assertEquals(a.time,b.time,0);
        l.solution().get(0).x=999;assertEquals(300,l.solution().get(0).x,0);
        for(int i=0;i<MechanicCatalog.LEVELS.size();i++){assertEquals(i,MechanicCatalog.get(i).id);assertEquals("mechanics_build_"+i+"_v1",MechanicCatalog.get(i).buildKey());}
    }
}
