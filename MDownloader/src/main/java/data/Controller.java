package data;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import data.event.downloader.DownloadingCompletedEvent;
import data.event.downloader.DownloadingCompletedListener;
import data.event.downloader.RunningCompletedEvent;
import data.event.gui.StatusUpdateEvent;
import data.event.gui.StatusUpdateListener;
import data.event.scaner.DirectoryScannedEvent;
import data.event.scaner.DirectoryScannedListener;

public class Controller implements DownloadingCompletedListener, DirectoryScannedListener {
	private static Controller instance;
	private String bidzURL = "http://llfull.bidz.com";
	private String modURL = "http://images.modnique.com";
	private List<StatusUpdateListener> listeners;
	private Downloader[] threads;
	private int activeThreadCount;
	private int downloadedFilesCount;
	private Map<String, String> allLinks;
	private static final int threadCount = 5;
	private DirectoryScanner scanner;

	private Controller() {
		listeners = new CopyOnWriteArrayList<StatusUpdateListener>();
	}

	public static synchronized Controller getInstance() {
		if (instance == null)
			instance = new Controller();
		return instance;
	}

	public void addStatusUpdateListener(StatusUpdateListener ls) {
		listeners.add(ls);
	}

	public void removeStatusUpdateListener(StatusUpdateListener ls) {
		listeners.remove(ls);
	}

	@Override
	public void listenForDownloadingCompletion(DownloadingCompletedEvent event) {
		if (event.isSuccessfullyCompleted()) {
			downloadedFilesCount++;
			StatusUpdateEvent e = new StatusUpdateEvent(this, false, event.getFileName(), "Status: Downloaded " + downloadedFilesCount + " from "
					+ allLinks.size() + " files");
			for (StatusUpdateListener ls : listeners) {
				ls.updateStatus(e);
			}
		}
	}

	@Override
	public void listenForRunningCompleted(RunningCompletedEvent event) {
		if (event.isRunningCompleted()) {
			activeThreadCount--;
		}
		if (activeThreadCount <= 0) {
			StatusUpdateEvent e = new StatusUpdateEvent(this, true, "", "Status: Downloading completed");
			for (StatusUpdateListener ls : listeners) {
				ls.updateStatus(e);
			}
		}
	}

	@Override
	public void listenForScanner(DirectoryScannedEvent event) {
		StatusUpdateEvent e = new StatusUpdateEvent(this, event.isCompleted(), event.getFileName(), event.getMessage());
		for (StatusUpdateListener ls : listeners) {
			ls.updateStatus(e);
		}
	}

	public void startScanning(Map<String, String> files, String pathToDirectory) {
		scanner = new DirectoryScanner(files, pathToDirectory);
		scanner.addDirectoryScannedListener(this);
		scanner.start();
	}

	public void stopScanning() {
		if (scanner != null)
			scanner.stopScanning();
	}

	public String getFileNameWithoutNumber(String fileName) {
		String fileExt = fileName.split("\\.")[fileName.split("\\.").length - 1];
		Pattern pattern = Pattern.compile(".*(?=_\\d{1}\\..*$)");
		Matcher matcher = pattern.matcher(fileName);
		String cleanedFileName;
		if (matcher.find())
			cleanedFileName = matcher.group() + "." + fileExt;
		else {
			pattern = Pattern.compile(".*(?=\\..*$)");
			matcher = pattern.matcher(fileName);
			cleanedFileName = matcher.find() ? matcher.group() + "." + fileExt : "";
		}
		return cleanedFileName;
	}

	public void stopDownloading() {
		if (threads != null)
			for (int i = 0; i < threads.length; i++) {
				threads[i].stopDownloading();
			}
	}

	public boolean startDownloading(Map<String, String> allLinks, boolean createSuperLink) {
		boolean started = true;
		if (!allLinks.isEmpty()) {
			this.allLinks = allLinks;
			threads = new Downloader[threadCount];
			activeThreadCount = threads.length;
			downloadedFilesCount = 0;
			int i = 0;
			for (i = 0; i < threads.length; i++) {
				threads[i] = new Downloader(createSuperLink);
				threads[i].addDownloadingCompletedListener(this);
			}
			i = 0;
			for (Map.Entry<String, String> entry : allLinks.entrySet()) {
				threads[i++].putURL(entry.getKey(), entry.getValue());
				if (i >= threads.length)
					i = 0;
			}
			for (i = 0; i < threads.length; i++)
				threads[i].start();
		} else
			started = false;
		return started;
	}

