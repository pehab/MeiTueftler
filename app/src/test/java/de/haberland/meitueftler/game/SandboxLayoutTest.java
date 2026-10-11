package de.haberland.meitueftler.game;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import de.haberland.meitueftler.exchange.MachineFile;
import de.haberland.meitueftler.game.vehicle.*;
import de.haberland.meitueftler.game.mechanics.*;

public final class SandboxLayoutTest {
    @Test public void legacyBuildsGetIndependentDefaultLayouts()throws Exception {
        Level a=SandboxLayout.marble("[]");a.spawnX=400;
        assertEquals(LevelCatalog.SANDBOX.spawnX,SandboxLayout.marble("[]").spawnX,0);
        VehicleLevel v=SandboxLayout.vehicle("[]");v.surfaces.clear();
        assertFalse(VehicleCatalog.SANDBOX.surfaces.isEmpty());
        MechanicLevel m=SandboxLayout.mechanic("[]");m.moveMount(0,600,300);
        assertEquals(MechanicCatalog.SANDBOX.mountX(0),SandboxLayout.mechanic("[]").mountX(0),0);
    }
    @Test public void marbleLayoutSurvivesSharingAndDrivesSimulation()throws Exception {
        Level l=SandboxLayout.marble("[]");l.spawnX=300;l.spawnY=120;l.goalX=700;l.goalY=500;
        String build=SandboxLayout.pack(new JSONArray(),l);
        MachineFile file=MachineFile.decode(MachineFile.encode(MachineFile.Category.MARBLES,"Test",build));
        Level loaded=SandboxLayout.marble(file.build);
        PhysicsEngine engine=new PhysicsEngine(loaded,java.util.Collections.emptyList());
        assertEquals(300,engine.x,0);assertEquals(120,engine.y,0);assertEquals(700,loaded.goalX,0);
        assertEquals(2,new JSONObject(MachineFile.encode(MachineFile.Category.MARBLES,"Test",build)).getInt("workshopVersion"));
    }
    @Test public void vehicleTerrainAndUnrestrictedMotorSurviveSharing()throws Exception {
        VehicleLevel l=SandboxLayout.vehicle("[]");l.goalX=550;l.surfaces.clear();l.surfaces.add(new VehicleLevel.Surface(100,500,800,300,true));
        VehiclePart motor=new VehiclePart(VehiclePart.Kind.MOTOR,700,400,24,0);motor.clamp(l);assertEquals(700,motor.x,0);
        motor.clamp(VehicleCatalog.LEVELS.get(0));assertTrue(motor.x<=VehicleCatalog.LEVELS.get(0).buildRight);
        JSONArray parts=new JSONArray("[{\"kind\":\"MOTOR\",\"x\":700,\"y\":400,\"size\":24,\"angle\":0}]");
        MachineFile file=MachineFile.decode(MachineFile.encode(MachineFile.Category.VEHICLES,"Test",SandboxLayout.pack(parts,l)));
        VehicleLevel loaded=SandboxLayout.vehicle(file.build);assertEquals(1,loaded.surfaces.size());assertEquals(300,loaded.surfaces.get(0).by,0);assertTrue(loaded.surfaces.get(0).twoSided);assertEquals(550,loaded.goalX,0);
        l.surfaces.clear();assertTrue(SandboxLayout.vehicle(SandboxLayout.pack(parts,l)).surfaces.isEmpty());
    }
    @Test public void mechanicMountOrderAndCargoSurviveSharing()throws Exception {
        MechanicLevel l=SandboxLayout.mechanic("[]");l.moveMount(2,700,250);l.cargoX=600;l.cargoY=400;l.goalX=800;
        MachineFile file=MachineFile.decode(MachineFile.encode(MachineFile.Category.MECHANICS,"Test",SandboxLayout.pack(new JSONArray(),l)));
        MechanicLevel loaded=SandboxLayout.mechanic(file.build);assertEquals(700,loaded.mountX(2),0);assertEquals(250,loaded.mountY(2),0);assertEquals(2,loaded.nearMount(700,250));assertEquals(600,loaded.cargoX,0);assertEquals(800,loaded.goalX,0);
    }
    @Test public void malformedLayoutsAreRejectedAtImport()throws Exception {
        VehicleLevel l=SandboxLayout.vehicle("[]");JSONArray parts=new JSONArray("[{\"kind\":\"MOTOR\",\"x\":170,\"y\":445,\"size\":24,\"angle\":0}]");
        JSONObject file=new JSONObject(MachineFile.encode(MachineFile.Category.VEHICLES,"Test",SandboxLayout.pack(parts,l)));
        file.getJSONObject("layout").getJSONArray("surfaces").getJSONArray(0).put(0,"Infinity");
        try{MachineFile.decode(file.toString());fail();}catch(MachineFile.FormatException expected){assertTrue(expected.getMessage().contains("Levelpunkte"));}
    }
}
