package net.minecraftforge.debug;

import org.apache.logging.log4j.Logger;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.objectdict.ObjectDictionary;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@EventBusSubscriber
@Mod(modid = "objectdicttest", name = "Object Dictionary Test", version = "1.0", acceptableRemoteVersions = "*")
public class ObjectDictTest
{
    private static Logger logger;
    
    @Mod.EventHandler
    public void pre(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        logger.info(String.format("'%s' is registered in the Block ObjectDict with the tags %s", HORSECHOP.getRegistryName(), ObjectDictionary.getObjectDictionary(Item.class, new ResourceLocation("food")).getTagsFor(HORSECHOP)));
    }
    
    @GameRegistry.ObjectHolder("objectdicttest:horsechop")
    public static final Item HORSECHOP = null;
    
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().register(new ItemFood(5, false).setRegistryName("horsechop"));
    }
    
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event)
    {
        ModelLoader.setCustomModelResourceLocation(HORSECHOP, 0, new ModelResourceLocation("objectdicttest:horsechop", "inventory"));
    }
}
