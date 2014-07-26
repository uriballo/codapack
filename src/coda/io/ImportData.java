/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package coda.io;

import au.com.bytecode.opencsv.CSVReader;
import coda.DataFrame;
import coda.Variable;
import coda.Zero;
import coda.Zero;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
/**
 *
 * @author mcomas
 */
public class ImportData{
    private ImportData(){ }
    static public DataFrame importXLS(String fname) throws FileNotFoundException, IOException, InvalidFormatException{
        return importXLS(fname, true, "NA","<",0);
    }
    static public DataFrame importXLS(String fname, boolean headers) throws FileNotFoundException, IOException, InvalidFormatException{
        return importXLS(fname, headers, "NA", "<", 0);
    }
    static public DataFrame importXLS(String fname, String notAvailable) throws FileNotFoundException, IOException, InvalidFormatException{
        return importXLS(fname, true, notAvailable, "<", 0);
    }
    static public DataFrame importXLS(String fname, boolean headers, String notAvailable, String notDetected, int rowStart) throws FileNotFoundException, IOException, InvalidFormatException{
        DataFrame df = new DataFrame();
  
        InputStream inp = null;
        try{
            inp = new FileInputStream (fname);
        }catch (FileNotFoundException e){
            System.out.println ("File not found in the specified path.");
            return null;
        }
        Workbook wb = null;
        if( fname.endsWith(".xls") ){
            wb = new HSSFWorkbook(inp);
        }else if( fname.endsWith(".xlsx") ){
            wb = new XSSFWorkbook(inp);
        }else{
            System.out.println ("Format not recognized.");
            return null;
        }
        int nsheet = 0;
        if(wb.getNumberOfSheets() > 1){
            String name_sheet[] = new String[wb.getNumberOfSheets()];
            for(int i=0;i<wb.getNumberOfSheets();i++){
                name_sheet[i] = wb.getSheetName(i);
            }
            final JDialog dialog = new JDialog();
            final JList sheet_selection = new JList(name_sheet);
            final JScrollPane scroll = new JScrollPane();
            sheet_selection.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    if (evt.getClickCount() == 2){
                        dialog.dispose();
                    }
                }
            });
            dialog.setTitle("Choose one sheet");
            dialog.setModal(true);
            dialog.setSize(200,100);
            scroll.setViewportView(sheet_selection);
            dialog.getContentPane().add(scroll);
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            nsheet  = sheet_selection.getSelectedIndex();
        }
        
        Sheet sheet = wb.getSheetAt(nsheet);
        /*
         * Reading the headers
         */
        ArrayList<Integer> columns = new ArrayList<Integer>();
        ArrayList<String> names = new ArrayList<String>();
        ArrayList<Variable> variables = new ArrayList<Variable>();
        if(headers){            
            Row row = sheet.getRow(rowStart);
            Iterator<Cell> cells = row.cellIterator ();
            while(cells.hasNext()){
                Cell cell = cells.next();
                String name = null;
                if(cell.getCellType() == Cell.CELL_TYPE_STRING){
                    name = cell.getRichStringCellValue().getString().trim();
                }
                if(cell.getCellType() == Cell.CELL_TYPE_NUMERIC){
                    name = Double.valueOf(cell.getNumericCellValue()).toString();
                }
                if(name != null){
                    columns.add(cell.getColumnIndex());
                    names.add(name);
                    variables.add(new Variable(name));
                    
                }
            }
        }else{
            Row row = sheet.getRow(rowStart);
            Iterator<Cell> cells = row.cellIterator ();
            int counter = 1;
            while(cells.hasNext()){
                Cell cell = cells.next();
                columns.add(cell.getColumnIndex());
                names.add("V" + counter);
                variables.add(new Variable("V" + counter++));
            }
        }
        /*
         * Reading data and saving it according the data structure
         */
        FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
        Iterator<Integer> it = columns.iterator();
        int k = 0;
        while(it.hasNext()){
            int ncol = it.next();
            String name = names.get(k);
            Variable var = variables.get(k);

            int nrow = (headers ? rowStart+1 : rowStart+0);
            boolean cont = true;
            String str;
            while(cont){
                Row row = sheet.getRow(nrow);
                Cell cell;
                CellValue cellValue;
                try{
                    cell = row.getCell(ncol);
                    try{
                        cellValue = evaluator.evaluate(cell);
                        if( var.isText() ){
                            if(cellValue.getCellType() == Cell.CELL_TYPE_STRING){
                                var.add(cellValue.getStringValue().trim());
                            }else if(cellValue.getCellType() == Cell.CELL_TYPE_NUMERIC){
                                var.add(Double.valueOf(cellValue.getNumberValue()).toString());
                            }
                        }else{
                            if(cellValue.getCellType() == Cell.CELL_TYPE_STRING){
                                str = cellValue.getStringValue().trim();
                                if( str.compareTo(notAvailable) == 0){
                                    var.add(Double.NaN);
                                }else if( str.startsWith(notDetected)){
                                    var.add(new Zero(
                                            Double.parseDouble(
                                            str.substring(1,str.length()).replace(",", "."))));
                                    //var.add(Double.MIN_VALUE);
                                }else{
                                    var.toText();
                                    var.add(str);
                                }
                            }else if(cellValue.getCellType() == Cell.CELL_TYPE_NUMERIC){
                                var.add(Double.valueOf(cellValue.getNumberValue()));
                            }
                        }
                    }catch(NotImplementedException nie){
                        JOptionPane.showMessageDialog(null, "Error evaluating: " + cell.getCellFormula() + " in column " + name);
                        break;
                    }
                }catch(NullPointerException ex){
                    cont = false;
                }
                nrow++;
            }
            df.addData(name, var);
            k++;
        }
        return df;
    }
    
    /**
     *
     */
    public static String NON_AVAILABLE = "NA";
    /**
     *
     */
    public static String NON_DETECTED  = "<";

    public static boolean header = true;
    /**
     *
     */
    public static char separator = ' ';
    /**
     *
     */
    public static char decimal = '.';
    /**
     *
     */
    public static char quoteChar = '"';
    public static boolean doubleQuote = false;
    /**
     *
     * @param fname
     * @return
     */
    public static DataFrame importText(String fname){
        ArrayList<Variable> variables = new ArrayList<Variable>();
        CSVReader reader = null;
        String [] headers;
        try {
            if (header){
                reader = new CSVReader(new FileReader(fname), separator);
                if ((headers = reader.readNext()) != null) {
                    for (int part = 0; part < headers.length; part++) {
                        variables.add(new Variable(headers[part]));
                        //System.out.println(headers[part]);
                    }
                }
            }else{
                reader = new CSVReader(new FileReader(fname),separator);
                if ((headers = reader.readNext()) != null){
                    for (int part = 0; part < headers.length; part++){
                       String newHeader = "Var"+(part+1);
                       variables.add(new Variable(newHeader));
                    }
                }
                reader = new CSVReader(new FileReader(fname),separator);
            }

            String [] nextLine;
            String stringnumber;
            boolean first = true;
            int istart = 0;
            while ((nextLine = reader.readNext()) != null) {
                if(first){
                    if (nextLine.length  == variables.size()+1){
                        istart = 1;
                    }
                    first = false;
                }
                for (int part = istart; part < nextLine.length; part++){
                    stringnumber = nextLine[part];
                    Variable var = variables.get(part-istart);
                    //var.add(nextLine[part]);
                    if( var.isText() ){
                         var.add(stringnumber.trim());
                    }else{
                        
                        boolean f = false;
                        double num = 0;
                        try{
                            num = Double.parseDouble(stringnumber.replace(",", "."));
                        }catch(NumberFormatException e){
                            f = true;
                        }
                        if(f){
                            String str = stringnumber.trim();
                            if( str.compareTo(NON_AVAILABLE) == 0){
                                var.add( Double.NaN);
                            }else if( str.startsWith(NON_DETECTED)){
                                var.add(new Zero(
                                        Double.parseDouble(
                                        str.substring(1,str.length()).replace(",", "."))));
                            }else{
                                var.toText();
                                //variables.remove(part);
                                //variables.add(part, var);
                                //var.categorize();
                                var.add(str);
                            }
                        }else{
                            var.add(num);
                        }
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            
        } catch (IOException ex) {
        }
        DataFrame dataframe = new DataFrame();
        for(Variable vv:variables){
            dataframe.addData(vv.getName(), vv);
            /*
            if(vv instanceof NumericVar){
            NumericVar v =(NumericVar)vv;
               for(int i=0;i<v.size();i++){
               System.out.println(v.getName() + " " + v.get(i).getClass() + " " + v.get(i).getValue());
               }
            }else{
               FactorVar v =(FactorVar)vv;
               for(int i=0;i<v.size();i++){
                  System.out.println(v.getName() + " " + v.get(i).getClass() + " " + v.get(i).getFactor());
               }
            }
             * */
        }
        return dataframe;
    }
}
