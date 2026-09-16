package application.ui.preview;

import java.io.InputStream;
import java.net.URL;
import java.util.function.BiPredicate;
import java.util.logging.Logger;

import org.daisy.dotify.api.embosser.Embosser;
import org.daisy.dotify.api.embosser.EmbosserWriter;
import org.daisy.braille.utils.pef.PEFConverterFacade;
import org.daisy.braille.utils.pef.PEFHandler;
import org.daisy.braille.utils.pef.PEFHandler.Alignment;

import application.common.Configuration;
import application.common.FeatureSwitch;
import application.common.NetworkDevice;

import org.daisy.dotify.api.embosser.Device;

import org.daisy.braille.utils.pef.PrinterDevice;
import org.daisy.braille.utils.pef.Range;

import javafx.concurrent.Task;

class EmbossTask extends Task<Void> {
	private static final Logger logger = Logger.getLogger(EmbossTask.class.getCanonicalName());
	private final URL url;
	private final String deviceName;
	private final String align;
	private final Range range;
	private final int copies;
	private final Configuration conf;
	private final BiPredicate<Integer, Integer> beforeNextCopy;
	private int copiesSent = 0;
	
	/**
	 * Creates a new emboss task.
	 * @param url the file to emboss
	 * @param deviceName the device name
	 * @param align the alignment
	 * @param range the page range
	 * @param copies the number of copies
	 * @param conf the configuration
	 * @param beforeNextCopy called on the task thread before each copy after the first, with
	 * 			the number of the next copy and the total number of copies. The call may block
	 * 			(for example while waiting for the user). Returning false cancels the remaining copies.
	 */
	EmbossTask(URL url, String deviceName, String align, Range range, int copies, Configuration conf, BiPredicate<Integer, Integer> beforeNextCopy) {
		this.url = url;
		this.deviceName = deviceName;
		this.align = align;
		this.range = range;
		this.copies = copies;
		this.conf = conf;
		this.beforeNextCopy = beforeNextCopy;
	}
	
	/**
	 * Gets the number of copies that were sent to the embosser.
	 * @return the number of copies sent
	 */
	int getCopiesSent() {
		return copiesSent;
	}

	@Override
	protected Void call() throws Exception {
		logger.info("About to emboss " + (copies>1?copies + " copies ":"") + "on " + deviceName + " with alignment " + align + " and range " + range);
		if (FeatureSwitch.EMBOSSING.isOn()) {
			for (int i=0; i<copies; i++) {
				if (i>0 && !beforeNextCopy.test(i+1, copies)) {
					logger.info("Embossing cancelled after " + i + " of " + copies + " copies.");
					break;
				}
				logger.info("Sending copy " + (i+1) + " of " + copies + " to " + deviceName);
				try (InputStream iss = url.openStream()) {
					//TODO: don't recreate objects for each copy unless necessary
					Embosser emb = conf.getConfiguredEmbosser();
					Device bd = NetworkDevice.isNetworkDevice(deviceName)
							? NetworkDevice.parse(deviceName)
							: new PrinterDevice(deviceName, false);
					EmbosserWriter writer = emb.newEmbosserWriter(bd);
					
					PEFHandler.Builder phb = new PEFHandler.Builder(writer).
												range(range).
												offset(0);
					if (conf.supportsAligning()) {
				        Alignment alignment = Alignment.CENTER_INNER;
				        try {
				        	alignment = Alignment.valueOf(align.toUpperCase());
				        } catch (IllegalArgumentException e) {
				        	e.printStackTrace();
				        }
						phb.align(alignment);
					}
					new PEFConverterFacade(conf.getEmbosserCatalog()).parsePefFile(iss, phb.build());
				}
				copiesSent++;
			}
		} else {
			logger.info("Embossing is deactivated.");
		}
		return null;
	}

}