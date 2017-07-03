package coda.gui;

import coda.DataFrame;
import coda.Numeric;
import coda.Text;
import coda.Variable;
import coda.Workspace;
import coda.gui.table.ExcelAdapter;
import java.util.HashSet;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.spreadsheet.GridBase;
import org.controlsfx.control.spreadsheet.SpreadsheetCell;
import org.controlsfx.control.spreadsheet.SpreadsheetCellType;
import org.controlsfx.control.spreadsheet.SpreadsheetColumn;
import org.controlsfx.control.spreadsheet.SpreadsheetView;


/**
 *
 * @author marc
 */
public final class Table extends BorderPane{

    /**
     *
     */
    
    SpreadsheetView spv;
    /**
     *
     */
    
    public Table(){
        
        int rowCount = 0;
        int columnCount = 0;
        GridBase grid = new GridBase(rowCount, columnCount);

        ObservableList<ObservableList<SpreadsheetCell>> rows = FXCollections.observableArrayList();
//        for (int row = 0; row < grid.getRowCount(); ++row) {
//            final ObservableList<SpreadsheetCell> list = FXCollections.observableArrayList();
//            for (int column = 0; column < grid.getColumnCount(); ++column) {
//                list.add(SpreadsheetCellType.STRING.createCell(row, column, 1, 1,"value"));
//            }
//            rows.add(list);
//        }
        //grid.setRows(rows);

        spv = new SpreadsheetView(grid);
        this.setCenter(spv);
    }
    public void addDataFrame(DataFrame df){
        int rowCount = df.nObservations();
        int columnCount = df.nVariables();
        GridBase grid = new GridBase(rowCount, columnCount);


        grid.getColumnHeaders().setAll(df.getNames());
        
        ObservableList<ObservableList<SpreadsheetCell>> rows = FXCollections.observableArrayList();
        for (int row = 0; row < grid.getRowCount(); ++row) {
            final ObservableList<SpreadsheetCell> list = FXCollections.observableArrayList();
            for (int column = 0; column < grid.getColumnCount(); ++column) {
                Variable var = df.get(column); 
                if(var.isNumeric()){
                    Numeric num = (Numeric)var.get(row);
                    list.add(SpreadsheetCellType.DOUBLE.createCell(row, column, 1, 1, num.getValue()));
                }else{
                    Text txt = (Text)var.get(row);
                    list.add(SpreadsheetCellType.STRING.createCell(row, column, 1, 1, txt.getValue()));
                }                
            }
            rows.add(list);
        }
        grid.setRows(rows);
        spv.setGrid(grid);
        ObservableList<SpreadsheetColumn> cols = spv.getColumns();
        for(SpreadsheetColumn col: cols){
            col.setPrefWidth(70);
            
        }
    }
}