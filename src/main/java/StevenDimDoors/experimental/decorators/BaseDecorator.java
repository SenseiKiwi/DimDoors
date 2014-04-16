package StevenDimDoors.experimental.decorators;

import java.util.Random;

import net.minecraft.item.ItemDoor;
import net.minecraft.world.World;
import StevenDimDoors.experimental.RoomData;
import StevenDimDoors.mod_pocketDim.Point3D;
import StevenDimDoors.mod_pocketDim.mod_pocketDim;
import StevenDimDoors.mod_pocketDim.config.DDProperties;
import StevenDimDoors.mod_pocketDim.tileentities.TileEntityDimDoor;

public abstract class BaseDecorator
{
	public BaseDecorator() { }
	
	public abstract boolean canDecorate(RoomData room);
	public abstract void decorate(RoomData room, World world, Point3D offset, Random random, DDProperties properties);
	
	public void placeDimensionalDoor(World world, int x, int y, int z, int orientation, boolean disablePair)
	{
		// Place a dimensional door but prevent it from generating a pair at its destination
		ItemDoor.placeDoorBlock(world, x, y, z, orientation, mod_pocketDim.dimensionalDoor);
		((TileEntityDimDoor) world.getBlockTileEntity(x, y + 1, z)).hasGennedPair = disablePair;
	}
}
