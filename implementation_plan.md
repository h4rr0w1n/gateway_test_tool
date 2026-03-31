# AMHS/SWIM Gateway Testing Tool Implementation Plan

## Goal Description
Develop a comprehensive testing tool to validate the EUR Doc 047 Appendix A - AMHS/SWIM Gateway operations. The tool orchestrates AMHS-to-AMQP and AMQP-to-AMHS test scenarios. Assuming RESTful-compatible API interfaces to the target system (IUT), the tool will serve both as the AMHS Test Tool (injecting X.400/P1/P3/P7 equivalent structures via REST) and the SWIM Test Tool (exchanging AMQP properties via REST envelopes). 

Key UX/UI requirement: Test domains must be visibly separated, with 1-click execution buttons available to run entire domains. Cases involving data validation/rejection are flagged with `***` to facilitate integration of custom pentesting payloads.

## User Review Required
- Please review if the UI grouping (A-J domains) and the test execution engine setup meet your expectations for the 1-click execution.
- If you have an existing UI framework preference (e.g., Streamlit, React, Vue, PyQt), please let me know so we can scaffold that.

## Proposed Changes

### 1. Architecture & UI Design
- **Frontend Interface (e.g., React/Vue or Streamlit):**
  - **Dashboard Layout:** A clear, domain-centric view. 
  - **1-Click Execution Buttons:** Each major domain (A through J) will have a dedicated "Run Domain Tests" button.
  - **Visual Highlighting:** Malformed data/syntax check cases will be prominently marked `***` for pentest payload injection.
- **Backend/Execution Engine (Python/Node.js):**
  - **RESTful Adapter:** Converts AMHS and AMQP test instructions into REST requests.
  - **Test Case Orchestrator:** Manages sequential/parallel execution of test cases within a domain.
  - **Assertion & Verdict Engine:** Verifies payload conversions, HTTP response codes (mapping to DR/NDRs), and logs.

### 2. Domain & Test Case Catalog

#### Chapter 4: Gateway Operations (AMHS → AMQP)
- **Domain A: Normal Message Conversion**
  - CTSW001, CTSW002, CTSW009, CTSW020
- **Domain B: Report Generation for Successful Delivery**
  - CTSW003
- **Domain C: NDR Generation on Rejection**
  - CTSW004: Syntax error in ATS-message-header `***`
  - CTSW005: Time exceeds latest delivery time
  - CTSW006: Payload size exceeds configured maximum `***`
  - CTSW007: Multiple body parts `***`
  - CTSW008: Unsupported content-type `***`
  - CTSW010: Addressing more AMQP consumers than maximum `***`
- **Domain D: Body Part Type and Encoding Validation**
  - CTSW016: Processing of current EIT `***`
  - CTSW017: Incoming IPM with an ia5-text-body-part `***`
  - CTSW018: Incoming IPM with a general-text-body-part (ISO 646)
  - CTSW019: Incoming IPM with general-text-body-part (non-ISO 646) `***`
- **Domain E: Probe Handling**
  - CTSW011, CTSW012, CTSW013
- **Domain F: Receipt Notification and RN Handling**
  - CTSW014, CTSW015

#### Chapter 5: Gateway Operations (AMQP → AMHS)
- **Domain G: Normal Message Conversion**
  - CTSW101, CTSW103, CTSW104, CTSW105, CTSW106, CTSW107, CTSW108, CTSW109
- **Domain H: Rejection and Validation**
  - CTSW102: Reject AMQP message missing minimum required information `***`
  - CTSW110: Reject AMQP message with unsupported content-type `***`
  - CTSW111: Reject AMQP message if payload size exceeds maximum `***`
  - CTSW112: Reject AMQP message addressing more AMHS users than maximum `***`
- **Domain I: Body Part Type and Encoding**
  - CTSW115: amqp-value with different amhs_bodypart_type and encoding `***`
  - CTSW116: Binary message with FTBP attributes
- **Domain J: Incoming Reports and Notifications**
  - CTSW113, CTSW114

### 3. Preparation for Pentesting Payloads (`***`)
For all tests flagged with `***`, the tool architecture will allow an external JSON/YAML dictionary of payloads to override the standard REST payload bodies. This encourages security testing (e.g., providing a 10MB string for CTSW006/CTSW111 or sending `\n\r` padding bytes in ATS headers for CTSW004).

## Verification Plan

### Automated Tests
- Mocks will be established for the AMHS and AMQP endpoints to simulate successful responses and structured error reports.
- Component tests will execute individual module payload serialization/deserialization logic.

### Manual Verification
- Will start by running the application locally.
- A user dry run will visually check that all Domains and Test cases are properly rendered.
- 1-click domain execution routines will be functionally checked against the mock endpoint adapter.
