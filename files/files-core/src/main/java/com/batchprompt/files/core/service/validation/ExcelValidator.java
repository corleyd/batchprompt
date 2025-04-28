package com.batchprompt.files.core.service.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExcelValidator {

    private final ObjectMapper objectMapper;

    public static class FieldInfo {
        private final String fieldName;
        private final String fieldType;
        private final int fieldOrder;
        private final String description;

        public FieldInfo(String fieldName, String fieldType, int fieldOrder, String description) {
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.fieldOrder = fieldOrder;
            this.description = description;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getFieldType() {
            return fieldType;
        }

        public int getFieldOrder() {
            return fieldOrder;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class ValidationResult {
        private final boolean valid;
        private final JsonNode errors;
        private final List<Map<String, String>> records;
        private final List<FieldInfo> fields;

        public ValidationResult(boolean valid, JsonNode errors, List<Map<String, String>> records, List<FieldInfo> fields) {
            this.valid = valid;
            this.errors = errors;
            this.records = records;
            this.fields = fields;
        }

        public boolean isValid() {
            return valid;
        }

        public JsonNode getErrors() {
            return errors;
        }

        public List<Map<String, String>> getRecords() {
            return records;
        }
        
        public List<FieldInfo> getFields() {
            return fields;
        }
    }

    public ValidationResult validateExcelFile(InputStream inputStream) {
        List<String> validationErrors = new ArrayList<>();
        List<Map<String, String>> records = new ArrayList<>();
        List<FieldInfo> fields = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            // Validation rule 1: Must have exactly one worksheet
            if (workbook.getNumberOfSheets() != 1) {
                validationErrors.add("File must contain exactly one worksheet");
                return createValidationResult(validationErrors, records, fields);
            }

            Sheet sheet = workbook.getSheetAt(0);
            
            // Get header row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                validationErrors.add("File must contain header row");
                return createValidationResult(validationErrors, records, fields);
            }

            // Extract column names from header row and create field info
            List<String> columnNames = new ArrayList<>();
            int fieldOrder = 0;
            for (Cell cell : headerRow) {
                String headerValue = getCellValueAsString(cell);
                if (headerValue != null && !headerValue.isEmpty()) {
                    columnNames.add(headerValue);
                    // Infer field type based on the data in the first data row
                    String fieldType = inferFieldType(sheet, cell.getColumnIndex());
                    fields.add(new FieldInfo(headerValue, fieldType, fieldOrder++, "Imported from Excel"));
                }
            }
            
            if (columnNames.isEmpty()) {
                validationErrors.add("Header row must contain at least one column");
                return createValidationResult(validationErrors, records, fields);
            }
            
            int headerColumnCount = columnNames.size();

            // Validate data rows
            int rowCount = 0;
            for (Row row : sheet) {
                // Skip header row
                if (row.getRowNum() == 0) continue;
                
                // Check if row is empty and skip it
                boolean isEmpty = true;
                for (int i = 0; i < headerColumnCount && isEmpty; i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null && !getCellValueAsString(cell).trim().isEmpty()) {
                        isEmpty = false;
                    }
                }
                if (isEmpty) {
                    continue;  // Skip empty rows
                }
                
                rowCount++;
                
                // Check if data row has more columns than header
                int lastCellNum = row.getLastCellNum();
                if (lastCellNum > headerColumnCount) {
                    validationErrors.add("Row " + row.getRowNum() + " contains more columns than the header row");
                    continue;
                }
                
                // Process valid row into a record
                Map<String, String> record = new HashMap<>();
                // Store the original row number from the file
                record.put("_rowNum", String.valueOf(row.getRowNum() + 1)); // +1 to convert to 1-based index for user display
                for (int i = 0; i < headerColumnCount; i++) {
                    Cell cell = row.getCell(i);
                    String value = (cell != null) ? getCellValueAsString(cell) : "";
                    record.put(columnNames.get(i), value);
                }
                
                records.add(record);
            }
            
            if (rowCount == 0) {
                validationErrors.add("File must contain at least one data row");
                return createValidationResult(validationErrors, records, fields);
            }
            
        } catch (IOException e) {
            validationErrors.add("Invalid Excel file format: " + e.getMessage());
        } catch (Exception e) {
            validationErrors.add("Error processing file: " + e.getMessage());
        }
        
        return createValidationResult(validationErrors, records, fields);
    }

    /**
     * Infer field type based on the data in the first few data rows
     */
    private String inferFieldType(Sheet sheet, int columnIndex) {
        // Check up to 5 data rows to infer type
        for (int i = 1; i <= 5; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            Cell cell = row.getCell(columnIndex);
            if (cell == null) continue;
            
            switch (cell.getCellType()) {
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return "DATE";
                    }
                    // Check if it's an integer
                    if (cell.getNumericCellValue() == Math.floor(cell.getNumericCellValue())) {
                        return "INTEGER";
                    }
                    return "DECIMAL";
                case BOOLEAN:
                    return "BOOLEAN";
                default:
                    return "STRING";
            }
        }
        
        // Default to string if no data found
        return "STRING";
    }

    private ValidationResult createValidationResult(List<String> errors, List<Map<String, String>> records, List<FieldInfo> fields) {
        ObjectNode errorsNode = objectMapper.createObjectNode();
        ArrayNode errorsArray = errorsNode.putArray("errors");
        
        for (String error : errors) {
            errorsArray.add(error);
        }
        
        return new ValidationResult(errors.isEmpty(), errorsNode, records, fields);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                // Convert to string but avoid scientific notation
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}