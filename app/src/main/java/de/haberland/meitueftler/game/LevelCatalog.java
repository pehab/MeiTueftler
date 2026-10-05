package de.haberland.meitueftler.game;
import java.util.List;

public final class LevelCatalog {
    private LevelCatalog() {}
    private static Ramp r(double x,double y,double length,double angle) { return new Ramp(x,y,length,angle,false); }
    private static Ramp wall(double x,double y,double length,double angle) { return new Ramp(x,y,length,angle,true); }
    private static Ramp spring(double x,double y,double length,double angle) { return new Ramp(x,y,length,angle,false,Ramp.Kind.TRAMPOLINE); }
    private static Ramp block(double x,double y,double length,double angle) { return new Ramp(x,y,length,angle,false,Ramp.Kind.BLOCK); }
    private static Ramp fan(double x,double y,double angle,int power) { Ramp r=new Ramp(x,y,320,angle,false,Ramp.Kind.FAN);r.power=power;return r; }
    private static Ramp linked(double x,double y,double length,double angle,Ramp.Kind kind,int channel) {
        Ramp r=new Ramp(x,y,length,angle,true,kind);r.channel=channel;r.clamp();return r;
    }
    private static Level gateLevel(int id,String name,int source,int channel) {
        Level old=LEVELS.get(source);
        java.util.ArrayList<Ramp> fixed=new java.util.ArrayList<>(old.fixed);
        java.util.ArrayList<Ramp> solution=new java.util.ArrayList<>(old.solution);
        Ramp sensor=linked(old.spawnX,110,160,0,Ramp.Kind.SWITCH,channel);
        if(id==20)fixed.add(sensor);
        else { sensor=new Ramp(source==16?300:old.spawnX,source==16?75:110,160,0,false,Ramp.Kind.SWITCH);sensor.channel=channel;solution.add(sensor); }
        fixed.add(linked(old.goalX,old.goalY-70,560,0,Ramp.Kind.DOOR,channel));
        if(source==10)fixed.add(linked(old.goalX,old.goalY+70,560,0,Ramp.Kind.DOOR,channel));
        return new Level(id,name,id==20?"Berühre den roten Schalter. Die rote Tür öffnet den Weg zum Korb.":"Baue einen Schalter mit der gleichen Verbindung wie die Tür. Die Murmel muss ihn zuerst berühren.",old.hint+(source==16?" Setze den Schalter rechts neben den Start in den Luftstrom und wähle ":" Setze den Schalter unter den Start und wähle ")+sensor.channelLabel()+". Der Sensor ist durchlässig; die Tür bleibt nach Berührung offen.","Eine kleine Berührung kann etwas anderes auslösen. Gleiche Farbe und Zahl verbinden Schalter und Tür.",old.spawnX,old.spawnY,old.goalX,old.goalY,6,old.bonusRamps+(id==20?0:1),fixed,solution);
    }
    public static final List<Level> LEVELS=new java.util.ArrayList<>(java.util.Arrays.asList(
        new Level(0,"Die erste Rampe","Bring die Murmel in den orangefarbenen Korb.","Lege ein langes Brett unter die Murmel. Das rechte Ende muss tiefer liegen.","Auf einer schrägen Rampe rollt die Murmel zur tieferen Seite.",110,60,720,500,3,1,java.util.Collections.emptyList(),java.util.Arrays.asList(r(300,230,520,20))),
        new Level(1,"Mehr Gefälle","Der Korb steht diesmal näher. Probiere einen anderen Winkel.","Eine steilere Rampe verändert den Weg der Murmel.","Der Winkel beeinflusst sowohl den Schwung als auch die Flugrichtung.",110,60,680,500,3,1,java.util.Collections.emptyList(),java.util.Arrays.asList(r(300,230,520,30))),
        new Level(2,"Die Brücke","Fange die Murmel mit einem zweiten Brett auf.","Das zweite Brett beginnt unter dem Ende des ersten.","Eine zweite Rampe kann eine fallende Murmel auffangen und weiterleiten.",110,60,920,500,4,2,java.util.Arrays.asList(wall(440,480,140,90)),java.util.Arrays.asList(r(280,210,440,15),r(650,370,360,20))),
        new Level(3,"Alles andersherum","Die Murmel startet rechts. Baue einen Weg nach links.","Diesmal müssen die linken Brettenden tiefer liegen.","Drehst du die Rampe um, ändert sich die Rollrichtung.",890,60,80,500,4,2,java.util.Arrays.asList(wall(560,480,140,90)),java.util.Arrays.asList(r(720,210,440,-15),r(350,370,360,-20))),
        new Level(4,"Weit nach rechts","Nutze zwei Rampen für eine weite Reise.","Baue erst eine Rampe unter den Start, dann eine weiter rechts.","Schwung trägt die Murmel auch über eine kleine Lücke.",110,60,930,500,4,2,java.util.Arrays.asList(wall(500,550,180,0)),java.util.Arrays.asList(r(290,190,480,20),r(670,360,360,25))),
        new Level(5,"Weit nach links","Schicke die Murmel zum Korb am linken Rand.","Zwei nach links geneigte Bretter helfen dir.","Du kannst eine Idee spiegeln und damit einen neuen Weg bauen.",890,60,70,500,4,2,java.util.Arrays.asList(wall(500,550,180,0)),java.util.Arrays.asList(r(710,190,480,-20),r(330,360,360,-25))),
        new Level(6,"Sanft anschieben","Auch flache Rampen können die Murmel weit bringen.","Beginne hoch oben und neige die Bretter nur ein wenig.","Auch bei wenig Gefälle sammelt die Murmel unterwegs Schwung.",110,60,940,500,4,2,java.util.Arrays.asList(wall(480,480,120,90)),java.util.Arrays.asList(r(290,160,440,10),r(620,300,360,15))),
        new Level(7,"Die lange Reise","Baue einen sanften Weg von rechts nach links.","Lege das zweite Brett links unter das erste.","Aufeinander abgestimmte Rampen leiten die Murmel Schritt für Schritt weiter.",890,60,60,500,4,2,java.util.Arrays.asList(wall(520,480,120,90)),java.util.Arrays.asList(r(710,160,440,-10),r(380,300,360,-15))),
        new Level(8,"Kurze Bretter","Probiere kürzere Bretter und finde den richtigen Abstand.","Das erste Brett kann kurz sein. Das zweite fängt die Murmel weiter unten auf.","Nicht nur der Winkel, sondern auch die Brettlänge verändert den Weg.",110,60,800,500,4,2,java.util.Arrays.asList(wall(150,450,140,90)),java.util.Arrays.asList(r(250,190,360,25),r(520,350,280,20))),
        new Level(9,"Deine Maschine","Dein nächster Auftrag: Baue einen Weg zum linken Korb.","Beginne rechts mit einem kurzen Brett und ergänze ein zweites in der Mitte.","Ausprobieren, beobachten und umbauen: So entstehen gute Erfindungen.",890,60,200,500,4,2,java.util.Arrays.asList(wall(850,450,140,90)),java.util.Arrays.asList(r(750,190,360,-25),r(480,350,280,-20))),
        new Level(10,"Hoch hinaus","Der Korb steht hoch oben. Lass die Murmel mit einem Trampolin springen.","Platziere ein Trampolin unter dem Start und neige es leicht nach rechts.","Die Feder im Trampolin gibt Energie an die Murmel ab. Deshalb kann sie wieder nach oben springen.",200,60,620,180,6,1,java.util.Collections.emptyList(),java.util.Arrays.asList(spring(220,400,240,15))),
        new Level(11,"Die Block-Rutsche","Baue mit einem dicken Block einen Weg nach rechts.","Auch auf der schrägen Oberseite eines Blocks kann die Murmel rollen.","Ein Block kann als Begrenzung oder als kurze, dicke Rampe dienen.",110,60,490,500,6,1,java.util.Collections.emptyList(),java.util.Arrays.asList(block(180,230,240,20))),
        new Level(12,"Sprung nach links","Schicke die Murmel mit einem Trampolin zum hohen Korb links.","Neige das Trampolin diesmal nach links. Das linke Ende liegt tiefer.","Die Neigung des Trampolins bestimmt die Richtung des Rückstoßes.",800,60,380,180,6,1,java.util.Collections.emptyList(),java.util.Arrays.asList(spring(780,400,240,-15))),
        new Level(13,"Der Block nach links","Lenke die Murmel vom rechten Start zum Korb in der Mitte.","Setze einen Block unter den rechten Start und neige ihn nach links.","Die Murmel folgt der Oberfläche. Du kannst denselben Baustein für verschiedene Wege nutzen.",890,60,510,500,6,1,java.util.Collections.emptyList(),java.util.Arrays.asList(block(820,230,240,-30))),
        new Level(14,"Über die Mauer","Die feste Mauer steht im Weg. Springe darüber hinweg.","Ein tief liegendes, leicht nach rechts geneigtes Trampolin bringt die Murmel über die Mauer.","Mit einem Sprung kannst du ein Hindernis überwinden. Höhe und Richtung müssen zusammenpassen.",200,60,780,400,6,1,java.util.Arrays.asList(new Ramp(450,370,240,90,true,Ramp.Kind.BLOCK)),java.util.Arrays.asList(spring(220,400,240,15))),
        new Level(15,"Springen und umlenken","Kombiniere ein Trampolin und einen Block zu deiner neuen Maschine.","Springe zuerst nach rechts. Ein nach links geneigter Block unter der Flugbahn lenkt die Murmel zurück zum Korb.","Bauteile können zusammenarbeiten: Erst gibt die Feder Schwung, dann verändert der Block die Richtung.",200,60,450,500,6,2,java.util.Collections.emptyList(),java.util.Arrays.asList(spring(220,400,240,15),block(700,330,240,-20))),
        new Level(16,"Frischer Wind","Ein Ventilator hilft der Murmel zum weit entfernten Korb.","Setze einen mittelstarken Ventilator links unter den Start. Der Luftkegel zeigt schräg nach rechts oben.","Luft kann die Murmel anschieben. Richtung und Stärke bestimmen, wohin sie getragen wird.",200,60,710,500,6,1,java.util.Collections.emptyList(),java.util.Arrays.asList(fan(120,300,-60,2))),
        new Level(17,"Kräftig pusten","Der Korb steht weiter rechts. Probiere kräftigen Wind.","Nimm denselben schrägen Luftkegel und stelle die Stärke auf kräftig.","Stärkerer Wind gibt der Murmel mehr Schub. Sie kommt dadurch an einer anderen Stelle an.",200,60,780,500,6,1,java.util.Collections.emptyList(),java.util.Arrays.asList(fan(120,300,-60,3))),
        new Level(18,"Wind nach links","Puste die Murmel vom rechten Start nach links.","Drehe den Ventilator nach links oben und setze ihn rechts unter den Start.","Du kannst die Blasrichtung rundherum drehen. Luft wirkt nur im sichtbaren Luftkegel.",800,60,290,500,6,1,java.util.Collections.emptyList(),java.util.Arrays.asList(fan(880,300,-120,2))),
        new Level(19,"Luftpost","Der Korb hängt hoch oben. Trage die Murmel mit kräftigem Wind dorthin.","Ein kräftiger Ventilator links unter dem Start kann die Murmel über die Werkbank tragen.","Luft kann einen fallenden Ball bremsen und anheben. Sobald er den Luftkegel verlässt, fällt er wieder.",200,60,620,180,6,1,java.util.Collections.emptyList(),java.util.Arrays.asList(fan(120,300,-60,3)))
    ));
    static {
        LEVELS.add(gateLevel(20,"Sesam, öffne dich!",0,1));
        LEVELS.add(gateLevel(21,"Die blaue Tür",3,2));
        LEVELS.add(gateLevel(22,"Sprung mit Schalter",10,3));
        LEVELS.add(gateLevel(23,"Wind öffnet Wege",16,4));
    }
    public static final Level SANDBOX=new Level(-1,"Freier Bauplatz","Baue mit Brettern, Trampolinen, Blöcken, Ventilatoren, Schaltern und Türen deine eigene Murmelmaschine.","Hier gibt es keinen vorgeschriebenen Aufbau. Baue, starte und verändere deine Maschine.","Jeder Versuch ist eine neue Idee.",110,60,850,520,20,20,java.util.Collections.emptyList(),java.util.Collections.emptyList());
    public static Level get(int id) { return id<0?SANDBOX:LEVELS.get(Math.min(id,LEVELS.size()-1)); }
}
