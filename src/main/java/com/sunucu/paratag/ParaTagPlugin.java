package com.sunucu.paratag;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParaTagPlugin extends JavaPlugin implements Listener {

    private static final String PARA_RENK = "&a";
    private static final String PING_RENK = "&b";
    private static final String PING_SEMBOL = "\u25C6";
    private static final long GUNCELLEME_SANIYE = 2L;

    private Economy economy;
    private final Map<UUID, ArmorStand[]> tagStands = new HashMap<>();

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault/ekonomi eklentisi bulunamadi! Plugin devre disi birakiliyor.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        for (Player p : Bukkit.getOnlinePlayers()) {
            createStandsFor(p);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    updateStandsFor(p);
                }
            }
        }.runTaskTimer(this, 20L, GUNCELLEME_SANIYE * 20L);

        getLogger().info("ParaTagPlugin aktif!");
    }

    @Override
    public void onDisable() {
        for (ArmorStand[] pair : tagStands.values()) {
            for (ArmorStand a : pair) {
                if (a != null && !a.isDead()) a.remove();
            }
        }
        tagStands.clear();
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        createStandsFor(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeStandsFor(event.getPlayer());
    }

    private void createStandsFor(Player player) {
        removeStandsFor(player);

        Location loc = player.getLocation();

        ArmorStand paraStand = spawnInvisibleStand(loc);
        ArmorStand pingStand = spawnInvisibleStand(loc);

        player.addPassenger(pingStand);
        pingStand.addPassenger(paraStand);

        tagStands.put(player.getUniqueId(), new ArmorStand[]{paraStand, pingStand});
        updateStandsFor(player);
    }

    private ArmorStand spawnInvisibleStand(Location loc) {
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setInvisible(true);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setSmall(true);
        stand.setBasePlate(false);
        stand.setCustomNameVisible(true);
        stand.setPersistent(false);
        return stand;
    }

    private void updateStandsFor(Player player) {
        ArmorStand[] pair = tagStands.get(player.getUniqueId());
        if (pair == null) return;
        ArmorStand paraStand = pair[0];
        ArmorStand pingStand = pair[1];

        if (paraStand == null || paraStand.isDead() || pingStand == null || pingStand.isDead()) {
            createStandsFor(player);
            return;
        }

        double bakiye = economy.getBalance(player);
        String bakiyeMetni = kisalt(bakiye);
        int ping = player.getPing();

        paraStand.setCustomName(renkli(PARA_RENK + "$ " + bakiyeMetni));
        pingStand.setCustomName(renkli(PING_RENK + PING_SEMBOL + " " + ping + "ms"));
    }

    private void removeStandsFor(Player player) {
        ArmorStand[] pair = tagStands.remove(player.getUniqueId());
        if (pair == null) return;
        for (ArmorStand a : pair) {
            if (a != null && !a.isDead()) a.remove();
        }
    }

    private String renkli(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String kisalt(double sayi) {
        if (sayi >= 1_000_000_000) return new DecimalFormat("0.##").format(sayi / 1_000_000_000) + "b";
        if (sayi >= 1_000_000) return new DecimalFormat("0.##").format(sayi / 1_000_000) + "m";
        if (sayi >= 1_000) return new DecimalFormat("0.##").format(sayi / 1_000) + "k";
        return new DecimalFormat("0.##").format(sayi);
    }
}
