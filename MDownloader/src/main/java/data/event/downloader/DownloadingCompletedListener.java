package data.event.downloader;

import java.util.EventListener;

public interface DownloadingCompletedListener extends EventListener {
	void listenForDownloadingCompletion(DownloadingCompletedEvent event);
	void listenForRunningCompleted(RunningCompletedEvent event);
}
