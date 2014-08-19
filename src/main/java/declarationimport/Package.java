package declarationimport;

import java.util.Collection;
import java.util.Collections;

public class Package {
	
	public Package(String packagename, String packageversion) {
		this.packagename = packagename;
		this.packageversion = packageversion;
		this.dependencies = Collections.emptyList();
		this.nextversion = null;
	}
	public String packagename;
	public String packageversion;
	public Collection<Dependency> dependencies;
	public NextVersion nextversion;

}
