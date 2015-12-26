package org.area515.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.area515.resinprinter.job.PrintJob;
import org.area515.resinprinter.printer.Printer;

import freemarker.cache.StringTemplateLoader;
import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class TemplateEngine {
	private static StringTemplateLoader templateLoader = new StringTemplateLoader();
	private static Configuration config = null;
	
	
	public static final TemplateExceptionHandler INFO_IGNORE_HANDLER = new TemplateExceptionHandler() {
		public void handleTemplateException(TemplateException te, Environment env, Writer out) throws TemplateException {
			System.out.println("TemplateExceptionHandler:" + te);
		}
	};

	public static String convertToFreeMarkerTemplate(String template) {
		if (template == null || template.trim().length() == 0) {
			return template;
		}
		String[] replacements = new String[] {
				"CURSLICE", 
				"LayerThickness", 
				"ZDir", 
				"ZLiftRate", 
				"ZLiftDist", 
				"buildAreaMM", 
				"LayerTime", 
				"FirstLayerTime", 
				"NumFirstLayers",
				"buildPlatformXPixels",
				"buildPlatformYPixels"};
		for (String replacement : replacements) {
			template = template.replaceAll("\\$" + replacement, "\\$\\{" + replacement + "\\}");
		}
		
		return template;
	}
	
	public static String buildData(PrintJob job, Printer printer, String templateString) throws IOException, TemplateException {
		if (config == null) {
	        config = new Configuration(Configuration.VERSION_2_3_21);
	        config.setDefaultEncoding("UTF-8");
	        config.setTemplateExceptionHandler(INFO_IGNORE_HANDLER);
	        config.setTemplateLoader(templateLoader);
	        config.setBooleanFormat("yes,no");
		}
		
		//com.cfs.daq.script.SharedInterpreter has similar stuff in it...
        Map<String, Object> root = new HashMap<String, Object>();
        /*
        	$ZDir
        	$CURSLICE
        	$LayerThickness// the thickenss of the layer in mm
        	$ZLiftDist// how far we're lifting
        	$ZLiftRate// the rate at which we're lifting
        $ZBottomLiftRate// the rate at which we're lifting for the bottom layers
        $ZRetractRate// how fast we'r retracting
        $SlideTiltVal// any used slide / tilt value on the x axis
        $BlankTime// how long to show the blank in ms
        	$LayerTime// total delay for a layer for gcode commands to complete - not including expusre time
        	$FirstLayerTime// time to expose the first layers in ms
        	$NumFirstLayers// number of first layers
        */

		root.put("now", new Date());
		root.put("CURSLICE", job.getCurrentSlice());
		root.put("LayerThickness", job.getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().getSliceHeight());
		root.put("ZDir", job.getPrinter().getConfiguration().getSlicingProfile().getDirection().getVector());
		root.put("ZLiftRate", job.getZLiftSpeed());
		root.put("ZLiftDist", job.getZLiftDistance());
		Double buildArea = job.getPrintFileProcessor().getBuildAreaMM(job);
		root.put("buildAreaMM", buildArea == null || buildArea < 0?null:buildArea);
		root.put("LayerTime", job.getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().getExposureTime());
		root.put("FirstLayerTime", job.getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().getFirstLayerExposureTime());
		root.put("NumFirstLayers", job.getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().getNumberOfFirstLayers());
		root.put("buildPlatformXPixels", job.getPrinter().getConfiguration().getSlicingProfile().getxResolution());
		root.put("buildPlatformYPixels", job.getPrinter().getConfiguration().getSlicingProfile().getyResolution());
		root.put("job", job);
		root.put("printer", printer);
		
        /* Get the template (uses cache internally) */
        Object source = templateLoader.findTemplateSource(templateString);
        if (source == null) {
        	templateLoader.putTemplate(templateString, templateString);
        }
        Template template = config.getTemplate(templateString);
        template.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        
        try {
	        Writer out = new StringWriter();
	        template.process(root, out);
	        return out.toString();
        } catch (TemplateException e) {
        	//This means that buildAreaMM isn't supported for this printer
        	if (e.getBlamedExpressionString().equals("buildAreaMM") && e.getMessage().contains("The following has evaluated to null or missing")) {
        		return null;
        	}

        	throw e;
        }
	}
	
	public static Object runScript(PrintJob job, ScriptEngine engine, String script, String scriptName) throws ScriptException {
		engine.put("now", new Date());
		engine.put("$CURSLICE", job.getCurrentSlice());
		engine.put("$LayerThickness", job.getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().getSliceHeight());
		engine.put("$ZDir", job.getPrinter().getConfiguration().getSlicingProfile().getDirection().getVector());
		engine.put("$ZLiftRate", job.getZLiftSpeed());
		engine.put("$ZLiftDist", job.getZLiftDistance());
		Double buildArea = job.getPrintFileProcessor().getBuildAreaMM(job);
		engine.put("$buildAreaMM", buildArea == null || buildArea < 0?Double.NaN:buildArea);
		engine.put("$LayerTime", job.getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().getExposureTime());
		engine.put("$FirstLayerTime", job.getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().getFirstLayerExposureTime());
		engine.put("$NumFirstLayers", job.getPrinter().getConfiguration().getSlicingProfile().getSelectedInkConfig().getNumberOfFirstLayers());
		engine.put("$buildPlatformXPixels", job.getPrinter().getConfiguration().getSlicingProfile().getxResolution());
		engine.put("$buildPlatformYPixels", job.getPrinter().getConfiguration().getSlicingProfile().getyResolution());
		engine.put("job", job);
		engine.put("printer", job.getPrinter());
		engine.put(ScriptEngine.FILENAME, scriptName);
		
		return engine.eval(script);
	}
}
