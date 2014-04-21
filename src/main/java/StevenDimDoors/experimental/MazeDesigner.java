package StevenDimDoors.experimental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

import net.minecraft.util.MathHelper;
import StevenDimDoors.experimental.decorators.DecoratorDoors;
import StevenDimDoors.mod_pocketDim.Point3D;
import StevenDimDoors.mod_pocketDim.util.WeightedContainer;
import StevenDimDoors.mod_pocketDim.util.WeightedRandom;

public class MazeDesigner
{
	private static final int MAZE_WIDTH = 34;
	private static final int MAZE_LENGTH = 34;
	private static final int MAZE_HEIGHT = 20;
	private static final int MIN_HEIGHT = 4;
	private static final int MIN_SIDE = 3;
	private static final int SPLIT_COUNT = 9;
	
	private MazeDesigner() { }
	
	public static MazeDesign generate(Random random, Iterable<? extends DecoratorDoors> decorators)
	{
		final int MAX_ATTEMPTS = 3;
		
		PartitionNode<RoomData> root = null;
		ArrayList<RoomData> rooms = null;
		DirectedGraph<RoomData, DoorwayData> layout = null;
		ArrayList<ArrayList<RoomData>> groups = null;
		ArrayList<SectionData> sections = null;
		RoomData entrance = null;
		BoundingBox bounds = null;
		
		boolean ready = false;
		for (int attempts = 0; !ready && attempts < MAX_ATTEMPTS; attempts++)
		{
			root = null;
			rooms = null;
			layout = null;
			sections = null;
			
			// Construct a random binary space partitioning of our maze volume
			root = partitionRooms(MAZE_WIDTH, MAZE_HEIGHT, MAZE_LENGTH, SPLIT_COUNT, random);
	
			// Attach rooms to all the leaf nodes of the partition tree
			rooms = new ArrayList<RoomData>(1 << SPLIT_COUNT);
			attachRooms(root, rooms);
			
			// Shuffle the list of rooms so that they're not listed in any ordered way in the room graph
			// This is the only convenient way of randomizing the maze sections generated later
			Collections.shuffle(rooms, random);
			
			// Construct an adjacency graph of the rooms we've carved out. Two rooms are
			// considered adjacent if and only if a doorway could connect them. Their
			// common boundary must be large enough for a doorway.
			layout = createRoomGraph(root, rooms, random);
			
			// Cut out random subgraphs from the adjacency graph
			groups = createMazeSections(layout, random);

			// Remove unnecessary passages through floors/ceilings and some from the walls
			for (ArrayList<RoomData> group : groups)
			{
				pruneDoorways(group, layout, random);
			}
			
			// Set the max door capacity for each room
			setDoorCapacities(layout, decorators);
			
			// List all sections and remove any with insufficient door capacity
			sections = pruneSections(groups, layout);
			ready = !sections.isEmpty();
		}
		if (!ready)
		{
			throw new IllegalStateException("Failed to generate a maze after several attempts.");
		}
		
		// Set up the placement of dimensional doors within the maze
		entrance = createMazeLinks(sections, random);
		
		// Calculate the orientations and distances of all rooms
		setGlobalDistances(entrance, layout);
		
		// Calculate bounding box
		bounds = calculateBounds(layout);
		
		return new MazeDesign(root, layout, bounds);
	}

	private static void attachRooms(PartitionNode<RoomData> node, ArrayList<RoomData> partitions)
	{
		if (node.isLeaf())
		{
			partitions.add(new RoomData(node));
		}
		else
		{
			attachRooms(node.leftChild(), partitions);
			attachRooms(node.rightChild(), partitions);
		}
	}
	
	private static PartitionNode<RoomData> partitionRooms(int width, int height, int length, int maxLevels, Random random)
	{
		PartitionNode<RoomData> root = new PartitionNode<RoomData>(width, height, length);
		splitByRandomX(root, maxLevels, random);
		return root;
	}
	
