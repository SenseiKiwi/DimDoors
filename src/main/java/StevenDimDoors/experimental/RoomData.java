package StevenDimDoors.experimental;

import java.util.ArrayList;

public class RoomData
{
	/* Implementation Note:
	 * Plans for links between rooms are stored in lists rather than a graph,
	 * even though they duplicate some graph functionality, because there are
	 * relatively few of them compared to the number of rooms. Moreover, some
	 * links don't even have destinations because they're intended to lead to
	 * other dungeons.
	 */
	
	private int capacity;
	private int distance;
	private int orientation;
	private boolean decayed;
	private boolean northClosed;
	private boolean southClosed;
	private boolean eastClosed;
	private boolean westClosed;
	private boolean bottomClosed;
	private boolean topClosed;
	private PartitionNode<RoomData> partitionNode;
	private ArrayList<LinkPlan> inboundLinks;
	private ArrayList<LinkPlan> outboundLinks;
	private DirectedGraph<RoomData, DoorwayData> layout;
	private IGraphNode<RoomData, DoorwayData> layoutNode;
	
	public RoomData(PartitionNode<RoomData> partitionNode)
	{
		this.partitionNode = partitionNode;
		this.inboundLinks = new ArrayList<LinkPlan>();
		this.outboundLinks = new ArrayList<LinkPlan>();
		this.layoutNode = null;
		this.layout = null;
		this.distance = -1;
		this.capacity = -1;
		this.decayed = false;
		this.northClosed = false;
		this.southClosed = false;
		this.eastClosed = false;
		this.westClosed = false;
		this.topClosed = false;
		this.bottomClosed = false;
		partitionNode.setData(this);
	}
	
	public PartitionNode<RoomData> getPartitionNode()
	{
		return this.partitionNode;
	}
	
	public IGraphNode<RoomData, DoorwayData> getLayoutNode()
	{
		return this.layoutNode;
	}
	
	@SuppressWarnings("hiding")
	public void addToLayout(DirectedGraph<RoomData, DoorwayData> layout)
	{
		this.layout = layout;
		this.layoutNode = layout.addNode(this);
	}
	
	public boolean isDecayed()
	{
		return decayed;
	}
	
	public void setDecayed(boolean value)
	{
		this.decayed = value;
	}
	
	public ArrayList<LinkPlan> getInboundLinks()
	{
		return this.inboundLinks;
	}
	
	public ArrayList<LinkPlan> getOutboundLinks()
	{
		return this.outboundLinks;
	}
	
	public int getDistance()
	{
		return distance;
	}
	
	public void setDistance(int value)
	{
		distance = value;
	}
	
	public void remove()
	{
		// Remove the room from the partition tree and from the layout graph.
		// Also remove any ancestors that become leaf nodes.
		PartitionNode<RoomData> parent;
		PartitionNode<RoomData> current = partitionNode;
		while (current != null && current.isLeaf())
		{
			parent = current.parent();
			current.remove();
			current = parent;
		}
		
		// Remove the room from the layout graph
		layout.removeNode(layoutNode);
		
		// Remove any links
		while (!inboundLinks.isEmpty())
			inboundLinks.get(inboundLinks.size() - 1).remove();

		while (!outboundLinks.isEmpty())
			outboundLinks.get(outboundLinks.size() - 1).remove();
		
		// Wipe the room's data, as a precaution
		layout = null;
		partitionNode = null;
		inboundLinks = null;
		outboundLinks = null;
	}
	
	public int getMaxDoorCapacity()
	{
		return capacity;
	}
	
	public void setMaxDoorCapacity(int value)
	{
		this.capacity = value;
	}
	
	public int getRemainingDoorCapacity()
	{
		return (capacity - outboundLinks.size());
	}
	
	public boolean isProtected()
	{
		return !inboundLinks.isEmpty() || !outboundLinks.isEmpty();
	}
	
	public boolean hasDoors()
	{
		return !outboundLinks.isEmpty();
	}
	
	public void cacheSideFlags()
	{
		northClosed = true;
		southClosed = true;
		eastClosed = true;
		westClosed = true;
		topClosed = true;
		bottomClosed = true;
		for (IEdge<RoomData, DoorwayData> passage : layoutNode.inbound())
		{
			switch (passage.data().axis())
			{
			case DoorwayData.X_AXIS:
				westClosed = false;
				break;
			case DoorwayData.Z_AXIS:
				northClosed = false;
				break;
			case DoorwayData.Y_AXIS:
				bottomClosed = false;
				break;
			}
		}
		for (IEdge<RoomData, DoorwayData> passage : layoutNode.outbound())
		{
			switch (passage.data().axis())
			{
			case DoorwayData.X_AXIS:
				eastClosed = false;
				break;
			case DoorwayData.Z_AXIS:
				southClosed = false;
				break;
			case DoorwayData.Y_AXIS:
				topClosed = false;
				break;
			}
		}
	}
	
	public boolean isNorthSideClosed()
	{
		return northClosed;
	}
	
	public boolean isSouthSideClosed()
	{
		return southClosed;
	}
	
	public boolean isEastSideClosed()
	{
		return eastClosed;
	}
	
	public boolean isWestSideClosed()
	{
		return westClosed;
	}
	
	public boolean isTopSideClosed()
	{
		return topClosed;
	}
	
	public boolean isBottomSideClosed()
	{
		return bottomClosed;
	}
	
	public int getOrientation()
	{
		return orientation;
	}
	
	public void setOrientation(int value)
	{
		this.orientation = value;
	}
}
