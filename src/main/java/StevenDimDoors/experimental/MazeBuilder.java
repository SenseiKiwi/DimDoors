package StevenDimDoors.experimental;

import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import StevenDimDoors.experimental.decorators.DecoratorBase;
import StevenDimDoors.experimental.decorators.DecoratorCentralDoor;
import StevenDimDoors.experimental.decorators.DecoratorDoors;
import StevenDimDoors.experimental.decorators.DecoratorFinder;
import StevenDimDoors.experimental.decorators.DecoratorHallwayDoor;
import StevenDimDoors.experimental.decorators.DecoratorPillarDoors;
import StevenDimDoors.experimental.decorators.DecoratorWallDoors;
import StevenDimDoors.mod_pocketDim.Point3D;
import StevenDimDoors.mod_pocketDim.config.DDProperties;
import StevenDimDoors.mod_pocketDim.core.DimLink;
import StevenDimDoors.mod_pocketDim.core.LinkTypes;
import StevenDimDoors.mod_pocketDim.core.NewDimData;
import StevenDimDoors.mod_pocketDim.core.PocketManager;

public class MazeBuilder
{
	private static final int POCKET_WALL_GAP = 4;
	private static final int DECORATION_CHANCE = 1;
	private static final int MAX_DECORATION_CHANCE = 3;
	
	private MazeBuilder() { }
	
	public static void generate(World world, int x, int y, int z, Random random, DDProperties properties)
	{
		ArrayList<DecoratorDoors> doorSetters = new ArrayList<DecoratorDoors>();
		doorSetters.add(new DecoratorCentralDoor());
		doorSetters.add(new DecoratorPillarDoors());
		doorSetters.add(new DecoratorWallDoors());
		doorSetters.add(new DecoratorHallwayDoor());

		// ISSUE FOR LATER: The room needs to be shifted so as to be centered on its entrance
		/*int trials = 10000;
		long average = 0;
		long timing = 0;
		long min = Integer.MAX_VALUE;
		long max = Integer.MIN_VALUE;
		for (int k = 0; k < trials; k++)
		{
			timing = System.nanoTime();
			MazeDesigner.generate(random, doorSetters);
			timing = System.nanoTime() - timing;
			average += timing;
			min = Math.min(min, timing);
			max = Math.max(max, timing);
		}
		average /= trials;
		System.out.println("MIN DESIGN TIME: " + (min / 1000000) + " ms");
		System.out.println("MAX DESIGN TIME: " + (max / 1000000) + " ms");
		System.out.println("AVERAGE DESIGN TIME: " + (average / 1000000) + " ms");*/
		
		// Produce a design
		MazeDesign design = MazeDesigner.generate(random, doorSetters);
		
		// Compute an offset for the design and translate it to its final coordinates
		BoundingBox bounds = design.getBounds();
		Point3D offset = new Point3D(x - bounds.width() / 2, y - bounds.height() - 1, z - bounds.length() / 2);
		offset.subtract(bounds.minCorner());
		design.translate(offset);
		
		// Prepare a decay operation - used by some of the building functions
		SphereDecayOperation decay = new SphereDecayOperation(random, 0, 0, Block.stoneBrick.blockID, 2);
		
		// Build the maze
		buildRooms(design.getLayout(), world);
		carveDoorways(design.getLayout(), world, decay, random);
		applyRandomDestruction(design, world, decay, random);
		decorateRooms(design.getLayout(), world, random, properties);
		buildPocketWalls(bounds, world, properties);
	}
	
