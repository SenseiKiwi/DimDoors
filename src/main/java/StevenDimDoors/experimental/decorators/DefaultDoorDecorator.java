package StevenDimDoors.experimental.decorators;

import java.util.Random;

import net.minecraft.world.World;
import StevenDimDoors.experimental.LinkPlan;
import StevenDimDoors.experimental.RoomData;
import StevenDimDoors.mod_pocketDim.Point3D;
import StevenDimDoors.mod_pocketDim.config.DDProperties;

public class DefaultDoorDecorator extends BaseDecorator
{	
	@Override
	public boolean canDecorate(RoomData room)
	{
		return !room.getOutboundLinks().isEmpty();
	}

	@Override
	public void decorate(RoomData room, World world, Random random, DDProperties properties)
	{
		// TODO: This is just an improvised implementation for testing
		Point3D corner = room.getPartitionNode().minCorner().clone();
		
		int count = 0;
		Point3D source = null;
		for (LinkPlan plan : room.getOutboundLinks())
		{
			source = new Point3D(corner.getX() + 2, corner.getY() + 2, corner.getZ() + count + 1);
			super.placeDimensionalDoor(world, source.getX(), source.getY() - 1, source.getZ(), 0, plan.isInternal());
			plan.setSourcePoint(source);
			count++;
		}
		
		if (source == null)
		{
			throw new IllegalStateException("This should never happen because this decorator only applies if outbound links exist!");
		}
		for (LinkPlan plan : room.getInboundLinks())
		{
			plan.setDestinationPoint(source);
		}
		
	}

}
