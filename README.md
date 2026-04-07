# AMHS/SWIM Gateway Test-Tool Manual Injection-Based Workflow

## Overview / Giới thiệu
This tool is designed to validate the compliance of the AMHS/SWIM Gateway according to **ICAO EUR Doc 047 Appendix A**. It serves as a manual payload injector to test both AMHS-to-AMQP and AMQP-to-AMHS conversion logic.

Công cụ này được thiết kế để xác thực tính tuân thủ của AMHS/SWIM Gateway theo tài liệu **ICAO EUR Doc 047 Appendix A**. Đây là công cụ phun tải (payload injector) thủ công để kiểm thử logic chuyển đổi giữa AMHS và AMQP.

---

## 1. Installation & Build / Cài đặt & Biên dịch
### Requirements / Yêu cầu:
- Java Development Kit (JDK) 11+.
- Maven 3.6+.
- Access to an AMHS MTA and SWIM Message Broker.

### Build:
```bash
mvn package
```
The executable JAR will be located at `target/amhs-swim-test-tool-1.0.0.jar`.

---

## 2. Configuration / Cấu hình
Before running, configure connection parameters in `config/test.properties`.

Trước khi chạy, hãy cấu hình các tham số kết nối trong `config/test.properties`.

### Key Parameters / Các tham số chính:
- `swim.broker.host`: SWIM Broker address.
- `gateway.test_recipient`: Default AMHS recipient address (O/R address).
- `gateway.max_size`: Maximum message size limit (bytes).
- `amqp_broker_profile`: Broker type (STANDARD, SOLACE, RABBITMQ, etc.)

---

## 3. Manual Injection Workflow / Quy trình Kiểm thử Thủ công
According to EUR Doc 047, test results are **manually verified**. The tool automates injection and logs specific verification steps for the tester.

Theo EUR Doc 047, các kết quả kiểm thử được **xác thực thủ công**. Công cụ tự động hóa việc phun tin nhắn và ghi lại các bước xác thực cụ thể cho người kiểm thử.

### Steps / Các bước:
1. **Launch GUI / Khởi chạy giao diện**:
   ```bash
   java -jar target/amhs-swim-test-tool-1.0.0.jar
   ```
2. **Check Connection / Kiểm tra kết nối**: Verify the broker status in the UI.
3. **Select Test Case / Chọn Test Case**: Choose a test (e.g., CTSW101) from the list.
4. **Input Parameters / Nhập tham số**: (Optional) Customize the payload or priority.
5. **Execute / Thực thi**: Press the injection button.
6. **Verify Results / Xác thực kết quả**: Follow the **detailed steps in the GUI log** or refer to the [Manual Verification Guide](docs/MANUAL_VERIFICATION_GUIDE.md).

---

## 4. Documentation / Tài liệu
- [Manual Verification Guide](docs/MANUAL_VERIFICATION_GUIDE.md): Detailed verification steps for CTSW101-CTSW116.
- [Adapter Architecture](docs/ADAPTER_ARCHITECTURE.md): Technical details of the SWIM Messaging Adapters.
- [Protocol Mapping](docs/API_MAPPING.md): Mapping between AMQP 1.0 and X.400 attributes.
- [Security Guide](docs/SECURITY_GUIDE.md): Configuring signed messages (AMHS SEC).

---

## 5. Troubleshooting / Xử lý sự cố
- **X400_E_NOCONNECT**: Check AMHS MTA gateway channel and network connectivity.
- **JCSMPException**: Ensure Solace Broker is running and VPN name is correct.
- **Signature Failure**: Update `gateway_identity.p12` and import CA certs.