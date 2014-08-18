package data;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import data.event.scaner.DirectoryScannedEvent;
import data.event.scaner.DirectoryScannedListener;

public class DirectoryScanner extends Thread {
	private Map<String, String> files;
	private String pathToDirectory;
	private List<DirectoryScannedListener> listeners;
	private boolean stopScanning = false;

	public DirectoryScanner(Map<String, String> files, String pathToDirectory) {
		listeners = new CopyOnWriteArrayList<DirectoryScannedListener>();
		this.files = files;
		this.pathToDirectory = pathToDirectory;
	}

	public void addDirectoryScannedListener(DirectoryScannedListener ls) {
		listeners.add(ls);
	}

	public void removeDirectoryScannedListener(DirectoryScannedListener ls) {
		listeners.remove(ls);
	}

	public void stopScanning() {
		stopScanning = true;
	}

	@Override
	public void run() {
		if (files != null) {
			int i = 0;
			stopScanning = false;
			Set<String> filesInDirectory = getListOfFilesInDirectory(pathToDirectory);
			DirectoryScannedEvent event = null;
			for (Entry<String, String> entry : files.entrySet()) {
				i++;
				if (!stopScanning) {
					event = null;
					entry.getKey();
					entry.getValue();
					if (filesInDirectory.contains((String) entry.getKey()))
						event = new DirectoryScannedEvent(this, false, entry.getValue(), "Status: Scanned " + i + " files of " + files.size());
					else
						event = new DirectoryScannedEvent(this, false, "", "Status: Scanned " + i + " files of " + files.size());
				} else
					break;
				for (DirectoryScannedListener ls : listeners)
					ls.listenForScanner(event);
			}
			event = new DirectoryScannedEvent(this, true, "", "Status: Scanning stoped. Scanned " + i + " files of " + files.size());
			for (DirectoryScannedListener ls : listeners)
				ls.listenForScanner(event);
		}
	}

	private Set<String> getListOfFilesInDirectory(String path) {
		Set<String> listOfFiles = new HashSet<String>();
		if (path != null && !path.isEmpty()) {
			DirectoryScannedEvent event = new DirectoryScannedEvent(this, false, "", "Status: Starting scanning");
			for (DirectoryScannedListener ls : listeners)
				ls.listenForScanner(event);
			File files[] = new File(path).listFiles();
			for (int i = 0; i < files.length && !stopScanning; i++)
				listOfFiles.add(Controller.getInstance().getFileNameWithoutNumber(files[i].getName()));
		}
		return listOfFiles;
	}
}
