package net.etylop.immersivefarming.common.data;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.common.data.generators.IFBlockStateProvider;
import net.etylop.immersivefarming.common.data.generators.IFRecipes;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ImmersiveFarming.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class IFDataProvider {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper exHelper = event.getExistingFileHelper();

        ImmersiveFarming.log.info("-============ Immersive Farming Data Generation ============-");

        generator.addProvider(event.includeClient(), new IFBlockStateProvider(output, exHelper));
        generator.addProvider(event.includeClient(), new IFRecipes(output));
    }
}