	private static void splitByRandomX(PartitionNode<RoomData> node, int levels, Random random)
	{
		if (node.width() >= 2 * MIN_SIDE)
		{
			node.splitByX(MathHelper.getRandomIntegerInRange(random,
					node.minCorner().getX() + MIN_SIDE, node.maxCorner().getX() - MIN_SIDE + 1));

			if (levels > 1)
			{
				splitByRandomZ(node.leftChild(), levels - 1, random);
				splitByRandomZ(node.rightChild(), levels - 1, random);
			}
		}
		else if (levels > 1)
		{
			splitByRandomZ(node, levels - 1, random);
		}
	}
	
	private static void splitByRandomZ(PartitionNode<RoomData> node, int levels, Random random)
	{
		if (node.length() >= 2 * MIN_SIDE)
		{
			node.splitByZ(MathHelper.getRandomIntegerInRange(random,
					node.minCorner().getZ() + MIN_SIDE, node.maxCorner().getZ() - MIN_SIDE + 1));

			if (levels > 1)
			{
				splitByRandomY(node.leftChild(), levels - 1, random);
				splitByRandomY(node.rightChild(), levels - 1, random);
			}
		}
		else if (levels > 1)
		{
			splitByRandomY(node, levels - 1, random);
		}
	}
	
	private static void splitByRandomY(PartitionNode<RoomData> node, int levels, Random random)
	{
		if (node.height() >= 2 * MIN_HEIGHT)
		{
			node.splitByY(MathHelper.getRandomIntegerInRange(random,
					node.minCorner().getY() + MIN_HEIGHT, node.maxCorner().getY() - MIN_HEIGHT + 1));

			if (levels > 1)
			{
				splitByRandomX(node.leftChild(), levels - 1, random);
				splitByRandomX(node.rightChild(), levels - 1, random);
			}
		}
		else if (levels > 1)
		{
			splitByRandomX(node, levels - 1, random);
		}
	}
	
	private static DirectedGraph<RoomData, DoorwayData> createRoomGraph(PartitionNode<RoomData> root, ArrayList<RoomData> rooms, Random random)
	{
		DirectedGraph<RoomData, DoorwayData> layout = new DirectedGraph<RoomData, DoorwayData>();
		
		// Add all rooms to a graph
		for (RoomData room : rooms)
		{
			room.addToLayout(layout);
		}
		// Add edges for each room
		for (IGraphNode<RoomData, DoorwayData> node : layout.nodes())
		{
			findDoorways(node.data(), root, layout);
		}
		return layout;
	}
	
