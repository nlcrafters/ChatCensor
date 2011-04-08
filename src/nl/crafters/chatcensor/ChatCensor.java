package nl.crafters.chatcensor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin; 
import org.bukkit.plugin.PluginManager; 
import org.bukkit.util.config.Configuration;
import com.nijiko.permissions.*;
import com.nijikokun.bukkit.Permissions.*;
import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.dataholder.worlds.WorldsHolder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import com.matejdro.bukkit.jail.*;

/** 
 * ChatCensor for Bukkit
 * 
 * @author ddj
 *
 */
public class ChatCensor extends JavaPlugin{
    private final ChatCensorPlayerListener playerListener = new ChatCensorPlayerListener(this);
	static String maindirectory = "plugins/ChatCensor/";
	static File configFile = new File(maindirectory + "Config.yml");
	
	public static PermissionHandler Permissions=null;
	public WorldsHolder wh = null;
	
	public int pSystem = 0;
	public boolean useiConomy = false;
	public boolean useAutoKick = false;
	public boolean useAutoJail = false;
	public boolean useJailMute = false;
	public boolean useKickWhenBroke = false;
	public double useIncreaseJailTime=0.0;
	public boolean useRegex = false;
	
	public static boolean broadcasttoconsole = true;
	

	public Database db;
	public boolean useMYSQL = false;
	public String mysqlDatabase = "minecraft";
	public String mysqlUser = "root";
	public String mysqlPassword = "root";
	public String mysqlHost = "localhost";
	public String mysqlPort = "3306";
	public Jail jail = null;

	public Censor c;
	public int PlayerFine = 50;
	public int JailTime = 2;
	public final static char DEG = '\u00A7';
	
	public Configuration config;
	
	public String CHATPREFIX = ChatColor.RED + "[NLC]" + ChatColor.AQUA;
	
	boolean setupPermissions() {
		
		// first search for groupmanager
		Plugin p = this.getServer().getPluginManager().getPlugin("GroupManager");
		pSystem =0;
		if (p!=null) {
			if (!this.getServer().getPluginManager().isPluginEnabled("GroupManager")) {
				this.getServer().getPluginManager().enablePlugin(p);
			}
			GroupManager gm = (GroupManager) p;
			wh = gm.getWorldsHolder();
			pSystem = 1;
			AddLog("GroupManager system found");
		}
		else 
		{
			p = this.getServer().getPluginManager().getPlugin("Permissions");
			if(ChatCensor.Permissions == null) {
			    if(p!= null) {
			    	ChatCensor.Permissions = ((Permissions)p ).getHandler();
			    	pSystem = 2;
			    	AddLog("Permission system found");
			    } 
			}
		}
		if (pSystem==0) {
	    	AddLog("Permission/Groupmanager system found or not enabled. Disabling plugin.");
	    	this.getServer().getPluginManager().disablePlugin(this);
	    	return false;
		}
		return true;
		
	}

