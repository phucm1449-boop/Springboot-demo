# Hướng dẫn chạy image `bevismai22112001/shopapp-service:0.0.1`

Tài liệu này ghi lại từng bước để chạy image đã pull từ Docker Hub, kèm giải thích
cho người mới. App cần một database MySQL riêng nên ta chạy **2 container**:
một cho MySQL, một cho app — cùng nằm trên một Docker network để nói chuyện được
với nhau.

## Sơ đồ luồng

```
1. Tạo network       →  2. Chạy MySQL container  →  3. Chờ MySQL "healthy"
                                                            ↓
5. Kiểm tra kết quả   ←  4. Chạy app container (dùng image đã pull)
```

---

## Bước 0: Kiểm tra image đã có sẵn chưa

```bash
docker images | grep -i shopapp
```
- `docker images`: liệt kê toàn bộ image hiện có trên máy.
- `grep -i shopapp`: lọc dòng chứa "shopapp" (không phân biệt hoa/thường) để xác
  nhận image `bevismai22112001/shopapp-service:0.0.1` đã pull về máy.

```bash
docker ps -a
```
- Xem tất cả container (đang chạy **và** đã dừng), để chắc chắn không có container
  nào trùng tên gây lỗi khi tạo mới.

---

## Bước 1: Tạo network riêng cho 2 container

```bash
docker network create shopapp-net
```
- Tạo một mạng ảo Docker tên `shopapp-net`.
- **Vì sao cần?** App và MySQL là 2 container riêng biệt. Nếu không cùng network,
  app không thể gọi tới MySQL bằng tên container (`shopapp-mysql`) — chúng sẽ như
  2 máy không quen biết nhau.

---

## Bước 2: Chạy container MySQL

```bash
docker run -d --name shopapp-mysql --network shopapp-net \
  -e MYSQL_DATABASE=shopapp \
  -e MYSQL_ROOT_PASSWORD=123456789 \
  -p 3307:3306 \
  --health-cmd="mysqladmin ping -h localhost -uroot -p123456789" \
  --health-interval=5s --health-timeout=5s --health-retries=10 \
  mysql:8.0
```

| Phần lệnh | Ý nghĩa |
|---|---|
| `docker run -d` | Chạy container ở chế độ **detached** (chạy nền, không chiếm terminal) |
| `--name shopapp-mysql` | Đặt tên container để dễ tham chiếu sau này (`docker logs shopapp-mysql`, ...) |
| `--network shopapp-net` | Gắn container vào network tạo ở Bước 1 |
| `-e MYSQL_DATABASE=shopapp` | Bảo MySQL tự tạo sẵn database tên `shopapp` khi khởi động lần đầu |
| `-e MYSQL_ROOT_PASSWORD=123456789` | Password cho user `root` — phải khớp với config `DBMS_PASSWORD` mà app dùng |
| `-p 3307:3306` | Map cổng: `3307` trên máy host → `3306` trong container (cổng chuẩn MySQL). Dùng `3307` ở ngoài để tránh đụng MySQL khác có thể đang chạy sẵn ở `3306` |
| `--health-cmd ...` | Cấu hình Docker tự kiểm tra "MySQL đã sẵn sàng nhận kết nối chưa", mỗi 5s, tối đa 10 lần |
| `mysql:8.0` | Image chính thức MySQL 8.0 từ Docker Hub |

---

## Bước 3: Chờ MySQL "healthy" rồi mới chạy app

```bash
for i in $(seq 1 30); do
  status=$(docker inspect --format='{{.State.Health.Status}}' shopapp-mysql 2>/dev/null)
  echo "Attempt $i: $status"
  if [ "$status" = "healthy" ]; then break; fi
  sleep 3
done
```
- `docker inspect --format='{{.State.Health.Status}}' shopapp-mysql`: hỏi Docker
  trạng thái health hiện tại của container (`starting`, `healthy`, hoặc `unhealthy`).
- Vòng lặp kiểm tra mỗi 3 giây, tối đa 30 lần (~90 giây), dừng ngay khi thấy `healthy`.
- **Vì sao cần?** MySQL cần vài giây để khởi tạo database lần đầu. Nếu chạy app
  ngay, app sẽ báo lỗi "connection refused".

---

## Bước 4: Chạy container app từ image đã pull

