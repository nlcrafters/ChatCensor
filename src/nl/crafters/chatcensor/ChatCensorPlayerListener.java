package nl.crafters.chatcensor;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;
import com.matejdro.bukkit.jail.Jail;
import com.nijiko.coelho.iConomy.iConomy;
import com.nijiko.coelho.iConomy.system.Account;

public class ChatCensorPlayerListener extends PlayerListener {
	public static ChatCensor plugin;
	public static Censor c;
	public final static int lineLength = 53;
	
	public ChatCensorPlayerListener(ChatCensor instance) {
		plugin = instance;
	}
	// Listens for player chats (event registered in main class)
	public void onPlayerChat(PlayerChatEvent event) {
		if (event.isCancelled()) 
			return;
		Player player = event.getPlayer();
		// check if player is in jail, and mute is on:
		if (plugin.useAutoJail) {
			try {
				if (plugin.jail.API.isJailed(player.getName()) && plugin.useJailMute)  
				{
					player.sendMessage(plugin.getMessage("messagetext.player-muted", null));
					event.setCancelled(true);
					player = null;
					return;
				}
			} catch (Exception e) {
				plugin.AddLog("Error in accessing Jail API:" + e.getMessage());
			}
		}
		String cMessage = plugin.c.isForbidden(event.getMessage());
		boolean badnews = false;
		if (cMessage.substring(0,1).equals("0")) {
			badnews = true;
		}
		cMessage = cMessage.substring(1);
		if (badnews) 
		{
			event.setMessage(cMessage);
			TakeAction(player);			
		}
		
		player = null;
		cMessage = null;
	}

	private void TakeAction(Player player) {
		plugin.db.AddCounter(player.getName(), "total");
		FinePlayer(player,false);
		KickPlayer(player,false);
		JailPlayer(player,false);
	}
	private boolean KickPlayer(Player p, boolean ignoreStatus) {
		if (plugin.useAutoKick || ignoreStatus )
		{
			plugin.playerKicked(p);
			String kMessage = "";
			if (plugin.useiConomy || ignoreStatus ) {
				kMessage = plugin.getColor("labelsandcolors.chat-message-color") + plugin.getMessage("messagetext.player-kicked-fined", p);
				//kMessage = "Automatische kick ivm /vloeken! + een boete van " + plugin.PlayerFine + " coin!";
			}
			else {
				kMessage = plugin.getColor("labelsandcolors.chat-message-color") + plugin.getMessage("messagetext.player-kicked", p);
				//kMessage = "Je bent van deze server af gegooid ivm /vloeken!";
			}
			p.kickPlayer(kMessage);
			plugin.db.AddCounter(p.getName(), "total");
			plugin.db.AddCounter(p.getName(), "kick");
			kMessage = null;
		}
		return true;
	}
	private boolean JailPlayer(Player p, boolean ignoreStatus) {
		if (plugin.useAutoJail || ignoreStatus ) {
			
			try {
				if (Jail.zones.size() > 0) {
					int nJailTime = plugin.db.GetJailTime(p,plugin.JailTime);
					plugin.playerJailed(p);
					CraftServer cs = (CraftServer) plugin.getServer();
					CommandSender coms = new ConsoleCommandSender(plugin.getServer());
					cs.dispatchCommand(coms,"jail " + p.getName() + " " + nJailTime);
					plugin.db.AddCounter(p.getName(), "jail");
				}
			} catch (Exception e) {
				plugin.AddLog("Error accessing Jail plugin:" + e.getMessage());
			}
		}
		return true;
	}
	private boolean FinePlayer(Player p, boolean ignoreStatus) {
		if (plugin.useiConomy) {
			plugin.playerFined(p);
			Account account = iConomy.getBank().getAccount(p.getName());
			if (account.hasEnough(plugin.PlayerFine))
			{
				account.subtract(plugin.PlayerFine);
			}
			else {
				double balance = account.getBalance();
				account.subtract(balance);
				if ( (plugin.useKickWhenBroke==true) && (plugin.useAutoKick==false) ) {
					KickPlayer(p,true);
				}
			}
			plugin.db.AddCounter(p.getName(), "fine");
			account = null;
		}
		return true;
	}
	
	
	// send log message to player or server (when used from console)
	public void Log(Player p, String message) {
		if (p==null) {
			plugin.AddLog(message);
		}
		else {
			p.sendMessage(message);
		}
	}
	
