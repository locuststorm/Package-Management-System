package model.database;

import util.FileIO;
import util.Package;
import util.Person;
import util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/*
 * Class that handles operations to and from the data files and DatabaseMaps
 */

public class Database {
	
	private DatabaseMaps dbMaps;
	private JSONFileIO jsonIO;
	
	private String packageDirName;
	private String currentDirName;
	private String archiveDirName;
	
	public Database(String rootDirName) {
		
		this.packageDirName = rootDirName + "/packages";
		this.currentDirName = packageDirName + "/current";
		this.archiveDirName = packageDirName + "/archive";
		
		this.dbMaps = new DatabaseMaps();
		this.jsonIO = new JSONFileIO();
	}
	
	/*
	 * Function whose start is controlled by the controller
	 * 
	 * Creates new package directories if they do not exist
	 * Reads the current database, initializing the DatabaseMaps
	 */
	public void start() {
		// check if rootFolder and subfolders exist, create if they do not.
		System.out.println("Creating package directories: " + packageDirName + ", " + archiveDirName);
		FileIO.makeDirs(new String[] {packageDirName, currentDirName, archiveDirName});
		
		// read the active package database
		readCurrentDatabase();
	}
	
	/* Checks in a package into the DatabaseMaps and write a new file for the person */
	public Package checkInPackage(Person person, Package pkg) {
		// modify current DatabaseMaps
		dbMaps.addPackage(person, pkg);
		// write new person file
		writePersonFile(person,currentDirName);
		return null;
	}
	
	/* Sets a check out date for the package and removes the package */
	public void checkOutPackage(Package pkg) {
		// modify package checkOut date
		pkg.setCheckOutDate(new Date());
		// remove package from packageID:Package map
		dbMaps.deletePackage(pkg);
	}
	
	/* Return a list of all persons in the current directory */
	public ArrayList<Person> getAllCurrentPersons() {
		return dbMaps.getAllPersons();
	}
	
	/* returns a list of all packages in the current directory */
	public ArrayList<Package> getAllCurrentPackages() {
		return dbMaps.getAllPackages();
	}
	
	/* return sorted entries for all active entries - defined as those without a check out date */
	public ArrayList<Pair<Person,Package>> getActiveEntries() {
		ArrayList<Package> pkgList = getAllCurrentPackages();
		ArrayList<Pair<Person,Package>> results = new ArrayList<Pair<Person,Package>>();
		
		// iterate through all packages, adding those without a check out date
		for (Package pkg: pkgList) {
			if(pkg.getCheckOutDate() != null) {
				results.add(new Pair<Person,Package> (dbMaps.getPerson(pkg),pkg));
			}
		}
		
		// sort entries
		sortEntries(results); 
		
		return results;
	}
	
	/*
	 * Returns all of the packages checked in or out on, before, or after a calendar date
	 * Possible predicates:
	 * 	"on" - will look for packages on a specific day
	 * 	"before" - will look for packages before a specific day
	 * 	"after" - will look for packages after a specific day
	 */
	//TODO
	public ArrayList<Pair<Person,Package>> getEntriesByDate(Date date, String predicate) {
		return null;
	}

	/* Returns all of the packages associated with a person */
	public ArrayList<Pair<Person,Package>> getEntriesByPerson(Person person) {
		ArrayList<Pair<Person,Package>> results = new ArrayList<Pair<Person,Package>>();
		
		// iterate through all packages, adding them to the results
		for (Package pkg: dbMaps.getPackages(person)) {
			results.add(new Pair<Person,Package>(person,pkg));
		}
		
		return results;
	}
	
	/*
	 * Adds a person to the databaseMap and writes a file for the person
	 * 
	 * If the person is already in the archive, their file will be moved and their
	 * data will be read from the archive file.
	 * 
	 * If the person is not in the archive, a new person will be added to the databaseMap
	 * and a new file will be written for them.
	 */
	public void addPerson(Person person) {
		//Check if person is in the archive
		ArrayList<String> archiveFileNames = FileIO.getFileNamesFromDirectory(archiveDirName);
		ArrayList<String> archiveNetIDs = archiveFileNames;
		
		//TODO if it gets too big, do a binary search
		//If person file is in archive, add file to DatabaseMaps and delete file
		if(archiveNetIDs.contains(person.getNetID())) {
			String archiveFile = archiveDirName + '/' + person.getNetID();
			addPersonPackagesFromFile(archiveFile);
			FileIO.deleteFile(archiveFile);
		} else {
			//If not in the archive, add new person to DatabaseMaps
			dbMaps.addPerson(person);
		}
		
		//Write the new file
		writePersonFile(person, currentDirName); 
	}
	
	/*
	 * Moves a person from the current directory to the archive directory and deletes
	 * their information from the DatabaseMaps
	 */
	public void deletePerson(Person person) {
		// move person file to the archive 
		writePersonFile(person, archiveDirName);
		FileIO.deleteFile(currentDirName + '/' + person.getNetID());
		// remove person from DatabaseMaps
		dbMaps.deletePerson(person);
		
	}
	
	/* 
	 * return entries sorted by:
	 * 	LastName
	 * 	FirstName
	 * 	Check-out date
	 *  Check-in date
	 */
	
	//TODO write sort function
	private void sortEntries(ArrayList<Pair<Person,Package>> PPList){
//		Comparator<ArrayList<Pair<Package,Person>>> comparator = 
//				new Comparator<ArrayList<Pair<Package,Person>>>();
//		Collections.sort(PPList,comparator);
	}
	
	/*
	 * Writes a json file containing a person object and all of its associated package objects
	 * to the directory indicated by baseDirectory
	 */
	private void writePersonFile(Person person, String baseDirectory) {
		// create the person and package pair from the DatabaseMaps object
		Pair<Person,ArrayList<Package>> dbPair = 
				new Pair<Person,ArrayList<Package>>(person,dbMaps.getPackages(person));
		String personFile = baseDirectory + '/' + person.getNetID();
		
		//write to file
		try {
			jsonIO.writeDatabaseJSONFile(dbPair, personFile);
		} catch(IOException e) {
			//TODO actually catch the exception
			e.printStackTrace();
		}
	}
	
	/*
	 * Reads a json file containing a person object and all of its associated package objects
	 * from fileName into a Pair object
	 */
	private Pair<Person,ArrayList<Package>> readPersonFile(String fileName) {
		Pair<Person,ArrayList<Package>> dbPair = null;
		try {
			 dbPair = jsonIO.readDatabaseJSONFile(fileName);
		} catch (IOException e) {
			// TODO actually catch exception
			e.printStackTrace();
		}
		
		return dbPair;
		
	}
	
	/*
	 * Initializes the current database by reading all of the files in the current directory
	 * and placing all of their attributes into the databaseMaps
	 */
	private void readCurrentDatabase() {
		ArrayList<String> currentFileNames = FileIO.getFileNamesFromDirectory(currentDirName);
		for (String fileName: currentFileNames) {
			addPersonPackagesFromFile(fileName);
		}
	}
	
	/*
	 * Reads and adds all of the person and package information from the fileName to databaseMaps
	 */
	private void addPersonPackagesFromFile(String fileName) {
		Pair<Person,ArrayList<Package>> dbPair = readPersonFile(fileName);
		Person person = dbPair.first;
		ArrayList<Package> packages = dbPair.second;
		
		dbMaps.addPerson(person);
		for (Package pkg: packages) {
			dbMaps.addPackage(person, pkg);
		}
	}
		 
	public static void Main(String arg0) {
		
	}
	
}
