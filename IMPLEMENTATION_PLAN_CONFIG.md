# Implementation Plan: Configuration & GUI Enhancements

## Summary of Requirements

1. **Configurable Case Payloads via Config File**: Each test case payload should be configurable via a config file instead of loading defaults each time. Include a "Revert To Default" button.

2. **CTSW1xx Default Payloads Configuration**: When clicking CTSW1xx cases, default payloads should be configurable and sent the same as other cases.

3. **Modernized Payload Display**: The display of case payloads before sending should be modernized. AMQP content should have a new button to add a file/binary from the system (file picker dialog).

4. **Guideline Editing**: The guideline should be edited to fit with the current chosen case. Need to identify where the guide is parsed in.

---

## Current Architecture Analysis

### Key Files Involved:

1. **`TestFrame.java`** - Main GUI frame with test buttons and configuration tab
2. **`TestParamDialog.java`** - Dialog for entering test parameters before execution
3. **`SwimToAmhsTests.java`** - Contains all test case definitions (CTSW101-CTSW116) with hardcoded default payloads
4. **`BaseTestCase.java`** - Base class defining `TestParameter` structure
5. **`TestConfig.java`** - Configuration manager for broker settings
6. **`cases.json`** - Contains guideline/documentation text for each test case

### Current Flow:
1. User clicks a test case button (e.g., CTSW101) in `TestFrame.java`
2. `runTest()` method calls `testCase.getRequiredParameters()` to get parameter definitions
3. `TestParamDialog` is created with these parameters, showing default values from `SwimToAmhsTests.java`
4. User modifies values and clicks Execute
5. `testCase.execute(userInputs)` is called with the user-provided values

---

## Implementation Steps

### Step 1: Create Case-Specific Configuration File Structure

**New File: `config/case_payloads.properties`**
- Store default payloads for each test case parameter
- Format: `CTSW101.p1=Default Text Payload`, `CTSW101.p2=Default Binary Payload`, etc.

**Modify `TestConfig.java`:**
- Add methods to load/save case-specific payloads
- Add method to revert case payloads to hardcoded defaults
- Add method to get case payload with fallback chain: file → memory → hardcoded default

### Step 2: Extend TestParameter Class

**Modify `BaseTestCase.java.TestParameter`:**
- Add `configKey` field to link parameter to config file entry
- Add `getDefaultValue()` to fetch from config instead of hardcoded value

### Step 3: Update SwimToAmhsTests.java

**For each test case (CTSW101-CTSW116):**
- Change hardcoded default values in `TestParameter` constructors to fetch from config
- Example: `new TestParameter("p1", "Text Payload:", TestConfig.getInstance().getCasePayload("CTSW101", "p1", "Default Text"), true)`

### Step 4: Modernize TestParamDialog.java

**Add File Upload Feature:**
- Add "Browse..." or "Load File" button next to each parameter field
- Use `JFileChooser` to allow users to select files from the system
- Read file content and populate the text area/field
- For binary files, encode as Base64 or hex string for display/editing

**UI Improvements:**
- Better layout with icons
- Clear visual distinction between text and binary payloads
- Show file name when a file is loaded
- Add "Clear" button to reset individual fields

### Step 5: Add Revert To Default Button

**In TestParamDialog:**
- Add "Revert To Default" button that resets all fields to config file values
- Add "Revert All Cases to Default" option in Settings tab

**In TestFrame Settings Tab:**
- Add "Manage Case Payloads" section
- Add "Revert All Case Payloads to Defaults" button
- Show current configured values with edit capability

### Step 6: Guideline Display Enhancement

**Current Location:** Guidelines are in `cases.json` but NOT currently displayed in the GUI

**Where guidelines are used:**
- In `SwimToAmhsTests.java`, the `logManualAction()` method logs verification steps
- These are written to the execution log, not shown before execution

**Implementation:**
- Load guideline from `cases.json` based on selected test case ID
- Display guideline in `TestParamDialog` before execution (read-only panel at top)
- Or create a separate "Guideline Viewer" panel/tab that updates when a case is selected

**Modify `TestParamDialog`:**
- Add constructor parameter for guideline text
- Add scrollable text area at top of dialog showing the guideline
- Parse `cases.json` to extract guideline for the specific case ID

---

## File Changes Summary

| File | Changes |
|------|---------|
| `config/case_payloads.properties` | NEW - Store case-specific default payloads |
| `TestConfig.java` | Add case payload management methods |
| `BaseTestCase.java` | Extend TestParameter with config key support |
| `SwimToAmhsTests.java` | Update all test cases to use config-based defaults |
| `TestParamDialog.java` | Add file browser, modernize UI, show guidelines |
| `TestFrame.java` | Add "Revert All" button in Settings, guideline viewer integration |
| `cases.json` | Keep as-is (already contains guidelines) |

---

## Technical Details

### Config File Format (`config/case_payloads.properties`)
```properties
# Case Payload Configuration
# Format: <CASE_ID>.<PARAM_KEY>=<DEFAULT_VALUE>

# CTSW101
CTSW101.p1=CTSW101 Text Payload
CTSW101.p2=CTSW101 Binary Payload

# CTSW102
CTSW102.payload=CTSW102 Rejection Sample

# CTSW103
CTSW103.p1=CTSW103

# ... etc for all cases
```

### TestConfig Methods to Add
```java
public String getCasePayload(String caseId, String paramKey, String hardcodedDefault)
public void setCasePayload(String caseId, String paramKey, String value)
public void revertCasePayloadsToDefaults()
public Map<String, String> getAllCasePayloads(String caseId)
```

### File Loading in TestParamDialog
```java
JButton btnBrowse = new JButton("📁 Load File");
btnBrowse.addActionListener(e -> {
    JFileChooser chooser = new JFileChooser();
    int result = chooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
        File file = chooser.getSelectedFile();
        String content = new String(Files.readAllBytes(file.toPath()));
        // Set content to the associated text field/area
    }
});
```

### Guideline Loading
```java
// Parse cases.json to get guideline for specific case
JSONObject casesJson = new JSONObject(new String(Files.readAllBytes(Paths.get("cases.json"))));
String guideline = casesJson.getString(caseId);
```

---

## Testing Plan

1. **Unit Tests:**
   - Verify config file loading/saving
   - Test fallback chain (file → memory → hardcoded)
   - Test file reading functionality

2. **GUI Tests:**
   - Click each CTSW1xx button and verify parameters load from config
   - Test "Load File" button with various file types
   - Test "Revert To Default" functionality
   - Verify guideline displays correctly for each case

3. **Integration Tests:**
   - Modify config file externally, restart tool, verify changes persist
   - Execute test cases with modified payloads
   - Verify payloads are sent correctly to AMQP broker

---

## Notes

- The guideline text in `cases.json` is quite long and formatted with special characters. May need preprocessing for clean display.
- Binary file loading should offer encoding options (Base64, Hex, Raw)
- Consider adding validation for file size limits
- Auto-save config on window close (already exists for general config, extend to case payloads)