	public boolean checkiConomy() {
	        Plugin test = this.getServer().getPluginManager().getPlugin("iConomy");
	        if (test != null) {
	            return true;
	        } else {
	            return false;
	        }
    }    
	public boolean checkJail() {
		
	        Plugin test = this.getServer().getPluginManager().getPlugin("Jail");
	        if (test != null) {
	        	jail = (Jail) test;
	            return true;
	        } else {
	            return false;
	        }
	}    
	public String getMessage(String configMessage,Player p) {
		config.load();
		String t = config.getString(configMessage);
		String playerName = "";
		if (p!=null) {
			//playerName = p.getName();
			playerName = p.getDisplayName();
			t = t.replaceAll("@playername@", getColor("labelsandcolors.marked-words-color") + playerName + getColor("labelsandcolors.chat-message-color"));
		}
		Integer jt = JailTime;
		
		try {
			jt = db.GetJailTime(p,JailTime);
		} catch (Exception e) {
		} 
		t = t.replaceAll("@fine-amount@", getColor("labelsandcolors.marked-words-color") + Integer.toString(PlayerFine) + getColor("labelsandcolors.chat-message-color")  );
		t = t.replaceAll("@jailtime@", getColor("labelsandcolors.marked-words-color") + Integer.toString(jt) + getColor("labelsandcolors.chat-message-color")  );
		t = t.replaceAll("@autojail-status@", getColor("labelsandcolors.marked-words-color") + Boolean.toString(useAutoJail) + getColor("labelsandcolors.chat-message-color"));
		t = t.replaceAll("@autokick-status@", getColor("labelsandcolors.marked-words-color") + Boolean.toString(useAutoKick) + getColor("labelsandcolors.chat-message-color"));
		t = t.replaceAll("@autopayfine-status@", getColor("labelsandcolors.marked-words-color") + Boolean.toString(useiConomy) + getColor("labelsandcolors.chat-message-color"));
		t = t.replaceAll("@jailmute-status@", getColor("labelsandcolors.marked-words-color") + Boolean.toString(useJailMute) + getColor("labelsandcolors.chat-message-color"));		
		return t;
	}
	public String getColor(String configColor) 
	{
		String col =config.getString(configColor); 
		String out = "";
		if (col.length()==2) {
			out = DEG + col.substring(1);
			return out;
		}
		return col;
	}
	public void reload() {
    	config = this.getConfiguration();
    	config.load();
		PlayerFine = config.getInt("settings.fine-amount", -1);
		CHATPREFIX = getColor("labelsandcolors.chat-prefix-color") + getMessage("labelsandcolors.chat-prefix",null) + " " + getColor("labelsandcolors.chat-message-color");
		if (checkiConomy() ) {
			useiConomy = config.getBoolean("toggles.use-autofine", false);
			//AddLog("** iConomy plugin found!");
		}
		if (checkJail() ) {
			useAutoJail = config.getBoolean("toggles.use-autojail",false);
			useJailMute = config.getBoolean("toggles.use-jailmute", false);
			//AddLog("** Jail plugin found!");
		}
		useAutoKick = config.getBoolean("toggles.use-autokick", false);
		useKickWhenBroke = config.getBoolean("settings.kick-when-broke", false);
		useRegex = config.getBoolean("settings.use-regex", false);
		PlayerFine = config.getInt("settings.fine-amount", -1);
		JailTime = config.getInt("settings.jail-time",-1);
		
		AddLog("Configuration reloaded");
	}
    public void loadConfig() {
    	
    	config = this.getConfiguration();
    	config.load();
		PlayerFine = config.getInt("settings.fine-amount", -1);
		CHATPREFIX = getColor("labelsandcolors.chat-prefix-color") + 
		             getMessage("labelsandcolors.chat-prefix",null) +
		             getColor("labelsandcolors.chat-message-color");
		if (checkiConomy() ) {
			useiConomy = config.getBoolean("toggles.use-autofine", false);
			//AddLog("** iConomy plugin found!");
		}
		if (checkJail() ) {
			useAutoJail = config.getBoolean("toggles.use-autojail",false);
			useJailMute = config.getBoolean("toggles.use-jailmute", false);
			//AddLog("** Jail plugin found!");
		}
		
		// 
		useMYSQL = (config.getString("database.type").equalsIgnoreCase("mysql") );
		if (useMYSQL) {
			AddLog("** Using mysql database");
		}
		else {
			AddLog("** Using sqlite database");
		}
		mysqlDatabase = config.getString("database.mysql.name");
		mysqlUser = config.getString("database.mysql.username");
		mysqlPassword = config.getString("database.mysql.password");
		mysqlHost = config.getString("database.mysql.hostname");
		mysqlPort = config.getString("database.mysql.port");
		useAutoKick = config.getBoolean("toggles.use-autokick", false);
		useKickWhenBroke = config.getBoolean("settings.kick-when-broke", false);
		PlayerFine = config.getInt("settings.fine-amount", -1);
		JailTime = config.getInt("settings.jail-time",-1);
		useRegex = config.getBoolean("settings.use-regex", false);
		useIncreaseJailTime = config.getDouble("settings.auto-increase-jail-time",0.0);
		if (useRegex) {
			AddLog("** Using regex!!");
		}
		AddLog("iConomy/autokick/autojail/mute/fine:");
		AddLog(useiConomy + "/" + useAutoKick + "/" + useAutoJail+ "(" + JailTime + "min)/" + useJailMute + "/" + PlayerFine);
    }
    public void AddLog(String message) {
    	Logger.getLogger("Minecraft").info("[ChatCensor] " + message);
    }
    public void saveConfig() {
    	//config = this.getConfiguration();
    	//etProperty("toggles.use-autofine", useiConomy);
    	//etProperty("toggles.use-autokick", useAutoKick);
    	//etProperty("toggles.use-autojail", useAutoJail);
    	//etProperty("toggles.use-jailmute", useJailMute);
    	//etProperty("settings.fine-amount", PlayerFine);			
    	//etProperty("settings.jail-time", JailTime);
    	//ave();
    }

