# Cổng giao bài tập học viên

Ứng dụng Spring Boot MVC/Thymeleaf có phân quyền `STUDENT`, `LECTURER`, `ADMIN`. Mọi thao tác ghi dùng POST có CSRF; danh tính và quyền sở hữu lấy từ principal đã đăng nhập, không nhận `studentId`, `lecturerId` hay vai trò từ form.

## Môi trường và khởi chạy

- JDK 25
- MySQL 8.4
- Maven Wrapper đi kèm repository

Tạo database `task_manager`, sau đó chạy ứng dụng. Có thể cấu hình kết nối bằng biến môi trường; mặc định là `jdbc:mysql://localhost:3306/task_manager?createDatabaseIfNotExist=true&serverTimezone=UTC`, user `root`, password rỗng.

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/task_manager?serverTimezone=UTC"
$env:DB_USERNAME = "task_manager"
$env:DB_PASSWORD = "<mật-khẩu-local>"
.\mvnw.cmd spring-boot:run
```

Flyway tự chạy các migration. Hibernate chỉ xác thực schema (`ddl-auto=validate`), không tự tạo hoặc xóa bảng. Không lưu mật khẩu thật trong repository. Tài khoản công khai tạo qua `/register` luôn nhận `ROLE_STUDENT`, mật khẩu lưu bằng BCrypt.

## Vai trò và luồng URL

| URL | STUDENT | LECTURER | ADMIN |
| --- | --- | --- | --- |
| `/student/assignments`, `/student/results/**` | Chỉ bài được giao và kết quả của mình | Không | Không |
| `/lecturer/assignments/**` | Không | Chỉ bài do mình phụ trách; giao, mở/đóng bài và chấm | Không |
| `/admin/dashboard` | Không | Không | Xem tài khoản, bài tập, thống kê; không sửa điểm |
| `/register`, `/login` | Công khai | Công khai | Công khai |

Quan hệ dữ liệu: `users`–`roles` là nhiều-nhiều qua `users_roles`; giảng viên sở hữu nhiều `assignments`; học viên nhận bài qua `assignment_students` với khóa chính ghép; `submissions` tham chiếu cặp assignment/học viên, có `UNIQUE(assignment_id, student_id)`. Không có chức năng upload hoặc tự động truy cập URL bài nộp.

Giảng viên tạo DRAFT với hạn nộp tương lai, giao cho học viên, rồi mở bài. Chỉ học viên được giao mới xem/nộp khi bài ở trạng thái OPEN. Thời gian nộp là `Instant` do server tạo; nộp lại cập nhật cùng bản ghi. Đúng thời điểm deadline được chấp nhận; sau deadline và khi CLOSED, server từ chối. Chấm điểm chỉ được thực hiện một lần bởi giảng viên sở hữu bài đã đóng; điểm 0–10, tối đa hai chữ số thập phân. Mọi trang hiển thị nội dung người dùng bằng Thymeleaf `th:text`.

## Dữ liệu demo và cấp vai trò

1. Khởi động ứng dụng, đăng ký sáu tài khoản: `admin`, `lecturer1`, `lecturer2`, `student1`, `student2`, `student3`. Mật khẩu demo do người vận hành tự chọn, không có mật khẩu mặc định.
2. Chạy `docs/demo-data.sql` bằng tài khoản MySQL quản trị tin cậy trên database local. Script gán role, tạo ba bài ở các trạng thái DRAFT/OPEN/CLOSED và một bài nộp đã chấm. Không cung cấp endpoint tự cấp role; đăng xuất rồi đăng nhập lại để nạp authorities mới.
3. Đăng nhập tài khoản demo để kiểm tra các màn hình theo vai trò.

Chỉ chạy script trên database demo; nó thay vai trò của ba tài khoản có tên `admin`, `lecturer1`, `lecturer2`.

## Kiểm thử và nghiệm thu

```powershell
.\mvnw.cmd clean test
```

Profile test dùng H2 trong bộ nhớ và schema được tạo bằng cùng migration Flyway. `CourseworkSecurityTest` kiểm tra đăng nhập bắt buộc, ranh giới chủ sở hữu học viên/giảng viên, học viên chưa được giao, deadline, CSRF, nộp lại, validation điểm, vai trò chấm điểm, escaping HTML và ngăn tự cấp ADMIN. Các test HTTP xác nhận cả status/view và dữ liệu sau request.

| Tiêu chí đề bài | Kiểm tra |
| --- | --- |
| T01–T02 | GET/POST ID của học viên hoặc giảng viên khác trả 404, dữ liệu không đổi |
| T03–T04 | Tài khoản chưa được giao, bài CLOSED/hết hạn bị từ chối; đúng deadline được nộp |
| T05–T06 | POST thiếu CSRF bị 403; nộp lại giữ nguyên một submission và chủ sở hữu |
| T07–T08 | Điểm ngoài phạm vi/sai độ chính xác không lưu; STUDENT và ADMIN không thể chấm |
| T09–T10 | HTML được escape; form đăng ký chỉ tạo STUDENT dù gửi thêm trường `roles` |

Bảng kiểm thử tự động không thay thế việc smoke test MySQL và trình duyệt. Kiểm tra thêm: USER không có quyền truy cập vùng giảng viên/quản trị; ADMIN chỉ giám sát, không chấm điểm; phân trang hiển thị 10 bản ghi/trang.
