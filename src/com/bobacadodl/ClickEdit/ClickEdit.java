package com.bobacadodl.ClickEdit;


import com.comphenix.protocol.wrappers.WrappedChatComponent;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ClickEdit extends JavaPlugin implements Listener {
	private SignGUI gui;
	boolean command_only = false;

	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		gui = new SignGUI(this);
		if (!new File(this.getDataFolder(), "config.yml").exists()) {
			this.saveDefaultConfig();
			reloadConfig();
		}
		command_only = getConfig().getBoolean("command-only");
	}

	public void onDisable() {
		gui.destroy();
	}

	@EventHandler
	public void onSignChange(SignChangeEvent event) {
		if (event.getPlayer().hasPermission("clickedit.color")) {
			for (int i = 0; i < event.getLines().length; i++) {
				event.setLine(
						i,
						ChatColor.translateAlternateColorCodes('&',
								event.getLines()[i]));
			}
		}
	}

	@EventHandler
	public void onSignClick(PlayerInteractEvent event) {
		if (!command_only) {
			Player p = event.getPlayer();
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK && p.isSneaking()
					&& p.hasPermission("clickedit.edit")) {
				Block b = event.getClickedBlock();
				if (b != null) {
					if (b.getState() instanceof Sign) {
						if (event.getPlayer().getItemInHand() != null) {
							if (event.getPlayer().getItemInHand().getType() == Material.SIGN) {
								return;
							}
						}
						Sign sign = (Sign) b.getState();
						editSign(sign, p, sign.getLines());
					}
				}
			}
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (command.getName().equalsIgnoreCase("clickedit")) {
			// open editor
			if (args.length > 0) {
				if (args[0].equalsIgnoreCase("edit")) {
					// sender.sendMessage(ChatColor.DARK_AQUA+"["+ChatColor.AQUA+"ClickEdit"+ChatColor.DARK_AQUA+"] "+ChatColor.RED+"Invalid args!");
					if (args.length == 1) {
						// edit the sign they're looking at
						if (sender instanceof Player) {
							Player p = (Player) sender;
							if (!p.hasPermission("clickedit.edit")) {
								p.sendMessage(ChatColor.RED
										+ "You do not have permission! (clickedit.edit)");
								return true;
							}
							Block block = p.getTargetBlock(null, 8);
							if (isSign(block)) {
								Sign sign = (Sign) block.getState();
								editSign(sign, p, sign.getLines());
							} else {
								p.sendMessage(ChatColor.DARK_AQUA + "["
										+ ChatColor.AQUA + "ClickEdit"
										+ ChatColor.DARK_AQUA + "] "
										+ ChatColor.RED
										+ "ERROR- Invalid block!");
							}
						} else {
							sender.sendMessage(ChatColor.DARK_AQUA + "["
									+ ChatColor.AQUA + "ClickEdit"
									+ ChatColor.DARK_AQUA + "] "
									+ ChatColor.RED
									+ "ERROR- Must be a player!");
						}
					} else {
						// edit line with command... cause why not
						if (args.length >= 3) {
							if (sender instanceof Player) {
								Player p = (Player) sender;
								if (!p.hasPermission("clickedit.edit")) {
									p.sendMessage(ChatColor.RED
											+ "You do not have permission! (clickedit.edit)");
									return true;
								}
								if (isInt(args[1])) {
									int line = Integer.parseInt(args[1]) - 1;
									if (line >= 0 && line < 4) {
										String text = StringUtils.join(args,
												' ', 2, args.length);
										Block block = p.getTargetBlock(null, 8);
										if (isSign(block)) {
											Sign sign = (Sign) block.getState();
											editSign(sign, p, line, text);
										} else {
											p.sendMessage(ChatColor.DARK_AQUA
													+ "[" + ChatColor.AQUA
													+ "ClickEdit"
													+ ChatColor.DARK_AQUA
													+ "] " + ChatColor.RED
													+ "ERROR- Invalid block!");
										}
									} else {
										p.sendMessage(ChatColor.DARK_AQUA
												+ "["
												+ ChatColor.AQUA
												+ "ClickEdit"
												+ ChatColor.DARK_AQUA
												+ "] "
												+ ChatColor.RED
												+ "ERROR- Invalid args! Must be between 1 and 4");
									}
								} else {
									p.sendMessage(ChatColor.DARK_AQUA
											+ "["
											+ ChatColor.AQUA
											+ "ClickEdit"
											+ ChatColor.DARK_AQUA
											+ "] "
											+ ChatColor.RED
											+ "ERROR- Invalid args! Must be an integer");
								}
							} else {
								sender.sendMessage(ChatColor.DARK_AQUA + "["
										+ ChatColor.AQUA + "ClickEdit"
										+ ChatColor.DARK_AQUA + "] "
										+ ChatColor.RED
										+ "ERROR- Must be a player!");
							}
						}
					}
					return true;
				}
			}
			sender.sendMessage(ChatColor.DARK_AQUA + "==" + ChatColor.AQUA
					+ "ClickEdit v" + getDescription().getVersion()
					+ ChatColor.DARK_AQUA + "==");
			sender.sendMessage(ChatColor.DARK_GREEN + "- by bobacadodl");
			sender.sendMessage(ChatColor.AQUA + "• /clickedit edit"
					+ ChatColor.GRAY + " - Edit the sign you're looking at");
			sender.sendMessage(ChatColor.AQUA
					+ "• /clickedit edit [line #] [text]" + ChatColor.GRAY
					+ " - Edit a line of the sign you are looking at");
			return true;
		}
		return false;
	}

	public boolean isInt(String s) {
		return StringUtils.isNumeric(s);
	}

	public boolean isSign(Block block) {
		if (block != null) {
			if (block.getState() instanceof Sign) {
				return true;
			}
		}
		return false;
	}

	public boolean editSign(Sign sign, Player p, int line, String text) {
		final Location signLoc = sign.getLocation();
		Block signBlock = sign.getBlock();
		if (!canBreak(p, signBlock)) {
			p.sendMessage(ChatColor.DARK_RED
					+ "You are not allowed to edit this sign!");
			return false;
		}
		if (!canPlace(p, signBlock, new ItemStack(Material.SIGN, 1))) {
			p.sendMessage(ChatColor.DARK_RED
					+ "You are not allowed to edit this sign!");
			return false;
		}

		String[] currentLines = sign.getLines();
		currentLines[line] = text;
		SignChangeEvent signChangeEvent = new SignChangeEvent(sign.getBlock(),
				p, currentLines);
		if (signChangeEvent.isCancelled()) {
			// can't edit sign
			p.sendMessage(ChatColor.DARK_RED + "You can't edit this sign!");
			return false;
		}

		sign.setLine(line, signChangeEvent.getLine(line));
		sign.update(true);
		return true;
	}

	public boolean canBreak(Player p, Block b) {
		BlockBreakEvent breakEvent = new BlockBreakEvent(b, p);
		Bukkit.getServer().getPluginManager().callEvent(breakEvent);
		return !breakEvent.isCancelled();
	}

	public boolean canPlace(Player p, Block b, ItemStack toPlace) {
		BlockPlaceEvent placeEvent = new BlockPlaceEvent(b, b.getState(),
				b.getRelative(BlockFace.DOWN), toPlace, p, false);
		Bukkit.getServer().getPluginManager().callEvent(placeEvent);
		return !placeEvent.isCancelled();
	}
	
	public WrappedChatComponent[] toWrap(String [] lines) {
		WrappedChatComponent [] arr = new WrappedChatComponent [lines.length];
		for(int i=0; i< lines.length; i++) {
			arr[i] = WrappedChatComponent.fromText(lines[i]);
		}
		return arr;
	}
	public boolean editSign(final Sign sign, final Player p,
			String... initLines) {
		final Location signLoc = sign.getLocation();
		Block signBlock = sign.getBlock();
		if (!canBreak(p, signBlock)) {
			p.sendMessage(ChatColor.DARK_RED
					+ "You are not allowed to edit this sign!");
			return false;
		}
		if (!canPlace(p, signBlock, new ItemStack(Material.SIGN, 1))) {
			p.sendMessage(ChatColor.DARK_RED
					+ "You are not allowed to edit this sign!");
			return false;
		}
		p.sendMessage(ChatColor.DARK_GREEN + "You are now editing the sign..");

		gui.open(p, toWrap(initLines), new SignGUI.SignGUIListener() {
			@Override
			public void onSignDone(Player player, String[] lines) {
				Block b = signLoc.getBlock();
				if (isSign(b)) {
					Sign sign = (Sign) b.getState();
					SignChangeEvent signChangeEvent = new SignChangeEvent(sign.getBlock(), player, lines);
					Bukkit.getServer().getPluginManager().callEvent(signChangeEvent);
					if (!signChangeEvent.isCancelled()) {
						sign.setLine(0, signChangeEvent.getLine(0));
						sign.setLine(1, signChangeEvent.getLine(1));
						sign.setLine(2, signChangeEvent.getLine(2));
						sign.setLine(3, signChangeEvent.getLine(3));
						sign.update(true);
						player.sendMessage(ChatColor.GREEN
								+ "Sign successfully edited!");
					} else {
						p.sendMessage(ChatColor.DARK_RED
								+ "Sign edit cancelled!");
					}
				} else {
					p.sendMessage(ChatColor.RED
							+ "The sign was broken while you were editing it!");
				}
			}

		});
		return true;
	}
}
