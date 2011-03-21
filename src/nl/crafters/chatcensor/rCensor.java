package nl.crafters.chatcensor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class rCensor {
	
	private static Logger logger = Logger.getLogger(Censor.class.getName());
	
	private static ArrayList<FilterWord> filter = new ArrayList<FilterWord>();
	
	public static void load() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader("data/censor.txt"));
			String line;
			while((line = reader.readLine()) != null) {
				if(line.equals(""))
					continue;
				String[] split = line.split(":");
				String word = split[0];
				String replacement = "";
				if(split.length == 2) {
					replacement = split[2];
				} else if(split.length == 1) {
					for(int i = 0; i < word.length(); i++) {
						replacement += "*";
					}
				}
				filter.add(new FilterWord(word, replacement, Pattern.compile("(.*)(?i)"+split[0]+"(.*)")));				
			}
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Error loading censored words!", e);
		}
	}
	
	public String filter(String s) {
		for(FilterWord word : filter) {
			if(word.matches(s)) {
				s = word.filter(s);
			}
		}
		return s;
	}
	
	public static class FilterWord {
		
		private String replacement;
		
		private String word;

		private Pattern pattern;

		public FilterWord(String string, String replacement, Pattern pattern) {
			this.word = string;
			this.replacement = replacement;
			this.pattern = pattern;
		}
		
		public String filter(String s) {
			return s.replaceAll("(?i)"+word, replacement);
		}

		public String getWord() {
			return word;
		}
		
		public String getReplacement() {
			return replacement;
		}
		
		public boolean matches(String s) {
			return pattern.matcher(s).matches();
		}
	}
}