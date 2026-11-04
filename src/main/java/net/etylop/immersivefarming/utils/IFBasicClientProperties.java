package net.etylop.immersivefarming.utils;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.multiblocks.ClientMultiblocks.MultiblockManualData;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.client.utils.IERenderTypes;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.util.Utils;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.etylop.immersivefarming.block.IFMultiblocks;
import net.etylop.immersivefarming.block.multiblocks.IFTemplateMultiblock;
import net.etylop.immersivefarming.block.utils.IFDynamicModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

public class IFBasicClientProperties implements MultiblockManualData
{
	private static final Map<ResourceLocation, IFDynamicModel> MODELS = new HashMap<>();

	private final IETemplateMultiblock multiblock;
	@Nullable
	private NonNullList<ItemStack> materials;
	private final Supplier<IFDynamicModel> model;
	private final Optional<Quaternionf> rotation;

	public IFBasicClientProperties(IETemplateMultiblock multiblock)
	{
		this(multiblock, OptionalDouble.empty());
	}

	public IFBasicClientProperties(IETemplateMultiblock multiblock, OptionalDouble yRotationRadians)
	{
		this.multiblock = multiblock;
		this.model = Suppliers.memoize(() -> MODELS.get(multiblock.getUniqueName()));
		this.rotation = yRotationRadians.stream()
				.mapToObj(r -> new Quaternionf().rotateY((float)r))
				.findAny();
	}

	public static void initModels()
	{
		for(IMultiblock mb : IFMultiblocks.IF_MULTIBLOCKS)
			if (mb instanceof IFTemplateMultiblock ifMB)
				MODELS.put(mb.getUniqueName(), new IFDynamicModel(ifMB.getBlockName().getPath()));
	}

	@Override
	public NonNullList<ItemStack> getTotalMaterials()
	{
		if(materials==null)
		{
			List<StructureBlockInfo> structure = multiblock.getStructure(Minecraft.getInstance().level);
			materials = NonNullList.create();
			for(StructureBlockInfo info : structure)
			{
				// Skip dummy blocks in total
				if(info.state().hasProperty(IEProperties.MULTIBLOCKSLAVE) && info.state().getValue(IEProperties.MULTIBLOCKSLAVE))
					continue;
				ItemStack picked = Utils.getPickBlock(info.state());
				boolean added = false;
				for(ItemStack existing : materials)
					if(ItemStack.isSameItem(existing, picked))
					{
						existing.grow(1);
						added = true;
						break;
					}
				if(!added)
					materials.add(picked.copy());
			}
		}
		return materials;
	}

	@Override
	public void renderFormedStructure(PoseStack transform, MultiBufferSource bufferSource)
	{
		transform.pushPose();
		BlockPos offset = multiblock.getMasterFromOriginOffset();
		transform.translate(offset.getX(), offset.getY(), offset.getZ());
		if(rotation.isPresent())
		{
			transform.translate(0.5, 0, 0.5);
			transform.mulPose(rotation.get());
			transform.translate(-0.5, 0, -0.5);
		}
		List<BakedQuad> nullQuads = model.get().getNullQuads();
		VertexConsumer buffer = bufferSource.getBuffer(IERenderTypes.TRANSLUCENT_FULLBRIGHT);
		nullQuads.forEach(quad -> buffer.putBulkData(
				transform.last(), quad, 1, 1, 1, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
		));
		transform.popPose();
	}

	@Override
	public boolean canRenderFormedStructure()
	{
		return true;
	}
}
