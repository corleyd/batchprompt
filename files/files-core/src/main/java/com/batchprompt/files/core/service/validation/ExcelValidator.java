package com.batchprompt.files.core.service.validation;

import com.batchprompt.files.core.model.FileEntity;
import com.batchprompt.files.core.model.FileRecord;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExcelValidator {

    private final ObjectMapper objectMapper;

    /**
     * Interface for processing records as they are read from the Excel file
     */
    public interface RecordProcessor {
        /**
         * Process a validated record from the Excel file
         * @param fileRecord The FileRecord to process
         */
        void processRecord(FileRecord fileRecord);
    }

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
        private final List<FieldInfo> fields;
        private final int recordCount;

        public ValidationResult(boolean valid, JsonNode errors, List<FieldInfo> fields, int recordCount) {
            this.valid = valid;
            this.errors = errors;
            this.fields = fields;
            this.recordCount = recordCount;
        }

        public boolean isValid() {
            return valid;
        }

        public JsonNode getErrors() {
            return errors;
        }
        
        public List<FieldInfo> getFields() {
            return fields;
        }
        
        public int getRecordCount() {
            return recordCount;
        }
    }

    /**
     * Validates an Excel file and returns field information and validation errors.
     * Records are processed via the callback to avoid loading all records into memory.
     * 
     * @param inputStream The Excel file input stream
     * @param fileEntity The file entity for which records are being created
     * @param recordProcessor Callback to handle each valid record 
     * @return ValidationResult with field information and any validation errors
     */
    public ValidationResult validateExcelFile(InputStream inputStream, FileEntity fileEntity, RecordProcessor recordProcessor) {
        List<String> validationErrors = new ArrayList<>();
        List<FieldInfo> fields = new ArrayList<>();
        int recordCount = 0;

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            // Validation rule 1: Must have exactly one worksheet
            if (workbook.getNumberOfSheets() != 1) {
                validationErrors.add("File must contain exactly one worksheet");
                return createValidationResult(validationErrors, fields, recordCount);
            }

            Sheet sheet = workbook.getSheetAt(0);
            
            // Get header row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                validationErrors.add("File must contain header row");
                return createValidationResult(validationErrors, fields, recordCount);
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
                return createValidationResult(validationErrors, fields, recordCount);
            }
            
            int headerColumnCount = columnNames.size();

            // Validate data rows
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
                
                // Check if data row has more columns than header
                int lastCellNum = row.getLastCellNum();
                if (lastCellNum > headerColumnCount) {
                    validationErrors.add("Row " + row.getRowNum() + " contains more columns than the header row");
                    continue;
                }
                
                // Process valid row into a record
                Map<String, String> recordMap = new HashMap<>();
                // Store the original row number from the file
                recordMap.put("_rowNum", String.valueOf(row.getRowNum() + 1)); // +1 to convert to 1-based index for user display
                for (int i = 0; i < headerColumnCount; i++) {
                    Cell cell = row.getCell(i);
                    String value = (cell != null) ? getCellValueAsString(cell) : "";
                    recordMap.put(columnNames.get(i), value);
                }
                
                // Create a FileRecord instance directly
                JsonNode jsonRecord = objectMapper.valueToTree(recordMap);
                FileRecord fileRecord = FileRecord.builder()
                        .fileRecordUuid(UUID.randomUUID())
                        .file(fileEntity)
                        .recordNumber(recordCount + 1) // Record numbers start at 1
                        .record(jsonRecord)
                        .build();
                
                // Process the FileRecord immediately via the callback
                recordProcessor.processRecord(fileRecord);
                recordCount++;
            }
            
            if (recordCount == 0) {
                validationErrors.add("File must contain at least one data row");
                return createValidationResult(validationErrors, fields, recordCount);
            }
            
        } catch (IOException e) {
            validationErrors.add("Invalid Excel file format: " + e.getMessage());
        } catch (Exception e) {
            validationErrors.add("Error processing file: " + e.getMessage());
        }
        
        return createValidationResult(validationErrors, fields, recordCount);
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

    private ValidationResult createValidationResult(List<String> errors, List<FieldInfo> fields, int recordCount) {
        ObjectNode errorsNode = objectMapper.createObjectNode();
        ArrayNode errorsArray = errorsNode.putArray("errors");
        
        for (String error : errors) {
            errorsArray.add(error);
        }
        
        return new ValidationResult(errors.isEmpty(), errorsNode, fields, recordCount);
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