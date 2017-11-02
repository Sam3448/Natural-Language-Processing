import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.*;

public class Parse {
	public static void main(String[] args) throws Exception 
	{
		if(args.length != 2){
			System.out.println(
	                "\nPrints the parse tree of input file.\n\n" +
	                        "Usage:   java parse grammar_file sentences_file \n");
			return;
		}
		Rule S = handleCFG(args[0]);
		List<String> texts = handleSen(args[1]);
		for (String text : texts) {
			try{
				//System.out.printf("Parse trees for '%s'\n", text);
				//System.out.println("===================================================");
				Parser p = new Parser(S, text);
				p.printTree();
			}catch(Exception e){
				//e.printStackTrace();
				System.out.println("NONE");
				continue;
			}
		}
	}
	
	public static Rule handleCFG(String s) throws Exception{
		HashMap<String, Rule> rules = new HashMap<>();
		List<String[]> storeFile = new ArrayList<>();
		File cfg = new File(s);
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(cfg)));
		String line = "";
		while((line = br.readLine()) != null){
			String[] r = line.split("\t");
			rules.put(r[1], new Rule(r[1]));
			storeFile.add(r);
		}
		br.close();
		for(String[] r : storeFile){
			String rule_from = r[1];
			List<String> rule_to = new ArrayList<>();
			if(!r[2].contains(" ")) rule_to.add(r[2]);
			else rule_to.addAll(Arrays.asList(r[2].split(" ")));
			
			Rule cur = rules.get(rule_from);
			Object[] temp = new Object[rule_to.size()];
			for(int i = 0; i < rule_to.size(); i++){
				String element = rule_to.get(i);
				if(rules.containsKey(element)) temp[i] = rules.get(rule_to.get(i));
				else temp[i] = rule_to.get(i);
			}
			cur.add(new Production(temp, Math.abs(Math.log(Double.parseDouble(r[0]))/Math.log(2))));
		}
		return rules.get("ROOT");
	}
	
	public static List<String> handleSen(String s) throws Exception{
		File sen = new File(s);
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(sen)));
		String line = "";
		//int count = 0;
		List<String> texts = new ArrayList<>();
		while((line = br.readLine()) != null){
			if(line.length() > 0)texts.add(line);
		}
		br.close();
		return texts;
	}
	
	public interface Upper{} //upper level of terminal, production
	
	public static class Terminal implements Upper{ // for words
		String val;
		public Terminal(String val){
			this.val = val;
		}
		public String getValue(){
			return this.val;
		}
		public boolean equals(Object comp){
			if(comp == null) return false;
			if(this == comp) return true;
			if(comp instanceof Terminal) return ((Terminal) comp).val.equals(this.val);
			if(comp instanceof String) return this.val.equals((String)comp);
			return false;
		}
	}
	public static class Rule implements Upper, Iterable<Production>{
		String name;
		ArrayList<Production> productions;
		public Rule(String name, Production... productions){
			this.name = name;
			this.productions = new ArrayList<Production>(Arrays.asList(productions));
		}
		public void add(Production productions) {
			this.productions.add(productions);
		}
		public int getSize(){
			return productions.size();
		}
		public Production getProduction(int index){
			return productions.get(index);
		}
		public Iterator<Production> iterator() {
			return productions.iterator();
		}
		public boolean equals(Object comp){
			if(comp == null || comp.getClass() != this.getClass()) return false;
			if(this == comp) return true;
			Rule compRule = (Rule) comp;
			return this.name.equals(compRule.name) && this.productions.equals(compRule.productions);
		}
		public String toString(){
			String s = this.name + "->";
			if (!productions.isEmpty()) {
				for (int i = 0; i < productions.size() - 1; i++) {
					s += productions.get(i) + "(" + productions.get(i).weight + ")" + " | ";
				}
				s += productions.get(productions.size() - 1) + "(" + productions.get(productions.size() - 1).weight + ")";
			}
			return s;
		}
	}
	
	public static class Production implements Upper{
		List<Upper> terminals;
		List<Rule> rules; 
		double weight;
		public Production(Upper[] terms, double weight){
			this.weight = weight;
			terminals = Arrays.asList(terms);
			this.rules = getRules();
		}
		public Production(Object[] terms, double weight){
			this.weight = weight;
			this.terminals = new ArrayList<Upper>();
			for (Object item : terms) {
				if (item instanceof String) {
					this.terminals.add(new Terminal((String)item));
				}
				else if (item instanceof Upper) {
					this.terminals.add((Upper)item);
				}
			}
			this.rules = getRules();
		}
		public int getSize(){
			return terminals.size();
		}
		public Upper getProduction(int index){
			return terminals.get(index);
		}
		private List<Rule> getRules(){
			ArrayList<Rule> rules = new ArrayList<Rule>();
			for (Upper term : terminals) {
				if (term instanceof Rule) {
					rules.add((Rule)term);
				}
			}
			return rules;
		}
		public boolean equals(Object comp){
			if(comp == null || comp.getClass() != getClass()) return false;
			if(this == comp) return true;
			Production compProduction = (Production)comp;
			return this.terminals.equals(compProduction.terminals);
		}
		public String toString(){
			String s = "";
			if (!terminals.isEmpty()) {
				for (int i = 0; i < terminals.size() - 1; i++) {
					Upper t = terminals.get(i);
					if (t instanceof Rule) s += ((Rule)t).name;
					else s += ((Terminal)t).val;
					s += " ";
				}
				Upper t = terminals.get(terminals.size() - 1);
				if (t instanceof Rule) s += ((Rule)t).name;
				else s += ((Terminal)t).val; 
			}
			return s;
		}
	}
	
	public static class TableState{
		String name;
		Production production;
		int dotIndex;
		double weight;
		TableColumn startCol, endCol;
		public TableState(String name, Production production, int dotIndex, double weight, TableColumn startCol){
			this.name = name;
			this.production = production;
			this.dotIndex = dotIndex;
			this.weight = weight;
			this.startCol = startCol;
			this.endCol = null;
		}
		public boolean isCompleted() {
			return dotIndex >= production.getSize();
		}
		public Upper getNextTerm() {
			if (isCompleted()) return null;
			return production.getProduction(dotIndex);
		}
		public boolean equals(Object other){
			if(other == null || other.getClass() != getClass()) return false;
			if(this == other) return true;
			TableState compTableState = (TableState)other;
			return this.name.equals(compTableState.name) && this.production.equals(compTableState.production) 
					//&& compTableState.weight == weight 
					&& dotIndex == compTableState.dotIndex && startCol == compTableState.startCol;
		}
		public String toString(){
			String s = "";
			for (int i = 0; i < production.getSize(); i++) {
				if (i == dotIndex) s += "\u00B7 ";
				Upper t = production.getProduction(i);
				if (t instanceof Rule) s += ((Rule)t).name;
				else s += ((Terminal)t).val;
				s += " ";
			}
			if (dotIndex == production.getSize()) s += "\u00B7";
			return String.format("%-6s -> %-20s [%d-%d] %.2f", name, s, startCol.index, endCol.index, weight);
		}
		public int hashCode(){
			return this.name.hashCode() + this.production.hashCode() + this.dotIndex + this.startCol.index;
		}
	}
	
	public static class TableColumn implements Iterable<TableState>{
		String token;
		int index;
		List<TableState> states;
		
		public TableColumn(int index, String token){
			this.index = index;
			this.token = token;
			this.states = new ArrayList<>();
		}
		public TableState insertState(TableState state){
			int index = states.indexOf(state);
			if(index > -1){
				TableState s = states.get(index);
				s.weight = Math.min(s.weight, state.weight);
				return s;
			}
			state.endCol = this;
			TableState tmp = null;
			for(TableState s : states){
				if(s.name.equals(state.name) && s.startCol == state.startCol && s.endCol == state.endCol
						&& s.dotIndex == s.production.getSize() && state.dotIndex == state.production.getSize()){
					if(state.weight < s.weight){
						states.set(states.indexOf(s), state);
						return state;
					}
					else
						tmp = s;
				}
			}
			if(tmp == null){
				states.add(state);
				return state;
			}
			return tmp;
		}
		public int getSize() {
			return states.size();
		}
		public TableState getState(int index) {
			return states.get(index);
		}
		private class ModifiableIterator implements Iterator<TableState>
		{
			private int i = 0;
			public boolean hasNext() {
				return i < states.size();
			}
			public TableState next() {
				TableState st = states.get(i);
				i++;
				return st;
			}
			public void remove() {
			}
		}
		public Iterator<TableState> iterator(){
			return new ModifiableIterator();
		}
		public void print(PrintStream out, boolean showUncompleted){
			out.printf("[%d] '%s'\n", index, token);
			out.println("=======================================");
			for (TableState state : this) {
				if (!state.isCompleted() && !showUncompleted) {
					continue;
				}
				out.println(state);
			}
			out.println();
		}
	}
	
	public static class Parser{
		TableColumn[] columns;//the whole table
		TableState finalState = null;
		public Parser(Rule startRule, String text) throws Exception{
			String[] tokens = text.split(" ");
			columns = new TableColumn[tokens.length + 1];
			columns[0] = new TableColumn(0, "");
			for (int i = 1; i <= tokens.length; i++) {
				columns[i] = new TableColumn(i, tokens[i-1]);
			}
			finalState = parse(startRule);
			if (finalState == null) {
				throw new Exception();
			}
		}
		private static final String GAMMA_RULE = "ROOT";      // "\u0194"
		
		protected TableState parse(Rule startRule){
			columns[0].insertState(new TableState(GAMMA_RULE, new Production(new Rule[]{startRule}, 0), 0, 0, columns[0]));
			for (int i = 0; i < columns.length; i++) {
				TableColumn col = columns[i];
				for (TableState state : col) {
					if (state.isCompleted()) {
						complete(col, state);
					}
					else {
						Upper term = state.getNextTerm();
						if (term instanceof Rule) predict(col, (Rule)term, i);
						else if (i + 1 < columns.length) scan(columns[i+1], state, ((Terminal)term).val);
					}
				}
				handleEpsilons(col, i);
				/*
				System.out.println("Column: " + i);
				for (TableState state : col) {
					System.out.println(state);
				}*/
			}
			for (TableState state : columns[columns.length - 1]) {
				if (state.name.equals(GAMMA_RULE) && state.isCompleted()) {
					return state;
				}
			}
			return null;
		}
		
		private void scan(TableColumn col, TableState state, String token) {
		    if (token.equals(col.token)) {
			    col.insertState(new TableState(state.name, state.production, state.dotIndex + 1, state.weight, state.startCol));
		    }
		}
		
		private boolean predict(TableColumn col, Rule rule, int colNum) {
			boolean changed = false;
			boolean q4 = true;//left_corner
		    for (Production prod : rule) {
		    	if(!q4){
		    		TableState st = new TableState(rule.name, prod, 0, prod.weight, col);
			    	TableState st2 = col.insertState(st);
			    	changed |= (st == st2);
		    	}
		    	else{
		    		if(!prod.rules.isEmpty() || 
		    				(colNum + 1 < columns.length && ((Terminal)prod.terminals.get(0)).val.equals(columns[colNum + 1].token))){
			    		TableState st = new TableState(rule.name, prod, 0, prod.weight, col);
				    	TableState st2 = col.insertState(st);
				    	changed |= (st == st2);
		    		}
		    	}
		    }
		    return changed;
		}
		
		private boolean complete(TableColumn col, TableState state) {
			boolean changed = false;
		    for (TableState st : state.startCol) {
		    	Upper term = st.getNextTerm();
		    	if (term instanceof Rule && ((Rule)term).name.equals(state.name)) {
		    		TableState st1 = new TableState(st.name, st.production, st.dotIndex + 1, st.weight + state.weight, st.startCol);
		            TableState st2 = col.insertState(st1);
		            changed |= (st1 == st2);
		    	}
		    }
		    return changed;
		}
		
		private void handleEpsilons(TableColumn col, int colNum){
			boolean changed = true;
			while (changed) {
				changed = false;
				for (TableState state : col) {
					Upper term = state.getNextTerm();//get production中.的下一个
					if (term instanceof Rule) {
						changed |= predict(col, (Rule)term, colNum);
					}
					if (state.isCompleted()) {
						changed |= complete(col, state);
					}
				}
			}
		}
		
		public void printTree(){
			Stack<TableState> stack = new Stack<>();
			stack.push(finalState);
			String s = "(" + finalState.name + " " + finalState.production + ")";
			TableState p = finalState;
			TableColumn c = p.endCol;
			TableState tmp;
			while(!stack.isEmpty() && c.index > 0){
				for(int i=c.states.indexOf(p)-1; i>0; i--){
					tmp = c.states.get(i);
					if(tmp.startCol.index < stack.peek().startCol.index)
						continue;
					Upper u = stack.peek().production.getProduction(stack.peek().dotIndex-1);
					if(tmp.isCompleted() && (u instanceof Rule && ((Rule)u).name.equals(tmp.name))){
						String ss = " (" + tmp.name + " " + tmp.production + ")";
						s = s.substring(0, s.lastIndexOf(" " + tmp.name))
								+ s.substring(s.lastIndexOf(" " + tmp.name)).replace(" " + tmp.name, ss);
						stack.push(tmp);
					}
				}
				
				TableState curr = c.states.get(0);
				if(curr.production.rules.size() > 0){
					if(curr.dotIndex == curr.production.getSize()){
						String ss = " (" + curr.name + " " + curr.production + ")";
						s = s.substring(0, s.lastIndexOf(" " + curr.name))
								+ s.substring(s.lastIndexOf(" " + curr.name)).replace(" " + curr.name, ss);
					}
					if(curr.dotIndex > 1){
						if(p.equals(curr))
							stack.pop();
						curr.dotIndex -= 1;
						curr.endCol = columns[c.index - 1];
						stack.push(curr);
						p = curr;
						c = p.endCol;
						continue;
					}else{
						curr = stack.pop();
						curr = stack.pop();
						if(curr.dotIndex > 1){
							curr.dotIndex -= 1;
							curr.endCol = columns[c.index - 1];
							stack.push(curr);
							p = curr;
							c = p.endCol;
							continue;
						}
					}
				}
				tmp = stack.pop();
				
				while(((Rule)tmp.production.getProduction(tmp.dotIndex-1)).name.equals(curr.name) && !stack.isEmpty()){
					if(curr.equals(c.states.get(0))){
						String ss = " (" + c.states.get(0).name + " " + c.states.get(0).production + ")";
						s = s.substring(0, s.lastIndexOf(" " + c.states.get(0).name))
								+ s.substring(s.lastIndexOf(" " + c.states.get(0).name)).replace(" " + c.states.get(0).name, ss);
					}
					if(tmp.dotIndex == 1){
						curr = tmp;
						tmp = stack.pop();
					}else{
						tmp.dotIndex -= 1;
						tmp.endCol = columns[c.index - 1];
						tmp.weight -= curr.weight;
						stack.push(tmp);
						break;
					}
				}
				if(stack.isEmpty()) break;
				p = stack.peek();
				c = p.endCol;
				//System.out.println(s);
				//System.out.println(stack.toString());
			}
			System.out.println(s);
			System.out.println(finalState.weight);
		}
	}
}
