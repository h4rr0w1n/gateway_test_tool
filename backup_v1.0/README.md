Hướng dẫn Sử dụng Công cụ Kiểm thử AMHS/SWIM Gateway
1. Giới thiệu
Công cụ kiểm thử (Test Tool) này được thiết kế để xác thực tính tuân thủ của AMHS/SWIM Gateway theo tài liệu EUR Doc 047 Appendix A. Công cụ hỗ trợ gửi và nhận tin nhắn qua cả hai giao thức:
AMHS (X.400): Sử dụng Isode X.400 Gateway API.
SWIM (AMQP): Sử dụng Solace PubSub+ Event Broker.
2. Yêu cầu Hệ thống
Java Development Kit (JDK): Phiên bản 11 trở lên.
Maven: Phiên bản 3.6 trở lên.
Thư viện bên thứ 3:
Isode X.400 Gateway API (JNI Wrapper cho Java).
Isode Directory API (JNI Wrapper cho Java).
Solace JCSMP (Java API cho AMQP).
Hạ tầng kiểm thử:
AMHS MTA (ví dụ: Isode MTA) đã được cấu hình kênh Gateway.
SWIM Message Broker (ví dụ: Solace) đã được cấu hình Queue/Topic.
ATN Directory Service (LDAP/DAP) có dữ liệu ánh xạ địa chỉ.
3. Cài đặt và Build
3.1. Tải phụ thuộc
Đảm bảo các file JAR của Isode và Solace được đặt trong thư mục lib/ hoặc đã được cài đặt vào kho Maven cục bộ.
3.2. Biên dịch dự án
Sử dụng lệnh Maven để đóng gói dự án:
bash
1
File thực thi sẽ nằm tại target/amhs-swim-test-tool-1.0.0.jar.
4. Cấu hình
Trước khi chạy, bạn cần cấu hình các tham số kết nối trong file config/test.properties.
4.1. Tham số kết nối AMHS
properties
12345
4.2. Tham số kết nối SWIM (AMQP)
properties
123456
4.3. Tham số Directory
properties
12345
4.4. Tham số Giới hạn (Constraints)
Dùng để kiểm thử các trường hợp từ chối (Rejection Tests).
properties
1234
4.5. Bảo mật (Security)
Để kiểm thử AMHS SEC (Signed Messages), cấu hình đường dẫn tới file PKCS#12.
properties
1234
5. Thực thi Kiểm thử
5.1. Chế độ Giao diện (GUI)
Khởi chạy công cụ với giao diện đồ họa để chọn từng test case thủ công:
bash
1
Bước 1: Kiểm tra trạng thái kết nối (Connect Status) ở góc trên màn hình.
Bước 2: Chọn nhóm test (AMHS→SWIM hoặc SWIM→AMHS).
Bước 3: Nhấn nút tương ứng với mã Test Case (ví dụ: CTSW001).
Bước 4: Quan sát khung Log để xem kết quả PASS/FAIL và chi tiết lỗi.
5.2. Chế độ Dòng lệnh (CLI) - Tự động hóa
Để chạy toàn bộ bộ test hoặc một nhóm test cụ thể (phục vụ CI/CD):
bash
12345678
6. Ánh xạ Test Case (Theo Appendix A)
Công cụ hỗ trợ đầy đủ 36 test case được định nghĩa trong EUR Doc 047 Appendix A.
Mã Test
Hướng
Mô tả ngắn
Trạng thái hỗ trợ
CTSW001
AMHS → SWIM
Chuyển đổi IPM cơ bản sang AMQP
✅ Đã triển khai
CTSW004
AMHS → SWIM
Sinh NDR khi lỗi cú pháp Header
✅ Đã triển khai
CTSW006
AMHS → SWIM
Từ chối message quá kích thước
✅ Đã triển khai
CTSW011
AMHS → SWIM
Kiểm thử Probe Conveyance
✅ Đã triển khai
CTSW101
SWIM → AMHS
Chuyển đổi AMQP unaware sang AMHS
✅ Đã triển khai
CTSW102
SWIM → AMHS
Từ chối AMQP thiếu thông tin bắt buộc
✅ Đã triển khai
CTSW110
SWIM → AMHS
Từ chối AMQP sai content-type
✅ Đã triển khai
CTSW116
SWIM → AMHS
Chuyển đổi binary message với FTBP
✅ Đã triển khai
(Xem file docs/TEST_CASES.md để biết chi tiết kịch bản cho từng mã test)
7. Logging và Giám sát
Công cụ ghi log theo yêu cầu của Chương 4.3.1 (Traffic logging) trong tài liệu đặc tả.
File Log: logs/traffic.log
Nội dung log:
Thời gian sự kiện (Event Date/Time).
Định danh message (MTS-Identifier, IPM-Identifier).
Hướng truyền (AMHS→SWIM hoặc ngược lại).
Hành động thực hiện (Convert, Reject, Report).
Mã lỗi (nếu có).
Ví dụ một dòng log:
text
1
8. Xử lý Sự cố (Troubleshooting)
8.1. Lỗi kết nối AMHS
Triệu chứng: Log báo X400_E_NOCONNECT.
Nguyên nhân: MTA chưa bật kênh Gateway hoặc tường lửa chặn cổng.
Khắc phục: Kiểm tra file x400tailor của MTA và cấu hình mạng.
8.2. Lỗi kết nối SWIM
Triệu chứng: Log báo JCSMPException: Cannot connect to host.
Nguyên nhân: Solace Broker chưa chạy hoặc VPN name sai.
Khắc phục: Kiểm tra swim.broker.host và swim.broker.vpn trong file cấu hình.
8.3. Lỗi Xác thực Chữ ký (Signature)
Triệu chứng: Test case CTSW004/CTSW110 báo Fail dù message hợp lệ.
Nguyên nhân: Certificate hết hạn hoặc không tin cậy.
Khắc phục: Cập nhật file gateway_identity.p12 và đảm bảo CA Cert đã được import vào truststore.