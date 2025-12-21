package space.yahho.mcmod.novaio;

import com.github.luben.zstd.Zstd;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
import net.minecraftforge.event.server.ServerStoppedEvent;
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
    // block entities
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    // items
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // tabs
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // command arguments
    public static final DeferredRegister<ArgumentTypeInfo<?,?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, MODID);

    // Creates a new Block with the id "novaio:slim_barrel", combining the namespace and path
    public static final RegistryObject<Block> SLIM_BARREL_BLOCK = BLOCKS.register("slim_barrel", () -> new SlimBarrelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).explosionResistance(8).instabreak().noOcclusion().instrument(NoteBlockInstrument.BASS).noLootTable()));
    // block entity "novaio:slim_barrel"
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<SlimBarrelBlockEntity>> SLIM_BARREL_ENTITY = BLOCK_ENTITIES.register("slim_barrel", () -> BlockEntityType.Builder.of(SlimBarrelBlockEntity::new, SLIM_BARREL_BLOCK.get()).build(null));
    // block item "novaio:slim_barrel"
    public static final RegistryObject<Item> SLIM_BARREL_BLOCK_ITEM = ITEMS.register("slim_barrel", () -> new BlockItem(SLIM_BARREL_BLOCK.get(), new Item.Properties()));
    // item "novaio:finite_qty_item"
    public static final RegistryObject<Item> FINITE_QTY_ITEM = ITEMS.register("finite_qty_item", () -> new FiniteQtyItem(new Item.Properties().stacksTo(Integer.MAX_VALUE)));
    // tab "novaio:novaio_tab"
    public static final RegistryObject<CreativeModeTab> NOVAIO_TAB = CREATIVE_MODE_TABS.register("novaio_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT) // tab order position
            .title(Component.nullToEmpty(MODNAME)) // name of this tab
            .icon(() -> SLIM_BARREL_BLOCK_ITEM.get().getDefaultInstance()) // the representative item of this mod
            .displayItems((parameters, output) -> {
                    // things shown inside of this tab
                    output.accept(SLIM_BARREL_BLOCK_ITEM.get());
            })
            .build());
    // command argument "novaio:novaio_uuid"
    public static final RegistryObject<ArgumentTypeInfo<?,?>> NOVAIO_UUID_ARGTYPE = COMMAND_ARGUMENT_TYPES.register("novaio_uuid", ()-> ArgumentTypeInfos.registerByClass(NovaIoUuidArgument.class, SingletonArgumentInfo.contextFree(NovaIoUuidArgument::novaIoUuid)));

    public NovaIO(@NotNull FMLJavaModLoadingContext ctx) {
        IEventBus modEventBus = ctx.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // block entities
        BLOCK_ENTITIES.register(modEventBus);
        // items
        ITEMS.register(modEventBus);
        // tabs
        CREATIVE_MODE_TABS.register(modEventBus);
        // command arguments
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
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) event.accept(SLIM_BARREL_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event) {
        // Do something when the server starts
        LOGGER.info("Initializing BigNumber data...");
        // <minecraft_path>/saves/<world_name>
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
            LOGGER.info("Successfully loaded BigNumbers.");
        } catch (Exception e) {
            LOGGER.error("Error during prepare datastore for world at {}, Reason: {}", worldPath, e);
            LOGGER.error("{} might not work correctly!", MODNAME);
        }

        byte[] zstdTest = Zstd.compress("hi!sushi!".getBytes());
        LOGGER.debug("ZStandard compression check: 'hi!sushi!' -> {} -> {}", zstdTest, Zstd.decompress(zstdTest));
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusListener {
            @SubscribeEvent
            public static void onCommandRegister(RegisterCommandsEvent event){
                LOGGER.debug("registering novaio commands...");
                NovaIoCommands.register(event.getDispatcher());
            }
    }

    @SubscribeEvent
    public void onLevelSave(LevelEvent.Save save) {
        // save is duty of serializeNBT()
        // if (save.getLevel() instanceof Level && ((Level) save.getLevel()).dimension() == Level.OVERWORLD) {
        //     NUMBER_MANAGER.saveAllStoredNumbers();
        // }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        // save is duty of serializeNBT()
        // NUMBER_MANAGER.saveAllStoredNumbers();

        // at client integrated server environment, exit of minecraft instance may not happen.
        // user might do continue with other world, but if failed to load its data, NUMBER_MANAGER will be kept before one.
        // so then, if we did not reset NUMBER_MANAGER here, number data for other world are mixed/overwriten into before loaded world's data.(corruption)
        // tl;dr. Prevent mixing next failure and last success
        NUMBER_MANAGER = null;
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
        }

        @SubscribeEvent
        public static void onModelBakeEvent(ModelEvent.ModifyBakingResult event) { }
    }
}
