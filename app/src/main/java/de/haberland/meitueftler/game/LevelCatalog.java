package de.haberland.meitueftler.game;
import java.util.List;

public final class LevelCatalog {
    private LevelCatalog() {}
    private static Ramp r(double x,double y,double length,double angle) { return new Ramp(x,y,length,angle,false); }
    private static Ramp wall(double x,double y,double length,double angle) { return new Ramp(x,y,length,angle,true); }
    public static final List<Level> LEVELS=java.util.Arrays.asList(
        new Level(0,"Die erste Rampe","Bring die Murmel in den orangefarbenen Korb.","Lege ein langes Brett unter die Murmel. Das rechte Ende muss tiefer liegen.","Auf einer schrägen Rampe rollt die Murmel zur tieferen Seite.",110,60,720,500,3,1,java.util.Collections.emptyList(),java.util.Arrays.asList(r(300,230,520,20))),
        new Level(1,"Mehr Gefälle","Der Korb steht diesmal näher. Probiere einen anderen Winkel.","Eine steilere Rampe verändert den Weg der Murmel.","Der Winkel beeinflusst sowohl den Schwung als auch die Flugrichtung.",110,60,680,500,3,1,java.util.Collections.emptyList(),java.util.Arrays.asList(r(300,230,520,30))),
        new Level(2,"Die Brücke","Fange die Murmel mit einem zweiten Brett auf.","Das zweite Brett beginnt unter dem Ende des ersten.","Eine zweite Rampe kann eine fallende Murmel auffangen und weiterleiten.",110,60,920,500,4,2,java.util.Arrays.asList(wall(440,480,140,90)),java.util.Arrays.asList(r(280,210,440,15),r(650,370,360,20))),
        new Level(3,"Alles andersherum","Die Murmel startet rechts. Baue einen Weg nach links.","Diesmal müssen die linken Brettenden tiefer liegen.","Drehst du die Rampe um, ändert sich die Rollrichtung.",890,60,80,500,4,2,java.util.Arrays.asList(wall(560,480,140,90)),java.util.Arrays.asList(r(720,210,440,-15),r(350,370,360,-20))),
        new Level(4,"Weit nach rechts","Nutze zwei Rampen für eine weite Reise.","Baue erst eine Rampe unter den Start, dann eine weiter rechts.","Schwung trägt die Murmel auch über eine kleine Lücke.",110,60,930,500,4,2,java.util.Arrays.asList(wall(500,550,180,0)),java.util.Arrays.asList(r(290,190,480,20),r(670,360,360,25))),
        new Level(5,"Weit nach links","Schicke die Murmel zum Korb am linken Rand.","Zwei nach links geneigte Bretter helfen dir.","Du kannst eine Idee spiegeln und damit einen neuen Weg bauen.",890,60,70,500,4,2,java.util.Arrays.asList(wall(500,550,180,0)),java.util.Arrays.asList(r(710,190,480,-20),r(330,360,360,-25))),
        new Level(6,"Sanft anschieben","Auch flache Rampen können die Murmel weit bringen.","Beginne hoch oben und neige die Bretter nur ein wenig.","Auch bei wenig Gefälle sammelt die Murmel unterwegs Schwung.",110,60,940,500,4,2,java.util.Arrays.asList(wall(480,480,120,90)),java.util.Arrays.asList(r(290,160,440,10),r(620,300,360,15))),
        new Level(7,"Die lange Reise","Baue einen sanften Weg von rechts nach links.","Lege das zweite Brett links unter das erste.","Aufeinander abgestimmte Rampen leiten die Murmel Schritt für Schritt weiter.",890,60,60,500,4,2,java.util.Arrays.asList(wall(520,480,120,90)),java.util.Arrays.asList(r(710,160,440,-10),r(380,300,360,-15))),
        new Level(8,"Kurze Bretter","Probiere kürzere Bretter und finde den richtigen Abstand.","Das erste Brett kann kurz sein. Das zweite fängt die Murmel weiter unten auf.","Nicht nur der Winkel, sondern auch die Brettlänge verändert den Weg.",110,60,800,500,4,2,java.util.Arrays.asList(wall(150,450,140,90)),java.util.Arrays.asList(r(250,190,360,25),r(520,350,280,20))),
        new Level(9,"Deine Maschine","Dein letzter Auftrag: Baue einen Weg zum linken Korb.","Beginne rechts mit einem kurzen Brett und ergänze ein zweites in der Mitte.","Ausprobieren, beobachten und umbauen: So entstehen gute Erfindungen.",890,60,200,500,4,2,java.util.Arrays.asList(wall(850,450,140,90)),java.util.Arrays.asList(r(750,190,360,-25),r(480,350,280,-20)))
    );
    public static final Level SANDBOX=new Level(-1,"Freier Bauplatz","Baue deine eigene Murmelmaschine. Was möchtest du ausprobieren?","Hier gibt es keinen vorgeschriebenen Aufbau. Baue, starte und verändere deine Maschine.","Jeder Versuch ist eine neue Idee.",110,60,850,520,12,12,java.util.Collections.emptyList(),java.util.Collections.emptyList());
    public static Level get(int id) { return id<0?SANDBOX:LEVELS.get(Math.min(id,LEVELS.size()-1)); }
}
