package de.haberland.meitueftler.game.vehicle;

import java.util.ArrayList;
import java.util.List;

/** A joint replaces a weld at its position and must join exactly two rigid assemblies. */
public final class VehicleAssembly {
    public static final double JOINT_REACH=6;
    public final int[] rigidRoots, connectedRoots;
    public final boolean[] validJoint;
    public final List<Link> links=new ArrayList<>();
    public int invalidJoints;
    public static final class Link {
        public final int marker,a,b;
        Link(int marker,int a,int b) { this.marker=marker;this.a=a;this.b=b; }
    }
    public VehicleAssembly(List<VehiclePart> parts) {
        int n=parts.size();rigidRoots=new int[n];validJoint=new boolean[n];
        for(int i=0;i<n;i++)rigidRoots[i]=i;
        for(int i=0;i<n;i++)if(!parts.get(i).joint())for(int j=i+1;j<n;j++)if(!parts.get(j).joint()&&touching(parts.get(i),parts.get(j))) {
            boolean hinged=false;
            for(VehiclePart p:parts)if(p.joint()&&parts.get(i).distance(p.x,p.y)<=JOINT_REACH&&parts.get(j).distance(p.x,p.y)<=JOINT_REACH){hinged=true;break;}
            if(!hinged)join(rigidRoots,i,j);
        }
        for(int i=0;i<n;i++)rigidRoots[i]=root(rigidRoots,i);
        for(int i=0;i<n;i++)if(parts.get(i).joint()) {
            VehiclePart marker=parts.get(i);List<Integer> neighbours=new ArrayList<>();
            for(int j=0;j<n;j++)if(!parts.get(j).joint()&&parts.get(j).distance(marker.x,marker.y)<=JOINT_REACH&&!neighbours.contains(rigidRoots[j]))neighbours.add(rigidRoots[j]);
            if(!neighbours.isEmpty())rigidRoots[i]=neighbours.get(0);
            if(neighbours.size()==2){validJoint[i]=true;links.add(new Link(i,neighbours.get(0),neighbours.get(1)));}
            else invalidJoints++;
        }
        connectedRoots=rigidRoots.clone();for(Link link:links)join(connectedRoots,link.a,link.b);
        for(int i=0;i<n;i++)connectedRoots[i]=root(connectedRoots,i);
    }
    private static void join(int[] roots,int a,int b) { roots[root(roots,b)]=root(roots,a); }
    private static int root(int[] roots,int i) { while(roots[i]!=i)i=roots[i];return i; }
    public static boolean touching(VehiclePart a,VehiclePart b) {
        double aa=Math.toRadians(a.angle),ba=Math.toRadians(b.angle);
        double adx=Math.cos(aa)*a.halfLength(),ady=Math.sin(aa)*a.halfLength(),bdx=Math.cos(ba)*b.halfLength(),bdy=Math.sin(ba)*b.halfLength();
        double distance=Math.min(Math.min(b.distance(a.x-adx,a.y-ady)+b.radius(),b.distance(a.x+adx,a.y+ady)+b.radius()),
            Math.min(a.distance(b.x-bdx,b.y-bdy)+a.radius(),a.distance(b.x+bdx,b.y+bdy)+a.radius()));
        // Distances above clamp inside the capsule. Any contained endpoint already touches.
        if(b.distance(a.x-adx,a.y-ady)==0||b.distance(a.x+adx,a.y+ady)==0||a.distance(b.x-bdx,b.y-bdy)==0||a.distance(b.x+bdx,b.y+bdy)==0)return true;
        double det=adx*bdy-ady*bdx;
        if(Math.abs(det)>1e-9) {
            double dx=b.x-a.x,dy=b.y-a.y,t=(dx*bdy-dy*bdx)/det,u=(dx*ady-dy*adx)/det;
            if(Math.abs(t)<=1&&Math.abs(u)<=1)return true;
        }
        return distance<=a.radius()+b.radius()+VehiclePhysics.CONNECTION_GAP;
    }
}
