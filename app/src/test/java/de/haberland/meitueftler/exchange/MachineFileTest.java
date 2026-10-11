package de.haberland.meitueftler.exchange;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import static de.haberland.meitueftler.exchange.MachineFile.Category.*;

public final class MachineFileTest {
    private static final String MARBLE="[{\"kind\":\"FAN\",\"x\":500,\"y\":300,\"length\":480,\"angle\":-170,\"power\":3,\"channel\":4},{\"kind\":\"DOOR\",\"x\":500,\"y\":300,\"length\":320,\"angle\":45,\"power\":2,\"channel\":4}]";
    private static final String VEHICLE="[{\"kind\":\"MOTOR\",\"x\":170,\"y\":445,\"size\":24,\"angle\":0,\"power\":3,\"direction\":-1},{\"kind\":\"DRIVE\",\"x\":170,\"y\":400,\"size\":14,\"angle\":0,\"power\":1,\"direction\":-1}]";
    private static final String MECHANIC="[{\"kind\":\"WINCH\",\"x\":300,\"y\":160,\"bx\":420,\"by\":260,\"size\":22,\"angle\":0,\"power\":3,\"direction\":-1,\"guide\":-1},{\"kind\":\"ROPE\",\"x\":300,\"y\":160,\"bx\":620,\"by\":400,\"size\":20,\"angle\":0,\"power\":2,\"direction\":1,\"guide\":2}]";
    private static JSONObject envelope()throws Exception{return new JSONObject(MachineFile.encode(MARBLES,"Murmelwelt",MARBLE));}
    private static void rejected(String json,String message)throws Exception{try{MachineFile.decode(json);fail("Accepted: "+json);}catch(MachineFile.FormatException expected){assertTrue(expected.getMessage(),expected.getMessage().contains(message));}}
    @Test public void allCategoriesRoundTripTheirSettingsAndUnicodeNames()throws Exception {
        MachineFile.Category[] categories={MARBLES,VEHICLES,MECHANICS};String[] builds={MARBLE,VEHICLE,MECHANIC};
        for(int i=0;i<3;i++){
            String encoded=MachineFile.encode(categories[i],"Peters Tüftler 🚗",builds[i]);MachineFile imported=MachineFile.decode(encoded);
            assertEquals(categories[i],imported.category);assertEquals("Peters Tüftler 🚗",imported.name);assertEquals(2,imported.partCount);
            assertTrue(new JSONArray(builds[i]).similar(new JSONArray(imported.build)));
            assertEquals(imported.build,MachineFile.read(new ByteArrayInputStream(encoded.getBytes(StandardCharsets.UTF_8))).build);
        }
    }
    @Test public void fullSixtyPartMachinesAndEmptyWorkInProgressAreAllowed()throws Exception {
        JSONArray parts=new JSONArray();JSONObject p=new JSONArray(MARBLE).getJSONObject(0);for(int i=0;i<60;i++)parts.put(p);
        assertEquals(60,MachineFile.decode(MachineFile.encode(MARBLES,"Große Bahn",parts.toString())).partCount);
        assertEquals(0,MachineFile.decode(MachineFile.encode(MECHANICS,"Leere Idee","[]")).partCount);
        JSONObject file=envelope();parts.put(p);file.put("parts",parts);rejected(file.toString(),"60 Bauteile");
    }
    @Test public void olderMarblePartsKeepTheirPlankAndControlDefaults()throws Exception {
        MachineFile file=MachineFile.decode(MachineFile.encode(MARBLES,"Alt","[{\"x\":500,\"y\":300,\"length\":480,\"angle\":20}]"));
        JSONObject p=new JSONArray(file.build).getJSONObject(0);assertEquals("PLANK",p.getString("kind"));assertEquals(2,p.getInt("power"));assertEquals(1,p.getInt("channel"));
    }
    @Test public void exportsContainNoLocalIdsProgressOrUnexpectedPartFields()throws Exception {
        JSONArray parts=new JSONArray(MARBLE);parts.getJSONObject(0).put("id","private").put("stars",3);
        String json=MachineFile.encode(MARBLES,"Bahn",parts.toString());assertFalse(json.contains("private"));assertFalse(json.contains("stars"));assertFalse(json.contains("\"id\""));
        assertEquals(6,new JSONObject(json).length());
    }
    @Test public void otherFilesUnknownCategoriesAndNewerFormatsExplainWhyTheyFail()throws Exception {
        rejected("{}","keine MeiTüftler");JSONObject file=envelope();file.put("category","unknown");rejected(file.toString(),"Werkstatt");
        file=envelope();file.put("version",2);rejected(file.toString(),"aktualisieren");file=envelope();file.put("workshopVersion",2);rejected(file.toString(),"aktualisieren");
        rejected(envelope()+" garbage","keine gültige");rejected("[","beschädigt");
    }
    @Test public void missingWronglyTypedAndUnknownPartsAreRejectedCompletely()throws Exception {
        JSONObject file=envelope();file.getJSONArray("parts").put("not a part");rejected(file.toString(),"beschädigt");
        file=envelope();file.getJSONArray("parts").getJSONObject(0).remove("x");rejected(file.toString(),"beschädigt");
        file=envelope();file.getJSONArray("parts").getJSONObject(0).put("kind","EXPLOSION");rejected(file.toString(),"beschädigt");
        file=envelope();file.getJSONArray("parts").getJSONObject(0).put("x","500");rejected(file.toString(),"Einstellung");
    }
    @Test public void impossibleNumbersAndFractionalSettingsAreRejected()throws Exception {
        for(Object bad:new Object[]{-1,1001,"NaN","Infinity",true,JSONObject.NULL}){
            JSONObject file=envelope();file.getJSONArray("parts").getJSONObject(0).put("x",bad);rejected(file.toString(),"Einstellung");
        }
        JSONObject file=envelope();file.getJSONArray("parts").getJSONObject(0).put("power",1.5);rejected(file.toString(),"Einstellung");
        file=envelope();file.getJSONArray("parts").getJSONObject(0).put("channel",5);rejected(file.toString(),"Einstellung");
    }
    @Test public void vehiclesRequireExactlyOneMotorAndNonzeroDirection()throws Exception {
        JSONObject file=new JSONObject(MachineFile.encode(VEHICLES,"Auto",VEHICLE));file.getJSONArray("parts").remove(0);rejected(file.toString(),"genau einen Motor");
        file=new JSONObject(MachineFile.encode(VEHICLES,"Auto",VEHICLE));file.getJSONArray("parts").put(file.getJSONArray("parts").getJSONObject(0));rejected(file.toString(),"genau einen Motor");
        file=new JSONObject(MachineFile.encode(VEHICLES,"Auto",VEHICLE));file.getJSONArray("parts").getJSONObject(0).put("direction",0);rejected(file.toString(),"Drehrichtung");
    }
    @Test public void ropeEndsAndGuideIndicesAreValidated()throws Exception {
        for(String key:new String[]{"bx","by","guide"}){
            JSONObject file=new JSONObject(MachineFile.encode(MECHANICS,"Seilzug",MECHANIC));file.getJSONArray("parts").getJSONObject(1).put(key,1001);rejected(file.toString(),"Einstellung");
        }
    }
    @Test public void oversizeFilesBadUtf8AndDeepNestingAreRejectedBeforeParsing()throws Exception {
        try{MachineFile.read(new ByteArrayInputStream(new byte[MachineFile.MAX_BYTES+1]));fail();}catch(MachineFile.FormatException e){assertTrue(e.getMessage().contains("zu groß"));}
        try{MachineFile.read(new ByteArrayInputStream(new byte[]{(byte)0xc3,0x28}));fail();}catch(MachineFile.FormatException e){assertTrue(e.getMessage().contains("UTF-8"));}
        rejected("[".repeat(2000)+"]".repeat(2000),"beschädigt");
        JSONObject file=envelope();file.put("name","Ä".repeat(MachineFile.MAX_BYTES/2));rejected(file.toString(),"zu groß");
    }
    @Test public void namesAndFilenamesStayBoundedAndCannotEscapeExportDirectory()throws Exception {
        for(String name:new String[]{""," ","x".repeat(41),"a\nb"}){JSONObject file=envelope();file.put("name",name);rejected(file.toString(),"name");}
        String filename=MachineFile.filename("../../Meine Maschine 🚗");assertFalse(filename.contains("/"));assertFalse(filename.contains(".."));assertTrue(filename.endsWith(MachineFile.SUFFIX));
        JSONArray gallery=new JSONArray();String original="x".repeat(40);gallery.put(new JSONObject().put("name",original));String next=MachineFile.uniqueName(original,gallery);assertTrue(next.length()<=40);assertTrue(next.endsWith("(Import 1)"));
        gallery.put(new JSONObject().put("name",next));assertTrue(MachineFile.uniqueName(original,gallery).endsWith("(Import 2)"));
    }
}
