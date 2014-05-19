package declarationimport;

import java.util.Collection;

import org.apache.commons.io.*;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.*;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.*;
import org.neo4j.tooling.GlobalGraphOperations;



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
		
		System.out.println(modulepath.getPath());
		
	}

}