	private static void findDoorways(RoomData room, PartitionNode<RoomData> root,
			DirectedGraph<RoomData, DoorwayData> layout)
	{
		// This function finds rooms adjacent to a specified room that could be connected
		// to it through a doorway. Edges are added to the room graph to denote rooms that
		// could be connected. The areas of their common bounds that could be carved
		// out for a passage are stored in the edges.
		
		// Three directions have to be checked: up, forward, and right. The other three
		// directions (down, back, left) aren't checked because other nodes will cover them.
		// That is, down for this room is up for some other room, if it exists. Also, rooms
		// are guaranteed to have at least one doorway to another room, because the minimum
		// dimensions to which a room can be partitioned still allow passages along all
		// its sides. A room's sibling in the partition tree is guaranteed to share a side
		// through which a doorway could exist. Similar arguments guarantee the existence
		// of passages such that the whole set of rooms is a connected graph - in other words,
		// there will always be a way to walk from any room to any other room.
		
		boolean[][] detected;
		PartitionNode<RoomData> adjacent;
		
		int a, b, c;
		int p, q, r;
		int minXI, minYI, minZI;
		int maxXI, maxYI, maxZI;
		Point3D otherMin;
		Point3D otherMax;
		DoorwayData doorway;
		
		PartitionNode<RoomData> partition = room.getPartitionNode();
		Point3D minCorner = partition.minCorner();
		Point3D maxCorner = partition.maxCorner();
		
		int minX = minCorner.getX();
		int minY = minCorner.getY();
		int minZ = minCorner.getZ();
		
		int maxX = maxCorner.getX();
		int maxY = maxCorner.getY();
		int maxZ = maxCorner.getZ();
		
		int width = partition.width();
		int height = partition.height();
		int length = partition.length();
		
		if (maxZ < root.maxCorner().getZ())
		{
			// Check for adjacent rooms along the XY plane
			detected = new boolean[width][height];
			for (a = 0; a < width; a++)
			{
				for (b = 0; b < height; b++)
				{
					if (!detected[a][b])
					{
						adjacent = root.findPoint(minX + a, minY + b, maxZ + 1);
						if (adjacent != null)
						{
							// Compute the dimensions available for a doorway
							otherMin = adjacent.minCorner();
							otherMax = adjacent.maxCorner();
							minXI = Math.max(minX, otherMin.getX());
							maxXI = Math.min(maxX, otherMax.getX());
							minYI = Math.max(minY, otherMin.getY());
							maxYI = Math.min(maxY, otherMax.getY());
							
							for (p = 0; p <= maxXI - minXI; p++)
							{
								for (q = 0; q <= maxYI - minYI; q++)
								{
									detected[p + a][q + b] = true;
								}	
							}
							// Check if we meet the minimum dimensions needed for a doorway
							if (maxXI - minXI + 1 >= MIN_SIDE && maxYI - minYI + 1 >= MIN_HEIGHT)
							{
								otherMin = new Point3D(minXI, minYI, maxZ);
								otherMax = new Point3D(maxXI, maxYI, maxZ + 1);
								doorway = new DoorwayData(otherMin, otherMax, DoorwayData.Z_AXIS);
								layout.addEdge(room.getLayoutNode(), adjacent.getData().getLayoutNode(), doorway);
							}
						}
						else
						{
							detected[a][b] = true;
						}
					}
				}
			}
		}
		
		
		if (maxX < root.maxCorner().getX())
		{
			// Check for adjacent rooms along the YZ plane
			detected = new boolean[height][length];
			for (b = 0; b < height; b++)
			{
				for (c = 0; c < length; c++)
				{
					if (!detected[b][c])
					{
						adjacent = root.findPoint(maxX + 1, minY + b, minZ + c);
						if (adjacent != null)
						{
							// Compute the dimensions available for a doorway
							otherMin = adjacent.minCorner();
							otherMax = adjacent.maxCorner();
							minYI = Math.max(minY, otherMin.getY());
							maxYI = Math.min(maxY, otherMax.getY());
							minZI = Math.max(minZ, otherMin.getZ());
							maxZI = Math.min(maxZ, otherMax.getZ());
							
							for (q = 0; q <= maxYI - minYI; q++)
							{
								for (r = 0; r <= maxZI - minZI; r++)
								{
									detected[q + b][r + c] = true;
								}	
							}
							// Check if we meet the minimum dimensions needed for a doorway
							if (maxYI - minYI + 1 >= MIN_HEIGHT && maxZI - minZI + 1 >= MIN_SIDE)
							{
								otherMin = new Point3D(maxX, minYI, minZI);
								otherMax = new Point3D(maxX + 1, maxYI, maxZI);
								doorway = new DoorwayData(otherMin, otherMax, DoorwayData.X_AXIS);
								layout.addEdge(room.getLayoutNode(), adjacent.getData().getLayoutNode(), doorway);
							}
						}
						else
						{
							detected[b][c] = true;
						}
					}
				}
			}
		}
		
		
		if (maxY < root.maxCorner().getY())
		{
			// Check for adjacent rooms along the XZ plane
			detected = new boolean[width][length];
			for (a = 0; a < width; a++)
			{
				for (c = 0; c < length; c++)
				{
					if (!detected[a][c])
					{
						adjacent = root.findPoint(minX + a, maxY + 1, minZ + c);
						if (adjacent != null)
						{
							// Compute the dimensions available for a doorway
							otherMin = adjacent.minCorner();
							otherMax = adjacent.maxCorner();
							minXI = Math.max(minX, otherMin.getX());
							maxXI = Math.min(maxX, otherMax.getX());
							minZI = Math.max(minZ, otherMin.getZ());
							maxZI = Math.min(maxZ, otherMax.getZ());
							
							for (p = 0; p <= maxXI - minXI; p++)
							{
								for (r = 0; r <= maxZI - minZI; r++)
								{
									detected[p + a][r + c] = true;
								}	
							}
							// Check if we meet the minimum dimensions needed for a doorway
							if (maxXI - minXI + 1 >= MIN_SIDE && maxZI - minZI + 1 >= MIN_SIDE)
							{
								otherMin = new Point3D(minXI, maxY, minZI);
								otherMax = new Point3D(maxXI, maxY + 1, maxZI);
								doorway = new DoorwayData(otherMin, otherMax, DoorwayData.Y_AXIS);
								layout.addEdge(room.getLayoutNode(), adjacent.getData().getLayoutNode(), doorway);
							}
						}
						else
						{
							detected[a][c] = true;
						}
					}
				}
			}
		}
		
		//Done!
	}
	
