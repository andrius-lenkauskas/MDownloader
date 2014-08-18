package data.event.scaner;

import java.util.EventObject;

public class DirectoryScannedEvent extends EventObject {
	private boolean isCompleted;
	private String message;
	private String fileName;

	public DirectoryScannedEvent(Object source, boolean isCompleted, String fileName, String message) {
		super(source);
		this.isCompleted = isCompleted;
		this.message = message;
		this.fileName = fileName;
	}

	public boolean isCompleted() {
		return isCompleted;
	}

	public String getMessage() {
		return message;
	}
	
	public String getFileName() {
		return fileName;
	}
}
