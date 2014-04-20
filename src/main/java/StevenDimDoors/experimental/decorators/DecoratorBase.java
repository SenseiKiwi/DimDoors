package StevenDimDoors.experimental.decorators;

import java.util.Random;

import net.minecraft.world.World;
import StevenDimDoors.experimental.RoomData;
import StevenDimDoors.mod_pocketDim.config.DDProperties;

public abstract class DecoratorBase
{
	public DecoratorBase() { }
	
	public abstract boolean canDecorate(RoomData room);
	public abstract void decorate(RoomData room, World world, Random random, DDProperties properties);
}
