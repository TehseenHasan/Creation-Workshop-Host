package org.area515.resinprinter.services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.area515.resinprinter.server.HostProperties;

import com.coremedia.iso.boxes.Container;
import com.google.common.io.ByteStreams;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.FragmentedMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.H264TrackImpl;

@Path("media")
public class MediaService {
    private static final Logger logger = LogManager.getLogger();
	public static MediaService INSTANCE = new MediaService();

	//All static for now, just to get this done...
	private File rawh264StreamFile = new File(System.getProperty("java.io.tmpdir"), "tempraw.h246");
	private File mp4StreamFile = new File(System.getProperty("java.io.tmpdir"), "temp.mp4");
	private Process rawH264ProducerProcess;
	
	//TODO: We need to Lock per Printer, not a global lock!
	private Lock processLock = new ReentrantLock();
	
	private MediaService(){}

	public File getRecordedFile() {
		return mp4StreamFile;
	}
	
	public class StreamCopier implements Runnable {
		private InputStream inputStream;
		private OutputStream outputStream;
		private String streamingCommandString;
		
		public StreamCopier(InputStream inputStream, OutputStream outputStream, String streamingCommandString) {
			this.inputStream = inputStream;
			this.outputStream = outputStream;
			this.streamingCommandString = streamingCommandString;
		}
		
		public void run() {
			try {
				IOUtils.copy(inputStream, outputStream);
			} catch (IOException e) {
				logger.error("Copy complete:{} Message:{}", streamingCommandString, e.getMessage());
			} finally {
				logger.info("Copy process is complete for:{}", streamingCommandString);
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
					}
				}
				if (outputStream != null) {
					try {
						outputStream.close();
					} catch (IOException e) {
					}
				}
			}
		}
		
	}
	
	//TODO: We need to actually get the printer by printername and then get the commandLineParameters from the MachineConfig not the HostProperties!!
	@GET
	@Path("startrecordvideo/{printerName}/x/{x}/y/{y}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse startVideo(@PathParam("printerName") String printerName, @PathParam("x") int x, @PathParam("y") String y) {
		processLock.lock();
		try {
			if (rawH264ProducerProcess != null) {
				return new MachineResponse("startrecord", true, "Printer:" + printerName + " already started recording");
			}

			logger.info("Attempting to start video");
			final String[] streamingCommand = HostProperties.Instance().getStreamingCommand();
			try {
				String[] replacedCommands = new String[streamingCommand.length];
				StringBuffer buffer = new StringBuffer();
				for (int t = 0; t < streamingCommand.length; t++) {
					replacedCommands[t] = MessageFormat.format(streamingCommand[t], x, y);
					buffer.append(replacedCommands[t]);
					buffer.append(" ");
				}				
				
				rawH264ProducerProcess = Runtime.getRuntime().exec(replacedCommands);
				final BufferedInputStream inputStream = new BufferedInputStream(rawH264ProducerProcess.getInputStream());
				final FileOutputStream outputStream = new FileOutputStream(rawh264StreamFile);
				final BufferedInputStream errorStream = new BufferedInputStream(rawH264ProducerProcess.getErrorStream());
				
				Thread writingThread = new Thread(new StreamCopier(inputStream, outputStream, buffer.toString()), "OriginalVideoWritingThread");
				writingThread.setDaemon(true);
				writingThread.start();
				Thread errorThread = new Thread(new StreamCopier(errorStream, System.err, buffer.toString() + " LOG"), "VideoErrorLoggingThread");
				errorThread.setDaemon(true);
				errorThread.start();
				return new MachineResponse("startrecord", true, "Printer:" + printerName + " started recording");
			} catch (IOException e) {
				logger.error("Couldn't start command line process:" + Arrays.toString(streamingCommand), e);
				if (rawH264ProducerProcess != null) {
					rawH264ProducerProcess.destroy();
					rawH264ProducerProcess = null;
				}
				return new MachineResponse("startrecord", false, "Printer:" + printerName + " couldn't record");
			}
		} finally {
			processLock.unlock();
		}
	}

	@GET
	@Path("stoprecordvideo/{printerName}")
	@Produces(MediaType.APPLICATION_JSON)
	public MachineResponse stopVideo(@PathParam("printerName") String printerName) {
		processLock.lock();
		try {
			if (rawH264ProducerProcess == null) {
				return new MachineResponse("stopvideorecord", false, "A recording hasn't been started yet for this printer: " + printerName);
			}

			logger.info("Attempting to end video");
			rawH264ProducerProcess.destroy();
			Thread thread = new Thread(new Runnable() {
				public void run() {
					FileOutputStream publishStream = null;
					FileDataSourceImpl originalFileSource = null;
					try {
						publishStream = new FileOutputStream(mp4StreamFile);
						originalFileSource = new FileDataSourceImpl(rawh264StreamFile);

						H264TrackImpl h264Track = new H264TrackImpl(originalFileSource);
						Movie m = new Movie();
						m.addTrack(h264Track);

						Container out = new FragmentedMp4Builder().build(m);
						FileChannel fc = publishStream.getChannel();
						out.writeContainer(fc);
					} catch (Exception e) {
						logger.error("Couldn't convert file from:" + rawh264StreamFile + " to:" + mp4StreamFile, e);
					} finally {
						if (publishStream != null) {
							try {
								publishStream.close();
							} catch (IOException e) {
							}
						}
						if (originalFileSource != null) {
							try {
								originalFileSource.close();
							} catch (IOException e) {
							}
							if (!rawh264StreamFile.delete()) {
								logger.warn("Couldn't delete old file:{}", rawh264StreamFile);
							}
						}
						rawH264ProducerProcess = null;
					}
				};
			}, "VideoMuxingThread");
			thread.setDaemon(true);
			thread.start();
			
			try {
				//You MUST wait for the MUXing process to complete before this method returns. The client depends on it...
				thread.join();
				return new MachineResponse("stopvideorecord", true, "MP4 Creation complete.");
			} catch (InterruptedException e) {
				return new MachineResponse("stopvideorecord", false, "Interrupted while waiting for MP4 to complete.");
			}
		} finally {
			processLock.unlock();
		}
	}
	
	//TODO: We need to actually get the printer by printername and then get the commandLineParameters from the MachineConfig not the HostProperties!!
	@GET
	@Path("takesnapshot/{printerName}/x/{x}/y/{y}")
    @Produces("image/png")
	public StreamingOutput takePicture(@PathParam("printerName") String printerName, @PathParam("x") final int x, @PathParam("y") final int y) {
	    return new StreamingOutput() {
			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				String[] streamingCommand = HostProperties.Instance().getImagingCommand();
				String[] replacedCommands = new String[streamingCommand.length];
				for (int t = 0; t < streamingCommand.length; t++) {
					replacedCommands[t] = MessageFormat.format(streamingCommand[t], x, y);
				}
				
				InputStream inputStream = null;
				processLock.lock();
				try {
					Process imagingProcess = Runtime.getRuntime().exec(replacedCommands);
					ByteStreams.copy(imagingProcess.getInputStream(), output);
				} finally {
					if (inputStream != null) {
						try {
							inputStream.close();
						} catch (IOException e) {
						}
					}
					if (output != null) {
						try {
							output.close();
						} catch (IOException e) {
						}
					}
					processLock.unlock();
				}
			}  
	    };
	}
}
