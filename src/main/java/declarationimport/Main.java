package declarationimport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.*;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.collections4.*;

import java.io.*;
import java.lang.reflect.Type;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.*;
import org.neo4j.tooling.GlobalGraphOperations;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;



public class Main {
	
	private static final String DB_PATH = "data";
	
	public enum Labels implements Label
	{
		Package,
		Declaration,
		Symbol
    }
	
	private static enum RelationshipTypes implements RelationshipType
	{
	    DECLARATION,
	    MENTIONEDSYMBOL,
	    DECLAREDSYMBOL
	}

	public static void main(String[] args){
		
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
		
		Transaction tx = graphDb.beginTx();
		 try
		 {
			 insertPackage(graphDb,"containers","0.5.5.1");
			 			 
		     tx.success();
		 }
		 finally
		 {
		     tx.close();
		 }

		graphDb.shutdown();
		
		System.out.println("success");
	}
	
	public static void insertPackage(GraphDatabaseService graphDb,String packagename,String versionnumber){
		
	    Node packagenode = graphDb.createNode(Labels.Package);
	    packagenode.setProperty("packagename",packagename);
	    packagenode.setProperty("versionnumber",versionnumber);
		
		File packagepath = new File("packages/" + packagename + "-" + versionnumber + "/");
		
		Collection<File> modulefiles =
				FileUtils.listFiles(packagepath,new SuffixFileFilter(".declarations"),TrueFileFilter.INSTANCE);
		
		for(File modulefile : modulefiles){
			insertModule(graphDb,packagenode,modulefile);
		}
		
	}
	
	public static void insertModule(GraphDatabaseService graphDb,Node packagenode,File modulepath){
		
		Gson gson = new Gson();
		
		Type declarationsType = new TypeToken<Collection<Declaration>>(){}.getType();

		try {

			Collection<Declaration> declarations = gson.fromJson(new FileReader(modulepath), declarationsType);
			for (Declaration declaration : declarations) {
				insertDeclaration(graphDb, packagenode, declaration);
			}

		} catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {

			e.printStackTrace();

		}
	}

	public static void insertDeclaration(GraphDatabaseService graphDb, Node packagenode, Declaration declaration) {
		
		Node declarationnode = graphDb.createNode(Labels.Declaration);
		packagenode.createRelationshipTo(declarationnode, RelationshipTypes.DECLARATION);
		declarationnode.setProperty("declarationast", declaration.declarationast);
		
		for(Symbol usedsymbol : declaration.usedsymbols){
			Node usedsymbolnode = createSymbolNode(graphDb, usedsymbol);
			declarationnode.createRelationshipTo(usedsymbolnode, RelationshipTypes.MENTIONEDSYMBOL);
		}
		
		for(Symbol declaredsymbol : declaration.declaredsymbols){
			Node declaredsymbolnode = createSymbolNode(graphDb, declaredsymbol);
			declarationnode.createRelationshipTo(declaredsymbolnode, RelationshipTypes.DECLAREDSYMBOL);
		}
		
	}

	public static Node createSymbolNode(GraphDatabaseService graphDb, Symbol symbol) {
		
		ResourceIterable<Node> potentialsymbolnodes = graphDb.findNodesByLabelAndProperty(Labels.Symbol, "symbolname", symbol.origin.name);
		Iterator<Node> fittingsymbolnodes = IteratorUtils.filteredIterator(
				potentialsymbolnodes.iterator(), 
				PredicateUtils.andPredicate(new GenreIs(symbol.entity),new ModuleIs(symbol.origin.module)));
		
		if(fittingsymbolnodes.hasNext()){
			
			return fittingsymbolnodes.next();
			
		}else{
			
		    Node symbolnode = graphDb.createNode(Labels.Symbol);
		    symbolnode.setProperty("symbolgenre", symbol.entity);
		    symbolnode.setProperty("symbolmodule", symbol.origin.module);
		    symbolnode.setProperty("symbolname", symbol.origin.name);
		    
		    return symbolnode;
			
		}
	    
	}

	public static class GenreIs implements Predicate<Node> {

		public String symbolgenre;

		GenreIs(String symbolgenre) {
			this.symbolgenre = symbolgenre;
		}

		public boolean evaluate(Node symbolnode) {
			return symbolnode.getProperty("symbolgenre") == symbolgenre;
		}

	}

	public static class ModuleIs implements Predicate<Node> {

		public String symbolmodule;

		ModuleIs(String symbolmodule) {
			this.symbolmodule = symbolmodule;
		}

		public boolean evaluate(Node symbolnode) {
			return symbolnode.getProperty("symbolmodule") == symbolmodule;
		}

	}
	

}


