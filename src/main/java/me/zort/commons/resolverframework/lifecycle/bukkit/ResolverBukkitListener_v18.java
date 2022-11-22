package me.zort.commons.resolverframework.lifecycle.bukkit;

import com.google.common.collect.Lists;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.List;

public class ResolverBukkitListener_v18 implements Listener {

    protected static List<Class<? extends Event>> BASE_EVENTS = Lists.newArrayList(
            AsyncPlayerChatEvent.class,
            AsyncPlayerPreLoginEvent.class,
            BlockBurnEvent.class,
            BlockCanBuildEvent.class,
            BlockDamageEvent.class,
            BlockDispenseEvent.class,
            BlockExpEvent.class,
            BlockExplodeEvent.class,
            BlockFadeEvent.class,
            BlockFormEvent.class,
            BlockFromToEvent.class,
            BlockGrowEvent.class,
            BlockIgniteEvent.class,
            BlockMultiPlaceEvent.class,
            BlockPhysicsEvent.class,
            BlockPistonExtendEvent.class,
            BlockPistonRetractEvent.class,
            BlockPlaceEvent.class,
            BlockRedstoneEvent.class,
            BlockSpreadEvent.class,
            BrewEvent.class,
            BlockBreakEvent.class,
            ChunkLoadEvent.class,
            ChunkPopulateEvent.class,
            ChunkUnloadEvent.class,
            CraftItemEvent.class,
            CreatureSpawnEvent.class,
            CreeperPowerEvent.class,
            EnchantItemEvent.class,
            EntityPortalEnterEvent.class,
            EntityDamageByEntityEvent.class,
            EntityDamageByBlockEvent.class,
            EntityDamageEvent.class,
            EntityPortalExitEvent.class,
            EntitySpawnEvent.class,
            InventoryCloseEvent.class,
            PlayerSpawnLocationEvent.class,
            ProjectileHitEvent.class,
            PlayerInteractAtEntityEvent.class,
            PlayerInteractEvent.class,
            PlayerJoinEvent.class,
            PlayerQuitEvent.class,
            PlayerMoveEvent.class,
            PlayerDropItemEvent.class,
            PlayerPickupItemEvent.class,
            PlayerTeleportEvent.class,
            WeatherChangeEvent.class
    );

    private final ResolverClassBukkit resolver;

    public ResolverBukkitListener_v18(ResolverClassBukkit resolver) {
        this.resolver = resolver;
    }

    /*@EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockBurn(BlockBurnEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockCanBuild(BlockCanBuildEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockDamage(BlockDamageEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockDispense(BlockDispenseEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockExp(BlockExpEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockFade(BlockFadeEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockForm(BlockFormEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockFormTo(BlockFromToEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockGrow(BlockGrowEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockMultiPlace(BlockMultiPlaceEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockPistonExtend(BlockPistonExtendEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockRedstone(BlockRedstoneEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBlockSpread(BlockSpreadEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBrew(BrewEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onChunkPopulate(ChunkPopulateEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onCraftItem(CraftItemEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onCreeperPower(CreeperPowerEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onEnchantItem(EnchantItemEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onSpawnLoc(PlayerSpawnLocationEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        invokeEvent(e);
    }
    @EventHandler
    public void onPlayerItemPickup(PlayerPickupItemEvent e) {
        invokeEvent(e);
    }*/

    protected void invokeEvent(Event e) {
        resolver.invokeListener(e);
    }

}
