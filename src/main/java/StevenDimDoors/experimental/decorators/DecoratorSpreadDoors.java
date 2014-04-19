package StevenDimDoors.experimental.decorators;

import java.util.Random;

import net.minecraft.world.World;
import StevenDimDoors.experimental.RoomData;
import StevenDimDoors.mod_pocketDim.config.DDProperties;

public class DecoratorSpreadDoors extends BaseDecorator
{
	@Override
	public boolean canDecorate(RoomData room)
	{
		return false;
	}

	@Override
	public void decorate(RoomData room, World world, Random random, DDProperties properties)
	{
		
	}

}