	public void playerFined(Player player) {
		String msg =getColor("labelsandcolors.chat-prefix-color") + getMessage("labelsandcolors.chat-prefix",null) + getColor("labelsandcolors.chat-message-color") + getMessage("broadcasttext.player-fined-message",player);
		this.getServer().broadcastMessage(msg);
		if (broadcasttoconsole) {
			AddLog(msg);
		}
	}
	public void playerJailed(Player player) {
		String msg =getColor("labelsandcolors.chat-prefix-color") + getMessage("labelsandcolors.chat-prefix",null) + getColor("labelsandcolors.chat-message-color") + getMessage("broadcasttext.player-jailed-message",player);
		this.getServer().broadcastMessage(msg);
		if (broadcasttoconsole) {
			AddLog(msg);
		}
	}
	
	public void playerKicked(Player player) {
		String msg =getColor("labelsandcolors.chat-prefix-color") + getMessage("labelsandcolors.chat-prefix",null) + getColor("labelsandcolors.chat-message-color") + getMessage("broadcasttext.player-kicked-message",player);
		this.getServer().broadcastMessage(msg);
		if (broadcasttoconsole) {
			AddLog(msg);
		}
	}

	private void CheckConfig() {
		getDataFolder().mkdirs();
		
		String name = "config.yml";
		File actual = new File(getDataFolder(),name);
		if (!actual.exists()) {
		    InputStream input =this.getClass().getResourceAsStream("/config.yml");
		    if (input != null) {
		        FileOutputStream output = null;
	
		        try {
		            output = new FileOutputStream(actual);
		            byte[] buf = new byte[8192];
		            int length = 0;
		            while ((length = input.read(buf)) > 0) {
		                output.write(buf, 0, length);
		            }
		            AddLog("Default configuration file written: " + name);
		        } catch (IOException e) {
		            e.printStackTrace();
		        } finally {
		            try {
		                if (input != null)
		                    input.close();
		            } catch (IOException e) {}
	
		            try {
		                if (output != null)
		                    output.close();
		            } catch (IOException e) {}
		        }
		    }            
		}
	}
	
	@Override
	public void onEnable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		AddLog("Enabling ChatCensor v" + pdfFile.getVersion());
		getCommand("cc").setExecutor(new CommandExecutor() {
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        		playerListener.onCommand(sender, command, label, args);
        	    return true;
            }
        });
		CheckConfig();
		loadConfig();
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_CHAT,this.playerListener,Event.Priority.Lowest,this);
		if (!setupPermissions() ) {
			return;
		}
		db = new Database(this);
		db.CheckDatabase();
		c = new Censor(this);
		AddLog("version "  + pdfFile.getVersion() + " enabled");
	}
	@Override
	public void onDisable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		c = null;
		db = null;
		config = null;
		Permissions = null;
		AddLog("version "  + pdfFile.getVersion() + " disabled");
		pdfFile = null;
	}

}
