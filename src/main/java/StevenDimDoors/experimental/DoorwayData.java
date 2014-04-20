package StevenDimDoors.experimental;

import StevenDimDoors.mod_pocketDim.Point3D;

public class DoorwayData extends BoundingBox
{
	public static final char X_AXIS = 'X';
	public static final char Y_AXIS = 'Y';
	public static final char Z_AXIS = 'Z';
	
	private char axis;
	
	public DoorwayData(Point3D minCorner, Point3D maxCorner, char axis)
	{
		super(minCorner, maxCorner);
		this.axis = axis;
	}
	
	public char axis()
	{
		return axis;
	}
}
