/**
 *
 *  phaseCorrector v1, 10 now. 2017 
    Fabrice P Cordelieres, fabrice.cordelieres at gmail.com
    
    Copyright (C) 2017 Fabrice P. Cordelieres
  
    License:
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

import java.util.Arrays;

import ij.IJ;
import ij.gui.Plot;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;

/**
 * Thsi class is designed to correct phase issues on confocal images acquired in bidirectionnal mode
 * @author fab
 *
 */
public class phaseCorrection {
	static int lastComputedShift=0;
	static double[] lastComputedX=null;
	static double[] lastComputedCCF=null;
	static boolean lastLineChoice=true;
	
	/**
	 * Applies the correction to the input ImageProcessor
	 * @param iproc the ImageProcessor to correct
	 * @param shift the displacement to apply to half the lines
	 * @param moveEvenLines true is the shift should be applied to even lines
	 * @return a corrected version of the input ImageProcessor
	 */
	public static void applyCorrection(ImageProcessor iproc, int shift, boolean moveEvenLines) {
		for(int i=moveEvenLines?0:1; i<iproc.getHeight(); i+=2) {
			float data[]=null;
			data=iproc.getRow(0, i, data, iproc.getWidth());
			if(shift>0) data=padDataLeft(data, shift);
			if(shift<0) data=padDataRight(data, shift);
			
			iproc.putRow(0, i, data, iproc.getWidth());
		}
	}
	
	/**
	 * Returns a corrected version of the input ImageProcessor
	 * @param iproc the ImageProcessor to correct
	 * @param shift the displacement to apply to half the lines
	 * @param moveEvenLines true is the shift should be applied to even lines
	 * @return a corrected version of the input ImageProcessor
	 */
	public static ImageProcessor correct(ImageProcessor iproc, int shift, boolean moveEvenLines) {
		ImageProcessor out=iproc.duplicate();
		
		for(int i=moveEvenLines?0:1; i<iproc.getHeight(); i+=2) {
			float data[]=null;
			data=iproc.getRow(0, i, data, iproc.getWidth());
			if(shift>0) data=padDataLeft(data, shift);
			if(shift<0) data=padDataRight(data, shift);
			
			out.putRow(0, i, data, iproc.getWidth());
		}
		
		return out;
	}
	
	/**
	 * Returns a corrected version of the input ImageProcessor
	 * @param iproc the input ImageProcessor
	 * @param maxDisplacement maximum displacement to apply during the CCF calculation
	 * @param moveEvenLines true if the CCF is to by calculated by moving the even lines, false is the odd lines are to be moved
	 * @return a corrected ImageProcessor
	 */
	public static ImageProcessor autoCorrect(ImageProcessor iproc, int maxDisplacement, boolean moveEvenLines) {
		computeCorrection(iproc, maxDisplacement, moveEvenLines);
		return correct(iproc, lastComputedShift, moveEvenLines);
	}
	
	/**
	 * Computes the correction to be applie to the input ImageProcessor
	 * @param iproc the input ImageProcessor
	 * @param maxDisplacement maximum displacement to apply during the CCF calculation
	 * @param moveEvenLines true if the CCF is to by calculated by moving the even lines, false is the odd lines are to be moved
	 */
	public static void computeCorrection(ImageProcessor iproc, int maxDisplacement, boolean moveEvenLines) {
		computeCCF(iproc, maxDisplacement, moveEvenLines);
		computeShift();
		lastLineChoice=moveEvenLines;
	}
	
	/**
	 * Computes the cross-correlation function (CCF) between odd and even lines
	 * @param iproc the input ImageProcessor
	 * @param maxDisplacement maximum displacement to apply during the CCF calculation
	 * @param moveEvenLines true if the CCF is to by calculated by moving the even lines, false is the odd lines are to be moved
	 * @return a double array containing the correlation coefficients for displacement between [-maxDisplacement, +maxDisplacement]
	 */
	public static double[] computeCCF(ImageProcessor iproc, int maxDisplacement, boolean moveEvenLines) {
		int nValues=(int) (iproc.getWidth()*Math.floor(iproc.getHeight()/2));
		float[] evenValues=new float[nValues];
		float[] oddValues=new float[nValues];
		double[] CCF=new double[2*maxDisplacement+1];
		
		double[] xAxis=new double[2*maxDisplacement+1];
		for(int i=-maxDisplacement; i<=maxDisplacement; i++) xAxis[i+maxDisplacement]=i;
		
		for(int shift=-maxDisplacement; shift<=maxDisplacement; shift++) {
			ImageProcessor shifted=correct(iproc, shift, moveEvenLines);
			int index=0;
			for(int y=0; y<((int) 2*Math.floor(shifted.getHeight()/2)); y+=2) {
				for(int x=0; x<shifted.getWidth(); x++) {
					evenValues[index]=shifted.getf(x, y);
					oddValues[index++]=shifted.getf(x, y+1);
				}
			}
			CCF[shift+maxDisplacement]=getCorrelationCoefficient(evenValues, oddValues);
		}
		
		lastComputedX=xAxis;
		lastComputedCCF=CCF;
		
		return CCF;	
	}
	
