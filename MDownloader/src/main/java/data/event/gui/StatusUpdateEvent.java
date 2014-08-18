package data.event.gui;

import java.util.EventObject;

public class StatusUpdateEvent extends EventObject {
	private String message;
	private String fileName;
	private boolean isCompleted;

	public StatusUpdateEvent(Object source, boolean isCompleted, String fileName, String message) {
		super(source);
		this.message = message;
		this.fileName = fileName;
		this.isCompleted = isCompleted;
	}

	public String getMessage() {
		return message;
	}

	public String getFileName() {
		return fileName;
	}

	public boolean isCompleted() {
		return isCompleted;
	}
}
