package StevenDimDoors.experimental.decorators;

import java.util.Random;

import net.minecraft.world.World;
import StevenDimDoors.experimental.LinkPlan;
import StevenDimDoors.experimental.PartitionNode;
import StevenDimDoors.experimental.RoomData;
import StevenDimDoors.mod_pocketDim.Point3D;
import StevenDimDoors.mod_pocketDim.config.DDProperties;

public class DecoratorCentralDoor extends BaseDecorator
{
	@Override
	public boolean canDecorate(RoomData room)
	{
		PartitionNode<RoomData> partition = room.getPartitionNode();
		return (room.getOutboundLinks().size() == 1 &&
				(partition.width() == 5 || partition.width() == 7) &&
				(partition.length() == 5 || partition.length() == 7));
	}

	@Override
	public void decorate(RoomData room, World world, Random random, DDProperties properties)
	{
		PartitionNode<RoomData> partition = room.getPartitionNode();
		Point3D source = partition.minCorner().clone();
		source.add(partition.width() / 2, 2, partition.length() / 2);
		LinkPlan outgoing = room.getOutboundLinks().get(0);
		super.placeDimensionalDoor(world, source.getX(), source.getY() - 1, source.getZ(), random.nextInt(4), outgoing.isInternal());
		outgoing.setSourcePoint(source);
		
		for (LinkPlan incoming : room.getInboundLinks())
		{
			incoming.setDestinationPoint(source);
		}
	}

}
