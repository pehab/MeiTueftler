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
    private static MechanicPart part(MechanicPart.Kind kind,double x,double y,double size,double angle){return new MechanicPart(kind,x,y,size,angle);}
    private static MechanicPart winch(double x,double y,int power){MechanicPart p=part(WINCH,x,y,22,0);p.power=power;return p;}
    private static MechanicPart rope(double ax,double ay,double bx,double by,int guide){return MechanicPart.rope(ax,ay,bx,by,guide);}
    private static List<MechanicPart> slackPendulum(){MechanicPart p=rope(450,90,720,180,-1);p.size=80;return parts(p);}
    private static List<MechanicPart> parts(MechanicPart... parts){return Arrays.asList(parts);}
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
            "Baue den Seilaufzug mit der Winde oben. Stelle sie auf kräftig. Eine schwache Winde kann diese Kiste nicht heben.",620,460,8,620,220,true,new double[]{620,100},lift(false,3)),
        new MechanicLevel(6,"Alles andersherum","Hebe die Kiste links. Welche Seite braucht das Gegengewicht?",
            "Ein 240er Balken bei (500,400), ein 8er Gewicht rechts bei (620,400). Die schwere Seite geht nach unten, die Kiste links nach oben.",380,400,3,420,310,false,new double[]{500,400},parts(part(BEAM,500,400,240,0),part(WEIGHT,620,400,8,0))),
        new MechanicLevel(7,"Die schräge Wippe","Die Kiste steht höher als der Drehpunkt. Baue einen schrägen Hebel.",
            "Lege einen 300er Balken mit -30° bei (500,380) durch den Drehpunkt. Ein 8er Gewicht bei (370,455) hebt die Kiste am anderen Ende.",630,305,3,570,250,false,new double[]{500,380},parts(part(BEAM,500,380,300,-30),part(WEIGHT,370,455,8,0))),
        new MechanicLevel(8,"Kleiner Lastarm","Schwere Kiste, wenig Platz: Nutze unterschiedliche Hebelarme.",
            "Ein 320er Balken bei (480,360) und ein 10er Gewicht bei (320,360). Der Drehpunkt bei (560,360) liegt dicht an der Last.",620,360,9,610,325,false,new double[]{560,360},parts(part(BEAM,480,360,320,0),part(WEIGHT,320,360,10,0))),
        new MechanicLevel(9,"Der Winkelhebel","Verbinde zwei Balken zu einem Winkel. Das Gegengewicht zieht an einem anderen Arm.",
            "Ein 240er Balken waagerecht bei (500,380), ein 160er Balken senkrecht bei (380,300). Das 10er Gewicht sitzt oben bei (380,220). Berührende Balken bilden einen Winkelhebel.",620,380,3,575,285,false,new double[]{500,380},parts(part(BEAM,500,380,240,0),part(BEAM,380,300,160,90),part(WEIGHT,380,220,10,0))),
        new MechanicLevel(10,"Die Pendelfähre","Schwinge die Kiste nach links ins Ziel. Ein Motor ist nicht nötig.",
            "Verbinde den Punkt bei (500,120) mit der Kiste durch ein Seil. Ohne Zugabe pendelt sie unter dem Punkt hindurch.",700,260,3,365,320,false,new double[]{500,120},parts(rope(500,120,700,260,-1))),
        new MechanicLevel(11,"Großer Pendelbogen","Ein langes Seil bringt die Kiste weit auf die andere Seite.",
            "Ein Seil vom Punkt bei (500,80) bis zur Kiste. Vergleiche den großen Bogen mit der kurzen Pendelfähre.",800,240,4,250,310,false,new double[]{500,80},parts(rope(500,80,800,240,-1))),
        new MechanicLevel(12,"Das Fangseil","Die Kiste fällt und wird erst dann vom Seil aufgefangen. Plane den Schwung.",
            "Verbinde den Punkt bei (450,90) mit der Kiste. Gib dem Seil 80 Einheiten Zugabe: Es spannt sich erst nach dem ersten Fall.",720,180,3,380,430,false,new double[]{450,90},slackPendulum()),
        new MechanicLevel(13,"Aufzug ohne Motor","Ein fallendes Gegengewicht hebt die Kiste auf ihrer Führung.",
            "Rolle auf (500,100), ein 8er Gewicht bei (300,240). Verbinde das Gewicht mit der Kiste und wähle die Rolle als Umlenkung.",620,450,3,620,260,true,new double[]{500,100},parts(part(PULLEY,500,100,22,0),part(WEIGHT,300,240,8,0),rope(300,240,620,450,0))),
        new MechanicLevel(14,"Die schwebende Fracht","Die Kiste hat keine Führung. Hebe und versetze sie mit einem Gegengewicht.",
            "Rolle bei (500,100), 8er Gewicht bei (300,240), Seil vom Gewicht zur Kiste über die Rolle. Beobachte den seitlichen Schwung.",620,450,3,390,180,false,new double[]{500,100},parts(part(PULLEY,500,100,22,0),part(WEIGHT,300,240,8,0),rope(300,240,620,450,0))),
        new MechanicLevel(15,"Diagonal nach links","Die Winde zieht schräg. Die Kiste darf frei schwingen.",
            "Winde bei (300,120), Seil zur Kiste. Mittel reicht; die Kiste schwingt während des Einziehens weit nach links.",650,430,3,180,480,false,new double[]{300,120},parts(winch(300,120,2),rope(300,120,650,430,-1))),
        new MechanicLevel(16,"Diagonal nach rechts","Baue einen schrägen Seilzug in die andere Richtung.",
            "Winde bei (700,120), Seil von dort zur Kiste links. Probiere die Windengeschwindigkeit und beobachte den Schwung.",350,430,3,820,480,false,new double[]{700,120},parts(winch(700,120,2),rope(700,120,350,430,-1))),
        new MechanicLevel(17,"Der hängende Kran","Die Winde steht abseits. Die Umlenkrolle bestimmt, wohin die Kiste schwingt.",
            "Winde bei (180,180), Rolle bei (550,90). Verbinde Winde und Kiste, wähle Rolle 2 als Umlenkung. Die frei hängende Kiste schwingt zum Ziel.",720,420,4,510,290,false,new double[]{180,180,550,90},parts(winch(180,180,2),part(PULLEY,550,90,22,0),rope(180,180,720,420,1))),
        new MechanicLevel(18,"Die Seilwippe","Hebe einen Balken mit der Winde statt mit einem Gewicht.",
            "240er Balken bei (500,400), Winde bei (750,100), Seil von der Winde zum rechten Balkenende an der Kiste. Die Wippe dreht um (500,400).",620,400,6,600,335,false,new double[]{500,400,750,100},parts(part(BEAM,500,400,240,0),winch(750,100,2),rope(750,100,620,400,-1))),
        new MechanicLevel(19,"Ziehen am langen Arm","Die Kiste ist links vom Drehpunkt. Ziehe rechts nach unten, um sie zu heben.",
            "Ein 320er Balken bei (500,360), Winde unten bei (760,500). Ein Seil verbindet die Winde mit dem rechten Balkenende bei (660,360).",380,360,6,410,280,false,new double[]{500,360,760,500},parts(part(BEAM,500,360,320,0),winch(760,500,3),rope(760,500,660,360,-1))),
        new MechanicLevel(20,"Zwei Winden, eine Last","Eine Winde reicht für diese Last nicht. Teile die Arbeit auf zwei Seile auf.",
            "Setze kräftige Winden bei (570,90) und (670,90). Zwei getrennte Seile ziehen an der Kiste. Die Führung hält sie in der Mitte.",620,450,20,620,260,true,new double[]{570,90,670,90},parts(winch(570,90,3),winch(670,90,3),rope(570,90,620,450,-1),rope(670,90,620,450,-1))),
        new MechanicLevel(21,"Gegengewicht hilft mit","Hilf der Winde mit einem fallenden Gegengewicht.",
            "Mittlere Winde bei (620,90), Rolle bei (400,100), 8er Gewicht bei (230,200). Ein Seil zieht direkt zur Winde, ein zweites verbindet Gewicht und Kiste über Rolle 2.",620,450,12,620,280,true,new double[]{620,90,400,100},parts(winch(620,90,2),part(PULLEY,400,100,22,0),part(WEIGHT,230,200,8,0),rope(620,90,620,450,-1),rope(230,200,620,450,1))),
        new MechanicLevel(22,"Gewicht an der Leine","Das Gegengewicht hängt neben der Wippe und zieht über ein Seil an ihrem Arm.",
            "240er Balken bei (500,360), 10er Gewicht bei (320,450). Verbinde das Gewicht per Seil mit dem linken Balkenende bei (380,360). Es zieht den linken Arm nach unten.",620,360,4,590,285,false,new double[]{500,360},parts(part(BEAM,500,360,240,0),part(WEIGHT,320,450,10,0),rope(320,450,380,360,-1))),
        new MechanicLevel(23,"Die große Hebemaschine","Kombiniere einen Winkelhebel, Seilzug und Gegengewicht für eine schwere Last.",
            "240er Balken bei (500,400), senkrechter 160er Balken bei (380,320), 8er Gewicht oben bei (380,240). Eine mittlere Winde bei (750,100) zieht per Seil an der Kiste. Gewicht und Winde arbeiten zusammen.",620,400,12,590,320,false,new double[]{500,400,750,100},parts(part(BEAM,500,400,240,0),part(BEAM,380,320,160,90),part(WEIGHT,380,240,8,0),winch(750,100,2),rope(750,100,620,400,-1)))
    );
    public static final MechanicLevel SANDBOX=new MechanicLevel(-1,"Freie Mechanikwerkstatt","Erfinde Hebel, Seilzüge und eigene Hebemaschinen. Keine Sterne und keine Teile-Vorgaben.",
        "Balken an einem Befestigungspunkt drehen sich. Zwei Befestigungen halten sie fest. Seilenden berühren Balken, Gewichte, Kiste oder Winde. Umlenkrollen müssen am Befestigungspunkt sitzen.",620,400,3,585,295,false,new double[]{500,400,300,160,620,100,180,400,820,160},lever(240,380,400));
    public static MechanicLevel get(int id) { return id<0||id>=LEVELS.size()?SANDBOX:LEVELS.get(id); }
}
