package StevenDimDoors.experimental.decorators;

import java.util.ArrayList;
import java.util.Random;

import StevenDimDoors.experimental.RoomData;

public class DecoratorFinder
{
	private static ArrayList<DecoratorBase> decorators = null;
	
	private DecoratorFinder() { }
	
	public static DecoratorBase find(RoomData room, Random random)
	{
		if (decorators == null)
		{
			load();
		}
		
		// Since there are only a few decorators right now, we just iterate
		// over the list and check them all. If we add a lot, we'll need to
		// switch to a more efficient approach.
		ArrayList<DecoratorBase> matches = new ArrayList<DecoratorBase>();
		for (DecoratorBase decorator : decorators)
		{
			if (decorator.canDecorate(room))
			{
				matches.add(decorator);
			}
		}
		
		if (matches.isEmpty())
		{
			return null;
		}
		return matches.get( random.nextInt(matches.size()) );
	}
	
	private static void load()
	{
		// List all the decorators we have
		decorators = new ArrayList<DecoratorBase>();
		decorators.add(new DecoratorLinkDestination());
		decorators.add(new DecoratorTorch());
		//decorators.add(new CorridorTrapDecorator());
		decorators.add(new DecoratorCentralDoor());
		decorators.add(new DecoratorPillarDoors());
		decorators.add(new DecoratorWallDoors());
		decorators.add(new DecoratorHallwayDoor());
	}
}
