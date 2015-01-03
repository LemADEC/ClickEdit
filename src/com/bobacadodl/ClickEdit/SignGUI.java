package com.bobacadodl.ClickEdit;

/**
 * Created by Nisovin!
 * Updated to 1.7 by bobacadodl!
 * Updated to 1.8 by thedark1337!
 * ProtocolLib by Comphenix
 * PacketWrapper by comphenix
 */

import com.comphenix.packetwrapper.WrapperPlayClientUpdateSign;
import com.comphenix.packetwrapper.WrapperPlayServerBlockAction;
import com.comphenix.packetwrapper.WrapperPlayServerBlockChange;
import com.comphenix.packetwrapper.WrapperPlayServerOpenSignEntity;
import com.comphenix.packetwrapper.WrapperPlayServerUpdateSign;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SignGUI implements Listener {

	protected ProtocolManager protocolManager;
	protected PacketAdapter packetListener;
	protected Map<UUID, SignGUIListener> listeners;
	protected Map<UUID, Vector> signLocations;

	public SignGUI(Plugin plugin) {
		this(plugin, true);
	}

	public SignGUI(Plugin plugin, boolean cleanup) {
		protocolManager = ProtocolLibrary.getProtocolManager();
		packetListener = new PacketListener(plugin);
		protocolManager.addPacketListener(packetListener);
		listeners = new ConcurrentHashMap<UUID, SignGUIListener>();
		signLocations = new ConcurrentHashMap<UUID, Vector>();
		if (cleanup) {
			Bukkit.getPluginManager().registerEvents(this, plugin);
		}
	}

	public void open(Player player, SignGUIListener response) {
		open(player, null, response);
	}

	public void open(Player player, WrappedChatComponent[] defaultText,
			SignGUIListener response) {
		WrapperPlayServerBlockAction blockAction = new WrapperPlayServerBlockAction();
		WrapperPlayServerBlockChange blockChange = new WrapperPlayServerBlockChange();
		WrapperPlayServerOpenSignEntity openSign = new WrapperPlayServerOpenSignEntity();
		WrapperPlayServerUpdateSign updateSign = new WrapperPlayServerUpdateSign();

		Material block = player.getLocation().getBlock().getType();
		// if setting pretext
		if (defaultText != null) {
			// set player location to sign block
			blockChange.setLocation(blockAction.getLocation());
			;
			blockAction.setBlockType(Material.SIGN_POST);
			blockChange.sendPacket(player);

			// set sign to pretext
			updateSign.setLocation(blockAction.getLocation());
			updateSign.setLines(defaultText);
			updateSign.sendPacket(player);
		}

		// open the sign
		openSign.setLocation(blockAction.getLocation());
		openSign.sendPacket(player);

		// restore the block and remove the sign
		if (defaultText != null) {
			blockChange.setLocation(blockAction.getLocation());
			blockAction.setBlockType(block);
			blockChange.sendPacket(player);
		}

		// listen for the player
		signLocations.put(player.getUniqueId(), player.getLocation().getBlock()
				.getLocation().toVector());
		listeners.put(player.getUniqueId(), response);
	}

	public void destroy() {
		protocolManager.removePacketListener(packetListener);
		listeners.clear();
		signLocations.clear();
	}

	public void cleanupPlayer(Player player) {
		listeners.remove(player.getUniqueId());
		signLocations.remove(player.getUniqueId());
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		cleanupPlayer(event.getPlayer());
	}

	public interface SignGUIListener {
		void onSignDone(Player player, WrappedChatComponent[] lines);

		void onSignDone(Player player, String[] lines);
	}

	class PacketListener extends PacketAdapter {

		Plugin plugin;

		public PacketListener(Plugin plugin) {
			super(plugin, ListenerPriority.NORMAL,
					PacketType.Play.Client.UPDATE_SIGN);
			this.plugin = plugin;
		}

		@Override
		public void onPacketReceiving(PacketEvent event) {
			// updating the sign
			WrapperPlayClientUpdateSign updateSign = new WrapperPlayClientUpdateSign(
					event.getPacket());

			final Player player = event.getPlayer();
			Vector v = signLocations.remove(player.getUniqueId());
			if (v == null)
				return;

			// make sure its the sign
			if (updateSign.getLocation().getX() != v.getBlockX())
				return;
			if (updateSign.getLocation().getY() != v.getBlockY())
				return;
			if (updateSign.getLocation().getZ() != v.getBlockZ())
				return;

			final WrappedChatComponent[] lines = updateSign.getLines();
			final SignGUIListener response = listeners.remove(event.getPlayer().getUniqueId());
			if (response != null) {
				event.setCancelled(true);
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
						new Runnable() {
							public void run() {
								response.onSignDone(player, lines);
							}
						});
			}
		}
	}
}