	private static void applyRandomDestruction(MazeDesign design, World world,
			SphereDecayOperation decay, Random random)
	{
		final int DECAY_BOX_SIZE = 7;
		final int DECAY_OPERATIONS = 4 + random.nextInt(4);
		final int DECAY_ATTEMPTS = 20;
		
		int x, y, z;
		int successes = 0;
		int attempts = 0;
		BoundingBox bounds = design.getBounds();
		Point3D corner = bounds.minCorner();
		PartitionNode<RoomData> root = design.getRootPartition();
		
		for (; successes < DECAY_OPERATIONS && attempts < DECAY_ATTEMPTS; attempts++)
		{
			// Select the coordinates at which to apply the decay operation
			x = random.nextInt(bounds.width()) - DECAY_BOX_SIZE / 2 + corner.getX();
			y = random.nextInt(bounds.height()) - DECAY_BOX_SIZE / 2 + corner.getY();
			z = random.nextInt(bounds.length()) - DECAY_BOX_SIZE / 2 + corner.getZ();
			
			// Check that the decay operation would not impact any protected areas
			// and mark the affected areas as decayed
			if (markDecayArea(x, y, z, DECAY_BOX_SIZE, root))
			{
				// Apply decay
				decay.apply(world, x, y, z, DECAY_BOX_SIZE, DECAY_BOX_SIZE, DECAY_BOX_SIZE);
				successes++;
			}
		}
	}
	
	private static boolean markDecayArea(int x, int y, int z, int DECAY_BOX_SIZE, PartitionNode<RoomData> root)
	{
		// Check if a given PartitionNode<RoomData> intersects the decay area. If it's a leaf, then check
		// if it's protected or not. Otherwise, check its children. The specific area is valid
		// if and only if there are no protected rooms and at least one (unprotected) room in it.
		// Also list the unprotected rooms to mark them if the decay operation will proceed.
		
		RoomData room;
		PartitionNode<RoomData> partition;
		ArrayList<RoomData> targets = new ArrayList<RoomData>();
		Stack<PartitionNode<RoomData>> nodes = new Stack<PartitionNode<RoomData>>();
		BoundingBox decayBounds = new BoundingBox(x, y, z, DECAY_BOX_SIZE, DECAY_BOX_SIZE, DECAY_BOX_SIZE);
		
		// Use depth-first search to explore all intersecting partitions
		nodes.push(root);
		while (!nodes.isEmpty())
		{
			partition = nodes.pop();
			if (decayBounds.intersects(partition))
			{
				if (partition.isLeaf())
				{
					room = partition.getData();
					if (room.isProtected())
						return false;
					targets.add(room);
				}
				else
				{
					if (partition.leftChild() != null)
						nodes.push(partition.leftChild());
					if (partition.rightChild() != null)
						nodes.push(partition.rightChild());
				}
			}
		}
		// If execution has reached this point, then there were no protected rooms.
		// Mark all intersecting rooms as decayed.
		for (RoomData target : targets)
		{
			target.setDecayed(true);
		}
		return !targets.isEmpty();
	}

	private static void buildRooms(DirectedGraph<RoomData, DoorwayData> layout, World world)
	{
		for (IGraphNode<RoomData, DoorwayData> node : layout.nodes())
		{
			PartitionNode<RoomData> room = node.data().getPartitionNode();
			buildBox(world, room.minCorner(), room.maxCorner(), Block.stoneBrick.blockID, 0);
		}
	}
	
	private static void decorateRooms(DirectedGraph<RoomData, DoorwayData> layout,
			World world, Random random, DDProperties properties)
	{
		RoomData room;
		DecoratorBase decorator;
		ArrayList<LinkPlan> links = new ArrayList<LinkPlan>();
		
		// Iterate over all rooms and apply decorators
		for (IGraphNode<RoomData, DoorwayData> node : layout.nodes())
		{
			room = node.data();
			links.addAll(room.getOutboundLinks());
			// Protected rooms must be decorated because they have links.
			// Otherwise, choose randomly whether to decorate.
			if (room.isProtected() || random.nextInt(MAX_DECORATION_CHANCE) < DECORATION_CHANCE)
			{
				decorator = DecoratorFinder.find(room, random);
				if (decorator != null)
				{
					decorator.decorate(room, world, random, properties);
				}
			}
		}
		
		// Iterate over all link plans and place links in the world
		NewDimData dimension = PocketManager.getDimensionData(world);
		for (LinkPlan plan : links)
		{
			createLinkFromPlan(plan, dimension, world);
		}
	}
	
