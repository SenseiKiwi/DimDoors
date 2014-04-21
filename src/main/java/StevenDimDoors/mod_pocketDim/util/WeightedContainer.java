package StevenDimDoors.mod_pocketDim.util;

/*.
 * Implements a simple generic item container for use with WeightedRandom.
 */
public class WeightedContainer<T> implements IWeightedItem, Comparable<WeightedContainer<T>>
{
	private int weight;
	private T data;
	
	public WeightedContainer(T data, int weight)
	{
		this.data = data;
		this.weight = weight;
	}
	
	public T getData()
	{
		return data;
	}
	
	@Override
	public int getWeight()
	{
		return weight;
	}
	
	@Override
	public WeightedContainer<T> clone()
	{
		return new WeightedContainer<T>(data, weight);
	}
	
	@Override
	public int compareTo(WeightedContainer<T> other)
	{
		return this.weight - other.weight;
	}
}
