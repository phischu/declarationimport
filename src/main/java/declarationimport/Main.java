package declarationimport;

import org.apache.commons.io.*;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Collection;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.*;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;



public class Main {
	
	public static final String PREFIX = "/home/pschuster/Projects/symbols/";
	public static final String DB_PATH = PREFIX + "data";
	public static final String PACKAGEINFO_PATH = PREFIX + "packageinfo";
	
	
	public enum Labels implements Label
	{
		Package,
		Declaration,
		Symbol,
		TypeSignature
    }
	
	private static enum RelationshipTypes implements RelationshipType
	{
		DEPENDENCY,
		NEXTVERSION,
	    DECLARATION,
	    MENTIONEDSYMBOL,
	    DECLAREDSYMBOL,
	    INSTALLATION
	}

	public static void main(String[] args) {

		GraphDatabaseService graphDb = new GraphDatabaseFactory()
				.newEmbeddedDatabase(DB_PATH);

		Transaction tx = graphDb.beginTx();
		try {

			graphDb.schema().indexFor(Labels.Package).on("packagename").create();
			graphDb.schema().indexFor(Labels.Symbol).on("symbolname").create();

			tx.success();
		} finally {
			tx.close();
		}

		Gson gson = new Gson();

		Type packagesType = new TypeToken<Collection<Package>>() {}.getType();

		try {
			Collection<Package> packages;
			packages = gson.fromJson(new FileReader(PACKAGEINFO_PATH), packagesType);
			for (Package packag : packages) {

				tx = graphDb.beginTx();
				try {
					insertPackage(graphDb, packag);

					tx.success();
				} finally {
					tx.close();
				}
			}
		} catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
			e.printStackTrace();
		}

		graphDb.shutdown();

		System.out.println("success");
	}
	
	public static void insertPackage(GraphDatabaseService graphDb,Package packag){
		
		final String packageid = packag.packagename + "-" + packag.packageversion;
		
		System.out.println(packageid);
		
		Node packagenode = createPackageNode(graphDb, packag.packagename, packag.packageversion);
		
		for(Dependency dependency : packag.dependencies){
			
			Node dependencynode = createPackageNode(graphDb,dependency.dependencyname,dependency.dependencyversion);
			
			packagenode.createRelationshipTo(dependencynode, RelationshipTypes.DEPENDENCY);
			
		}
		
		if(packag.nextversion != null) {
			
			Node nextversionnode = createPackageNode(graphDb,packag.packagename,packag.nextversion.nextversion);
			
			Relationship nextVersionRelationship =  packagenode.createRelationshipTo(nextversionnode, RelationshipTypes.NEXTVERSION);
			nextVersionRelationship.setProperty("change", packag.nextversion.change);
			
		}

		File packagepath = new File(PREFIX + "packages/lib/x86_64-linux-haskell-declarations-0.1/" + packageid + "/");
		
		if(!packagepath.exists()) return;
		
		Collection<File> modulefiles = 
				FileUtils.listFiles(packagepath,new SuffixFileFilter(".declarations"),TrueFileFilter.INSTANCE);
		
		for(File modulefile : modulefiles){
			insertModule(graphDb,packagenode,modulefile);
		}
		
		insertInstallation(graphDb,packagenode,packageid);
		
	}
	
	private static void insertInstallation(GraphDatabaseService graphDb, Node packagenode, String packageid) {
		
		Gson gson = new Gson();

		Type dependenciesType = new TypeToken<Collection<Dependency>>() {}.getType();
		
		String installationpath = PREFIX + "installations/" + packageid;

		try {
			Collection<Dependency> dependencies = gson.fromJson(new FileReader(installationpath), dependenciesType);

			for(Dependency dependency : dependencies){
				
				Node installationNode = createPackageNode(graphDb, dependency.dependencyname, dependency.dependencyversion);
				packagenode.createRelationshipTo(installationNode, RelationshipTypes.INSTALLATION);
				
			}
		} catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
			e.printStackTrace();
		}		
	}

	public static Node createPackageNode(GraphDatabaseService graphDb,String packagename,String packageversion){
		
		ResourceIterable<Node> potentialpackagenodes = graphDb.findNodesByLabelAndProperty(Labels.Package, "packagename", packagename);

		for (Node potentialpackagenode : potentialpackagenodes) {

			if (potentialpackagenode.getProperty("packageversion").equals(packageversion)) {
				return potentialpackagenode;
			}

		}
		
		Node packagenode = graphDb.createNode(Labels.Package);
	    packagenode.setProperty("packagename",packagename);
	    packagenode.setProperty("packageversion",packageversion);
	    
	    return packagenode;
		
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
		
		Node declarationnode = graphDb.createNode();
		if(declaration.declarationgenre.equals("TypeSignature")){
			declarationnode.addLabel(Labels.TypeSignature);
		}else{
			declarationnode.addLabel(Labels.Declaration);
		}
		packagenode.createRelationshipTo(declarationnode, RelationshipTypes.DECLARATION);
		declarationnode.setProperty("declarationast", declaration.declarationast);
		declarationnode.setProperty("declarationgenre", declaration.declarationgenre);
		
		for(Symbol usedsymbol : declaration.mentionedsymbols){
			Node usedsymbolnode = createSymbolNode(graphDb, usedsymbol);
			declarationnode.createRelationshipTo(usedsymbolnode, RelationshipTypes.MENTIONEDSYMBOL);
		}
		
		for(Symbol declaredsymbol : declaration.declaredsymbols){
			Node declaredsymbolnode = createSymbolNode(graphDb, declaredsymbol);
			declarationnode.createRelationshipTo(declaredsymbolnode, RelationshipTypes.DECLAREDSYMBOL);
		}
		
	}

	public static Node createSymbolNode(GraphDatabaseService graphDb, Symbol symbol) {

		ResourceIterable<Node> potentialsymbolnodes = graphDb.findNodesByLabelAndProperty(Labels.Symbol, "symbolname", symbol.name);

		for (Node potentialsymbolnode : potentialsymbolnodes) {

			if (potentialsymbolnode.getProperty("symbolgenre").equals(symbol.entity) &&
				potentialsymbolnode.getProperty("symbolmodule").equals(symbol.module)) {
				return potentialsymbolnode;
			}

		}

		Node symbolnode = graphDb.createNode(Labels.Symbol);
		symbolnode.setProperty("symbolgenre", symbol.entity);
		symbolnode.setProperty("symbolmodule", symbol.module);
		symbolnode.setProperty("symbolname", symbol.name);

		return symbolnode;

	}

}



