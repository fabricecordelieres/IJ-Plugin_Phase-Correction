import java.awt.AWTEvent;
import java.awt.Label;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;

public class phaseCorrection_Auto implements ExtendedPlugInFilter, DialogListener{
	private int flags = DOES_ALL|KEEP_PREVIEW|PARALLELIZE_STACKS;
	PlugInFilterRunner pfr=null;
	GenericDialog gd=null;
	
	int maxDisplacement=(int) Prefs.get("phaseCorrection_Auto_maxDisplacement.double", 10);
	boolean isEven=Prefs.get("phaseCorrection_Auto_isEven.boolean", true);
	boolean showPlot=Prefs.get("phaseCorrection_Auto_showPlot.boolean", true);
	boolean showTable=Prefs.get("phaseCorrection_Auto_showTable.boolean", true);
	boolean showLog=Prefs.get("phaseCorrection_Auto_showLog.boolean", true);
	
	boolean hasBeenCalculated=false;

	@Override
	public int setup(String arg, ImagePlus imp) {
		return flags;
	}

	@Override
	public void run(ImageProcessor ip) {
		if(gd.isActive()) {
			phaseCorrection.computeCorrection(ip, maxDisplacement, isEven);
			((Label) gd.getMessage()).setText("Last computed shift: "+phaseCorrection.getLastComputedShift());
			hasBeenCalculated=true;
		}
		phaseCorrection.applyCorrection(ip, phaseCorrection.getLastComputedShift(), isEven);
	}

	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		this.pfr=pfr;
		
		gd=new GenericDialog("Automated Phase Correction");
		gd.addMessage("Infos/bug report: fabrice.cordelieres@gmail.com");
		gd.addMessage("");
		
		gd.addSlider("Maximum_phase_correction_(pixels)", 0, 20, maxDisplacement);
		gd.addChoice("Which_lines_to_move ?", new String[]{"Even", "Odd"},  isEven?"Even":"Odd");
		gd.addCheckbox("Show_graph", showPlot);
		gd.addCheckbox("Show_table", showTable);
		gd.addCheckbox("Show_log", showLog);
		gd.addMessage("");
		gd.addPreviewCheckbox(pfr);
		gd.addMessage("");
		gd.addMessage("");
		gd.addDialogListener(this);
		gd.showDialog();
		
		if (gd!=null && gd.wasCanceled()) return DONE;
		
    		Prefs.set("phaseCorrection_Auto_maxDisplacement.double", maxDisplacement);
    		Prefs.set("phaseCorrector_Auto_isEven.boolean", isEven);
    		Prefs.set("phaseCorrection_Auto_showPlot.boolean", showPlot);
    		Prefs.set("phaseCorrection_Auto_showTable.boolean", showTable);
    		Prefs.set("phaseCorrection_Auto_showLog.boolean", showLog);
    		
    		if(!hasBeenCalculated) phaseCorrection.computeCorrection(imp.getProcessor(), maxDisplacement, isEven);
    		if(showPlot) phaseCorrection.plotLastComputedCCF();
		if(showTable) phaseCorrection.logLastComputedCCF();
		if(showLog) phaseCorrection.logLastComputedShift();
			
    		return IJ.setupDialog(imp, flags);
	}

	@Override
	public void setNPasses(int nPasses) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		maxDisplacement=(int) gd.getNextNumber();
		isEven=gd.getNextChoiceIndex()==0;
		showPlot=gd.getNextBoolean();
		showTable=gd.getNextBoolean();
		showLog=gd.getNextBoolean();
		
		return true;
	}

}