	private static ArrayList<ArrayList<RoomData>> createMazeSections(DirectedGraph<RoomData, DoorwayData> layout, Random random)
	{
		// The randomness of the sections generated here hinges on
		// the nodes in the graph being in a random order. We assume
		// that was handled in a previous step!
		
		// We split the maze into sections by choosing core rooms and removing
		// rooms that are a certain number of doorways away.
		
		final int MIN_INCLUSION_DISTANCE = 2;
		final int MAX_INCLUSION_DISTANCE = 3;
		final int MIN_SECTION_ROOMS = 5;
		
		int nextDistance;
		int inclusionDistance;
		RoomData room;
		RoomData neighbor;
		ArrayList<RoomData> section;
		IGraphNode<RoomData, DoorwayData> roomNode;
		
		Queue<RoomData> ordering = new LinkedList<RoomData>();
		ArrayList<ArrayList<RoomData>> sections = new ArrayList<ArrayList<RoomData>>();
		ArrayList<IGraphNode<RoomData, DoorwayData>> nodes = new ArrayList<IGraphNode<RoomData, DoorwayData>>(layout.nodeCount());
		
		// List all graph nodes so that we can iterate over this list instead
		// of using the graph's iterator. That avoids the risk of breaking
		// the graph's iterator during removals.
		for (IGraphNode<RoomData, DoorwayData> node : layout.nodes())
		{
			nodes.add(node);
		}
		
		// Repeatedly generate sections until all nodes have been visited
		for (IGraphNode<RoomData, DoorwayData> node : nodes)
		{
			// If this room hasn't been visited (distance = -1), then use it as the core of a new section
			// Otherwise, ignore it, since it was already processed. Also make sure to check that room
			// isn't null, which happens if the room was removed previously.
			room = node.data();
			if (room != null && room.getDistance() < 0)
			{
				// Perform a breadth-first search to tag surrounding nodes with distances
				inclusionDistance = MathHelper.getRandomIntegerInRange(random, MIN_INCLUSION_DISTANCE, MAX_INCLUSION_DISTANCE);
				section = new ArrayList<RoomData>();
				room.setDistance(0);
				ordering.add(room);
				
				while (room != null && room.getDistance() <= inclusionDistance)
				{
					ordering.remove();
					section.add(room);
					roomNode = room.getLayoutNode();
					nextDistance = room.getDistance() + 1;
					
					// Visit neighboring rooms and assign them distances,
					// if they don't have a proper distance assigned already.
					for (IEdge<RoomData, DoorwayData> edge : roomNode.inbound())
					{
						neighbor = edge.head().data();
						if (neighbor.getDistance() < 0)
						{
							neighbor.setDistance(nextDistance);
							ordering.add(neighbor);
						}
					}
					for (IEdge<RoomData, DoorwayData> edge : roomNode.outbound())
					{
						neighbor = edge.tail().data();
						if (neighbor.getDistance() < 0)
						{
							neighbor.setDistance(nextDistance);
							ordering.add(neighbor);
						}
					}
					room = ordering.peek();
				}
				
				// The remaining rooms in the ordering are those that are at the
				// frontier of the structure. They must be removed to create
				// a gap between this section and other sections.
				while (!ordering.isEmpty())
				{
					ordering.remove().remove();
				}
				
				// Check if this section contains enough rooms and capacity for doors
				if (section.size() >= MIN_SECTION_ROOMS)
				{
					sections.add(section);
				}
				else
				{
					// Discard the whole section
					for (RoomData target : section)
					{
						target.remove();
					}
				}
			}
		}
		return sections;
	}
	
