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
    public static List<VehiclePart> trailer() {
        return java.util.Arrays.asList(new VehiclePart(MOTOR,170,415,24,0),new VehiclePart(FRAME,170,427,160,0),
            new VehiclePart(WHEEL,110,467,32,0),new VehiclePart(WHEEL,210,467,32,0),
            new VehiclePart(FRAME,340,430,180,0),new VehiclePart(WHEEL,370,467,32,0),new VehiclePart(HINGE,250,429,14,0));
    }
    public static List<VehiclePart> walker() {
        List<VehiclePart> result=new ArrayList<>();
        VehiclePart motor=new VehiclePart(MOTOR,170,400,24,0);motor.power=3;result.add(motor);
        result.add(new VehiclePart(FRAME,170,420,160,0));
        for(double x:new double[]{90,250}) {
            result.add(new VehiclePart(FRAME,x,420,100,90));
            result.add(new VehiclePart(FOOT,x,370,18,0));result.add(new VehiclePart(FOOT,x,470,18,0));
            result.add(new VehiclePart(DRIVE,x,420,14,0));
        }
        return result;
    }
    private static List<VehiclePart> bridgeCar() {
        List<VehiclePart> result=new ArrayList<>(strongCar(32,160,400));
        result.add(new VehiclePart(WHEEL,170,442,32,0));return result;
    }
    private static List<VehiclePart> strongCar(double radius,double length,double height) {
        List<VehiclePart> result=car(radius,length,height);result.get(0).power=3;return result;
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
        level(5,"Die Kletterrampe","Eine steile Rampe führt hinauf. Probiere ungewöhnliche Rad-Anordnungen aus.","Räder können an jeder Stelle des Rahmens sitzen. Eine breite Basis verhindert das Umkippen. Setze den Motor etwas weiter nach vorn und auf kräftig.",260,ground(10,540,350,540,500,360,990,360),climber()),
        level(6,"Der bewegliche Anhänger","Ziehe deine zweite Baugruppe über die Bodenwellen. Ein Gelenk lässt den Anhänger mitkippen.","Baue einen kurzen Wagen mit zwei Rädern und dahinter einen zweiten Rahmen mit einem Rad. Setze ein Gelenk an die Berührung der beiden Rahmen. Halte andere Teile vom Gelenk fern.",475,ground(10,540,430,540,510,520,590,540,700,525,780,540,990,540),trailer()),
        level(7,"Über den Graben","Im Boden klafft eine Lücke. Baue eine Maschine, die sie überqueren kann.","Ein drittes Rad in der Mitte hält die Maschine an der Kante stabil. Probiere drei 32er Räder unter einem 160 langen Rahmen und einen kräftigen Motor.",475,ground(10,540,440,540,440,590,510,590,510,540,990,540),bridgeCar()),
        level(8,"Stufe für Stufe","Eine echte Treppe führt zum Ziel. Die senkrechten Kanten brauchen große Räder oder bewegliche Beine.","Große Räder rollen leichter über die Stufenkanten. Ein kräftiger Motor und ein tiefer Schwerpunkt helfen. Probiere 48er Räder an einem 160 langen Rahmen.",390,ground(10,540,390,540,390,525,470,525,470,510,550,510,550,495,630,495,630,480,710,480,710,465,790,465,990,465),strongCar(48,160,400)),
        level(9,"Die Laufmaschine","Kann deine Maschine auch ohne Räder vorankommen? Lass drehende Beine auf Gummifüßen laufen.","Ein 160 langer Rahmen mit Motor trägt links und rechts je einen senkrechten 100 langen Rahmen. Setze an jeden Drehpunkt einen Drehantrieb und an beide Enden der Beine Greiffüße. Beide Antriebe drehen im Uhrzeigersinn, Motor kräftig.",475,ground(10,540,990,540),walker()),
        level(10,"Über Stock und Stein","Kurze Bodenwellen wechseln mit kleinen Plateaus. Baue bewegliche Beine oder ein eigenes Geländefahrzeug.","Beginne mit der Laufmaschine: kurze Beine, zwei Drehantriebe und vier Greiffüße. Ein kräftiger Motor hilft über die Kanten.",475,ground(10,540,400,540,450,525,500,540,560,530,620,530,670,540,740,520,800,540,990,540),walker())
    );
    public static final VehicleLevel SANDBOX=level(-1,"Freie Maschinenwerkstatt","Erfinde deine eigene rollende oder kletternde Maschine. Der Motor bleibt im Startbereich.","Berührende Teile verbinden sich fest. Gelenke verbinden zwei Baugruppen beweglich. Drehantriebe drehen Beine mit Greiffüßen; tippe sie für Tempo und Drehsinn an. Lose Teile bekommen keine Motorkraft.",475,ground(10,540,400,540,530,470,650,540,990,540),car(40,180,430));
    public static VehicleLevel get(int id) { return id<0||id>=LEVELS.size()?SANDBOX:LEVELS.get(id); }
}
