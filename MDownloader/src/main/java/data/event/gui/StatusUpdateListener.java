package data.event.gui;

import java.util.EventListener;

public interface StatusUpdateListener extends EventListener{
	void updateStatus(StatusUpdateEvent event);
}