	private static void pruneDoorways(ArrayList<RoomData> group,
			DirectedGraph<RoomData, DoorwayData> layout, Random random)
	{
		// We receive a node for one of the rooms in a section of the maze
		// and we need to remove as many floor doorways as possible while
		// still allowing any room to be reachable from any other room.
		// In technical terms, we receive a node from a connected subgraph
		// and we need to remove as many Y_AXIS-type edges as possible while
		// preserving connectivity. We also want to randomly remove some of
		// the other doorways without breaking connectivity.
		
		// An efficient solution is to assign nodes to disjoint sets based
		// on their components, ignoring all Y_AXIS edges, then iterate over
		// a list of those edges and remove them if they connect two nodes
		// in the same set. Otherwise, merge their sets and keep the edge.
		// This is similar to algorithms for spanning trees. The same
		// idea applies for the other doorways, plus some randomness.
		
		// First, list all nodes in the subgraph
		IGraphNode<RoomData, DoorwayData> layoutNode;
		ArrayList<IGraphNode<RoomData, DoorwayData>> subgraph = new ArrayList<IGraphNode<RoomData, DoorwayData>>( group.size() );
		DisjointSet<IGraphNode<RoomData, DoorwayData>> components = new DisjointSet<IGraphNode<RoomData, DoorwayData>>( 2 * group.size() );
		
		for (RoomData room : group)
		{
			layoutNode = room.getLayoutNode();
			subgraph.add(layoutNode);
			components.makeSet(layoutNode);
		}
		
		// Now iterate over the list of nodes and merge their sets based on
		// being connected by X_AXIS or Z_AXIS doorways. We only have to look
		// at outbound edges since inbound edges mirror them. List any Y_AXIS
		// doorways we come across to consider removing them later, depending
		// on their impact on connectivity.
		ArrayList<IEdge<RoomData, DoorwayData>> targets =
				new ArrayList<IEdge<RoomData, DoorwayData>>();
		
		for (IGraphNode<RoomData, DoorwayData> room : subgraph)
		{
			for (IEdge<RoomData, DoorwayData> passage : room.outbound())
			{
				if (passage.data().axis() != DoorwayData.Y_AXIS)
				{
					components.mergeSets(passage.head(), passage.tail());
				}
				else
				{
					targets.add(passage);
				}
			}
		}
		
		// Shuffle the list of doorways to randomize which ones are removed
		Collections.shuffle(targets, random);
		
		// Merge sets together and remove unnecessary doorways
		for (IEdge<RoomData, DoorwayData> passage : targets)
		{
			if (!components.mergeSets(passage.head(), passage.tail()))
			{
				layout.removeEdge(passage);
			}
		}
		
		// Repeat the pruning process with X_AXIS and Z_AXIS doorways
		// In this case, unnecessary edges might be kept at random
		components.clear();
		targets.clear();
		
		for (IGraphNode<RoomData, DoorwayData> room : subgraph)
		{
			components.makeSet(room);
		}
		for (IGraphNode<RoomData, DoorwayData> room : subgraph)
		{
			for (IEdge<RoomData, DoorwayData> passage : room.outbound())
			{
				if (passage.data().axis() == DoorwayData.Y_AXIS)
				{
					components.mergeSets(passage.head(), passage.tail());
				}
				else
				{
					targets.add(passage);
				}
			}
		}
		Collections.shuffle(targets, random);
		for (IEdge<RoomData, DoorwayData> passage : targets)
		{
			if (!components.mergeSets(passage.head(), passage.tail()) && random.nextBoolean())
			{
				layout.removeEdge(passage);
			}
		}
	}
	
	private static void setDoorCapacities(DirectedGraph<RoomData, DoorwayData> layout,
			Iterable<? extends DecoratorDoors> decorators)
	{
		int capacity;
		int maxCapacity;
		RoomData room;
		for (IGraphNode<RoomData, DoorwayData> node : layout.nodes())
		{
			room = node.data();
			room.cacheSideFlags();
			maxCapacity = 0;
			
			// Only rooms with closed bottoms may have doors
			if (room.isBottomSideClosed())
			{
				for (DecoratorDoors decorator : decorators)
				{
					capacity = decorator.getDoorCapacity(room);
					if (capacity > maxCapacity)
						maxCapacity = capacity;
				}
			}
			room.setMaxDoorCapacity(maxCapacity);
		}
	}
	
