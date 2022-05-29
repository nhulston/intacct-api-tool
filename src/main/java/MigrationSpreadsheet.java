import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MigrationSpreadsheet {
    /** Column headers */
    private static final String[] projectEstimateHeaders = {
            "PJESTIMATEID", "PJESTIMATETYPENAME", "PROJECTID", "PROJECTLOCATIONID", "PJESTIMATEENTRY_LINENO",
            "PJESTIMATEENTRY_WFTYPE", "PJESTIMATEENTRY_TASKID", "PJESTIMATEENTRY_COSTTYPEID"
    };


    /** Other static vars */
    private static CellStyle style;


    /** Helper methods */
    private static void highlightCell(String value, Cell cell, Set<String> errors, String errorPrefix) {
        String error = errorPrefix + value + "\n";
        if (errors.add(error)) {
            Upload.appendTextArea(error);
        }
        cell.setCellStyle(style);
    }

    private static void highlightCellsNotInList(List<String> list, String value, Cell cell, Set<String> errors, String category) {
        if (list != null && !list.contains(value)) {
            String error = category + " not found: " + value + "\n";
            if (errors.add(error)) {
                Upload.appendTextArea(error);
            }
            cell.setCellStyle(style);
        }
    }

    private static String[] initHeaders(Row topRow) throws Exception {
        String[] headers;
        if (SelectMode.getMigrationMode() == 0 || true) {
            headers = projectEstimateHeaders;
        }
        SpreadsheetManager.setupColumnNumbers(topRow, headers);
        return headers;
    }


    /** Main driver for parsing the spreadsheet */
    public static void parseSpreadsheet(File file) throws Exception {
        // Setup initial spreadsheet vars
        FileInputStream fis = new FileInputStream(file);
        Workbook workbook = SpreadsheetManager.getWorkbook(fis, file);
        Iterator<Row> rowIterator = SpreadsheetManager.getRowIterator(workbook);

        // Orange highlighted
        style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Assigns the column corresponding to each field
        Row topRow = SpreadsheetManager.getTopRow(rowIterator);
        String[] headers = initHeaders(topRow);

        Upload.setTextArea("File upload successful. Please wait a few minutes depending on the file size...\n\n");

        // Project estimate specific code
        List<String> estimateTypes = null;
        List<String> costTypes = null;
        try {
            estimateTypes = RequestsManager.queryField("PJESTIMATETYPE", "NAME");
        } catch (IOException | ParserConfigurationException | SAXException e) { e.printStackTrace(); }
        try {
            costTypes = RequestsManager.queryField("STANDARDCOSTTYPE", "STANDARDCOSTTYPEID");
        } catch (IOException | ParserConfigurationException | SAXException e) { e.printStackTrace(); }
        int expectedLineNumber = 1;


        int[] columnNumbers = SpreadsheetManager.columnNumbers;
        Set<String> errorMessages = new HashSet<>();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (SpreadsheetManager.isRowEmpty(row)) continue;

            // Setup values
            String[] values = new String[headers.length];
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row.getCell(columnNumbers[i]);
                if (cell != null) {
                    values[i] = SpreadsheetManager.removeDecimal(row.getCell(columnNumbers[i]).toString().trim());
                }
            }

            // Sequential line numbers for Estimate ID
            if (values[0] == null || values[0].isEmpty()) {
                expectedLineNumber++;
            } else {
                expectedLineNumber = 1;
            }
            if (!values[4].equals(Integer.toString(expectedLineNumber))) {
                highlightCell(values[4], row.getCell(columnNumbers[4]), errorMessages, "Invalid line #, expected " + expectedLineNumber + ": ");
            }
            // Estimate Types
            highlightCellsNotInList(estimateTypes, values[1], row.getCell(columnNumbers[1]), errorMessages, "Estimate Type Name");
            // Cost Types
            highlightCellsNotInList(costTypes, values[7], row.getCell(columnNumbers[7]), errorMessages, "Cost Type ID");
            // Task ID
            (new Thread(() -> {
                try {
                    String field = RequestsManager.isFieldFound("STANDARDTASK", values[6], null);
                    if (field == null) {
                        highlightCell(values[6], row.getCell(columnNumbers[6]), errorMessages, "Task ID not found: ");
                    }
                } catch (IOException | ParserConfigurationException | SAXException e) {
                    e.printStackTrace();
                }
            })).start();
            // Project ID
            (new Thread(() -> {
                try {
                    String field = RequestsManager.isFieldFound("PROJECT", values[2], "LOCATIONID");
                    if (field == null) {
                        highlightCell(values[2], row.getCell(columnNumbers[2]), errorMessages, "Project ID not found: ");
                    } else {
                        // Location ID
                        if (!field.equals(values[3])) {
                            highlightCell(values[3], row.getCell(columnNumbers[3]), errorMessages, "Location ID doesn't match project: ");
                        }
                    }
                } catch (IOException | ParserConfigurationException | SAXException e) {
                    e.printStackTrace();
                }
            })).start();
            // Estimate WF Type matches Estimate Type Name
            (new Thread(() -> {
                try {
                    String field = RequestsManager.isFieldFound("PJESTIMATETYPE", values[1], "SELECTEDWFTYPES");
                    if (field == null) {
                        highlightCell(values[1], row.getCell(columnNumbers[1]), errorMessages, "Project Estimate Type not found: ");
                    } else {
                        if (!field.equals(values[5])) {
                            highlightCell(values[5], row.getCell(columnNumbers[5]), errorMessages, "Estimate WF Type doesn't match Estimate Type Name: ");
                        }
                    }
                } catch (IOException | ParserConfigurationException | SAXException e) {
                    e.printStackTrace();
                }
            })).start();
        }

        // Set output file and file name
        String name = file.getName();
        String newName;
        if (workbook instanceof HSSFWorkbook) {
            newName = name.substring(0, name.length() - 4) + " out.xls";
        } else {
            newName = name.substring(0, name.length() - 5) + " out.xlsx";
        }
        Upload.setFileToSave(newName, workbook);

        // Close files to prevent memory leak. Workbook is closed when output file saved
        // workbook.close();
        fis.close();
    }
}