	private static void createLinkFromPlan(LinkPlan plan, NewDimData dimension, World world)
	{
		// TODO: Support entrances! Right now we'll treat them as dungeon doors for testing
		
		DimLink link;
		Point3D source;
		Point3D destination;
		int orientation;
		
		source = plan.sourcePoint();
		orientation = world.getBlockMetadata(source.getX(), source.getY(), source.getZ()) & 3;
		
		// Check the link type and set the destination accordingly
		if (plan.isInternal())
		{
			// Create a link between sections
			destination = plan.destinationPoint();
			link = dimension.createLink(source.getX(), source.getY(), source.getZ(), LinkTypes.DUNGEON, orientation);
			dimension.setDestination(link, destination.getX(), destination.getY(), destination.getZ());
		}
		else
		{
			// Create a dungeon link
			dimension.createLink(source.getX(), source.getY(), source.getZ(), LinkTypes.DUNGEON, orientation);
		}
	}
	
	private static void carveDoorways(DirectedGraph<RoomData, DoorwayData> layout, World world,
			SphereDecayOperation decay, Random random)
	{	
		char axis;
		Point3D lower;
		DoorwayData doorway;
		
		for (IGraphNode<RoomData, DoorwayData> node : layout.nodes())
		{
			for (IEdge<RoomData, DoorwayData> passage : node.outbound())
			{
				// Carve out the passage
				doorway = passage.data();
				axis = doorway.axis();
				lower = doorway.minCorner();
				carveDoorway(world, axis, lower.getX(), lower.getY(), lower.getZ(),
						doorway.width(), doorway.height(), doorway.length(),
						decay, random);
				
				// If this is a vertical passage, then mark the upper room as decayed
				if (axis == DoorwayData.Y_AXIS)
				{
					passage.tail().data().setDecayed(true);
				}
			}
		}
	}
	
	private static void carveDoorway(World world, char axis, int x, int y, int z, int width, int height,
			int length, SphereDecayOperation decay, Random random)
	{
		final int MIN_DOUBLE_DOOR_SPAN = 10;
		
		int gap;
		int rx;
		int rz;
		switch (axis)
		{
			case DoorwayData.X_AXIS:
				if (length >= MIN_DOUBLE_DOOR_SPAN)
				{
					gap = (length - 2) / 3;
					carveDoorAlongX(world, x, y + 1, z + gap);
					carveDoorAlongX(world, x, y + 1, z + length - gap - 1);
				}
				else if (length > 3)
				{
					switch (random.nextInt(3))
					{
						case 0:
							carveDoorAlongX(world, x, y + 1, z + (length - 1) / 2);
							break;
						case 1:
							carveDoorAlongX(world, x, y + 1, z + 2);
							break;
						case 2:
							carveDoorAlongX(world, x, y + 1, z + length - 3);
							break;
					}
				}
				else
				{
					carveDoorAlongX(world, x, y + 1, z + 1);
				}
				break;
			case DoorwayData.Z_AXIS:
				if (width >= MIN_DOUBLE_DOOR_SPAN)
				{
					gap = (width - 2) / 3;
					carveDoorAlongZ(world, x + gap, y + 1, z);
					carveDoorAlongZ(world, x + width - gap - 1, y + 1, z);
				}
				else if (length > 3)
				{
					switch (random.nextInt(3))
					{
						case 0:
							carveDoorAlongZ(world, x + (width - 1) / 2, y + 1, z);
							break;
						case 1:
							carveDoorAlongZ(world, x + 2, y + 1, z);
							break;
						case 2:
							carveDoorAlongZ(world, x + width - 3, y + 1, z);
							break;
					}
				}
				else
				{
					carveDoorAlongZ(world, x + 1, y + 1, z);
				}
				break;
			case DoorwayData.Y_AXIS:
				gap = Math.min(width, length) - 2;
				if (gap > 1)
				{
					if (gap > 6)
					{
						gap = 6;
					}
					rx = x + random.nextInt(width - gap - 1) + 1;
					rz = z + random.nextInt(length - gap - 1) + 1;
					carveHole(world, rx + gap / 2, y, rz + gap / 2);
					decay.apply(world, rx, y - 1, rz, gap, 4, gap);
				}
				else
				{
					carveHole(world, x + 1, y, z + 1);
				}
				break;
		}
	}
	
