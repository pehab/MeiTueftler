package de.haberland.meitueftler.game.mechanics;

import java.util.Arrays;
import java.util.List;
import static de.haberland.meitueftler.game.mechanics.MechanicPart.Kind.*;

public final class MechanicCatalog {
    private MechanicCatalog() { }
    private static List<MechanicPart> lever(double length,double weightX,double y) {
        return Arrays.asList(new MechanicPart(BEAM,500,y,length,0),new MechanicPart(WEIGHT,weightX,y,8,0));
    }
    private static List<MechanicPart> lift(boolean corner,int power) {
        MechanicPart winch=new MechanicPart(WINCH,corner?300:620,corner?160:100,22,0);winch.power=power;
        MechanicPart rope=MechanicPart.rope(winch.x,winch.y,620,460,corner?1:-1);
        return corner?Arrays.asList(winch,new MechanicPart(PULLEY,620,100,22,0),rope):Arrays.asList(winch,rope);
    }
    public static final List<MechanicLevel> LEVELS=Arrays.asList(
        new MechanicLevel(0,"Der erste Hebel","Hebe die Kiste mit einem Balken und einem Gegengewicht ins grüne Feld.",
            "Lege einen 240 langen Balken waagerecht durch den Drehpunkt. Sein rechtes Ende berührt die Kiste. Setze ein 8er Gewicht ans linke Ende.",620,400,3,585,295,false,new double[]{500,400},lever(240,380,400)),
        new MechanicLevel(1,"Langer Arm, große Wirkung","Nutze einen längeren Hebelarm, um die Last zu heben.",
            "Ein 320 langer Balken bei x=500 berührt die Kiste und den Drehpunkt. Setze das 8er Gewicht ans linke Ende: mehr Abstand zum Drehpunkt bedeutet mehr Drehwirkung.",620,400,5,590,330,false,new double[]{540,400},lever(320,340,400)),
        new MechanicLevel(2,"Das Gegengewicht","Die Kiste ist schwerer. Bringe deinen Hebel ins richtige Gleichgewicht.",
            "Probiere einen 240 langen Balken durch den mittleren Drehpunkt und ein 8er Gegengewicht links. Ein zu leichtes Gewicht hebt die Kiste nicht.",620,380,6,580,275,false,new double[]{500,380},lever(240,380,380)),
        new MechanicLevel(3,"Der erste Aufzug","Ziehe die Kiste auf ihrer Führung nach oben in die Zielzone.",
            "Setze eine Seilwinde auf den oberen Befestigungspunkt. Verbinde ein Seil von der Winde bis zur Kiste. Die Winde zieht es automatisch ein.",620,460,3,620,220,true,new double[]{620,100},lift(false,2)),
        new MechanicLevel(4,"Um die Ecke","Der Motor sitzt links. Lenke sein Seil zur Kiste um.",
            "Seilwinde links, Umlenkrolle oben rechts. Verbinde die Winde mit der Kiste und wähle beim Seil über Umlenkung die Rolle. Die Rolle ändert die Richtung, nicht die Kraft.",620,460,3,620,220,true,new double[]{300,160,620,100},lift(true,2)),
        new MechanicLevel(5,"Die schwere Fracht","Welche Winde schafft die schwere Kiste nach oben?",
            "Baue den Seilaufzug mit der Winde oben. Stelle sie auf kräftig. Eine schwache Winde kann diese Kiste nicht heben.",620,460,8,620,220,true,new double[]{620,100},lift(false,3))
    );
    public static final MechanicLevel SANDBOX=new MechanicLevel(-1,"Freie Mechanikwerkstatt","Erfinde Hebel, Seilzüge und eigene Hebemaschinen. Keine Sterne und keine Teile-Vorgaben.",
        "Balken an einem Befestigungspunkt drehen sich. Zwei Befestigungen halten sie fest. Seilenden berühren Balken, Gewichte, Kiste oder Winde. Umlenkrollen müssen am Befestigungspunkt sitzen.",620,400,3,585,295,false,new double[]{500,400,300,160,620,100,180,400,820,160},lever(240,380,400));
    public static MechanicLevel get(int id) { return id<0||id>=LEVELS.size()?SANDBOX:LEVELS.get(id); }
}
