package StevenDimDoors.mod_pocketDim.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

public class WeightedRandom
{
	private WeightedRandom() { }
	
    public static int getTotalWeight(Collection<? extends IWeightedItem> items)
    {
        int total = 0;
        for (IWeightedItem item : items)
        {
        	total += item.getWeight();
        }
        return total;
    }

    public static <T extends IWeightedItem> T getRandomItem(Random random, Collection<T> items, int totalWeight)
    {
        if (totalWeight <= 0)
        {
            throw new IllegalArgumentException("totalWeight must be positive.");
        }

        T item;
        int index = random.nextInt(totalWeight);
        Iterator<T> iterator = items.iterator();
        
        while (iterator.hasNext())
        {
        	item = iterator.next();
        	index -= item.getWeight();
        	if (index < 0)
        	{
        		return item;
        	}
        }
        return null;
    }

    public static <T extends IWeightedItem> T getRandomItem(Random random, Collection<T> items)
    {
        return getRandomItem(random, items, getTotalWeight(items));
    }
}
