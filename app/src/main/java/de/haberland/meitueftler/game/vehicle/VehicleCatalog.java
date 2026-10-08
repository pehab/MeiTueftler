package de.haberland.meitueftler.game.vehicle;

import java.util.ArrayList;
import java.util.List;
import static de.haberland.meitueftler.game.vehicle.VehiclePart.Kind.*;

public final class VehicleCatalog {
    private VehicleCatalog() { }
    public static List<VehicleLevel.Surface> ground(double... points) {
        List<VehicleLevel.Surface> result=new ArrayList<>();
        for(int i=0;i<points.length-2;i+=2)result.add(new VehicleLevel.Surface(points[i],points[i+1],points[i+2],points[i+3]));
        return result;
    }
    public static List<VehiclePart> car(double radius,double length,double height) {
        return java.util.Arrays.asList(new VehiclePart(MOTOR,170,height,24,0),new VehiclePart(FRAME,170,height+12,length,0),
            new VehiclePart(WHEEL,170-length/2+12,height+radius+10,radius,0),new VehiclePart(WHEEL,170+length/2-12,height+radius+10,radius,0));
    }
    private static List<VehiclePart> climber() {
        List<VehiclePart> parts=car(40,200,425);parts.get(0).x+=30;parts.get(0).power=3;return parts;
    }
    private static VehicleLevel level(int id,String name,String task,String hint,double gy,List<VehicleLevel.Surface> surfaces,List<VehiclePart> solution) {
        return new VehicleLevel(id,name,task,hint,900,gy,surfaces,solution);
    }
    private static List<VehicleLevel.Surface> tunnel() {
        List<VehicleLevel.Surface> surfaces=ground(10,540,990,540);
        surfaces.add(new VehicleLevel.Surface(460,440,740,440,true));return surfaces;
    }
    public static final List<VehicleLevel> LEVELS=java.util.Arrays.asList(
        level(0,"Die ersten Räder","Baue Räder und einen Rahmen um den Motor. Dein Motor muss die Zielflagge erreichen.","Ein waagerechter Rahmen und zwei Räder geben einen stabilen Anfang. Räder müssen den Rahmen berühren.",475,ground(10,540,990,540),car(32,180,445)),
        level(1,"Über den Hügel","Welche Maschine schafft es über den Hügel?","Ein tiefer Schwerpunkt und genügend Abstand zwischen den Rädern helfen am Hang.",475,ground(10,540,370,540,520,460,670,540,990,540),car(40,180,430)),
        level(2,"Die Buckelpiste","Baue ein Fahrzeug, das über mehrere Bodenwellen rollt.","Größere Räder rollen leichter über Ecken. Ein sehr langer Rahmen kann aufsetzen.",475,ground(10,540,350,540,400,510,450,540,510,490,570,540,640,505,700,540,990,540),car(48,160,420)),
        level(3,"Durch den Tunnel","Passe deine Maschine unter dem niedrigen Balken hindurch.","Kleine Räder und ein flacher Aufbau brauchen weniger Platz nach oben.",485,tunnel(),car(24,160,460)),
        level(4,"Die Bergmaschine","Oben wartet das Ziel. Baue eine Maschine für den langen Anstieg.","Große Räder, ein tiefer Schwerpunkt und kräftiger Antrieb helfen. Zu viel Gewicht bremst.",300,ground(10,540,350,540,600,400,990,400),car(48,200,420)),
        level(5,"Die Kletterrampe","Eine steile Rampe führt hinauf. Probiere ungewöhnliche Rad-Anordnungen aus.","Räder können an jeder Stelle des Rahmens sitzen. Eine breite Basis verhindert das Umkippen. Setze den Motor etwas weiter nach vorn und auf kräftig.",260,ground(10,540,350,540,500,360,990,360),climber())
    );
    public static final VehicleLevel SANDBOX=level(-1,"Freie Maschinenwerkstatt","Erfinde deine eigene rollende oder kletternde Maschine. Der Motor bleibt im Startbereich.","Alles, was sich berührt, verbindet sich. Lose Bauteile fallen einzeln. Nur verbundene Räder erhalten Antrieb.",475,ground(10,540,400,540,530,470,650,540,990,540),car(40,180,430));
    public static VehicleLevel get(int id) { return id<0||id>=LEVELS.size()?SANDBOX:LEVELS.get(id); }
}
