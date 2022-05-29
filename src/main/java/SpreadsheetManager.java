import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class SpreadsheetManager {
    /** Variables */
    public static int[] columnNumbers;


    /** Helper methods */
    public static boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }

        String cell1 = row.getCell(row.getFirstCellNum()).toString().trim();
        if (cell1.length() > 0 && cell1.charAt(0) == '#') {
            return true;
        }

        DataFormatter dataFormatter = new DataFormatter();
        for(Cell cell: row) {
            if(dataFormatter.formatCellValue(cell).trim().length() > 0) {
                return false;
            }
        }
        return true;
    }

    public static void setupColumnNumbers(Row topRow, String[] requiredHeaders, String[] headers) throws Exception {
        columnNumbers = new int[requiredHeaders.length + headers.length];
        Arrays.fill(columnNumbers, -1);

        // 'i' is the column number. 'j' traverses the headers.
        for (int i = 0; i < topRow.getLastCellNum(); i++) {
            Cell cell = topRow.getCell(i);
            if (cell != null) {
                String cellText = cell.toString().trim();
                // Required headers
                for (int j = 0; j < requiredHeaders.length; j++) {
                    if (cellText.equals(requiredHeaders[j])) {
                        columnNumbers[j] = i;
                        break;
                    }
                }

                // Non-required headers
                for (int j = 0; j < headers.length; j++) {
                    if (cellText.equals(headers[j])) {
                        columnNumbers[j + requiredHeaders.length] = i;
                        break;
                    }
                }
            }
        }

        List<String> missingRequiredFields = new ArrayList<>();
        for (int i = 0; i < requiredHeaders.length; i++) {
            if (columnNumbers[i] == -1) {
                missingRequiredFields.add(requiredHeaders[i]);
            }
        }

        if (missingRequiredFields.size() > 0) {
            StringBuilder sb = new StringBuilder("Missing required headers in the first row: ");
            for (String s : missingRequiredFields) {
                sb.append(s).append(", ");
            }
            JOptionPane.showMessageDialog(Login.getFrame(), sb.substring(0, sb.length() - 2));
            throw new Exception(sb.substring(0, sb.length() - 2));
        }
    }

    public static void setupColumnNumbers(Row topRow, String[] headers) throws Exception {
        setupColumnNumbers(topRow, headers, new String[0]);
    }

    public static String removeDecimal(String s) {
        if (s.endsWith(".0")) {
            return s.substring(0, s.length() - 2);
        }
        return s;
    }

    public static Workbook getWorkbook(FileInputStream fis, File file) throws IOException {
        String extension = file.toString().substring(file.toString().length() - 4);

        if (extension.equals(".xls")) {
            return new HSSFWorkbook(fis);
        } else { // xlsx
            return new XSSFWorkbook(fis);
        }
    }

    public static Iterator<Row> getRowIterator(Workbook w) {
        Sheet sheet = w.getSheetAt(0);
        return sheet.rowIterator();
    }

    public static Row getTopRow(Iterator<Row> rowIterator) {
        Row topRow = rowIterator.next();
        while (SpreadsheetManager.isRowEmpty(topRow)) {
            topRow = rowIterator.next();
        }
        return topRow;
    }


    /** Driver method */
    public static void parseSpreadsheet(File file) throws Exception {
        if (SelectMode.getUpdateMode() > -1) {
            System.out.println("Update spreadsheet");
            UpdateSpreadsheet.parseSpreadsheet(file);
        } else if (SelectMode.getMigrationMode() > -1) {
            System.out.println("Migration spreadsheet");
            MigrationSpreadsheet.parseSpreadsheet(file);
        }
    }
}
