package StevenDimDoors.experimental;

import java.util.ArrayList;
import java.util.Random;

import StevenDimDoors.mod_pocketDim.util.IWeightedItem;

public class SectionData implements IWeightedItem
{
	// Specifies the chance of selecting a destination from protectedRooms
	// rather than from destinationRooms (which will then become protected)
	private static final int PROTECTED_DESTINATION_CHANCE = 3;
	private static final int MAX_PROTECTED_DESTINATION_CHANCE = 4;
	
	private int capacity;
	private ArrayList<RoomData> allRooms;
	private ArrayList<RoomData> sourceRooms;
	private ArrayList<RoomData> protectedRooms;
	private ArrayList<RoomData> destinationRooms;
	private ArrayList<LinkPlan> reservations; 
	
	private SectionData(ArrayList<RoomData> allRooms, ArrayList<RoomData> sourceRooms,
			ArrayList<RoomData> destinationRooms, int capacity)
	{
		this.capacity = capacity;
		this.allRooms = allRooms;
		this.sourceRooms = sourceRooms;
		this.destinationRooms = destinationRooms;
		this.protectedRooms = new ArrayList<RoomData>();
		this.reservations = new ArrayList<LinkPlan>();
	}
	
	public static SectionData createFromList(ArrayList<RoomData> rooms)
	{
		// This code assumes that the original list of rooms
		// will not be modified externally!
		
		int capacity = 0;
		ArrayList<RoomData> sourceRooms = new ArrayList<RoomData>();
		ArrayList<RoomData> destinationRooms = new ArrayList<RoomData>();
		
		for (RoomData room : rooms)
		{
			if (room.isBottomSideClosed())
			{
				destinationRooms.add(room);
				if (room.getMaxDoorCapacity() > 0)
				{
					capacity += room.getMaxDoorCapacity();
					sourceRooms.add(room);
				}
			}
		}
		return new SectionData(rooms, sourceRooms, destinationRooms, capacity);
	}
	
	public int capacity()
	{
		return capacity;
	}
	
	@Override
	public int getWeight()
	{
		return capacity;
	}
	
	public void createEntranceLink(Random random)
	{
		int index = random.nextInt(sourceRooms.size());
		RoomData room = sourceRooms.get(index);
		LinkPlan.createEntranceLink(room);
		if (room.getRemainingDoorCapacity() == 0)
		{
			sourceRooms.remove(index);
		}
		// It's okay to check containment in this list because
		// the number of protected rooms is expected to be small
		if (!protectedRooms.contains(room))
		{
			protectedRooms.add(room);
		}
		capacity--;
	}

	public void createDungeonLink(Random random)
	{
		int index = random.nextInt(sourceRooms.size());
		RoomData room = sourceRooms.get(index);
		LinkPlan.createDungeonLink(room);
		if (room.getRemainingDoorCapacity() == 0)
		{
			sourceRooms.remove(index);
		}
		// It's okay to check containment in this list because
		// the number of protected rooms is expected to be small
		if (!protectedRooms.contains(room))
		{
			protectedRooms.add(room);
		}
		capacity--;
	}

	public void reserveSectionLink(SectionData destination, Random random)
	{
		// This method "reserves" a link by decrementing the capacity of this
		// section and assigning a source room to the link. However, assigning
		// its destination in a particular section is deferred. Why?
		
		// We favor using source rooms as destinations to cut down the number
		// of rooms that have to be marked as protected against decay effects.
		// We defer assigning a destination until after all source rooms are
		// known so that we have that information available. Otherwise,
		// destination selection would be biased toward non-source rooms and
		// rooms with dungeon doors, which are placed before section links.
		
		int index = random.nextInt(sourceRooms.size());
		RoomData room = sourceRooms.get(index);
		destination.reserveDestination(LinkPlan.createInternalLink(room));
		if (room.getRemainingDoorCapacity() == 0)
		{
			sourceRooms.remove(index);
		}
		// It's okay to check containment in this list because
		// the number of protected rooms is expected to be small
		if (!protectedRooms.contains(room))
		{
			protectedRooms.add(room);
		}
		capacity--;
	}

	public void processReservedLinks(Random random)
	{
		for (LinkPlan link : reservations)
		{
			link.setDestination( getLinkDestination(random) );
		}
		reservations.clear();
	}
	
	private void reserveDestination(LinkPlan link)
	{
		reservations.add(link);
	}
	
	private RoomData getLinkDestination(Random random)
	{
		RoomData destination;
		
		// Choose whether to select a room that is already protected or select
		// from all possible destination rooms. Note that some destination rooms
		// may also be protected rooms already.
		if (random.nextInt(MAX_PROTECTED_DESTINATION_CHANCE) < PROTECTED_DESTINATION_CHANCE)
		{
			destination = protectedRooms.get( random.nextInt(protectedRooms.size()) );
		}
		else
		{
			destination = destinationRooms.get( random.nextInt(destinationRooms.size()) );
			// It's okay to check containment in this list because
			// the number of protected rooms is expected to be small
			if (!protectedRooms.contains(destination))
			{
				protectedRooms.add(destination);
			}
		}
		return destination;
	}

	public void remove()
	{
		// Clear out everything
		for (RoomData room : allRooms)
		{
			room.remove();
		}
		allRooms.clear();
		allRooms = null;
		sourceRooms.clear();
		sourceRooms = null;
		protectedRooms.clear();
		protectedRooms = null;
		destinationRooms.clear();
		destinationRooms = null;
		reservations.clear();
		reservations = null;
		capacity = -1;
	}
}
