import org.apache.poi.ss.usermodel.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class UpdateSpreadsheet {
    /** Column headers */
    private static final String[] requiredContractHeaders = {
        "LINE", "ITEMID", "QUANTITY", "UNIT", "LOCATIONID",
        "TRANSACTIONTYPE", "DATE", "CUSTOMER_ID",
    };
    private static final String[] optionalContractHeaders = {
        "GLPOSTINGDATE", "DOCUMENTNO", "DATEDUE", "REFERENCENO", "STATE",
        "PRICE", "TAXABLE", "BILLING_TEMPLATE", "DEPARTMENTID", "MEMO", "RETAINAGEPERCENTAGE", "PROJECTID", "CONVERSIONTYPE", "TASKID", "COSTTYPEID"
    };
    private static final String[] requiredPurchasingTxHeaders = {
        "TD Name", "Item Group", "Debit/Credit", "GL Account", "Is Offset?"
    };
    private static final String[] requiredCostCodeHeaders = {
        "ID",
    };
    private static final String[] optionalCostCodeHeaders = {
        "Item ID", "Parent ID", "Billable"
    };
    private static List<String[]> lineItems;


    /** Helper methods */
    private static String[][] initHeaders(Row topRow) throws Exception {
        String[] requiredHeaders;
        String[] headers;
        if (SelectMode.getUpdateMode() == 0) { // Create Contract
            requiredHeaders = requiredContractHeaders;
            headers = optionalContractHeaders;
        } else if (SelectMode.getUpdateMode() == 1) { // Create Contract CO
            // TODO implement
            throw new Exception("Not implemented yet");
        } else if (SelectMode.getUpdateMode() == 2) { // Update Project/Job
            // TODO implement
            throw new Exception("Not implemented yet");
        } else if (SelectMode.getUpdateMode() == 3) { // Update Employee
            // TODO implement
            throw new Exception("Not implemented yet");
        } else if (SelectMode.getUpdateMode() == 4){ // Add/update transaction definitions
            requiredHeaders = requiredPurchasingTxHeaders;
            headers = new String[0];
        } else { // Update standard cost codes
            requiredHeaders = requiredCostCodeHeaders;
            headers = optionalCostCodeHeaders;
        }
        SpreadsheetManager.setupColumnNumbers(topRow, requiredHeaders, headers);

        return new String[][]{requiredHeaders, headers};
    }


    /** Create contract */
    private static Object[] createContract(String[] values, boolean firstOperation, String[] lastValues) {
        if (values[0] != null && !values[0].isEmpty()) {
            // If new line entry, submit contract using lineItems and lastValues. Reset line items
            if (values[0].equals("1.0") || values[0].equals("1")) {
                if (firstOperation) {
                    firstOperation = false;
                } else {
                    String[] finalLastValues = lastValues;
                    List<String[]> finalLineItems = new ArrayList<>(lineItems);
                    (new Thread(() -> {
                        try {
                            RequestsManager.createContract(finalLastValues, finalLineItems);
                        } catch (IOException | ParserConfigurationException | SAXException e) {
                            e.printStackTrace();
                        }
                    })).start();
                    lineItems = new ArrayList<>();
                }
                lastValues = values;
            }

            // Line item headers: "LINE", "ITEMID", "QUANTITY", "UNIT", "LOCATIONID", "PRICE", "TAXABLE", "BILLING_TEMPLATE", "DEPARTMENTID",
            //                    "MEMO", "RETAINAGEPERCENTAGE", "PROJECTID", "CONVERSIONTYPE", "TASKID", "COSTTYPEID"
            String[] lineItem = new String[15];
            for (int i = 0; i < 15; i++) {
                if (i <= 4) {
                    lineItem[i] = values[i];
                } else {
                    lineItem[i] = values[i + 8];
                }
            }
            lineItems.add(lineItem);
        }

        return new Object[]{lastValues, firstOperation};
    }


    /** Update purchasing TDs */
    private static void updatePurchasingTDs(String[] values, String recordNo, int[] columnNumbers, List<String[]> allPurchasingTDFields) throws Exception {
        if (recordNo.length() == 0 && !values[columnNumbers[0]].isEmpty()) {
            String tdName = values[columnNumbers[0]];
            System.out.println("TD NAME: " + tdName);
            recordNo = RequestsManager.getRecordNoFromTDName(tdName);

            if (recordNo.contains("Error")) {
                Upload.setTextArea(recordNo);
                throw new Exception(recordNo);
            }
        }

        allPurchasingTDFields.add(values);
    }

    /** Get category headers for updating standard cost types */
    private static List<Integer> getCategoryHeadersForStandardCostTypes(Row topRow) {
        String base = "Standard Category ";
        List<Integer> categoryColumnNumbers = new ArrayList<>();
        for (int i = 0; i < topRow.getLastCellNum(); i++) {
            Cell cell = topRow.getCell(i);
            if (cell == null) continue;

            String text = cell.toString().trim();
            if (text.contains(base)) {
                categoryColumnNumbers.add(i);
            }
        }

        return categoryColumnNumbers;
    }

    /** Update standard cost types */
    private static void updateStandardCostTypes(String[] values, List<Integer> categoryColumnNumbers) {

    }


    /** Main driver for parsing the spreadsheet */
    public static void parseSpreadsheet(File file) throws Exception {
        // Setup initial spreadsheet vars
        FileInputStream fis = new FileInputStream(file);
        Workbook workbook = SpreadsheetManager.getWorkbook(fis, file);
        Iterator<Row> rowIterator = SpreadsheetManager.getRowIterator(workbook);

        // Assigns the column corresponding to each field
        Row topRow = SpreadsheetManager.getTopRow(rowIterator);
        String[][] temp = initHeaders(topRow);
        String[] requiredHeaders = temp[0];
        String[] headers = temp[1];

        Upload.setTextArea("File upload successful...\n\n");

        // Create contract variables
        String[] lastValues = new String[0];
        boolean firstOperation = true;
        lineItems = new ArrayList<>();

        // Purchasing transaction definition variables
        String recordNo = "";
        List<String[]> allPurchasingTDFields = new ArrayList<>();

        // Update standard cost code variables
        List<Integer> categoryColumnNumbers = getCategoryHeadersForStandardCostTypes(topRow);

        // Loop through rows...
        int[] columnNumbers = SpreadsheetManager.columnNumbers;
        while (rowIterator.hasNext()) {
            // Select non-empty row
            Row row = rowIterator.next();
            if (SpreadsheetManager.isRowEmpty(row)) {
                continue;
            }

            // Setup values
            String[] values = new String[requiredHeaders.length + headers.length];
            for (int i = 0; i < requiredHeaders.length; i++) {
                Cell cell = row.getCell(columnNumbers[i]);
                if (cell != null) {
                    values[i] = row.getCell(columnNumbers[i]).toString().trim();
                }
            }
            for (int i = 0; i < headers.length; i++) {
                int index = requiredHeaders.length + i;
                if (columnNumbers[index] != -1) {
                    Cell cell = row.getCell(columnNumbers[index]);
                    if (cell != null) {
                        values[index] = cell.toString().trim();
                    }
                }
            }

            // Create contract
            if (SelectMode.getUpdateMode() == 0) {
                Object[] tempArr = createContract(values, firstOperation, lastValues);
                lastValues = (String[]) tempArr[0];
                firstOperation = (boolean) tempArr[1];
            }

            // Update purchasing TDs
            else if (SelectMode.getUpdateMode() == 4) {
                updatePurchasingTDs(values, recordNo, columnNumbers, allPurchasingTDFields);
            }

            // Update standard cost code
            else if (SelectMode.getUpdateMode() == 5) {
                updateStandardCostTypes(values, categoryColumnNumbers);
            }
        }

        // Do last contract creation
        if (SelectMode.getUpdateMode() == 0) {
            String[] finalLastValues = lastValues;
            (new Thread(() -> {
                try {
                    RequestsManager.createContract(finalLastValues, lineItems);
                } catch (IOException | ParserConfigurationException | SAXException e) {
                    e.printStackTrace();
                }
            })).start();
        }
        else if (SelectMode.getUpdateMode() == 4) { // Make big request for updating purchasing TDs
            RequestsManager.updatePurchasingTDs(allPurchasingTDFields, columnNumbers, recordNo);
            Upload.appendTextArea("");
        }

        // Close files to prevent memory leak
        workbook.close();
        fis.close();
    }
}
