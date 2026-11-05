package net.etylop.immersivefarming.block.entity;

import net.etylop.immersivefarming.api.crafting.ComposterRecipe;
import net.etylop.immersivefarming.block.IFBlockEntities;
import net.etylop.immersivefarming.block.custom.CompostingBinBlock;
import net.etylop.immersivefarming.block.custom.CompostingBinBlock.Stage;
import net.etylop.immersivefarming.item.IFItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.fluids.FluidStack;

/**
 * Logic of the composting bin, held by its left part.
 * Cycle: filling (wet + dry matter) -> composting, interrupted by turnings with a shovel -> ready (collect the compost).
 */
public class CompostingBinBlockEntity extends BlockEntity
{
    private Stage stage = Stage.FILLING;
    private int wet = 0;
    private int dry = 0;
    private int phase = 0; // number of completed composting phases
    private int timer = 0; // ticks spent in the current composting phase

    public CompostingBinBlockEntity(BlockPos pos, BlockState state)
    {
        super(IFBlockEntities.COMPOSTING_BIN.get(), pos, state);
    }

    /** Wet matter and dry matter (mB, same values as the Industrial Composter recipes) needed to fill the bin */
    public static final int MATTER_CAPACITY = 70;
    public static final int COMPOST_OUTPUT = 16;
    /** Number of times the compost must be turned with a shovel */
    public static final int TURNS = 3;
    /** 20 minutes of composting in total, split in TURNS+1 phases */
    private static final int PHASE_DURATION = 20*60*20/(TURNS+1);

    public void tickServer()
    {
        if(stage!=Stage.COMPOSTING)
            return;
        timer++;
        if(timer >= PHASE_DURATION)
        {
            timer = 0;
            phase++;
            if(phase > TURNS)
            {
                stage = Stage.READY;
                playSound(SoundEvents.COMPOSTER_READY);
            }
            else
            {
                stage = Stage.TURNING;
                playSound(SoundEvents.BARREL_OPEN);
            }
            updateBlockStates();
        }
        setChanged();
    }

    public InteractionResult interact(Player player, InteractionHand hand)
    {
        ItemStack held = player.getItemInHand(hand);
        switch(stage)
        {
            case FILLING ->
            {
                if(held.isEmpty())
                    return InteractionResult.PASS;
                return fill(player, held);
            }
            case COMPOSTING ->
            {
                player.displayClientMessage(Component.translatable("block.immersivefarming.composting_bin.composting"), true);
            }
            case TURNING ->
            {
                if(!held.canPerformAction(ToolActions.SHOVEL_DIG))
                {
                    player.displayClientMessage(Component.translatable("block.immersivefarming.composting_bin.needs_turning"), true);
                    return InteractionResult.CONSUME;
                }
                held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                playSound(SoundEvents.ROOTED_DIRT_BREAK);
                spawnTurningParticles();
                stage = Stage.COMPOSTING;
                playSound(SoundEvents.BARREL_CLOSE);
                updateBlockStates();
                setChanged();
            }
            case READY ->
            {
                ItemStack compost = new ItemStack(IFItems.COMPOST.get(), COMPOST_OUTPUT);
                if(!player.getInventory().add(compost))
                    player.drop(compost, false);
                playSound(SoundEvents.COMPOSTER_EMPTY);
                reset();
            }
        }
        return InteractionResult.CONSUME;
    }

    private InteractionResult fill(Player player, ItemStack held)
    {
        Level level = getLevel();
        int[] value = getMatterValue(level, held);
        if(value[0]<=0&&value[1]<=0)
        {
            player.displayClientMessage(Component.translatable("block.immersivefarming.composting_bin.not_compostable"), true);
            return InteractionResult.CONSUME;
        }
        int capacity = MATTER_CAPACITY;
        // Sneaking inserts the whole stack
        int maxItems = player.isShiftKeyDown()?held.getCount(): 1;
        int dryLevelBefore = getVisualLevel(true);
        int wetLevelBefore = getVisualLevel(false);
        int inserted = 0;
        while(inserted < maxItems&&((value[0] > 0&&wet < capacity)||(value[1] > 0&&dry < capacity)))
        {
            wet = Math.min(capacity, wet+value[0]);
            dry = Math.min(capacity, dry+value[1]);
            inserted++;
        }
        if(inserted==0)
        {
            player.displayClientMessage(Component.translatable(value[0] > 0
                    ?"block.immersivefarming.composting_bin.wet_full"
                    :"block.immersivefarming.composting_bin.dry_full"), true);
            return InteractionResult.CONSUME;
        }
        spawnFillParticles(held.copyWithCount(1), value[1] > 0, value[0] > 0);
        if(!player.getAbilities().instabuild)
            held.shrink(inserted);
        // Same sounds as the vanilla composter: a different one when the content visibly rises
        boolean rose = getVisualLevel(true) > dryLevelBefore||getVisualLevel(false) > wetLevelBefore;
        playSound(rose?SoundEvents.COMPOSTER_FILL_SUCCESS: SoundEvents.COMPOSTER_FILL);

        if(wet >= capacity&&dry >= capacity)
        {
            stage = Stage.COMPOSTING;
            phase = 0;
            timer = 0;
            playSound(SoundEvents.BARREL_CLOSE);
        }
        updateBlockStates();
        setChanged();
        return InteractionResult.CONSUME;
    }

