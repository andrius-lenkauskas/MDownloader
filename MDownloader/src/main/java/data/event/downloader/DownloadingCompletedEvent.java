package data.event.downloader;

import java.util.EventObject;

public class DownloadingCompletedEvent extends EventObject {
	private boolean isSuccessfullyCompleted;
	private String fileName;

	public DownloadingCompletedEvent(Object source, boolean isSuccessfullyCompleted, String fileName) {
		super(source);
		this.isSuccessfullyCompleted = isSuccessfullyCompleted;
		this.fileName = fileName;
	}

	public boolean isSuccessfullyCompleted() {
		return isSuccessfullyCompleted;
	}

	public String getFileName() {
		return fileName;
	}
}
