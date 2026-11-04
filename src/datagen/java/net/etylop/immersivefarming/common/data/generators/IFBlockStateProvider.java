package net.etylop.immersivefarming.common.data.generators;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import blusunrize.immersiveengineering.data.models.NongeneratedModels;
import blusunrize.immersiveengineering.data.models.SplitModelBuilder;
import com.google.common.base.Preconditions;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.block.IFMultiblockLogic;
import net.etylop.immersivefarming.block.IFMultiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.client.model.generators.loaders.ObjModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IFBlockStateProvider extends BlockStateProvider {

    private final NongeneratedModels nongeneratedModels;
    final ExistingFileHelper exFileHelper;
    private final BlockModelProvider customModels;
    public IFBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper)
    {
        super(output, ImmersiveFarming.MOD_ID, exFileHelper);
        this.exFileHelper = exFileHelper;
        this.nongeneratedModels = new NongeneratedModels(output, exFileHelper);
        this.customModels = new BlockModelProvider(output, ImmersiveFarming.MOD_ID, exFileHelper) {

            protected void registerModels() {

            }
        };
    }
    @Override
    public @NotNull String getName(){
        return "Block Model/States";
    }
    @Override
    protected void registerStatesAndModels() {
        ImmersiveFarming.log.info("Registering block states...");
        composter();
    }

    private void composter(){
        ResourceLocation texture = modLoc("block/composter");
        ResourceLocation modelNormal = modLoc("models/block/multiblock/composter/composter.obj");
        ResourceLocation modelMirrored = modLoc("models/block/multiblock/composter/composter.obj");

        BlockModelBuilder normal = multiblockModel(IFMultiblockLogic.COMPOSTER.block().get(), modelNormal, texture, "", IFMultiblocks.COMPOSTER, false);
        BlockModelBuilder mirrored = multiblockModel(IFMultiblockLogic.COMPOSTER.block().get(), modelMirrored, texture, "_mirrored", IFMultiblocks.COMPOSTER, true);

        createMultiblock(IFMultiblocks.COMPOSTER.getBlock(), normal, mirrored, texture);
    }

    private BlockModelBuilder multiblockModel(Block block, ResourceLocation model, ResourceLocation texture, String add, TemplateMultiblock mb, boolean mirror){
        loadTemplateFor(mb);
        UnaryOperator<BlockPos> transform = UnaryOperator.identity();
        if(mirror){
            Vec3i size = mb.getSize(null);
            transform = p -> new BlockPos(size.getX() - p.getX() - 1, p.getY(), p.getZ());
        }
        final Vec3i offset = mb.getMasterFromOriginOffset();

        Stream<Vec3i> partsStream = mb.getTemplate(null).blocksWithoutAir().stream()
                .map(info -> info.pos())
                .map(transform)
                .map(p -> p.subtract(offset));

        String name = getMultiblockPath(block) + add;
        NongeneratedModels.NongeneratedModel base = nongeneratedModels.withExistingParent(name, mcLoc("block"))
                .customLoader(ObjModelBuilder::begin).modelLocation(model).automaticCulling(false).flipV(true).end()
                .texture("texture", texture)
                .texture("particle", texture);

        BlockModelBuilder split = this.models().withExistingParent(name + "_split", mcLoc("block"))
                .customLoader(SplitModelBuilder::begin)
                .innerModel(base)
                .parts(partsStream.collect(Collectors.toList()))
                .dynamic(false).end();

        return split;
    }


    /** From {@link blusunrize.immersiveengineering.data.blockstates.MultiblockStates} */
    private void loadTemplateFor(TemplateMultiblock multiblock){
        final ResourceLocation name = multiblock.getUniqueName();
        if(TemplateMultiblock.SYNCED_CLIENT_TEMPLATES.containsKey(name))
            return;
        final String filePath = "structures/" + name.getPath() + ".nbt";
        int slash = filePath.indexOf('/');
        String prefix = filePath.substring(0, slash);
        ResourceLocation shortLoc = new ResourceLocation(name.getNamespace(), filePath.substring(slash + 1));
        try{
            final Resource resource = exFileHelper.getResource(shortLoc, PackType.SERVER_DATA, "", prefix);
            try(final InputStream input = resource.open()){
                final CompoundTag nbt = NbtIo.readCompressed(input);
                final StructureTemplate template = new StructureTemplate();
                template.load(BuiltInRegistries.BLOCK.asLookup(), nbt);
                TemplateMultiblock.SYNCED_CLIENT_TEMPLATES.put(name, template);
            }
        }catch(IOException e){
            throw new RuntimeException("Failed on " + name, e);
        }
    }

    /** From {@link blusunrize.immersiveengineering.data.blockstates.BlockStates} */
    private void createMultiblock(Block b, ModelFile masterModel, ModelFile mirroredModel, ResourceLocation particleTexture){
        createMultiblock(b, masterModel, mirroredModel, IEProperties.MULTIBLOCKSLAVE, IEProperties.FACING_HORIZONTAL, IEProperties.MIRRORED, 180, particleTexture);
    }

    /** From {@link blusunrize.immersiveengineering.data.blockstates.BlockStates} */
    private void createMultiblock(Block b, ModelFile masterModel, @Nullable ModelFile mirroredModel, Property<Boolean> isSlave, EnumProperty<Direction> facing, @Nullable Property<Boolean> mirroredState, int rotationOffset, ResourceLocation particleTex){
        Preconditions.checkArgument((mirroredModel == null) == (mirroredState == null));
        VariantBlockStateBuilder builder = getVariantBuilder(b);

        boolean[] possibleMirrorStates;
        if(mirroredState != null)
            possibleMirrorStates = new boolean[]{false, true};
        else
            possibleMirrorStates = new boolean[1];
        for(boolean mirrored:possibleMirrorStates)
            for(Direction dir:facing.getPossibleValues()){
                final int angleY;
                final int angleX;
                if(facing.getPossibleValues().contains(Direction.UP)){
                    angleX = -90 * dir.getStepY();
                    if(dir.getAxis() != Direction.Axis.Y)
                        angleY = getAngle(dir, rotationOffset);
                    else
                        angleY = 0;
                }else{
                    angleY = getAngle(dir, rotationOffset);
                    angleX = 0;
                }

                ModelFile model = mirrored ? mirroredModel : masterModel;
                VariantBlockStateBuilder.PartialBlockstate partialState = builder.partialState()
//						.with(isSlave, false)
                        .with(facing, dir);

                if(mirroredState != null)
                    partialState = partialState.with(mirroredState, mirrored);

                partialState.setModels(new ConfiguredModel(model, angleX, angleY, true));
            }
    }

    /** From {@link blusunrize.immersiveengineering.data.blockstates.BlockStates} */
    private int getAngle(Direction dir, int offset){
        return (int) ((dir.toYRot() + offset) % 360);
    }

    private String getMultiblockPath(Block b){
        return "multiblock/" + getPath(b);
    }

    private String getPath(Block b){
        return Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(b)).getPath();
    }
}
