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
	
	private static enum RelationshipTypes implements RelationshipType
	{
	    DEPENDENCY
	}

	public static void main(String[] args){
		
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
		
		Transaction tx = graphDb.beginTx();
		 try
		 {
			 insertPackage("containers","0.5.5.1");
			 			 
		     tx.success();
		 }
		 finally
		 {
		     tx.close();
		 }

		graphDb.shutdown();
		
		System.out.println("success");
	}
	
	public static void insertPackage(String packagename,String versionnumber){
		
		File packagepath = new File("packages/" + packagename + "-" + versionnumber + "/");
		
		Collection<File> modulefiles =
				FileUtils.listFiles(packagepath,new SuffixFileFilter(".declarations"),TrueFileFilter.INSTANCE);
		
		for(File modulefile : modulefiles){
			System.out.println(modulefile.getPath());
		}
		
	}
	
	public static void insertModule(String path){
		
	}

}
