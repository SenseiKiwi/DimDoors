package StevenDimDoors.experimental.decorators;

import java.util.Random;

import net.minecraft.item.ItemDoor;
import net.minecraft.world.World;
import StevenDimDoors.experimental.RoomData;
import StevenDimDoors.mod_pocketDim.mod_pocketDim;
import StevenDimDoors.mod_pocketDim.config.DDProperties;
import StevenDimDoors.mod_pocketDim.tileentities.TileEntityDimDoor;

public abstract class BaseDecorator
{
	protected static final int DOOR_FACING_NEGATIVE_X = 0;
	protected static final int DOOR_FACING_NEGATIVE_Z = 1;
	protected static final int DOOR_FACING_POSITIVE_X = 2;
	protected static final int DOOR_FACING_POSITIVE_Z = 3;
	
	public BaseDecorator() { }
	
	public abstract boolean canDecorate(RoomData room);
	public abstract void decorate(RoomData room, World world, Random random, DDProperties properties);
	
	protected static void placeDimensionalDoor(World world, int x, int y, int z, int orientation, boolean isInternal)
	{
		// Place a dimensional door but prevent it from generating a pair at its destination
		ItemDoor.placeDoorBlock(world, x, y, z, orientation, mod_pocketDim.dimensionalDoor);
		((TileEntityDimDoor) world.getBlockTileEntity(x, y + 1, z)).hasGennedPair = isInternal;
	}
}
