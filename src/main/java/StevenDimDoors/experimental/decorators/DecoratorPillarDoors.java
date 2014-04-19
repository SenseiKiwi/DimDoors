package StevenDimDoors.experimental.decorators;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import StevenDimDoors.experimental.LinkPlan;
import StevenDimDoors.experimental.PartitionNode;
import StevenDimDoors.experimental.RoomData;
import StevenDimDoors.mod_pocketDim.Point3D;
import StevenDimDoors.mod_pocketDim.config.DDProperties;
import StevenDimDoors.mod_pocketDim.util.Shuffler;

public class DecoratorPillarDoors extends BaseDecorator
{
	@Override
	public boolean canDecorate(RoomData room)
	{
		PartitionNode<RoomData> partition = room.getPartitionNode();
		int width = partition.width();
		int length = partition.length();
		return (room.isProtected() && width >= 7 && length >= 7 &&
			(width & 1) == 1 && (length & 1) == 1);
	}

	@Override
	public void decorate(RoomData room, World world, Random random, DDProperties properties)
	{
		// Calculate the center point around which to place doors
		PartitionNode<RoomData> partition = room.getPartitionNode();
		int centerX = partition.minCorner().getX() + partition.width() / 2;
		int centerZ = partition.minCorner().getZ() + partition.length() / 2;
		int centerY = partition.minCorner().getY() + 1;
		int dx, dz, top, orientation;
		
		// Build doors
		int[] directions = new int[] { 0, 1, 2, 3 };
		Shuffler.shuffle(directions, random);
		int count = 0;
		for (LinkPlan outgoing : room.getOutboundLinks())
		{
			dx = 0;
			dz = 0;
			orientation = directions[count];
			switch (orientation)
			{
			case DOOR_FACING_POSITIVE_X:
				dx = 1;
				break;
			case DOOR_FACING_NEGATIVE_X:
				dx = -1;
				break;
			case DOOR_FACING_POSITIVE_Z:
				dz = 1;
				break;
			case DOOR_FACING_NEGATIVE_Z:
				dz = -1;
				break;
			}
			super.placeDimensionalDoor(world, centerX + dx, centerY, centerZ + dz, orientation, outgoing.isInternal());
			outgoing.setSourcePoint(new Point3D(centerX + dx, centerY + 1, centerZ + dz));
			count++;
		}
		
		// Build a pillar that fits around the doors
		world.setBlock(centerX - 1, centerY, centerZ - 1, Block.stoneBrick.blockID, 3, 0);
		world.setBlock(centerX - 1, centerY, centerZ + 1, Block.stoneBrick.blockID, 3, 0);
		world.setBlock(centerX + 1, centerY, centerZ - 1, Block.stoneBrick.blockID, 3, 0);
		world.setBlock(centerX + 1, centerY, centerZ + 1, Block.stoneBrick.blockID, 3, 0);
		world.setBlock(centerX, centerY, centerZ, Block.stoneBrick.blockID, 0, 0);
		
		world.setBlock(centerX - 1, centerY + 1, centerZ - 1, Block.stoneBrick.blockID, 0, 0);
		world.setBlock(centerX - 1, centerY + 1, centerZ + 1, Block.stoneBrick.blockID, 0, 0);
		world.setBlock(centerX + 1, centerY + 1, centerZ - 1, Block.stoneBrick.blockID, 0, 0);
		world.setBlock(centerX + 1, centerY + 1, centerZ + 1, Block.stoneBrick.blockID, 0, 0);
		world.setBlock(centerX, centerY + 1, centerZ, Block.stoneBrick.blockID, 0, 0);
		
		// Set up destinations for inbound links
		// We will randomly select from the positions of the doors placed
		for (LinkPlan incoming : room.getInboundLinks())
		{
			dx = 0;
			dz = 0;
			switch (directions[random.nextInt(count)])
			{
			case DOOR_FACING_POSITIVE_X:
				dx = 1;
				break;
			case DOOR_FACING_NEGATIVE_X:
				dx = -1;
				break;
			case DOOR_FACING_POSITIVE_Z:
				dz = 1;
				break;
			case DOOR_FACING_NEGATIVE_Z:
				dz = -1;
				break;
			}
			incoming.setDestinationPoint(new Point3D(centerX + dx, centerY + 1, centerZ + dz));
		}
		
		// Build a pillar above the doors if there's empty space
		if (partition.height() > 4)
		{
			top = partition.maxCorner().getY() - 1;
			for (centerY += 2; centerY <= top; centerY++)
			{
				for (dx = -1; dx <= 1; dx++)
				{
					for (dz = -1; dz <= 1; dz++)
					{
						world.setBlock(centerX + dx, centerY, centerZ + dz, Block.stoneBrick.blockID, 0, 0);
					}
				}
			}
		}
	}

}