	/**
	 * Computes the CCF between odd and even lines on the input ImageProcessor, then returns the shift where the CCF is at maximum
	 * @param iproc the input ImageProcessor
	 * @param maxDisplacement maximum displacement to apply during the CCF calculation
	 * @param moveEvenLines true if the CCF is to by calculated by moving the even lines, false is the odd lines are to be moved
	 * @return the optimum shift to register the odd and even lines
	 */
	private static int computeShift() {
		int shift=0;
			if(lastComputedCCF!=null) {
			double[] tmpArray=lastComputedCCF.clone();
			Arrays.sort(tmpArray);
			double maxCCF=tmpArray[lastComputedCCF.length-1];
			
			for(int i=1; i<lastComputedCCF.length; i++) shift=lastComputedCCF[i]==maxCCF?i:shift;
			shift-=(lastComputedCCF.length-1)/2;	
		}
		
		lastComputedShift=shift;
		
		return shift;
	}
	
	/**
	 * Returns the last computed shift
	 * @return the last computed shift
	 */
	public static int getLastComputedShift() {
		return lastComputedShift;
	}
	
	/**
	 * Returns the last computed CCF
	 * @return the last computed CCF
	 */
	public static double[] getLastComputedCCF() {
		return lastComputedCCF;
	}
	
	/**
	 * Plots the last computed CCF
	 */
	public static void plotLastComputedCCF() {
		Plot plot=new Plot("CCF ("+(lastLineChoice?"even":"odd")+" lines moved)", "Displacement", "CC");
		plot.addPoints(lastComputedX, lastComputedCCF, Plot.CONNECTED_CIRCLES);
		plot.show();
	}
	
	/**
	 * Logs to a Results Table the last computed CCF
	 */
	public static void logLastComputedCCF() {
		ResultsTable rt=new ResultsTable();
		for(int i=0; i<lastComputedX.length; i++) {
			rt.incrementCounter();
			rt.addValue("Displacement", lastComputedX[i]);
			rt.addValue("Correlation_coefficient", lastComputedCCF[i]);
		}
		rt.show("CCF ("+(lastLineChoice?"even":"odd")+" lines moved)");
	}
	
	/**
	 * Lots to the log window the last computed shift
	 */
	public static void logLastComputedShift() {
		IJ.log("CCF calculated by moving "+(lastLineChoice?"even":"odd")+" lines.");
		IJ.log("Detected displacement: "+lastComputedShift+" pixels.");
	}
	
	/**
	 * Left-pads the input float array with n zeros
	 * @param input the input float array
	 * @param n the number of leading zeros to add on the left side of the array
	 * @return a padded version of the input float array
	 */
	private static float[] padDataLeft(float[] input, int n){
		n=Math.abs(n);
		float[] output=new float[input.length+n];
		
		for(int i=0; i<input.length; i++) output[i+n]=input[i];
		
		return output;
	}
	
	/**
	 * Right-pads the input float array with n zeros
	 * @param input the input float array
	 * @param n the number of leading zeros to add on the right side of the array
	 * @return a padded version of the input float array
	 */
	private static float[] padDataRight(float[] input, int n){
		n=Math.abs(n);
		float[] output=new float[input.length+n];
		
		for(int i=0; i<input.length-n; i++) output[i]=input[i+n];
		
		return output;
	}
	
	/**
	 * Calculates the correlation coefficient between two input float arrays (based on description in Wikipedia)
	 * @param input1 first input float array
	 * @param input2 second input float array
	 * @return the correlation coefficient
	 */
	private static double getCorrelationCoefficient(float[] input1, float[] input2) {
	    double sx = 0.0;
	    double sy = 0.0;
	    double sxx = 0.0;
	    double syy = 0.0;
	    double sxy = 0.0;
	
	    int n = input1.length;
	
	    for(int i = 0; i < n; i++) {
	      double x = input1[i];
	      double y = input2[i];
	
	      sx += x;
	      sy += y;
	      sxx += x * x;
	      syy += y * y;
	      sxy += x * y;
	    }
	
	    // covariation
	    double cov = sxy / n - sx * sy / n / n;
	    // standard error of x
	    double sigmax = Math.sqrt(sxx / n -  sx * sx / n / n);
	    // standard error of y
	    double sigmay = Math.sqrt(syy / n -  sy * sy / n / n);
	
	    // correlation is just a normalized covariation
	    return cov / sigmax / sigmay;
	}
}
