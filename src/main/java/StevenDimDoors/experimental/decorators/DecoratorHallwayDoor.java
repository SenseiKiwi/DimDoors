package StevenDimDoors.experimental.decorators;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import StevenDimDoors.experimental.PartitionNode;
import StevenDimDoors.experimental.RoomData;
import StevenDimDoors.mod_pocketDim.config.DDProperties;

public class DecoratorHallwayDoor extends DecoratorDoors
{
	@Override
	public int getDoorCapacity(RoomData room)
	{
		PartitionNode<RoomData> partition = room.getPartitionNode();
		int width = partition.width();
		int length = partition.length();
		if (width == 3 && length > 3 && room.isEastSideClosed() && room.isWestSideClosed() &&
				(room.isNorthSideClosed() || room.isSouthSideClosed()))
		{
			return 1;
		}
		if (length == 3 && width > 3 && room.isNorthSideClosed() && room.isSouthSideClosed() &&
				(room.isEastSideClosed() || room.isWestSideClosed()))
		{
			return 1;
		}
		return 0;
	}

	@Override
	protected void decorateInternal(RoomData room, World world, Random random, DDProperties properties)
	{
		// Determine which end of the corridor can hold the door without blockin the entrance
		PartitionNode<RoomData> partition = room.getPartitionNode();
		int x = partition.minCorner().getX() + 1;
		int y = partition.minCorner().getY() + 1;
		int z = partition.minCorner().getZ() + 1;
		int orientation;
		if (partition.width() == 3)
		{
			if (room.isSouthSideClosed())
			{
				z += partition.length() - 3;
				orientation = DOOR_FACING_NEGATIVE_Z;
			}
			else
			{
				orientation = DOOR_FACING_POSITIVE_Z;
			}
		}
		else
		{
			if (room.isEastSideClosed())
			{
				x += partition.width() - 3;
				orientation = DOOR_FACING_NEGATIVE_X;
			}
			else
			{
				orientation = DOOR_FACING_POSITIVE_X;
			}
		}
		// Set a block above the door and the door itself
		world.setBlock(x, y + 2, z, Block.stoneBrick.blockID, 3, 0);
		super.placeDimensionalDoor(world, x, y, z, orientation, room.getOutboundLinks().get(0));
	}

}
