package StevenDimDoors.experimental.decorators;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import StevenDimDoors.experimental.PartitionNode;
import StevenDimDoors.experimental.RoomData;
import StevenDimDoors.mod_pocketDim.config.DDProperties;

public class DecoratorWallDoors extends DecoratorDoors
{
	@Override
	public int getDoorCapacity(RoomData room)
	{
		PartitionNode<RoomData> partition = room.getPartitionNode();
		int width = partition.width();
		int length = partition.length();
		if (partition.height() < 5)
		{
			return 0;
		}
		if (length >= 5 && width >= 10 && (room.isNorthSideClosed() || room.isSouthSideClosed()))
		{
			return 2;
		}
		if (width >= 5 && length >= 10 && (room.isEastSideClosed() || room.isWestSideClosed()))
		{
			return 2;
		}
		return 0;
	}

	@Override
	public void decorateInternal(RoomData room, World world, Random random, DDProperties properties)
	{
		// Check which sides are available for building
		PartitionNode<RoomData> partition = room.getPartitionNode();
		int width = partition.width();
		int length = partition.length();
		int count = 0;
		int[] directions = new int[4];
		if (length >= 5 && width >= 10 && room.isNorthSideClosed())
		{
			directions[count] = DOOR_FACING_POSITIVE_Z;
			count++;
		}
		if (length >= 5 && width >= 10 && room.isSouthSideClosed())
		{
			directions[count] = DOOR_FACING_NEGATIVE_Z;
			count++;
		}
		if (width >= 5 && length >= 10 && room.isEastSideClosed())
		{
			directions[count] = DOOR_FACING_NEGATIVE_X;
			count++;
		}
		if (width >= 5 && length >= 10 && room.isWestSideClosed())
		{
			directions[count] = DOOR_FACING_POSITIVE_X;
			count++;
		}
		
		// Decide on a random orientation and origin
		int x = partition.minCorner().getX();
		int y = partition.minCorner().getY() + 1;
		int z = partition.minCorner().getZ();
		int dx = 0;
		int dz = 0;
		int orientation = directions[random.nextInt(count)];
		switch (orientation)
		{
			case DOOR_FACING_POSITIVE_X:
				x++;
				z += (length - 10) / 3 + 2;
				dz = 1;
				break;
			case DOOR_FACING_NEGATIVE_X:
				x += width - 2;
				z += (length - 10) / 3 + 2;
				dz = 1;
				break;
			case DOOR_FACING_POSITIVE_Z:
				z++;
				x += (width - 10) / 3 + 2;
				dx = 1;
				break;
			case DOOR_FACING_NEGATIVE_Z:
				z += length - 2;
				x += (width - 10) / 3 + 2;
				dx = 1;
				break;
		}
		
		// Build the first door and the frame around it
		world.setBlock(x, y + 0, z, Block.stoneBrick.blockID, 0, 0);
		world.setBlock(x, y + 1, z, Block.stoneBrick.blockID, 0, 0);
		world.setBlock(x, y + 2, z, Block.stoneBrick.blockID, 3, 0);
		x += dx;
		z += dz;
		world.setBlock(x, y + 2, z, Block.stoneBrick.blockID, 0, 0);
		super.placeDimensionalDoor(world, x, y, z, orientation, room.getOutboundLinks().get(0));
		x += dx;
		z += dz;
		world.setBlock(x, y + 0, z, Block.stoneBrick.blockID, 0, 0);
		world.setBlock(x, y + 1, z, Block.stoneBrick.blockID, 0, 0);
		world.setBlock(x, y + 2, z, Block.stoneBrick.blockID, 3, 0);
		
		// Determine the placement of the second door
		x = partition.minCorner().getX();
		z = partition.minCorner().getZ();
		switch (orientation)
		{
			case DOOR_FACING_POSITIVE_X:
				x++;
				z += length - (length - 10) / 3 - 3;
				break;
			case DOOR_FACING_NEGATIVE_X:
				x += width - 2;
				z += length - (length - 10) / 3 - 3;
				break;
			case DOOR_FACING_POSITIVE_Z:
				z++;
				x += width - (length - 10) / 3 - 3;
				break;
			case DOOR_FACING_NEGATIVE_Z:
				z += length - 2;
				x += width - (length - 10) / 3 - 3;
				break;
		}
		
		// Build the second door and frame around it
		world.setBlock(x, y + 0, z, Block.stoneBrick.blockID, 0, 0);
		world.setBlock(x, y + 1, z, Block.stoneBrick.blockID, 0, 0);
		world.setBlock(x, y + 2, z, Block.stoneBrick.blockID, 3, 0);
		x -= dx;
		z -= dz;
		world.setBlock(x, y + 2, z, Block.stoneBrick.blockID, 0, 0);
		if (room.getOutboundLinks().size() > 1)
		{
			super.placeDimensionalDoor(world, x, y, z, orientation, room.getOutboundLinks().get(1));
		}
		x -= dx;
		z -= dz;
		world.setBlock(x, y + 0, z, Block.stoneBrick.blockID, 0, 0);
		world.setBlock(x, y + 1, z, Block.stoneBrick.blockID, 0, 0);
		world.setBlock(x, y + 2, z, Block.stoneBrick.blockID, 3, 0);
	}

}