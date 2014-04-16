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
}
