# Implementation Plan: ICAO EUR Doc 047 Compliance & Enhancement

This plan outlines the steps to ensure the **AMHS/SWIM Gateway Test Tool** fully complies with the ICAO EUR Doc 047 Appendix A (Testing Plan) requirements for test cases **CTSW101 through CTSW116**. It also includes enhancements for penetration testing and protocol-level customization.

## 1. Compliance Gap Analysis (CTSW101 - CTSW116)

Based on the audit of `SwimToAmhsTests.java` and `cases.json`, the following enhancements are needed:

- **CTSW113 (Notifications):** Currently hardcodes `rn,nrn`. Needs a parameter to test individual notification requests (RN or NRN).
- **CTSW115 (Encoding):** Hardcodes 3 scenarios in a loop. Needs a dynamic selector for Body Part Type and Encoding to allow manual "fuzzing" of these fields.
- **CTSW116 (FTBP/GZIP):** Needs more parameters for the second scenario (GZIP) to allow custom compressed payloads.
- **Penetration Testing:** The tool lacks a mechanism to inject "unexpected" AMQP application properties (fuzzing).

## 2. Proposed Technical Changes

### A. Core Messaging Layer (Penetration Testing Support)
- **Modify `AMQPProperties` (`SwimDriver.java`):** Add a `Map<String, Object> customProperties` field.
- **Modify `QpidSwimAdapter.java`:** Update `publishMessage` to iterate through `customProperties` and add them to the `ApplicationProperties` section. This allows injecting any key-value pair, supporting negative/penetration testing.

### B. Test Case Refinement
- **Modify `SwimToAmhsTests.java`:**
    - **CTSW113:** Add parameters for `Request RN` and `Request NRN`.
    - **CTSW115:** Add dropdown-style parameters for `Body Part Type` (IA5, General-Text) and `Encoding` (ISO-8859-1, etc.).
    - **CTSW116:** Add a parameter for `Compression Type` (None, GZIP).

### C. GUI Enhancements
- **Modify `TestParamDialog.java`:** Ensure it can handle dynamic key-value pairs for the new `customProperties` field if we decide to expose it globally.

## 3. Compliance Documentation
- **Create `docs/ICAO_COMPLIANCE_REPORT.md`:** A formal trace-ability matrix mapping ICAO requirements (Page 39-59 of Appendix A) to specific lines of code.

## 4. Verification Plan

### AMQP 1.0 Protocol Verification
- Run the tool against a standard AMQP 1.0 broker (e.g., RabbitMQ).
- Use `qpid-receive` or a similar tool to inspect the raw AMQP frames and verify:
    - Application properties are correctly formatted.
    - Custom properties (for penetration testing) are present.
    - Binary payloads (GZIP) are correctly handled in the `Data` section.

### Functional Verification
- Execute `CTSW113` and verify on the AMHS side that the notification flags are correctly set in the P1 envelope.
- Execute `CTSW115` with various encodings and verify character mapping in the AMHS IPM body.

---

> [!IMPORTANT]
> **User Approval Required**: Please confirm if you would like the "Custom Property" field to be available in **every** test case dialog or just as a general tool configuration. I recommend adding it to every case for maximum flexibility during penetration testing.
