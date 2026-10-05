package de.haberland.meitueftler.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Authored workshop routes. Every hint has a solution verified with the actual game physics. */
public final class LevelCatalog {
    private LevelCatalog() {}
    private static Ramp r(double x,double y,double length,double angle) { return new Ramp(x,y,length,angle,false); }
    private static Ramp spring(double x,double y,double length,double angle) { return new Ramp(x,y,length,angle,false,Ramp.Kind.TRAMPOLINE); }
    private static Ramp block(double x,double y,double length,double angle) { return new Ramp(x,y,length,angle,false,Ramp.Kind.BLOCK); }
    private static Ramp fan(double x,double y,double angle,int power) { return air(x,y,320,angle,power); }
    private static Ramp air(double x,double y,double length,double angle,int power) { Ramp r=new Ramp(x,y,length,angle,false,Ramp.Kind.FAN);r.power=power;return r; }
    private static List<Ramp> parts(Ramp... ramps) { return Arrays.asList(ramps); }
    private static Ramp barrier(double x,double y,double length,double angle) {
        // Border barriers extend outside the board so there is no shortcut around the visible edge.
        return new Ramp(x,y,length,angle,true,Ramp.Kind.BLOCK);
    }
    private static Ramp linked(double x,double y,double length,double angle,Ramp.Kind kind,int channel,boolean fixed) {
        Ramp r=new Ramp(x,y,length,angle,fixed,kind);r.channel=channel;return r;
    }
    private static Level puzzle(int id,String name,String task,String hint,String discovery,double sx,double sy,double gx,double gy,List<Ramp> fixed,List<Ramp> solution) {
        return new Level(id,name,task,hint,discovery,sx,sy,gx,gy,8,solution.size(),fixed,solution,2);
    }
    private static Level switched(int id,String name,Level route,double x,double y,int channel,boolean movable) {
        List<Ramp> fixed=new ArrayList<>(route.fixed),solution=new ArrayList<>(route.solution);
        Ramp sensor=linked(x,y,160,0,Ramp.Kind.SWITCH,channel,!movable);
        if(movable)solution.add(sensor);else fixed.add(sensor);
        fixed.add(linked(route.goalX,route.goalY-70,560,0,Ramp.Kind.DOOR,channel,true));
        fixed.add(linked(route.goalX,route.goalY+70,560,0,Ramp.Kind.DOOR,channel,true));
        return puzzle(id,name,route.task+" Berühre zuerst den "+sensor.channelLabel()+"-Schalter, um die Türen am Korb zu öffnen.",
                route.hint+(movable?" Baue außerdem einen Schalter mit Verbindung ":" Führe die Murmel unterwegs durch den festen Schalter mit Verbindung ")+sensor.channelLabel()+".",
                "Erst den Weg meistern, dann die passende Tür öffnen. "+route.discovery,
                route.spawnX,route.spawnY,route.goalX,route.goalY,fixed,solution);
    }
    private static List<Level> createLevels() {
        List<Level> levels=new ArrayList<>();
        levels.add(new Level(0,"Die erste Rampe","Bring die Murmel in den orangefarbenen Korb.","Lege ein langes Brett unter die Murmel. Das rechte Ende muss tiefer liegen.","Auf einer schrägen Rampe rollt die Murmel zur tieferen Seite.",110,60,720,500,3,1,java.util.Collections.emptyList(),java.util.Arrays.asList(r(300,230,520,20))));
        levels.add(new Level(1,"Mehr Gefälle","Der Korb steht diesmal näher. Probiere einen anderen Winkel.","Eine steilere Rampe verändert den Weg der Murmel.","Der Winkel beeinflusst sowohl den Schwung als auch die Flugrichtung.",110,60,680,500,3,1,java.util.Collections.emptyList(),java.util.Arrays.asList(r(300,230,520,30))));
        levels.add(puzzle(2,"Unter der Werkbank","Das Dach versperrt die direkte Flugbahn. Finde einen Weg darunter.","Leite die Murmel mit einem flachen Brett unter das Dach. Ein kleines Trampolin rechts unten gibt den letzten Schub.","Ein niedriger Durchgang braucht einen anderen Weg als ein hoher Sprung.",160.0,60.0,850.0,480.0,parts(barrier(200,500,160,90),barrier(300.000,-450.000,1400,90),barrier(620.000,220.000,560,0)),parts(r(226.658,368.743,440,10),spring(577.217,544.108,160,10))));
        levels.add(puzzle(3,"Hin und zurück","Start und Ziel liegen links, aber die Zwischenwand endet erst rechts.","Rolle oben nach rechts am Ende der Wand vorbei. Ein nach links geneigtes Trampolin schickt die Murmel unten zurück; Wind hilft beim Auffangen.","Manchmal führt der Weg zum Ziel zuerst vom Ziel weg.",180.0,60.0,160.0,490.0,parts(barrier(380.000,290.000,720,0)),parts(r(293.000,272.000,280,30),spring(873.000,494.000,200,-35),air(390.000,498.000,240,-80,2))));
        levels.add(puzzle(4,"Über den Wall","Überquere den Wall und lande unter dem Dach auf der anderen Seite.","Ein steiles Brett gibt Schwung für das Trampolin. Rechts unten bremst ein aufwärts gerichteter Ventilator den Fall.","Absprung und Landung sind zwei verschiedene Aufgaben.",160.0,60.0,850.0,450.0,parts(barrier(250,100,160,90),barrier(480.000,610.000,560,90),barrier(780.000,200.000,320,0)),parts(r(180.000,350.000,520,60),spring(269.229,517.943,200,0),air(820.000,543.081,280,-90,3))));
        levels.add(puzzle(5,"Die S-Kurve","Zwei versetzte Zwischenwände lassen nur einen Weg im Zickzack frei.","Leite die Murmel zuerst nach rechts über die obere Wand hinaus. Ein zweites Brett lenkt sie zwischen den Wänden zurück nach links.","Zwei Richtungswechsel bringen dich durch den versetzten Weg.",160.0,60.0,160.0,530.0,parts(barrier(310.000,230.000,700,0),barrier(700.000,410.000,700,0)),parts(r(340.000,151.000,560,20),r(633.000,328.000,480,-20))));
        levels.add(puzzle(6,"Tal und Hügel","Erst unten durch, dann über den nächsten Hügel: Finde die passende Höhe.","Ein langes, flaches Brett führt unter der ersten Sperre hindurch. Ein kräftiger Ventilator vor dem Hügel hebt die Murmel an.","Schwung und Auftrieb helfen an unterschiedlichen Stellen.",160.0,60.0,850.0,490.0,parts(barrier(350.000,-340.000,1400,90),barrier(670.000,680.000,560,90)),parts(r(337.000,429.239,560,15),air(615.488,539.000,240,-85,3))));
        levels.add(puzzle(7,"Zwei Fenster","Die beiden Sperren haben Durchgänge in der Mitte. Halte die Murmel auf der passenden Höhe.","Baue ein flaches Brett zum ersten Fenster und ein leicht geneigtes Trampolin zwischen die Sperren.","Ein Durchgang begrenzt Höhe und Richtung gleichzeitig.",160.0,60.0,850.0,480.0,parts(barrier(250,150,160,90),barrier(380.000,-450.000,1400,90),barrier(380.000,830.000,560,90),barrier(650.000,-450.000,1400,90),barrier(650.000,830.000,560,90)),parts(r(226.658,368.743,440,10),spring(577.217,544.108,160,10))));
        levels.add(puzzle(8,"Drei Sperren","Unten, oben, unten: Drei versetzte Sperren stehen zwischen Start und Korb.","Ein steiles Brett und ein flaches Trampolin bringen die Murmel durch den ersten Umweg. Wind rechts unten fängt sie nach dem Hügel auf.","Beobachte nicht nur das nächste Hindernis, sondern auch das danach.",160.0,60.0,940.0,510.0,parts(barrier(310.000,-430.000,1400,90),barrier(600.000,610.000,560,90),barrier(820.000,-450.000,1400,90)),parts(r(189.798,350.000,480,60),spring(272.937,538.895,240,0),air(825.726,558.000,280,-90,3))));
        levels.add(puzzle(9,"Auf die Galerie","Der Korb steht auf einer hohen Galerie hinter einer Sperre.","Führe die Murmel mit einem flachen Brett unter der Sperre hindurch. Ein kräftiger Ventilator rechts unter der Galerie hebt sie zum Korb.","Unten Schwung sammeln und erst am Ziel Höhe gewinnen.",160.0,60.0,800.0,180.0,parts(barrier(250,100,160,90),barrier(700.000,330.000,560,0),barrier(430.000,-560.000,1400,90)),parts(r(260.000,265.000,480,10),air(818.000,522.000,280,-90,3))));
        levels.add(new Level(10,"Hoch hinaus","Der Korb steht hoch oben. Lass die Murmel mit einem Trampolin springen.","Platziere ein Trampolin unter dem Start und neige es leicht nach rechts.","Die Feder im Trampolin gibt Energie an die Murmel ab. Deshalb kann sie wieder nach oben springen.",200,60,620,180,6,1,java.util.Collections.emptyList(),java.util.Arrays.asList(spring(220,400,240,15))));
        levels.add(new Level(11,"Die Block-Rutsche","Baue mit einem dicken Block einen Weg nach rechts.","Auch auf der schrägen Oberseite eines Blocks kann die Murmel rollen.","Ein Block kann als Begrenzung oder als kurze, dicke Rampe dienen.",110,60,490,500,6,1,java.util.Collections.emptyList(),java.util.Arrays.asList(block(180,230,240,20))));
        levels.add(puzzle(12,"Unten durch, oben ankommen","Start links oben, Ziel rechts oben: Die mittlere Sperre lässt nur unten durch.","Ein kurzes, steiles Brett gibt Schwung nach rechts. Ein flaches Trampolin tief vor der Sperre lässt die Murmel unten durch und danach wieder aufsteigen.","Ein tiefer Umweg kann dich zu einem hohen Ziel führen.",160.0,60.0,840.0,160.0,parts(barrier(500.000,-300.000,1400,90)),parts(r(221.000,151.421,280,45),spring(436.000,555.000,320,0))));
        levels.add(puzzle(13,"Der hohe Balkon","Der Weg zum Balkon führt unter der langen Sperre hindurch. Über dem Korb sitzt ein Dach.","Starte mit einer Rampe nach rechts. Ein sanfter schräger Luftstrom und ein nach links geneigtes Trampolin helfen beim Aufstieg hinter der Sperre.","Für eine Landung im Balkon brauchst du Höhe und die passende Richtung.",160.0,60.0,850.0,190.0,parts(barrier(680.000,-300.000,1400,90),barrier(865.000,70.000,230,0)),parts(r(292.754,234.883,480,30),spring(686.608,521.765,280,-15),air(557.090,486.976,200,-50,1))));
        levels.add(puzzle(14,"Durchs Tal zum Gipfel","Eine Sperre hängt von oben, die nächste wächst von unten. Der Korb wartet dahinter.","Ein Brett führt tief ins Tal. Ein Ventilator in der Mitte hebt die Murmel über den Hügel; ein leicht geneigtes Trampolin rechts fängt sie auf.","Zwischen zwei Hindernissen muss die Murmel ihre Höhe ändern.",160.0,60.0,900.0,220.0,parts(barrier(420.000,-330.000,1400,90),barrier(720.000,650.000,480,90)),parts(r(210,389,440,25),spring(853,495,200,-15),air(501,544,320,-80,2))));
        levels.add(puzzle(15,"Die linke Empore","Der Start ist rechts oben. Unter der Sperre hindurch geht es zum hohen Korb links.","Leite die Murmel nach links unten. Ein Trampolin und kräftiger Wind links sorgen hinter der Sperre für den Aufstieg.","Erst die Richtung ändern, dann den Auftrieb am richtigen Ort einsetzen.",820.0,60.0,160.0,180.0,parts(barrier(460.000,-320.000,1400,90)),parts(r(688.420,332.000,320,-25),spring(285.000,540.635,200,5),air(83.000,511.000,280,-70,3))));
        levels.add(new Level(16,"Frischer Wind","Ein Ventilator hilft der Murmel zum weit entfernten Korb.","Setze einen mittelstarken Ventilator links unter den Start. Der Luftkegel zeigt schräg nach rechts oben.","Luft kann die Murmel anschieben. Richtung und Stärke bestimmen, wohin sie getragen wird.",200,60,710,500,6,1,java.util.Collections.emptyList(),java.util.Arrays.asList(fan(120,300,-60,2))));
        levels.add(switched(17,"Wind und roter Schalter",levels.get(4),404.0,332.0,1,false));
        levels.add(switched(18,"Die blaue Windfähre",levels.get(15),440.0,457.0,2,false));
        levels.add(switched(19,"Luftpost zur gelben Tür",levels.get(9),512.0,292.0,3,false));
        levels.add(new Level(20,"Sesam, öffne dich!","Berühre den roten Schalter. Die rote Tür öffnet den Weg zum Korb.","Lege ein langes Brett unter die Murmel. Das rechte Ende muss tiefer liegen. Der feste rote Schalter öffnet die Tür.","Eine kleine Berührung kann etwas anderes auslösen. Gleiche Farbe und Zahl verbinden Schalter und Tür.",110,60,720,500,6,1,parts(linked(110,110,160,0,Ramp.Kind.SWITCH,1,true),linked(720,430,560,0,Ramp.Kind.DOOR,1,true)),parts(r(300,230,520,20))));
        levels.add(switched(21,"Schalter in der S-Kurve",levels.get(5),776.0,256.0,2,false));
        levels.add(switched(22,"Umweg mit Sprungschalter",levels.get(12),465.0,452.0,3,true));
        levels.add(switched(23,"Die große Maschine",levels.get(14),484.0,496.0,4,true));
        return Collections.unmodifiableList(levels);
    }
    public static final List<Level> LEVELS=createLevels();
    public static final Level SANDBOX=new Level(-1,"Freier Bauplatz","Baue mit Brettern, Trampolinen, Blöcken, Ventilatoren, Schaltern und Türen deine eigene Murmelmaschine.","Hier gibt es keinen vorgeschriebenen Aufbau. Baue, starte und verändere deine Maschine.","Jeder Versuch ist eine neue Idee.",110,60,850,520,20,20,Collections.emptyList(),Collections.emptyList());
    public static Level get(int id) { return id<0?SANDBOX:LEVELS.get(Math.min(id,LEVELS.size()-1)); }
}
