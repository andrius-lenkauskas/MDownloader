package data;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CopyOnWriteArrayList;

import data.event.downloader.DownloadingCompletedEvent;
import data.event.downloader.DownloadingCompletedListener;
import data.event.downloader.RunningCompletedEvent;

class Downloader extends Thread {
	private Map<String, String> urlMap;
	private boolean createSuperLink;
	private static final int READ_TIMEOUT = 5000;
	private static final int NUMBER_OF_FILES = 11;
	private static final int BUFFER_SIZE = 1024*20;
	private List<DownloadingCompletedListener> listeners;
	private boolean stopDownloading = false;

	public Downloader() {
		createSuperLink = false;
		urlMap = new HashMap<String, String>();
		listeners = new CopyOnWriteArrayList<DownloadingCompletedListener>();
	}

	public Downloader(boolean createSuperLink) {
		this.createSuperLink = createSuperLink;
		urlMap = new HashMap<String, String>();
		listeners = new CopyOnWriteArrayList<DownloadingCompletedListener>();
	}

	public void addDownloadingCompletedListener(DownloadingCompletedListener ls) {
		listeners.add(ls);
	}

	public void removeDownloadingCompletedListener(DownloadingCompletedListener ls) {
		listeners.remove(ls);
	}

	public void putURL(String fileSaveURL, String fileDownloadURL) {
		urlMap.put(fileSaveURL, fileDownloadURL);
	}

	public void stopDownloading() {
		stopDownloading = true;
	}

	@Override
	public void run() {
		for (Map.Entry<String, String> entry : urlMap.entrySet()) {
			if (!stopDownloading) {
				boolean isSuccessfullyDownloaded = false;
				String superLink = createSuperLink ? createSuperLink(entry.getValue()) : "";
				if (!superLink.isEmpty())
					isSuccessfullyDownloaded = downloadFiles(entry.getKey(), superLink);
				if (!isSuccessfullyDownloaded)
					isSuccessfullyDownloaded = downloadFiles(entry.getKey(), entry.getValue());
				DownloadingCompletedEvent event = new DownloadingCompletedEvent(this, isSuccessfullyDownloaded, new File(entry.getKey()).getName());
				for (DownloadingCompletedListener ls : listeners) {
					ls.listenForDownloadingCompletion(event);
				}
			}
		}
		RunningCompletedEvent event = new RunningCompletedEvent(this, true);
		for (DownloadingCompletedListener ls : listeners) {
			ls.listenForRunningCompleted(event);
		}
	}

	private boolean downloadFiles(String fileSaveURL, String fileDownloadURL) {
		boolean isSuccessfullyDownloaded = false;
		Map<String, String> filesURLsMap = createURLsListFromFirstFile(fileSaveURL, fileDownloadURL);
		for (Map.Entry<String, String> entry : filesURLsMap.entrySet()) {
			if (downloadFile(entry.getKey(), entry.getValue()))
				isSuccessfullyDownloaded = true;
		}
		return isSuccessfullyDownloaded;
	}

	private Map<String, String> createURLsListFromFirstFile(String fileSaveURL, String fileDownloadURL) {
		Map<String, String> map = new HashMap<String, String>();
		String fileExt = fileSaveURL.split("\\.")[fileSaveURL.split("\\.").length - 1];
		String fileSaveURLPart = getURLParthWithoutNumberAndExt(fileSaveURL, false);
		String fileDownloadURLPart = getURLParthWithoutNumberAndExt(fileDownloadURL, true);

		map.put(fileSaveURLPart + "." + fileExt, fileDownloadURLPart + "." + fileExt);
		for (int i = 1; i < NUMBER_OF_FILES; i++) {
			map.put(fileSaveURLPart + "_" + i + "." + fileExt, fileDownloadURLPart + "_" + i + "." + fileExt);
		}
		return map;
	}

	private String getURLParthWithoutNumberAndExt(String url, boolean isThisDownloadUrl) {
		Pattern pattern = Pattern.compile(".*(?=/)");
		Matcher matcher = pattern.matcher(url);
		String pathToFile = "";
		if (isThisDownloadUrl) {
			pathToFile = matcher.find() ? matcher.group() : "";
		} else
			pathToFile = new File(url).getParent();
		String fileName = new File(url).getName();
		pattern = Pattern.compile(".*(?=\\.)");
		matcher = pattern.matcher(fileName);
		String fileNameWithoutExt = matcher.find() ? matcher.group() : "";
		pattern = Pattern.compile(".*(?=_\\d{1}$)");
		matcher = pattern.matcher(fileNameWithoutExt);
		String fileNameWithoutFileNumber = matcher.find() ? matcher.group() : fileNameWithoutExt;
		String separator = isThisDownloadUrl ? "/" : File.separator;
		return pathToFile + separator + fileNameWithoutFileNumber;
	}

	private String createSuperLink(String fileDownloadURL) {
		Pattern pattern = Pattern.compile(".*/");
		Matcher matcher = pattern.matcher(fileDownloadURL);
		return matcher.find() ? matcher.group() + "super/" + new File(fileDownloadURL).getName() : "";
	}

	private boolean downloadFile(String fileSaveURL, String fileDownloadURL) {
		boolean isSuccessfullyDownloaded = false;
		URL url = null;
		URLConnection conn = null;
		BufferedInputStream in = null;
		FileOutputStream fout = null;
		try {
			url = new URL(fileDownloadURL);
			conn = url.openConnection();
			conn.setReadTimeout(READ_TIMEOUT);
			in = new BufferedInputStream(conn.getInputStream());
			fout = new FileOutputStream(fileSaveURL);
			byte data[] = new byte[BUFFER_SIZE];
			int count;
			while ((count = in.read(data, 0, BUFFER_SIZE)) != -1)
				fout.write(data, 0, count);
			isSuccessfullyDownloaded = true;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (fout != null)
				try {
					fout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return isSuccessfullyDownloaded;
	}
}
