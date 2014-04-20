package StevenDimDoors.experimental.decorators;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import StevenDimDoors.experimental.PartitionNode;
import StevenDimDoors.experimental.RoomData;
import StevenDimDoors.mod_pocketDim.Point3D;
import StevenDimDoors.mod_pocketDim.config.DDProperties;

public class DecoratorCorridorTrap extends DecoratorBase
{
	@Override
	public boolean canDecorate(RoomData room)
	{
		PartitionNode<RoomData> partition = room.getPartitionNode();
		int width = partition.width();
		int length = partition.length();
		return !room.isProtected() && !room.isDecayed() &&
				((width == 3 && length >= 5) || (length == 3 && width >= 5));
	}

	@Override
	public void decorate(RoomData room, World world, Random random, DDProperties properties)
	{
		// FIXME: This decorator has some issues. If it's applied to a room that has no rooms underneath,
		// then the TNT is visible outside the dungeon. Also, if a doorway intersects the trap, then one of
		// the TNT blocks will be exposed.
		
		// The aim is to place a pressure plate along a 1-wide corridor and place TNT along its sides,
		// underneath the walls so that players can't see the TNT.
		PartitionNode<RoomData> partition = room.getPartitionNode();
		Point3D corner = partition.minCorner();
		int x, z, k;
		int y = corner.getY();
		int width = partition.width();
		int length = partition.length();

		if (width == 3)
		{
			// Build trap along the z-axis
			z = random.nextInt(length - 4) + corner.getZ() + 2;
			x = corner.getX() + 1;
			for (k = -2; k <= 2; k++)
			{
				world.setBlock(x - 1, y, z + k, Block.tnt.blockID, 1, 0);
				world.setBlock(x + 1, y, z + k, Block.tnt.blockID, 1, 0);
			}
		}
		else
		{
			// Build trap along the x-axis
			x = random.nextInt(width - 4) + corner.getX() + 2;
			z = corner.getZ() + 1;
			for (k = -2; k <= 2; k++)
			{
				world.setBlock(x + k, y, z - 1, Block.tnt.blockID, 1, 0);
				world.setBlock(x + k, y, z + 1, Block.tnt.blockID, 1, 0);
			}
		}
		world.setBlock(x, y + 1, z, Block.pressurePlateStone.blockID);
	}

}
