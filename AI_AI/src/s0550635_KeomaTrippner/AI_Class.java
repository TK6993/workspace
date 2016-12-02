package s0550635_KeomaTrippner;

import java.awt.Point;
import java.awt.Polygon;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.geom.Area;


import lenz.htw.ai4g.ai.AI;
import lenz.htw.ai4g.ai.DriverAction;
import lenz.htw.ai4g.ai.Info;

public class AI_Class extends AI {
	
	int zielradi = 50;
	float nodedifferenceToObs = 25;
	Area areaOfAllObs = new Area();
	float neededAngle;
	float rotationalAcceleration;
	float orientation;
	float orientToObsMiddle;
	float acceleration;
	double desiredSpeed= info.getMaxVelocity();
	double desiredRotationalSpeed = info.getMaxAngularVelocity();
	double distance;
	double distanceToObsMid;
	public Polygon[] obstacles = info.getTrack().getObstacles();
	public ArrayList<Node> qNodes;
	public ArrayList<Node> fNodes;
	public Node route;
	public Node rescueNodes;
	private Node destinyNode;
	private boolean	searchingWay = true;
	ArrayList<Integer> obstacleMidPoints;// 0 = x, 1 = y, 2= x, 3 = y....
	public static ArrayList<Node> NODEPOINTS = new ArrayList<>();
	
	
	public AI_Class(Info info) {
		super(info);
		
		
	
		obstacleMidPoints = new ArrayList<>(); 
		for(Polygon p : obstacles){
			int xMid=0;
			int yMid=0;
			for(int i= 0; i< p.npoints; i++ ){
				xMid +=	p.xpoints[i];
				yMid +=	p.ypoints[i];
				//knotenPunkte.add(new Knoten(p.xpoints[i],p.ypoints[i],p));
			}
			xMid = xMid/p.npoints;
			yMid = yMid/p.npoints;
			obstacleMidPoints.add(xMid);
			obstacleMidPoints.add(yMid);
		}
		for(Polygon o : obstacles){
			//if(!o.equals(obstacles[0]) && !o.equals(obstacles[1])){
				areaOfAllObs.add(new Area(o));
				for(int i = 0; i< o.npoints; i++){
					 int i2 = ((i+1)%(o.npoints));// i+1
					 int i3 = ((i+2)%(o.npoints));// i+2
					 Vector2f  v1 = new Vector2f((o.xpoints[i2]-o.xpoints[i]),(o.ypoints[i2]-o.ypoints[i]));
					 Vector2f  v2 = new Vector2f((o.xpoints[i3]-o.xpoints[i2]),(o.ypoints[i3]-o.ypoints[i2]));
					 float angle = Vector2f.angle(v1, v2);
					if(angle < Math.PI){
						Vector2f normalV1 = new Vector2f(v1.y,-v1.x);
						Vector2f normalV2 = new Vector2f(v2.y,-v2.x);
						Vector2f normal = new Vector2f((normalV1.getX()+normalV2.getX())/2 , (normalV1.getY()+normalV2.getY())/2 ); // durchschnitt der normalen von v1 und v2
						normal.normalise();
						normal.scale(nodedifferenceToObs);
						Point neuerKnoten = new Point(o.xpoints[i2],o.ypoints[i2]);
						neuerKnoten.translate((int)normal.x, (int)normal.y);
						NODEPOINTS.add(new Node(neuerKnoten));
					}
				}
			//}
		}
		for(Node k : NODEPOINTS){
			findTheRightNeighbor(k);
			}
		for(Node k : NODEPOINTS){
			setCostsToNeighbours(k);
		}
		destinyNode = findWay(new Point((int)info.getX(),(int)info.getY()));
	}
	

	
		
	
		