```bash
docker run -d --name shopapp-app --network shopapp-net \
  -e DBMS_CONNECTION="jdbc:mysql://shopapp-mysql:3306/shopapp?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true" \
  -e DBMS_USERNAME=root \
  -e DBMS_PASSWORD=123456789 \
  -p 8088:8088 \
  bevismai22112001/shopapp-service:0.0.1
```

| Phần lệnh | Ý nghĩa |
|---|---|
| `-d --name shopapp-app --network shopapp-net` | Giống Bước 2 — chạy nền, đặt tên, gắn cùng network với MySQL |
| `-e DBMS_CONNECTION="jdbc:mysql://shopapp-mysql:3306/shopapp?..."` | URL kết nối JDBC tới MySQL. Dùng **tên container** `shopapp-mysql` (không phải `localhost`) — trong network Docker, container gọi nhau bằng tên container, Docker tự phân giải ra IP nội bộ. Cổng `3306` ở đây là cổng nội bộ trong network Docker (khác với `3307` — cổng đó chỉ dùng khi truy cập từ máy host ra ngoài) |
| `useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true` | Tham số JDBC cần thiết để kết nối MySQL 8 không lỗi SSL/timezone |
| `-e DBMS_USERNAME=root` / `-e DBMS_PASSWORD=123456789` | Thông tin đăng nhập MySQL, phải khớp với Bước 2 |
| `-p 8088:8088` | Map cổng `8088` host → `8088` container (cổng app Spring Boot chạy) |
| `bevismai22112001/shopapp-service:0.0.1` | Image đã pull — dòng này thực sự "chạy" nó |

> ⚠️ Các biến `-e` này khớp với tên biến môi trường mà `application.yml` của
> project đọc vào (`DBMS_CONNECTION`, `DBMS_USERNAME`, `DBMS_PASSWORD`). Nếu đặt
> sai tên biến, app sẽ dùng giá trị mặc định (`localhost`) và không kết nối được
> tới container MySQL.

---

## Bước 5: Kiểm tra kết quả

```bash
docker ps --filter "name=shopapp"
```
Liệt kê container có tên chứa "shopapp", xem cả 2 đã ở trạng thái `Up` chưa.

```bash
docker logs shopapp-app --tail 60
```
Xem 60 dòng log gần nhất của app — kiểm tra app khởi động lỗi gì không (kết nối
DB, chạy migration Flyway, ...).

```bash
docker logs shopapp-app 2>&1 | grep -i "Started DemoApplication\|Tomcat started"
```
Lọc log tìm 2 dòng xác nhận: Tomcat đã chạy trên cổng 8088, và Spring Boot app
đã "Started" thành công.

```bash
curl -s -o /dev/null -w "%{http_code}\n" "http://localhost:8088/api/v1/products?page=0&limit=5"
```
Gửi 1 request thử tới API `products`:
- `-s`: im lặng, không hiện progress bar.
- `-o /dev/null`: bỏ nội dung response (không cần xem, chỉ cần biết kết quả).
- `-w "%{http_code}\n"`: chỉ in ra mã HTTP trả về — `200` nghĩa là app chạy đúng
  và trả dữ liệu thành công.

---

## Tóm tắt luồng logic

1. Tạo "mạng" để 2 container thấy nhau
2. Bật MySQL trước
3. Đợi MySQL sẵn sàng
4. Bật app, chỉ cho nó địa chỉ MySQL qua biến môi trường
5. Kiểm tra log + gọi thử API để xác nhận chạy đúng

---

## Dừng / xoá khi không dùng nữa

```bash
docker stop shopapp-app shopapp-mysql
docker rm shopapp-app shopapp-mysql
docker network rm shopapp-net
```

> ⚠️ **Lưu ý mất dữ liệu:** container MySQL ở trên chạy **không có volume** lưu
> data, nên `docker rm shopapp-mysql` sẽ xoá luôn toàn bộ dữ liệu đã seed (bao
> gồm user admin mặc định). Nếu muốn giữ lại dữ liệu qua các lần chạy, cần thêm
> volume, ví dụ:
> ```bash
> docker run -d --name shopapp-mysql --network shopapp-net \
>   -e MYSQL_DATABASE=shopapp \
>   -e MYSQL_ROOT_PASSWORD=123456789 \
>   -p 3307:3306 \
>   -v shopapp_mysql_data:/var/lib/mysql \
>   mysql:8.0
> ```
> (thêm `-v shopapp_mysql_data:/var/lib/mysql` để MySQL lưu data vào volume
> `shopapp_mysql_data` thay vì bên trong container).
