package StevenDimDoors.experimental;

import StevenDimDoors.mod_pocketDim.Point3D;


public class MazeDesign
{
	private PartitionNode<RoomData> root;
	private DirectedGraph<RoomData, DoorwayData> layout;
	private BoundingBox bounds; // The real bounds of the design, which may be smaller than the root partition
	
	public MazeDesign(PartitionNode<RoomData> root, DirectedGraph<RoomData, DoorwayData> layout, BoundingBox bounds)
	{
		this.root = root;
		this.layout = layout;
		this.bounds = bounds;
	}

	public PartitionNode<RoomData> getRootPartition()
	{
		return root;
	}

	public DirectedGraph<RoomData, DoorwayData> getLayout()
	{
		return layout;
	}

	public BoundingBox getBounds()
	{
		return bounds;
	}

	public void translate(Point3D offset)
	{
		// Translate all rooms and doorways by a given offset
		for (IGraphNode<RoomData, DoorwayData> node : layout.nodes())
		{
			node.data().getPartitionNode().translate(offset);
			for (IEdge<RoomData, DoorwayData> edge : node.outbound())
			{
				edge.data().translate(offset);
			}
		}
		// Translate the structure bounds
		bounds.translate(offset);
	}
}
