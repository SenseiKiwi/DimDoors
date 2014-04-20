package StevenDimDoors.experimental.decorators;

import java.util.Random;

import net.minecraft.world.World;
import StevenDimDoors.experimental.PartitionNode;
import StevenDimDoors.experimental.RoomData;
import StevenDimDoors.mod_pocketDim.Point3D;
import StevenDimDoors.mod_pocketDim.config.DDProperties;

public class DecoratorCentralDoor extends DecoratorDoors
{
	@Override
	public int getDoorCapacity(RoomData room)
	{
		PartitionNode<RoomData> partition = room.getPartitionNode();
		int width = partition.width();
		int length = partition.length();
		return ((width == 5 || width == 7) && (length == 5 || length == 7)) ? 1 : 0;
	}

	@Override
	protected void decorateInternal(RoomData room, World world, Random random, DDProperties properties)
	{
		PartitionNode<RoomData> partition = room.getPartitionNode();
		Point3D source = partition.minCorner().clone();
		source.add(partition.width() / 2, 1, partition.length() / 2);
		super.placeDimensionalDoor(world, source.getX(), source.getY(), source.getZ(), random.nextInt(4),
				room.getOutboundLinks().get(0));
	}

}
