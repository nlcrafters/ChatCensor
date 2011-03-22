package nl.crafters.chatcensor;
import java.io.*;


import org.bukkit.ChatColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

public class Censor {
	static String maindirectory = "plugins/ChatCensor/";
	static File wordFile = new File(maindirectory + "Censor.txt");
	static Boolean WITHCOLORS = true;
	public final static char DEG = '\u00A7';
	public ChatCensor plugin;
    public static List<String> censored = new ArrayList<String>();
	private Random rGen  = new Random();

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
    			String strToMatch = s.toLowerCase();
    			boolean domatch = false;
    			if (plugin.useRegex) { 
    				// anywhere
    				if (strToMatch.startsWith("*") && strToMatch.endsWith("*")) { 
    					strToMatch = "(?i).*" + strToMatch.substring(1, strToMatch.length() -1) + ".*";
    				}
    				// starts with
    				else if (strToMatch.startsWith("*") && !strToMatch.endsWith("*")) {
    					strToMatch = "(?i)" + strToMatch.substring(1) + ".*";
    				}
    				// ends with
    				else if (!strToMatch.startsWith("*") && strToMatch.endsWith("*")) {
    					strToMatch = "(?i).*" + strToMatch.substring(0,strToMatch.length()-1);
    				} 
    				else {
    					strToMatch = "(?i)" + strToMatch;
    				}
    			}
    			else {
    				strToMatch = "(?i).*" + s.toLowerCase() + ".*";
    			}
    	        Pattern pattern = Pattern.compile(strToMatch);
    			Matcher matcher = pattern.matcher(out);
    			domatch = matcher.matches();
    			
    			
    			plugin.AddLog("Regex:" + plugin.useRegex +  " str:" + strToMatch +  " result:" + domatch);
    			
    			
    			if (domatch) {
	    	    	badnews = true;
					String replacement = plugin.getColor("labelsandcolors.replace-color")  + getRandomWord() + ChatColor.WHITE;
					plugin.AddLog("OUT:" + out);
					plugin.AddLog("machter:" + matcher.replaceAll(replacement));
					
					plugin.AddLog("OUT:" + out);
					plugin.AddLog("ReplaceAll:" + out.replaceAll(strToMatch,replacement));
					plugin.AddLog("OUT:" + out);
					
					
					out = replace(out,strToMatch,replacement);
					//out = out.replaceAll(strToMatch, replacement);
    	    		//out = out.toLowerCase().replace(s.toLowerCase(),replacement);
    				//out= out.replaceAll(ChatColor.WHITE + "" + ChatColor.YELLOW, "");
    				out = cleanMsgEnding(out);
    				plugin.AddLog("OUT:" + out);
    	    		
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
    static String replace(String inputStr, String patternstring, String replacementStr) {
    	// Compile regular expression
        Pattern pattern = Pattern.compile(patternstring);

        // Replace all occurrences of pattern in input
        Matcher matcher = pattern.matcher(inputStr);
        String output = matcher.replaceAll(replacementStr);
        return output;
    }
    
	// Clean up chat message to prevent client crashes
	protected static String cleanMsgEnding (String msg) {
		
		while (msg.length() > 0) {
			if (msg.endsWith(String.valueOf(DEG)) || msg.endsWith(" ")) {
				msg = msg.substring(0, msg.length()-1);
			} else if (msg.length() >= 2 && msg.charAt(msg.length() - 2) == DEG) {
				msg = msg.substring(0, msg.length()-2);
			} else {
				break;
			}
		}
		return msg;
	}
    
	private String getRandomWord() {
		String wlist = plugin.getMessage("labelsandcolors.replace-word", null);
		String replacements[] = wlist.split(",");
		String replacementChoice = "";
		if (replacements.length<=1)
		{
			replacementChoice = wlist;
		}
		else {
			int item = rGen.nextInt((replacements.length));
			replacementChoice = replacements[item];
		}
		return replacementChoice;
		
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
