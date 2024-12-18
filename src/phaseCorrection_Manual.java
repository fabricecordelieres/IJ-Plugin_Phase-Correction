import java.awt.AWTEvent;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;

public class phaseCorrection_Manual implements ExtendedPlugInFilter, DialogListener{
	private int flags = DOES_ALL|KEEP_PREVIEW|PARALLELIZE_STACKS;
	PlugInFilterRunner pfr=null;
	
	int shift=(int) Prefs.get("phaseCorrection_Manual_shift.double", 0);
	boolean isEven=Prefs.get("phaseCorrection_Manual_isEven.boolean", true);
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		return flags;
	}

	@Override
	public void run(ImageProcessor ip) {
		phaseCorrection.applyCorrection(ip, shift, isEven);
	}

	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		this.pfr=pfr;
		phaseCorrection.computeCorrection(WindowManager.getCurrentImage().getProcessor(), 20, isEven);
		
		GenericDialog gd=new GenericDialog("Manual Phase Correction");
		gd.addMessage("Infos/bug report: fabrice.cordelieres@gmail.com");
		gd.addMessage("");
		gd.addSlider("Phase_correction_(pixels)", -20,20, shift);
		gd.addChoice("Which_lines_to_move ?", new String[]{"Even", "Odd"},  isEven?"Even":"Odd");
		gd.addMessage("Suggested value: "+phaseCorrection.getLastComputedShift()+" (when moving "+(isEven?"even":"odd")+" lines)");
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		dialogItemChanged(gd, null);
		gd.showDialog();
		
		if (gd!=null && gd.wasCanceled()) return DONE;
      
        	Prefs.set("phaseCorrection_Manual_shift.double", shift);
		Prefs.set("phaseCorrection_Manual_isEven.boolean", isEven);
        return IJ.setupDialog(imp, flags);
	}

	@Override
	public void setNPasses(int nPasses) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		shift=(int) gd.getNextNumber();
		isEven=gd.getNextChoiceIndex()==0;
		
		return true;
	}

}
