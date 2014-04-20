package StevenDimDoors.experimental.decorators;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.item.ItemDoor;
import net.minecraft.world.World;
import StevenDimDoors.experimental.LinkPlan;
import StevenDimDoors.experimental.RoomData;
import StevenDimDoors.mod_pocketDim.Point3D;
import StevenDimDoors.mod_pocketDim.mod_pocketDim;
import StevenDimDoors.mod_pocketDim.config.DDProperties;
import StevenDimDoors.mod_pocketDim.tileentities.TileEntityDimDoor;

public abstract class DecoratorDoors extends DecoratorBase
{
	protected static final int DOOR_FACING_NEGATIVE_X = 0;
	protected static final int DOOR_FACING_NEGATIVE_Z = 1;
	protected static final int DOOR_FACING_POSITIVE_X = 2;
	protected static final int DOOR_FACING_POSITIVE_Z = 3;
	
	public DecoratorDoors() { }
	
	public abstract int getDoorCapacity(RoomData room);
	protected abstract void decorateInternal(RoomData room, World world, Random random, DDProperties properties);
	
	@Override
	public boolean canDecorate(RoomData room)
	{
		return (room.hasDoors() && room.getOutboundLinks().size() <= getDoorCapacity(room));
	}
	
	@Override
	public void decorate(RoomData room, World world, Random random, DDProperties properties)
	{
		// Do decorations first
		this.decorateInternal(room, world, random, properties);

		// Set up proper link destinations based on link sources in this room
		// Handling this here means it doesn't have to be duplicated in subclasses ^_^
		Point3D destination;
		ArrayList<LinkPlan> outlinks = room.getOutboundLinks();
		int count = outlinks.size();
		
		if (count > 1)
		{
			for (LinkPlan incoming : room.getInboundLinks())
			{
				destination = outlinks.get(random.nextInt(count)).sourcePoint();
				incoming.setDestinationPoint(destination);
			}
		}
		else
		{
			destination = outlinks.get(0).sourcePoint();
			for (LinkPlan incoming : room.getInboundLinks())
			{
				incoming.setDestinationPoint(destination);
			}
		}
	}
	
	protected static void placeDimensionalDoor(World world, int x, int y, int z, int orientation, LinkPlan link)
	{
		// Place a dimensional door but prevent it from generating a pair at its destination
		ItemDoor.placeDoorBlock(world, x, y, z, orientation, mod_pocketDim.dimensionalDoor);
		((TileEntityDimDoor) world.getBlockTileEntity(x, y + 1, z)).hasGennedPair = link.isInternal();
		link.setSourcePoint(new Point3D(x, y + 1, z));
	}
}
