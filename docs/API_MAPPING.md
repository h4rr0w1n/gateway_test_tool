# Ánh xạ API Sử Dụng Trong Tool

## 1. Phía AMHS (X.400)
Sử dụng Isode X.400 Gateway API và Client API (thông qua JNI Wrapper).

| Chức năng | API C (Isode) | Java Wrapper Method | Ghi chú |
| :--- | :--- | :--- | :--- |
| Mở session MTA | `X400mtOpen` | `AmhsDriver.openSession()` | Dùng cho gửi message (Gateway) |
| Tạo message | `X400mtMsgNew` | `AmhsDriver.createMessage()` | |
| Thêm tham số | `X400mtMsgAddStrParam` | `Message.addParameter()` | Thêm O/R Address, Subject... |
| Gửi message | `X400mtMsgSend` | `AmhsDriver.sendMessage()` | |
| Nhận message | `X400msMsgGetStart` | `AmhsDriver.receiveMessage()` | Dùng cho phía Client/UA |
| Kết thúc nhận | `X400msMsgGetFinish` | `AmhsDriver.finishReceive()` | Báo cáo DR/NDR |

## 2. Phía SWIM (AMQP/REST)
Sử dụng Solace JCSMP và Java HttpClient.

| Chức năng | API/Protocol | Java Class | Ghi chú |
| :--- | :--- | :--- | :--- |
| Kết nối Broker | Solace JCSMP | `SwimDriver.connect()` | AMQP 1.0 over TCP/TLS |
| Publish Message | Solace Producer | `SwimDriver.publish()` | Dùng cho SWIM -> AMHS |
| Subscribe Message | Solace Consumer | `SwimDriver.subscribe()` | Dùng cho AMHS -> SWIM |
| Auth Token | Keycloak OIDC | `SecurityManager.getToken()` | JWT cho REST API |
| Registry Lookup | REST API | `RegistryClient.lookup()` | Tìm kiếm dịch vụ SWIM |

## 3. Directory (Danh bạ)
Sử dụng Isode DSAPI và ATNDS API (thông qua JNI Wrapper).

| Chức năng | API C (Isode) | Java Wrapper Method | Ghi chú |
| :--- | :--- | :--- | :--- |
| Bind Directory | `DS_BindSimpleSync` | `DirectoryDriver.bind()` | Kết nối LDAP/DAP |
| Tìm kiếm | `DS_SearchSync` | `DirectoryDriver.search()` | Tìm O/R Address |
| Chuyển đổi AFTN | `ATNds_AFTN2AMHS` | `DirectoryDriver.convertAftnToAmhs()` | |
| Chuyển đổi AMHS | `ATNds_AMHS2AFTN` | `DirectoryDriver.convertAmhsToAftn()` | |