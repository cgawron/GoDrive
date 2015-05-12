package de.cgawron.godrive;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
//import javax.ejb.Singleton;
//import javax.enterprise.concurrent.ManagedScheduledExecutorService;

import java.util.logging.Logger;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import de.cgawron.go.sgf.GameTree;

//@Singleton
public class UpdateService {
	private static final Logger logger = Logger.getLogger(UpdateService.class.getName());

	public static final String MIMETYPE_SGF = "application/x-go-sgf";

	@Resource(name = "DefaultManagedScheduledExecutorService")
	//ManagedScheduledExecutorService scheduledExecutor;

	Runnable task = null;
	boolean busy = false;
	long submitted = 0;
	long lastSaved = 0;

	public void submitUpdateRequest(Drive service, File file, GameTree gameTree)
	{
		synchronized (UpdateService.class) {
			final GameTree gt = gameTree;
			task = new Runnable() {
				@Override
				public void run() {
					try {
						logger.info("saving gameTree");
						ByteArrayOutputStream stream = new ByteArrayOutputStream();
						gt.save(stream);
						String sgfContent = stream.toString();
						service.files()
								.update(file.getId(), file,
										ByteArrayContent.fromString(MIMETYPE_SGF, sgfContent))
								.execute();
					} catch (Exception e) {
						throw new RuntimeException("save failed", e);
					}
				}
			};
			submitted = System.currentTimeMillis();
		}
	}

	public void save()
	{
		if (busy) {
			logger.info("saveTask still busy ...");
			return;
		}
		long now = System.currentTimeMillis();
		synchronized (UpdateService.class) {
			long taskAge = now - submitted;
			long saveAge = now - lastSaved;
			if (taskAge > 15000 || saveAge > 60000) {
				Runnable saveTask = task;
				task = null;
				if (saveTask != null) {
					busy = true;
					saveTask.run();
					lastSaved = System.currentTimeMillis();
					busy = false;
				}
			}
		}
	}

	@PostConstruct
	public void init() {
		/*
		scheduledExecutor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				save();
			}
		}, 0, 5, TimeUnit.SECONDS);
		*/
	}

}
