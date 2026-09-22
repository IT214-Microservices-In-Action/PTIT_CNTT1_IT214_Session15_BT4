# BÀI TẬP 4: XÂY DỰNG "NHẠC TRƯỞNG" ORCHESTRATOR SAGA VỚI STATE MACHINE

## 1. So sánh mô hình Choreography và Orchestration với State Machine

Trong bài tập 3, chúng ta đã dùng **Choreography**, trong đó các service giao tiếp bằng cách phát ra các event (sự kiện) một cách độc lập.
* **Ưu điểm của Choreography**: Tính rời rạc cao, không có điểm lỗi tập trung (Single Point of Failure), các service không cần biết đến sự tồn tại của nhau.
* **Nhược điểm của Choreography**: Khi quy trình trở nên phức tạp (nhiều bước hơn như kiểm tra ưu đãi, vé điện tử, v.v.), việc theo dõi luồng sự kiện rất khó (hiện tượng Event Spaghetti). Nếu xảy ra lỗi ở một bước, việc trace lại luồng để bù trừ (compensation) hoặc debug rất tốn thời gian vì không có cái nhìn tổng quan.

Với **Orchestration sử dụng State Machine**:
* **Lợi ích khi quy trình phức tạp**: Orchestrator (đóng vai trò "Nhạc trưởng") sẽ theo dõi toàn bộ tiến trình. Nó biết chính xác một transaction đang ở trạng thái nào. Khi có lỗi, Orchestrator sẽ chủ động gọi lệnh bù trừ. Logic điều hướng (routing logic) và quản lý trạng thái được tách rời hoàn toàn khỏi logic nghiệp vụ của các service (Payment, Concert).
* Hệ thống sẽ dễ maintain và dễ debug hơn nhiều do luồng đi được định nghĩa rõ ràng ở một nơi duy nhất.

---

## 2. Mô tả chi tiết các trạng thái (States) và sự kiện (Events)

### Các Trạng thái (BookingState)
1. **INITIATED**: Khởi tạo yêu cầu đặt vé.
2. **PAYMENT_PENDING**: Đang chờ xử lý thanh toán từ Payment Service.
3. **PAYMENT_COMPLETED**: Thanh toán đã thành công.
4. **SEAT_RESERVING**: Đang gọi Concert Service để giữ chỗ.
5. **BOOKING_CONFIRMED**: Đặt vé hoàn tất.
6. **CANCELLED**: Bị hủy bỏ (có thể do thanh toán thất bại hoặc giữ chỗ thất bại).

### Các Sự kiện (BookingEvent)
* **PROCESS_PAYMENT**: Kích hoạt việc gọi Payment Service.
* **PAYMENT_SUCCESS**: Sự kiện báo thanh toán thành công.
* **PAYMENT_FAILED**: Sự kiện báo thanh toán thất bại (vượt quá số lần thử).
* **RESERVE_SEATS**: Kích hoạt việc gọi Concert Service.
* **RESERVATION_SUCCESS**: Sự kiện báo giữ chỗ thành công.
* **RESERVATION_FAILED**: Sự kiện báo giữ chỗ thất bại.

### Luồng chuyển đổi (Transition)
`INITIATED` --(PROCESS_PAYMENT)--> `PAYMENT_PENDING`
`PAYMENT_PENDING` --(PAYMENT_SUCCESS)--> `PAYMENT_COMPLETED`
`PAYMENT_PENDING` --(PAYMENT_FAILED)--> `CANCELLED`
`PAYMENT_COMPLETED` --(RESERVE_SEATS)--> `SEAT_RESERVING`
`SEAT_RESERVING` --(RESERVATION_SUCCESS)--> `BOOKING_CONFIRMED`
`SEAT_RESERVING` --(RESERVATION_FAILED)--> `CANCELLED` (và gọi bù trừ Refund)

---

## 3. Giải thích Retry Policy và cơ chế bù trừ (Compensation)

* **Retry Policy**: Cấu hình thử lại tối đa 3 lần với thời gian chờ 2 giây (`RetryPolicy(3, 2000)`) cho quá trình thanh toán. Nếu sau 3 lần gọi Payment Service mà vẫn có lỗi hoặc timeout, State Machine sẽ ngừng cố gắng và chuyển trạng thái sang `CANCELLED`.
* **Cơ chế Bù trừ (Compensation)**: 
  * Nếu lỗi ở bước thanh toán (đã cố 3 lần vẫn lỗi), giao dịch chuyển sang `CANCELLED`, do chưa bị trừ tiền nên có thể không cần bù trừ (hoặc chỉ ghi log).
  * Nếu lỗi ở bước giữ chỗ (Seat Reserving), giao dịch chuyển sang `CANCELLED`. Do lúc này trạng thái trước đó là `PAYMENT_COMPLETED` (đã trừ tiền), State Machine sẽ lập tức gọi `paymentService.refund(transaction)` để hoàn tiền cho khách.

---

## 4. Hướng dẫn cài đặt và chạy

**Yêu cầu hệ thống**:
- Java 17+
- Maven 3.6+

**Cách chạy**:
1. Mở terminal tại thư mục `concert-orchestrator-state-machine`.
2. Chạy lệnh: `mvn spring-boot:run`
3. Ứng dụng sẽ khởi động và `BookingSimulationRunner` sẽ tự động chạy luồng mô phỏng.

---

## 5. Kết quả chạy thử mong muốn

Dựa trên dữ liệu đầu vào:
```json
{
  "bookingId": "CONCERT-2026-088",
  "concertCode": "LIVE-HCM-2026-ULTRA",
  "customerId": "VIP-2024",
  "customerEmail": "rika@email.com",
  "ticketQuantity": 3,
  "amount": 5500000
}
```

**Log console:**
```
=================================================
Bắt đầu mô phỏng Orchestrator Saga State Machine
=================================================
[Orchestrator] State: INITIATED -> Event: PROCESS_PAYMENT -> New State: PAYMENT_PENDING
[Orchestrator] RetryPolicy: Activity 'processPayment' - Attempt 1/3
[PaymentService] Processing payment of 5500000.0 for customer VIP-2024...
[PaymentService] Payment successful for booking CONCERT-2026-088.
[Orchestrator] State: PAYMENT_PENDING -> Event: PAYMENT_SUCCESS -> New State: PAYMENT_COMPLETED
[Orchestrator] State: PAYMENT_COMPLETED -> Event: RESERVE_SEATS -> New State: SEAT_RESERVING
[ConcertService] Reserving 3 seats for concert LIVE-HCM-2026-ULTRA...
[ConcertService] Seats reserved successfully for booking CONCERT-2026-088.
[Orchestrator] State: SEAT_RESERVING -> Event: RESERVATION_SUCCESS -> New State: BOOKING_CONFIRMED
[Orchestrator] Final State: BOOKING_CONFIRMED for booking CONCERT-2026-088
=================================================
Kết thúc mô phỏng
=================================================
```
