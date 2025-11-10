package space.yahho.mcmod.novaio;

import com.github.luben.zstd.Zstd;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import space.yahho.mcmod.novaio.block.SlimBarrelBlock;
import space.yahho.mcmod.novaio.block.entity.SlimBarrelBlockEntity;
import space.yahho.mcmod.novaio.command.arguments.NovaIoUuidArgument;
import space.yahho.mcmod.novaio.command.server.NovaIoCommands;
import space.yahho.mcmod.novaio.item.FiniteQtyItem;
import space.yahho.mcmod.novaio.number.NovaIoNumberManager;

import java.io.File;
import java.nio.file.Path;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(NovaIO.MODID)
public class NovaIO {

    public static final String MODNAME = "NovaIO";
    // Define mod id in a common place for everything to reference
    public static final String MODID = "novaio";
    // Directly reference a slf4j logger
    public static final Logger LOGGER= LogUtils.getLogger();
    public static NovaIoNumberManager NUMBER_MANAGER;
    // Create a Deferred Register to hold Blocks which will all be registered under the "novaio" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "novaio" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "novaio" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredRegister<ArgumentTypeInfo<?,?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, MODID);
    // Creates a new Block with the id "novaio:example_block", combining the namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("slim_barrel", () -> new SlimBarrelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).explosionResistance(8).instabreak().noOcclusion().instrument(NoteBlockInstrument.BASS).noLootTable()));
    public static final RegistryObject<BlockEntityType<SlimBarrelBlockEntity>> SLIM_BARREL_ENTITY = BLOCK_ENTITIES.register("slim_barrel", () -> BlockEntityType.Builder.of(SlimBarrelBlockEntity::new, EXAMPLE_BLOCK.get()).build(null));
    // Creates a new BlockItem with the id "novaio:example_block", combining the namespace and path
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("slim_barrel", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> FINITE_QTY_ITEM = ITEMS.register("finite_qty_item", () -> new FiniteQtyItem(new Item.Properties().stacksTo(Integer.MAX_VALUE)));

    // Creates a new food item with the id "novaio:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEat()
            .nutrition(1)
            .saturationMod(2f)
            .build())));

    // Creates a creative tab with the id "novaio:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .title(Component.nullToEmpty(MODNAME))
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                    output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
            })
            .build());

    public static final RegistryObject<ArgumentTypeInfo<?,?>> NOVAIO_UUID_ARGTYPE = COMMAND_ARGUMENT_TYPES.register("novaio_uuid", ()-> ArgumentTypeInfos.registerByClass(NovaIoUuidArgument.class, SingletonArgumentInfo.contextFree(NovaIoUuidArgument::novaIoUuid)));

    public NovaIO(@NotNull FMLJavaModLoadingContext ctx) {
        IEventBus modEventBus = ctx.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        COMMAND_ARGUMENT_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ctx.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        if (Config.logDirtBlock) LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info("{}{}", Config.magicNumberIntroduction, Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
        Path worldPath = event.getServer().getWorldPath(LevelResource.ROOT).normalize().toAbsolutePath();
        File datastorePathFile = worldPath.resolve(MODNAME).toFile();
        LOGGER.debug("world path: {}", worldPath);
        try {
            if (!datastorePathFile.isDirectory()) {
                if (datastorePathFile.exists()) {
                    datastorePathFile.delete();
                }
                else datastorePathFile.mkdir();
            }
            NUMBER_MANAGER = new NovaIoNumberManager(datastorePathFile.toPath());
        } catch (Exception e) {
            LOGGER.error("Error during prepare datastore for world at {}, Reason: {}", worldPath, e);
            LOGGER.error("{} might not work correctly!", MODNAME);
        }

        byte[] zstdTest = Zstd.compress("hi!sushi!".getBytes());
        LOGGER.debug("ZStd check: 'hi!sushi!' -> {} -> {}", zstdTest, Zstd.decompress(zstdTest));
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public class ForgeBusListener {
            @SubscribeEvent
            public static void onCommandRegister(RegisterCommandsEvent event){
                LOGGER.debug("registering novaio commands...");
                NovaIoCommands.register(event.getDispatcher());
            }
    }

    @SubscribeEvent
    public void onLevelSave(LevelEvent.Save save) {
        if (save.getLevel() == Level.OVERWORLD) {
            NUMBER_MANAGER.saveAllStoredNumbers();
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        NUMBER_MANAGER.saveAllStoredNumbers();
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void onModelBakeEvent(ModelEvent.ModifyBakingResult event) { }
    }
}