	private static ArrayList<SectionData> pruneSections(ArrayList<ArrayList<RoomData>> groups,
			DirectedGraph<RoomData, DoorwayData> layout)
	{
		// For a section to be valid, it must have enough space for at least
		// two doors in rooms without floor holes, or else it'll be discarded.
		
		final int MIN_SECTION_CAPACITY = 2;
		
		SectionData section;
		ArrayList<SectionData> sections = new ArrayList<SectionData>();

		for (ArrayList<RoomData> group : groups)
		{
			section = SectionData.createFromList(group);
			if (section.capacity() >= MIN_SECTION_CAPACITY)
			{
				sections.add(section);
			}
			else
			{
				section.remove();
			}
		}
		return sections;
	}
	
	private static RoomData createMazeLinks(ArrayList<SectionData> sections, Random random)
	{
		// We have 4 objectives here...
		// 1. Place the entrance to the maze
		// 2. Place links to other dungeons
		// 3. Place internal links connecting the different sections of the maze
		// 4. Place more internal links to confuse people
		
		// We assume that all sections have a minimum capacity of 2. Time to check...
		for (SectionData section : sections)
		{
			if (section.capacity() < 2)
			{
				throw new IllegalArgumentException("All sections must have a capacity of at least 2.");
			}
		}
		
		int index;
		int count;
		int totalCapacity;
		RoomData entrance;
		SectionData selection;
		SectionData destination;
		ArrayList<SectionData> usableSections;

		// Check if there is only one section. Our concerns differ depending
		// on whether there is one or more than one.
		if (sections.size() > 1)
		{	
			// More than 1 section
			usableSections = (ArrayList<SectionData>) sections.clone();
			
			// Select the room in which to place the entrance.
			// We can safely consider all sections because createMazeSections()
			// guarantees that each one has at least the capacity for 2 doors.
			// Remove the selected section if it falls below a capacity of 2
			// since we need to leave at least 1 capacity for section linking.
			index = random.nextInt(usableSections.size());
			selection = usableSections.get(index);
			entrance = selection.createEntranceLink(random);
			if (selection.capacity() <= 1)
			{
				usableSections.remove(index);
			}
			
			// Place 3 to 4 dungeon doors in random sections
			// Remove any sections that fall under a capacity of 2.
			count = 3 + random.nextInt(2);
			totalCapacity = WeightedRandom.getTotalWeight(usableSections);
			for (; count > 0 && !usableSections.isEmpty(); count--)
			{
				index = WeightedRandom.getRandomItemIndex(random, usableSections, totalCapacity);
				selection = usableSections.get(index);
				selection.createDungeonLink(random);
				totalCapacity--;
				if (selection.capacity() == 1)
				{
					usableSections.remove(index);
					totalCapacity--;
				}
			}
			
			// The next task is to place internal links. These links must connect
			// the different maze sections to create a strongly connected graph.
			linkMazeSections(sections, random);
			
			// Add 1 to 3 extra internal links to confuse people
			usableSections.clear();
			totalCapacity = 0;
			for (SectionData section : sections)
			{
				if (section.capacity() > 0)
				{
					usableSections.add(section);
					totalCapacity += section.capacity();
				}
			}
			count = 1 + random.nextInt(3);
			for (; count > 0 && !usableSections.isEmpty(); count--)
			{
				index = WeightedRandom.getRandomItemIndex(random, usableSections, totalCapacity);
				selection = usableSections.get(index);
				destination = sections.get( random.nextInt(sections.size()) );
				selection.reserveSectionLink(destination, random);
				totalCapacity--;
				if (selection.capacity() == 0)
				{
					usableSections.remove(index);
				}
			}
			
			// Finally, make sure to process all reservations for section links.
			for (SectionData section : sections)
			{
				section.processReservedLinks(random);
			}
		}
		else
		{
			// Only 1 section
			selection = sections.get(0);
			// Place entrance door in a random room
			entrance = selection.createEntranceLink(random);
			// Place 3 to 4 dungeon doors or fewer, based on capacity
			count = Math.min(3 + random.nextInt(2), selection.capacity());
			for (; count > 0; count--)
			{
				selection.createDungeonLink(random);
			}
		}
		return entrance;
	}
	