	public String[] convertToFileName(String line, boolean checkIfIsModURL) {
		String result[] = null;
		String urlOrPartialURL = geturlOrPartialURL(line);
		String url = getURL(line);
		if (!url.isEmpty()) {
			String line2 = checkIfIsModURL && isModUrl(url) ? "" : line.substring(0, line.lastIndexOf(urlOrPartialURL));
			String[] lineArr = line2.split("\\s");
			if (lineArr.length == 1 && lineArr[0].isEmpty()) {
				result = new String[2];
				result[0] = getFileNameFromURL(urlOrPartialURL);
				result[1] = url;
			} else if (lineArr.length == 1) {
				result = new String[2];
				result[0] = getItemIdFromSKU(lineArr[0]).isEmpty() ? getFileNameFromURL(urlOrPartialURL) : getItemIdFromSKU(lineArr[0]);
				result[1] = url;
			} else if (lineArr.length >= 2) {
				String sku = getItemIdFromSKU(lineArr[0]);
				String fileNamePart = "";
				for (int i = 1; i < lineArr.length; i++)
					fileNamePart = fileNamePart + getCleanStringForURL(lineArr[i]);
				result = new String[2];
				result[0] = sku.isEmpty() && fileNamePart.isEmpty() ? getFileNameFromURL(urlOrPartialURL) : sku + fileNamePart;
				result[1] = url;
			}
			setFileNameExtension(result);
		}
		return result;
	}

	private boolean isModUrl(String urlPath) {
		boolean isMod = false;
		if (isValidURL(urlPath))
			try {
				URL urlObj = new URL(urlPath);
				if (urlObj.getPath().toLowerCase().startsWith("/sales/"))
					isMod = true;
			} catch (MalformedURLException e) {
			}
		return isMod;
	}

	private void setFileNameExtension(String[] url) {
		if (url != null) {
			String[] ext = url[0].split("\\.");
			String[] ext2 = url[1].split("\\.");
			if (ext.length < 2)
				url[0] = url[0] + "." + ext2[ext2.length - 1];
		}
	}

	private String geturlOrPartialURL(String line) {
		String url = "";
		Pattern pattern = Pattern.compile("\\S+:.*\\S");
		Matcher matcher = pattern.matcher(line);
		while (matcher.find())
			url = matcher.group();
		url = isValidURL(url) ? url : "";
		if (url.isEmpty() && !line.isEmpty()) {
			pattern = Pattern.compile("/sales/.*\\S");
			matcher = pattern.matcher(line);
			while (matcher.find())
				url = matcher.group();
			if (!(isValidURL(modURL + url) && !url.isEmpty()))
				url = "";
			if (url.isEmpty()) {
				String[] lineArr = line.split("\\s");
				url = lineArr.length > 0 ? lineArr[lineArr.length - 1] : "";
				if (!(isValidURL(bidzURL + url) && !url.isEmpty()))
					url = "";
			}
		}
		if (getFileNameFromURL(url).split("\\.").length < 2)
			url = "";
		return url;
	}

	private String getURL(String line) {
		String url = "";
		Pattern pattern = Pattern.compile("\\S+:.*\\S");
		Matcher matcher = pattern.matcher(line);
		while (matcher.find())
			url = matcher.group();
		url = isValidURL(url) ? url : "";
		if (url.isEmpty() && !line.isEmpty()) {
			pattern = Pattern.compile("/sales/.*\\S");
			matcher = pattern.matcher(line);
			while (matcher.find())
				url = matcher.group();
			url = isValidURL(modURL + url) && !url.isEmpty() ? modURL + url : "";
			if (url.isEmpty()) {
				String[] lineArr = line.split("\\s");
				url = lineArr.length > 0 ? lineArr[lineArr.length - 1] : "";
				url = isValidURL(bidzURL + url) && !url.isEmpty() ? bidzURL + url : "";
			}
		}
		if (getFileNameFromURL(url).split("\\.").length < 2)
			url = "";
		try {
			URL urlObj = new URL(url);
			String path = urlObj.getPath().replaceAll("thumbs/", "").replaceAll("super/", "");
			URI uri = new URI(urlObj.getProtocol(), urlObj.getUserInfo(), urlObj.getHost(), urlObj.getPort(), path, urlObj.getQuery(),
					urlObj.getRef());
			url = uri.toURL().toString();
		} catch (MalformedURLException | URISyntaxException e) {
			url = "";
		}
		return url;
	}

	private boolean isValidURL(String url) {
		return url.matches("(?i)^http(s)?://[a-z0-9-]+(.[a-z0-9-]+)*(:[0-9]+)?(/.*)?$");
	}

	private String getCleanStringForURL(String line) {
		String result = "";
		Pattern pattern = Pattern.compile("\\w+");
		Matcher matcher = pattern.matcher(line);
		while (matcher.find())
			result = result + "_" + matcher.group();
		return result;
	}

	private String getItemIdFromSKU(String line) {
		Pattern pattern = Pattern.compile("\\w+");
		Matcher matcher = pattern.matcher(line);
		return matcher.find() ? matcher.group() : "";
	}

	private String getFileNameFromURL(String line) {
		return new File(line).getName();
	}
}