		// TODO Auto-generated constructor stub
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "KeO";
	}
	
	@Override
	public DriverAction update(boolean zurueck) {
		double MeinX= info.getX();
		double MeinY= info.getY();
		if(destinyNode.getyValue()!=info.getCurrentCheckpoint().getY() && route.predecessorgerNode== null)
		{
			if(searchingWay){
				route = null;
				searchingWay =false;
				destinyNode = findWay(new Point((int)MeinX,(int)MeinY));
			}
		}
		
		if(route!=null){
			double ZielX = route.getxValue();
			double ZielY = route.getyValue();
			if(istImBereich(MeinX, MeinY,ZielX,ZielY,10)&& route.predecessorgerNode != null){
				route = route.predecessorgerNode;
			}
		
			if(info.getVelocity().length()== 0){route = rescueNodes;}
			
			distance = Point.distance(route.getxValue(),route.getyValue(),MeinX, MeinY);
			orientation = info.getOrientation();
			neededAngle = (float) Math.atan2(ZielY-MeinY,ZielX-MeinX)-orientation;
			
			neededAngle = AngleProblemFix(neededAngle);
			
			//Arrive
			if(distance < zielradi){desiredSpeed = distance*info.getMaxVelocity()/zielradi;}
			else{desiredSpeed= info.getMaxVelocity();}
			
			if(neededAngle < 0.2){desiredRotationalSpeed= (float) ((neededAngle*info.getMaxAngularVelocity())/0.2);}
			else{desiredRotationalSpeed = info.getMaxAngularVelocity();}
	
			//Matching
			rotationalAcceleration = (float) ((desiredRotationalSpeed-info.getAngularVelocity()));
			acceleration = (float) ((desiredSpeed-info.getVelocity().length())/6);
		}
	
		
		//obstacleAvoid(MeinX, MeinY);
		
		
		return new DriverAction(acceleration,rotationalAcceleration);
	}
	
	public float AngleProblemFix( float winkel){
		if(winkel>Math.PI)winkel = (float) (winkel - 2* Math.PI);
		if(winkel<-Math.PI)winkel= (float) (winkel + 2*Math.PI);
		return winkel;
	}
	
	public void obstacleAvoid( double meinX, double meinY){
		for(int mid = 4; mid< obstacleMidPoints.size(); mid = mid+2){
			orientToObsMiddle = 	(float) (orientation-Math.atan2(obstacleMidPoints.get(mid+1)-meinY,obstacleMidPoints.get(mid)-meinX));
			orientToObsMiddle = AngleProblemFix(orientToObsMiddle);
			distanceToObsMid = 	Point.distance(obstacleMidPoints.get(mid), obstacleMidPoints.get(mid+1),meinX, meinY);
			if(distanceToObsMid <140 && Math.abs(orientToObsMiddle)<1.9 ){
					rotationalAcceleration = (float) (((orientToObsMiddle*info.getMaxAngularVelocity())-info.getAngularVelocity())/0.5);
					acceleration = info.getMaxAcceleration()/2;
			}
		}
	}

	@Override
	public String getTextureResourceName() {
		// TODO Auto-generated method stub
		return "/s0550635_KeomaTrippner/car.png";
	}
	
	public Node findWay(Point myposition){
		qNodes = new ArrayList<>();
		fNodes = new ArrayList<>();
		Node destinyNode = new Node(info.getCurrentCheckpoint());
		Node startNode = new Node(myposition);
		addNode(startNode);
		addNode(destinyNode);

		startNode.currentCosts = 0;
		qNodes.add(startNode);
		
		while(!qNodes.isEmpty()){

			Node v = qNodes.remove(findSmallesCostsInList(qNodes));
			for(Node n : v.getNeighbours()){
				if(!fNodes.contains(n)){
					if(!qNodes.contains(n)){
						qNodes.add(n);
						n.currentCosts=Double.MAX_VALUE;
					}
				if((v.currentCosts+v.neighbourWayCosts.get(v.getNeighbours().indexOf(n)))< n.currentCosts){
					n.currentCosts = v.currentCosts+v.neighbourWayCosts.get(v.getNeighbours().indexOf(n));
					n.predecessorgerNode = v;
				}
				}
			}
			fNodes.add(v);
			if(fNodes.contains(destinyNode))break;
		}

		route = createRoute(destinyNode).predecessorgerNode;
		rescueNodes = route;
		searchingWay =true;

		return destinyNode;
	}
	
	public Node createRoute(Node node){
		 if (node == null || node.predecessorgerNode == null) {
	         return node;
	     }

	     Node remaining = createRoute(node.predecessorgerNode);
	     node.predecessorgerNode.predecessorgerNode = node;
	     node.predecessorgerNode = null;
	    return remaining;
	 }
		 
	

	
	public void findTheRightNeighbor(Node k){
		for(Node p : NODEPOINTS){
			if(!k.equals(p)){
			Polygon poly = new Polygon();
			poly.addPoint(k.getxValue()+27, k.getyValue()+27);
			poly.addPoint(k.getxValue()-27, k.getyValue()-27);
			poly.addPoint(p.getxValue()+27, p.getyValue()+27);
			poly.addPoint(p.getxValue()-27, p.getyValue()-27);

			Area area = new Area(poly);
					area.intersect(areaOfAllObs);
					if(area.isEmpty()){
						k.getNeighbours().add(p);}
			}
			}
	}
	
	public void setCostsToNeighbours(Node k){
		for(int i = 0; i<k.getNeighbours().size();i++){
			double kosten = Point.distance(k.getxValue(), k.getyValue(),k.getNeighbours().get(i).getxValue(), k.getNeighbours().get(i).getyValue());
			k.neighbourWayCosts.add(kosten);
		}
	}
	public void setcurrentCostsToNeighbours(Node start){
		for(Node k : qNodes){
			if(start.getNeighbours().contains(k)){
				k.currentCosts=start.neighbourWayCosts.get(start.getNeighbours().indexOf(k));
			}
		}
	}
	
	
	public int findSmallesCostsInList(ArrayList<Node> q){
		double minKosten = Double.MAX_VALUE;
		int guenstigsterIndex= -1;
		for(int k = 0; k<q.size();k++){
			if(q.get(k).currentCosts<=minKosten){
				minKosten = q.get(k).currentCosts;
				guenstigsterIndex = k;
			}
		}
		return guenstigsterIndex;
	}
	
	private void addNode(Node k){
		
		NODEPOINTS.add(k);
		for(Node p : NODEPOINTS){
			if(!k.equals(p)){
			Polygon poly = new Polygon();
			poly.addPoint(k.getxValue()+7, k.getyValue()+7);
			poly.addPoint(k.getxValue()-7, k.getyValue()-7);
			poly.addPoint(p.getxValue()+7, p.getyValue()+7);
			poly.addPoint(p.getxValue()-7, p.getyValue()-7);
			Area area = new Area(poly);
					area.intersect(areaOfAllObs);
					if(area.isEmpty()){
						double kosten = Point.distance(k.getxValue(), k.getyValue(),p.getxValue(), p.getyValue());
						k.getNeighbours().add(p);
						k.neighbourWayCosts.add(kosten);
						p.getNeighbours().add(k);
						p.neighbourWayCosts.add(kosten);
					}
			}}
	}
	
	private boolean istImBereich(double xMobile, double yMobile,double x, double y, int radius){
		if((xMobile<=x+radius) && (xMobile>= x-radius) && (yMobile<= y+radius) && (yMobile>=y-radius)){
			return true;
		} 
		return false;
	}
	
	@Override
	public void doDebugStuff() {
		
		GL11.glBegin(GL11.GL_LINES);
		for(int k = 0; k<NODEPOINTS.size();k++){ 
			for(Node p : NODEPOINTS.get(k).getNeighbours()){
				GL11.glVertex2i(NODEPOINTS.get(k).getxValue(),NODEPOINTS.get(k).getyValue());
				GL11.glVertex2i(p.getxValue(),p.getyValue());

			}
		}
		GL11.glEnd();
		
		//System.out.println(distanceToObsMid);
		//System.out.println(orientZuObsMid);

		
	}
	
}
