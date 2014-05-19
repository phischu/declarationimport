package declarationimport;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class Main {
	
	private static final String DB_PATH = "data";

	public static void main(String[] args){
		
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
		graphDb.shutdown();
		
		System.out.println("hallo");
	}

}
