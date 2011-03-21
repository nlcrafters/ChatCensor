package nl.crafters.chatcensor;
import java.io.*;


import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;

public class Censor {
	static String maindirectory = "plugins/ChatCensor/";
	static File wordFile = new File(maindirectory + "Censor.txt");
	static Boolean WITHCOLORS = true;
	public ChatCensor plugin;
    public static List<String> censored = new ArrayList<String>();

    // Constructor, calls method which fills censored string array 
    public Censor(ChatCensor instance) {
    	plugin = instance;
    	loadWords();
    }
	public void Log(Player p, String message) {
		if (p==null) {
			plugin.AddLog(message);
		}
		else {
			p.sendMessage(plugin.CHATPREFIX + " " + message);
		}
	}
    
    // Checks chatline for forbidden words 
    public String isForbidden(String in) 
    {
    	String out = in;
    	boolean badnews = false;
    	for (String s : censored) {
    		if (s.length() > 2) {
	    	    if (in.toLowerCase().contains(s.toLowerCase())) {
	    	    	badnews = true;
	    	    	if (WITHCOLORS) {
	    	    		out = out.toLowerCase().replace(s.toLowerCase(),"@bananen@");
	    	    	}
	    	    	else {
	        	    	out = out.toLowerCase().replace(s.toLowerCase(),"@bananen@");
	    	    	}
	    	    }
    		}
    	}
    	if (badnews) {
    		return "0" + out;
    	}
    	else 
    	{
    		return "1" + out;
    	}
    }
    public boolean wordExists(String w) {
    	return censored.contains(w.toLowerCase());
    }
    public boolean addWord(Player p,String w) {
    	if (plugin.db.AddWord(w)) {
    		Log(p,ChatColor.GREEN + "Added " + w + " to word list");
    		censored.add(w);
    		return true;
    	}
    	else {
			Log(p,ChatColor.RED + "Could not add word " + w + " to wordlist!");
    	}
    	return true;
    }
    public boolean removeWord(Player p,String w) {
    	if (plugin.db.DeleteWord(w)) {
    		Log(p,ChatColor.GREEN + "Removed one word from word list");
    		censored.remove(w);
    		return true;
    	}
    	else {
    		Log(p,ChatColor.RED + "Word is not in list!");
    		return false;
    	}
    }
    public void listWords(Player p, String sstring) {
    	String list = "";
    	int c = 0;
    	for (String s : censored) {
    		if (s.contains(sstring) || sstring.equalsIgnoreCase("")) {
    			c++;
	    		if (list.equalsIgnoreCase("")) {
	    			list+= s;
	    		}
	    		else {
	    			list = list + "-" + s;
	    		}
    		}
    	}
    	if (sstring.equalsIgnoreCase("")) {
    		Log(p,ChatColor.AQUA + "List contains " + censored.size() + " words:");
    		Log(p,ChatColor.AQUA + list);
    		Log(p,ChatColor.AQUA + "List contains " + censored.size() + " words:");
    	}
    	else {
    		Log(p,ChatColor.AQUA + "Words found:" + c);
    		Log(p,ChatColor.AQUA + list);
    	}
    }
    
    // Loads forbidden words from Censor.txt into string array
    // Also Creates plugin/ChatCensor folder and empty Censor.txt file in it
    public void loadWords() {
    	//plugin.AddLog("Start loading words");
    	censored.clear();
    	Integer i = 0;
    	String list[] = plugin.db.GetWords();
    	if (list.length<=1) {
    		return;
    	}
        try {
        	for (String word : list) {
        		censored.add(word);
        		i++;
        	}
            plugin.AddLog("Loaded " + i + " words");
            i = 0;
        } catch (Exception e) {
        	plugin.AddLog("Error loading the wordlist");
        }
    }

}
