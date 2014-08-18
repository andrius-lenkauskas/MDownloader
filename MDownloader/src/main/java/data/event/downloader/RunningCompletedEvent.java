package data.event.downloader;

import java.util.EventObject;

public class RunningCompletedEvent extends EventObject {
	private boolean runningCompleted;

	public RunningCompletedEvent(Object source, boolean runningCompleted) {
		super(source);
		this.runningCompleted = runningCompleted;
	}

	public boolean isRunningCompleted() {
		return runningCompleted;
	}
}