	private static void linkMazeSections(ArrayList<SectionData> sections, Random random)
	{
		// This algorithm constructs links sections together using Dimensional Doors
		// to create a random strongly connected graph. It takes into account capacity
		// constraints as well. We assume that all sections have at least 1 door capacity.
		
		// Sanity checking
		for (SectionData section : sections)
		{
			if (section.capacity() < 1)
			{
				throw new IllegalArgumentException("All sections must have a capacity of at least 1.");
			}
		}
		
		final int EXTENSION_CHANCE = 2;
		final int MAX_EXTENSION_CHANCE = 3;
		
		int index;
		SectionData start;
		SectionData current;
		SectionData next;
		
		// Total spare capacity of the sections not in "remaining"
		int capacity;
		// Sections not in the graph
		ArrayList<SectionData> remaining = (ArrayList<SectionData>) sections.clone();
		// Sections that are part of an incomplete cycle
		ArrayList<SectionData> attached = new ArrayList<SectionData>(sections.size());
		// Sections that are part of a cycle and thus strongly connected
		ArrayList<SectionData> connected = new ArrayList<SectionData>(sections.size());
		// Sections that are part of a cycle and have spare capacity - used to start new cycles
		ArrayList<SectionData> starters = new ArrayList<SectionData>(sections.size());
		
		// Shuffle remaining to achieve randomness
		Collections.shuffle(remaining, random);
		// Remove the starting node to serve as the base of our strongly connected graph
		start = remaining.remove(remaining.size() - 1);
		starters.add(start);
		connected.add(start);
		capacity = start.capacity();
		
		// Repeat until all sections are connected
		while (!remaining.isEmpty())
		{
			// DEBUG CODE!!!
			for (SectionData section : starters)
			{
				if (section.capacity() == 0)
				{
					throw new IllegalStateException("THIS SHOULDN'T BE POSSIBLE! ;-;");
				}
			}
			// END OF DEBUG CODE!!!
			
			// Select a section from which to start a new cycle
			index = WeightedRandom.getRandomItemIndex(random, starters, capacity);
			start = starters.get(index);
			// Select the first new section in the cycle and link to it
			current = remaining.remove(remaining.size() - 1);
			attached.add(current);
			start.reserveSectionLink(current, random);
			// Add the current section's capacity to the total, but subtract two to account
			// for the link just created and for the future link from this section to another
			capacity += current.capacity() - 2;
			// Remove the starting section from starters if it has exhausted its capacity
			if (start.capacity() == 0)
			{
				starters.remove(index);
			}
			
			// Continue attaching sections to the partial cycle while there are are still sections
			// left to be added and we have no spare capacity. Or we could randomly decide to
			// continue even with spare capacity. Spare capacity means we could start a new cycle
			// safely and still achieve strong connectivity. Randomness here influences the kinds
			// of graphs we can get.
			while (!remaining.isEmpty() && (capacity == 0 ||
					random.nextInt(MAX_EXTENSION_CHANCE) < EXTENSION_CHANCE))
			{
				next = remaining.remove(remaining.size() - 1);
				attached.add(next);
				current.reserveSectionLink(next, random);
				// Account for this section's capacity, but subtract one for
				// the future link that will connect this section to another
				capacity += next.capacity() - 1;
				current = next;
			}
			next = connected.get(random.nextInt(connected.size()));
			current.reserveSectionLink(next, random);
			for (SectionData section : attached)
			{
				connected.add(section);
				if (section.capacity() > 0)
				{
					starters.add(section);
				}
			}
			attached.clear();
		}
		
		// Done! At this point, all sections are connected.
	}
	