	private static void carveDoorAlongX(World world, int x, int y, int z)
	{
		setBlockDirectly(world, x, y, z, 0, 0);
		setBlockDirectly(world, x, y + 1, z, 0, 0);
		setBlockDirectly(world, x + 1, y, z, 0, 0);
		setBlockDirectly(world, x + 1, y + 1, z, 0, 0);
	}
	
	private static void carveDoorAlongZ(World world, int x, int y, int z)
	{
		setBlockDirectly(world, x, y, z, 0, 0);
		setBlockDirectly(world, x, y + 1, z, 0, 0);
		setBlockDirectly(world, x, y, z + 1, 0, 0);
		setBlockDirectly(world, x, y + 1, z + 1, 0, 0);
	}
	
	private static void carveHole(World world, int x, int y, int z)
	{
		setBlockDirectly(world, x, y, z, 0, 0);
		setBlockDirectly(world, x, y + 1, z, 0, 0);
	}
	
	private static void buildPocketWalls(BoundingBox bounds, World world, DDProperties properties)
	{
		// Build the inner Fabric of Reality box
		Point3D minCorner = bounds.minCorner().clone();
		Point3D maxCorner = bounds.maxCorner().clone();
		minCorner.add(-POCKET_WALL_GAP - 1, -POCKET_WALL_GAP - 1, -POCKET_WALL_GAP - 1);
		maxCorner.add(POCKET_WALL_GAP + 1, POCKET_WALL_GAP + 1, POCKET_WALL_GAP + 1);
		buildBox(world, minCorner, maxCorner, properties.FabricBlockID, 0);
		
		// Build the outer Eternal Fabric box
		minCorner.add(-1, -1, -1);
		maxCorner.add(1, 1, 1);
		buildBox(world, minCorner, maxCorner, properties.PermaFabricBlockID, 0);
	}
	
	private static void buildBox(World world, Point3D minCorner, Point3D maxCorner, int blockID, int metadata)
	{
		int minX = minCorner.getX();
		int minY = minCorner.getY();
		int minZ = minCorner.getZ();
		
		int maxX = maxCorner.getX();
		int maxY = maxCorner.getY();
		int maxZ = maxCorner.getZ();
		
		int x, y, z;
		
		for (x = minX; x <= maxX; x++)
		{
			for (z = minZ; z <= maxZ; z++)
			{
				setBlockDirectly(world, x, minY, z, blockID, metadata);
				//setBlockDirectly(world, x, maxY, z, blockID, metadata);
			}
		}
		for (x = minX; x <= maxX; x++)
		{
			for (y = minY + 1; y < maxY; y++)
			{
				setBlockDirectly(world, x, y, minZ, blockID, metadata);
				setBlockDirectly(world, x, y, maxZ, blockID, metadata);
			}
		}
		for (z = minZ + 1; z < maxZ; z++)
		{
			for (y = minY + 1; y < maxY; y++)
			{
				setBlockDirectly(world, minX, y, z, blockID, metadata);
				setBlockDirectly(world, maxX, y, z, blockID, metadata);
			}
		}
	}
	
	private static void setBlockDirectly(World world, int x, int y, int z, int blockID, int metadata)
	{
		if (blockID != 0 && Block.blocksList[blockID] == null)
		{
			return;
		}

		int cX = x >> 4;
		int cZ = z >> 4;
		int cY = y >> 4;
		Chunk chunk;

		int localX = (x % 16) < 0 ? (x % 16) + 16 : (x % 16);
		int localZ = (z % 16) < 0 ? (z % 16) + 16 : (z % 16);
		ExtendedBlockStorage extBlockStorage;

		chunk = world.getChunkFromChunkCoords(cX, cZ);
		extBlockStorage = chunk.getBlockStorageArray()[cY];
		if (extBlockStorage == null) 
		{
			extBlockStorage = new ExtendedBlockStorage(cY << 4, !world.provider.hasNoSky);
			chunk.getBlockStorageArray()[cY] = extBlockStorage;
		}
		extBlockStorage.setExtBlockID(localX, y & 15, localZ, blockID);
		extBlockStorage.setExtBlockMetadata(localX, y & 15, localZ, metadata);
		chunk.setChunkModified();
	}
}
