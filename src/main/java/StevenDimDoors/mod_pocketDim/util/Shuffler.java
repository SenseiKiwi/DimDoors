package StevenDimDoors.mod_pocketDim.util;

import java.util.Random;

public class Shuffler
{
	private Shuffler() { }
	
	public static <T> void shuffle(T[] data, Random random)
	{
		T temp;
		int j;
		for (int i = data.length - 1; i > 0; i--)
        {
        	j = random.nextInt(i + 1);
        	temp = data[j];
        	data[j] = data[i];
        	data[i] = temp;
        }
    }
	
	public static void shuffle(int[] data, Random random)
	{
		int temp;
		int j;
        for (int i = data.length - 1; i > 0; i--)
        {
        	j = random.nextInt(i + 1);
        	temp = data[j];
        	data[j] = data[i];
        	data[i] = temp;
        }
    }
}