    /**
     * @return {wet, dry} matter in mB given by one item, from the Industrial Composter recipes
     */
    public static int[] getMatterValue(Level level, ItemStack stack)
    {
        if(level==null||stack.isEmpty())
            return new int[]{0, 0};
        for(ComposterRecipe recipe : ComposterRecipe.RECIPES.getRecipes(level))
        {
            if(recipe.fluidProduct&&recipe.matches(stack))
            {
                FluidStack[] outputs = recipe.getFluidOutput();
                return new int[]{amount(outputs[0]), amount(outputs[1])};
            }
        }
        return new int[]{0, 0};
    }

    private static int amount(FluidStack fluid)
    {
        return fluid==null||fluid.isEmpty()?0: fluid.getAmount();
    }

    private void reset()
    {
        stage = Stage.FILLING;
        wet = 0;
        dry = 0;
        phase = 0;
        timer = 0;
        updateBlockStates();
        setChanged();
    }

    /**
     * Called when the bin is broken: the finished compost is not lost
     */
    public void dropContents()
    {
        if(stage==Stage.READY&&level!=null)
        {
            ItemStack compost = new ItemStack(IFItems.COMPOST.get(), COMPOST_OUTPUT);
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), compost);
            stage = Stage.FILLING;
        }
    }

    /**
     * Fill level shown by one half: dry matter on the left, wet matter on the right while filling
     */
    private int getVisualLevel(boolean left)
    {
        return switch(stage)
        {
            case FILLING ->
            {
                int capacity = MATTER_CAPACITY;
                int amount = left?dry: wet;
                // The top level is only shown when this half is really full
                if(amount >= capacity)
                    yield CompostingBinBlock.MAX_LEVEL;
                yield (int)Math.ceil((CompostingBinBlock.MAX_LEVEL-1f)*amount/capacity);
            }
            // The pile settles after each turning
            case TURNING -> Math.max(2, CompostingBinBlock.MAX_LEVEL-2*(phase-1));
            case COMPOSTING -> CompostingBinBlock.MAX_LEVEL;
            case READY -> CompostingBinBlock.MAX_LEVEL/2;
        };
    }

    /**
     * Mirror the stage and fill levels into the block states of both halves, for the models and particles
     */
    private void updateBlockStates()
    {
        if(level==null)
            return;
        // lower left, lower right, upper left, upper right
        BlockPos[] positions = CompostingBinBlock.getAllParts(worldPosition, getBlockState().getValue(CompostingBinBlock.FACING));
        for(int i = 0; i < positions.length; i++)
        {
            BlockState current = level.getBlockState(positions[i]);
            if(!(current.getBlock() instanceof CompostingBinBlock))
                continue;
            // Only the lower half shows the content
            int visualLevel = i < 2?getVisualLevel(i==0): 0;
            BlockState updated = current.setValue(CompostingBinBlock.STAGE, stage)
                    .setValue(CompostingBinBlock.LEVEL, visualLevel);
            if(updated!=current)
                level.setBlock(positions[i], updated, Block.UPDATE_ALL);
        }
    }

    private void playSound(SoundEvent sound)
    {
        if(level!=null)
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 1, 1);
    }

    /**
     * Bits of the inserted item and composter particles above the half that received matter (dry: left, wet: right)
     */
    private void spawnFillParticles(ItemStack item, boolean dryHalf, boolean wetHalf)
    {
        if(!(level instanceof ServerLevel serverLevel))
            return;
        BlockPos partner = CompostingBinBlock.getPartnerPos(worldPosition, getBlockState());
        ItemParticleOption itemParticle = new ItemParticleOption(ParticleTypes.ITEM, item);
        for(BlockPos pos : new BlockPos[]{dryHalf?worldPosition: null, wetHalf?partner: null})
        {
            if(pos==null)
                continue;
            double x = pos.getX()+0.5, y = pos.getY()+0.85, z = pos.getZ()+0.5;
            serverLevel.sendParticles(itemParticle, x, y, z, 8, 0.25, 0.05, 0.25, 0.05);
            serverLevel.sendParticles(ParticleTypes.COMPOSTER, x, y, z, 6, 0.3, 0.1, 0.3, 0);
        }
    }

    private void spawnTurningParticles()
    {
        if(level instanceof ServerLevel serverLevel)
        {
            BlockPos partner = CompostingBinBlock.getPartnerPos(worldPosition, getBlockState());
            BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState());
            for(BlockPos pos : new BlockPos[]{worldPosition, partner})
                serverLevel.sendParticles(particle, pos.getX()+0.5, pos.getY()+0.9, pos.getZ()+0.5, 15, 0.3, 0.1, 0.3, 0.05);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt)
    {
        super.saveAdditional(nbt);
        nbt.putString("stage", stage.getSerializedName());
        nbt.putInt("wet", wet);
        nbt.putInt("dry", dry);
        nbt.putInt("phase", phase);
        nbt.putInt("timer", timer);
    }

    @Override
    public void load(CompoundTag nbt)
    {
        super.load(nbt);
        stage = Stage.FILLING;
        for(Stage s : Stage.values())
            if(s.getSerializedName().equals(nbt.getString("stage")))
                stage = s;
        wet = nbt.getInt("wet");
        dry = nbt.getInt("dry");
        phase = nbt.getInt("phase");
        timer = nbt.getInt("timer");
    }
}
