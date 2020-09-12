package dunkmania101.spatialharvesters.objects.tile_entities;

import dunkmania101.spatialharvesters.data.CommonConfig;
import dunkmania101.spatialharvesters.data.CustomEnergyStorage;
import dunkmania101.spatialharvesters.init.TileEntityInit;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HeatGeneratorTE extends TileEntity implements ITickableTileEntity {
    public HeatGeneratorTE(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    public HeatGeneratorTE() {
        this(TileEntityInit.HEAT_GENERATOR.get());
    }

    private final CustomEnergyStorage energyStorage = createEnergy();

    private final LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> energyStorage);

    private final int speed = CommonConfig.HEAT_GENERATOR_SPEED.get();
    private CustomEnergyStorage createEnergy() {
        int capacity = speed * 1000;
        return new CustomEnergyStorage(capacity, capacity) {
            @Override
            protected void onEnergyChanged() {
                markDirty();
            }

            @Override
            public boolean canExtract() {
                return true;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        };
    }

    @Override
    public void remove() {
        super.remove();
        energy.invalidate();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            return energy.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void tick() {
        if (world != null && !world.isRemote) {
            for (Direction direction : Direction.values()) {
                Block block = world.getBlockState(pos.offset(direction)).getBlock();
                if (block == Blocks.MAGMA_BLOCK || block == Blocks.LAVA || block == Blocks.FIRE) {
                    energyStorage.addEnergy(speed);
                }
                int energy = energyStorage.getEnergyStored();
                if (energy > 0) {
                    TileEntity tile = world.getTileEntity(pos.offset(direction));
                    if (tile != null) {
                        LazyOptional<IEnergyStorage> tile_energy = tile.getCapability(CapabilityEnergy.ENERGY, direction);
                        if (tile_energy.isPresent()) {
                            int tile_received = tile_energy.orElse(null).receiveEnergy(energy, false);
                            energyStorage.consumeEnergy(tile_received);
                        }
                    }
                }
            }
        }
    }
}