	private static void setGlobalDistances(RoomData entrance, DirectedGraph<RoomData, DoorwayData> layout)
	{
		// This function tags all rooms in a maze with their shortest-path distances.
		// Note that different types of passages are assigned different weights here
		// to represent that some passages are harder or involve more risk than others.
		// We'll use Djiktra's algorithm to calculate the distances correctly.
		final int SIDE_PASSAGE_WEIGHT = 1;
		final int HOLE_DOWN_WEIGHT = 10;
		final int HOLE_UP_WEIGHT = 100;
		final int DOOR_WEIGHT = 1000;
		final int HUGE_WEIGHT = 1000000;
		
		// Reset all distances
		for (IGraphNode<RoomData, DoorwayData> node : layout.nodes())
		{
			node.data().setDistance(HUGE_WEIGHT);
		}
		entrance.setDistance(0);
		
		// Apply Djiktra's algorithm
		int distance;
		RoomData currentRoom;
		RoomData neighborRoom;
		WeightedContainer<RoomData> currentEntry;
		IGraphNode<RoomData, DoorwayData> currentNode;
		PriorityQueue<WeightedContainer<RoomData>> ordering = new PriorityQueue<WeightedContainer<RoomData>>();
		
		ordering.add( new WeightedContainer<RoomData>(entrance, 0) );
		while (!ordering.isEmpty())
		{
			currentEntry = ordering.remove();
			currentRoom = currentEntry.getData();
			
			// To avoid dealing with updating entries in the priority queue,
			// we simply keep inserting new entries as distances change.
			// That means we might have old entries mixed in, so make sure
			// this entry is relevant. Old entries have mismatched weights.
			if (currentEntry.getWeight() == currentRoom.getDistance())
			{
				currentNode = currentRoom.getLayoutNode();
				for (IEdge<RoomData, DoorwayData> passage : currentNode.inbound())
				{
					distance = currentRoom.getDistance();
					if (passage.data().axis() != DoorwayData.Y_AXIS)
					{
						distance += SIDE_PASSAGE_WEIGHT;
					}
					else
					{
						distance += HOLE_DOWN_WEIGHT;
					}
					neighborRoom = passage.head().data();
					if (distance < neighborRoom.getDistance())
					{
						neighborRoom.setDistance(distance);
						ordering.add(new WeightedContainer<RoomData>(neighborRoom, distance));
					}
				}
				for (IEdge<RoomData, DoorwayData> passage : currentNode.outbound())
				{
					distance = currentRoom.getDistance();
					if (passage.data().axis() != DoorwayData.Y_AXIS)
					{
						distance += SIDE_PASSAGE_WEIGHT;
					}
					else
					{
						distance += HOLE_UP_WEIGHT;
					}
					neighborRoom = passage.tail().data();
					if (distance < neighborRoom.getDistance())
					{
						neighborRoom.setDistance(distance);
						ordering.add(new WeightedContainer<RoomData>(neighborRoom, distance));
					}
				}
				for (LinkPlan link : currentRoom.getOutboundLinks())
				{
					if (link.isInternal())
					{
						distance = currentRoom.getDistance() + DOOR_WEIGHT;
						neighborRoom = link.destination();
						if (distance < neighborRoom.getDistance())
						{
							neighborRoom.setDistance(distance);
							ordering.add(new WeightedContainer<RoomData>(neighborRoom, distance));
						}
					}
				}
			}
		}
	}
	
	private static BoundingBox calculateBounds(DirectedGraph<RoomData, DoorwayData> layout)
	{
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		
		Point3D min;
		Point3D max;
		PartitionNode<RoomData> partition;
		for (IGraphNode<RoomData, DoorwayData> node : layout.nodes())
		{
			partition = node.data().getPartitionNode();
			min = partition.minCorner();
			max = partition.maxCorner();
			
			minX = Math.min(minX, min.getX());
			minY = Math.min(minY, min.getY());
			minZ = Math.min(minZ, min.getZ());
			
			maxX = Math.max(maxX, max.getX());
			maxY = Math.max(maxY, max.getY());
			maxZ = Math.max(maxZ, max.getZ());
		}
		return new BoundingBox(new Point3D(minX, minY, minZ), new Point3D(maxX, maxY, maxZ));
	}
}
