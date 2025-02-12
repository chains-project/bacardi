package com.expl0itz.worldwidechat.inventory.wwctranslategui;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.expl0itz.worldwidechat.WorldwideChat;
import com.expl0itz.worldwidechat.commands.WWCTranslateChatIncoming;
import com.expl0itz.worldwidechat.commands.WWCTranslateChatOutgoing;
import com.expl0itz.worldwidechat.inventory.WWCInventoryManager;
import com.expl0itz.worldwidechat.util.ActiveTranslator;
import com.expl0itz.worldwidechat.util.CommonDefinitions;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;

public class WWCTranslateGUIChatMenu implements InventoryProvider {

	private WorldwideChat main = WorldwideChat.instance;

	private String targetPlayerUUID = "";

	public WWCTranslateGUIChatMenu(String targetPlayerUUID) {
		this.targetPlayerUUID = targetPlayerUUID;
	}

	/* Get translation info */
	public static SmartInventory getTranslateChatMenu(String targetPlayerUUID) {
		String playerTitle = "";
		playerTitle = ChatColor.BLUE + CommonDefinitions.getMessage("wwctGUIChatMenu", new String[] {WorldwideChat.instance.getServer()
				.getPlayer(UUID.fromString(targetPlayerUUID)).getName()});
		return SmartInventory.builder().id("translateChatMenu").provider(new WWCTranslateGUIChatMenu(targetPlayerUUID))
				.size(4, 9).manager(WorldwideChat.instance.getInventoryManager()).title(playerTitle).build();
	}

	@Override
	public void init(Player player, InventoryContents contents) {
		try {
			ActiveTranslator targetTranslator = main.getActiveTranslator(targetPlayerUUID);
			
			/* White stained glass borders as default, Green stained glass borders for active */
			ItemStack customBorders = XMaterial.WHITE_STAINED_GLASS_PANE.parseItem();
			ItemMeta borderMeta = customBorders.getItemMeta();
			borderMeta.setDisplayName(" ");
			customBorders.setItemMeta(borderMeta);
			contents.fillBorders(ClickableItem.empty(customBorders));
			
			if (targetTranslator.getTranslatingChatOutgoing() || targetTranslator.getTranslatingChatIncoming()) {
				ItemStack customBordersActive = XMaterial.GREEN_STAINED_GLASS_PANE.parseItem();
				ItemMeta borderMetaActive = customBordersActive.getItemMeta();
				borderMetaActive.setDisplayName(" ");
				customBordersActive.setItemMeta(borderMetaActive);
				contents.fillBorders(ClickableItem.empty(customBordersActive));
			}
			
			/* Outgoing Chat Button */
			if ((player.getUniqueId().toString().equals(targetPlayerUUID) && player.hasPermission("worldwidechat.wwctco")) || (!player.getUniqueId().toString().equals(targetPlayerUUID)) && player.hasPermission("worldwidechat.wwctco.otherplayers")) {
				ItemStack outgoingChatButton = XMaterial.CHEST_MINECART.parseItem();
				ItemMeta outgoingChatMeta = outgoingChatButton.getItemMeta();
				if (targetTranslator.getTranslatingChatOutgoing()) {
					outgoingChatMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
					outgoingChatMeta.addEnchant(XEnchantment.matchXEnchantment("power").get().parseEnchantment(), 1, false);
					outgoingChatMeta.setDisplayName(ChatColor.GREEN
							+ CommonDefinitions.getMessage("wwctGUIChatOutgoingButton"));
				} else {
					outgoingChatMeta.setDisplayName(ChatColor.YELLOW
							+ CommonDefinitions.getMessage("wwctGUIChatOutgoingButton"));
				}
				outgoingChatButton.setItemMeta(outgoingChatMeta);
				contents.set(1, 3, ClickableItem.of(outgoingChatButton, e -> {
					String[] args = { main.getServer().getPlayer(UUID.fromString(targetPlayerUUID)).getName() };
					WWCTranslateChatOutgoing translateChatOutgoing = new WWCTranslateChatOutgoing((CommandSender) player, null, null, args);
					translateChatOutgoing.processCommand();
					getTranslateChatMenu(targetPlayerUUID).open(player);
				}));	
			}
			
			/* Incoming Chat Button */
			if ((player.getUniqueId().toString().equals(targetPlayerUUID) && player.hasPermission("worldwidechat.wwctci")) || (!player.getUniqueId().toString().equals(targetPlayerUUID)) && player.hasPermission("worldwidechat.wwctci.otherplayers")) {
            	ItemStack incomingChatButton = XMaterial.MAP.parseItem();
    			ItemMeta incomingChatMeta = incomingChatButton.getItemMeta();
    			if (targetTranslator.getTranslatingChatIncoming()) {
    				incomingChatMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    				incomingChatMeta.addEnchant(XEnchantment.matchXEnchantment("power").get().parseEnchantment(), 1, false);
    				incomingChatMeta.setDisplayName(ChatColor.GREEN
    						+ CommonDefinitions.getMessage("wwctGUIChatIncomingButton"));
    			} else {
    				incomingChatMeta.setDisplayName(ChatColor.YELLOW
    						+ CommonDefinitions.getMessage("wwctGUIChatIncomingButton"));
    			}
    			incomingChatButton.setItemMeta(incomingChatMeta);
    			contents.set(1, 5, ClickableItem.of(incomingChatButton, e -> {
    				String[] args = { main.getServer().getPlayer(UUID.fromString(targetPlayerUUID)).getName() };
    				WWCTranslateChatIncoming translateChatIncoming = new WWCTranslateChatIncoming((CommandSender) player, null, null, args);
    				translateChatIncoming.processCommand();
    				getTranslateChatMenu(targetPlayerUUID).open(player);
    			}));	
			}
			
			/* Bottom Left Option: Previous Page */
			contents.set(2, 4, ClickableItem.of(WWCInventoryManager.getCommonButton("Previous"), e -> {
				WWCTranslateGUIMainMenu.getTranslateMainMenu(targetPlayerUUID).open(player);
			}));
		} catch (Exception e) {
			WWCInventoryManager.inventoryError(player, e);
		}
	}

	@Override
	public void update(Player player, InventoryContents contents) {
		WWCInventoryManager.checkIfPlayerIsMissing(player, targetPlayerUUID);
	}
}
