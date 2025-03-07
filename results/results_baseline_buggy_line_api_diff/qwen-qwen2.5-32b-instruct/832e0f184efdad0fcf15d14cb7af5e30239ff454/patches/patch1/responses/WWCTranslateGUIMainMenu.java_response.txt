package com.expl0itz.worldwidechat.inventory.wwctranslategui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.expl0itz.worldwidechat.WorldwideChat;
import com.expl0itz.worldwidechat.commands.WWCGlobal;
import com.expl0itz.worldwidechat.commands.WWCTranslate;
import com.expl0itz.worldwidechat.commands.WWCTranslateBook;
import com.expl0itz.worldwidechat.commands.WWCTranslateEntity;
import com.expl0itz.worldwidechat.commands.WWCTranslateItem;
import com.expl0itz.worldwidechat.commands.WWCTranslateSign;
import com.expl0itz.worldwidechat.conversations.wwctranslategui.RateLimitConversation;
import com.expl0itz.worldwidechat.inventory.WWCInventoryManager;
import com.expl0itz.worldwidechat.util.ActiveTranslator;
import com.expl0itz.worldwidechat.util.CommonDefinitions;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;

public class WWCTranslateGUIMainMenu implements InventoryProvider {

	private WorldwideChat main = WorldwideChat.instance;

	private String targetPlayerUUID = "";

	public WWCTranslateGUIMainMenu(String targetPlayerUUID) {
		this.targetPlayerUUID = targetPlayerUUID;
	}

	/* Get translation info */
	public static SmartInventory getTranslateMainMenu(String targetPlayerUUID) {
		String playerTitle = "";
		if (targetPlayerUUID.equals("GLOBAL-TRANSLATE-ENABLED")) {
			playerTitle = ChatColor.BLUE + CommonDefinitions.getMessage("wwctGUIMainMenuGlobal");
		} else {
			playerTitle = ChatColor.BLUE + CommonDefinitions.getMessage("wwctGUIMainMenuPlayer", new String[] {WorldwideChat.instance.getServer()
					.getPlayer(UUID.fromString(targetPlayerUUID)).getName()});
		}
		return SmartInventory.builder().id("translateMainMenu").provider(new WWCTranslateGUIMainMenu(targetPlayerUUID))
				.size(5, 9).manager(WorldwideChat.instance.getInventoryManager()).title(playerTitle).build();
	}

	@Override
	public void init(Player player, InventoryContents contents) {
		try {
			/* Default white stained glass borders for inactive */
			ItemStack customDefaultBorders = XMaterial.WHITE_STAINED_GLASS_PANE.parseItem();
			if (!main.getActiveTranslator(targetPlayerUUID).getUUID().equals("")) {
				customDefaultBorders = XMaterial.GREEN_STAINED_GLASS_PANE.parseItem();
			}
			ItemMeta defaultBorderMeta = customDefaultBorders.getItemMeta();
			defaultBorderMeta.setDisplayName(" ");
			customDefaultBorders.setItemMeta(defaultBorderMeta);
			contents.fillBorders(ClickableItem.empty(customDefaultBorders));

			/* New translation button */
			ItemStack translationButton = XMaterial.COMPASS.parseItem();
			ItemMeta translationMeta = translationButton.getItemMeta();
			translationMeta.setDisplayName(
					CommonDefinitions.getMessage("wwctGUITranslationButton"));
			translationButton.setItemMeta(translationMeta);
			contents.set(2, 4, ClickableItem.of(translationButton, e -> {
				WWCTranslateGUISourceLanguage.getSourceLanguageInventory("", targetPlayerUUID).open(player);
			}));

			/* Set active translator to our current target */
			ActiveTranslator targetTranslator = main.getActiveTranslator(targetPlayerUUID);
			
			if (!targetPlayerUUID.equals("GLOBAL-TRANSLATE-ENABLED") && player.hasPermission("worldwidechat.wwcte")
					&& (player.hasPermission("worldwidechat.wwcte.otherplayers") || player.getUniqueId().toString().equals(targetPlayerUUID))) {
				ItemStack entityButton = XMaterial.NAME_TAG.parseItem();
				ItemMeta entityMeta = entityButton.getItemMeta();
				if (targetTranslator.getTranslatingEntity()) {
					entityMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
					entityMeta.addEnchant(XEnchantment.PROTECTION_ENVIRONMENTAL, 1, false);
					entityMeta.setDisplayName(ChatColor.GREEN
							+ CommonDefinitions.getMessage("wwctGUIEntityButton"));
				} else {
					entityMeta.setDisplayName(ChatColor.YELLOW
							+ CommonDefinitions.getMessage("wwctGUIEntityButton"));
				}
				entityButton.setItemMeta(entityMeta);
				contents.set(2, 2, ClickableItem.of(entityButton, e -> {
					String[] args = { main.getServer().getPlayer(UUID.fromString(targetPlayerUUID)).getName() };
					WWCTranslateEntity translateEntity = new WWCTranslateEntity((CommandSender) player, null, null, args);
					translateEntity.processCommand();
					getTranslateMainMenu(targetPlayerUUID).open(player);
				}));
			}
			
			/* Chat Translation Button */
			if (!targetPlayerUUID.equals("GLOBAL-TRANSLATE-ENABLED")
					&& ((targetPlayerUUID.equals(player.getUniqueId().toString()) && (player.hasPermission("worldwidechat.wwctco") || player.hasPermission("worldwidechat.wwctci"))) 
							|| (!targetPlayerUUID.equals(player.getUniqueId().toString()) && (player.hasPermission("worldwidechat.wwctco.otherplayers") || player.hasPermission("worldwidechat.wwctci.otherplayers")))) {
				ItemStack chatButton = XMaterial.PAINTING.parseItem();
				ItemMeta chatMeta = chatButton.getItemMeta();
				if (targetTranslator.getTranslatingChatOutgoing() || targetTranslator.getTranslatingChatIncoming()) {
					chatMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
					chatMeta.addEnchant(XEnchantment.PROTECTION_ENVIRONMENTAL, 1, false);
					List<String> outLoreChat = new ArrayList<>();
					outLoreChat.add(ChatColor.LIGHT_PURPLE + CommonDefinitions.getMessage("wwctGUIExistingChatIncomingEnabled", new String[] {ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + targetTranslator.getTranslatingChatIncoming()}));
					outLoreChat.add(ChatColor.LIGHT_PURPLE + CommonDefinitions.getMessage("wwctGUIExistingChatOutgoingEnabled", new String[] {ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + targetTranslator.getTranslatingChatOutgoing()}));
					chatMeta.setLore(outLoreChat);
					chatMeta.setDisplayName(ChatColor.GREEN
							+ CommonDefinitions.getMessage("wwctGUIChatButton"));
				} else {
					chatMeta.setDisplayName(ChatColor.YELLOW
							+ CommonDefinitions.getMessage("wwctGUIChatButton"));
				}
				chatButton.setItemMeta(chatMeta);
				contents.set(3, 4, ClickableItem.of(chatButton, e -> {
					WWCTranslateGUISourceLanguage.getSourceLanguageInventory("", targetPlayerUUID).open(player);
				}));
			}
		} catch (Exception e) {
			WWCInventoryManager.inventoryError(player, e);
		}
	}

	@Override
	public void update(Player player, InventoryContents contents) {
		WWCInventoryManager.checkIfPlayerIsMissing(player, targetPlayerUUID);
	}
}