	// Check permissions simplified
	public boolean checkP(Player p, String perm) {
		if (p==null) {
			return true;
		}
		else {
			if (plugin.pSystem==1) {
				 return plugin.wh.getWorldPermissions(p).has(p,perm);
			}
			if (plugin.pSystem==2) {
				if (ChatCensor.Permissions.has(p,perm)) {
					return true;
				}
			}
		}
		return false;
	}
	
	// if user/serverconsole isses a command, this method is executed
	public void onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		String cmd = command.getName();
		String action = "";
		
		Player player = null;
		
		if (!cmd.equalsIgnoreCase("cc"))
		{
			return;
		}
	    if (sender instanceof Player){
	     	player = (Player)sender;
	    }
	    else {
	    	player = null;
	    }
		String wordTarget = "";
		
		try {
				action = args[0].toLowerCase();
			} catch (Exception e) {
				action = "status";
			}
		if ( action.equalsIgnoreCase("setfine") && checkP(player,"ChatCensor.modify") ) {
			plugin.PlayerFine = Integer.parseInt(args[1]);
			plugin.saveConfig();
		}
		else if ( action.equalsIgnoreCase("setjailtime") && checkP(player,"ChatCensor.modify") ) {
			plugin.JailTime = Integer.parseInt(args[1]);
			plugin.saveConfig();
		}
		else if ( action.equalsIgnoreCase("save") && checkP(player,"ChatCensor.modify") ) {
			plugin.saveConfig();
			Log(player,ChatColor.AQUA + "Configuration saved");
		}
		else if ( action.equalsIgnoreCase("reset") && checkP(player,"ChatCensor.modify") ) {
			String ttype = "" ;
			try {
				ttype = args[1];
			} catch (Exception e) {
				Log(player,plugin.CHATPREFIX  + " Command usage is: /cc reset playername");
				return;
			}
			plugin.db.ResetCounter(player,ttype);
			Log(player,ChatColor.AQUA + "Data has been reset for player: " + ChatColor.RED + ttype);
		}
		else if ( action.equalsIgnoreCase("reload") && checkP(player,"ChatCensor.reload") ) {
			plugin.reload();
			Log(player,ChatColor.AQUA + "Configuration reloaded");
		}
		else if ( action.equalsIgnoreCase("import") && checkP(player,"ChatCensor.reload") ) {
			plugin.db.importWords(player);
			plugin.c.loadWords();
		}
		else if ( action.equalsIgnoreCase("export") && checkP(player,"ChatCensor.reload") ) {
			plugin.db.exportWords(player);
		}
		else if (action.equalsIgnoreCase("add") && (args.length==2) && checkP(player,"ChatCensor.modify") ) {
			wordTarget = args[1].toLowerCase();
			if (wordTarget.length() < 3) {
				Log(player,plugin.CHATPREFIX  + " Word is too short (minimum length:3)");
			}
			else {
				plugin.c.addWord(player,wordTarget);
				plugin.c.loadWords();
			}
		}
		else if (action.equalsIgnoreCase("remove") && (args.length==2) && checkP(player,"ChatCensor.modify") ) {
			wordTarget = args[1].toLowerCase();
			plugin.c.removeWord(player,wordTarget);
		}
		else if (action.equalsIgnoreCase("clearall") && checkP(player,"ChatCensor.modify") ) {
			plugin.db.resetList();
			plugin.c.loadWords();
			Log(player,plugin.CHATPREFIX  + " List has been cleared, 0 words in database now!");
		}
		else if (action.equalsIgnoreCase("list") && checkP(player,"ChatCensor.list")) {
			String sstring = "";
			if (args.length==2) {
				sstring  = args[1].toLowerCase();
			}
			plugin.c.listWords(player,sstring);
		}
		else if (action.equalsIgnoreCase("toggle") && checkP(player,"ChatCensor.modify")) {
			String ttype = "" ;
			try {
				ttype = args[1];
			} catch (Exception e) {
				// TODO: handle exception
				Log(player,plugin.CHATPREFIX  + " Possible toggles: jail - mute - pay - kick" + plugin.useAutoJail);
				Log(player,plugin.CHATPREFIX  + " Type /cc toggles to see all toggle statusses");
			}
			if (ttype.equalsIgnoreCase("jail")) {
				if (plugin.checkJail() ) {
					plugin.useAutoJail=!plugin.useAutoJail;
					plugin.saveConfig();
					Log(player,plugin.CHATPREFIX  + " Auto-jail status now is:" + ChatColor.YELLOW +  plugin.useAutoJail);
				}
				else {
					Log(player,plugin.CHATPREFIX  + " Jail plugin not available, so auto-jail = " + ChatColor.YELLOW +  plugin.useAutoJail);
				}
			}
			if (ttype.equalsIgnoreCase("mute")) {
				if (plugin.checkJail() ) {
					plugin.useJailMute=!plugin.useJailMute;
					plugin.saveConfig();
					Log(player,plugin.CHATPREFIX  + " Mute while in jail:" + ChatColor.YELLOW +  plugin.useJailMute);
				}
				else {
					Log(player,plugin.CHATPREFIX  + " Jail plugin not available, so auto-mute = " + ChatColor.YELLOW +  plugin.useAutoJail);
				}
			}
			if (ttype.equalsIgnoreCase("pay")) {
				if (plugin.checkiConomy()) {
					plugin.useiConomy=!plugin.useiConomy;
					plugin.saveConfig();
					Log(player,plugin.CHATPREFIX  + " Auto-pay fine status now is:" + ChatColor.YELLOW + plugin.useiConomy);
				}
			
				else {
					Log(player,plugin.CHATPREFIX  + " iConomy plugin not available, so auto-fine = " + ChatColor.YELLOW +  plugin.useiConomy);
				}
					
			}
			if (ttype.equalsIgnoreCase("regex")) {
				plugin.useRegex=!plugin.useRegex;
				Log(player,plugin.CHATPREFIX  + " Use regex:" + ChatColor.YELLOW + plugin.useRegex);
			}
			if (ttype.equalsIgnoreCase("kick")) {
				plugin.useAutoKick=!plugin.useAutoKick;
				plugin.saveConfig();
				Log(player,plugin.CHATPREFIX  + " Auto-Kick status now is:" + ChatColor.YELLOW + plugin.useAutoKick);
			}
		}
		else if (action.equalsIgnoreCase("kick") && checkP(player,"ChatCensor.action.kick")) {
			String ttype = "" ;
			try {
				ttype = args[1];
			} catch (Exception e) {
				// TODO: handle exception
				Log(player,plugin.CHATPREFIX  + " name not found, use /cc kick playername");
				return;
			}
			Player p = null;
			for (Player i : plugin.getServer().getOnlinePlayers())
			{
				if (i.getName().equalsIgnoreCase(ttype))
					p = i;
			}
			if (p!=null) {
				KickPlayer(p,true);
			}
			else {
				Log(player,plugin.CHATPREFIX  + " name " + ttype + " not found, use /cc fineplayername");
			}
			p = null;
			ttype=null;
		}
		else if (action.equalsIgnoreCase("jail") && checkP(player,"ChatCensor.action.jail")) {
			String ttype = "" ;
			try {
				ttype = args[1];
			} catch (Exception e) {
				// TODO: handle exception
				Log(player,plugin.CHATPREFIX  + " name not found, use /cc jail playername");
				return;
			}
			Player p = null;
			for (Player i : plugin.getServer().getOnlinePlayers())
			{
				if (i.getName().equalsIgnoreCase(ttype)) {
					p = i;
				}
			}
			if (p!=null) {
				JailPlayer(p,true);
			}
			else {
				Log(player,plugin.CHATPREFIX  + " name " + ttype + " not found, use /cc fineplayername");
			}
			p = null;
			ttype=null;
		}
		else if (action.equalsIgnoreCase("fine") && checkP(player,"ChatCensor.action.fine")) {
			String ttype = "" ;
			try {
				ttype = args[1];
			} catch (Exception e) {
				// TODO: handle exception
				Log(player,plugin.CHATPREFIX  + " name not found, use /cc fineplayername");
				return;
			}
			Player p = null;
			for (Player i : plugin.getServer().getOnlinePlayers())
			{
				if (i.getName().equalsIgnoreCase(ttype))
					p = i;
			}
			if (p!=null) {
				FinePlayer(p,true);
			}
			else {
				Log(player,plugin.CHATPREFIX  + " name " + ttype + " not found, use /cc fineplayername");
			}
			p = null;
			ttype=null;
		}
		else if (action.equalsIgnoreCase("check") && checkP(player,"ChatCensor.status")  ) {
			String ttype = "" ;
			try {
				ttype = args[1];
			} catch (Exception e) {
				Log(player,plugin.CHATPREFIX  + " name not found, use /cc check playername");
				return;
			}
			String stats[] = plugin.db.GetCounters(ttype).split(";");
			if (stats.length>2) {
				Log(player,plugin.CHATPREFIX  + " ---- ChatCensor stats for " + ttype + " ---- ");
				Log(player,plugin.CHATPREFIX  + " Jailed: " + ChatColor.YELLOW + stats[1] + " times");
				Log(player,plugin.CHATPREFIX  + " Kicked: " + ChatColor.YELLOW + stats[2] + " times");
				Log(player,plugin.CHATPREFIX  + " Fined: " + ChatColor.YELLOW + stats[4] + " coin");
				Log(player,plugin.CHATPREFIX  + " Total: " + ChatColor.YELLOW + stats[0] + " times");
			}
			else {
				Log(player,plugin.CHATPREFIX  + " player not found, use /cc check playername");
				return;
			}
		}
		else if (action.equalsIgnoreCase("top") && checkP(player,"ChatCensor.status")  ) 
		{
			plugin.db.GetList(player);
		}
		if (action.equalsIgnoreCase("status")  ) {
			if (checkP(player,"ChatCensor.list")) {
				Log(player,plugin.CHATPREFIX  + " Auto-Kick status: " + ChatColor.YELLOW + plugin.useAutoKick);
				Log(player,plugin.CHATPREFIX  + " Auto-jail status: " + ChatColor.YELLOW + plugin.useAutoJail + " /" + plugin.JailTime + " min");
				Log(player,plugin.CHATPREFIX  + " Mute while in jail: " + ChatColor.YELLOW + plugin.useJailMute);
				Log(player,plugin.CHATPREFIX  + " Regex mode enabled: " + ChatColor.YELLOW + plugin.useRegex);
				Log(player,plugin.CHATPREFIX  + " Auto-pay fine status: " + ChatColor.YELLOW + plugin.useiConomy);
				Log(player,plugin.CHATPREFIX  + " Fine value: " + ChatColor.YELLOW + plugin.PlayerFine);
			}
			if (action.equalsIgnoreCase("status") && checkP(player,"ChatCensor.status")) {
				if (player==null) {
					return;
				}
				String stats[] = plugin.db.GetCounters(player.getName()).split(";");
				if (stats.length<=1) {
					String tmp = "0;0;0;0;0";
					stats=tmp.split(";");
					tmp=null;
				}
				Log(player,plugin.CHATPREFIX  + " ---- ChatCensor stats ---- ");
				Log(player,plugin.CHATPREFIX  + " Jailed: " + ChatColor.YELLOW + stats[1] + " times");
				Log(player,plugin.CHATPREFIX  + " Kicked: " + ChatColor.YELLOW + stats[2] + " times");
				Log(player,plugin.CHATPREFIX  + " Fined: " + ChatColor.YELLOW + stats[4] + " coin");
				Log(player,plugin.CHATPREFIX  + " Total: " + ChatColor.YELLOW + stats[0] + " times");
				stats=null;
				
			}
			
		}
		action=null;
		player=null;
		wordTarget=null;
		action=null;
		
	}
}
